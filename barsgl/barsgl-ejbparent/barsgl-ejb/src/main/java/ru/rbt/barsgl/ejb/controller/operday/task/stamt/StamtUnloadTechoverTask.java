package ru.rbt.barsgl.ejb.controller.operday.task.stamt;

import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.etc.TextResourceController;
import ru.rbt.barsgl.ejb.repository.WorkprocRepository;
import ru.rbt.barsgl.ejb.security.AuditController;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.barsgl.ejbcore.util.DateUtils;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.Assert;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.controller.operday.task.DwhUnloadStatus.SUCCEDED;
import static ru.rbt.barsgl.ejb.controller.operday.task.stamt.UnloadStamtParams.POSTING_TECHOVER;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.TechoverTask;
import static ru.rbt.barsgl.ejbcore.validation.ErrorCode.OPERDAY_LDR_STEP_ERR;
import static ru.rbt.barsgl.ejbcore.validation.ErrorCode.OPERDAY_TASK_ALREADY_EXC;

/**
 * Created by Ivan Sevastyanov on 20.01.2017.
 */
public class StamtUnloadTechoverTask implements ParamsAwareRunnable {

    @EJB
    private AuditController auditController;

    @Inject
    private StamtUnloadController unloadController;

    @Inject
    private DateUtils dateUtils;

    @Inject
    private BankCalendarDayRepository calendarRepository;

    @Inject
    private WorkprocRepository workprocRepository;

    @EJB
    private CoreRepository repository;

    @Inject
    private TextResourceController resourceController;

    @Inject
    private StamtUnloadBalanceTask unloadBalanceTask;

    @Inject
    private StamtUnloadDeltaTask unloadDeltaTask;

    private enum TechoverContext {
        EXECUTE_DATE, LWDATE
    }

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        long headerIdFull = -1;
        Date executeDate = unloadController.getExecuteDate(properties);
        Date lwDate = calendarRepository.getWorkdayBefore(executeDate).getId().getCalendarDate();
        properties.put(TechoverContext.EXECUTE_DATE, executeDate);
        properties.put(TechoverContext.LWDATE, lwDate);
        if (checkRun(properties)) {
            auditController.info(TechoverTask, format("Начало выгрузки технических овердрафтов за дату '%s'", dateUtils.onlyDateString(executeDate)));
            try {
                headerIdFull = unloadController.createHeader(executeDate, UnloadStamtParams.POSTING_TECHOVER);
                auditController.info(TechoverTask, format("Исключаем в текущей выгрузке ранее выгруженные PCID '%s'", unloadController.moveIntrodayHistory()));
                auditController.info(TechoverTask, format("Удалено старых данных '%s'", unloadDeltaTask.cleanOld()));
                unloadDataPosings(executeDate);
                unloadController.setHeaderStatus(headerIdFull, DwhUnloadStatus.SUCCEDED);
            } catch (Exception e) {
                unloadController.setHeaderStatus(headerIdFull, DwhUnloadStatus.ERROR);
                auditController.error(TechoverTask
                        , format("Ошибка при выгрузке techover проводок в STAMT за '%s'", dateUtils.onlyDateString(executeDate)), null, e);
                return;
            }
            try {
                headerIdFull = unloadController.createHeader(executeDate, UnloadStamtParams.BALANCE_TECHOVER);
                unloadBalanceDelta(executeDate);
                unloadController.setHeaderStatus(headerIdFull, DwhUnloadStatus.SUCCEDED);
            } catch (Exception e) {
                unloadController.setHeaderStatus(headerIdFull, DwhUnloadStatus.ERROR);
                auditController.error(TechoverTask
                        , format("Ошибка при выгрузке techover остатков в STAMT за '%s'", dateUtils.onlyDateString(executeDate)), null, e);
                return;
            }

            auditController.info(TechoverTask, format("Помечено полупроводок как выгруженных по теховердрафту %s в опердне %s "
                    , repository.executeNativeUpdate("update gl_pdjover d set d.unf = 'Y' where d.unf = 'N' and operday = ?", executeDate)
                    , dateUtils.onlyDateString(executeDate)));

            auditController.info(TechoverTask, format("Выгрузка технических овердрафтов за дату '%s' завершена успешно", dateUtils.onlyDateString(executeDate)));
        }
    }

    private void unloadDataPosings(Date executeDate) throws Exception {
        int cntTotal = (int) repository.executeInNewTransaction(persistence -> {
            int cnt = repository.executeNativeUpdate(
                    resourceController.getContent("ru/rbt/barsgl/ejb/controller/operday/task/stamt/tech/stamt_techover_unload_pst.sql").replaceAll("execdate", DateUtils.dbDateString(executeDate)));
            return cnt;
        });
        auditController.info(TechoverTask, format("Выгружено проводок по техническому овердрафту '%s' в операционном дне '%s'"
            , cntTotal, dateUtils.onlyDateString(executeDate)));
    }

    private void unloadBalanceDelta(Date executeDate) throws Exception {
        repository.executeInNewTransaction(persistence -> {
            auditController.info(TechoverTask
                    , format("Выгружено счетов с проводками TECHOVER для STAMT за дату: '%s', '%s'"
                            , dateUtils.onlyDateString(executeDate), fillBalanceDelta(executeDate)));
            return null;
        });
    }

    private boolean checkRun(Properties properties) throws Exception {
        try {
            Date executeDate = (Date) properties.get(TechoverContext.EXECUTE_DATE);
            Assert.isTrue(0 == unloadController.getAlreadyHeaderCount(executeDate, POSTING_TECHOVER, SUCCEDED)
                , () -> new ValidationError(OPERDAY_TASK_ALREADY_EXC, POSTING_TECHOVER.getParamName() + " " + POSTING_TECHOVER.getParamDesc()
                            , dateUtils.onlyDateString(executeDate)));
            final String stepName = Optional.ofNullable(
                    properties.getProperty("stepName")).orElse("MI4GL").trim();
            Assert.isTrue(workprocRepository.isStepOK(stepName, (Date) properties.get(TechoverContext.LWDATE))
                , () -> new ValidationError(OPERDAY_LDR_STEP_ERR, stepName, dateUtils.onlyDateString((Date) properties.get(TechoverContext.LWDATE))));
            unloadController.checkConsumed(executeDate);
            return true;
        } catch (ValidationError validationError) {
            auditController.error(TechoverTask, "Невозможно выгрузить теховер в стамт", null, validationError);
            return false;
        }
    }

    private int fillBalanceDelta(Date executeDate) throws Exception {
        repository.executeNativeUpdate(resourceController.getContent("ru/rbt/barsgl/ejb/controller/operday/task/stamt/tech/stamt_techover_tmp.sql"));
        repository.executeNativeUpdate(resourceController.getContent("ru/rbt/barsgl/ejb/controller/operday/task/stamt/tech/stamt_techover_insacc.sql"), executeDate);
        repository.executeNativeUpdate(resourceController
                .getContent("ru/rbt/barsgl/ejb/controller/operday/task/stamt/tech/stamt_techover_baldelta.sql"
                ).replaceAll("execdate", dateUtils.dbDateString(executeDate)));
        auditController.info(TechoverTask, format("Удалено счетов из GL_BALSTMD записей для обновления '%s'"
                , repository.executeNativeUpdate(resourceController
                        .getContent("ru/rbt/barsgl/ejb/controller/operday/task/stamt/stamt_delete_exists.sql"))));
        return repository.executeNativeUpdate(resourceController.getContent("ru/rbt/barsgl/ejb/etc/resource/stm/stmbal_delta_ins_res.sql"));
    }
}
