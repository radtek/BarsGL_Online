package ru.rbt.barsgl.ejb.controller.operday.task.dwh;

import org.apache.commons.lang3.time.DateUtils;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.shared.Repository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.AccDealCloseTask;

public class DwhProcessClosedDealsTask extends AbstractJobHistoryAwareTask {

    public enum LoadType {
        Discrete, Full
    }

    public static String LOAD_TYPE_KEY = "Load_Type";
    public static String LOAD_DATE_KEY = "Load_Date";
    public static String DWH_CLOSED_DEALS_STATUS_TABNAME_KEY = "DwhStatus_TabName";
    public static String DWH_CLOSED_DEALS_TABNAME_KEY = "ClosedDeals_TabName";
    public static String DWH_CLOSED_DEALS_STREAM_KEY = "ClosedDeals_StreamId";
    public static String MAP_KEY = "map";

    private enum TaskProcessClosedContext {
        IS_LOAD_DATE_PRESET
        , LOAD_DATE
        , LOAD_TYPE
    }

    public static final Logger log = Logger.getLogger(DwhProcessClosedDealsTask.class.getName());


    @Override
    protected boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        auditController.info(AccDealCloseTask, "Начало выгрузки закрытых сделок из DWH");
        try {
            jobHistoryRepository.executeInNewTransaction(jobHistoryRepository.getPersistence(Repository.BARSGLNOXA), persistence -> {
                jobHistoryRepository.executeTransactionally(jobHistoryRepository.getDataSource(Repository.BARSGLNOXA), connection -> {
                    try (PreparedStatement preparedStatement = connection.prepareStatement(
                            "begin\n" +
                                    "    PKG_DWHDEALS.process_deals(?, ?, ?, ?, ?);\n" +
                                    "end;")){
                        preparedStatement.setDate(1, null != getLoadDateContext(properties) ? new java.sql.Date(getLoadDateContext(properties).getTime()) : null);
                        preparedStatement.setString(2, getLoadTypeContext(properties).name());
                        preparedStatement.setString(3, getDwhClosedDealsStreamId(properties));
                        preparedStatement.setString(4, getDwhClosedDealsStatusTabname(properties));
                        preparedStatement.setString(5, getDwhClosedDealsTabname(properties));
                        preparedStatement.executeUpdate();
                        return null;
                    }
                });
                return null;
            });
            auditController.info(AccDealCloseTask, "Окончание выгрузки закрытых сделок из DWH");
            return true;
        } catch (Throwable e) {
            auditController.error(AccDealCloseTask, "Ошибка при выполнении задачи выгрузки закрытых сделок", null, e);
            return false;
        }
    }

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        try {
            return isLoadDatePresetContext(properties)
                    || getLoadTypeContext(properties) == LoadType.Full
                    || checkMart(getDwhClosedDealsStatusTabname(properties), getDwhClosedDealsStreamId(properties));
        } catch (ValidationError e) {
            auditController.error(AccDealCloseTask, e.getMessage(), null, e);
            return false;
        }
    }

    @Override
    protected boolean checkOk(String jobName, Properties properties) {
        return true;
    }

    @Override
    protected void initExec(String jobName, Properties properties) {
        Map<TaskProcessClosedContext, Object> context = new HashMap<>();
        properties.put(MAP_KEY, context);
        context.put(TaskProcessClosedContext.LOAD_TYPE
                , LoadType.valueOf(Optional.ofNullable(properties.getProperty(LOAD_TYPE_KEY)).orElse(LoadType.Discrete.name())));
        context.put(TaskProcessClosedContext.LOAD_DATE
                , Optional.ofNullable(getLoadDate(properties)).orElseGet(
                        () -> {
                            if (getLoadTypeContext(properties) == LoadType.Full) {
                                return null;
                            } else {
                                return operdayController.getOperday().getLastWorkingDay();
                            }
                        }));
        context.put(TaskProcessClosedContext.IS_LOAD_DATE_PRESET, properties.getProperty(LOAD_DATE_KEY) != null);
    }

    private Date getLoadDate(Properties properties) {
        try {
            return DateUtils.parseDate(properties.getProperty(LOAD_DATE_KEY), "yyyy-MM-dd", "dd.MM.yyyy");
        } catch (Throwable t) {
            log.log(Level.SEVERE, "Ошибка при разборе параметра даты выгрузки: " + t.getMessage(), t);
            return null;
        }
    }

    private Date getLoadDateContext(Properties properties) {
        return (Date) ((Map<TaskProcessClosedContext, Object>)properties.get(MAP_KEY)).get(TaskProcessClosedContext.LOAD_DATE);
    }

    private LoadType getLoadTypeContext(Properties properties) {
        return (LoadType) ((Map<TaskProcessClosedContext, Object>)properties.get(MAP_KEY)).get(TaskProcessClosedContext.LOAD_TYPE);
    }

    private boolean isLoadDatePresetContext(Properties properties) {
        return (Boolean) ((Map<TaskProcessClosedContext, Object>)properties.get(MAP_KEY)).get(TaskProcessClosedContext.IS_LOAD_DATE_PRESET);
    }

    private String getDwhClosedDealsStatusTabname(Properties properties) {
        return Optional.ofNullable(properties.getProperty(DWH_CLOSED_DEALS_STATUS_TABNAME_KEY)).orElseGet(() -> {
            try {
                return jobHistoryRepository.selectFirst("select PKG_DWHDEALS.GET_STATUS_TAB_DEFAULT_NAME tab from dual").getString("tab");
            } catch (SQLException e) {
                throw new DefaultApplicationException(e.getMessage(), e);
            }
        });
    }

    private String getDwhClosedDealsTabname(Properties properties) {
        return Optional.ofNullable(properties.getProperty(DWH_CLOSED_DEALS_TABNAME_KEY)).orElseGet(() -> {
            try {
                return jobHistoryRepository.selectFirst("select PKG_DWHDEALS.GET_CLOSED_TAB_DEFAULT_NAME tab from dual").getString("tab");
            } catch (SQLException e) {
                throw new DefaultApplicationException(e.getMessage(), e);
            }
        });
    }

    private String getDwhClosedDealsStreamId(Properties properties) {
        return Optional.ofNullable(properties.getProperty(DWH_CLOSED_DEALS_STREAM_KEY))
                .orElseThrow(() -> new DefaultApplicationException(String.format("Не задано название потока ключом '%s'", DWH_CLOSED_DEALS_STREAM_KEY)));
    }

    private boolean checkMart(String dwhStatusTableName, String streamId) throws Exception {
        try {
            DataRecord dmartRecord = jobHistoryRepository
                    .selectFirst(jobHistoryRepository.getDataSource(Repository.BARSGLNOXA)
                            , format("with\n" +
                                    " a as (select /*+ materialize */ * from %s)\n" +
                                    "select case when o.lwdate <= t.as_of_date then '1' else '0' end st, as_of_date, stream, nvl(l.STATUS,'NONE') ok_state\n" +
                                    "  from gl_od o, a t, GL_LOADSTAT l\n" +
                                    " where t.STREAM = l.STREAM_ID(+)\n" +
                                    "   and t.STREAM = ?\n" +
                                    "   and t.as_of_date = l.DTL(+)", dwhStatusTableName), streamId);
            Assert.isTrue(null != dmartRecord, ()-> new ValidationError(ErrorCode.TASK_ERROR, "Невозможно проверить статус витрины DWH. Статусная таблица пуста."));
            Assert.isTrue(Objects.equals(dmartRecord.getString("st"), "1")
                    , ()-> new ValidationError(ErrorCode.TASK_ERROR, "Невозможно выгрузить закрытые сделки из DWH. Дата готовности меньше LWDATE"));
//            Assert.isTrue(!Objects.equals(dmartRecord.getString("ok_state"), getOkStatus())
//                    , ()-> new ValidationError(ErrorCode.TASK_ERROR, format("Выгрузка по потоку %s в дате %s уже была проведена успешно", dmartRecord.getString("stream"), dateUtils.onlyDateString(dmartRecord.getDate("as_of_date")))));
            return true;
        } catch (Throwable e) {
            auditController.error(AccDealCloseTask, "Не прошла проверка готовности витрины DWH", null, e);
            return false;
        }
    }

    private String getOkStatus() throws SQLException {
        return jobHistoryRepository.selectFirst("select PKG_DWHDEALS.get_status_ok() col from dual").getString("col");
    }
}
