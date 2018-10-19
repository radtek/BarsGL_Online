package ru.rbt.barsgl.ejbcore.page;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.util.Sql2Xls;
import ru.rbt.barsgl.shared.Export.ExcelExportHead;
import ru.rbt.barsgl.shared.Repository;
import ru.rbt.barsgl.shared.RpcRes_Base;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejbcore.page.SqlPageSupportBean.TimeoutInternal.DEFAULT_SQL_TIMEOUT;

/**
 * Created by Ivan Sevastyanov
 */
public class SqlPageSupportBean implements SqlPageSupport {

    private static final Pattern SELECT_PATPATTERN = Pattern.compile("select");

    private static Logger log = Logger.getLogger(SqlPageSupportBean.class);

    public static final String WHERE_ALIAS = "v1";
    public static final int MAX_ROW_COUNT = 5000;
    public static final int MAX_ROW_COUNT_LIMIT = 100000;

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
        List<OrderByColumn> orderList = new ArrayList<OrderByColumn>();
        orderList.add(orderBy);
        return selectRows(nativeSql, criterion, pageSize, getFirstRowNumber(pageSize, pageNumber), orderList);
    }

    @Override
    public List<DataRecord> select(final String nativeSql, Repository rep, Criterion<?> criterion, int pageSize, int pageNumber, OrderByColumn orderBy) {
        List<OrderByColumn> orderList = new ArrayList<OrderByColumn>();
        orderList.add(orderBy);
        return selectRows(nativeSql, rep, criterion, pageSize, getFirstRowNumber(pageSize, pageNumber), orderList);
    }

    @Override
    public List<DataRecord> selectRows(String nativeSql, Criterion<?> criterion, int pageSize, int startWith, List<OrderByColumn> orderBy) {
        return selectRows(nativeSql, Repository.BARSGL, criterion, pageSize, startWith, orderBy);
    }

    @Override
    public List<DataRecord> selectRows(String nativeSql, Repository rep, Criterion<?> criterion, int pageSize, int startWith, List<OrderByColumn> orderBy) {
        SQL sql = null;
        try {
            sql = prepareCommonSqlOrder(defineSql(nativeSql), criterion, orderBy);
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
                                    return standardSublist(result, startWith);
                                }
                            }
                            if (startWith > result.size()) {
                                // sometimes startsWith > total rows count!!! Possible GUI Bug?!
                                return Collections.emptyList();
                            } else {
                                return standardSublist(result, startWith);
                            }
                        }
                    } catch (SQLException e) {
                        if (result.size() > 0 && startWith <= result.size()) {
                            return standardSublist(result, startWith);
                        } else {
                            throw e;
                        }
                    }
                })));
        try {
            List<DataRecord> result = new ArrayList<>();
            result.addAll(resf.get(timeout, timeUnit));
            return result;
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
            sql = prepareCommonSql(defineSql(nativeSql), criterion, null);
            resultSql = "select 1 cntr from ( " + sql.getQuery() + " ) where rownum <= " + (MAX_ROW_COUNT + 1);

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

    public static SQL prepareCommonSql(final String nativeSql, Criterion criterion, OrderByColumn orderBy) {
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
        resultSql = isWherePresents ? "select * from (" + resultSql + ") " + WHERE_ALIAS : resultSql;
        resultSql += (null != whereClause ? " where " + whereClause.getQuery() : "");
        resultSql += (null != orderBy ? (" order by " + orderBy.getColumn() + " " + orderBy.getOrder()) : "");

        if (!isHinted) {
            resultSql = SELECT_PATPATTERN.matcher(resultSql).replaceFirst("select /*+ first_rows(30) */");
        }

        return new SQL(resultSql, null != whereClause ? whereClause.getParams() : null);
    }

    public static SQL prepareCommonSqlOrder(final String nativeSql, Criterion criterion, List<OrderByColumn> orderBy) {
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
        resultSql = isWherePresents ? "select * from (" + resultSql + ") " + WHERE_ALIAS : resultSql;
        resultSql += (null != whereClause ? " where " + whereClause.getQuery() : "");
        resultSql += (null != orderBy ? (" order by " + StringUtils.listToString(
                orderBy.stream().map(order -> order.getColumn() + " " + order.getOrder()).collect(Collectors.toList()), ",")) : "");

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
    public String export2Excel(String nativeSql, List<XlsColumn> xlsColumns, Criterion<?> criterion, int pageSize, int startWith, OrderByColumn orderBy, ExcelExportHead head) throws Exception {
        return export2Excel(nativeSql, Repository.BARSGL, xlsColumns, criterion, pageSize, startWith, orderBy, head);
    }

    @Override
    public String export2Excel(String nativeSql, Repository repository, List<XlsColumn> xlsColumns, Criterion<?> criterion, int pageSize, int startWith, OrderByColumn orderBy, ExcelExportHead head, boolean allrows) throws Exception {
        String resultSql = null;
        final ArrayList<Object> params = new ArrayList<>();
        try {
            SQL sql = prepareCommonSql(defineSql(nativeSql), criterion, null);
            if (null != sql.getParams()) {
                params.addAll(Arrays.asList(sql.getParams()));
            }

            resultSql =  "select * from ( " + sql.getQuery() + " ) where rownum <= " + ((allrows ? MAX_ROW_COUNT_LIMIT : MAX_ROW_COUNT)  + 1);

            List<DataRecord> dataRecords = getRows4Excel(repository, resultSql, sql.getParams());
            Sql2Xls getXls = new Sql2Xls(dataRecords);
            if (dataRecords.size() == MAX_ROW_COUNT_LIMIT) {
                head.setFormTitle(head.getFormTitle() + format(" \n\n !!! ВНИМАНИЕ! Выборка неполная. Достигнут максимальный размер выгрузки данных: %s строк!", MAX_ROW_COUNT_LIMIT));
            }
            getXls.setHead(head);
            getXls.setColumns(xlsColumns);

            File f = File.createTempFile("barsgl", ".xlsx", new File(System.getProperty("java.io.tmpdir")));
            OutputStream outStream = new FileOutputStream(f);

            try {
                getXls.process(outStream);
                outStream.flush();
                return f.getAbsolutePath();
            } catch (Exception e) {
                throw new DefaultApplicationException(e.getMessage(), e);
            } finally {
                outStream.close();
            }

        } catch (Exception e) {
            throw new DefaultApplicationException((e.getMessage() != null ? e.getMessage() : "") + (resultSql != null ? (" sql: " + resultSql
                    + " Parameters list: " + params.stream().map(p -> "param = " + p).collect(Collectors.joining(":"))) : ""), e);
        }
    }

    @Override
    public String export2ExcelSort(String nativeSql, Repository repository, List<XlsColumn> xlsColumns, Criterion<?> criterion, int pageSize, int startWith, List<OrderByColumn> orderBy, ExcelExportHead head, boolean allrows) throws Exception {
        String resultSql = null;
        final ArrayList<Object> params = new ArrayList<>();
        try {
            SQL sql = prepareCommonSqlOrder(defineSql(nativeSql), criterion, orderBy);
            if (null != sql.getParams()) {
                params.addAll(Arrays.asList(sql.getParams()));
            }

            resultSql =  "select * from ( " + sql.getQuery() + " ) where rownum <= " + ((allrows ? MAX_ROW_COUNT_LIMIT : MAX_ROW_COUNT)  + 1);

            List<DataRecord> dataRecords = getRows4Excel(repository, resultSql, sql.getParams());
            Sql2Xls getXls = new Sql2Xls(dataRecords);
            if (dataRecords.size() == MAX_ROW_COUNT_LIMIT) {
                head.setFormTitle(head.getFormTitle() + format(" \n\n !!! ВНИМАНИЕ! Выборка неполная. Достигнут максимальный размер выгрузки данных: %s строк!", MAX_ROW_COUNT_LIMIT));
            }
            getXls.setHead(head);
            getXls.setColumns(xlsColumns);

            File f = File.createTempFile("barsgl", ".xlsx", new File(System.getProperty("java.io.tmpdir")));
            OutputStream outStream = new FileOutputStream(f);

            try {
                getXls.process(outStream);
                outStream.flush();
                return f.getAbsolutePath();
            } catch (Exception e) {
                throw new DefaultApplicationException(e.getMessage(), e);
            } finally {
                outStream.close();
            }

        } catch (Exception e) {
            throw new DefaultApplicationException((e.getMessage() != null ? e.getMessage() : "") + (resultSql != null ? (" sql: " + resultSql
                    + " Parameters list: " + params.stream().map(p -> "param = " + p).collect(Collectors.joining(":"))) : ""), e);
        }
    }

    @Override
    public RpcRes_Base<Boolean> export2ExcelExists(String nativeSql, Repository repository, Criterion<?> criterion) throws Exception {
        String resultSql = null;
        final ArrayList<Object> params = new ArrayList<>();
        try {
            SQL sql = prepareCommonSql(defineSql(nativeSql), criterion, null);
            if (null != sql.getParams()) {
                params.addAll(Arrays.asList(sql.getParams()));
            }

            resultSql =  "select 1 from ( " + sql.getQuery() + " ) where rownum <= 1";

            List<DataRecord> dataRecords = getRows4Excel(repository, resultSql, sql.getParams());
            if (null == dataRecords || dataRecords.size() == 0)
                return new RpcRes_Base<>(false, true, "Нет данных для выгрузки");
            else
                return new RpcRes_Base<>(true, false, "");

        } catch (Exception e) {
            throw new DefaultApplicationException((e.getMessage() != null ? e.getMessage() : "") + (resultSql != null ? (" sql: " + resultSql
                    + " Parameters list: " + params.stream().map(p -> "param = " + p).collect(Collectors.joining(":"))) : ""), e);
        }
    }

    @Override
    public String export2Excel(String nativeSql, Repository rep, List<XlsColumn> xlsColumns, Criterion<?> criterion, int pageSize, int startWith, OrderByColumn orderBy, ExcelExportHead head) throws Exception {
        return export2Excel(nativeSql, rep, xlsColumns, criterion, pageSize, startWith, orderBy, head, false);
    }

    private List<DataRecord> getRows4Excel(Repository dbRepository, String sql, Object[] params) throws Exception {
        return getSqlResult(dbRepository, new SQL(sql, params), 1, MAX_ROW_COUNT_LIMIT,  DEFAULT_SQL_TIMEOUT.getTimeUnit(), DEFAULT_SQL_TIMEOUT.getTimeout());
    }

    private int calculateCount(Repository dbRepository, String sql, Object[] params) throws Exception {
        List<DataRecord> result = getSqlResult(dbRepository, new SQL(sql, params), 1, MAX_ROW_COUNT + 1, DEFAULT_SQL_TIMEOUT.getTimeUnit(), DEFAULT_SQL_TIMEOUT.getTimeout());
        return MAX_ROW_COUNT + 1 == result.size() ? -MAX_ROW_COUNT : result.size();
    }

    private List<DataRecord> standardSublist(List<DataRecord> totalResult, int startWith) {
        return totalResult.subList(startWith - 1, totalResult.size());
    }

}
