package ru.rbt.barsgl.ejbcore.page;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.util.Sql2Xls;
import ru.rbt.barsgl.ejbcore.util.StringUtils;
import ru.rbt.barsgl.shared.Assert;
import ru.rbt.barsgl.shared.column.XlsColumn;
import ru.rbt.barsgl.shared.criteria.Criterion;
import ru.rbt.barsgl.shared.criteria.OrderByColumn;
import ru.rbt.barsgl.shared.enums.Repository;

import javax.ejb.EJB;
import javax.sql.DataSource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Ivan Sevastyanov
 */
public class SqlPageSupportBean implements SqlPageSupport {
    private static Logger log = Logger.getLogger(SqlPageSupportBean.class);

    public static final String WHERE_ALIAS = "v1";
    public static final String COUNT_ALIAS = "v2";
    public static final int MAX_PAGE_SIZE = 100000;
    public static final int MAX_ROW_COUNT = 5000;

    @EJB
    private CoreRepository repository;

    @Override
    public List<DataRecord> select(final String nativeSql, Criterion<?> criterion, int pageSize, int pageNumber, OrderByColumn orderBy) {
        return selectRows(nativeSql, criterion, pageSize, getFirstRowNumber(pageSize, pageNumber), orderBy);
    }

    @Override
    public List<DataRecord> select(final String nativeSql, Repository rep, Criterion<?> criterion, int pageSize, int pageNumber, OrderByColumn orderBy) {
        return selectRows(nativeSql, rep, criterion, pageSize, getFirstRowNumber(pageSize, pageNumber), orderBy);
    }

    @Override
    public List<DataRecord> selectRows(String nativeSql, Criterion<?> criterion, int pageSize, int startWith, OrderByColumn orderBy) {
        return selectRows(nativeSql, Repository.BARSGL, criterion, pageSize, startWith, orderBy);
    }

