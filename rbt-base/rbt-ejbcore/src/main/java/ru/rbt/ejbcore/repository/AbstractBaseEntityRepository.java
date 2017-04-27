package ru.rbt.ejbcore.repository;

import org.apache.log4j.Logger;
import ru.rbt.ejbcore.DataAccessCallback;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.JpaAccessCallback;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.datarec.DataRecordUtils;
import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.shared.Assert;
import ru.rbt.shared.enums.Repository;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.persistence.*;
import javax.sql.DataSource;
import javax.transaction.TransactionSynchronizationRegistry;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import javax.annotation.PostConstruct;
import javax.naming.InitialContext;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Created by Ivan Sevastyanov
 */
public abstract class AbstractBaseEntityRepository<T extends BaseEntity, K extends Serializable>
        implements IBaseEntityMultiRepository<T, K> {

    public static final int MAX_ROWS = 1000;

    @PersistenceContext(unitName="GLAS400DataSource")
    protected EntityManager persistence;

    @PersistenceContext(unitName="RepAS400DataSource")
    protected EntityManager barsrepPersistence;

    //@Resource(mappedName="/jdbc/As400GL")
    private DataSource dataSource;

    //@Resource(mappedName="/jdbc/As400Rep")
    private DataSource  barsrepDataSource;

    @Resource(lookup = "java:app/env/BarsglDataSourceName")
    private String barsglDataSourceName;

    @Resource(lookup = "java:app/env/BarsrepDataSourceName")
    private String barsrepDataSourceName;
    
    @Resource
    private EJBContext context;

    //@Resource (mappedName = "java:comp/TransactionSynchronizationRegistry")                             
    @Resource
    private TransactionSynchronizationRegistry trx;

    private static final Logger log = Logger.getLogger(AbstractBaseEntityRepository.class);

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<DataRecord> selectNonTransaction(String sqlString, Object ... params) throws SQLException {
        return this.selectNonTransaction(dataSource, sqlString, params);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<DataRecord> select(String sqlString, Object ... params) throws SQLException {
        return this.select(dataSource,  sqlString, params);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<DataRecord> selectMaxRows(String sqlString, int maxRows, Object[] params) throws SQLException {
        return this.selectMaxRows(dataSource, sqlString, maxRows, params);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public DataRecord selectFirst(String sqlString, Object... params) throws SQLException {
        return this.selectFirst(dataSource, sqlString, params);
    }

    @Override
    public DataRecord selectOne(String sqlString, Object... params) throws SQLException {
        return this.selectOne(dataSource, sqlString, params);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public <E> E executeInNonTransaction(DataAccessCallback<E> callback) throws Exception {
        return this.executeInNonTransaction(dataSource, callback);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public <E> E executeInNonTransaction(DataAccessCallback<E> callback, Repository enumRepository) throws Exception {
        return this.executeInNonTransaction(getDataSource(enumRepository), callback);
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public <E> E executeTransactionally(DataAccessCallback<E> callback) throws Exception {
        return this.executeTransactionally(dataSource, callback);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public <E> E executeInNewTransaction(JpaAccessCallback<E> callback) throws Exception {
        return this.executeInNewTransaction(persistence, callback);
    }

    @Override
    public <E> E selectFirst(Class<E> clazz, String jpaQuery, Object... params) {
        return this.selectFirst(persistence, clazz, jpaQuery, params);
    }

    @Override
    public <E> E selectOne(Class<E> clazz, String jpaQuery, Object... params) {
        return this.selectOne(persistence, clazz, jpaQuery, params);
    }

    @Override
    public <E> List<E> select(Class<E> clazz, String jpaQuery, Object ... params) {
        return this.select(persistence, clazz, jpaQuery, params);
    }

    @Override
    public <E> List<E> selectHinted(Class<E> clazz, String jpaQuery
            , Object[] params, Map<String,String> hints) {
        return this.selectHinted(persistence, clazz, jpaQuery, params, hints);
    }

    @Override
    public T save(T entity) {
        return this.save(persistence, entity);
    }

    public T save(T entity, boolean flush) {
        return this.save(persistence, entity, flush);
    }

    @Override
    public T update(T entity) {
        return this.update(persistence, entity);
    }

    public T update(T entity, boolean flush) {
        return this.update(persistence, entity, flush);
    }

    @Override
    public void remove(T entity) {
        this.remove(persistence, entity);
    }

    @Override
    public int executeUpdate(String jpQuery, Object... params) {
        return this.executeUpdate(persistence, jpQuery, params);
    }

    @Override
    public int executeNativeUpdate(String nativeSQL, Object... params) {
        return this.executeNativeUpdate(persistence, nativeSQL, params);
    }

    @Override
    public T refresh(T entity) {
        return this.refresh(persistence, entity);
    }

    @Override
    public T refresh(T entity, boolean force) {
        return this.refresh(persistence, entity, force);
    }

    @Override
    public T findById(Class<T> clazz, K primaryKey) {
        return this.findById(persistence, clazz, primaryKey);
    }

    @Override
    public Long nextId(String sequenceName) {
        return this.nextId(persistence, sequenceName);
    }

    @Override
    public Integer nextIntegerId(String sequenceName) throws SQLException {
        return this.nextIntegerId(dataSource, sequenceName);
    }

    @Override
    public <E> List<E> findNative(Class<E> entityClass, String sqlQuery, int max, Object ... params) {
        return this.findNative(persistence, entityClass, sqlQuery, max, params);
    }

    public <E> E findNativeFirst(Class<E> entityClass, String sqlQuery, Object ... params) {
        return this.findNativeFirst(persistence, entityClass, sqlQuery, params);
    }

    @Asynchronous
    public <V> Future<V> invokeAsynchronous(JpaAccessCallback<V> callback) throws Exception {
        return this.invokeAsynchronous(persistence, callback);
    }

    public void flush() {
        this.flush(persistence);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<DataRecord> selectNonTransaction(DataSource dataSource, String sqlString, Object... params) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return DataRecordUtils.select(connection, sqlString, params, MAX_ROWS);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<DataRecord> select(DataSource dataSource, String sqlString, Object... params) throws SQLException {
        return selectMaxRows(dataSource, sqlString, MAX_ROWS, params);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<DataRecord> selectMaxRows(DataSource dataSource, String sqlString, int maxRows, Object[] params) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return DataRecordUtils.select(connection, sqlString, params, maxRows);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public DataRecord selectFirst(DataSource dataSource, String sqlString, Object... params) throws SQLException {
        List<DataRecord> list = selectMaxRows(dataSource, sqlString, 1, params);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public <E> E executeInNonTransaction(DataSource dataSource, DataAccessCallback<E> callback) throws Exception {
        try(Connection connection = dataSource.getConnection()) {
            return callback.call(connection);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public <E> E executeTransactionally(DataSource dataSource, DataAccessCallback<E> callback) throws Exception {
        try(Connection connection = dataSource.getConnection()) {
            return callback.call(connection);
        }
    }

    @Override
    public DataRecord selectOne(DataSource dataSource, String sqlString, Object... params) throws SQLException {
        List<DataRecord> list = select(dataSource, sqlString, params);
        checkSingleQueryResult(list);
        return list.get(0);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public <E> E executeInNewTransaction(EntityManager persistence, JpaAccessCallback<E> callback) throws Exception {
        log.debug("Executing in NEW TX: " + getTransactionKey());
        try {
            return callback.call(persistence);
        } catch (Throwable e) {
            e.printStackTrace();
            setRollbackOnly();
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    @Override
    public <E> E selectFirst(EntityManager persistence, Class<E> clazz, String jpaQuery, Object... params) {
        List<E> list = select(persistence, clazz, jpaQuery, params);
        return list.isEmpty() ? null : (E) list.get(0);
    }

    @Override
    public <E> E selectOne(EntityManager persistence, Class<E> clazz, String jpaQuery, Object... params) {
        List<E> list = select(persistence, clazz, jpaQuery, params);
        checkSingleQueryResult(list);
        return list.get(0);
    }

    @Override
    public <E> List<E> select(EntityManager persistence, Class<E> clazz, String jpaQuery, Object... params) {
        return selectHinted(persistence, clazz, jpaQuery, params, null);
    }

    @Override
    public <E> List<E> selectHinted(EntityManager persistence, Class<E> clazz, String jpaQuery
            , Object[] params, Map<String, String> hints) {
        TypedQuery<E> query = persistence.createQuery(jpaQuery, clazz);
        // TODO https://bugs.eclipse.org/bugs/show_bug.cgi?id=372689 query.setMaxResults(MAX_ROWS);
        if (null != hints) {
            for (Map.Entry<String,String> entry : hints.entrySet()) {
                query.setHint(entry.getKey(), entry.getValue());
            }
        }
        if (null != params) {
            for (int i = 1; i <= params.length; i++) {
                query.setParameter(i, params[i-1]);
            }
        }
        return query.getResultList();
    }

    @Override
    public T save(EntityManager persistence, T entity) {
        return save(persistence, entity, true);
    }

    @Override
    public T save(EntityManager persistence, T entity, boolean flush) {
        persistence.persist(entity);
        if (flush) {
            persistence.flush();
        }
        return entity;
    }

    @Override
    public T update(EntityManager persistence, T entity) {
        return update(persistence, entity, true);
    }

    @Override
    public T update(EntityManager persistence, T entity, boolean flash) {
        entity = persistence.merge(entity);
        if (flash) persistence.flush();
        return entity;
    }

    @Override
    public void remove(EntityManager persistence, T entity) {
        persistence.remove(entity);
    }

    @Override
    public int executeUpdate(EntityManager persistence, String jpQuery, Object... params) {
        Assert.notNull(jpQuery, "Query string cannot be null");
        Query query = persistence.createQuery(jpQuery);
        bindParams(query, params);
        int result = query.executeUpdate();
        persistence.flush();
        return result;
    }

    @Override
    public int executeNativeUpdate(EntityManager persistence, String nativeSQL, Object... params) {
        Assert.notNull(nativeSQL, "Native SQL query string cannot be null");
        Query query = persistence.createNativeQuery(nativeSQL);
        bindParams(query, params);
        int result = query.executeUpdate();
        persistence.flush();
        return result;
    }

    @Override
    public T refresh(EntityManager persistence, T entity) {
        persistence.refresh(entity);
        return entity;
    }

    @Override
    public T refresh(EntityManager persistence, T entity, boolean force) {
        if (force) {
            return (T) persistence.createQuery(
                    "from " + entity.getClass().getName() + " e where e.id = ?1"
                    , entity.getClass()).setHint("javax.persistence.cache.storeMode", "REFRESH")
                    .setParameter(1, entity.getId()).getSingleResult();

        } else {
            return refresh(persistence, entity);
        }
    }

    @Override
    public T findById(EntityManager persistence, Class<T> clazz, K primaryKey) {
        return persistence.find(clazz, primaryKey);
    }

    @Override
    public Long nextId(EntityManager persistence, String sequenceName) {
        Long id = (Long) persistence.createNativeQuery("select (next value for " + sequenceName + ") id_seq from sysibm.sysdummy1").getSingleResult();
        return id;
    }

    @Override
    public Integer nextIntegerId(DataSource dataSource, String sequenceName) throws SQLException {
        return selectOne(dataSource, "select (next value for " + sequenceName + ") id_seq from sysibm.sysdummy1").getInteger("id_seq");
    }

    @Override
    public <E> List<E> findNative(EntityManager persistence, Class<E> entityClass, String sqlQuery, int max, Object... params) {

        Assert.isTrue(!isEmpty(sqlQuery), "Query string cannot be null");

        Query query;
        if (null != entityClass) {
            query = persistence.createNativeQuery(sqlQuery, entityClass);
        } else {
            query = persistence.createNativeQuery(sqlQuery);
        }
        bindParams(query, params);
        if (max > 0) {
            query.setMaxResults(max);
        }
        return (List<E>)query.getResultList();
    }

    @Override
    public <E> E findNativeFirst(EntityManager persistence, Class<E> entityClass, String sqlQuery, Object... params) {
        List<E> list = findNative(persistence, entityClass, sqlQuery, 1, params);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    @Asynchronous
    public <V> Future<V> invokeAsynchronous(EntityManager persistence, JpaAccessCallback<V> callback) throws Exception {
        try {
            log.debug("Executing asynchronous, TX: " + getTransactionKey());
            return new AsyncResult<>(callback.call(persistence));
        } catch (Exception e) {
            e.printStackTrace();
            setRollbackOnly();
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    @Override
    public void flush(EntityManager persistence) {
        persistence.flush();
    }

    private void checkSingleQueryResult(List list) {
        if (1 < list.size()) {
            throw new NonUniqueResultException("Found more than one entity on query");
        } else
        if (list.isEmpty()){
            throw new NoResultException("Query returned no results");
        }
    }

    private void bindParams(Query query, Object ... params) {
        if (params != null) {
            for (int i = 1; i <= params.length; i++) {
                query.setParameter(i, params[i-1]);
            }
        }
    }

    @Override
    public Object getTransactionKey() {
        return trx.getTransactionKey();
    }

    @Override
    public void setRollbackOnly() {
        if (!context.getRollbackOnly()) {
            context.setRollbackOnly();
        }
    }

    @Override
    public boolean isRollbackOnly() {
        return context.getRollbackOnly();
    }

    public EntityManager getPersistence(Repository repository) throws Exception {
        if(null == repository) {
            return persistence;
        }
        switch (repository) {
            case BARSGL:
                return persistence;
            case BARSREP:
                return barsrepPersistence;
            default:
                throw new Exception("Неизвестный репозиторий: " + repository.name());
        }
    }

    public DataSource getDataSource(Repository repository) throws Exception {
        if(null == repository) {
            return dataSource;
        }
        switch (repository) {
            case BARSGL:
                return dataSource;
            case BARSREP:
                return barsrepDataSource;
            default:
                throw new Exception("Неизвестный репозиторий: " + repository.name());
        }
    }

    @PostConstruct
    public void init() {
        dataSource = findConnection(barsglDataSourceName);
        barsrepDataSource = findConnection(barsrepDataSourceName);
    }

    private DataSource findConnection(String jndiName) {
        try {
            InitialContext c = new InitialContext();
            return  (DataSource) c.lookup(jndiName);
        } catch (Throwable e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }
}
