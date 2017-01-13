package ru.rbt.barsgl.ejbtest;

import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.task.RecalcBS2Task;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;

/**
 * Created by Ivan Sevastyanov on 17.02.2016.
 */
public class Bs2UnloadTest extends AbstractTimerJobTest {

    @Test
    public void test() throws Exception {

        updateOperday(Operday.OperdayPhase.COB, Operday.LastWorkdayStatus.CLOSED, Operday.PdMode.BUFFER);

        SingleActionJob job = SingleActionJobBuilder.create()
                .withClass(RecalcBS2Task.class).build();
        jobService.executeJob(job);
    }
}
