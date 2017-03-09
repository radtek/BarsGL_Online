package ru.rbt.barsgl.gwt.server.rpc.sync;

import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.tasks.ejb.entity.task.JobHistory;
import ru.rbt.tasks.ejb.job.BackgroundJobsController;
import ru.rbt.tasks.ejb.repository.JobHistoryRepository;
import ru.rbt.barsgl.gwt.core.server.rpc.AbstractGwtService;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.jobs.TimerJobHistoryWrapper;

import java.util.Properties;

/**
 * Created by Ivan Sevastyanov on 01.11.2016.
 */
public class PdSyncServiceImpl extends AbstractGwtService implements PdSyncService {

    private static final String JOB_NAME = "PdSyncTask";

    @Override
    public RpcRes_Base<TimerJobHistoryWrapper> execPdSync() throws Exception {
        try {
            boolean isAlreadyRunning = localInvoker.invoke(JobHistoryRepository.class, "isAlreadyRunningLike", new Object[]{null, JOB_NAME});
            if (isAlreadyRunning) {
                return new RpcRes_Base<>(null, true, "Есть незаконченная задача синхронизации");
            } else {
                TimerJobHistoryWrapper history = localInvoker.invoke(BackgroundJobsController.class, "createTimerJobHistory", JOB_NAME);
                Properties properties = new Properties();
                properties.setProperty(AbstractJobHistoryAwareTask.JobHistoryContext.HISTORY_ID.name(), history.getIdHistory().toString());
                localInvoker.invoke(BackgroundJobsController.class, "executeJobAsync", JOB_NAME, properties, 10000);
                return new RpcRes_Base<>(history, false, "");
            }
        } catch (Exception e) {
            return new RpcRes_Base<>(null, true, "Ошибка при запуске задачи: " + e.getMessage());
        }
    }
}
