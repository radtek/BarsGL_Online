package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.PreCobStepController;
import ru.rbt.barsgl.ejb.integr.bg.BackValueOperationController;
import ru.rbt.tasks.ejb.entity.task.JobHistory;
import ru.rbt.barsgl.ejb.integr.bg.EtlPostingController;
import ru.rbt.tasks.ejb.repository.JobHistoryRepository;
import ru.rbt.barsgl.ejb.repository.WorkprocRepository;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.barsgl.shared.enums.OperState;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.SQLException;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.audit.entity.AuditRecord.LogCode.RecalcWTAC;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;
import static ru.rbt.ejbcore.validation.ErrorCode.OPERDAY_LDR_STEP_ERR;
import static ru.rbt.ejbcore.validation.ErrorCode.OPERDAY_STATE_INVALID;
import static ru.rbt.ejbcore.validation.ErrorCode.OPERDAY_TASK_ALREADY_EXC;

/**
 * Created by Ivan Sevastyanov on 17.02.2016.
 * повторная обработка операций в статусе WTAC
 */
public class ReprocessWtacOparationsTask implements ParamsAwareRunnable {

    public static final String STEP_NAME_KEY = "stepName";
    public static final String DEFAULT_STEP_NAME = "A1GL";

    public static final String JOB_NAME = ReprocessWtacOparationsTask.class.getSimpleName();

    @EJB
    private OperdayController operdayController;

    @Inject
    private WorkprocRepository workprocRepository;

    @Inject
    private DateUtils dateUtils;

    @EJB
    private JobHistoryRepository jobHistoryRepository;

    @EJB
    private AuditController auditController;

    @EJB
    private EtlPostingController etlPostingController;

    @EJB
    private PreCobStepController preCobStepController;

    @EJB
    private BackValueOperationController backValueOperationController;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        final Operday currentOperday = operdayController.getOperday();
        JobHistory history = null;
        if (checkRun(properties)) {
            try {
                history = jobHistoryRepository.executeInNewTransaction(persistence ->
                        jobHistoryRepository.createHeader(jobName, currentOperday.getCurrentDate()));
                execWork();
                updateHeaderStatus(history, DwhUnloadStatus.SUCCEDED);
            } catch (Throwable e) {
                auditController.error(RecalcWTAC, "Ошибка при переобработке операций в статусе WTAC", null, e);
                updateHeaderStatus(history, DwhUnloadStatus.ERROR);
            }
        }
    }

    private void execWork() throws Exception {
        jobHistoryRepository.executeInNewTransaction(persistence -> {
            final Operday currentOperday = operdayController.getOperday();
            // обработка только за предыдущий день, так как задача может быть запущена в течение ОД по достижении A1GL
            Date dateWtacPrev = currentOperday.getLastWorkingDay();
            String dateStr = dateUtils.onlyDateString(dateWtacPrev);

            auditController.info(RecalcWTAC
                    , format("Повторная обработка операций со статусом '%s' за день '%s'", OperState.WTAC, dateStr ));
            int errorCount = etlPostingController.reprocessWtacOperations(dateWtacPrev, dateWtacPrev ).size();
            if (errorCount > 0){
                auditController.warning(RecalcWTAC, format("Обработано с ошибкой %d операций", errorCount), null, "");
            } else {
                auditController.info(RecalcWTAC, format("Все операции со статусом %s повторно обработаны", OperState.WTAC));
            }

            auditController.info(RecalcWTAC
                    , format("Повторная обработка веерных операций со статусом '%s' за '%s'", OperState.WTAC, dateStr));
            int errorCountFan = preCobStepController.reprocessWtacFan(new java.sql.Date(dateWtacPrev.getTime()));
            if (errorCountFan > 0) {
                auditController.warning(RecalcWTAC, format("Обработано с ошибкой %d веерных операций", errorCount), null, "");
            } else {
                auditController.info(RecalcWTAC, format("Все веерные операции со статусом %s повторно обработаны", OperState.WTAC));
            }

            auditController.info(RecalcWTAC
                    , format("Повторная обработка BackValue операций со статусом '%s' за день '%s'", OperState.BWTAC, dateStr));
            int errorCountBv = backValueOperationController.reprocessWtacBackValue(dateWtacPrev);
            if (errorCountBv > 0){
                auditController.warning(RecalcWTAC, format("Обработано с ошибкой %d BackValue операций", errorCountBv), null, "");
            } else {
                auditController.info(RecalcWTAC, format("Все BackValue операции со статусом %s повторно обработаны", OperState.WTAC));
            }

            auditController.info(RecalcWTAC
                    , format("Повторная обработка операций со статусом '%s' за день '%s' закончена", OperState.WTAC, dateStr));

            return null;
        });
    }

    private boolean checkRun(Properties properties) throws SQLException {
        if (TaskUtils.getCheckRun(properties, true)) {
            try {
                final String stepName = Optional
                        .ofNullable(properties.getProperty(STEP_NAME_KEY)).orElse(DEFAULT_STEP_NAME);
                Assert.isTrue(!isEmpty(stepName), () -> new DefaultApplicationException("Пустой параметр 'Шаг шагрузки'"));
                Assert.isTrue(workprocRepository.isStepOK(stepName, operdayController.getOperday().getLastWorkingDay())
                        , () -> new ValidationError(OPERDAY_LDR_STEP_ERR, stepName, dateUtils.onlyDateString(operdayController.getOperday().getLastWorkingDay())));
                Assert.isTrue(!jobHistoryRepository.isTaskOK(JOB_NAME, operdayController.getOperday().getCurrentDate())
                        , () -> new ValidationError(OPERDAY_TASK_ALREADY_EXC, JOB_NAME, dateUtils.onlyDateString(operdayController.getOperday().getCurrentDate())));
                Assert.isTrue(ONLINE == operdayController.getOperday().getPhase()
                        , () -> new ValidationError(OPERDAY_STATE_INVALID, operdayController.getOperday().getPhase().name(), ONLINE.name()));
            } catch (Throwable e) {
                auditController.error(RecalcWTAC, "Невозможно переобработать операции в статусе WTAC", null, e);
                return false;
            }
        }
        return true;
    }

    private void updateHeaderStatus(JobHistory history, DwhUnloadStatus status) throws Exception {
        if (null != history) {
            jobHistoryRepository.executeInNewTransaction(persistence -> jobHistoryRepository.updateStatus(history, status));
        }
    }
}
