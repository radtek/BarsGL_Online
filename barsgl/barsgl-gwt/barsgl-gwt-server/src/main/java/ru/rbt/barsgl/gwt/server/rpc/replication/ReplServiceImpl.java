package ru.rbt.barsgl.gwt.server.rpc.replication;

import ru.rbt.barsgl.ejb.controller.operday.task.ReplicateManualTask;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.gwt.core.server.rpc.AbstractGwtService;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.jobs.TimerJobHistoryWrapper;
import ru.rbt.tasks.ejb.job.BackgroundJobsController;
import ru.rbt.tasks.ejb.repository.JobHistoryRepository;

import java.util.Properties;

/**
 * Created by er17503 on 13.06.2017.
 */
public class ReplServiceImpl extends AbstractGwtService implements ReplService {
    private static final int JOB_DELAY_SEC = 1; //1 sec
    private static final String JOB_NAME = ReplicateManualTask.class.getSimpleName();

    @Override
    public RpcRes_Base<TimerJobHistoryWrapper> Test() throws Exception {
        try {
            boolean isAlreadyRunning = localInvoker.invoke(JobHistoryRepository.class, "isAlreadyRunningLike", new Object[]{null, JOB_NAME});
            if (isAlreadyRunning) {
                return new RpcRes_Base<>(null, true, "Есть незаконченная задача репликации " + JOB_NAME);
            } else {
                TimerJobHistoryWrapper history = localInvoker.invoke(BackgroundJobsController.class, "createTimerJobHistory", JOB_NAME);
                Properties properties = new Properties();
                properties.setProperty(AbstractJobHistoryAwareTask.JobHistoryContext.HISTORY_ID.name(), history.getIdHistory().toString());
                localInvoker.invoke(BackgroundJobsController.class, "executeJobAsync", JOB_NAME, properties, JOB_DELAY_SEC  * 1000);
                return new RpcRes_Base<>(history, false, "Задача репликации запустится через " + JOB_DELAY_SEC + " сек");
            }
        } catch (Exception e) {
            return new RpcRes_Base<>(null, true, "Ошибка при запуске задачи: " + e.getMessage());
        }
    }
}
