package ru.rbt.barsgl.ejb.controller.operday.task.stamt;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.controller.operday.task.TaskUtils;
import ru.rbt.barsgl.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejbcore.controller.etc.TextResourceController;
import ru.rbt.barsgl.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.barsgl.ejbcore.util.DateUtils;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.Assert;
import ru.rbt.barsgl.shared.enums.EnumUtils;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

import static java.lang.String.format;
import static org.apache.commons.lang3.time.DateUtils.parseDate;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.COB;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.audit.entity.AuditRecord.LogCode.*;
import static ru.rbt.barsgl.ejbcore.validation.ErrorCode.ALREADY_UNLOADED;
import static ru.rbt.barsgl.ejbcore.validation.ErrorCode.OPERDAY_STATE_INVALID;

/**
 * Created by Ivan Sevastyanov on 27.01.2016.
 */
public class StamtUnloadBalanceTask implements ParamsAwareRunnable {

    /**
     * режим зыгрузки остатков
     */
    public enum BalanceDeltaMode {
        /**
         * Удаляем только то, что будем добавлять
         */
        InsertUpdate,
        /**
         * Удаляем все перед заливкой
         */
        Replace
    }

    @EJB
    private CoreRepository repository;

    @EJB
    private AuditController auditController;

    @Inject
    private StamtUnloadController unloadController;

    @Inject
    private DateUtils dateUtils;

    @EJB
    private OperdayController operdayController;

    @Inject
    private TextResourceController textResourceController;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        long headerIdFull = -1;
        Date executeDate = null;
        try {
            executeDate = TaskUtils.getDateFromGLOD(properties, repository, operdayController.getOperday());
        } catch (Throwable e) {
            auditController.error(StamtUnload, "Ошибка определения даты опердня при выгрузке остатков в STAMT. Выгрузка отложена", null, e);
            return;
        }
        auditController.info(StamtUnload
                , format("Начало выгрузки остатков для STAMT (шаг 1) за дату: '%s'", dateUtils.onlyDateString(executeDate)));
        try {
            if (checkRun(executeDate, UnloadStamtParams.BALANCE_FULL)) {
                headerIdFull = unloadController.createHeader(executeDate, UnloadStamtParams.BALANCE_FULL);
                auditController.info(StamtUnloadBalFull, format("Удалено старых записей '%s'", cleanOldFull()));
                auditController.info(StamtUnloadBalFull
                        , format("Выгружено счетов для STAMT за дату: '%s', '%s'", dateUtils.onlyDateString(executeDate), fillDataCurrent(executeDate)));
                unloadController.setHeaderStatus(headerIdFull, DwhUnloadStatus.SUCCEDED);
            }
        } catch (Exception e) {
            unloadController.setHeaderStatus(headerIdFull, DwhUnloadStatus.ERROR);
            auditController.error(StamtUnload
                    , format("Ошибка при выгрузке остатков в STAMT за '%s'", dateUtils.onlyDateString(executeDate)), null, e);
            return;
        }

