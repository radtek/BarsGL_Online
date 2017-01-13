package ru.rbt.barsgl.ejbtesting.job.service;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;

import javax.ejb.EJB;
import java.util.Properties;

import static java.lang.String.format;

/**
 * Created by Ivan Sevastyanov
 */
public class TestingJobTwo implements ParamsAwareRunnable {

    private static final Logger LOG = Logger.getLogger(TestingJobTwo.class);

    @EJB
    private TestingJobRegistrationBean jobRegistration;

    @Override
    public void run(String jobName, Properties properties) {
        LOG.info(format("Executing job class '%s', name '%s'", this.getClass().getName(), jobName));
        jobRegistration.registerJobAction(jobName);
    }
}
