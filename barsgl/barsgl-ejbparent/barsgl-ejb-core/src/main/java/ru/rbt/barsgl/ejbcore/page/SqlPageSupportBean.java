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
import ru.rbt.ejbcore.datarec.DefaultJdbcAdapter;
import ru.rbt.ejbcore.datarec.JdbcAdapter;
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ru.rbt.barsgl.ejbcore.page.SqlPageSupportBean.TimeoutInternal.DEFAULT_SQL_TIMEOUT;

/**
 * Created by Ivan Sevastyanov
 */
public class SqlPageSupportBean implements SqlPageSupport {

    private static final Pattern SELECT_PATPATTERN = Pattern.compile("select");

    private static Logger log = Logger.getLogger(SqlPageSupportBean.class);

    public static final String WHERE_ALIAS = "v1";
    public static final int MAX_PAGE_SIZE = 100000;
    public static final int MAX_ROW_COUNT = 5000;

    public enum TimeoutInternal {
        DEFAULT_SQL_TIMEOUT(3, TimeUnit.MINUTES);

        private final int timeout;
        private final TimeUnit timeUnit;

        TimeoutInternal(int timeout, TimeUnit timeUnit) {
            this.timeout = timeout;
            this.timeUnit = timeUnit;
        }

        public int getTimeout() {
            return timeout;
        }

        public TimeUnit getTimeUnit() {
            return timeUnit;
        }
    }

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
        SQL sql = null;
        try {
            sql = prepareCommonSql3(defineSql(nativeSql), criterion, orderBy);
            log.info("SQL[selectRows] => " + sql.getQuery());
            log.info("Parameters list: " + Arrays.asList(sql.getParams()).stream().map(p -> "param = " + p).collect(Collectors.joining(":")));
            return getSqlResult(rep, sql, startWith, pageSize, TimeoutInternal.DEFAULT_SQL_TIMEOUT.getTimeUnit(), TimeoutInternal.DEFAULT_SQL_TIMEOUT.getTimeout());
        } catch (Exception e) {
            throw new DefaultApplicationException((e.getMessage() != null ? e.getMessage() : "") + (sql != null ? (" sql: " + sql.getQuery()
                    + " Parameters list: " + Arrays.asList(sql.getParams()).stream().map(p -> "param = " + p).collect(Collectors.joining(":"))) : ""), e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<DataRecord> getSqlResult(Repository dbRepository, SQL sql, int startWith, int pageSize, TimeUnit timeUnit, int timeout) throws Exception {
        final PreparedStatement[] query = {null};
        Future<List<DataRecord>> resf = repository.invokeAsynchronous(repository.getPersistence(dbRepository), persistence -> (repository.executeTransactionally(repository.getDataSource(dbRepository)
                , connection -> {
                    query[0] = connection.prepareStatement(sql.getQuery());
                    query[0].setQueryTimeout(new Long(TimeUnit.SECONDS.convert(timeout, timeUnit)).intValue());
                    DataRecordUtils.bindParameters(query[0], sql.getParams());
                    JdbcAdapter adapter = new DefaultJdbcAdapter(query[0]);
                    List<DataRecord> result = new ArrayList<>();
                    try {
                        try (ResultSet rs = query[0].executeQuery()){
                            while (rs.next()) {
                                DataRecord rec = new DataRecord(rs, adapter);
                                result.add(rec);
                                if ((startWith + pageSize - 1) == result.size()) {
                                    return result.subList(startWith - 1, result.size());
                                }
                            }
                            return result.subList(startWith - 1, result.size());
                        }
                    } catch (SQLException e) {
                        if (result.size() > 0 && startWith <= result.size()) {
                            return result.subList(startWith - 1, result.size());
                        } else {
                            throw e;
                        }
                    }
                })));
        try {
            return resf.get(timeout, timeUnit);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw e;
        } catch (TimeoutException e) {
            if (query[0] != null)
                query[0].cancel();
            if (!resf.isDone())
                resf.cancel(true);
            throw e;
        }
    }

    @Override
    public int count(String nativeSql, Criterion<?> criterion) {
        return count(nativeSql, Repository.BARSGL, criterion);
    }

    @Override
    public int count(String nativeSql, Repository rep, Criterion<?> criterion) {
        SQL sql = null;
        String resultSql = null;
        try {
            sql = prepareCommonSql3(defineSql(nativeSql), criterion, null);
            resultSql = "select count(1) cnt from ( " + sql.getQuery() + " ) where rownum <= " + (MAX_ROW_COUNT + 1);

            return calculateCount(rep, resultSql, sql.getParams());

        } catch (Throwable e) {
            throw new DefaultApplicationException((e.getMessage() != null ? e.getMessage() : "") + (resultSql != null ? (" sql: " + resultSql
                    + " Parameters list: " + Arrays.asList(sql.getParams()).stream().map(p -> "param = " + p).collect(Collectors.joining(":"))) : ""), e);
        }
    }

    private int getFirstRowNumber(int pageSize, int pageNumber) {
        return (pageNumber - 1) * pageSize + 1;
    }

    private static boolean isWherePresents(String nativeSql) {
        return nativeSql.toLowerCase().contains("where");
    }

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

    private static SQL prepareCommonSql3(final String nativeSql, Criterion criterion, OrderByColumn orderBy) {
        Assert.isTrue(!StringUtils.isEmpty(nativeSql), "sql is empty");

        final String HINT = "first_rows";

        final boolean isHinted = nativeSql.toLowerCase().contains(HINT);


        String resultSql = nativeSql.trim();


        final boolean isWherePresents = isWherePresents(resultSql);

        SQL whereClause = null;

        if (criterion != null) {
            whereClause = WhereInterpreter.interpret(criterion, isWherePresents ? WHERE_ALIAS : null);
        }

        // применяем where
        resultSql = isWherePresents ? "select * from (" + resultSql + ") " : resultSql;
        resultSql += (null != whereClause ? " where " + whereClause.getQuery() : "");
        resultSql += (null != orderBy ? (" order by " + orderBy.getColumn() + " " + orderBy.getOrder()) : "");

        if (!isHinted) {
            resultSql = SELECT_PATPATTERN.matcher(resultSql).replaceFirst("select /*+ first_rows(30) */");
        }

        return new SQL(resultSql, null != whereClause ? whereClause.getParams() : null);
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
        List<DataRecord> result = getSqlResult(dbRepository, new SQL(sql, params), 1, 1, DEFAULT_SQL_TIMEOUT.getTimeUnit(), DEFAULT_SQL_TIMEOUT.getTimeout());
        return MAX_ROW_COUNT + 1 == result.get(0).getInteger("cnt") ? -MAX_ROW_COUNT : result.get(0).getInteger("cnt");
    }

    private static Object[] addItem(Object[] array, Object object) {
        Object[] params = new Object[array.length + 1];
        System.arraycopy(array, 0, params, 0, array.length);
        params[array.length] = object;
        return params;
    }
}
