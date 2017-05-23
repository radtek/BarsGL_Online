package ru.rbt.barsgl.ejb.controller.operday.task.stamt;

import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.controller.operday.task.TaskUtils;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.ejb.repository.WorkprocRepository;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.StamtUnload;
import static ru.rbt.audit.entity.AuditRecord.LogCode.StamtUnloadBalStep3;
import static ru.rbt.barsgl.ejb.controller.operday.task.stamt.UnloadStamtParams.BALANCE_DELTA_FLEX;

/**
 * Created by Ivan Sevastyanov on 19.09.2016.
 * Выгрузка остатков по счетам с оборотами FLEX (STEP3)
 */
public class StamtUnloadBalanceFlexTask extends AbstractJobHistoryAwareTask {

//    @EJB
//    private AuditController auditController;

    @Inject
    private StamtUnloadController unloadController;

//    @Inject
//    private DateUtils dateUtils;

    @Inject
    private WorkprocRepository workprocRepository;

//    @EJB
//    private OperdayController operdayController;

    @EJB
    private CoreRepository coreRepository;

    @Override
    protected boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        long headerId = -1;
        final Date executeDate = getExecuteDate(properties);
        try {
            if (checkRun(jobHistory.getJobName(), properties)) {
                headerId = unloadController.createHeader(executeDate, UnloadStamtParams.BALANCE_DELTA_FLEX);
                auditController.info(StamtUnloadBalStep3, format("Выгружено лицевых счетов  за %s с оборотам FLEX: '%s'"
                        , dateUtils.onlyDateString(executeDate), fillFlexData(executeDate)));
                unloadController.setHeaderStatus(headerId, DwhUnloadStatus.SUCCEDED);
            }
            return true;
        } catch (Throwable e) {
            unloadController.setHeaderStatus(headerId, DwhUnloadStatus.ERROR);
            auditController.error(StamtUnloadBalStep3, format("Ошиибка выгрузки лицевых счетов с оборотами FLEX за %s"
                    , dateUtils.onlyDateString(executeDate)), null, e);
            return false;
        }
    }

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        final Date executeDate = getExecuteDate(properties);
        try {
            if (TaskUtils.getCheckRun(properties, true)) {

                unloadController.checkConsumed();

                boolean isAlready = unloadController.getAlreadyHeaderCount(executeDate, BALANCE_DELTA_FLEX) > 0;
                Assert.isTrue(!isAlready, () -> new ValidationError(ErrorCode.STAMT_DELTA_ERR
                        , format("Выгрузка остатков по счетам (%s) для STAMT (шаг AfterFlex) в ОД '%s' невозможна"
                        , BALANCE_DELTA_FLEX.getParamName(), dateUtils.onlyDateString(executeDate)), null, "Выгрузка счетов уже произведена"));

                final String stepName = Optional.ofNullable(
                        properties.getProperty("stepName")).orElse("MI3GL").trim();
                final boolean isStepOk = workprocRepository.isStepOK(stepName, executeDate);
                Assert.isTrue(isStepOk, () -> new ValidationError(ErrorCode.STAMT_DELTA_ERR
                        , format("Выгрузка остатков по счетам (%s) для STAMT (шаг AfterFlex) в ОД '%s' невозможна. Не завершен шаг '%s'"
                        , BALANCE_DELTA_FLEX.getParamName(), dateUtils.onlyDateString(executeDate), stepName)));
            }
            return true;
        } catch (ValidationError validationError) {
            auditController.warning(StamtUnload, "Невозможно запустить выгрузку в STAMT (шаг AfterFlex)", null, validationError);
            return false;
        }
    }

    private int fillFlexData(Date executeDate) throws Exception {
        return (int) coreRepository.executeInNewTransaction(persistence -> {
            return coreRepository.executeTransactionally((conn)-> {
                try (CallableStatement statement = conn.prepareCall("{ CALL GL_STMFLEX(?,?) }")){
                    statement.setDate(1, new java.sql.Date(executeDate.getTime()));
                    statement.registerOutParameter(2, java.sql.Types.INTEGER);
                    statement.execute();
                    return statement.getInt(2);
                }
            });
        });
    }

    private Date getExecuteDate(Properties properties) throws SQLException, ParseException {
        return TaskUtils.getDateFromGLOD(properties, workprocRepository, operdayController.getOperday());
    }

    @Override
    protected void initExec(String jobName, Properties properties) {}
}
