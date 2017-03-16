package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.cob.CobRunningStepWork;
import ru.rbt.barsgl.ejb.controller.cob.CobRunningTaskController;
import ru.rbt.barsgl.ejb.controller.cob.CobStatRecalculator;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.ejb.entity.cob.CobStepStatistics;
import ru.rbt.barsgl.ejb.entity.sec.AuditRecord;
import ru.rbt.barsgl.ejb.entity.task.JobHistory;
import ru.rbt.barsgl.ejb.repository.cob.CobStatRepository;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.shared.enums.CobStep;
import ru.rbt.barsgl.shared.enums.CobStepStatus;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.PreCob;

/**
 * Created by ER18837 on 16.03.17.
 */
public class ExecutePreCOBTaskFake extends AbstractJobHistoryAwareTask {

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
        try {
            final Operday operday = operdayController.getOperday();

            auditController.info(AuditRecord.LogCode.Operday, format("Запуск процедуры закрытия ОД: '%s'", dateUtils.onlyDateString(operday.getCurrentDate())));

            Long idCob = statRecalculator.calculateCob(true);
            List<CobStepStatistics> steps = statRepository.getCobSteps(idCob);

            List<CobRunningStepWork> works = new ArrayList<>();

            int st = 0;
            for(CobStepStatistics step: steps) {
                works.add(new CobRunningStepWork(CobStep.values()[st++], () -> {
                    return taskController.fakeTimerStep(10, CobStepStatus.Success);   //step.getEstimated().intValue()
                }));
            }

            for (CobRunningStepWork work : works) {
                CobStepStatistics step = taskController.executeWithLongRunningStep(idCob, work.getStep(), work.getWork());
                if (null == step){
                    auditController.error(PreCob,
                            format("Не удалось создать шаг выполнения для '%s'", work), null, new DefaultApplicationException(""));
                } else
                if (step.getStatus() == CobStepStatus.Halt) {
                    auditController.error(PreCob,
                            format("Сбой выполнения COB. Шаг %s: '%s'. Процесс остановлен", step.getPhaseNo().toString(), step.getPhaseName()), null, new DefaultApplicationException(""));
                    return false;
                }
            }
            auditController.info(AuditRecord.LogCode.Operday, "Процедура закрытия ОД отработала успешно");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        return false;
    }

    @Override
    protected void initExec(String jobName, Properties properties) {
    }
}
