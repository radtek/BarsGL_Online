package ru.rbt.barsgl.ejb.controller.operday.task.stamt;

import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.ejb.props.PropertyName;
import ru.rbt.barsgl.ejb.repository.AqRepository;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.controller.etc.TextResourceController;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.BackgroundLocalization;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.BalanceMode.GIBRID;
import static ru.rbt.barsgl.ejb.entity.gl.BackvalueJournal.BackvalueJournalState.NEW;
import static ru.rbt.ejbcore.validation.ErrorCode.TASK_ERROR;

/**
 * Created by Ivan Sevastyanov on 05.04.2018.
 */
public class StamtLocalizationSessionTask extends AbstractJobHistoryAwareTask {

    @Inject
    private TextResourceController textResourceController;

    @EJB
    private BankCalendarDayRepository calendarDayRepository;

    @Inject
    private DateUtils dateUtils;

    @EJB
    private PropertiesRepository propertiesRepository;

    @Inject
    private AqRepository aqRepository;

    @Override
    protected boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        return true;
    }

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        // check balance mode
        try {
            if (checkBaltur() && checkQueues() && checkCommon()) {
                return true;
            }
            return false;
        } catch (Throwable e) {
            auditController.error(BackgroundLocalization, "Ошибка при выполнении фоновой локализации", null, e);
            return false;
        }
    }

    @Override
    protected void initExec(String jobName, Properties properties) {

    }

    private boolean checkCommon() throws SQLException {
        try {
            final String balanceMode = jobHistoryRepository.selectOne("select GLAQ_PKG_UTL.GET_CURRENT_BAL_STATE BALANCE_MODE from dual").getString("BALANCE_MODE");
            Assert.isTrue(GIBRID.name().equals(balanceMode)
                    , () -> new ValidationError(TASK_ERROR, format("Текущий режим пересчета остатков %s не соответствует заданному: %s", balanceMode, GIBRID)));
            // check gl_bvjrnl entries exists
            Assert.isTrue(0 < jobHistoryRepository.selectOne("select count(1) cnt from gl_bvjrnl where state = ?", NEW.name()).getLong("CNT")
                    , () -> new ValidationError(TASK_ERROR, "Нет записей для локализации в журнале"));
        } catch (ValidationError e) {
            auditController.error(BackgroundLocalization, "Не прошла проверка возможности выполнения задачи фоновой локализации (общие проверки)", null, e);
            return false;
        }
        return true;
    }

    private boolean checkBaltur() throws Exception {
        try {
            final Date minDate = calendarDayRepository.getWorkDateBefore(operdayController.getOperday().getCurrentDate(), 5, false);
            List<DataRecord> failedEntries= jobHistoryRepository.select(textResourceController.getContent("ru/rbt/barsgl/ejb/controller/operday/task/stamt/localize_check_balance.sql")
                , minDate, operdayController.getOperday().getCurrentDate(), minDate);
            Assert.isTrue(failedEntries.isEmpty(), () -> new ValidationError(TASK_ERROR, "Обнаружены дубли в балансе банка, выборочно по следующим счетам: "
                + failedEntries.stream().map(d -> format("Счет %s:%s Дата %s Кол-во строк %s"
                        , d.getString("bsaacid"), d.getString("acid"), dateUtils.onlyDateString(d.getDate("dat")), d.getLong("cnt"))).collect(Collectors.joining(",", "<", ">"))));
        } catch (ValidationError e) {
            auditController.error(BackgroundLocalization, "Не прошла проверка возможности выполнения задачи фоновой локализации (баланс банка)", null, e);
            return false;
        }
        return true;
    }

    @SuppressWarnings("ConstantConditions")
    private boolean checkQueues() throws Exception {
        try {
            List<DataRecord> sqlQueueStats = aqRepository.getQueuesStats();
            checkOneQueue(PropertyName.AQBALANCE_CHECK_MSG_CNT, aqRepository.getNormalQueueName(), sqlQueueStats, "Normal queue");
            checkOneQueue(PropertyName.AQBALANCE_CHECK_EMSG_CNT, aqRepository.getExceptionQueueName(), sqlQueueStats, "Exception queue");
        } catch (ValidationError e) {
            auditController.error(BackgroundLocalization, "Не прошла проверка возможности выполнения задачи фоновой локализации (состояние очередей)", null, e);
            return false;
        }
        return true;
    }

    private void checkOneQueue(PropertyName propertyName, String queueName, List<DataRecord> queueStatistics, String queueType) throws ExecutionException {
        long maxCnt = propertiesRepository.getNumber(propertyName.getName());
        DataRecord statistic = queueStatistics.stream().filter(r -> queueName.equals(r.getString("QUEUE_NAME"))).findAny()
                .orElseThrow(() -> new DefaultApplicationException(format("%s statistics is not found", queueType)));
        Assert.isTrue(maxCnt >= statistic.getLong("WAITING") + statistic.getLong("READY") + statistic.getLong("EXPIRED")
                , () -> new ValidationError(ErrorCode.AQ_COMMON_CODE, format("В очереди %s количество сообщений '%s' больше максимального '%s' для выполнения задачи"
                        , queueType, statistic.getLong("WAITING") + statistic.getLong("READY") + statistic.getLong("EXPIRED"), maxCnt)));
    }

}
