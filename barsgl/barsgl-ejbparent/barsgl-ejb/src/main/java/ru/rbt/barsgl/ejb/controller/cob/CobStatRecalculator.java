package ru.rbt.barsgl.ejb.controller.cob;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.repository.cob.CobStatRepository;
import ru.rbt.barsgl.ejb.security.AuditController;
import ru.rbt.barsgl.shared.enums.CobStep;

import javax.ejb.*;
import javax.inject.Inject;
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

    @Inject
    private OperdayController operdayController;

    @Inject
    private CobStatRepository statRepository;

    @EJB
    private AuditController auditController;

    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Long calculateCob() {
        try {
            Operday operday = operdayController.getOperday();
            Date curdate = operday.getCurrentDate();

            Long idCob = statRepository.createCobStepGroup(curdate);
            for (CobStep step : CobStep.values()) {
                Long parameter = statRepository.getStepParameter(step, curdate, operday.getLastWorkingDay());
                statRepository.setStepEstimation(idCob, step, parameter);
            }
            return idCob;
        } catch (Throwable t) {
            auditController.error(PreCob, "Ошибка при расчете длительности COB", null, t);
            return null;
        }
    }
}
