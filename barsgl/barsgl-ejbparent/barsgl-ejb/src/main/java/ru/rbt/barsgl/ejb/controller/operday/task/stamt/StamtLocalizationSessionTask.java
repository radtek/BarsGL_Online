package ru.rbt.barsgl.ejb.controller.operday.task.stamt;

import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.ejbcore.controller.etc.TextResourceController;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.xml.bind.ValidationException;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static java.lang.String.format;
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

    @Override
    protected boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        return true;
    }

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        // check balance mode
        try {
            final String balanceMode = jobHistoryRepository.selectOne("select GLAQ_PKG_UTL.GET_CURRENT_BAL_STATE BALANCE_MODE from dual").getString("BALANCE_MODE");
            Assert.isTrue(GIBRID.name().equals(balanceMode)
                    , () -> new ValidationError(TASK_ERROR, format("Текущий режим пересчета остатков %s не соответствует заданному: %s", balanceMode, GIBRID)));

            // check gl_bvjrnl entries exists
            Assert.isTrue(0 < jobHistoryRepository.selectOne("select count(1) cnt from gl_bvjrnl where state = ?", NEW.name()).getLong("CNT")
                , () -> new ValidationError(TASK_ERROR, "Нет записей для локализации в журнале"));

            checkBaltur();
        } catch (ValidationException e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    protected void initExec(String jobName, Properties properties) {

    }

    private boolean checkBaltur() throws Exception {
        final Date minDate = calendarDayRepository.getWorkDateBefore(operdayController.getOperday().getCurrentDate(), 5, false);
        List<DataRecord> failedEntries= jobHistoryRepository.select(textResourceController.getContent("ru/rbt/barsgl/ejb/controller/operday/task/stamt/localize_check_balance.sql")
            , minDate, operdayController.getOperday().getCurrentDate(), minDate);
        Assert.isTrue(failedEntries.isEmpty(), () -> new ValidationError(TASK_ERROR, "Обнаружены дубли в балансе банка, выборочно по следующим счетам: "
            + failedEntries.stream().map(d -> format("Счет %s:%s Дата %s Кол-во строк %s"
                    , d.getString("bsaacid"), d.getString("acid"), dateUtils.onlyDateString(d.getDate("dat")), d.getLong("cnt"))).collect(Collectors.joining(",", "<", ">"))));
        return true;
    }


}
