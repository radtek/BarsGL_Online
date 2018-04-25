package ru.rbt.barsgl.ejb.controller.operday.task.srvacc.newexample;

import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;

import javax.inject.Inject;
import java.util.Properties;

/**
 * Created by er18837 on 23.04.2018.
 */
public class AccLQ2Task implements ParamsAwareRunnable {

    @Inject
    private AccLQJms2Proc processor;

    @Inject
    private AccLQJms2Communicator communicator;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        communicator.init(properties);
        processor.init(communicator);
    }
}
