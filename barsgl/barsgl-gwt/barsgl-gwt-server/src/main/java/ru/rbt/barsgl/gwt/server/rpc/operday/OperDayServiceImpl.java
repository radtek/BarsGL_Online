package ru.rbt.barsgl.gwt.server.rpc.operday;

import ru.rbt.barsgl.ejb.common.controller.od.COB_OK_Controller;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.cob.CobStatService;
import ru.rbt.barsgl.ejb.controller.operday.PdModeController;
import ru.rbt.barsgl.ejb.controller.operday.task.*;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.ejb.integr.dict.LwdBalanceCutController;
import ru.rbt.barsgl.shared.operday.LwdBalanceCutWrapper;
import ru.rbt.tasks.ejb.job.BackgroundJobsController;
import ru.rbt.tasks.ejb.repository.JobHistoryRepository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.mapping.job.TimerJob;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.gwt.core.server.rpc.RpcResProcessor;
import ru.rbt.shared.ExceptionUtils;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.cob.CobWrapper;
import ru.rbt.barsgl.shared.enums.ProcessingStatus;
import ru.rbt.barsgl.shared.jobs.TimerJobHistoryWrapper;
import ru.rbt.barsgl.shared.operday.COB_OKWrapper;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;

import javax.persistence.PersistenceException;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.util.Properties;

import static ru.rbt.barsgl.ejb.controller.cob.CobStatService.COB_FAKE_NAME;
import static ru.rbt.barsgl.ejb.controller.cob.CobStatService.COB_TASK_NAME;
import ru.rbt.security.gwt.server.rpc.operday.info.OperDayInfoServiceImpl;

/**
 * Created by akichigi on 23.03.15.
 */
public class OperDayServiceImpl extends OperDayInfoServiceImpl implements OperDayService{
    private static final String COB_NAME_LIKE = "ExecutePreCOBTask";
    private static final int COB_DELAY_SEC = 3;
        
    @Override
    protected void additionalAction(OperDayWrapper wrapper) throws Exception {
        boolean isAlreadyRunning = localInvoker.invoke(JobHistoryRepository.class, "isAlreadyRunningLike", new Object[]{null, "ExecutePreCOBTask"});
//        if (!isAlreadyRunning){
//            COB_OKWrapper cobOkWrapper = localInvoker.invoke(COB_OK_Controller.class, "getData");
//            wrapper.setCobOkWrapper(cobOkWrapper);
//        }
        wrapper.setIsCOBRunning(isAlreadyRunning);
    }

    @Override
    public RpcRes_Base<LwdBalanceCutWrapper> setLwdBalanceCut(LwdBalanceCutWrapper wrapper) throws Exception {
        return new RpcResProcessor<LwdBalanceCutWrapper>() {
            @Override
            public RpcRes_Base<LwdBalanceCutWrapper> buildResponse() throws Throwable {
                return localInvoker.invoke(LwdBalanceCutController.class, "create", wrapper);
            }
        }.process();
    }

    @Override
    public RpcRes_Base<LwdBalanceCutWrapper> getLwdBalanceCut() throws Exception {
        return new RpcResProcessor<LwdBalanceCutWrapper>() {
            @Override
            public RpcRes_Base<LwdBalanceCutWrapper> buildResponse() throws Throwable {
                return localInvoker.invoke(LwdBalanceCutController.class, "get");
            }
        }.process();
    }

    @Override
    public RpcRes_Base<COB_OKWrapper> getCOB_OK() throws Exception {
        return new RpcResProcessor<COB_OKWrapper>() {
            @Override
            protected RpcRes_Base<COB_OKWrapper> buildResponse() throws Throwable {
                COB_OKWrapper cobOkWrapper = new COB_OKWrapper();
                boolean isAlreadyRunning = localInvoker.invoke(JobHistoryRepository.class, "isAlreadyRunningLike", new Object[]{null, "ExecutePreCOBTask"});
                if (!isAlreadyRunning){
                    cobOkWrapper = localInvoker.invoke(COB_OK_Controller.class, "getData");
                }
                return new RpcRes_Base<>(cobOkWrapper, false, "");
            }
        }.process();
    }
    
