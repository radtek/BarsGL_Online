package ru.rbt.barsgl.ejb.controller.operday.task.stamt;

import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.BackvalueJournalController;
import ru.rbt.barsgl.ejb.controller.od.OperdaySynchronizationController;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.ejb.repository.WorkprocRepository;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.Optional;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.BufferModeSyncBackvalue;
import static ru.rbt.audit.entity.AuditRecord.LogCode.BufferModeSyncBackvalue2;
import static ru.rbt.ejbcore.validation.ErrorCode.IS_NOT_ENOUTH_DATA;
import static ru.rbt.ejbcore.validation.ErrorCode.STAMT_UNLOAD_AFTER_FINAL_STEP;

/**
 * Created by Ivan Sevastyanov on 27.04.2018.
 * Задача сброса проводок бэквалуе из буфера выгрузки их в STAMT
 * без наличия шагов загрузки WTXXX по окончании загрузки (конечный шаг загрузчика)
 */
public class SyncStamtBackvalueTaskP2 extends AbstractJobHistoryAwareTask {

    public final static String FINAL_WORKPROC_STEP_NAME_KEY = "finalStepName";


    @Inject
    private StamtUnloadController unloadController;

    @Inject
    private WorkprocRepository workprocRepository;

    @Inject
    private SyncStamtBackvalueTask originalUnloadTask;

    @EJB
    private OperdaySynchronizationController synchronizationController;

    @EJB
    private BackvalueJournalController backvalueJournalController;

    @Inject
    private StamtUnloadPstIncrementTask incrementTask;

    @Override
    protected boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        try {
            final Operday operday = operdayController.getOperday();
            Assert.isTrue(synchronizationController.waitStopProcessing(), () -> new ValidationError(ErrorCode.TERM_TIMEOUT, jobHistory.getJobName()));
            jobHistoryRepository.executeInNewTransaction(persistence -> {
                synchronizationController.syncBackvaluePostings(operday.getCurrentDate());
                synchronizationController.restartSequencePD(synchronizationController.getMaxPdId(100L));
                return null;
            });
            auditController.info(BufferModeSyncBackvalue2, format("Пересчитана локализация по счетам '%s'"
                    , backvalueJournalController.recalculateLocalIncrementBuffer()));
            auditController.info(BufferModeSyncBackvalue2, format("Переренсено в историю полупроводок: '%s'"
                    , (int)jobHistoryRepository.executeInNewTransaction(pers
                            -> synchronizationController.moveGLPdsToHistory(operday.getCurrentDate()))));

            auditController.info(BufferModeSyncBackvalue, format("Разрешение обработки '%s'", originalUnloadTask.allowAccess()));

            Assert.isTrue(incrementTask.checkRun(operday.getCurrentDate(), new Properties())
                    , () -> new ValidationError(STAMT_UNLOAD_AFTER_FINAL_STEP, format("Не прошла проверка возм-ти выполнен инкр. выгрузки ОД %s"
                            , dateUtils.onlyDateString(operday.getCurrentDate()))));
            final String incrJobName = jobHistory.getJobName() + "_incr";
            auditController.info(BufferModeSyncBackvalue, format("Запуск задачи %s инкрементальной выгрузки в STAMT после синхронизации", incrJobName));
            incrementTask.run(incrJobName, new Properties());
            return true;
        } catch (Throwable e) {
            auditController.warning(BufferModeSyncBackvalue2, format("Ошибка при выгрузке бэквалуе в STAMT по окончании шага загрузки '%s'", getFinalStepName(properties)), null, e);
            return false;
        }
    }

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        final String finalStepName = getFinalStepName(properties);
        try {
            final Operday operday = operdayController.getOperday();
            Assert.isTrue(workprocRepository.isStepOK(finalStepName, operday.getLastWorkingDay())
                            , () -> new ValidationError(STAMT_UNLOAD_AFTER_FINAL_STEP
                            , format("Периодическая инкрементальная выгрузка в STAMT невозможна. Не закончен ожидаемый шаг загрузки '%s'"
                            , finalStepName)));
            return originalUnloadTask.checkExecSync(STAMT_UNLOAD_AFTER_FINAL_STEP, operday) && unloadController.checkConsumed();
        } catch (Throwable e) {
            auditController.warning(BufferModeSyncBackvalue2, format("Невозможно выгрузить бэквалуе в стамт по окончании шага загрузки '%s'", finalStepName), null, e);
            return false;
        }
    }

    @Override
    protected boolean checkJobStatus(String jobName, Properties properties) {
        return checkAlreadyRunning(jobName, properties);
    }

    @Override
    protected void initExec(String jobName, Properties properties) {

    }

    private String getFinalStepName(Properties properties) {
        return Optional.ofNullable(properties.getProperty(FINAL_WORKPROC_STEP_NAME_KEY))
                .orElseThrow(() -> new ValidationError(IS_NOT_ENOUTH_DATA, format("Не определено значение для параметра '%s' окончательный шаг загрузки", FINAL_WORKPROC_STEP_NAME_KEY)));
    }
}
