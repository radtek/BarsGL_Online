package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.ejbcore.DataAccessCallback;
import ru.rbt.barsgl.ejbcore.DbTryingExecutor;
import ru.rbt.barsgl.ejbcore.TryingDataAccessCallback;
import ru.rbt.barsgl.ejbtesting.test.TryingTestingBean;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.TimeUnit;

/**
 * Created by Ivan Sevastyanov on 01.12.2016.
 */
public class TryingExecutionTest extends AbstractRemoteTest {

    @Test
    public void test() {

        Assert.assertTrue(3 == (int)remoteAccess.invoke(TryingTestingBean.class, "testTrying", 3, 3));
        Assert.assertTrue(10 == (int)remoteAccess.invoke(TryingTestingBean.class, "testTrying", 10, 10));

        boolean exc = false;
        try {
            remoteAccess.invoke(TryingTestingBean.class, "testTrying", 3, 2);
        } catch (Exception e) {
            e.printStackTrace();
            exc = true;
        }
        Assert.assertTrue(exc);

    }
}
