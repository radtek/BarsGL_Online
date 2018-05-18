package ru.rbt.barsgl.ejb.controller.operday.task.cmn;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.controller.operday.task.TaskUtils;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.tasks.ejb.entity.task.JobHistory;
import ru.rbt.tasks.ejb.repository.JobHistoryRepository;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.Task;
import static ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus.*;
import static ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask.JobHistoryContext.*;
import static ru.rbt.ejbcore.validation.ErrorCode.OPERDAY_TASK_ALREADY_EXC;
import static ru.rbt.ejbcore.validation.ErrorCode.OPERDAY_TASK_ALREADY_RUN;

/**
 * Created by Ivan Sevastyanov on 14.09.2016.
 */
public abstract class AbstractJobHistoryAwareTask implements ParamsAwareRunnable {

    @EJB
    protected OperdayController operdayController;

    @EJB
    protected JobHistoryRepository jobHistoryRepository;

    @EJB
    protected AuditController auditController;

    @Inject
    protected DateUtils dateUtils;

    public enum JobHistoryContext {
        HISTORY_ID, HISTORY, JOB_NAME
    }

    @Override
    public void run(String jobName, Properties properties) throws Exception {   // final
        try {
            jobHistoryRepository.executeInNewTransaction(persistence -> {
                initExecPrivate(jobName, properties);
                initExec(jobName, properties);
                JobHistory history = (JobHistory) properties.get(HISTORY);
                if (checkAll(jobName, properties)) {
                    if (null == history) {
                        history = jobHistoryRepository.executeInNewTransaction(pers
                                -> jobHistoryRepository.createHeader(jobName, getOperday(properties)));
                        properties.put(HISTORY_ID.name(), history.getId());
                        properties.setProperty(HISTORY_ID.name(), history.getId().toString());
                    }
                    try {
                        final JobHistory finalHistory = history;
                        if (jobHistoryRepository.executeInNewTransaction(pers -> execWork(finalHistory, properties))) {
                            updateJobStatus(history, SUCCEDED);
                        } else {
                            updateJobStatus(history, ERROR);
                        }
                    } catch (Throwable e) {
                        auditController.error(Task, format("Необработанная ошибка при выполнении задачи '%s'", jobName), null, e);
                        if (null != history) {
                            updateJobStatus(history, ERROR);
                        }
                    } finally {
                        properties.remove(HISTORY_ID.name());
                        properties.setProperty(HISTORY_ID.name(), "");
                    }
                } else
                if (null != history && history.isRunning()) {
                    updateJobStatus(history, SKIPPED);
                }
                return null;
            });
        } catch (Exception e) {
            auditController.error(Task, format("Необработанная ошибка верхнего уровня при выполнении задачи '%s'", jobName), null, e);
        }
    }

    /**
     * реализация полезной работы.
     * @param jobHistory history record
     * @param properties свойства
     * @return Возвращает true, если работа выполнена успешно, иначе false
     * @throws Exception
     */
    protected abstract boolean execWork(JobHistory jobHistory, Properties properties) throws Exception;

    /**
     * реализация проверки возможности выполнения задачи
     * @param jobName
     * @param properties
     * @return true, если проверка прошла и можно выполнять {@link ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask#execWork(JobHistory, Properties)}
     * @throws Exception
     */
    protected abstract boolean checkRun(String jobName, Properties properties) throws Exception;

    /**
     * Умолчательная реализация
     * @return текущий закрытый/открытый ОД
     */
    protected Date getOperday(Properties properties) {
        return operdayController.getOperday().getCurrentDate();
    }

    protected JobHistory getHistory(Properties properties) {
        return (JobHistory) properties.get(HISTORY);
    }

    private JobHistory updateJobStatus(final JobHistory history, DwhUnloadStatus status) throws Exception {
        if (null != history) {
            return jobHistoryRepository.executeInNewTransaction(persistence -> jobHistoryRepository.updateStatus(history, status));
        } else return null;
    }

    /**
     * проверка нужно ли запускать задачу взависимости от того запускалась ли она в ОД  AbstractJobHistoryAwareTask#getOperday(java.util.Properties)
     * @param jobName
     * @param properties
     * @return true если проверка прошла и задача должна выполняться
     */
    protected boolean checkJobStatus(String jobName, Properties properties) {
        try {
            return checkOk(jobName, properties) && checkAlreadyRunning(jobName, properties);
        } catch (ValidationError e) {
            auditController.warning(Task, format("Задача %s не выполнена", jobName), null, e);
            return false;
        }
    }

    protected boolean checkOk(String jobName, Properties properties) {
        Assert.isTrue(!jobHistoryRepository.isTaskOK(jobName, getOperday(properties))
                , () -> new ValidationError(OPERDAY_TASK_ALREADY_EXC, jobName, dateUtils.onlyDateString(getOperday(properties))));
        return true;
    }

    protected boolean checkAlreadyRunning(String jobName, Properties properties) {
        Assert.isTrue(!jobHistoryRepository.isAlreadyRunning(jobName, getOperday(properties))
                , () -> new ValidationError(OPERDAY_TASK_ALREADY_RUN, jobName, dateUtils.onlyDateString(getOperday(properties))));
        return true;
    }

    /**
     * Инициализация контекста
     * В properties с ключом {@link JobHistoryContext#HISTORY} устанавливается текущая запись истории {@link JobHistory}
     * @param properties используется для настроек выполнения и хранения контекста
     *
     */
    protected abstract void initExec(String jobName, Properties properties) throws Exception;

    private void initExecPrivate(String jobName, Properties properties) {
        properties.put(JobHistoryContext.JOB_NAME, jobName);
        JobHistory history = getPreinstlledJobHistory(properties);
        if (null != history) {
            properties.put(JobHistoryContext.HISTORY, history);
        }
    }

    /**
     * находим из свойств текущую историю по идентифиактору из <code>properties</code>, если есть и устанавливаем ее в контекст
     * с ключом {@link ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask.JobHistoryContext#HISTORY}
     * @param properties
     * @return
     */
    protected JobHistory getPreinstlledJobHistory(Properties properties) {
        Long historyId;
        try {
            historyId = Long.parseLong(Optional.ofNullable(properties.getProperty(JobHistoryContext.HISTORY_ID.name())).orElse("-1"));
        } catch (NumberFormatException e) {
            historyId = -1L;
        }
        if (0 < historyId) {
            return Assert.notNull(jobHistoryRepository.findById(JobHistory.class, historyId)
                    , format("Не найдена запись истории запуска с ИД: '%s'", historyId));
        }
        return null;
    }

    private boolean checkAll(String jobName, Properties properties) throws Exception {
        if (isNeedCheckRun(properties)) {
            return checkJobStatus(jobName, properties) && checkRun(jobName, properties);
        }
        return true;
    }

    private boolean isNeedCheckRun(Properties properties) {
        if (!TaskUtils.getCheckRun(properties, true)) {
            auditController.warning(Task, String.format("Отключена проверка предварительных условий по задаче %s. Задача будет выполнена принудительно.", properties.get(JOB_NAME)));
            return false;
        }
        return true;
    }
}
