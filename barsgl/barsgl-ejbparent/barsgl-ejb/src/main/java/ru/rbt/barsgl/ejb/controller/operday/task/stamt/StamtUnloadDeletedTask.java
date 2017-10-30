package ru.rbt.barsgl.ejb.controller.operday.task.stamt;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.controller.operday.task.TaskUtils;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.controller.etc.TextResourceController;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.StamtPstDeleted;
import static ru.rbt.ejbcore.validation.ErrorCode.STAMT_UNLOAD_DELETED;

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
        final long[] headerPstId = {-1};
        final long[] headerBalId = {-1};
        Date operday = (Date) properties.get(UnloadDeletedPostingsContext.OPERDAY);
        try {
            headerPstId[0] = unloadController.createHeader(operday, UnloadStamtParams.POSTING_DELETE);

            auditController.info(StamtPstDeleted, format("Выгружено в STAMT удаленных проводок: %s, ОД: %s"
                    , fillDeltaPostings(properties), dateUtils.onlyDateString(operday)));
            unloadController.setHeaderStatus(headerPstId[0], DwhUnloadStatus.SUCCEDED);

            headerBalId[0] = unloadController.createHeader(operday, UnloadStamtParams.BALANCE_DELTA);
            auditController.info(StamtPstDeleted, format("Выгружено в STAMT остатков по удаленным проводкам: %s, ОД: %s"
                    , fillDeletedBalance(properties), dateUtils.onlyDateString(operday)));
            unloadController.setHeaderStatus(headerBalId[0], DwhUnloadStatus.SUCCEDED);

        } catch (Throwable e) {
            unloadController.setHeaderStatus(headerPstId[0], DwhUnloadStatus.ERROR);
            unloadController.setHeaderStatus(headerBalId[0], DwhUnloadStatus.ERROR);
            auditController.error(StamtPstDeleted, format("Ошибка при выполнении выгрузки проводк/остатков по удаленным проводкам: ОД: %s"
                    , dateUtils.onlyDateString(operday)), null, e);
            return false;
        }
        return true;
    }

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        try {
            return unloadController.checkConsumed(STAMT_UNLOAD_DELETED) && checkNewDeleted(properties);
        } catch (Throwable e) {
            auditController.error(StamtPstDeleted, "Не прошла проверка возможности выполнения выгрузки удаленных проводок в STAMT", null, e);
            return false;
        }
    }

    @Override
    protected boolean checkJobStatus(String jobName, Properties properties) {
        return TaskUtils.getCheckRun(properties, true) ? (checkAlreadyRunning(jobName, properties)) : true;
    }

    @Override
    protected void initExec(String jobName, Properties properties) {
        properties.put(UnloadDeletedPostingsContext.OPERDAY, getOperday(properties));
        properties.put(UnloadDeletedPostingsContext.LWDATE, operdayController.getOperday().getLastWorkingDay());
    }

    @Override
    protected Date getOperday(Properties properties) {
        try {
            return TaskUtils.getExecuteDate("operday", properties, operdayController.getOperday().getCurrentDate());
        } catch (ParseException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Это первый запуск задачи?
     * @param properties
     * @return true, если первый запуск
     * @throws SQLException
     */

    private boolean isFirstUnload(Properties properties) throws SQLException {
        return 1 == repository.selectFirst("select count(1) cnt from gl_sched_h where operday = ? and sched_name = ?"
                , properties.get(UnloadDeletedPostingsContext.OPERDAY), StamtUnloadDeletedTask.class.getSimpleName()).getInteger("cnt");
    }

    private void deleteOld(Properties properties) throws SQLException {
        if (isFirstUnload(properties)) {
            auditController.info(StamtPstDeleted
                    , format("Первый запуск задачи выгрузки удаленных проводок в STAMT в ОД '%s'. Удалено строк '%s'"
                            , dateUtils.onlyDateString((Date) properties.get(UnloadDeletedPostingsContext.OPERDAY)), repository.executeNativeUpdate("delete from GL_STMDEL")));
        }
    }

    private boolean checkNewDeleted(Properties properties) throws IOException, SQLException {
        try {
            Date operday = (Date) properties.get(UnloadDeletedPostingsContext.OPERDAY);
            Date lwDate = (Date) properties.get(UnloadDeletedPostingsContext.LWDATE);
            long cnt = repository.selectFirst(resourceController.getContent("ru/rbt/barsgl/ejb/controller/operday/task/stamt/del/stamt_deleted_count.sql")
                    .replaceAll("\\?1", "'" + DateUtils.dbDateString(lwDate) + "'")
                    .replaceAll("\\?2", "'" + DateUtils.dbDateString(operday) + "'")).getLong("cnt");
            Assert.isTrue(cnt > 0, () -> new ValidationError(STAMT_UNLOAD_DELETED, format("Нет удаленных проводок для выгрузки в STAMT. ОД %s", dateUtils.onlyDateString(operday))));
            auditController.info(StamtPstDeleted, format("Найдены потенциально новые удаленные проводки. ОД: %s, кол-во: %s", dateUtils.onlyDateString(operday), cnt));
            return true;
        } catch (Throwable e) {
            auditController.error(StamtPstDeleted, "Не прошла проверка возможности выполнения задачи выгрузки удаленных остатков", null, e);
            return false;
        }
    }

    /**
     * остатки по удаленным счетам всегда отдаем по всем записям в таблице GL_STMDEL
     * @return кол-во строк - счет/дата
     */
    private int fillDeletedBalance(Properties properties) throws Exception {
        try {
            return (int) repository.executeInNewTransaction(persistence -> {
                repository.executeNativeUpdate(resourceController.getContent("ru/rbt/barsgl/ejb/controller/operday/task/stamt/del/stamt_deleted_balance.sql"));
                return repository.executeNativeUpdate(resourceController.getContent("ru/rbt/barsgl/ejb/etc/resource/stm/stmbal_delta_ins_res.sql"));
            });
        } catch (Throwable e) {
            Date operday = (Date) properties.get(UnloadDeletedPostingsContext.OPERDAY);
            auditController.error(StamtPstDeleted, format("Ошибка при выгрузке остатков по удаленным проводкам для STAMT для ОД %s", dateUtils.onlyDateString(operday)), null, e);
            throw e;
        }
    }

    private int fillDeltaPostings(Properties properties) throws Exception {
        Date operday = (Date) properties.get(UnloadDeletedPostingsContext.OPERDAY);
        Date lwDate = (Date) properties.get(UnloadDeletedPostingsContext.LWDATE);
        try {
            return (int) repository.executeInNewTransaction(persistence -> {
                auditController.info(StamtPstDeleted, format("Начало выгрузки по удаленым проводкам за ОД %s", dateUtils.onlyDateString(operday)));
                deleteOld(properties);
                repository.executeNativeUpdate(resourceController
                        .getContent("ru/rbt/barsgl/ejb/controller/operday/task/stamt/del/stamt_deleted_pcid.sql")
                        .replaceAll("\\?1", "'" + DateUtils.dbDateString(lwDate) + "'")
                        .replaceAll("\\?2", "'" + DateUtils.dbDateString(operday) + "'")
                );
                auditController.info(StamtPstDeleted, format("Сессионная таблица с проводками создана. Записей '%s'", repository.selectFirst("select count(1) cnt from TMP_PCID_DEL").getLong("cnt")));
                return repository.executeNativeUpdate(resourceController.getContent("ru/rbt/barsgl/ejb/controller/operday/task/stamt/del/stamt_deleted_result.sql"));
            });
        } catch (Throwable e) {
            auditController.error(StamtPstDeleted, format("Ошибка при выгрузке удаленных проводок в STAMT. ОД: %s", dateUtils.onlyDateString(operday)), null, e);
            throw e;
        }
    }
}
