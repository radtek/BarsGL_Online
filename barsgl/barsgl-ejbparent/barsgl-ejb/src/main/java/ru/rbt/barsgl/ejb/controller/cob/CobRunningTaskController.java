package ru.rbt.barsgl.ejb.controller.cob;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.cob.CobStatId;
import ru.rbt.barsgl.ejb.entity.cob.CobStepStatistics;
import ru.rbt.barsgl.ejb.repository.cob.CobStatRepository;
import ru.rbt.barsgl.ejb.security.AuditController;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.ExceptionUtils;
import ru.rbt.barsgl.shared.enums.CobStep;
import ru.rbt.barsgl.shared.enums.CobStepStatus;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;
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

    @Inject
    CobStatService statService;

    @EJB
    private CobStatRecalculator recalculator;

    public CobStepStatistics executeWithLongRunningStep(Long idCob, CobStep cobStepEnum, CobRunningWork work) {
        CobStepStatistics step = statRepository.findById(CobStepStatistics.class, new CobStatId(idCob, cobStepEnum.getPhaseNo()));
        try {
            recalculator.setStepStart(idCob, step);
            CobStepResult result = statRepository.executeInNewTransaction(persistence -> work.runWork());
            switch(result.getStepStatus()) {
                case Success:
                    recalculator.setStepSuccess(idCob, step, result.getMessage());
                    break;
                case Skipped:
                    recalculator.setStepSkipped(idCob, step, result.getMessage());
                    break;
                case Error:
                    recalculator.setStepError(idCob, step, result.getMessage(), result.getErrorMessage(), result.getStepStatus());
                    break;
                case Halt:
                    recalculator.setStepError(idCob, step, result.getMessage(), result.getErrorMessage(), result.getStepStatus());
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
                try {
                    recalculator.setStepError(idCob, step, "Шаг завершен с ошибкой", getErrorMessage(t), CobStepStatus.Halt);
                    return statRepository.refresh(step, true);
                } catch (Exception ignored) {}
            }
            return null;
        }
    }

    public String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable,
                ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class,
                IllegalArgumentException.class, PersistenceException.class, DefaultApplicationException.class);
    }

    public boolean execWorks(List<CobRunningStepWork> works, Long idCob) {
        for (CobRunningStepWork work : works) {
            CobStepStatistics step = executeWithLongRunningStep(idCob, work.getStep(), work.getWork());
            if (null == step) {
                auditController.error(PreCob,
                        format("Не удалось создать шаг выполнения для '%s'", work), null, new DefaultApplicationException(""));
            } else if (step.getStatus() == CobStepStatus.Halt) {
                auditController.error(PreCob,
                        format("Сбой выполнения COB. Шаг %s: '%s'. Процесс остановлен", step.getPhaseNo().toString(), step.getPhaseName()), null, new DefaultApplicationException(""));
                return false;
            }
        }
        return true;
    }

}
