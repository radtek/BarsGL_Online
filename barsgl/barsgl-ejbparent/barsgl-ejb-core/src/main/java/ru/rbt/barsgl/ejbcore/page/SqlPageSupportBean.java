package ru.rbt.barsgl.ejbcore.page;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.util.Sql2Xls;
import ru.rbt.barsgl.shared.Export.ExcelExportHead;
import ru.rbt.barsgl.shared.Repository;
import ru.rbt.barsgl.shared.column.XlsColumn;
import ru.rbt.barsgl.shared.criteria.Criterion;
import ru.rbt.barsgl.shared.criteria.OrderByColumn;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.datarec.DataRecordUtils;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.shared.Assert;

import javax.ejb.EJB;
import javax.sql.DataSource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
        String resultSql = null;
        try {
            SQL sql = prepareCommonSql2(defineSql(nativeSql), criterion, orderBy, startWith, pageSize);
            final List<Object> params = new ArrayList<>();
            if (null != sql.getParams()) {
                params.addAll(Arrays.asList(sql.getParams()));
            }

            resultSql = preparePaging(sql.getQuery(), params, pageSize, startWith);

            log.info("SQL[selectRows] => " + resultSql);
            log.info("Parameters list: " + params.stream().map(p -> "param = " + p).collect(Collectors.joining(":")));
            DataSource dataSource = repository.getDataSource(rep);
            return repository.selectMaxRows(dataSource, resultSql, MAX_ROW_COUNT, params.toArray());
        } catch (Exception e) {
            throw new DefaultApplicationException(e.getMessage() + (resultSql != null ? (" sql: " + resultSql) : ""), e);
        }
    }

    @Override
    public int count(String nativeSql, Criterion<?> criterion) {
        return count(nativeSql, Repository.BARSGL, criterion);
    }

    @Override
    public int count(String nativeSql, Repository rep, Criterion<?> criterion) {
        String resultSql = null;
        try {
            SQL sql = prepareCommonSql2(defineSql(nativeSql), criterion, null, 1, MAX_ROW_COUNT);

            if (isWherePresents(sql.getQuery())) {
                resultSql = "select 1 from (" + sql.getQuery() + " ) where rownum <= " + (MAX_ROW_COUNT + 1);
            } else {
                resultSql = "select 1 from (" + sql.getQuery() + " where rownum <= " + (MAX_ROW_COUNT + 1) + ")";
            }
            int cnt = calculateCount(rep, resultSql, sql.getParams());
            if (MAX_ROW_COUNT + 1 == cnt)
                cnt = -MAX_ROW_COUNT;       // свыше MAX_ROW_COUNT
            return cnt;
        } catch (Exception e) {
            throw new DefaultApplicationException(e.getMessage() + (resultSql != null ? (" sql: " + resultSql) : ""), e);
        }
    }

    private int getFirstRowNumber(int pageSize, int pageNumber) {
        return (pageNumber - 1) * pageSize + 1;
    }

    private static boolean isWherePresents(String nativeSql) {
        return nativeSql.toLowerCase().contains("where");
    }

    /*public static SQL prepareCommonSql(final String nativeSql, Criterion criterion) {
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
    }*/

    private static SQL prepareCommonSql2(final String nativeSql, Criterion criterion, OrderByColumn orderBy, int startWith, int pageSize) {
        Assert.isTrue(!StringUtils.isEmpty(nativeSql), "sql is empty");

        String upperSql = nativeSql.trim();
        final boolean isWherePresents = isWherePresents(upperSql);

        SQL whereClause = null;

        if (criterion != null) {
            whereClause = WhereInterpreter.interpret(criterion, isWherePresents ? WHERE_ALIAS : null);
        }

        String resultSql = upperSql + (null != orderBy ? (" order by " + orderBy.getColumn() + " " + orderBy.getOrder()) : "");
        resultSql = "select " + WHERE_ALIAS + ".*, rownum rn from (" + resultSql + ") " + WHERE_ALIAS + " ";

        // применяем where
        resultSql += (null != whereClause ? " where " + whereClause.getQuery() + " and rownum <= ? " : " where rownum <= ? ");

        return new SQL(resultSql, null != whereClause ? addItem(whereClause.getParams(), pageSize + startWith - 1) : new Object[]{pageSize + startWith - 1});
    }

    private String preparePaging(String query, List<Object> params, int pageSize, int startWith) {
        if (pageSize > 0 ) {
            if (startWith > 1) {
                String resultSql = "select * from (" + query + ") v where v.rn >= ?  and v.rn <= ?";
                params.add(startWith);
                params.add(startWith + pageSize - 1);
                return resultSql;
            } else {
                String resultSql = "select * from (" + query + ") v where v.rn <= ?";
                params.add(startWith + pageSize - 1);
                return resultSql;
            }
        }
        return query;
    }

    private static String defineSql(String sql) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        if (sql.trim().toLowerCase().startsWith("select")) return sql;
        String[] parts = sql.split("@");
        Class cls = Class.forName(parts[0]);
        Object obj =  cls.newInstance();
        Method method = cls.getDeclaredMethod(parts[1]);
        return (String) method.invoke(obj, null);
    }

    @Override
    public String export2Excel(String nativeSql, List<XlsColumn> xlsColumns, Criterion<?> criterion, int pageSize, int startWith, OrderByColumn orderBy, ExcelExportHead head) {
        return export2Excel(nativeSql, Repository.BARSGL, xlsColumns, criterion, pageSize, startWith, orderBy, head);
    }

    @Override
    public String export2Excel(String nativeSql, Repository rep, List<XlsColumn> xlsColumns, Criterion<?> criterion, int pageSize, int startWith, OrderByColumn orderBy, ExcelExportHead head)  {
        int pgSize = (pageSize == 0 || pageSize > MAX_PAGE_SIZE) ? MAX_PAGE_SIZE : pageSize;

        String pagingString = buildRowNnumberMarker2(orderBy, pgSize);
        try {
               SQL sql = prepareCommonSql2(defineSql(nativeSql), criterion, null, startWith, pageSize);
               final ArrayList<Object> params = new ArrayList<>();
               if (null != sql.getParams()) {
                   params.addAll(Arrays.asList(sql.getParams()));
               }

           String resultSql = preparePaging2(sql.getQuery(), pagingString, params, startWith, pgSize);   // pagingString,

            DataSource dataSource = repository.getDataSource(rep);
            return (String) repository.executeInNonTransaction(dataSource, connection -> {
                String excelSql = resultSql;        // TODO resultSQL
                Sql2Xls getXls = new Sql2Xls(excelSql, params);
                getXls.setHead(head);
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

    private int calculateCount(Repository dbRepository, String sql, Object[] params) throws Exception {
        final long CNT_TIMEOUT_MS = 3*60*1000; // 3 минуты на расчет кол-ва строк
        return (int) repository.executeTransactionally(repository.getDataSource(dbRepository), connection -> {
            int cnt = 0;
            long till_to = System.currentTimeMillis() + CNT_TIMEOUT_MS;
            try (PreparedStatement query = connection.prepareStatement(sql)){
                query.setFetchSize(1);
                DataRecordUtils.bindParameters(query, params);
                if (query.getQueryTimeout() == 0) {
                    query.setQueryTimeout(DataRecordUtils.DEFAULT_QUERY_TIMEOUT);
                }
                try (ResultSet resultSet = query.executeQuery()) {
                    while (resultSet.next()) {
                        cnt++;
                        if (System.currentTimeMillis() >= till_to) {
                            return cnt > 0 ? -cnt : -MAX_ROW_COUNT;
                        }
                    }
                }
            }
            return cnt;
        });
    }

    private static Object[] addItem(Object[] array, Object object) {
        Object[] params = new Object[array.length + 1];
        System.arraycopy(array, 0, params, 0, array.length);
        params[array.length] = object;
        return params;
    }
}
