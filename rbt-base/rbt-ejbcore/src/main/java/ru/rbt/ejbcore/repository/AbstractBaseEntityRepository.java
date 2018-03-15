package ru.rbt.ejbcore.repository;

import org.apache.log4j.Logger;
import ru.rbt.ejbcore.DataAccessCallback;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.JpaAccessCallback;
import ru.rbt.ejbcore.PersistenceProvider;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.datarec.DataRecordUtils;
import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.shared.Assert;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.*;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.*;
import javax.sql.DataSource;
import javax.transaction.TransactionSynchronizationRegistry;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Created by Ivan Sevastyanov
 */
public abstract class AbstractBaseEntityRepository<T extends BaseEntity, K extends Serializable>
        implements IBaseEntityMultiRepository<T, K> {

    public static final int MAX_ROWS = 1000;

    @Inject
    private Instance<PersistenceProvider<? extends Enum>> instancePersistenceProvider;

    private PersistenceProvider persistenceProvider;

//    @PersistenceContext(unitName="GLOracleDataSource")
//    protected EntityManager persistence;
//
//    @PersistenceContext(unitName="RepAS400DataSource")
//    protected EntityManager barsrepPersistence;
//
////    @Resource(mappedName="/jdbc/As400GL")
////    @Resource(mappedName="/jdbc/OracleGL")
////    @Resource(mappedName="jdbc/OracleGL")
//    private DataSource dataSource;
//
////    @Resource(mappedName="/jdbc/As400Rep")
////    @Resource(mappedName="jdbc/As400Rep")
//    private DataSource  barsrepDataSource;
//
//    @Resource(lookup = "java:app/env/BarsglDataSourceName")
//    private String barsglDataSourceName;
//
//    @Resource(lookup = "java:app/env/BarsrepDataSourceName")
//    private String barsrepDataSourceName;
    
    @Resource
    private EJBContext context;

    //@Resource (mappedName = "java:comp/TransactionSynchronizationRegistry")                             
    @Resource
    private TransactionSynchronizationRegistry trx;

    @Resource
    private SessionContext sessionContext;

    private static final Logger log = Logger.getLogger(AbstractBaseEntityRepository.class);

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<DataRecord> selectNonTransaction(String sqlString, Object ... params) throws SQLException {
        return this.selectNonTransaction(getDataSource(), sqlString, params);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<DataRecord> select(String sqlString, Object ... params) throws SQLException {
        return this.select(getDataSource(),  sqlString, params);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<DataRecord> selectMaxRows(String sqlString, int maxRows, Object[] params) throws SQLException {
        return this.selectMaxRows(getDataSource(), sqlString, maxRows, params);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public DataRecord selectFirst(String sqlString, Object... params) throws SQLException {
        return this.selectFirst(getDataSource(), sqlString, params);
    }

    @Override
    public DataRecord selectOne(String sqlString, Object... params) throws SQLException {
        return this.selectOne(getDataSource(), sqlString, params);
    }

    @Override
    public <T extends Enum> DataRecord selectOne( T enumRepository, String sqlString, Object... params) throws Exception {
        return this.selectOne(getDataSource(enumRepository), sqlString, params);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public <E> E executeInNonTransaction(DataAccessCallback<E> callback) throws Exception {
        return this.executeInNonTransaction(getDataSource(), callback);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public <E, T extends Enum> E executeInNonTransaction(DataAccessCallback<E> callback, T enumRepository) throws Exception {
        return this.executeInNonTransaction(getDataSource(enumRepository), callback);
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public <E> E executeTransactionally(DataAccessCallback<E> callback) throws Exception {
        return this.executeTransactionally(getDataSource(), callback);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public <E> E executeInNewTransaction(JpaAccessCallback<E> callback) throws Exception {
        return this.executeInNewTransaction(getPersistence(), callback);
    }

    @Override
    public <E> E selectFirst(Class<E> clazz, String jpaQuery, Object... params) {
        return this.selectFirst(getPersistence(), clazz, jpaQuery, params);
    }

    @Override
    public <E> E selectOne(Class<E> clazz, String jpaQuery, Object... params) {
        return this.selectOne(getPersistence(), clazz, jpaQuery, params);
    }

    public <E> List<E> selectMaxRows(Class<E> clazz, String jpaQuery, int max, Object ... params) {
        return this.select(getPersistence(), clazz, jpaQuery, max, params);
    }

    @Override
    public <E> List<E> select(Class<E> clazz, String jpaQuery, Object ... params) {
        return this.select(getPersistence(), clazz, jpaQuery, 0, params);
    }

    @Override
    public <E> List<E> selectHinted(Class<E> clazz, String jpaQuery
            , Object[] params, Map<String,String> hints) {
        return this.selectHinted(getPersistence(), clazz, jpaQuery, 0, params, hints);
    }

    @Override
    public T save(T entity) {
        return this.save(getPersistence(), entity);
    }

    public T save(T entity, boolean flush) {
        return this.save(getPersistence(), entity, flush);
    }

    @Override
    public T update(T entity) {
        return this.update(getPersistence(), entity);
    }

    public T update(T entity, boolean flush) {
        return this.update(getPersistence(), entity, flush);
    }

    @Override
    public void remove(T entity) {
        this.remove(getPersistence(), entity);
    }

    @Override
    public int executeUpdate(String jpQuery, Object... params) {
        return this.executeUpdate(getPersistence(), jpQuery, params);
    }

    @Override
    public int executeNativeUpdate(String nativeSQL, Object... params) {
        return this.executeNativeUpdate(getDataSource(), nativeSQL, params);
    }

    @Override
    public T refresh(T entity) {
        return this.refresh(getPersistence(), entity);
    }

    @Override
    public T refresh(T entity, boolean force) {
        return this.refresh(getPersistence(), entity, force);
    }

    @Override
    public T findById(Class<T> clazz, K primaryKey) {
        return this.findById(getPersistence(), clazz, primaryKey);
    }

    @Override
    public Long nextId(String sequenceName) {
        return this.nextId(getPersistence(), sequenceName);
    }

    @Override
    public Integer nextIntegerId(String sequenceName) throws SQLException {
        return this.nextIntegerId(getDataSource(), sequenceName);
    }

    @Override
    public <E> List<E> findNative(Class<E> entityClass, String sqlQuery, int max, Object ... params) {
        return this.findNative(getPersistence(), entityClass, sqlQuery, max, params);
    }

    public <E> E findNativeFirst(Class<E> entityClass, String sqlQuery, Object ... params) {
        return this.findNativeFirst(getPersistence(), entityClass, sqlQuery, params);
    }

    @Asynchronous
    public <V> Future<V> invokeAsynchronous(JpaAccessCallback<V> callback) throws Exception {
        return this.invokeAsynchronous(getPersistence(), callback);
    }

    public void flush() {
        this.flush(getPersistence());
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
//        log.debug("Executing in NEW TX: " + getTransactionKey());
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
        List<E> list = select(persistence, clazz, jpaQuery, 1, params);
        return list.isEmpty() ? null : (E) list.get(0);
    }

    @Override
    public <E> E selectOne(EntityManager persistence, Class<E> clazz, String jpaQuery, Object... params) {
        List<E> list = select(persistence, clazz, jpaQuery, 1, params);
        checkSingleQueryResult(list);
        return list.get(0);
    }

    @Override
    public <E> List<E> select(EntityManager persistence, Class<E> clazz, String jpaQuery, int max, Object... params) {
        return selectHinted(persistence, clazz, jpaQuery, max, params, null);
    }

    @Override
    public <E> List<E> selectHinted(EntityManager persistence, Class<E> clazz, String jpaQuery, int max
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
        if (max > 0) {
            query.setMaxResults(max);
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

    /**
     * Метод заменяет следующий, корректно работает с полем DATA = null
     * @param dataSource
     * @param nativeSQL
     * @param params
     * @return
     */
    public int executeNativeUpdate(DataSource dataSource, String nativeSQL, Object... params) {
        try (Connection connection = dataSource.getConnection()) {
            return DataRecordUtils.executeUpdate(connection, nativeSQL, params);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Некорректно работает с полем DATA = null
     * @param persistence
     * @param nativeSQL
     * @param params
     * @return
     */
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
        return ((BigDecimal) persistence.createNativeQuery("select "
                + sequenceName + ".nextval id_seq from dual").getSingleResult()).longValue();
    }

    @Override
    public Integer nextIntegerId(DataSource dataSource, String sequenceName) throws SQLException {
        return ((BigDecimal) getPersistence().createNativeQuery("select "
                + sequenceName + ".nextval id_seq from dual").getSingleResult()).intValue();
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


    //    @Override
    public <E> List<E> selectJpa(String jpaQuery, Object... params) {
        return this.selectJpa(getPersistence(), jpaQuery, params);
    }

    //    @Override
    public <E> List<E> selectJpa(EntityManager persistence, String jpaQuery, Object... params) {
        Assert.isTrue(!isEmpty(jpaQuery), "Query string cannot be null");

        Query query = persistence.createQuery(jpaQuery);
        bindParams(query, params);
        return query.getResultList();
    }

    @Override
    @Asynchronous
    public <V> Future<V> invokeAsynchronous(EntityManager persistence, JpaAccessCallback<V> callback) throws Exception {
        try {
//            log.debug("Executing asynchronous, TX: " + getTransactionKey());
            return new AsyncResult<>(callback.call(persistence));
        } catch (Exception e) {
            e.printStackTrace();
            setRollbackOnly();
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public <V> Future<V> invoke(JpaAccessCallback<V> callback) throws Exception {
        try {
//            log.debug("Executing asynchronous by thread, TX: " + getTransactionKey());
            return new AsyncResult<>(callback.call(getPersistence()));
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

    protected EntityManager getPersistence(){
        return persistenceProvider.getDefaultPersistence();
    }

    public <T extends Enum> EntityManager getPersistence(T repository) throws Exception{
        return persistenceProvider.getPersistence(repository);
//        if(null == repository) {
//            return persistence;
//        }
//        switch (repository) {
//            case BARSGL:
//                return persistence;
//            case BARSREP:
//                return barsrepPersistence;
//            default:
//                throw new Exception("Неизвестный репозиторий: " + repository.name());
//        }
    }

    protected DataSource getDataSource() {
        return persistenceProvider.getDefaultDataSource();
}

    public <T extends Enum> DataSource getDataSource(T repository) throws Exception {
        return persistenceProvider.getDataSource(repository);

//        if(null == repository) {
//            return dataSource;
//        }
//        switch (repository) {
//            case BARSGL:
//                return dataSource;
//            case BARSREP:
//                return barsrepDataSource;
//            default:
//                throw new Exception("Неизвестный репозиторий: " + repository.name());
//        }
    }

    @PostConstruct
    public void configure() {
        if (instancePersistenceProvider.isUnsatisfied()) {
            throw new RuntimeException("Application PersistenceProvider not found!");
        } else {
            persistenceProvider = instancePersistenceProvider.get();
        }
    }

//    @PostConstruct
//    public void init() {
//        dataSource = findConnection(barsglDataSourceName);
//        try{
//            barsrepDataSource = findConnection(barsrepDataSourceName);
//        }catch(Exception ex){
//            log.info("DataSource not found: "+barsrepDataSourceName);
//        }
//    }
//
//    private DataSource findConnection(String jndiName) {
//        try {
//            InitialContext c = new InitialContext();
//            return  (DataSource) c.lookup(jndiName);
//        } catch (Throwable e) {
//            throw new DefaultApplicationException(e.getMessage(), e);
//        }
//    }
}
