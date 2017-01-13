package ru.rbt.barsgl.ejbtesting.job;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.barsgl.ejbtesting.job.service.TestingJobRegistrationBean;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.util.Properties;

import static java.lang.String.format;

/**
 * Created by Ivan Sevastyanov
 */
@Stateless
@LocalBean
public class TestingJob implements ParamsAwareRunnable {

    private static final Logger LOG = Logger.getLogger(TestingJob.class);

    @EJB
    private TestingJobRegistrationBean jobRegistration;

    @Override
    public void run(String jobName, Properties properties) {
        LOG.info(format("Executing job class '%s', name '%s'", this.getClass().getName(), jobName));
        jobRegistration.registerJobAction(jobName);
    }
}
