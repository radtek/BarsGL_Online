package ru.rbt.barsgl.gwt.server.rpc.operday;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.PdModeController;
import ru.rbt.barsgl.ejb.controller.operday.task.CloseLastWorkdayBalanceTask;
import ru.rbt.barsgl.ejb.controller.operday.task.ExecutePreCOBTask;
import ru.rbt.barsgl.ejb.controller.operday.task.OpenOperdayTask;
import ru.rbt.barsgl.ejb.job.BackgroundJobsController;
import ru.rbt.barsgl.ejbcore.mapping.job.TimerJob;
import ru.rbt.security.gwt.server.rpc.operday.info.OperDayInfoServiceImpl;
import ru.rbt.barsgl.gwt.core.server.rpc.RpcResProcessor;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.enums.OperDayButtons;
import ru.rbt.barsgl.shared.enums.ProcessingStatus;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;

import java.text.SimpleDateFormat;

/**
 * Created by akichigi on 23.03.15.
 */
public class OperDayServiceImpl extends OperDayInfoServiceImpl implements OperDayService{

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
                return runTask(CloseLastWorkdayBalanceTask.class.getSimpleName(), "Закрытие баланса предыдущего операционного дня");
            }
        }.process();
    }

    @Override
    public RpcRes_Base<Boolean> runExecutePreCOBTask() throws Exception{
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
        return (Boolean) localInvoker.invoke(ExecutePreCOBTask.class, "checkPackagesToloadExists");
    }

}
