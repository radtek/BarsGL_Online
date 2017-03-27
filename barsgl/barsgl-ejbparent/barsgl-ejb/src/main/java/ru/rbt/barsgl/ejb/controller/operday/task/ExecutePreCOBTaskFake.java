package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.cob.CobRunningStepWork;
import ru.rbt.barsgl.ejb.controller.cob.CobRunningTaskController;
import ru.rbt.barsgl.ejb.controller.cob.CobStatRecalculator;
import ru.rbt.barsgl.ejb.controller.cob.CobStepResult;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.ejb.entity.cob.CobStepStatistics;
import ru.rbt.barsgl.ejb.entity.sec.AuditRecord;
import ru.rbt.barsgl.ejb.entity.task.JobHistory;
import ru.rbt.barsgl.ejb.repository.cob.CobStatRepository;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.Assert;
import ru.rbt.barsgl.shared.enums.CobStep;
import ru.rbt.barsgl.shared.enums.CobStepStatus;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.Task;
import static ru.rbt.barsgl.ejbcore.validation.ErrorCode.OPERDAY_TASK_ALREADY_RUN;
import static ru.rbt.barsgl.shared.enums.CobStepStatus.*;

/**
 * Created by ER18837 on 16.03.17.
 */
public class ExecutePreCOBTaskFake extends AbstractJobHistoryAwareTask {
    final CobStepStatus results[] = {Success, Success, Error, Skipped, Success, Halt};

    @Inject
    private CobStatRepository statRepository;

    @Inject
    private CobRunningTaskController taskController;

    @EJB
    private CobStatRecalculator statRecalculator;

    public boolean execWork() throws Exception {
        return execWork(null, null);
    }

    @Override
    public boolean execWork(JobHistory jobHistory, Properties properties) {
        final Operday operday = operdayController.getOperday();

        auditController.info(AuditRecord.LogCode.Operday, format("Запуск процедуры закрытия ОД: '%s'", dateUtils.onlyDateString(operday.getCurrentDate())));

        Long idCob = statRecalculator.calculateCob(true);
        List<CobStepStatistics> steps = statRepository.getCobSteps(idCob);

        List<CobRunningStepWork> works = new ArrayList<>();

        // TODO здесь создаются шаги. Можно задать любую продолжительность и любой возвращаемый статус
        int st = 0;
        int res = 0;
        for(CobStepStatistics step: steps) {
            final CobStepStatus status = (res >= results.length) ? NotStart : results[res];
            works.add(new CobRunningStepWork(CobStep.values()[st++], () -> {
                return fakeTimerStep(idCob, step, step.getEstimated().intValue(), status);
            }));
            res++;
        }

        if (taskController.execWorks(works, idCob)) {
            auditController.info(AuditRecord.LogCode.Operday, "Процедура закрытия ОД отработала успешно");
            return true;
        } else
            return false;
    }

    public CobStepResult fakeTimerStep(Long idCob, CobStepStatistics step, int duration, CobStepStatus status) throws Exception {
        statRecalculator.setStepMessage(idCob, step, "Запуск шага " + step.getPhaseNo());
        if (status != Skipped) {
            Thread.sleep((duration == 0 ? 5 : 10) * 1000L);
        }
        String errorMsg = (status == CobStepStatus.Error || status == CobStepStatus.Halt) ? "Это ошибка !" : "";

        statRecalculator.setStepMessage(idCob, step, "Завершен шаг " + step.getPhaseNo());

        return new CobStepResult(status, "Шаг " + status.getLabel(), errorMsg);
    }

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        return true;
    }

    @Override
    protected void initExec(String jobName, Properties properties) {
    }

    @Override
    protected boolean checkJobStatus(String jobName, Properties properties) {
        try {
            Assert.isTrue(!jobHistoryRepository.isAlreadyRunning(jobName, getHistory(properties).getId(), getOperday(properties))
                    , () -> new ValidationError(OPERDAY_TASK_ALREADY_RUN, jobName, dateUtils.onlyDateString(getOperday(properties))));
            return true;
        } catch (ValidationError e) {
            auditController.warning(Task, format("Задача %s не выполнена", jobName), null, e);
            return false;
        }
    }
}
