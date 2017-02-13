package ru.rbt.barsgl.ejb.controller.operday.task.stamt;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.controller.operday.task.TaskUtils;
import ru.rbt.barsgl.ejbcore.controller.etc.TextResourceController;
import ru.rbt.barsgl.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.barsgl.ejbcore.util.DateUtils;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.Assert;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.text.ParseException;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.controller.operday.task.DwhUnloadStatus.STARTED;
import static ru.rbt.barsgl.ejb.controller.operday.task.stamt.UnloadStamtParams.BALANCE_DELTA_INCR;
import static ru.rbt.barsgl.ejb.controller.operday.task.stamt.UnloadStamtParams.DELTA_POSTING_INCR;
import static ru.rbt.barsgl.audit.entity.AuditRecord.LogCode.StamtIncrement;
import static ru.rbt.barsgl.ejbcore.validation.ErrorCode.STAMT_INCR_DELTA;

/**
 * Created by Ivan Sevastyanov on 08.08.2016.
 */
public class StamtUnloadPstIncrementTask implements ParamsAwareRunnable {

    @EJB
    private OperdayController operdayController;

    @EJB
    private AuditController auditController;

    @EJB
    private CoreRepository repository;
    
    @EJB
    private BeanManagedProcessor beanManagedProcessor;
    
    @Inject
    private TextResourceController textResourceController;

    @Inject
    private StamtUnloadController unloadController;

    @Inject
    private DateUtils dateUtils;

    @Inject
    private StamtUnloadBalanceTask unloadBalanceTask;

    @Inject
    private StamtUnloadDeltaTask unloadDeltaTask;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        final Date operday = getOperday(properties);
        if (checkRun(operday, properties)) {
            auditController.info(StamtIncrement, format("Начало инкр.выгрузки за операционный день '%s'", dateUtils.onlyDateString(operday)));
            long headerId = unloadController.createHeader(operday, DELTA_POSTING_INCR);
            // postings
            try {
                auditController.info(StamtIncrement, format("Исключаем в текущей выгрузке ранее выгруженные PCID '%s'", unloadController.moveIntrodayHistory()));
                auditController.info(StamtIncrement, format("Удалено старых данных '%s'", unloadDeltaTask.cleanOld()));
                auditController.info(StamtIncrement, format("Выгружено инкрем. проводок в STAMT: %s", fillBackvalueIncrPostings(operday)));
                unloadController.setHeaderStatus(headerId, DwhUnloadStatus.SUCCEDED);
            } catch (Throwable e) {
                unloadController.setHeaderStatus(headerId, DwhUnloadStatus.ERROR);
                auditController.error(StamtIncrement, format("Ошибка при инкрем. выгрузке проводок в STAMT за %s", dateUtils.onlyDateString(operday)), null, e);
                return;
            }

            // balances
            headerId = unloadController.createHeader(operday, BALANCE_DELTA_INCR);
            try {
                auditController.info(StamtIncrement, format("Выгружено остатков по счетам с проводками backvalue: '%s'"
                        , unloadBalanceTask.fillDataDelta(operday, StamtUnloadBalanceTask.BalanceDeltaMode.InsertUpdate)));
                unloadController.setHeaderStatus(headerId, DwhUnloadStatus.SUCCEDED);
            } catch (Throwable e) {
                unloadController.setHeaderStatus(headerId, DwhUnloadStatus.ERROR);
                auditController.error(StamtIncrement, format("Ошибка при инкрем.выгрузке остатков в STAMT за %s", dateUtils.onlyDateString(operday)), null, e);
            }

        }
    }

    public boolean checkRun(Date operday, Properties properties) throws Exception {
        if (Boolean.parseBoolean(Optional.ofNullable(properties.getProperty("checkRun")).orElse("true"))) {
            try {
                Assert.isTrue(operdayController.getOperday().getPhase() == ONLINE
                        , () -> new ValidationError(STAMT_INCR_DELTA, format("Операционный день в фазе:  %s, ожидалось %s", operdayController.getOperday().getPhase(), ONLINE)));
                Assert.isTrue(0 == unloadController.getAlreadyHeaderCount(operday, DELTA_POSTING_INCR, STARTED)
                        , () -> new ValidationError(STAMT_INCR_DELTA, format("Инкрементальная выгрузка проводок не закончена в '%s'", dateUtils.onlyDateString(operdayController.getOperday().getCurrentDate()))));
                Assert.isTrue(0 == unloadController.getAlreadyHeaderCount(operday, BALANCE_DELTA_INCR, STARTED)
                        , () -> new ValidationError(STAMT_INCR_DELTA, format("Инкрементальная выгрузка остатков не закончена в '%s'", dateUtils.onlyDateString(operdayController.getOperday().getCurrentDate()))));
                return true;
            } catch (Throwable e) {
                auditController.error(StamtIncrement, "Задача инкр.выгрузки backvalue не отработала", null, e);
                return false;
            }
        } else {
            return true;
        }
    }

    private Date getOperday(Properties properties) throws ParseException {
        return TaskUtils.getExecuteDate("operday", properties, operdayController.getOperday().getCurrentDate());
    }
    
    private int fillBackvalueIncrPostings(Date operday) throws Exception {
        return beanManagedProcessor.executeInNewTxWithTimeout(((persistence, connection) -> {
            return repository.executeNativeUpdate(textResourceController.getContent("ru/rbt/barsgl/ejb/controller/operday/task/stamt/stamt_inload_pst_incr.sql"), operday);
        }), 3 * 60 * 60);
    }


}
