package ru.rbt.barsgl.ejb.controller.operday.task.stamt;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.task.TaskUtils;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.NewAccounts;
import static ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus.PROCESSING;
import static ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus.SUCCEDED;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.controller.operday.task.stamt.UnloadStamtParams.NEW_ACCOUNTS;
import static ru.rbt.ejbcore.validation.ErrorCode.TASK_ERROR;

public class StamtUnloadNewAccountsTask implements ParamsAwareRunnable{

    @EJB
    private CoreRepository repository;

    @Inject
    private StamtUnloadController controller;

    @EJB
    private OperdayController operdayController;

    @Inject
    private DateUtils dateUtils;

    @EJB
    private AuditController auditController;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        if (checkRun(properties)) {
            Date operdate = TaskUtils.getExecuteDate("operday", properties, operdayController.getOperday().getCurrentDate());
            String operdateString = dateUtils.onlyDateString(operdate);
            auditController.info(NewAccounts, format("Начало выгрузки новых счетов в STAMT за дату '%s'", operdateString));
            auditController.info(NewAccounts, format("Выгружено счетов в операционном дне '%s': '%s'"
                , operdateString, fillData(operdate)));
            auditController.info(NewAccounts, format("Окончание выгрузки новых счетов в STAMT за дату '%s'", operdateString));
        }
    }

    private int fillData(Date operday) throws Exception {
        return (int) repository.executeInNewTransaction(persistence -> {
            return repository.executeTransactionally(connection -> {
                try (CallableStatement proc = connection.prepareCall("{call GL_ACCSTM_UNLOAD(?, ?, ?, ?) }")) {
                    proc.setDate(1, new java.sql.Date(operday.getTime()));
                    proc.registerOutParameter(2,  Types.INTEGER);
                    proc.setString(3, UnloadStamtParams.NEW_ACCOUNTS.getParamName());
                    proc.setString(4, UnloadStamtParams.NEW_ACCOUNTS.getParamDesc());
                    proc.executeUpdate();
                    return proc.getInt(2);
                }
            });
        });
    }

    private boolean checkRun(Properties properties) throws SQLException {
        if (TaskUtils.getCheckRun(properties, true)) {
            Operday operday = operdayController.getOperday();
            try {
                String onlyCurrentDateString = dateUtils.onlyDateString(operday.getCurrentDate());
                Assert.isTrue(controller.getAlreadyHeaderCount(operday.getCurrentDate(), NEW_ACCOUNTS
                        , DwhUnloadStatus.STARTED, SUCCEDED, PROCESSING) == 0
                        , () -> new ValidationError(TASK_ERROR, format("В операционном дне '%s' имеются необработанные выгрузки", onlyCurrentDateString)));
                Assert.isTrue(controller.getAlreadyHeaderCount(operday.getLastWorkingDay(), NEW_ACCOUNTS
                        , DwhUnloadStatus.STARTED, SUCCEDED, PROCESSING) == 0
                        , () -> new ValidationError(TASK_ERROR, format("В операционном дне '%s' имеются необработанные выгрузки", dateUtils.onlyDateString(operday.getLastWorkingDay()))));

                Assert.isTrue(operday.getPhase() == ONLINE
                    , () -> new ValidationError(TASK_ERROR, format("'Операционный день %s' должен быть в фазе '%s', фактическая фаза '%s'"
                                , onlyCurrentDateString, ONLINE, operday.getPhase())));

                return true;
            } catch (ValidationError e) {
                auditController.warning(NewAccounts, "Невозможно выгрузить новые счета в STAMT", null, e);
                return false;
            }
        } else {
            return true;
        }
    }
}
