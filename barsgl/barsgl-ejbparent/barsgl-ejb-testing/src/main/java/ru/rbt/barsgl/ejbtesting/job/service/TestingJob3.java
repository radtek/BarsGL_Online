package ru.rbt.barsgl.ejbtesting.job.service;

import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;

import java.util.Properties;
import java.util.logging.Logger;

/**
 * Created by Ivan Sevastyanov
 */
public class TestingJob3 implements ParamsAwareRunnable {

    private static final Logger logger = Logger.getLogger(TestingJob3.class.getName());

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        logger.info("!!!STARTING TestingJob3!!!");
        Thread.sleep(900_000);
        logger.info("!!!COMPLETING TestingJob3!!!");
    }
}
