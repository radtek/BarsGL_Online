package ru.rbt.barsgl.ejb.controller.cob;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.cob.CobStepStatistics;
import ru.rbt.barsgl.ejb.repository.cob.CobStatRepository;
import ru.rbt.barsgl.ejb.security.AuditController;
import ru.rbt.barsgl.ejbcore.util.DateUtils;
import ru.rbt.barsgl.ejbcore.validation.ErrorCode;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.enums.CobStep;
import ru.rbt.barsgl.shared.enums.CobStepStatus;

import javax.ejb.*;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.util.concurrent.TimeUnit.MINUTES;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.PreCob;


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
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Long calculateCob(boolean withRun) {
        try {
            Operday operday = operdayController.getOperday();
            Date curdate = operday.getCurrentDate();
            // TODO проверка, что COB в этот опердень не запущен!!

            if (statRepository.getRunCobStatus(curdate) == CobStepStatus.Running) {
                throw new ValidationError(ErrorCode.COB_IS_RUNNING, dateUtils.onlyDateString(curdate));
            }

            Long idCob = statRepository.createCobStepGroup(curdate);
            BigDecimal totalEstimate = BigDecimal.ZERO;
            for (CobStep step : CobStep.values()) {
                Long parameter = statRepository.getStepParameter(step, curdate, operday.getLastWorkingDay());
                statRepository.setStepEstimate(idCob, step.getPhaseNo(), parameter);
            }
            if(withRun) {   // TODO это чтобы сразу пометить запущенным. Надо отлельный признак
                statRepository.setStepStart(idCob, 1, operdayController.getSystemDateTime());
            }
            return idCob;
        } catch (Throwable t) {
            auditController.error(PreCob, "Ошибка при расчете длительности COB", null, t);
            return null;
        }
    }

    public void setStepStart(Long idCob, CobStepStatistics step) throws Exception {
        auditController.info(PreCob, String.format("Начало выполнения шага %d: '%s'", step.getPhaseNo(), step.getPhaseName()));
        statRepository.executeInNewTransaction(persistence ->
                statRepository.setStepStart(idCob, step.getPhaseNo(), operdayController.getSystemDateTime()));
    }

    public void setStepSuccess(Long idCob, CobStepStatistics step, String message) throws Exception {
        auditController.info(PreCob, message, step);
        statRepository.executeInNewTransaction(persistence -> statRepository.setStepSuccess(
                idCob, step.getPhaseNo(), operdayController.getSystemDateTime(), message));
    }

    public void setStepSkipped(Long idCob, CobStepStatistics step, String message) throws Exception {
        auditController.info(PreCob, message, step);
        statRepository.executeInNewTransaction(persistence -> statRepository.setStepSkipped(
                idCob, step.getPhaseNo(), operdayController.getSystemDateTime(), message));
    }

    public void setStepError(Long idCob, CobStepStatistics step, String message, String errorMessage, CobStepStatus stepStatus) throws Exception {
        auditController.error(PreCob, message, step, new ValidationError(ErrorCode.COB_STEP_ERROR, errorMessage));
        statRepository.executeInNewTransaction(persistence -> statRepository.setStepError(
                idCob, step.getPhaseNo(), operdayController.getSystemDateTime(), message, errorMessage, stepStatus));
    }

    public void setStepMessage(Long idCob, CobStepStatistics step, String message) throws Exception {
        auditController.info(PreCob, message, step);
        statRepository.executeInNewTransaction(persistence -> statRepository.updateStepMessage(idCob, step.getPhaseNo(),
                String.format("%s: %s", msgDateFormat.format(operdayController.getSystemDateTime()) ,message)));
    }

}
