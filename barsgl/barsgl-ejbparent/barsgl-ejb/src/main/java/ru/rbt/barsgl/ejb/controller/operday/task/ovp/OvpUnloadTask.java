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
import ru.rbt.shared.Assert;

import javax.ejb.EJB;
import javax.validation.ValidationException;
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

    public enum OvpUnloadParam {
        POSTING("BARS_GL_OCP_POST", "GL_OCPPOST LOAD")
        , REST("BARS_GL_OCP_REST", "GL_OCPREST LOAD");
        private String parName;
        private String parDesc;

        OvpUnloadParam(String parName, String parDesc) {
            this.parName = parName;
            this.parDesc = parDesc;
        }

        public String getParName() {
            return parName;
        }

        public String getParDesc() {
            return parDesc;
        }
    }

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

    private boolean checkRun(Properties properties) throws SQLException {
        try {
            if (TaskUtils.getCheckRun(properties, true)) {
                List<DataRecord> unprocs = repository.select("select * from V_OVP_NOTPROC");
                Assert.isTrue(unprocs.size() == 0
                        , () -> new ValidationException(format("Найдены необработанные выгрузки по ОВП: %s"
                                ,unprocs.stream().map(d -> d.getString("id_key") + ":" + d.getString("parname") + ":" + d.getString("pardesc"))
                                        .collect(Collectors.joining(" ")))));
                Operday operday = operdayController.getOperday();
                Assert.isTrue(operday.getPhase() == Operday.OperdayPhase.ONLINE
                        , () -> new ValidationException(format("Операционный день в статусе: %s", operday.getPhase())));
            }
            return true;
        } catch (ValidationException e) {
            auditController.warning(AuditRecord.LogCode.Ocp, "Выгрузка по ОВП невозможна: " + e.getMessage());
            return false;
        }
    }

    private long createHeader(OvpUnloadParam param) throws Exception {
        return (long) repository.executeInNewTransaction(persistence -> {
            long id = repository.nextId("GL_SEQ_OCPTDS");
            repository.executeNativeUpdate("insert into GL_OCPTDS (ID_KEY,PARNAME,PARVALUE,PARDESC,OPERDAY,START_LOAD,END_LOAD)\n" +
                            "            values (?, ?, ?, ?, ?, systimestamp, null)"
                    , id, param.getParName(), DwhUnloadStatus.STARTED.getFlag(), param.getParDesc(), operdayController.getOperday().getCurrentDate());
            return id;
        });
    }

    private void updateHeaderState(long headerId, DwhUnloadStatus status) throws Exception {
        repository.executeInNewTransaction(persistence -> {
            repository.executeNativeUpdate("update GL_OCPTDS set parvalue = ?, end_load = systimestamp where id_key = ?", status.getFlag(), headerId);
            return null;
        });
    }

    private int unloadPostings(Date executeDate) throws Exception {
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

    private int unloadRest(Date currentDate) throws Exception {
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
