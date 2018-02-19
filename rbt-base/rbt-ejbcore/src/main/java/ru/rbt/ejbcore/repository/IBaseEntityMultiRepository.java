package ru.rbt.ejbcore.repository;

import ru.rbt.ejbcore.DataAccessCallback;
import ru.rbt.ejbcore.JpaAccessCallback;
import ru.rbt.ejbcore.datarec.DBParam;
import ru.rbt.ejbcore.datarec.DBParams;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.datarec.DataRecordUtils;
import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.ejbcore.util.EjbCoreUtils;

import javax.ejb.Asynchronous;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.*;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Created by SotnikovAV on 25.10.2016.
 */
public interface IBaseEntityMultiRepository<T extends BaseEntity, K extends Serializable> extends BaseEntityRepository<T, K> {
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    List<DataRecord> selectNonTransaction(DataSource dataSource, String sqlString, Object... params) throws SQLException;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    List<DataRecord> select(DataSource dataSource, String sqlString, Object... params) throws SQLException;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    List<DataRecord> selectMaxRows(DataSource dataSource, String sqlString, int maxRows, Object[] params) throws SQLException;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    DataRecord selectFirst(DataSource dataSource, String sqlString, Object... params) throws SQLException;

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    <E> E executeInNonTransaction(DataSource dataSource, DataAccessCallback<E> callback) throws Exception;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    <E> E executeTransactionally(DataSource dataSource, DataAccessCallback<E> callback) throws Exception;

    Integer nextIntegerId(DataSource dataSource, String sequenceName) throws SQLException;

    DataRecord selectOne(DataSource dataSource, String sqlString, Object... params) throws SQLException;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    <E> E executeInNewTransaction(EntityManager persistence, JpaAccessCallback<E> callback) throws Exception;

    <E> E selectFirst(EntityManager persistence, Class<E> clazz, String jpaQuery, Object... params);

    <E> E selectOne(EntityManager persistence, Class<E> clazz, String jpaQuery, Object... params);

    <E> List<E> select(EntityManager persistence, Class<E> clazz, String jpaQuery, int max, Object... params);

    <E> List<E> selectHinted(EntityManager persistence, Class<E> clazz, String jpaQuery, int max
            , Object[] params, Map<String, String> hints);

    T save(EntityManager persistence, T entity);

    T save(EntityManager persistence, T entity, boolean flush);

    T update(EntityManager persistence, T entity);

    T update(EntityManager persistence, T entity, boolean flash);

    void remove(EntityManager persistence, T entity);

    int executeUpdate(EntityManager persistence, String jpQuery, Object... params);

    int executeNativeUpdate(EntityManager persistence, String nativeSQL, Object... params);

    T refresh(EntityManager persistence, T entity);

    T refresh(EntityManager persistence, T entity, boolean force);

    T findById(EntityManager persistence, Class<T> clazz, K primaryKey);

    Long nextId(EntityManager persistence, String sequenceName);

    <E> List<E> findNative(EntityManager persistence, Class<E> entityClass, String sqlQuery, int max, Object... params);

    <E> E findNativeFirst(EntityManager persistence, Class<E> entityClass, String sqlQuery, Object... params);

    @Asynchronous
    <V> Future<V> invokeAsynchronous(EntityManager persistence, JpaAccessCallback<V> callback) throws Exception;

    void flush(EntityManager persistence);

    boolean isRollbackOnly();

    void setRollbackOnly();

    Object getTransactionKey();

    default DBParams executeCallable(DataSource dataSource, String callable, DBParams params) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             CallableStatement statement = connection.prepareCall(callable)){
            for (int i = 1; i <= params.getParams().size(); i++) {
                DBParam param = params.getParams().get(i-1);
                if (EjbCoreUtils.containsEnum(param.getDirectionType(), DBParam.DBParamDirectionType.IN, DBParam.DBParamDirectionType.IN_OUT)) {
                    DataRecordUtils.bindParameters(statement, params.getParams().stream().map(DBParam::getValue).toArray());
                } else {
                    statement.registerOutParameter(i, param.getParamType());
                }
            }
            statement.executeUpdate();
            for (int i = 1; i<= params.getParams().size(); i++) {
                DBParam param = params.getParams().get(i - 1);
                if (param.getDirectionType() == DBParam.DBParamDirectionType.OUT) {
                    param.setValue(getValue(statement, param.getParamType(), i));
                }
            }
        }
        return params;
    }

    static Object getValue(CallableStatement statement, int sqlType, int index) throws SQLException {
        Object value = statement.getObject(index);
        if (statement.wasNull()) {
            return null;
        }
        switch (sqlType) {
            case Types.DATE:
                return statement.getDate(index);
            case Types.TIMESTAMP:
            case Types.TIME:
                // преобразуем дату к Date
                Timestamp timestamp = statement.getTimestamp(index);
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
                InputStream is = statement.getBlob(index).getBinaryStream();
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
                return statement.getString(index);
            default:
                return value;
        }

    }
}