    @Override
    public RpcRes_Base<ProcessingStatus> getProcessingStatus() throws Exception {
        return new RpcResProcessor<ProcessingStatus>() {
            @Override
            protected RpcRes_Base<ProcessingStatus> buildResponse() throws Throwable {
                // получаем
                ProcessingStatus prc = localInvoker.invoke(OperdayController.class, "getProcessingStatus");
                if (prc == null) throw new Throwable("Не найдена информация по операционному дню'.");

                return new RpcRes_Base<>(prc, false, "");
            }
        }.process();
    }

    @Override
    public RpcRes_Base<Boolean> runCloseLastWorkdayBalanceTask() throws Exception{
        return new RpcResProcessor<Boolean>() {
            @Override
            protected RpcRes_Base<Boolean> buildResponse() throws Throwable {
                if (!isPreCOBAllowed()) {
                    throw new RuntimeException("Флаг мониторинга в недопустимом для закрытия дня статусе." +
                            "\n Вероятно, обработка проводок еще не закончена");
                }

                return localInvoker.invoke(CloseLwdBalanceCutTask.class, "closeBalance", true, true);
            }
        }.process();
    }

    @Override
    public RpcRes_Base<Boolean> runOpenOperdayTask() throws Exception {
        return new RpcResProcessor<Boolean>() {
            @Override
            protected RpcRes_Base<Boolean> buildResponse() throws Throwable {
                return runTask(OpenOperdayTask.class.getSimpleName(), "Открытие операционного дня");
            }
        }.process();
    }

    @Override
    public RpcRes_Base<OperDayWrapper> swithPdMode() throws Exception {
        return new RpcResProcessor<OperDayWrapper>(){
            @Override
            protected RpcRes_Base<OperDayWrapper> buildResponse() throws Throwable {
                Operday od = localInvoker.invoke(OperdayController.class, "getOperday");
                localInvoker.invoke(PdModeController.class, "swithPdMode", od.getPdMode());
                return getOperDay();
            }
        }.process();
    }

    private RpcRes_Base<Boolean> runTask(String className, String errorMessage) throws Exception {
        localInvoker.invoke(BackgroundJobsController.class, "refreshJobsStatus");

        TimerJob job = localInvoker.invoke(BackgroundJobsController.class, "getJob", className);
        if (job == null) {
            throw new RuntimeException(Utils.Fmt("Не найдено задание '{0}'.", errorMessage));
        }
        if (job.getState() == TimerJob.JobState.STARTED)
            throw new RuntimeException(Utils.Fmt("Задание '{0}' уже запущено.", errorMessage));

        localInvoker.invoke(BackgroundJobsController.class, "executeJob", job);

        return new RpcRes_Base<>(true, false, "");
    }

    private boolean isPreCOBAllowed() throws Exception {
        return (Boolean) localInvoker.invoke(ExecutePreCOBTaskNew.class, "checkPackagesToloadExists");
    }

    public RpcRes_Base<CobWrapper> getCobInfo(Long idCob) throws Exception {
        return new RpcResProcessor<CobWrapper>() {
            @Override
            protected RpcRes_Base<CobWrapper> buildResponse() throws Throwable {
                RpcRes_Base<CobWrapper> res = localInvoker.invoke(CobStatService.class, "getCobInfo", idCob);
                if (res == null) throw new Throwable("Не удалось получить данные мониторинга COB!");
                return res;
            }
        }.process();
    }

    public RpcRes_Base<CobWrapper> calculateCob() throws Exception {
        return new RpcResProcessor<CobWrapper>() {
            @Override
            protected RpcRes_Base<CobWrapper> buildResponse() throws Throwable {
                RpcRes_Base<CobWrapper> res = localInvoker.invoke(CobStatService.class, "calculateCob");
                if (res == null) throw new Throwable("Не удалось рассчитать данные мониторинга COB!");
                return res;
            }
        }.process();
    }

