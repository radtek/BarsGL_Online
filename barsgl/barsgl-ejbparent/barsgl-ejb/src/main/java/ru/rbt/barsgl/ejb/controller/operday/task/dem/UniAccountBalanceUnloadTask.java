package ru.rbt.barsgl.ejb.controller.operday.task.dem;

import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import javax.ejb.EJB;
import java.util.Properties;

/**
 * Created by Ivan Sevastyanov on 22.01.2016.
 * выгрузка остатков по запросу
 */
public class UniAccountBalanceUnloadTask extends AbstractJobHistoryAwareTask {

    @EJB
    private UniAccountBalanceUnloadTaskSupport taskSupport;

    @Override
    protected boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        return taskSupport.execute(properties);
    }

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        return true;
    }

    @Override
    protected boolean checkJobStatus(String jobName, Properties properties) {
        return checkAlreadyRunning(jobName, properties);
    }

    @Override
    protected void initExec(String jobName, Properties properties) throws Exception {}

}