    @Override
    public List<DataRecord> selectRows(String nativeSql, Repository rep, Criterion<?> criterion, int pageSize, int startWith, OrderByColumn orderBy) {
        SQL sql = prepareCommonSql(nativeSql, criterion);
        final List<Object> params = new ArrayList<>();
        if (null != sql.getParams()) {
            params.addAll(Arrays.asList(sql.getParams()));
        }

        String resultSql = preparePaging(sql.getQuery(), params, pageSize, startWith, orderBy);

        try {
            log.debug("SQL[selectRows] => " + resultSql);
            params.forEach(p -> log.debug("Param = " + p));
            DataSource dataSource = repository.getDataSource(rep);
            return repository.selectMaxRows(dataSource, resultSql, MAX_ROW_COUNT, params.toArray());
        } catch (Exception e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    @Override
    public int count(String nativeSql, Criterion<?> criterion) {
        return count(nativeSql, Repository.BARSGL, criterion);
    }

    @Override
    public int count(String nativeSql, Repository rep, Criterion<?> criterion) {
        SQL sql = prepareCommonSql(nativeSql, criterion);
        String resultSql = "select count(*) cnt from (" + sql.getQuery() + " fetch first " + (MAX_ROW_COUNT + 1) + " rows only) " + COUNT_ALIAS;
        try {
            DataSource dataSource = repository.getDataSource(rep);
            int cnt = repository.selectFirst(dataSource, resultSql, sql.getParams()).getInteger("cnt");
            if (MAX_ROW_COUNT + 1 == cnt)
                cnt = -MAX_ROW_COUNT;       // свыше MAX_ROW_COUNT
            return cnt;
        } catch (Exception e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private int getFirstRowNumber(int pageSize, int pageNumber) {
        return (pageNumber - 1) * pageSize + 1;
    }

    private static boolean isWherePresents(String nativeSql) {
        return nativeSql.toLowerCase().contains("where");
    }

    private static SQL prepareCommonSql(final String nativeSql, Criterion criterion) {
        Assert.isTrue(!StringUtils.isEmpty(nativeSql), "sql is empty");

        SQL whereClause = null;

        if (criterion != null) {
            whereClause = WhereInterpreter.interpret(criterion, isWherePresents(nativeSql) ? WHERE_ALIAS : null);
        }

        String resultSql;
        if (isWherePresents(nativeSql)) {
            resultSql = "select * from (" + nativeSql + ") " + WHERE_ALIAS + " ";
        } else {
            resultSql = nativeSql;
        }
        // применяем where
        resultSql += (null != whereClause ? " where " + whereClause.getQuery() : "");
        return new SQL(resultSql, null != whereClause ? whereClause.getParams() : null);
    }

    private String preparePaging(String query, List<Object> params, int pageSize, int startWith, OrderByColumn orderBy) {
        String orderByColumn = orderBy != null ? orderBy.getColumn() + " " + orderBy.getOrder() : "1";
        String orderByString = orderBy != null ? " order by " + orderByColumn : "";
        if (pageSize > 0 ) {
            if (startWith > 1) {
                String pagingString = "row_number() over (order by " + orderByColumn + ") rn";
                String resultSql = "select * from (select aaa.*, " + pagingString + " from (" + query + ") aaa) v where v.rn between ? and ?";
                params.add(startWith);
                params.add(startWith + pageSize - 1);
                return resultSql;
            } else {
                String resultSql = "select * from (" + query + ") v " + orderByString + " fetch first " + pageSize + " rows only";
                return resultSql;
            }
        } else {
            return orderBy != null ? "select * from (" + query + ") v " + orderByString  : query;
        }
    }

    @Override
    public String export2Excel(String nativeSql, List<XlsColumn> xlsColumns, Criterion<?> criterion, int pageSize, int startWith, OrderByColumn orderBy) {
        return export2Excel(nativeSql, Repository.BARSGL, xlsColumns, criterion, pageSize, startWith, orderBy);
    }

    @Override
    public String export2Excel(String nativeSql, Repository rep, List<XlsColumn> xlsColumns, Criterion<?> criterion, int pageSize, int startWith, OrderByColumn orderBy)  {
        int pgSize = (pageSize == 0 || pageSize > MAX_PAGE_SIZE) ? MAX_PAGE_SIZE : pageSize;

        String pagingString = buildRowNnumberMarker2(orderBy, pgSize);

        SQL sql = prepareCommonSql(nativeSql, criterion);
        final ArrayList<Object> params = new ArrayList<>();
        if (null != sql.getParams()) {
            params.addAll(Arrays.asList(sql.getParams()));
        }

        String resultSql = preparePaging2(sql.getQuery(), pagingString, params, startWith, pgSize);   // pagingString,

        try {
            DataSource dataSource = repository.getDataSource(rep);
            return (String) repository.executeInNonTransaction(dataSource, connection -> {
                String excelSql = resultSql;        // TODO resultSQL
                Sql2Xls getXls = new Sql2Xls(excelSql, params);
                getXls.setColumns(xlsColumns);

                File f = File.createTempFile("barsgl", ".xlsx", new File(System.getProperty("java.io.tmpdir")));
                OutputStream outStream = new FileOutputStream(f);
                try {
                    getXls.process(outStream, connection);
                    outStream.flush();
                    return f.getAbsolutePath();    // TODO
                } catch (Exception e) {
                    throw new DefaultApplicationException(e.getMessage(), e);
                } finally {
                    outStream.close();
                }
            });
        } catch (Exception e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private String preparePaging2(String query, String pagingString, List<Object> params, int startWith, int pageSize) {
        if (StringUtils.isEmpty(pagingString)) {
            return query;
        }
        else if (pageSize == 0) {
            return "select * from (" + query + ") T " + pagingString;
        }
        else {
            params.add(startWith);
            params.add(startWith + pageSize - 1);
            return "select * from (select aaa.*, " + pagingString + " from (" + query + ") aaa) v where v.rn between ? and ?";
        }
    }

    private String buildRowNnumberMarker2(OrderByColumn orderBy, int pageSize) {
        String orderStr = "order by " + (orderBy != null ? orderBy.getColumn() + " " + orderBy.getOrder() : "1");
        if (pageSize > 0)
            return "row_number() over (" + orderStr + ") rn";
        else if (orderBy != null)
            return orderStr;
        else return "";
    }
}