        auditController.info(StamtUnload
                , format("Окончание выгрузки остатков для STAMT (шаг 1) за дату: '%s'", dateUtils.onlyDateString(executeDate)));
    }

    /**
     * Заполнение проводок в текущем операционном дне
     * @return
     */
    private int fillDataCurrent(Date executeDate) throws Exception {
        return (int)repository.executeInNewTransaction(persistence -> {

            unloadController.createTemporaryTableWithDate("gl_tmp_curdate", "curdate", executeDate);

            return repository.executeNativeUpdate(
                    textResourceController.getContent("ru/rbt/barsgl/ejb/etc/resource/stm/stmbal_cur_select.sql"));
        });
    }

    /**
     * Заполнение проводок backdate
     * @param executeDate дата выполнения (ОД)
     * @param deltaMode
     * @return
     * @throws Exception
     */
    public int fillDataDelta(Date executeDate,  BalanceDeltaMode deltaMode) throws Exception {
        return (int) repository.executeInNewTransaction(persistence -> {

            unloadController.createTemporaryTableWithDate("gl_tmp_curdate", "curdate", executeDate);
            unloadController.createTemporaryTableWithDate("gl_tmp_od", "curdate", operdayController.getOperday().getCurrentDate());

            String select = textResourceController.getContent("ru/rbt/barsgl/ejb/etc/resource/stm/stmbal_delta_select.sql");
            repository.executeNativeUpdate(
                    "declare global temporary table GL_TMP_STMTBAL as (\n" + select + "\n) with data \n" +
                            "with replace  on commit preserve rows");
            auditController.info(StamtUnloadBalDelta, format("Таблица с остатками создана. Кол-во счетов: %s"
                    , repository.selectFirst("select count(1) from session.GL_TMP_STMTBAL").getLong(0)));

            repository.executeNativeUpdate("CREATE UNIQUE INDEX QTEMP.IDX_GL_TMP_STMTBAL ON SESSION.GL_TMP_STMTBAL(STATDATE,CBACCOUNT)");
            repository.executeTransactionally(connection -> {
                String cbacc, precbacc = "";
                java.sql.Date bdat, statdate, prebdat = new java.sql.Date(parseDate("01.01.2030", "dd.MM.yyyy").getTime());
                BigDecimal closeblnca = new BigDecimal("0");
                BigDecimal closeblncn = new BigDecimal("0");
                try (PreparedStatement st = connection.prepareStatement("SELECT * FROM session.GL_TMP_STMTBAL ORDER BY cbaccount, statdate")
                     ; ResultSet rs = st.executeQuery()) {
                    while (rs.next()){
                        cbacc = rs.getString("cbaccount");
                        bdat = rs.getDate("bdat");
                        statdate = rs.getDate("statdate");
                        if (cbacc.equals(precbacc) && bdat.equals(prebdat)) {
                            try (PreparedStatement upd = connection.prepareStatement(textResourceController.getContent("ru/rbt/barsgl/ejb/etc/resource/stm/stmbal_delta_upd.sql"))) {
                                upd.setBigDecimal(1, closeblnca);
                                upd.setBigDecimal(2, closeblncn);
                                upd.setBigDecimal(3, closeblnca);
                                upd.setBigDecimal(4, closeblncn);
                                upd.setBigDecimal(5, new BigDecimal("0.0"));
                                upd.setBigDecimal(6, new BigDecimal("0.0"));
                                upd.setBigDecimal(7, new BigDecimal("0.0"));
                                upd.setBigDecimal(8, new BigDecimal("0.0"));
                                upd.setString(9, cbacc);
                                upd.setDate(10, statdate);
                                upd.executeUpdate();
                            }
                        } else {
                            closeblnca = rs.getBigDecimal("closeblnca");
                            closeblncn = rs.getBigDecimal("closeblncn");
                        }
                        prebdat = bdat;
                        precbacc = cbacc;
                    }
                }
                return null;
            });
            if (deltaMode == BalanceDeltaMode.InsertUpdate) {
                auditController.info(StamtUnloadBalDelta, format("Удалено счетов из GL_BALSTMD записей для обновления '%s'"
                        , repository.executeNativeUpdate(textResourceController
                                .getContent("ru/rbt/barsgl/ejb/controller/operday/task/stamt/stamt_delete_exists.sql"))));
            } else if (deltaMode == BalanceDeltaMode.Replace) {
                auditController.info(StamtUnloadBalDelta, format("Удалено старых записей (BACKDATE) '%s'", cleanOldDelta()));
            }
            return repository.executeNativeUpdate(textResourceController.getContent("ru/rbt/barsgl/ejb/etc/resource/stm/stmbal_delta_ins_res.sql"));
        });
    }

    public boolean checkRun(Date executeDate, UnloadStamtParams params) throws SQLException {
        try {
            Assert.isTrue(unloadController.getAlreadyHeaderCount(executeDate, params) == 0
                    , () -> new ValidationError(ALREADY_UNLOADED, "STAMT. Выгрузка счетов уже произведена"));

            Operday.OperdayPhase[] phases = new Operday.OperdayPhase[] {COB, ONLINE};
            Assert.isTrue(EnumUtils.contains(phases, operdayController.getOperday().getPhase())
                    , () -> new ValidationError(OPERDAY_STATE_INVALID, operdayController.getOperday().getPhase().name()
                            , Arrays.toString(phases)));
            return true;
        } catch (ValidationError e) {
            auditController.warning(AuditRecord.LogCode.StamtUnload
                    , format("Выгрузка остатков по счетам (%s) для STAMT в ОД '%s' невозможна"
                            , params.getParamName(), dateUtils.onlyDateString(executeDate)), null, e);
            return false;
        }
    }

    private int cleanOldFull() throws Exception {
        return (int) repository.executeInNewTransaction(persistence -> {
            return repository.executeNativeUpdate("delete from GL_BALSTM");
        });
    }

    private int cleanOldDelta() throws Exception {
        return (int) repository.executeInNewTransaction(persistence -> {
            return repository.executeNativeUpdate("delete from GL_BALSTMD");
        });
    }
}
