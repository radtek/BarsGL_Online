package ru.rbt.barsgl.ejb.controller.lg;

import ru.rbt.barsgl.ejb.entity.lg.LongRunningPatternStepEnum;
import ru.rbt.barsgl.ejb.entity.lg.LongRunningTaskStep;
import ru.rbt.tasks.ejb.entity.task.JobHistory;
import ru.rbt.barsgl.ejb.repository.lg.LongRunningTaskStepRepository;

import javax.ejb.EJB;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Ivan Sevastyanov on 19.10.2016.
 */
public class LongRunningTaskController {

    private static final Logger log = Logger.getLogger(LongRunningTaskController.class.getName());

    @EJB
    private LongRunningTaskStepRepository taskStepRepository;

    public LongRunningTaskStep executeWithLongRunningStep(JobHistory jobHistory
            , LongRunningPatternStepEnum patternStepEnum
            , LongRunningWork work) throws Exception {
        LongRunningTaskStep step = taskStepRepository
                .executeInNewTransaction(persistence -> taskStepRepository.createTaskStep(patternStepEnum, jobHistory));
        try {
            if (taskStepRepository.executeInNewTransaction(persistence -> work.runWork())) {
                taskStepRepository.executeInNewTransaction(persistence -> taskStepRepository.setSuccess(step));
            } else {
                taskStepRepository.executeInNewTransaction(persistence -> taskStepRepository.setError(step));
            }
            return taskStepRepository.findById(LongRunningTaskStep.class, step.getId());
        } catch (Throwable e) {
            log.log(Level.SEVERE, "Error on long running job: ", e);
            if (null != step){
                taskStepRepository.executeInNewTransaction(persistence -> taskStepRepository.setError(step));
                return taskStepRepository.findById(LongRunningTaskStep.class, step.getId());
            }
            return null;
        }
    }

}
