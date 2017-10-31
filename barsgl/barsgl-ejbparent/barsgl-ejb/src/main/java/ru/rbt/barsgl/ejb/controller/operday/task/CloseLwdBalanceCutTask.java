package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.cob.CobStepResult;
import ru.rbt.barsgl.ejb.controller.od.OperdaySynchronizationController;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.ejb.entity.dict.LwdBalanceCutView;
import ru.rbt.barsgl.ejb.integr.bg.BackValueOperationController;
import ru.rbt.barsgl.ejb.integr.bg.EtlPostingController;
import ru.rbt.barsgl.ejb.repository.dict.LwdBalanceCutRepository;
import ru.rbt.barsgl.ejb.repository.dict.LwdCutCachedRepository;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.CobStepStatus;
import ru.rbt.barsgl.shared.enums.ProcessingStatus;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.shared.Assert;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import javax.ejb.*;
import javax.inject.Inject;
import java.util.Date;
import java.util.Properties;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MINUTES;
import static ru.rbt.audit.entity.AuditRecord.LogCode.Operday;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;

/**
 * Created by er18837 on 07.08.2017.
 */
@Singleton
@AccessTimeout(value = 5, unit = MINUTES)
public class CloseLwdBalanceCutTask extends AbstractJobHistoryAwareTask {

    final private String myTaskName = CloseLwdBalanceCutTask.class.getSimpleName();

    @Inject
    private OperdayController operdayController;

    @Inject
    private EtlPostingController etlPostingController;

    @Inject
    private BackValueOperationController bvPostingController;

    @Inject
    private LwdCutCachedRepository lwdCutCachedRepository;

    @EJB
    private OperdaySynchronizationController synchronizationController;

    @EJB
    private LwdBalanceCutRepository lwdBalanceCutRepository;

    @Inject
    DateUtils dateUtils;

    @Override
    public boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        LwdBalanceCutView cutRecord = lwdCutCachedRepository.getRecord();
        auditController.info(AuditRecord.LogCode.Operday, String.format("Задача '%s': Требуется автоматическое закрытие БАЛАНСА предыдущего дня: дата '%s', время '%s'",
                myTaskName, dateUtils.onlyDateString(cutRecord.getRunDate()), cutRecord.getCutTime()));

