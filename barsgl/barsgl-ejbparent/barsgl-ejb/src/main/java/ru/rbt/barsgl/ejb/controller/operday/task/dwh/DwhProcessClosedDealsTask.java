package ru.rbt.barsgl.ejb.controller.operday.task.dwh;

import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import java.util.Properties;

public class DwhProcessClosedDealsTask extends AbstractJobHistoryAwareTask {

    @Override
    protected boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        return false;
    }

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        return false;
    }

    @Override
    protected void initExec(String jobName, Properties properties) {

    }
}
