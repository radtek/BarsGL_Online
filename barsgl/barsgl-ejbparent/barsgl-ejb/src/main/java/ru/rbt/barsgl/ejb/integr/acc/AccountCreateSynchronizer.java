package ru.rbt.barsgl.ejb.integr.acc;

import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejbcore.controller.etc.TextResourceController;
import ru.rbt.ejbcore.datarec.DBParam;
import ru.rbt.ejbcore.datarec.DBParams;

import javax.inject.Inject;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.logging.Logger;

import static ru.rbt.ejbcore.datarec.DBParam.DBParamDirectionType.IN;
import static ru.rbt.ejbcore.datarec.DBParam.DBParamDirectionType.OUT;
import static ru.rbt.ejbcore.datarec.DBParam.DbParamType.INTEGER;
import static ru.rbt.ejbcore.datarec.DBParam.DbParamType.VARCHAR;

/**
 * Created by Ivan Sevastyanov on 10.11.2017.
 */
public class AccountCreateSynchronizer {

    private static final Logger log = Logger.getLogger(AccountCreateSynchronizer.class.getName());

    @Inject
    private CoreRepository repository;

    @Inject
    private TextResourceController text;

    public  <T> T callSynchronously(Lock monitor, Callable<T> callable) throws Exception {
        if (monitor.tryLock(5, TimeUnit.MINUTES)) {
            try {
                return callable.call();
            } finally {
                monitor.unlock();
            }
        } else {
            throw new TimeoutException("Wait time to lock is exceeded");
        }
    }

    public <T> T callSynchronously(SyncKey syncKey, int timeoutSec, Callable<T> callable) throws Exception {
        String handle = lock(syncKey.getKey(), timeoutSec);
        try {
            return callable.call();
        } finally {
            releaseLock(handle);
        }
    }

    /**
     * получаем монопольную блокировку
     * @param lockName название блокировки
     * @param timeoutSec время ожидания
     * @return lock handle
     * @throws IOException
     * @throws SQLException
     */
    private String lock(String lockName, int timeoutSec) throws IOException, SQLException {
        DBParams result = repository.executeCallable(text.getContent("ru/rbt/barsgl/ejb/integr/acc/request_db_lock.sql")
                , DBParams.createParams(new DBParam(VARCHAR, IN, lockName)
                        , new DBParam(INTEGER, IN, timeoutSec), new DBParam(VARCHAR, OUT)));
        String handle = result.getParams().get(2).getString();
        log.info("Lock taken by name: " + lockName + " handle: " + handle);
        return handle;
    }

    /**
     * освобождаем блокировку
     * @param handle lock handle
     * @throws IOException
     * @throws SQLException
     */
    private void releaseLock(String handle) throws IOException, SQLException {
        repository.executeCallable(text.getContent("ru/rbt/barsgl/ejb/integr/acc/release_db_lock.sql")
                , DBParams.createParams(new DBParam(VARCHAR, IN, handle)));
        log.info("Lock released by handle: " + handle);
    }
}
