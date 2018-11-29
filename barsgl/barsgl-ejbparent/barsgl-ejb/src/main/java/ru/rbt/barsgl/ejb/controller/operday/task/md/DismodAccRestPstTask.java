package ru.rbt.barsgl.ejb.controller.operday.task.md;

import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import javax.inject.Inject;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.DismodAccRestPst;
import static ru.rbt.barsgl.ejb.controller.operday.task.md.DismodOutState.S;
import static ru.rbt.barsgl.ejb.controller.operday.task.md.DismodParam.LOADRESRPOST;
import static ru.rbt.barsgl.ejb.controller.operday.task.md.DismodParam.LOADREST;
import static ru.rbt.ejbcore.validation.ErrorCode.DISMOD_ERR;

/**
 * Created by Ivan Sevastyanov on 29.11.2018.
 * Задача формирования витрины с оборотами по проводкам BACKVALUE для DISMOD
 * получаем счета по dblink из таблицы OUT_ACCOUNT_BASKET по ним берем - заполняется в задаче {@link ru.rbt.barsgl.ejb.controller.operday.task.md.DismodAccRestTask}
 * обороты и исходящие остатки и выкладываем себе в GL_MD_REST_PST
 * за каждый календарный день по счету начиная с минимальной даты проводки и до LWDATE включительно
 * задача должна работать один раз в опердне
 */
public class DismodAccRestPstTask extends AbstractJobHistoryAwareTask {

    @Inject
    private DismodRepository repository;

    @Inject
    private DismodAccRestTask mainTask;

    @Override
    protected boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        long id = repository.createDismodHeader(LOADRESRPOST, operdayController.getOperday().getLastWorkingDay());
        try {
            auditController.info(DismodAccRestPst, "Начало формирования витрины BACKVALUE для DISMOD");
            repository.executeNOXA("begin PKG_MD_ACCOUNT.PROCESS_ACC_REST_PST(); end;");
            auditController.info(DismodAccRestPst, "Окончание формирования витрины BACKVALUE для DISMOD");
            repository.updateDismodHeader(id, S);
            return true;
        } catch (Exception e) {
            auditController.error(DismodAccRestPst, "Ошибка при формировании витрины для DISMOD", null, e);
            repository.updateDismodHeader(id, DismodOutState.E);
            return false;
        }
    }

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        return checkFirstTaskSucceeded() && mainTask.checkDismodOutState();
    }

    @Override
    protected void initExec(String jobName, Properties properties) throws Exception {}

    private boolean checkFirstTaskSucceeded() {
        try {
            Assert.isTrue(repository.getDismodHeader(operdayController.getOperday().getLastWorkingDay(), S, LOADREST).isPresent()
                , () -> new ValidationError(DISMOD_ERR, format("Задача по загрузке счетов из DISMOD еще не выполнена в опердне '%s'", operdayController.getOperday().getLastWorkingDay())));
            return true;
        } catch (Throwable e) {
            auditController.warning(DismodAccRestPst
                    , "Не прошла проверка возможности выполнения формирования витрины по остаткам/обротам backvalue для DISMOD", null, e);
            return false;
        }
    }
}
