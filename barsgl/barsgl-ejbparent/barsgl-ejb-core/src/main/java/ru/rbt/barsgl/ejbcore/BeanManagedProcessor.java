package ru.rbt.barsgl.ejbcore;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import javax.transaction.SystemException;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;
import java.sql.Connection;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;

/**
 * Created by Ivan Sevastyanov
 */
@Stateless
@LocalBean
@TransactionManagement(TransactionManagementType.BEAN)
public class BeanManagedProcessor {

    private static final Logger logger = Logger.getLogger(BeanManagedProcessor.class.getName());

    /**
     * @depricated установка таймаута была нужна для увеличения возможного времени обработки
     * сейчас таймаут сервера большой (3-5 часов), поэтому нет смысла его вообще устанавливать
     */
    private static final int DEFAULT_TIMEOUT_SEC = 5 * 60 * 60;

    @Resource
    private UserTransaction tx;

    @PersistenceContext(unitName="GLOracleDataSource")
    protected EntityManager persistence;

    @Resource (mappedName = "java:comp/TransactionSynchronizationRegistry")
    private TransactionSynchronizationRegistry trx;

//    @Resource(mappedName="/jdbc/As400GL")
    @Resource(mappedName="/jdbc/OracleGL")
    private DataSource dataSource;

    /**
     * Выполнить транзакционную работу в новой транзакции с установкой таймаута
     * @param callback обработчик
     * @param timeoutSecond таймаут транзакции в секундах, если 0, то устанавливается таймаут по умолчанию (vendor specific)
     * @param <E> тип результата
     * @return результат в контексте внутренней транзакции
     * @throws Exception
     */
    public <E> E executeInNewTxWithTimeout(BeanManagedExecutor<E> callback, int timeoutSecond) throws Exception {
        if (timeoutSecond > DEFAULT_TIMEOUT_SEC) {
            setTxTimeout(timeoutSecond);
        } else {
            logger.warning(String.format("Target transaction timeout '%s' less or equals default '%s'. It is not changed."
                    , timeoutSecond, DEFAULT_TIMEOUT_SEC));
        }
        beginTx();
        try(Connection connection = dataSource.getConnection()) {
            E result = callback.execute(persistence, connection);
            commitTx();
            return result;
        } catch (Exception e) {
            rollbackTx();
            throw e;
        }
    }

    /**
     * Выполнение с дефолтным таймаутом 15 мин
     * @param callback
     * @param <E>
     * @return
     * @throws Exception
     */
    public <E> E executeInNewTxWithDefaultTimeout(BeanManagedExecutor<E> callback) throws Exception {
        return executeInNewTxWithTimeout(callback, DEFAULT_TIMEOUT_SEC);
    }

    private void beginTx() {
        try {
            tx.begin();
        } catch (Throwable e) {
            processTxError(e);
        }
    }

    private void commitTx() {
        try {
            tx.commit();
        } catch (Throwable e) {
            processTxError(e);
        }
    }

    public void rollbackTx() {
        try {
            tx.rollback();
        } catch (Throwable e) {
            processTxError(e, false);
        }
    }

    private void setTxTimeout(int second) {
        try {
            tx.setTransactionTimeout(second);
        } catch (SystemException e) {
            processTxError(e);
        }
    }

    private void processTxError(Throwable e) {
        processTxError(e, true);
    }

    private void processTxError(Throwable e, boolean thrownError) {
        logger.log(Level.SEVERE, format("processing error: thrown <%s>", thrownError) , e);
        if (thrownError) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

}
