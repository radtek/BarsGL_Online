package ru.rbt.barsgl.ejbtest;

import org.junit.Ignore;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.operday.task.LoadProcessRunAsyncTask;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;

import java.util.logging.Logger;

/**
 * Created by ER22228
 */
public class LoadProcessRunAsyncTaskTest extends AbstractTimerJobTest {

    public static final Logger logger = Logger.getLogger(LoadProcessRunAsyncTaskTest.class.getName());

    @Test
    @Ignore
    public void testCall() throws Exception {
        SingleActionJob job =
            SingleActionJobBuilder.create()
                .withClass(LoadProcessRunAsyncTask.class)
                .withProps(
                    "step = WT_1\n" +
                    "operday = 2016-02-02\n"
                )
                .build();
        jobService.executeJob(job);
    }
}
