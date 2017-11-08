package ru.rbt.barsgl.ejb.controller.operday.task.stamt;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.task.TaskUtils;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.barsgl.shared.enums.EnumUtils;
import ru.rbt.ejbcore.controller.etc.TextResourceController;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.StamtUnload;
import static ru.rbt.audit.entity.AuditRecord.LogCode.StamtUnloadBalFull;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.COB;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.ejbcore.validation.ErrorCode.ALREADY_UNLOADED;
import static ru.rbt.ejbcore.validation.ErrorCode.OPERDAY_STATE_INVALID;

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
            return repository.executeNativeUpdate(textResourceController.getContent("ru/rbt/barsgl/ejb/etc/resource/stm/stmbal_cur_select.sql"), executeDate);
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
            return repository.executeTransactionally((conn)-> {
                try (CallableStatement statement = conn.prepareCall("{ CALL GL_STMBALANCE_BAK(?, ?) }")){
                    statement.setDate(1, new java.sql.Date(executeDate.getTime()));
                    statement.registerOutParameter(2, java.sql.Types.INTEGER);
                    statement.execute();
                    return statement.getInt(2);
                }
            });
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