        Operday operday = operdayController.getOperday();
        final Date currentDateTime = operdayController.getSystemDateTime();
        final Date currentDate = DateUtils.onlyDate(currentDateTime);
        final String curDateStr = dateUtils.onlyDateString(operday.getCurrentDate());
        final String prevDateStr = dateUtils.onlyDateString(operday.getLastWorkingDay());
        if (OPEN == operday.getLastWorkdayStatus()) {
            try {
                return lwdBalanceCutRepository.executeInNewTransaction(persistence -> {
                    if (closeBalance(true).getResult()) {
                        lwdBalanceCutRepository.updateCloseTimestamp(currentDate, currentDateTime);
                        lwdCutCachedRepository.flushCache();
                        return true;
                    } else
                        return false;
                });
            } catch (Throwable e) {
                auditController.error(Operday, format("Ошибка закрытия БАЛАНСА предыдущего ОД '%s'. Текущий ОД '%s'"
                        , prevDateStr, curDateStr), null, e);
                return false;
            }
        } else {
            auditController.info(Operday, format("БАЛАНС предыдущего ОД '%s' уже был закрыт. Текущий ОД '%s'"
                    , prevDateStr, curDateStr));
            try {
                lwdBalanceCutRepository.executeInNewTransaction(persistence -> {
                    lwdBalanceCutRepository.updateCloseTimestamp(currentDate, currentDateTime);
                    lwdCutCachedRepository.flushCache();
                    return true;
                });
            } catch (Throwable e) {
                auditController.error(Operday, format("Ошибка закрытия БАЛАНСА предыдущего ОД '%s'. Текущий ОД '%s'"
                        , prevDateStr, curDateStr), null, e);
                return false;
            }
        }
        auditController.info(AuditRecord.LogCode.Operday, format("Успешное завершение залачи '%s'", myTaskName));
        return true;
    }

    // проверяет запуск конкурирующих задач
    public RpcRes_Base<Boolean> checkNotRunOther(Date operday, Class myTask, Class ... otherTasks) {
        for (Class other : otherTasks) {
            if (jobHistoryRepository.isAlreadyRunning(other.getSimpleName(), operday)) {
                String msg = format("Нельзя запустить задачу '%s': выполняется задача '%s'", myTask.getSimpleName(), other.getSimpleName());
                auditController.warning(Operday, msg);
                return new RpcRes_Base<>(false, true, msg);
            }
        }
        return new RpcRes_Base<>(true, false, "");
    }

    // проверить запуск PdSync u PreCOB
    private RpcRes_Base<Boolean> checkNotRunOther(Date operdate) {
        return checkNotRunOther(operdate, this.getClass(), PdSyncTask.class, ExecutePreCOBTaskNew.class);
    }

    @Override
    public boolean checkRun(String jobName, Properties properties) throws Exception {
        Operday operday = operdayController.getOperday();
        if (ONLINE != operday.getPhase()) {
            auditController.warning(Operday, format("Нельзя запустить задачу '%s': опердень в статусе '%s'"
                    , myTaskName, operday.getPhase().name() ));
            return false;
        }

        final Date currentDateTime = operdayController.getSystemDateTime();
        final Date currentDate = DateUtils.onlyDate(currentDateTime);

        LwdBalanceCutView cutRecord = lwdCutCachedRepository.getRecord();
        boolean needRun = null != cutRecord                                         // есть запись
                && null != cutRecord.getCutDateTime()                               // установлено время
                && currentDate.equals(cutRecord.getRunDate())                       // надо закрыть сегодня
                && !currentDateTime.before(cutRecord.getCutDateTime());             // пришло время!

        boolean notRunOther = checkNotRunOther(operday.getCurrentDate()).getResult();
        return needRun && notRunOther;
    }

    @Override
    protected void initExec(String jobName, Properties properties) {

    }

    // ======== перенесено из CloseLastWorkdayBalanceTask =========

    @Lock(LockType.WRITE)
    // измененный вариант CloseLastWorkdayBalanceTask.executeWork
    public RpcRes_Base<Boolean> closeBalance(boolean fromOnline) {
        final Operday operday = operdayController.getOperday();
        String curDateStr = dateUtils.onlyDateString(operday.getCurrentDate());
        String prevDateStr = dateUtils.onlyDateString(operday.getLastWorkingDay());
        try {
            auditController.info(AuditRecord.LogCode.Operday, format("Закрытие БАЛАНСА предыдущего ОД '%s'. Текущий ОД '%s'.", prevDateStr, curDateStr));

            checkOperdayStatus(operdayController.getOperday());

            boolean isWasProcessingAllowed = false;
            if (fromOnline) {
                // проверим, что не запущены конкурирующие задачи
                RpcRes_Base<Boolean> res = checkNotRunOther(operday.getCurrentDate());
                if (!res.getResult())
                    return res;
                isWasProcessingAllowed = operdayController.isProcessingAllowed();
                if (!synchronizationController.waitStopProcessing()) {
                    String msg = format("Не удалось остановить обработку проводок. Закрытие БАЛАНСА предыдущего ОД '%s' прервано. Текущий ОД '%s'", prevDateStr, curDateStr);
                    auditController.error(Operday, msg, null, "");
                    return new RpcRes_Base<>(false, true, msg);
                }
                reprocessErckStorno(operday.getLastWorkingDay(), operday.getCurrentDate());
            }

            operdayController.closeLastWorkdayBalance();

            if (isWasProcessingAllowed) {
                operdayController.setProcessingStatus(ProcessingStatus.ALLOWED);
            }

            String msg = format("Успешное закрытие БАЛАНСА предыдущего ОД '%s'. Текущий ОД '%s'", prevDateStr, curDateStr);
            auditController.info(AuditRecord.LogCode.Operday, msg);
            return new RpcRes_Base<>(true, false, msg);
        } catch (Throwable e) {
            String msg = format("Ошибка закрытия БАЛАНСА предыдущего ОД '%s'. Текущий ОД '%s'", prevDateStr, curDateStr);
            auditController.error(AuditRecord.LogCode.Operday, msg, null, e);
            return new RpcRes_Base<>(false, true, msg);
        }
    }

    private void checkOperdayStatus(Operday operday) {
        Assert.isTrue(OPEN == operday.getLastWorkdayStatus()
                , format("Баланс предыдущего операционного дня '%s' находится в статусе '%s'"
                        , dateUtils.onlyDateString(operday.getLastWorkingDay()), operday.getLastWorkdayStatus()));
    }

    public CobStepResult reprocessErckStorno(Date prevdate, Date curdate) throws Exception {
        int[] cntBvMnl = bvPostingController.reprocessErckStornoBvMnl(prevdate, curdate);
        int[] cntBvAuto = bvPostingController.reprocessErckStornoBvAuto(prevdate, curdate);
        int[] cntToday = etlPostingController.reprocessErckStornoToday(prevdate, curdate);
        int all = cntBvMnl[0] + cntBvAuto[0] + cntToday[0];
        if (all > 0) {
            return new CobStepResult(CobStepStatus.Success, format("Обработано СТОРНО операций BV_MANUAL: %d из %d, AUTOMATIC bv: %d из %d, AUTOMATIC не bv: %d из %d"
                    , cntBvMnl[1], cntBvMnl[0] , cntBvAuto[1], cntBvAuto[0], cntToday[1], cntToday[0]));
        } else {
            return new CobStepResult(CobStepStatus.Skipped, "Не найдено сторно операций для повторной обработки");
        }
    }

}
