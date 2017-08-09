package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.cob.CobStepResult;
import ru.rbt.barsgl.ejb.controller.od.OperdaySynchronizationController;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.ejb.entity.dict.LwdBalanceCut;
import ru.rbt.barsgl.ejb.entity.dict.LwdBalanceCutView;
import ru.rbt.barsgl.ejb.integr.bg.BackValueOperationController;
import ru.rbt.barsgl.ejb.integr.bg.EtlPostingController;
import ru.rbt.barsgl.ejb.repository.dict.LwdBalanceCutRepository;
import ru.rbt.barsgl.ejb.repository.dict.LwdCutCachedRepository;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.barsgl.shared.enums.CobStepStatus;
import ru.rbt.barsgl.shared.enums.ProcessingStatus;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.shared.Assert;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.Date;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.BufferModeSyncTask;
import static ru.rbt.audit.entity.AuditRecord.LogCode.Operday;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;

/**
 * Created by er18837 on 07.08.2017.
 */
public class CloseLwdBalanceCutTask extends AbstractJobHistoryAwareTask {

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

    @EJB
    private AuditController auditController;

    @Inject
    DateUtils dateUtils;

    @Override
    public boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        Operday operday = operdayController.getOperday();
        final Date currentDateTime = operdayController.getSystemDateTime();
        final Date currentDate = DateUtils.onlyDate(currentDateTime);
        if (OPEN == operday.getLastWorkdayStatus()) {
            try {
                return (boolean) lwdBalanceCutRepository.executeInNewTransaction(persistence -> {
                    // TODO остановить обработку
                    if (closeBalance(true)) {
                        lwdBalanceCutRepository.updateCloseTimestamp(currentDate, currentDateTime);
                        lwdCutCachedRepository.flushCache();
                        return true;
                    } else
                        return false;
                });
            } catch (Throwable e) {
                auditController.error(Operday, format("Ошибка закрытия БАЛАНСА предыдущего ОД из задачи принудительного закрытия баланса. Текущий ОД '%s'"
                        , dateUtils.onlyDateString(operday.getCurrentDate())), null, e);
                return false;
            }
        } else {
            auditController.info(Operday, "Баланс предыдущего дня уже был закрыт вне задачи принудительного закрытия");
            try {
                return (boolean) lwdBalanceCutRepository.executeInNewTransaction(persistence -> {
                    lwdBalanceCutRepository.updateCloseTimestamp(currentDate, currentDateTime);
                    lwdCutCachedRepository.flushCache();
                    return true;
                });
            } catch (Throwable e) {
                auditController.error(Operday, format("Ошибка закрытия БАЛАНСА предыдущего ОД из задачи принудительного закрытия баланса. Текущий ОД '%s'"
                        , dateUtils.onlyDateString(operday.getCurrentDate())), null, e);
                return false;
            }
        }
    }

    public boolean checkRunOther(Date operday, Class myTask, Class ... otherTasks) {
        for (Class other : otherTasks) {
            if (jobHistoryRepository.isAlreadyRunning(other.getSimpleName(), operday)) {
                auditController.warning(Operday, format("Нельзя запустить задачу '%s': выполняется задача '%s'"
                        , myTask.getSimpleName(), other.getSimpleName() ));
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        Operday operday = operdayController.getOperday();
        if (ONLINE != operday.getPhase()) {
            auditController.warning(Operday, format("Нельзя запустить задачу '%s': опердень в статусе '%s'"
                    , this.getClass().getSimpleName(), operday.getPhase().name() ));
            return false;
        }

        final Date currentDateTime = operdayController.getSystemDateTime();
        final Date currentDate = DateUtils.onlyDate(currentDateTime);

        LwdBalanceCutView cutRecord = lwdCutCachedRepository.getRecord();
        boolean needClose = null != cutRecord                               // есть запись
                && currentDate.equals(cutRecord.getRunDate())               // надо закрыть сегодня
                && !currentDateTime.before(cutRecord.getCutDateTime());     // пришло время!

        // проверить запуск PdSync u PreCOB
        boolean isRunOther = checkRunOther(operday.getCurrentDate(), this.getClass(), PdSyncTask.class, ExecutePreCOBTaskNew.class);
        return needClose && !isRunOther;
    }

    @Override
    protected void initExec(String jobName, Properties properties) {

    }

    // ======== перенесено из CloseLastWorkdayBalanceTask =========

    // был executeWork
    public boolean closeBalance(boolean withStorno) {
        final Operday operday = operdayController.getOperday();
        String curdate = dateUtils.onlyDateString(operday.getLastWorkingDay());
        String prevdate = dateUtils.onlyDateString(operday.getCurrentDate());
        try {
            final boolean isWasProcessingAllowed = operdayController.isProcessingAllowed();
            if (!synchronizationController.waitStopProcessing()) {
                auditController.error(Operday, format("Не удалось остановить обработку проводок. Закрытие баланса предыдущего ОД '%s' прервано. Текущий ОД '%s'"
                                , prevdate, curdate), null, "");
                return false;
            }
            auditController.info(AuditRecord.LogCode.Operday, format("Закрытие баланса предыдущего ОД '%s'. Текущий ОД '%s'."
                    , prevdate, curdate));

            checkOperdayStatus(operdayController.getOperday());

            if (withStorno) {
                etlPostingController.reprocessErckStorno(operday.getLastWorkingDay(), operday.getCurrentDate());
            }

            operdayController.closeLastWorkdayBalance();

            if (isWasProcessingAllowed) {
                operdayController.setProcessingStatus(ProcessingStatus.ALLOWED);
            }

            auditController.info(AuditRecord.LogCode.Operday, format("Успешное окончание закрытия баланса предыдущего ОД '%s'. Текущий ОД '%s'."
                    , prevdate, curdate));
            return true;
        } catch (Throwable e) {
            auditController.error(AuditRecord.LogCode.Operday, format("Ошибка закрытия баланса предыдущего ОД '%s'. Текущий ОД '%s'."
                    , prevdate, curdate), null, e);
            return false;
        }
    }

    private void checkOperdayStatus(Operday operday) {
        Assert.isTrue(OPEN == operday.getLastWorkdayStatus()
                , format("Баланс предыдущего операционного дня '%s' находится в статусе '%s'"
                        , dateUtils.onlyDateString(operday.getLastWorkingDay()), operday.getLastWorkdayStatus()));
    }

    public CobStepResult reprocessErckStorno(Date prevdate, Date curdate) throws Exception {
        int cntBv = bvPostingController.reprocessErckStornoMnl(prevdate, curdate);
        int cnt = bvPostingController.reprocessErckStornoAuto(prevdate, curdate);
        cnt += etlPostingController.reprocessErckStorno(prevdate, curdate);
        if (cnt > 0 || cntBv > 0) {
            return new CobStepResult(CobStepStatus.Success, format("Обработано СТОРНО операций AUTOMATIC: %d, BV_MANUAL: %d, ", cnt, cntBv));
        } else {
            return new CobStepResult(CobStepStatus.Skipped, "Не найдено сторно операций для повторной обработки");
        }
    }

}
