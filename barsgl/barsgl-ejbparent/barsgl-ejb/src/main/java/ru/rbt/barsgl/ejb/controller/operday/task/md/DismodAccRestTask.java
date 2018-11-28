package ru.rbt.barsgl.ejb.controller.operday.task.md;

import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.shared.HasLabel;
import ru.rbt.barsgl.shared.Repository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DBParam;
import ru.rbt.ejbcore.datarec.DBParams;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import java.sql.SQLException;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.DismodAccRest;
import static ru.rbt.barsgl.ejb.controller.operday.task.md.DismodAccRestTask.DismodOutState.E;
import static ru.rbt.barsgl.ejb.controller.operday.task.md.DismodAccRestTask.DismodOutState.S;
import static ru.rbt.ejbcore.datarec.DBParam.DBParamDirectionType.OUT;
import static ru.rbt.ejbcore.datarec.DBParam.DbParamType.*;
import static ru.rbt.ejbcore.validation.ErrorCode.DISMOD_ERR;

/**
 * Created by Ivan Sevastyanov on 26.11.2018.
 */
public class DismodAccRestTask extends AbstractJobHistoryAwareTask {

    public enum DismodOutState implements HasLabel {
        L("Загружается"), S("Сформирован"), E ("Ошибка");

        private final String label;

        DismodOutState(String name) {
            this.label = name;
        }

        @Override
        public String getLabel() {
            return label;
        }
    }

    public enum DismodParam {
        LOADREST, LOADRESRPOST
    }

    public static final String OUT_PROCESS_NAME = "LoadAccount";

    @Override
    protected boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        long id = createDismodHeader(DismodParam.LOADREST);
        try {
            auditController.info(DismodAccRest, "Начало формирования витрины для DISMOD");
            executeNOXA("begin PKG_MD_ACCOUNT.PROCESS_ACC_REST(); end;");
            auditController.info(DismodAccRest, "Окончание формирования витрины для DISMOD");
            updateDismodHeader(id, S);
            return true;
        } catch (Exception e) {
            auditController.error(DismodAccRest, "Ошибка при формировании витрины для DISMOD", null, e);
            updateDismodHeader(id, E);
            return false;
        }
    }

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        try {
            DataRecord record = selectFirstNOXA(format("select * from %s where operday = ? and status = ? and process_nm = ?"
                    , getOutLogTableName()), operdayController.getOperday().getLastWorkingDay(), S.name(), OUT_PROCESS_NAME);
            Assert.isTrue(null != record,
                    () -> new ValidationError(DISMOD_ERR, format("Не готова витрина счетов %s в модуле дисконтирования", getAccountBasketTableName())));
            return true;
        } catch (ValidationError error) {
            auditController.warning(DismodAccRest, "Фомирование витрины для DISMOD невозможно: " + error.getMessage(), null, error);
            return false;
        }
    }

    @Override
    protected void initExec(String jobName, Properties properties) throws Exception {}

    private DataRecord selectFirstNOXA(String sql, Object ... params) throws SQLException {
        return jobHistoryRepository.selectFirst(jobHistoryRepository.getDataSource(Repository.BARSGLNOXA), sql, params);
    }

    private int executeNOXA(String sql, Object ... params) throws SQLException {
        return jobHistoryRepository.executeNativeUpdate(jobHistoryRepository.getDataSource(Repository.BARSGLNOXA), sql, params);
    }

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

    private long createDismodHeader (DismodParam param) throws Exception {
        return jobHistoryRepository.executeInNewTransaction(persistence -> {
            DBParams result = jobHistoryRepository.executeCallable(
                    "declare\n" +
                    "    l_id number;\n" +
                    "begin\n" +
                    "    insert into gl_md_log (id_key, parname, status, operday, start_load, end_load)\n" +
                    "                    values (seq_dismod.nextval, ?, ?, ?, systimestamp, null) returning id_key into l_id;\n" +
                    "    ? := l_id;\n" +
                    "end;"
                    , DBParams.createParams(new DBParam(VARCHAR, param.name()), new DBParam(VARCHAR, DismodOutState.L.name())
                            , new DBParam(DATE, operdayController.getOperday().getLastWorkingDay()), new DBParam(LONG, OUT)));
            return result.getParams().stream().filter(p -> p.getDirectionType() == OUT).findFirst()
                    .map(p-> (long)p.getValue()).orElseThrow(() -> new DefaultApplicationException("out parameter is not found"));
        });
    }

    private void updateDismodHeader(long id, DismodOutState state) throws Exception {
        jobHistoryRepository.executeInNewTransaction(persistence ->
                jobHistoryRepository.executeNativeUpdate("update gl_md_log set status = ?, end_load = systimestamp where id_key = ?", state.name(), id));
    }

}
