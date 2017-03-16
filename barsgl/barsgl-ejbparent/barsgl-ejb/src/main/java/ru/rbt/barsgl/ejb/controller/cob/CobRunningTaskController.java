package ru.rbt.barsgl.ejb.controller.cob;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.cob.CobStatId;
import ru.rbt.barsgl.ejb.entity.cob.CobStepStatistics;
import ru.rbt.barsgl.ejb.repository.cob.CobStatRepository;
import ru.rbt.barsgl.ejb.security.AuditController;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.validation.ErrorCode;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.ExceptionUtils;
import ru.rbt.barsgl.shared.enums.CobStep;
import ru.rbt.barsgl.shared.enums.CobStepStatus;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.PreCob;

/**
 * Created by ER18837 on 15.03.17.
 */
public class CobRunningTaskController {
    private static final Logger log = Logger.getLogger(CobRunningTaskController.class.getName());

    @EJB
    private CobStatRepository statRepository;

    @Inject
    private OperdayController operdayController;

    @EJB
    private AuditController auditController;

    public CobStepStatistics executeWithLongRunningStep(Long idCob, CobStep cobStepEnum, CobRunningWork work) throws Exception {
        CobStepStatistics step = statRepository.findById(CobStepStatistics.class, new CobStatId(idCob, cobStepEnum.getPhaseNo()));
        try {
            auditController.info(PreCob, String.format("Начало выполнения шага %d: '%s'", cobStepEnum.getPhaseNo(), cobStepEnum.getPhaseName()));
            statRepository.executeInNewTransaction(persistence ->
                    statRepository.setStepStart(idCob, step.getPhaseNo(), operdayController.getSystemDateTime()));
            CobStepResult result = statRepository.executeInNewTransaction(persistence -> work.runWork());
            switch(result.getStepStatus()) {
                case Success:
                    auditController.info(PreCob, result.getMessage(), step);
                    statRepository.executeInNewTransaction(persistence -> statRepository.setStepSuccess(
                            idCob, step.getPhaseNo(), operdayController.getSystemDateTime(), result.getMessage()));
                    break;
                case Skipped:
                    auditController.info(PreCob, result.getMessage(), step);
                    statRepository.executeInNewTransaction(persistence -> statRepository.setStepSkipped(
                            idCob, step.getPhaseNo(), operdayController.getSystemDateTime(), result.getMessage()));
                    break;
                case Error:
                case Halt:
                    auditController.error(PreCob, result.getMessage(), step, new ValidationError(ErrorCode.COB_STEP_ERROR, result.getErrorMessage()));
                    statRepository.executeInNewTransaction(persistence -> statRepository.setStepError(
                            idCob, step.getPhaseNo(), operdayController.getSystemDateTime(), result.getMessage(), result.getErrorMessage()));
                    break;
                default:
                    auditController.error(PreCob, result.getMessage(), step, "Invalid COB result status: " + result.getStepStatus().name());
                    break;
            }
            return statRepository.refresh(step, true);
        } catch (Throwable t) {
            log.log(Level.SEVERE, "Error on long running cob: ", t);
            String msg = String.format("Шаг COB %d '%s' завершен с ошибкой", cobStepEnum.getPhaseNo(), cobStepEnum.getPhaseName());
            auditController.error(PreCob, msg, step, t);
            if (null != step){
                statRepository.executeInNewTransaction(persistence ->
                    statRepository.setStepError(idCob, step.getPhaseNo(), operdayController.getSystemDateTime(),
                        "Шаг завершен с ошибкой", getErrorMessage(t)));   // TODO подумать тему сообщений
                return statRepository.refresh(step, true);
            }
            return null;
        }
    }

    public CobStepResult fakeTimerStep(int duration, CobStepStatus status) throws InterruptedException {
        Thread.sleep(duration * 1000L);
        String errorMsg = (status == CobStepStatus.Error || status == CobStepStatus.Halt) ? "Это ошибка !" : "";

        return new CobStepResult(status, "Шаг " + status.getLabel(), errorMsg);
    }

    public String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable,
                ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class, DefaultApplicationException.class,
                PersistenceException.class);
    }

}
