package ru.rbt.barsgl.ejb.controller.operday.task.balsep;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.task.AccountBalanceUnloadTask;
import ru.rbt.barsgl.ejb.controller.operday.task.DwhUnloadParams;
import ru.rbt.barsgl.ejb.controller.operday.task.TaskUtils;
import ru.rbt.ejbcore.controller.etc.TextResourceController;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.barsgl.shared.enums.EnumUtils;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.COB;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus.*;
import static ru.rbt.audit.entity.AuditRecord.LogCode.AccountBalanceUnload;
import static ru.rbt.audit.entity.AuditRecord.LogCode.DwhUnloadPosting;
import static ru.rbt.ejbcore.validation.ErrorCode.ALREADY_UNLOADED;
import static ru.rbt.ejbcore.validation.ErrorCode.OPERDAY_STATE_INVALID;

/**
 * Created by Ivan Sevastyanov
 * Выгрузка счетов зарегистрированных в Bars GL (DWH.GL_ACC)
 */
public class AccountBalanceRegisteredUnloadTask implements ParamsAwareRunnable {

    public static final String CHECK_RUN_KEY = "checkRun";

    private static final Logger logger = Logger.getLogger(AccountBalanceRegisteredUnloadTask.class.getName());

    @Inject
    private AccountBalanceUnloadTask legecyTask;

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @EJB
    private AuditController auditController;

    @Inject
    private OperdayController operdayController;

    @Inject
    private DateUtils dateUtils;

    @EJB
    private GLOperationRepository repository;

