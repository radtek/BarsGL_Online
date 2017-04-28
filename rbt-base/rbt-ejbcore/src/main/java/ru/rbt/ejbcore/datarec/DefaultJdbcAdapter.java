package ru.rbt.ejbcore.datarec;

import oracle.jdbc.OraclePreparedStatement;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Адаптер JDBC, используемый по умолчанию
 */
public class DefaultJdbcAdapter implements JdbcAdapter {

    private static final Logger log = Logger.getLogger(DefaultJdbcAdapter.class.getName());

    private static final int UNSUPPORTED_FEATURE = -10101010;

    private ParameterMetaData parameterMetaData;

    public DefaultJdbcAdapter(PreparedStatement preparedStatement) {
        try {
            parameterMetaData = preparedStatement.getParameterMetaData();
        } catch (SQLException e) {
            log.log(Level.WARNING, "Error on get parameter meta data: " + e.getMessage());
            parameterMetaData = null;
        }
    }

    public DefaultJdbcAdapter() {
        parameterMetaData = null;
    }

    public void setParameterValue(PreparedStatement ps, int index, Object value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            if (value instanceof Date) {
                // сохраняем дату вместе со временем
                ps.setTimestamp(index, new Timestamp( ((Date)value).getTime() ));
            } else if (value.getClass().equals(byte[].class)) {
                // записываем бинарные данные через поток
                byte[] bytes = (byte[]) value;
                ps.setBinaryStream(index, new ByteArrayInputStream(bytes), bytes.length);
            } else if (value instanceof String) {
                int sqlType = getParameterType(parameterMetaData, index);
                if (Types.CHAR == sqlType) {
                    if (ps instanceof OraclePreparedStatement) {
                        ((OraclePreparedStatement)ps).setFixedCHAR(index, (String) value);
                        return;
                    }
                }
                ps.setString(index, (String) value);
            } else {
                // остальное отдаем на откуп драйверу
                ps.setObject(index, value);
            }
        }
    }

    public Object getResultValue(ResultSet rs, int index) throws SQLException {
        int sqltype = rs.getMetaData().getColumnType(index);
        Object value = rs.getObject(index);
        if (rs.wasNull()) {
            return null;
        }
        switch (sqltype) {
            case Types.DATE:
                return rs.getDate(index);
            case Types.TIMESTAMP:
            case Types.TIME:
                // преобразуем дату к Date
                Timestamp timestamp = rs.getTimestamp(index);
                if (null != timestamp) {
                    return new Date(timestamp.getTime());
                } else {
                    return null;
                }
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
            case Types.BLOB:
                // считываем бинарные данные через поток
                InputStream is = rs.getBinaryStream(index);
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buff = new byte[4096];
                    do {
                        int count = is.read(buff);
                        if (count < 0) {
                            break;
                        }
                        baos.write(buff, 0, count);
                    } while (true);
                    return baos.toByteArray();
                } catch (IOException ex) {
                    SQLException sqlex = new SQLException("Failed to read binary data");
                    sqlex.initCause(ex);
                    throw sqlex;
                }
            case Types.CLOB:
                return rs.getString(index);
            default:
                return value;
        }
    }

    public Object getResultValue(CallableStatement cs, int index, int type) throws SQLException {
        Object value = cs.getObject(index);
        if (cs.wasNull()) {
            return null;
        }
        switch (type) {
            case Types.TIMESTAMP:
            case Types.DATE:
            case Types.TIME:
                // преобразуем дату к Date
                return new Date(cs.getTimestamp(index).getTime());
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
            case Types.BLOB:
            case Types.CLOB:
                // считываем бинарные данные напрямую
                return cs.getBytes(index);
            default:
                return value;
        }
    }

    private int getParameterType(ParameterMetaData parameterMetaData, int index) throws SQLException {
        try {
            if (null != parameterMetaData){
                return parameterMetaData.getParameterType(index);
            }
        } catch (SQLException e) {
            log.log(Level.WARNING, "Error on get parameter type idx=" + index+ ": " + e.getMessage());
            return UNSUPPORTED_FEATURE;
        }
        return UNSUPPORTED_FEATURE;
    }
}
