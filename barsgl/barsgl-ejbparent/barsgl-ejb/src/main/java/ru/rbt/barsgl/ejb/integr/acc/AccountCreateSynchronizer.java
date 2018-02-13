package ru.rbt.barsgl.ejb.integr.acc;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;

/**
 * Created by Ivan Sevastyanov on 10.11.2017.
 */
public class AccountCreateSynchronizer {

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

}
