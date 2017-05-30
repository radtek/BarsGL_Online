package ru.rbt.barsgl.ejbtest;

import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.operday.task.dem.UniAccountBalanceUnloadTask;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;

import java.util.logging.Logger;

/**
 * Created by ER22228
 * Выгрузка данных о проводках и остатках в DWH
 * @fsd 8.2
 */
public class UniAccountBalanceUnloadIT extends AbstractTimerJobIT {

    public static final Logger logger = Logger.getLogger(UniAccountBalanceUnloadIT.class.getName());

    @Test
    public void testFull() throws Exception {
        SingleActionJob job = SingleActionJobBuilder.create()
                .withClass(UniAccountBalanceUnloadTask.class)
                                  .withProps("operday=2016-03-12")
                                  .build();
        jobService.executeJob(job);
    }
}
