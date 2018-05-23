package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.BackvalueJournalController;
import ru.rbt.barsgl.ejb.controller.lg.LongRunningStepWork;
import ru.rbt.barsgl.ejb.controller.lg.LongRunningTaskController;
import ru.rbt.barsgl.ejb.controller.od.OperdaySynchronizationController;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.ejb.entity.lg.LongRunningPatternStepEnum;
import ru.rbt.barsgl.ejb.entity.lg.LongRunningTaskStep;
import ru.rbt.barsgl.ejb.repository.WorkprocRepository;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.shared.enums.BalanceMode;
import ru.rbt.barsgl.shared.enums.ProcessingStatus;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.BufferModeSync;
import static ru.rbt.audit.entity.AuditRecord.LogCode.BufferModeSyncTask;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.BUFFER;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.DIRECT;
import static ru.rbt.barsgl.shared.enums.BalanceMode.NOCHANGE;

/**
 * Created by Ivan Sevastyanov on 25.03.2016.
 */
public class PdSyncTask extends AbstractJobHistoryAwareTask {

    public static final String WAIT_STEP_FOR_DIRECT_KEY = "waitStepForDirect";
    public static final String WAIT_STEP_MESSAGE_FOR_DIRECT_KEY = "waitStepMessageForDirect";

    @EJB
    private OperdaySynchronizationController synchronizationController;

    @EJB
    private CoreRepository coreRepository;

//    @EJB
//    private AuditController auditController;
//
//    @EJB
//    private OperdayController operdayController;

    @Inject
    private WorkprocRepository workprocRepository;

    @EJB
    private BackvalueJournalController journalController;

    @Inject
    private LongRunningTaskController taskController;

    public enum PdSyncTaskContext {
        OPERDAY
    }

    @Override
    public boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        final Operday operday = (Operday) properties.get(PdSyncTaskContext.OPERDAY);
        auditController.info(BufferModeSyncTask, format("Режим ввода проводок '%s'. Начало синхронизации проводок", BUFFER));
        final boolean isWasProcessingAllowed = operdayController.isProcessingAllowed();

        List<LongRunningStepWork> works = new ArrayList<>();

        works.add(new LongRunningStepWork(LongRunningPatternStepEnum.SYNC_CHECK_RUN, () -> {
            try {
                Operday.PdMode mode = operdayController.getOperday().getPdMode();
                Assert.isTrue(BUFFER == mode
                        , () -> new ValidationError(ErrorCode.TASK_ERROR, format("Режим ввода проводок '%s' ожидалось '%s'", mode, BUFFER)));

                JobHistory history = getAlreadyRunningOne(properties);
                Assert.isTrue(history == null, () -> new ValidationError(ErrorCode.TASK_ERROR
                        , format("Задача синхронизации '%s' уже запущена c ИД '%s'", history.getJobName(), history.getId())));
                return true;
            } catch (ValidationError e) {
                auditController.warning(BufferModeSyncTask, "Невозможно запустить задачу синхронизации полупроводок", null, e);
                return false;
            }
        }));
        works.add(new LongRunningStepWork(LongRunningPatternStepEnum.SYNC_WAIT_STOPPING, () -> {
            if (!synchronizationController.waitStopProcessing()) {
                auditController.error(BufferModeSyncTask
                        , "Не удалось остановить обработку проводок. Синхронизация прервана", null, "");
                return false;
            }
            return true;
        }));

        works.add(new LongRunningStepWork(LongRunningPatternStepEnum.SYNC_GLPD, () -> proceedSyncronization(properties)));

        works.add(new LongRunningStepWork(LongRunningPatternStepEnum.SYNC_MOVE_GLPDARCH, () -> {
            try {
                auditController.info(BufferModeSyncTask, format("Перенесено проводок из буфера в архив: %s"
                        , coreRepository.executeInNewTransaction(pers -> synchronizationController.moveGLPdsToHistory(operday.getCurrentDate()))));
                return true;
            } catch (Exception e) {
                auditController.error(BufferModeSyncTask, "Не удалось перенести проводки в архив", null, e);
                return false;
            }
        }));

