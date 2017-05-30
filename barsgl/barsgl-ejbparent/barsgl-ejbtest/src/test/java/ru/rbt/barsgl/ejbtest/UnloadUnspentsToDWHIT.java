package ru.rbt.barsgl.ejbtest;

import org.junit.Ignore;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.operday.task.UnloadUnspentsToDWHServiceTask;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;

import java.util.logging.Logger;

/**
 * Created by ER22228
 * Выгрузка данных о проводках и остатках в DWH
 * @fsd 8.2
 */
@Ignore("Больше не используется тестируемый функционал")
public class UnloadUnspentsToDWHIT extends AbstractTimerJobIT {

    public static final Logger logger = Logger.getLogger(UnloadUnspentsToDWHIT.class.getName());

    @Test
    public void testFull() throws Exception {
        SingleActionJob job = SingleActionJobBuilder.create()
                .withClass(UnloadUnspentsToDWHServiceTask.class)
                                  .withProps("#minDay=2016-02-18\n#maxDay=2016-03-18\ncheckRun=false")
                                  .build();
        jobService.executeJob(job);
    }
}
