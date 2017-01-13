package ru.rbt.barsgl.ejb.controller.operday.task.stamt;

import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.BackvalueJournalController;
import ru.rbt.barsgl.ejb.controller.od.OperdaySynchronizationController;
import ru.rbt.barsgl.ejb.controller.operday.task.ExecutePreCOBTask;
import ru.rbt.barsgl.ejb.controller.operday.task.TaskUtils;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.ejb.entity.task.JobHistory;
import ru.rbt.barsgl.ejb.etc.TextResourceController;
import ru.rbt.barsgl.ejb.repository.WorkprocRepository;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.util.DateUtils;
import ru.rbt.barsgl.ejbcore.util.StringUtils;
import ru.rbt.barsgl.ejbcore.validation.ErrorCode;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.Assert;
import ru.rbt.barsgl.shared.ExceptionUtils;
import ru.rbt.barsgl.shared.enums.ProcessingStatus;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.COB;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.BUFFER;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.BufferModeSyncBackvalue;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.Operday;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.Task;
import static ru.rbt.barsgl.ejb.repository.WorkprocRepository.WorkprocState.E;
import static ru.rbt.barsgl.ejb.repository.WorkprocRepository.WorkprocState.O;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.isEmpty;
import static ru.rbt.barsgl.ejbcore.validation.ErrorCode.OPERDAY_TASK_ALREADY_EXC;
import static ru.rbt.barsgl.ejbcore.validation.ErrorCode.OPERDAY_TASK_ALREADY_RUN;
import static ru.rbt.barsgl.ejbcore.validation.ErrorCode.TASK_ERROR;

/**
 * Created by Ivan Sevastyanov on 26.09.2016.
 * промежуточная синхронизация backvalue с буфером, если день открыт в режиме BUFFER и инкрем выгрузка в STAMT
 */
public class SyncStamtBackvalueTask extends AbstractJobHistoryAwareTask {

    @Inject
    private WorkprocRepository workprocRepository;

    @Inject
    private TextResourceController textResourceController;

    @Inject
    private ExecutePreCOBTask preCOBTask;

    @EJB
    private OperdaySynchronizationController synchronizationController;

    @Inject
    private StamtUnloadPstIncrementTask incrementTask;

    @Inject
    private DateUtils dateUtils;

    @EJB
    private BackvalueJournalController backvalueJournalController;

    @EJB
    private CoreRepository coreRepository;

