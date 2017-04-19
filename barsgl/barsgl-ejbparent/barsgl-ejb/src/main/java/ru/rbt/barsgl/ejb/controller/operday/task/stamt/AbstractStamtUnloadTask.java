package ru.rbt.barsgl.ejb.controller.operday.task.stamt;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.controller.operday.task.TaskUtils;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.ejbcore.controller.etc.TextResourceController;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.barsgl.ejbcore.page.SQL;
import ru.rbt.barsgl.ejbcore.page.WhereInterpreter;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.barsgl.shared.criteria.Criteria;
import ru.rbt.barsgl.shared.enums.EnumUtils;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.COB;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.ejbcore.validation.ErrorCode.ALREADY_UNLOADED;
import static ru.rbt.ejbcore.validation.ErrorCode.OPERDAY_STATE_INVALID;

/**
 * Created by ER21006 on 19.01.2016.
 */
public abstract class AbstractStamtUnloadTask implements ParamsAwareRunnable {


    @Inject
    private OperdayController operdayController;

    @EJB
    protected CoreRepository repository;

    @EJB
    protected AuditController auditController;

    @Inject
    private ru.rbt.ejbcore.util.DateUtils dateUtils;

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @Inject
    private StamtUnloadController unloadController;

    @Inject
    private TextResourceController textController;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        if (checkRun(properties)) {
            final Date executeDate = getExecuteDate(properties);
            long headerId = unloadController.createHeader(executeDate, getUnloadType());
            try {
                preUnload();
                auditController.info(getLogCode(), format("Начало выгрузки в STAMT за дату: '%s'", dateUtils.onlyDateString(executeDate)));
                auditController.info(getLogCode(), format("Удалено старых проводок: '%s'", cleanOld()));
                auditController.info(getLogCode(), format("Выгружено проводок для STAMT в текущем ОД: '%s'", fillData(executeDate)));
                unloadController.setHeaderStatus(headerId, DwhUnloadStatus.SUCCEDED);
                postUnload();
                auditController.info(getLogCode(), format("Выполнена выгрузка в STAMT  за '%s'", dateUtils.onlyDateString(executeDate)));
            } catch (Exception e) {
                unloadController.setHeaderStatus(headerId, DwhUnloadStatus.ERROR);
                auditController.error(getLogCode(), format("Ошибка при выгрузке в STAMT  за '%s'", dateUtils.onlyDateString(executeDate)), null, e);
            }
        }
    }


    protected abstract Criteria buildOperationCriteria(Date operday);

    protected abstract String getTargetTableName();

    protected abstract AuditRecord.LogCode getLogCode();

    protected abstract UnloadStamtParams getUnloadType();

    /**
     * выполняется перед началом выгрузки
     */
    protected void preUnload() {}

    /**
     * выполняется после успешной выгрузки
     */
    protected void postUnload() {}

    public boolean checkRun(Properties properties) throws Exception {
        try {
            if (Boolean.parseBoolean(Optional.ofNullable(properties.getProperty("checkRun")).orElse("true"))) {
                Operday.OperdayPhase[] phases = new Operday.OperdayPhase[] {COB, ONLINE};
                Assert.isTrue(EnumUtils.contains(phases, operdayController.getOperday().getPhase())
                    , () -> new ValidationError(OPERDAY_STATE_INVALID, operdayController.getOperday().getPhase().name()
                                , Arrays.toString(phases)));

                Assert.isTrue(unloadController.getAlreadyHeaderCount(getExecuteDate(properties), getUnloadType()) == 0
                        , () -> new ValidationError(ALREADY_UNLOADED, format("STAMT. Выгрузка счетов (%s) уже произведена", getUnloadType().name())));
            }
            return true;
        } catch (ValidationError e) {
            auditController.warning(getLogCode(), "Невозможно выполнить выгрузку в STAMT ", null, e);
            return false;
        }
    }

    private int fillData(Date operday) throws Exception {
        return beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) ->
        {
            SQL operationSql = WhereInterpreter.interpret(buildOperationCriteria(operday), "o");
            String sql = textController.getContent("ru/rbt/barsgl/ejb/controller/operday/task/stamt/stamt_unload.sql")
                    .replace("$date_criteria$", operationSql.getQuery()).replace("$tablename$", getTargetTableName());
             return repository.executeNativeUpdate(sql, operationSql.getParams());
        } , 60 * 60);
    }


    public final int cleanOld() throws Exception {
        return (Integer)repository.executeInNewTransaction(persistence -> repository.executeNativeUpdate("DELETE FROM " + getTargetTableName())
        );
    }

    private Date getExecuteDate(Properties properties) throws SQLException, ParseException {
        return TaskUtils.getDateFromGLOD(properties, repository, operdayController.getOperday());
    }

}