    @Inject
    private TextResourceController textResourceController;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        java.util.Date executeDate = TaskUtils.getDateFromGLOD(properties, repository, operdayController.getOperday());
        if (checkRun(executeDate, properties)) {
            repository.executeInNewTransaction(p1 -> {
                auditController.info(AccountBalanceUnload
                        , format("Начало выгрузка в DWH остатков/оборотов по счетам зарег-ым в GL за дату '%s'", executeDate));
                long headerId = 0;
                try {
                    headerId = legecyTask.createHeaders(DwhUnloadParams.UnloadBalanceRegistered, executeDate);
                    moveToHistory();
                    legecyTask.clearAll();
                    fillRegistered(executeDate);
                    legecyTask.setResultStatus(headerId, SUCCEDED);
                    auditController.info(AccountBalanceUnload
                        , "Успешное завершение выгрузка остатков/оборотов по счетам GL и совместно используемым с Майдас");
                } catch (Exception e) {
                    auditController.error(AccountBalanceUnload, "Ошибка выгрузки остатков оборотов в DWH: ", null, e);
                    if (0 < headerId) {
                        legecyTask.setResultStatus(headerId, ERROR);
                    }
                }
                return null;
            });
        }
    }

    /*don't used GL_SHACOD*/
    //*
    public void fillRegistered(java.util.Date executeDate) throws Exception {
        logger.info("Registered rows: " +
                        beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence, connection) -> {
                            String select = textResourceController.getContent(
                                "ru/rbt/barsgl/ejb/controller/operday/task/balsep/fill_reg_select.sql");
                            repository.executeNativeUpdate(
                                "declare global temporary table GLVD_BAL as (\n" + select.replace("?", "'2016-01-01'") + "\n) DEFINITION ONLY\n" +
                                    "with replace  on commit preserve rows");
                            repository.executeNativeUpdate("insert into session.GLVD_BAL " + select, executeDate);

                            String bsaacid, prebsaacid = "";
                            Date bdat, prebdat = null, cdat;
                            Long closeBalance = new Long("0");
                            Long closeBalanceRur = new Long("0");

                            try (PreparedStatement statement = connection.prepareStatement("select * from SESSION.GLVD_BAL order by bsaacid, cdat")
                                 ; ResultSet rs = statement.executeQuery()) {
                                while (rs.next()) {
                                    bsaacid = rs.getString("bsaacid");
                                    bdat = rs.getDate("bdat");
                                    cdat = rs.getDate("cdat");
                                    if (prebsaacid.equals(bsaacid) && bdat.equals(prebdat)) {
                                        repository.executeNativeUpdate(
                                            "update SESSION.GLVD_BAL set obal = ?, obalrur = ?, dtrn = 0, ctrn = 0, dtrnrur = 0, ctrnrur = 0 where cdat = ? and bsaacid = ?"
                                            , closeBalance, closeBalanceRur, cdat, bsaacid);
                                    } else {
                                        closeBalance = rs.getLong("obal") + rs.getLong("dtrn") + rs.getLong("ctrn");
                                        closeBalanceRur = rs.getLong("obalrur") + rs.getLong("dtrnrur") + rs.getLong("ctrnrur");
                                    }
                                    prebsaacid = bsaacid;
                                    prebdat = bdat;
                                }

                                return repository.executeNativeUpdate(textResourceController.getContent(
                                    "ru/rbt/barsgl/ejb/controller/operday/task/balsep/fill_reg_result.sql"));
                            }
                        }));

        //
        // Добавлено 2016.06.17
        //
        logger.info("Registered rows (substep): " +
                        beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence, connection) -> {
                            String select = textResourceController.getContent(
                                "ru/rbt/barsgl/ejb/controller/operday/task/balsep/fill_reg_select.sql");
                            repository.executeNativeUpdate(
                                "declare global temporary table GLVD_BAL_SUBSTEP1 as (\n" + select.replace("?", "'2016-01-01'") + "\n) DEFINITION ONLY\n" +
                                    "with replace  on commit preserve rows");
                            repository.executeNativeUpdate(textResourceController.getContent(
                                "ru/rbt/barsgl/ejb/controller/operday/task/balsep/fill_reg_insert_substep1.sql"), executeDate);
                            repository.executeNativeUpdate(
                                "UPDATE SESSION.GLVD_BAL_SUBSTEP1 SET OBAL = OBAL+DTRN+CTRN, " +
                                    "OBALRUR = OBALRUR+DTRNRUR+CTRNRUR, DTRN = 0, CTRN = 0, DTRNRUR = 0, CTRNRUR = 0 WHERE CDAT<>BDAT");

                            return repository.executeNativeUpdate(textResourceController.getContent(
                                "ru/rbt/barsgl/ejb/controller/operday/task/balsep/fill_reg_result_substep1.sql"));
                        }));
    }
    //*/

    private boolean checkRun(java.util.Date executeDate, Properties properties) throws Exception {
        try {
            if (Boolean.parseBoolean(Optional.ofNullable(properties.getProperty(CHECK_RUN_KEY)).orElse("true"))) {

                Assert.isTrue(0 == TaskUtils.getDwhAlreadyHeaderCount(executeDate, DwhUnloadParams.UnloadBalanceRegistered, repository)
                        , () -> new ValidationError(ALREADY_UNLOADED
                                , format("Выгрузка остатков по зарег.счетам невозможна: выгрузка уже запущена или выполнена в текущем ОД (%s)"
                                , dateUtils.onlyDateString(executeDate))));

                Operday.OperdayPhase[] phases = new Operday.OperdayPhase[] {COB, ONLINE};
                Assert.isTrue(EnumUtils.contains(phases, operdayController.getOperday().getPhase())
                        , () -> new ValidationError(OPERDAY_STATE_INVALID, operdayController.getOperday().getPhase().name(), Arrays.toString(phases)));
            }
            return true;
        } catch (ValidationError e) {
            auditController.warning(DwhUnloadPosting, format("Невозможно выгрузить остатки по зарегистрированным счетам за '%s'"
                    , dateUtils.onlyDateString(executeDate)), null, e);
            return false;
        }
    }

    /**
     * сохранение предыдущей выгрузки для истории
     */
    private void moveToHistory() throws Exception {
        repository.executeInNewTransaction(persistence -> {
            logger.info("Rows saved from previous unloading: " + repository.executeNativeUpdate(
                "insert into GLVD_BAL_H (DAT,ACID,BSAACID,GLACID,OBAL,DTRN,CTRN,DTRNBD,CTRNBD,DTRNMID,CTRNMID,UNLOAD_DAT,OBALRUR,DTRNRUR,CTRNRUR,DTRNMIDRUR,CTRNMIDRUR)" +
                    "(select DAT,ACID,BSAACID,GLACID,OBAL,DTRN,CTRN,DTRNBD,CTRNBD,DTRNMID,CTRNMID,UNLOAD_DAT,OBALRUR,DTRNRUR,CTRNRUR,DTRNMIDRUR,CTRNMIDRUR from GLVD_BAL)")
                            + " is moved to balance history");
            return null;
        });
    }

}
