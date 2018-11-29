package ru.rbt.barsgl.ejb.controller.operday.task.md;

import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import javax.inject.Inject;
import java.sql.SQLException;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.DismodAccRest;
import static ru.rbt.barsgl.ejb.controller.operday.task.md.DismodOutState.S;
import static ru.rbt.barsgl.ejb.controller.operday.task.md.DismodParam.LOADREST;
import static ru.rbt.ejbcore.validation.ErrorCode.DISMOD_ERR;

/**
 * Created by Ivan Sevastyanov on 26.11.2018.
 * Задача формирования витрины с оборотами в LWDATE для DISMOD
 * получаем счета по dblink из таблицы OUT_ACCOUNT_BASKET по ним берем
 * обороты и исходящие остатки и выкладываем себе в GL_MD_REST
 * задача должна работать один раз в опердне
 */
public class DismodAccRestTask extends AbstractJobHistoryAwareTask {

    @Inject
    private DismodRepository dismodRepository;

    public static final String OUT_PROCESS_NAME = "LoadAccount";

    @Override
    protected boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        long id = dismodRepository.createDismodHeader(LOADREST, operdayController.getOperday().getLastWorkingDay());
        try {
            auditController.info(DismodAccRest, "Начало формирования витрины для DISMOD");
            dismodRepository.executeNOXA("begin PKG_MD_ACCOUNT.PROCESS_ACC_REST(); end;");
            auditController.info(DismodAccRest, "Окончание формирования витрины для DISMOD");
            dismodRepository.updateDismodHeader(id, S);
            return true;
        } catch (Exception e) {
            auditController.error(DismodAccRest, "Ошибка при формировании витрины для DISMOD", null, e);
            dismodRepository.updateDismodHeader(id, DismodOutState.E);
            return false;
        }
    }

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        return checkDismodOutState();
    }

    @Override
    protected void initExec(String jobName, Properties properties) throws Exception {}

    private String getOutLogTableName () throws SQLException {
        return jobHistoryRepository.selectFirst("select PKG_MD_ACCOUNT.GET_OUT_LOG_TAB_NAME() nm from dual").getString("nm");
    }

    private String getAccountBasketTableName () {
        try {
            return jobHistoryRepository.selectFirst("select PKG_MD_ACCOUNT.GET_ACCOUNT_BASKET_TAB_NAME() nm from dual").getString("nm");
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public boolean checkDismodOutState() {
        try {
            Assert.isTrue(dismodRepository.getDismodOutState(getOutLogTableName(), OUT_PROCESS_NAME, operdayController.getOperday().getLastWorkingDay()).isPresent(),
                    () -> new ValidationError(DISMOD_ERR, format("Не готова витрина счетов %s в модуле дисконтирования за опердень '%s'", getAccountBasketTableName(), operdayController.getOperday().getLastWorkingDay())));
            return true;
        } catch (Throwable e) {
            auditController.warning(DismodAccRest, "Фомирование витрины для DISMOD невозможно: " + e.getMessage(), null, e);
            return false;
        }
    }

}