        works.add(new LongRunningStepWork(LongRunningPatternStepEnum.SYNC_SWITCH_PDMODE, () -> {
            try {
                Operday.PdMode taskPdMode = getTargetPdMode(properties);
                if (null != taskPdMode && taskPdMode != operday.getPdMode()) {
                    auditController.info(BufferModeSyncTask
                            , format("Текущий режим обработки проводок '%s' не соотв вычисленному '%s', переключаем"
                                    , operday.getPdMode(), taskPdMode));
                    operdayController.swithPdMode(operday.getPdMode());
                } else {
                    auditController.info(BufferModeSyncTask
                            , format("Текущий режим обработки проводок '%s' соответствует вычисленному '%s', оставляем как было"
                                    , operday.getPdMode(), taskPdMode));
                }
                return true;
            } catch (Exception e) {
                auditController.error(BufferModeSyncTask, "Не удалось переключить режим загрузки", null, e);
                return false;
            }
        }));
        works.add(new LongRunningStepWork(LongRunningPatternStepEnum.SYNC_RECALC, () -> {
            try {
                journalController.recalculateBackvalueJournal();
            } catch (Throwable e) {
                auditController.error(BufferModeSyncTask, "Ошибка при пересчете локализации", null, e);
            }
            return true;
        }));
        works.add(new LongRunningStepWork(LongRunningPatternStepEnum.SYNC_ALLOW_PROCESSING, () -> {
            if (isWasProcessingAllowed) {
                try {
                    operdayController.setProcessingStatus(ProcessingStatus.ALLOWED);
                } catch (Throwable e) {
                    auditController.warning(BufferModeSyncTask, "Ошибка при разрешении обработки", null, e);
                }
            }
            return true;
        }));
        for (LongRunningStepWork work : works) {
            LongRunningTaskStep step = taskController.executeWithLongRunningStep(jobHistory, work.getStep(), work.getWork());
            if (null == step){
                auditController.error(BufferModeSyncTask,
                        format("Не удалось создать шаг выполнения для '%s'", work), null, new DefaultApplicationException(""));
            } else
            if (!step.isSuccess()) {
                auditController.error(BufferModeSyncTask,
                        format("Сбой синхронизация проводок. Шаг '%s'. Процесс остановлен", step), null, new DefaultApplicationException(""));
                return false;
            }
        }
        auditController.info(BufferModeSyncTask, "Синхронизация проводок завершена успешно");
        return true;
    }

    @Override
    public boolean checkRun(String jobName, Properties properties) throws Exception {
        // проверки в теле задачи
        return true;
    }

    @Override
    protected boolean checkJobStatus(String jobName, Properties properties) {
        // проверки в теле задачи
        return true;
    }

    private boolean proceedSyncronization(Properties properties) {
        try {
            return (boolean) coreRepository.executeInNewTransaction(persistence -> {
                synchronizationController.syncPostings(getTargetBalanceMode(properties));
                DataRecord stats = synchronizationController.getBifferStatistic();
                Assert.isTrue(stats.getLong("pd_cnt") == 0, () -> new DefaultApplicationException("Остались полупроводки в буфере после синхронизации"));
                Assert.isTrue(stats.getLong("bal_cnt") == 0, () -> new DefaultApplicationException("Остались обороты в буфере после синхронизации"));
                return true;
            });
        } catch (Throwable e) {
            auditController.error(BufferModeSyncTask, "Ошибка синхронизации проводок", null, e);
            return false;
        }
    }

    private Operday.PdMode getTargetPdMode(Properties properties) throws SQLException {
        Operday operday = (Operday) properties.get(PdSyncTaskContext.OPERDAY);

        String pdModeString = properties.getProperty(OpenOperdayTask.PD_MODE_KEY);
        if (!StringUtils.isEmpty(pdModeString)) {
            return Operday.PdMode.valueOf(pdModeString);
        } else {
            String waitStepForDirect = Optional.ofNullable(properties.getProperty(WAIT_STEP_FOR_DIRECT_KEY)).orElse("IFLEX");
            DataRecord waitWorkprocRecord = workprocRepository.getWorkprocRecord(waitStepForDirect, operday.getLastWorkingDay());
            String waitStepMessageForDirect = Optional.ofNullable(properties.getProperty(WAIT_STEP_MESSAGE_FOR_DIRECT_KEY)).orElse("MI5GL");

            if (null != waitWorkprocRecord && "O".equals(waitWorkprocRecord.getString("RESULT"))
                        && waitStepMessageForDirect.equals(waitWorkprocRecord.getString("MSG"))) {
                return DIRECT;
            } else {
                return BUFFER;
            }
        }
    }

    /**
     * ru.rbt.barsgl.ejb.common.mapping.od.Operday.BalanceMode#NOCHANGE by default
     * @param properties set ru.rbt.barsgl.ejb.controller.operday.task.OpenOperdayTask#BALANCE_MODE_KEY for target mode
     * @return balance mode
     */
    public BalanceMode getTargetBalanceMode(Properties properties) {
        BalanceMode balanceMode = Optional.ofNullable(properties.getProperty(OpenOperdayTask.BALANCE_MODE_KEY))
                .map(BalanceMode::valueOf).orElse(NOCHANGE);
        auditController.info(BufferModeSync, String.format("Целевой режим обработки проводок: %s", balanceMode.name()));
        return balanceMode;
    }

    @Override
    protected void initExec(String jobName, Properties properties) {
        properties.put(PdSyncTaskContext.OPERDAY, operdayController.getOperday());
    }

    /**
     * если есть запущенные но текущая задача
     * @param properties
     * @return
     */
    private JobHistory getAlreadyRunningOne(Properties properties) {
//        final JobHistory history = (JobHistory) properties.get(JobHistoryContext.HISTORY);
        final JobHistory history = getPreinstlledJobHistory(properties);
        return jobHistoryRepository.getAlreadyRunningLike(history.getId(), PdSyncTask.class.getSimpleName());
    }
}
