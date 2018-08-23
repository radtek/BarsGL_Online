package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.ejb.repository.Reg47422JournalRepository;
import ru.rbt.barsgl.ejb.repository.dict.ClosedPeriodCashedRepository;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.MakeInvisible47422;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.props.PropertyName.REG47422_DEPTH;

/**
 * Created by er18837 on 20.08.2018.
 */
public class MakeInvisible47422Task extends AbstractJobHistoryAwareTask {

    private static final String myTaskName = CloseLwdBalanceCutTask.class.getSimpleName();

    public static final String REG47422_DEPTH_KEY = "depth";
    public static final String REG47422_CRPRD_KEY = "withClosedPeriod";

    private final int REG47422_DEF_DEPTH = 4;

    @EJB
    private Reg47422JournalRepository journalRepository;

    @EJB
    private PropertiesRepository propertiesRepository;

    @EJB
    private AuditController auditController;

    @EJB
    private ClosedPeriodCashedRepository closedPeriodRepository;

    @Inject
    private BankCalendarDayRepository calendarDayRepository;

    @Override
    protected void initExec(String jobName, Properties properties) throws Exception {

    }

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        Operday operday = operdayController.getOperday();
        if (ONLINE != operday.getPhase()) {
            auditController.warning(AuditRecord.LogCode.Operday, format("Нельзя запустить задачу '%s': опердень в статусе '%s'"
                    , myTaskName, operday.getPhase().name() ));
            return false;
        }

//        final Date currentDateTime = operdayController.getSystemDateTime();
//        final Date currentDate = DateUtils.onlyDate(currentDateTime);

        checkAlreadyRunning(myTaskName, properties);
        return true;
    }

    @Override
    protected boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        /* TODO
        определить дату начала dateFrom (POD >= dateFrom && POD <= lwdate, PROCDATE is null or PROCDATE <= lwdate)
        обновить GL_REG47422
        склеить проводки с одной датой
            одиночные
            веера
        обработать проводки с разной датой
            одиночные
            веера
        */
        Operday operday = operdayController.getOperday();
        Date dateFrom = getDateFrom(operday.getCurrentDate(), properties);

        return false;
    }

    /**
     * Определяет дату начала обработки (из таблицы настроек и свойст задачи)
     * @param curdate
     * @param properties
     * @return
     * @throws SQLException
     */
    private Date getDateFrom(Date curdate, Properties properties) throws SQLException {
        int depth = REG47422_DEF_DEPTH;
        String depthProp = properties.getProperty(REG47422_DEPTH_KEY);
        if (null != depthProp) {
            try {
                depth = Integer.parseInt(depthProp);
                if (depth < 1 || depth > 30)
                    throw new NumberFormatException("Значение должно быть от 1 до 30");
            } catch (NumberFormatException e) {
                ValidationError error = new ValidationError(ErrorCode.RE47422_ERROR, String.format("Неверное значение %s в свойствах задачи: %s %s", REG47422_DEPTH_KEY, depthProp, e.getMessage()));
                auditController.error(MakeInvisible47422, "Ошибка при выполнении задачи " + myTaskName, null, error);
            }
        }
        else {
            depth = (int)(long)propertiesRepository.getNumberDef(REG47422_DEPTH.name(), (long)REG47422_DEF_DEPTH);
        }
        Date dateFrom = calendarDayRepository.getWorkDateBefore(curdate, depth, false);
        boolean withClosedPeriod = Boolean.valueOf(properties.getProperty(REG47422_CRPRD_KEY));
        if (!withClosedPeriod) {
            Date ldate = closedPeriodRepository.getPeriod().getLastDate();
            dateFrom = dateFrom.after(ldate) ? dateFrom : calendarDayRepository.getWorkDateAfter(ldate, false);
        }
        return dateFrom;
    }

    /**
     * Заполняет журнал новыми и измененными данными
     * @param dateFrom
     * @return
     * @throws Exception
     */
    private int loadNewData(Date dateFrom) throws Exception {
        int res = journalRepository.executeInNewTransaction(persistence -> {
            // найти проводки с отличием, проапдейтить VALID = 'U'
            int changed = journalRepository.findChangedPst();
            // вставить измененные записи (STATE = 'CHANGE') и новын записи (STATE = 'LOAD')
            int inserted = journalRepository.insertNewAndChangedPst(dateFrom);
            if (changed > 0) {
                // статус измененных = 'N'
                Assert.isTrue(changed == journalRepository.updateChangedPstOld(), String.format("Ошибка при изменении статуса, ожидается записей: %в", changed));
            }
            return inserted;
        });
        auditController.info(MakeInvisible47422, "Вставлено новых / измененных записей в GL_REG47422: " + res);
        return res;
    }

}
