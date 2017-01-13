package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.operday.task.dem.UniAccountBalanceUnloadTask;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;

import java.io.StringReader;
import java.util.Properties;

/**
 * Created by Ivan Sevastyanov on 22.01.2016.
 */
public class OndemandBalanceUnloadTest extends AbstractTimerJobTest {

    /**
     * проверка корректности выгрузки остатков по запросу
     * @throws Exception
     */
    @Test public void test() throws Exception {
        Properties props = new Properties();
        props.load(new StringReader("operday=26.02.2015"));

        SingleActionJobBuilder builder = SingleActionJobBuilder.create()
                .withClass(UniAccountBalanceUnloadTask.class).withProps(props);
        SingleActionJob job = builder.build();

        job = (SingleActionJob) baseEntityRepository.save(job);

        registerJob(job);

        jobService.executeJob(job);

        Assert.assertTrue(0 < baseEntityRepository.select("select * from gl_accrest r").size());
    }
}
