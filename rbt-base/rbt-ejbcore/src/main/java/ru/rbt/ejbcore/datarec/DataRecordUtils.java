package ru.rbt.ejbcore.datarec;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Ivan Sevastyanov
 */
public class DataRecordUtils {

    public static final int DEFAULT_QUERY_TIMEOUT = 15 * 60;

    public static int executeUpdate(Connection connection, String sql, Object[] objects) throws SQLException {
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(sql);
            bindParameters(statement, objects);
            return statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (null != statement) {
                statement.close();
            }
        }
    }

    /**
     * Список строк завернутых в DataRecord
     * @param conn
     * @param query
     * @param params
     * @param max
     * @return
     * @throws SQLException
     */
    public static List<DataRecord> select(Connection conn, String query, Object[] params, int max) throws SQLException {
        return select(conn, query, params, max, 0);
    }

    /**
     * Список строк завернутых в DataRecord c таймаутом
     * @param conn
     * @param query
     * @param params
     * @param max
     * @param timeout second
     * @return
     * @throws SQLException
     */
    public static List<DataRecord> select(Connection conn, String query, Object[] params, int max, int timeout) throws SQLException {
        List result = new ArrayList();
        try (PreparedStatement ps = conn.prepareStatement(query)){
            JdbcAdapter adapter = new DefaultJdbcAdapter(ps);
            if (0 < timeout) {
                // если установлен таймаут в параметрах
                ps.setQueryTimeout(timeout);
            } else if (ps.getQueryTimeout() == 0) {
                // по умолчанию 15 минут, если таймаут не установлен сервером приложений по умолчанию
                ps.setQueryTimeout(DEFAULT_QUERY_TIMEOUT);
            }
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    adapter.setParameterValue(ps, i + 1, params[i]);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                int rows_fetched = 0;
                while (rs.next()) {
                    DataRecord rec = new DataRecord(rs, adapter);
                    result.add(rec);
                    rows_fetched++;
                    if ((max > 0) && (rows_fetched >= max)) {
                        break;
                    }
                }
            }
            return result;
        }
    }

    public static void bindParameters(PreparedStatement statement, Object[] objects) throws SQLException {
        if (null != objects) {
            for (int i = 0; i < objects.length; i++) {
                Object param = objects[i];
                if (null == param) {
                    statement.setObject(i + 1, null);
                } else
                if (param instanceof String) {
                    statement.setString(i + 1, (String)param);
                } else
                if (param instanceof Date) {
                    statement.setDate(i + 1, new java.sql.Date(((Date)param).getTime()));
                } else
                if (param instanceof Long) {
                    statement.setLong(i + 1, ((Long)param).longValue());
                } else
                if (param instanceof BigDecimal) {
                    statement.setBigDecimal(i + 1, (BigDecimal) param);
                } else
                if (param instanceof Integer) {
                    statement.setInt(i + 1, ((Integer) param).intValue());
                } else {
                    statement.setObject(i + 1, param);
                }
            }
        }
    }

    /**
     * todo убрать дублирование !!!
     * @param statement
     * @param i
     * @param object
     * @throws SQLException
     */
    public static void bindParameter(PreparedStatement statement, int i, Object object) throws SQLException {
        if (null != object) {
            if (null == object) {
                statement.setObject(i, null);
            } else if (object instanceof String) {
                statement.setString(i, (String) object);
            } else if (object instanceof Date) {
                statement.setDate(i, new java.sql.Date(((Date) object).getTime()));
            } else if (object instanceof Long) {
                statement.setLong(i, ((Long) object).longValue());
            } else if (object instanceof BigDecimal) {
                statement.setBigDecimal(i, (BigDecimal) object);
            } else if (object instanceof Integer) {
                statement.setInt(i, ((Integer) object).intValue());
            } else {
                statement.setObject(i, object);
            }
        }
    }
}
