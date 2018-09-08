package ru.rbt.barsgl.ejb.controller.operday.task.ovp;

import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.task.TaskUtils;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.shared.enums.EnumUtils;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import javax.inject.Inject;
import java.text.ParseException;
import java.util.Date;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.OcpFinal;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.COB;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.ejbcore.validation.ErrorCode.OCP_UNLOAD_ERR;

/**
 * Created by Ivan Sevastyanov on 10.08.2018.
 */
public class OvpUnloadFinalTask extends AbstractJobHistoryAwareTask {

    @Inject
    private OvpUnloadTask originalTask;

    @Inject
    private DateUtils dateUtils;

    @Override
    protected boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {

        Date executeOperday = getOperday(properties);

        try {
            long idposthead = originalTask.createHeader(OvpUnloadParam.FINAL_POSTING, executeOperday);
            auditController.info(AuditRecord.LogCode.Ocp, format("Выгружено проводок по ОВП: %s", originalTask.unloadPostings(executeOperday)));
            originalTask.updateHeaderState(idposthead, DwhUnloadStatus.SUCCEDED);
        } catch (Throwable e) {
            auditController.error(AuditRecord.LogCode.Ocp, "Ошибка при выгрузке проводок по ОВП. Остатки не выгружаем.", null, e);
            return false;
        }

        try {
            long idresthead = originalTask.createHeader(OvpUnloadParam.FINAL_REST, executeOperday);
            auditController.info(AuditRecord.LogCode.Ocp, format("Выгружено остатков по ОВП: %s", originalTask.unloadRest(executeOperday)));
            originalTask.updateHeaderState(idresthead, DwhUnloadStatus.SUCCEDED);
            return true;
        } catch (Throwable e) {
            auditController.error(AuditRecord.LogCode.Ocp, "Ошибка при выгрузке остатков по ОВП. Остатки не выгружаем.", null, e);
            return false;
        }
    }

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        return originalTask.checkUnprocessed() && checkOperdayState();
    }

    @Override
    protected Date getOperday(Properties properties) {
        try {
            return TaskUtils.getExecuteDate("operday", properties, calculateExecuteDate());
        } catch (ParseException e) {
            throw new DefaultApplicationException(e);
        }
    }

    private Date calculateExecuteDate() {
        if (operdayController.getOperday().getPhase() == Operday.OperdayPhase.ONLINE) {
            return operdayController.getOperday().getLastWorkingDay();
        } else {
            return operdayController.getOperday().getCurrentDate();
        }
    }

    private boolean checkOperdayState() {
        try {
            Assert.isTrue(EnumUtils.contains(new Operday.OperdayPhase[] {COB, ONLINE}, operdayController.getOperday().getPhase()),
                    () -> new ValidationError(OCP_UNLOAD_ERR, String.format("Операционный день '%s' в недопустимом статусе: %s"
                            , dateUtils.onlyDateString(operdayController.getOperday().getCurrentDate()), operdayController.getOperday().getPhase())));
            return true;
        } catch (ValidationError e) {
            auditController.warning(OcpFinal, "Выгрузка по ОВП FINAL невозможна: " + e.getMessage(), null, e);
            return false;
        }
    }

    @Override
    protected void initExec(String jobName, Properties properties) throws Exception {}
}
