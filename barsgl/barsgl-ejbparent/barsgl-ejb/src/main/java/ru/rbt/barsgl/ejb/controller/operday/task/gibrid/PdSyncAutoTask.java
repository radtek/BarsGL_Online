package ru.rbt.barsgl.ejb.controller.operday.task.gibrid;

import ru.rbt.barsgl.ejb.controller.operday.task.PdSyncTask;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.ejb.repository.WorkprocRepository;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.BufferModeSyncAuto;
import static ru.rbt.ejbcore.validation.ErrorCode.PD_SYNC_GIBRID;

/**
 * Created by Ivan Sevastyanov on 25.04.2018.
 */
public class PdSyncAutoTask extends AbstractJobHistoryAwareTask {

    public static final String STEP_KEY_NAME = "stepName";
    public static final String DEFAULT_STEP_NAME = "SOD_P4";

    @Inject
    private PdSyncTask originalSyncTask;

    @Inject
    private WorkprocRepository workprocRepository;

    @Override
    protected boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        return originalSyncTask.execWork(jobHistory, properties);
    }

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        try {
            final String stepName = Optional.ofNullable(
                    properties.getProperty(STEP_KEY_NAME)).orElse(DEFAULT_STEP_NAME).trim();
            final boolean isStepOk = workprocRepository.isStepOK(stepName, operdayController.getOperday().getLastWorkingDay());
            Assert.isTrue(isStepOk, () -> new ValidationError(PD_SYNC_GIBRID
                    , format("Автоматическая синхронизация буфера невозможна. Не завершен шаг '%s' загрузчика в ОД '%s'"
                    , stepName, dateUtils.onlyDateString(operdayController.getOperday().getLastWorkingDay()))));
        } catch (ValidationError e) {
            auditController.warning(BufferModeSyncAuto, "Не прошла проверка выполнения автоматического сброса буфера", null, e);
            return false;
        }
        return true;
    }

    @Override
    protected void initExec(String jobName, Properties properties) {
        properties.put(PdSyncTask.PdSyncTaskContext.OPERDAY, operdayController.getOperday());
    }
}
