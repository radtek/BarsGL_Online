package ru.rbt.ejbcore.repository;

import ru.rbt.ejbcore.DataAccessCallback;
import ru.rbt.ejbcore.JpaAccessCallback;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.ejb.Asynchronous;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.SQLException;
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

    <E> List<E> select(EntityManager persistence, Class<E> clazz, String jpaQuery, Object... params);

    <E> List<E> selectHinted(EntityManager persistence, Class<E> clazz, String jpaQuery
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
}
