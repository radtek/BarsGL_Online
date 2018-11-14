package ru.rbt.barsgl.ejb.controller.operday.task.stamt;

import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import javax.inject.Inject;
import java.sql.SQLException;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.StamtUnloadForce;
import static ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus.ERROR;
import static ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus.SUCCEDED;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.controller.operday.task.stamt.UnloadStamtParams.FORCE_BALANCE_DELTA;
import static ru.rbt.barsgl.ejb.controller.operday.task.stamt.UnloadStamtParams.FORCE_DELTA_POSTING;
import static ru.rbt.ejbcore.validation.ErrorCode.STAMT_UNLOAD_FORCE;

/**
 * Created by Ivan Sevastyanov on 13.11.2018.
 */
public class StamtUnloadPostingForceTask extends AbstractJobHistoryAwareTask {

    @Inject
    private StamtUnloadController unloadController;

    public enum ForceState {
        Y,N,S
    }

    @Override
    protected boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        Long[] headers = unloadController.createHeaders(operdayController.getOperday().getCurrentDate(), FORCE_DELTA_POSTING, FORCE_BALANCE_DELTA);
        try {
            auditController.info(StamtUnloadForce, format("Начало принудительной выгрузки проводок в STAMT. Записей к обработке '%s'", selectCountForProcess()));

            jobHistoryRepository.executeInNewTransaction(persistence -> {
                jobHistoryRepository.executeNativeUpdate("begin PKG_STMT_FORCE.UNLOAD_ALL_SERIAL; end;");
                return null;
            });
            auditController.info(StamtUnloadForce, "Успешное окончание принудительной выгрузки в STAMT");
            unloadController.updateHeaders(headers, SUCCEDED);

            return true;
        } catch (Exception e) {
            unloadController.updateHeaders(headers, ERROR);
            auditController.error(StamtUnloadForce, "Ошибка при принудит. выгрузке в STAMT", null, e);
            return false;
        }
    }

    @Override
    @SuppressWarnings("ALL")
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        try {
            unloadController.checkConsumed(STAMT_UNLOAD_FORCE);
            Assert.isTrue(selectCountForProcess() > 0, () -> new ValidationError(STAMT_UNLOAD_FORCE, "Нет необработанных PCID для принудительной выгрузки"));
            Operday operday = operdayController.getOperday();
            Assert.isTrue(operday.getPhase() == ONLINE
                , () -> new ValidationError(STAMT_UNLOAD_FORCE, format("Операционный день '%s' в недопустимом статусе '%s'"
                            , dateUtils.onlyDateString(operday.getCurrentDate()), operday.getPhase())));
            return true;
        } catch (ValidationError e) {
            auditController.warning(StamtUnloadForce, format("Задача %s не выполнена", jobName), null, e);
            return false;
        }
    }

    @Override
    protected void initExec(String jobName, Properties properties) throws Exception {}

    @SuppressWarnings("All")
    private int selectCountForProcess() throws SQLException {
        return jobHistoryRepository.selectFirst("select count(1) cnt from GL_STMPCID s where s.processed = ?"
                , ForceState.N.name()).getInteger("cnt");
    }
}
