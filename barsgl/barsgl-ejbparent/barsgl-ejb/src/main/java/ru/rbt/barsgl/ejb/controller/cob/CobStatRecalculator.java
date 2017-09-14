package ru.rbt.barsgl.ejb.controller.cob;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.cob.CobStepStatistics;
import ru.rbt.barsgl.ejb.repository.cob.CobStatRepository;
import ru.rbt.barsgl.shared.enums.CobPhase;
import ru.rbt.barsgl.shared.enums.CobStepStatus;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;

import javax.ejb.*;
import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.util.concurrent.TimeUnit.MINUTES;
import static ru.rbt.audit.entity.AuditRecord.LogCode.PreCob;


/**
 * Created by ER18837 on 10.03.17.
 * расчет времени выполнения COB
 */
@Singleton
@AccessTimeout(value = 5, unit = MINUTES)
public class CobStatRecalculator {
    private final SimpleDateFormat msgDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public enum CobStatAction {
        Calculate, IncEstimate
    }

    @Inject
    private OperdayController operdayController;

    @EJB
    private CobStatRepository statRepository;

    @EJB
    private AuditController auditController;

    @Inject
    DateUtils dateUtils;

    public Long calculateCob() {
        return calculateCob(false);
    }

    @Lock(LockType.WRITE)
    public Long calculateCob(boolean withRun) {
        try {
            return statRepository.executeInNewTransaction(persistence -> {
                Operday operday = operdayController.getOperday();
                Date curdate = operday.getCurrentDate();
                // TODO проверка, что COB в этот опердень не запущен!!

                if (statRepository.getRunCobStatus(curdate) == CobStepStatus.Running) {
                    throw new ValidationError(ErrorCode.COB_IS_RUNNING, dateUtils.onlyDateString(curdate));
                }

                auditController.info(PreCob, "Расчет длительности COB");
                Long idCob = statRepository.createCobStepGroup(curdate);
                for (CobPhase phase : CobPhase.values()) {
                    Long parameter = statRepository.getStepParameter(phase, curdate, operday.getLastWorkingDay());
                    statRepository.setStepEstimate(idCob, phase.getPhaseNo(), parameter);
                }
                return idCob;
            });
        } catch (Throwable t) {
            auditController.error(PreCob, "Ошибка при расчете длительности COB", null, t);
            throw new DefaultApplicationException(t.getMessage(), t);
        }
    }

    public void setStepStart(Long idCob, CobStepStatistics step, CobPhase phase) throws Exception {
        String msg = "Начало выполнения шага";
        auditController.info(PreCob, String.format("%s %d: '%s'", msg, phase.getPhaseNo(), step.getPhaseName()), step);
        statRepository.executeInNewTransaction(persistence ->
                statRepository.setStepStart(idCob, phase.getPhaseNo(), operdayController.getSystemDateTime(), getLogMessage(msg)));
    }

    public void setStepSuccess(Long idCob, CobStepStatistics step, CobPhase phase, String message) throws Exception {
        auditController.info(PreCob, message, step);
        try {
            statRepository.executeInNewTransaction(persistence -> statRepository.setStepSuccess(
                    idCob, phase.getPhaseNo(), operdayController.getSystemDateTime(), getLogMessage(message), true));
        } catch (Exception e) {
                statRepository.executeInNewTransaction(persistence -> statRepository.setStepSuccess(
                        idCob, phase.getPhaseNo(), operdayController.getSystemDateTime(), getLogMessage(message), false));
        }
    }

    public void setStepSkipped(Long idCob, CobStepStatistics step, CobPhase phase, String message) throws Exception {
        auditController.info(PreCob, message, step);
        try {
            statRepository.executeInNewTransaction(persistence -> statRepository.setStepSkipped(
                    idCob, phase.getPhaseNo(), operdayController.getSystemDateTime(), getLogMessage(message), true));
        } catch (Exception e) {
                statRepository.executeInNewTransaction(persistence -> statRepository.setStepSkipped(
                        idCob, phase.getPhaseNo(), operdayController.getSystemDateTime(), getLogMessage(message), false));
        }
    }

    public void setStepError(Long idCob, CobStepStatistics step, CobPhase phase, String message, String errorMessage, CobStepStatus stepStatus) throws Exception {
        auditController.error(PreCob, message, step, new ValidationError(ErrorCode.COB_STEP_ERROR, errorMessage));
        try {
            statRepository.executeInNewTransaction(persistence -> statRepository.setStepError(
                    idCob, phase.getPhaseNo(), operdayController.getSystemDateTime(), getLogMessage(message), errorMessage, stepStatus, true));
        } catch (Exception e) {
                statRepository.executeInNewTransaction(persistence -> statRepository.setStepError(
                        idCob, phase.getPhaseNo(), operdayController.getSystemDateTime(), getLogMessage(message), errorMessage, stepStatus, false));
        }
    }

    public void addStepInfo(Long idCob, CobPhase phase, String message) {
        auditController.info(PreCob, message);
        setStepMessage(idCob, phase, message);
    }

    public void addStepWarning(Long idCob, CobPhase phase, String message) {
        auditController.warning(PreCob, message);
        setStepMessage(idCob, phase, message);
    }

    public void addStepError(Long idCob, CobPhase phase, String message, Throwable e) {
        auditController.error(PreCob, message, null, null, e);
        setStepMessage(idCob, phase, message);
    }

    private void setStepMessage(Long idCob, CobPhase phase, String message) {
        try {
            statRepository.executeInNewTransaction(persistence ->
                    statRepository.updateStepMessage(idCob, phase.getPhaseNo(), getLogMessage(message), true));
        } catch (Exception e) {
            try {
                statRepository.executeInNewTransaction(persistence ->
                        statRepository.updateStepMessage(idCob, phase.getPhaseNo(), getLogMessage(message), false));
            } catch (Exception e1) {}
        }
    }

    private String getLogMessage(String message) {
        return String.format("%s  %s", msgDateFormat.format(operdayController.getSystemDateTime()), message);
    }
}
