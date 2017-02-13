package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.controller.BackvalueJournalController;
import ru.rbt.barsgl.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.barsgl.ejbcore.util.DateUtils;
import ru.rbt.barsgl.ejbcore.validation.ErrorCode;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.Assert;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.Optional;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.COB;
import static ru.rbt.barsgl.audit.entity.AuditRecord.LogCode.RecalcBS2;

/**
 * Created by Ivan Sevastyanov on 17.02.2016.
 * пересчет остатков по БС2 по журналу BACKVALUE
 */
public class RecalcBS2Task implements ParamsAwareRunnable {

    @EJB
    private BackvalueJournalController backvalueJournalController;

    @EJB
    private AuditController auditController;

    @EJB
    private OperdayController operdayController;

    @EJB
    private CoreRepository repository;

    @Inject
    private DateUtils dateUtils;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        try {
            if (checkRun(properties)) {
                auditController.info(RecalcBS2, format("Начало пересчета остатков по БС2 в закрытом ОД '%s'"
                        , dateUtils.onlyDateString(operdayController.getOperday().getCurrentDate())));
                auditController.info(RecalcBS2, format("Всего пересчитано дат '%s'"
                        , repository.executeInNewTransaction(persistence -> backvalueJournalController.recalculateBS2())));
            }
        } catch (Throwable e) {
            auditController.error(RecalcBS2, "Ошибка при пересчета остатков по счетам БС2", null, e);
        }
    }

    private boolean checkRun(Properties properties) {
        if (TaskUtils.getCheckRun(properties, true)) {
            Assert.isTrue(COB == operdayController.getOperday().getPhase()
                    , () -> new ValidationError(ErrorCode.OPERDAY_STATE_INVALID
                            , operdayController.getOperday().getPhase().name(), COB.name()));
        }
        return true;
    }
}
