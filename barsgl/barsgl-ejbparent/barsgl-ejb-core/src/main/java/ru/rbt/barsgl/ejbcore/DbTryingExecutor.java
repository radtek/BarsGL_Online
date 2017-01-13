package ru.rbt.barsgl.ejbcore;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;

/**
 * Created by Ivan Sevastyanov on 01.12.2016.
 */
@Stateless
@LocalBean
public class DbTryingExecutor {

    private static final Logger logger = Logger.getLogger(DbTryingExecutor.class.getName());

    @EJB
    private CoreRepository repository;

    public <E> E tryExecuteTransactionally(TryingDataAccessCallback<E> callback, final int attemptNumber, TimeUnit delayUnit, long delay) throws Exception {
        int cnt = 0;
        Throwable throwable = null;
        while (attemptNumber - cnt > 0) {
            cnt++;
            try {
                final int finalCnt = cnt;
                return (E) repository.executeInNewTransaction(persistence ->
                        repository.executeTransactionally(connection -> callback.call(connection, finalCnt))
                );
            } catch (Throwable e) {
                throwable = e;
                logger.log(Level.SEVERE, "Error on attempting callback", e);
                if (attemptNumber - cnt > 0) {
                    // задержка только если будет еще попытка
                    delayUnit.sleep(delay);
                }
            }
        }
        InterruptedException exception = new InterruptedException(format("Exceeds attemts count '%s' of executing callback", attemptNumber));
        throw new Exception(exception.initCause(throwable));
    }


}
