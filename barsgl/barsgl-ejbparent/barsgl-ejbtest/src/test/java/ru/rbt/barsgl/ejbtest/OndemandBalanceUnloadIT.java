package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.controller.operday.task.dem.UniAccountBalanceUnloadTask;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import java.io.StringReader;
import java.util.Properties;

/**
 * Created by Ivan Sevastyanov on 22.01.2016.
 */
public class OndemandBalanceUnloadIT extends AbstractTimerJobIT {

    /**
     * проверка корректности выгрузки остатков по запросу
     * @throws Exception
     */
    @Test public void test() throws Exception {

        final String jobName = UniAccountBalanceUnloadTask.class.getSimpleName();

        baseEntityRepository.executeNativeUpdate(
        "declare \n" +
        "   pragma autonomous_transaction;\n" +
        "begin\n" +
        "   execute immediate 'truncate table gl_accrest';\n" +
        "end;");

        baseEntityRepository.executeNativeUpdate("delete from gl_sched_h where SCHED_NAME = ?", jobName);
        JobHistory history = getLastHistRecordObject(jobName);
        Assert.assertNull(history);

        Properties props = new Properties();
        props.load(new StringReader("operday=26.02.2015"));

        SingleActionJobBuilder builder = SingleActionJobBuilder.create()
                .withClass(UniAccountBalanceUnloadTask.class).withProps(props).withName(jobName);
        SingleActionJob job = builder.build();

        jobService.executeJob(job);

        history = getLastHistRecordObject(jobName);
        Assert.assertNotNull(history);
        Assert.assertEquals(DwhUnloadStatus.SUCCEDED, history.getResult());

        Assert.assertTrue(0 < baseEntityRepository.selectOne("select count(1) cnt from gl_accrest r").getLong("cnt"));
    }
}
