package ru.rbt.barsgl.ejb.controller.operday.task.stamt;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.controller.operday.task.TaskUtils;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejbcore.controller.etc.TextResourceController;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.StamtPstDeleted;

/**
 * Created by er21006 on 08.08.2017.
 */
public class StamtUnloadDeletedTask extends AbstractJobHistoryAwareTask {

    @EJB
    private CoreRepository repository;

    @Inject
    private TextResourceController resourceController;

    @EJB
    private AuditController auditController;

    @Inject
    private StamtUnloadController unloadController;

    private enum UnloadDeletedPostingsContext {
        OPERDAY, LWDATE
    }

    @Override
    protected boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        Date operday = (Date) properties.get(UnloadDeletedPostingsContext.OPERDAY);
        Date lwDate = (Date) properties.get(UnloadDeletedPostingsContext.LWDATE);
        final long[] headerId = {-1};
        try {
            repository.executeInNewTransaction(persistence -> {
                auditController.info(StamtPstDeleted, format("Начало выгрузки по удаленым проводкам за ОД %s", dateUtils.onlyDateString(operday)));
                headerId[0] = unloadController.createHeader(operday, UnloadStamtParams.POSTING_DELETE);
                deleteOld(properties);
                repository.executeNativeUpdate(resourceController
                        .getContent("ru/rbt/barsgl/ejb/controller/operday/task/stamt/del/stamt_deleted_pcid.sql")
                        .replaceAll("\\?1", "'" + DateUtils.dbDateString(lwDate) + "'")
                        .replaceAll("\\?2", "'" + DateUtils.dbDateString(operday) + "'")
                );
                auditController.info(StamtPstDeleted, format("Сессионная таблица с проводками создана. Записей '%s'", repository.selectFirst("select count(1) cnt from session.TMP_PCID_DEL").getLong("cnt")));
                repository.executeNativeUpdate("create index QTEMP.UN_TMP_IDX1 on SESSION.TMP_PCID_DEL (PCID)");
                repository.executeNativeUpdate("create unique index QTEMP.UN_TMP_IDX2 on SESSION.TMP_PCID_DEL (PID)");
                auditController.info(StamtPstDeleted, format("Выгружено в STAMT удаленных проводок: %s "
                        , repository.executeNativeUpdate(resourceController.getContent("ru/rbt/barsgl/ejb/controller/operday/task/stamt/del/stamt_deleted_result.sql"))));
                unloadController.setHeaderStatus(headerId[0], DwhUnloadStatus.SUCCEDED);
                return null;
            });
        } catch (Throwable e) {
            unloadController.setHeaderStatus(headerId[0], DwhUnloadStatus.ERROR);
            auditController.error(StamtPstDeleted, "Ошибка при выгрузке удаленных проводок в STAMT", null, e);
            return false;
        }
        return true;
    }

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        return unloadController.checkConsumed();
    }

    @Override
    protected boolean checkJobStatus(String jobName, Properties properties) {
        return TaskUtils.getCheckRun(properties, true) ? checkAlreadyRunning(jobName, properties) : true;
    }

    @Override
    protected void initExec(String jobName, Properties properties) {
        properties.put(UnloadDeletedPostingsContext.OPERDAY, operdayController.getOperday().getCurrentDate());
        properties.put(UnloadDeletedPostingsContext.LWDATE, operdayController.getOperday().getLastWorkingDay());
    }

    @Override
    protected Date getOperday(Properties properties) {
        return operdayController.getOperday().getCurrentDate();
    }

    /**
     * Это первый запуск задачи?
     * @param properties
     * @return true, если первый запуск
     * @throws SQLException
     */

    private boolean isFirstUnload(Properties properties) throws SQLException {
        return 1 == repository.selectFirst("select count(1) cnt from gl_sched_h where operday = ?"
                , properties.get(UnloadDeletedPostingsContext.OPERDAY)).getInteger("cnt");
    }

    private void deleteOld(Properties properties) throws SQLException {
        if (isFirstUnload(properties)) {
            auditController.info(StamtPstDeleted
                    , format("Первый запуск задачи выгрузки удаленных проводок в STAMT в ОД '%s'. Удалено строк '%s'"
                            , dateUtils.onlyDateString((Date) properties.get(UnloadDeletedPostingsContext.OPERDAY)), repository.executeNativeUpdate("delete from GL_STMDEL")));
        }
    }
}