    @Override
    public RpcRes_Base<TimerJobHistoryWrapper> runExecutePreCOBTask() throws Exception{
/*
        return new RpcResProcessor<Boolean>() {
            @Override
            protected RpcRes_Base<Boolean> buildResponse() throws Throwable {
                if (!isPreCOBAllowed()) {
                    throw new RuntimeException("Флаг мониторинга в недопустимом для закрытия дня статусе." +
                            "\n Вероятно, обработка проводок еще не закончена");
                }
                return runTask(ExecutePreCOBTask.class.getSimpleName(), "Перевод опердня в состояние PRE_COB");
            }
        }.process();
*/

        try {
            boolean isAlreadyRunning = localInvoker.invoke(JobHistoryRepository.class, "isAlreadyRunningLike", new Object[]{null, COB_NAME_LIKE});
            if (isAlreadyRunning) {
                return new RpcRes_Base<>(null, true, "Есть незаконченная задача СОВ");
            }
            Properties properties = new Properties();
            RpcRes_Base<Boolean> enableRun = localInvoker.invoke(ExecutePreCOBTaskNew.class, "checkEnableRun", COB_TASK_NAME, properties);
            if (!enableRun.getResult()) {
                return new RpcRes_Base<>(null, true, enableRun.getMessage());
            }
            else {
                TimerJobHistoryWrapper history = localInvoker.invoke(BackgroundJobsController.class, "createTimerJobHistory", COB_TASK_NAME);
                properties.setProperty(AbstractJobHistoryAwareTask.JobHistoryContext.HISTORY_ID.name(), history.getIdHistory().toString());
                localInvoker.invoke(BackgroundJobsController.class, "executeJobAsync", COB_TASK_NAME, properties, COB_DELAY_SEC * 1000);
                return new RpcRes_Base<>(history, false, "Задача СОВ запустится через " + COB_DELAY_SEC + " сек");
            }
        } catch (Exception e) {
            return new RpcRes_Base<>(null, true, "Ошибка при запуске задачи: " + getErrorMessage(e));
        }
    }

    /*для отладки интерфейса*/
    @Override
    public RpcRes_Base<TimerJobHistoryWrapper> runExecuteFakeCOBTask() throws Exception{
        try {
            boolean isAlreadyRunning = localInvoker.invoke(JobHistoryRepository.class, "isAlreadyRunningLike", new Object[]{null, COB_NAME_LIKE});
            if (isAlreadyRunning) {
                return new RpcRes_Base<>(null, true, "Есть незаконченная задача СОВ");
            } else {
                TimerJobHistoryWrapper history = localInvoker.invoke(BackgroundJobsController.class, "createTimerJobHistory", COB_FAKE_NAME);
                Properties properties = new Properties();
                properties.setProperty(AbstractJobHistoryAwareTask.JobHistoryContext.HISTORY_ID.name(), history.getIdHistory().toString());
                localInvoker.invoke(BackgroundJobsController.class, "executeJobAsync", COB_FAKE_NAME, properties, COB_DELAY_SEC * 1000);
                return new RpcRes_Base<>(history, false, "Задача эмуляции СОВ запустится через " + COB_DELAY_SEC + " сек");
            }
        } catch (Exception e) {
            return new RpcRes_Base<>(null, true, "Ошибка при запуске задачи: " + getErrorMessage(e));
        }
    }

    public String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable,
                ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class,
                IllegalArgumentException.class, PersistenceException.class, DefaultApplicationException.class);
    }

    @Override
    public RpcRes_Base<Boolean> switchAccessMode(OperDayWrapper wrapper) throws Exception {
        return new RpcResProcessor<Boolean>(){

            @Override
            protected RpcRes_Base<Boolean> buildResponse() throws Throwable {
                localInvoker.invoke(OperdayController.class, "refresh");
                boolean res = localInvoker.invoke(OperdayController.class, "swithAccessMode", wrapper.getAccessMode());
                if (!res) throw new Throwable("Ошибка изменения доступа");
                return new RpcRes_Base<>(res, false, "");
            }
        }.process();
    }


}
