package ru.rbt.barsgl.ejbcore.job;

import org.apache.log4j.Logger;

import javax.ejb.EJB;
import java.util.Properties;

/**
 * Created by Ivan Sevastyanov
 */
public class PersistedJobsStatusChecker implements ParamsAwareRunnable {

    private static final Logger LOG = Logger.getLogger(PersistedJobsStatusChecker.class);

    @EJB
    private BackgroundJobService bgService;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        LOG.debug("About to check persisted jobs");
        bgService.refreshJobsStatus();
        LOG.debug("Actualization of persisted jobs status has completed");
    }
}
