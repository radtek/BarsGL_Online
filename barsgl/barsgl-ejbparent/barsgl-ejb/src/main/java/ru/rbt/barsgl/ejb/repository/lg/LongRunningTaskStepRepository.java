package ru.rbt.barsgl.ejb.repository.lg;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.entity.lg.LongRunningPatternStepEnum;
import ru.rbt.barsgl.ejb.entity.lg.LongRunningTaskStep;
import ru.rbt.barsgl.ejb.entity.lg.LongRunningTaskStepId;
import ru.rbt.barsgl.ejb.entity.lg.LongRunningTaskStepPattern;
import ru.rbt.tasks.ejb.entity.task.JobHistory;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 * Created by Ivan Sevastyanov on 19.10.2016.
 */
@Stateless
@LocalBean
public class LongRunningTaskStepRepository extends AbstractBaseEntityRepository<LongRunningTaskStep, LongRunningTaskStepId> {

    @EJB
    private OperdayController operdayController;

    public LongRunningTaskStep createTaskStep(LongRunningPatternStepEnum stepPattern, JobHistory jobHistory) {
        LongRunningTaskStep step = new LongRunningTaskStep();
        step.setId(new LongRunningTaskStepId(stepPattern.getIdStep(), jobHistory.getId()));
        step.setStartDate(operdayController.getSystemDateTime());
        step.setStatus(DwhUnloadStatus.STARTED);
        return save(step);
    }

    public LongRunningTaskStep setSuccess(LongRunningTaskStep step) {
        step.setStatus(DwhUnloadStatus.SUCCEDED);
        step.setEndDate(operdayController.getSystemDateTime());
        return update(step);
    }

    public LongRunningTaskStep setError(LongRunningTaskStep step) {
        step.setStatus(DwhUnloadStatus.ERROR);
        step.setEndDate(operdayController.getSystemDateTime());
        return update(step);
    }

}