    @Override
    protected boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        final String jobName = jobHistory.getJobName();
        auditController.info(BufferModeSyncBackvalue
                , format("Запуск задачи инкрем.выгрузки задача '%s' после шага '%s'", jobName, getStepName(jobName, properties)));
        Operday operday = operdayController.getOperday();
        try {
            if (operday.getPhase() == ONLINE) {
                jobHistoryRepository.executeInNewTransaction(persistence1 -> workprocRepository
                        .updateWorkprocMessage(getStepName(jobName, properties), getWorkprocDate(operday), "STARTED"));
                if (checkExecSync(operday)) {
                    Assert.isTrue(synchronizationController.waitStopProcessing(), () -> new ValidationError(ErrorCode.TERM_TIMEOUT, jobName));
                    jobHistoryRepository.executeInNewTransaction(persistence -> {
                        synchronizationController.syncBackvaluePostings(operday.getCurrentDate());
                        synchronizationController.restartSequencePD(synchronizationController.getMaxPdId(100L));
                        return null;
                    });
                    auditController.info(BufferModeSyncBackvalue, format("Пересчитана локализация по счетам ''%s"
                            , backvalueJournalController.recalculateLocalIncrementBuffer()));
                    auditController.info(BufferModeSyncBackvalue, format("Переренсено в историю полупроводок: '%s'"
                            , (int)jobHistoryRepository.executeInNewTransaction(pers
                                    -> synchronizationController.moveGLPdsToHistory(operday.getCurrentDate()))));
                }
                auditController.info(BufferModeSyncBackvalue, format("Установлен флаг O на workproc '%s'"
                        , coreRepository.executeInNewTransaction( pers -> workprocRepository.updateWorkproc(getStepName(jobName, properties), getWorkprocDate(operday)
                        , O, format("Синхронизация по задаче %s выполнена", jobName)))));

                auditController.info(BufferModeSyncBackvalue, format("Разрешеие обработки '%s'", allowAccess()));

                Assert.isTrue(incrementTask.checkRun(operday.getCurrentDate(), new Properties())
                        , () -> new ValidationError(TASK_ERROR, format("Не прошла проверка возм-ти выполнен инкр. выгрузки ОД %s"
                                , dateUtils.onlyDateString(operday.getCurrentDate()))));
                final String incrJobName = jobName + "_incr";
                auditController.info(BufferModeSyncBackvalue, format("Запуск задачи %s инкрементальной выгрузки в STAMT после синхронизации", incrJobName));
                incrementTask.run(incrJobName, new Properties());
            } else if (operday.getPhase() == COB) {
                workprocRepository.updateWorkproc(getStepName(jobName, properties), getWorkprocDate(operday)
                        , O, format("Задача %s не выполнена опердень закрыт", jobName));
            } else {
                throw new ValidationError(TASK_ERROR, format("Операционный день в недопустимом статусе %s", operday.getPhase()));
            }
            auditController.info(BufferModeSyncBackvalue
                    , format("Окончание выполнения задачи инкрем.выгрузки задача '%s' после шага '%s'", jobName, getStepName(jobName, properties)));
            return true;
        } catch (Throwable e) {
            auditController.error(BufferModeSyncBackvalue
                    , "Ошибка при выполнении синхронизации/инкр.выгрузки проводок backvalue в STAMT", null, e);
            jobHistoryRepository.executeInNewTransaction(persistence ->
                    workprocRepository.updateWorkproc(getStepName(jobName, properties), getWorkprocDate(operday)
                    , E, format("Ошибка при вып. задачи %s: %s"
                            , jobName, StringUtils.substr(ExceptionUtils.getErrorMessage(e, ValidationError.class, DataTruncation.class
                                    , SQLException.class, DefaultApplicationException.class), 500))));
            return false;
        } finally {
            allowAccess();
        }

    }

    @Override
    protected boolean checkJobStatus(String jobName, Properties properties) {
        try {
            Assert.isTrue(!jobHistoryRepository.isAlreadyRunning(jobName, getOperday(properties))
                    , () -> new ValidationError(OPERDAY_TASK_ALREADY_RUN, jobName, dateUtils.onlyDateString(getOperday(properties))));
            return true;
        } catch (ValidationError e) {
            auditController.warning(Task, format("Задача %s не выполнена", jobName), null, e);
            return false;
        }
    }

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        try {
            if (TaskUtils.getCheckRun(properties, true)) {
                Assert.isTrue(workprocRepository.isWaitingStepPresent(getStepName(jobName, properties)
                            , getWorkprocDate(operdayController.getOperday()))
                    , () -> new ValidationError(ErrorCode.OPERDAY_LDR_STEP_ABSENT, getStepName(jobName, properties)
                                , dateUtils.onlyDateString(operdayController.getOperday().getLastWorkingDay())));
            }
            return true;
        } catch (ValidationError e) {
            auditController.warning(BufferModeSyncBackvalue, "Промежуточная синхронизация backvalue невозможна"
                    , null, e);
            return false;
        }
    }

    /**
     * нужно ли провдоить синхронизацию или только провести выгрузку
     * @return
     */
    private boolean checkExecSync(Operday operday) {
        try {
            Assert.isTrue(BUFFER == operdayController.getOperday().getPdMode()
                    , () -> new ValidationError(TASK_ERROR
                            , format("Режим сохранения проводок %s ожидалось %s", operdayController.getOperday().getPdMode(), BUFFER)));
            DataRecord statBackvalue = workprocRepository.selectFirst(textResourceController
                    .getContent("ru/rbt/barsgl/ejb/controller/operday/task/stamt/buffer_backvalue_stat.sql"), operday.getCurrentDate());
            Assert.isTrue(statBackvalue.getLong("cnt") > 0
                    , () -> new ValidationError(TASK_ERROR, "Нет проводок backvalue для синхронизации"));
            Assert.isTrue(ONLINE == operday.getPhase()
                    , () -> new ValidationError(TASK_ERROR, format("Операционный день в фазе %s ожидалось %s", operday.getPhase(), ONLINE)));
            return true;
        } catch (ValidationError e) {
            auditController.warning(BufferModeSyncBackvalue, "Синхронизация не будет произведена", null, e);
            return false;
        } catch (Exception e) {
            throw new DefaultApplicationException("Ошибка вычисления статуса необх синхронизации", e);
        }
    }

    private String getStepName(String jobName, Properties properties) {
        final String stepName = Optional
                .ofNullable(properties.getProperty("stepName")).orElseGet(() -> throwNoStepDefined());
        Assert.isTrue(!isEmpty(stepName), () -> new DefaultApplicationException(format("Пустой параметр 'Шаг шагрузки', задача '%s'", jobName)));
        return stepName;
    }

    private String throwNoStepDefined()  throws ValidationError {
        throw new ValidationError(ErrorCode.IS_NOT_ENOUTH_DATA
                , format("Синхронизация данных backvalue: дата '%s'", operdayController.getOperday().getCurrentDate()));
    }

    private Date getWorkprocDate(Operday operday) {
        if (operday.getPhase() == ONLINE) {
            return operday.getLastWorkingDay();
        } else if (operday.getPhase() == COB) {
            return operday.getCurrentDate();
        } else {
            throw new ValidationError(TASK_ERROR, format("Операционный день в недопустимом статусе %s", operday.getPhase()));
        }
    }
    @Override
    protected void initExec(String jobName, Properties properties) {}

    private boolean allowAccess() {
        try {
            jobHistoryRepository.executeInNewTransaction(p -> {operdayController.setProcessingStatus(ProcessingStatus.ALLOWED); return null;});
            return true;
        } catch (Exception e) {
            auditController.warning(Operday, "Ошибка разрешения запуска обработки проводок. Статус не изменен", null, e);
            return false;
        }
    }

}
