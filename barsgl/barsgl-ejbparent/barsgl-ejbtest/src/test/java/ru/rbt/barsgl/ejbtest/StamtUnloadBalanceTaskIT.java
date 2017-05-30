package ru.rbt.barsgl.ejbtest;

import org.junit.Ignore;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.operday.task.stamt.StamtUnloadBalanceStep2Task;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;

import java.util.logging.Logger;

/**
 * Created by ER22228
 */
public class StamtUnloadBalanceTaskIT extends AbstractTimerJobIT {

    public static final Logger logger = Logger.getLogger(StamtUnloadBalanceTaskIT.class.getName());

    @Test
    @Ignore
    public void testRun() throws Exception {
        SingleActionJob job =
            SingleActionJobBuilder.create()
                .withClass(StamtUnloadBalanceStep2Task.class)
                .withProps("operday=01.10.2015")
                .build();
        jobService.executeJob(job);
    }
}
