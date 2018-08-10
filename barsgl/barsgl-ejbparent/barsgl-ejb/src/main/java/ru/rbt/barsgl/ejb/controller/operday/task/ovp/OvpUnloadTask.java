package ru.rbt.barsgl.ejb.controller.operday.task.ovp;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.task.TaskUtils;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;

import javax.ejb.EJB;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Created by Ivan Sevastyanov on 29.11.2017.
 */
public class OvpUnloadTask implements ParamsAwareRunnable {

    @EJB
    private CoreRepository repository;

    @EJB
    private AuditController auditController;

    @EJB
    private OperdayController operdayController;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        if (!checkRun(properties)) {
            return;
        }

        try {
            long idposthead = createHeader(OvpUnloadParam.POSTING);
            auditController.info(AuditRecord.LogCode.Ocp, format("Выгружено проводок по ОВП: %s", unloadPostings(operdayController.getOperday().getCurrentDate())));
            updateHeaderState(idposthead, DwhUnloadStatus.SUCCEDED);
        } catch (Throwable e) {
            auditController.error(AuditRecord.LogCode.Ocp, "Ошибка при выгрузке проводок по ОВП. Остатки не выгружаем.", null, e);
            return;
        }

        try {
            long idresthead = createHeader(OvpUnloadParam.REST);
            auditController.info(AuditRecord.LogCode.Ocp, format("Выгружено остатков по ОВП: %s", unloadRest(operdayController.getOperday().getCurrentDate())));
            updateHeaderState(idresthead, DwhUnloadStatus.SUCCEDED);
        } catch (Throwable e) {
            auditController.error(AuditRecord.LogCode.Ocp, "Ошибка при выгрузке остатков по ОВП. Остатки не выгружаем.", null, e);
        }
    }

    public boolean checkRun(Properties properties) throws SQLException {
        try {
            if (TaskUtils.getCheckRun(properties, true)) {
               return checkUnprocessed() && checkOperdayState();
            }
            return true;
        } catch (ValidationError e) {
            auditController.warning(AuditRecord.LogCode.Ocp, "Выгрузка по ОВП невозможна: " + e.getMessage(), null, e);
            return false;
        }
    }

    private boolean checkOperdayState() {
        try {
            Operday operday = operdayController.getOperday();
            Assert.isTrue(operday.getPhase() == Operday.OperdayPhase.ONLINE
                    , () -> new ValidationError(ErrorCode.OCP_UNLOAD_ERR, format("Операционный день в статусе: %s", operday.getPhase())));
            return true;
        } catch (ValidationError e) {
            auditController.warning(AuditRecord.LogCode.Ocp, "Выгрузка по ОВП невозможна: " + e.getMessage(), null, e);
            return false;
        }
    }

    public boolean checkUnprocessed() throws SQLException {
        try {
            List<DataRecord> unprocs = repository.select("select * from V_OVP_NOTPROC");
            Assert.isTrue(unprocs.size() == 0
                    , () -> new ValidationError(ErrorCode.OCP_UNLOAD_ERR, format("Найдены необработанные выгрузки по ОВП: %s"
                            ,unprocs.stream().map(d -> d.getString("id_key") + ":" + d.getString("parname") + ":" + d.getString("pardesc"))
                                    .collect(Collectors.joining(" ")))));
            return true;
        } catch (ValidationError e) {
            auditController.warning(AuditRecord.LogCode.Ocp, "Выгрузка по ОВП невозможна: " + e.getMessage(), null, e);
            return false;
        }
    }

    public long createHeader(OvpUnloadParam param, Date operday) throws Exception {
        return (long) repository.executeInNewTransaction(persistence -> {
            long id = repository.nextId("GL_SEQ_OCPTDS");
            repository.executeNativeUpdate("insert into GL_OCPTDS (ID_KEY,PARNAME,PARVALUE,PARDESC,OPERDAY,START_LOAD,END_LOAD)\n" +
                            "            values (?, ?, ?, ?, ?, systimestamp, null)"
                    , id, param.getParName(), DwhUnloadStatus.STARTED.getFlag(), param.getParDesc(), operday);
            return id;
        });
    }

    private long createHeader(OvpUnloadParam param) throws Exception {
        return createHeader(param, operdayController.getOperday().getCurrentDate());
    }

    public void updateHeaderState(long headerId, DwhUnloadStatus status) throws Exception {
        repository.executeInNewTransaction(persistence -> {
            repository.executeNativeUpdate("update GL_OCPTDS set parvalue = ?, end_load = systimestamp where id_key = ?", status.getFlag(), headerId);
            return null;
        });
    }

    public int unloadPostings(Date executeDate) throws Exception {
        return (int) repository.executeInNewTransaction(persistence -> {
            return repository.executeTransactionally((conn)-> {
                try (CallableStatement statement = conn.prepareCall("{ CALL PKG_OVP.UNLOAD_PST(?, ?) }")){
                    statement.setDate(1, new java.sql.Date(executeDate.getTime()));
                    statement.registerOutParameter(2, Types.INTEGER);
                    statement.execute();
                    return statement.getInt(2);
                }
            });
        });
    }

    public int unloadRest(Date currentDate) throws Exception {
        return (int) repository.executeInNewTransaction(persistence -> {
            return repository.executeTransactionally((conn)-> {
                try (CallableStatement statement = conn.prepareCall("{ CALL PKG_OVP.UNLOAD_REST(?, ?) }")){
                    statement.setDate(1, new java.sql.Date(currentDate.getTime()));
                    statement.registerOutParameter(2, Types.INTEGER);
                    statement.execute();
                    return statement.getInt(2);
                }
            });
        });
    }
}
