package ru.rbt.barsgl.ejbtesting.test;

import ru.rbt.barsgl.ejbcore.DbTryingExecutor;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.TryingDataAccessCallback;

import javax.ejb.EJB;
import java.util.concurrent.TimeUnit;

/**
 * Created by Ivan Sevastyanov on 01.12.2016.
 */
public class TryingTestingBean {

    @EJB
    private DbTryingExecutor executor;

    public int testTrying(int checkNumber, int attemts) throws Exception {
        TryingDataAccessCallback<Integer> callback = (TryingDataAccessCallback<Integer>) (connection, att) -> {
            if (checkNumber != att) {
                throw new DefaultApplicationException(String.format("Not equals check = %s, attemts = %s", checkNumber, att));
            }
            return att;
        };
        return executor.tryExecuteTransactionally(callback, attemts, TimeUnit.HOURS, 0);
    }
}
