package ru.rbt.barsgl.ejb.controller.operday.task.stamt;

import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejbcore.controller.etc.TextResourceController;
import ru.rbt.barsgl.ejb.repository.WorkprocRepository;
import ru.rbt.barsgl.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.barsgl.ejbcore.util.DateUtils;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.Assert;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.PreparedStatement;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.controller.operday.task.stamt.UnloadStamtParams.BALANCE_TECHOVER;
import static ru.rbt.barsgl.ejb.controller.operday.task.stamt.UnloadStamtParams.POSTING_TECHOVER;
import static ru.rbt.barsgl.audit.entity.AuditRecord.LogCode.TechoverTask;
import ru.rbt.barsgl.ejb.controller.operday.task.TaskUtils;
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
                auditController.info(TechoverTask, format("Выгружено проводок по техническому овердрафту '%s' в операционном дне '%s'"
                        , unloadDataPosings(lwDate), dateUtils.onlyDateString(executeDate)));
                unloadController.setHeaderStatus(headerIdFull, DwhUnloadStatus.SUCCEDED);
            } catch (Exception e) {
                unloadController.setHeaderStatus(headerIdFull, DwhUnloadStatus.ERROR);
                auditController.error(TechoverTask
                        , format("Ошибка при выгрузке techover проводок в STAMT за '%s'", dateUtils.onlyDateString(executeDate)), null, e);
                return;
            }
            try {
                headerIdFull = unloadController.createHeader(executeDate, UnloadStamtParams.BALANCE_TECHOVER);
                auditController.info(TechoverTask, format("Выгружено счетов с проводками TECHOVER для STAMT за дату: '%s', '%s'"
                                , dateUtils.onlyDateString(executeDate), unloadBalanceDelta(lwDate)));
                unloadController.setHeaderStatus(headerIdFull, DwhUnloadStatus.SUCCEDED);
            } catch (Exception e) {
                unloadController.setHeaderStatus(headerIdFull, DwhUnloadStatus.ERROR);
                auditController.error(TechoverTask
                        , format("Ошибка при выгрузке techover остатков в STAMT за '%s'", dateUtils.onlyDateString(executeDate)), null, e);
                return;
            }

            auditController.info(TechoverTask, format("Помечено полупроводок как выгруженных по теховердрафту %s в опердне %s "
                    , repository.executeNativeUpdate("update gl_pdjover d set d.unf = 'Y' where d.unf = 'N' and operday >= ?", lwDate)
                    , dateUtils.onlyDateString(executeDate)));

            auditController.info(TechoverTask, format("Выгрузка технических овердрафтов за дату '%s' завершена успешно", dateUtils.onlyDateString(executeDate)));
        }
    }

    private int unloadDataPosings(Date lwdate) throws Exception {
        return (int) repository.executeInNewTransaction(persistence -> repository.executeTransactionally(connection -> {
            try (PreparedStatement st = connection.prepareStatement(resourceController.getContent("ru/rbt/barsgl/ejb/controller/operday/task/stamt/tech/stamt_techover_unload_pst.sql"))){
                st.setDate(1, new java.sql.Date(lwdate.getTime()));
                return st.executeUpdate();
            }
        }));
    }

    private int unloadBalanceDelta(Date lwdate) throws Exception {
        return (int) repository.executeInNewTransaction(persistence -> {
            return fillBalanceDelta(lwdate);
        });
    }

    private boolean checkRun(Properties properties) throws Exception {
        try {
            if (TaskUtils.getCheckRun(properties, true)) {
                Date executeDate = (Date) properties.get(TechoverContext.EXECUTE_DATE);
                Assert.isTrue(0 == unloadController.getAlreadyHeaderCount(executeDate, POSTING_TECHOVER)
                        , () -> new ValidationError(OPERDAY_TASK_ALREADY_EXC, POSTING_TECHOVER.getParamName() + " " + POSTING_TECHOVER.getParamDesc()
                                , dateUtils.onlyDateString(executeDate)));
                Assert.isTrue(0 == unloadController.getAlreadyHeaderCount(executeDate, BALANCE_TECHOVER)
                        , () -> new ValidationError(OPERDAY_TASK_ALREADY_EXC, BALANCE_TECHOVER.getParamName() + " " + BALANCE_TECHOVER.getParamDesc()
                                , dateUtils.onlyDateString(executeDate)));
                final String stepName = Optional.ofNullable(
                        properties.getProperty("stepName")).orElse("MI4GL").trim();
                Assert.isTrue(workprocRepository.isStepOK(stepName, (Date) properties.get(TechoverContext.LWDATE))
                        , () -> new ValidationError(OPERDAY_LDR_STEP_ERR, stepName, dateUtils.onlyDateString((Date) properties.get(TechoverContext.LWDATE))));
                unloadController.checkConsumed();
            }
            return true;
        } catch (ValidationError validationError) {
            auditController.error(TechoverTask, "Невозможно выгрузить теховер в стамт", null, validationError);
            return false;
        }
    }

    private int fillBalanceDelta(Date lwdate) throws Exception {
        repository.executeTransactionally(connection -> {
            repository.executeNativeUpdate(resourceController.getContent("ru/rbt/barsgl/ejb/controller/operday/task/stamt/tech/stamt_techover_tmp.sql"));
            try (PreparedStatement st = connection.prepareStatement(resourceController.getContent("ru/rbt/barsgl/ejb/controller/operday/task/stamt/tech/stamt_techover_insacc.sql"))) {
                st.setDate(1, new java.sql.Date(lwdate.getTime()));
                st.executeUpdate();
            }
            try (PreparedStatement st = connection.prepareStatement(resourceController
                    .getContent("ru/rbt/barsgl/ejb/controller/operday/task/stamt/tech/stamt_techover_baldelta.sql").replaceAll("execdate", dateUtils.dbDateString(lwdate)))) {
                st.executeUpdate();
            }
            return null;
        });
        auditController.info(TechoverTask, format("Удалено счетов из GL_BALSTMD записей для обновления '%s'"
                , repository.executeNativeUpdate(resourceController.getContent("ru/rbt/barsgl/ejb/controller/operday/task/stamt/stamt_delete_exists.sql"))));
        return repository.executeNativeUpdate(resourceController.getContent("ru/rbt/barsgl/ejb/etc/resource/stm/stmbal_delta_ins_res.sql"));
    }
}
