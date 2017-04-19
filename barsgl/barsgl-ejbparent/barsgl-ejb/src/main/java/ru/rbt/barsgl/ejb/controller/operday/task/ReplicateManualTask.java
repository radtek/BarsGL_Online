package ru.rbt.barsgl.ejb.controller.operday.task;

import javax.ejb.EJB;
import javax.inject.Inject;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.BankCalendarDay;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.common.repository.od.OperdayRepository;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.ejbcore.util.DateUtils;

import static org.apache.commons.lang3.time.DateUtils.truncate;
import static ru.rbt.audit.entity.AuditRecord.LogCode.ReplAfterHolidays;
import static ru.rbt.audit.entity.AuditRecord.LogCode.Task;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;

public class ReplicateManualTask implements ParamsAwareRunnable {

    private static final Logger logger = Logger.getLogger(ReplicateManualTask.class.getName());

    @EJB
    private BeanManagedProcessor beanManagedProcessor;
    @EJB
    private AuditController auditController;
    @Inject
    private BankCalendarDayRepository calendarDayRepository;
    @Inject
    private OperdayRepository operdayRepository;
    @Inject
    private ru.rbt.ejbcore.util.DateUtils dateUtils;

    @EJB
    private OperdayController operdayController;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        Date currDate = operdayController.getOperday().getCurrentDate();

        Date workDayBefor = calendarDayRepository
                .getWorkdayBefore(operdayController.getOperday().getCurrentDate()).getId().getCalendarDate();

        Date sysTime = operdayController.getSystemDateTime();

        if (checkRun(sysTime)) {

            try {
                beanManagedProcessor.executeInNewTxWithDefaultTimeout(((persistence, connection) -> {
                    auditController.info(ReplAfterHolidays,
                            String.format("Запуск задачи репликации после выходных, предыдущий рабочий день %s, опердень %s"
                                    , dateUtils.onlyDateString(workDayBefor), dateUtils.onlyDateString(currDate)));
                    operdayRepository.executeNativeUpdate("call REPLICATE_MANUAL()");
                    auditController.info(ReplAfterHolidays, String.format("Репликации после выходных прошла успешно, предыдущий рабочий день %s, опердень %s"
                            , dateUtils.onlyDateString(workDayBefor), dateUtils.onlyDateString(currDate)));
                    return null;
                }));
            } catch (Throwable e) {
                auditController.error(ReplAfterHolidays, String.format("Ошибка репликации после выходных, предыдущий рабочий день %s, опердень %s"
                        , dateUtils.onlyDateString(workDayBefor), dateUtils.onlyDateString(currDate)), null, e);
            }
        } else {
            auditController.info(ReplAfterHolidays, String.format("Репликации после выходных не запускалась, предыдущий рабочий день %s, опердень %s"
                    , dateUtils.onlyDateString(workDayBefor), dateUtils.onlyDateString(currDate)));
        }
    }

    public boolean checkRun(Date sysTime) {
        Date workday = calendarDayRepository.getWorkdayBefore(
                org.apache.commons.lang3.time.DateUtils.addDays(sysTime,1)).getId().getCalendarDate();
        Date yesterday = org.apache.commons.lang3.time.DateUtils.addDays(sysTime, -1);
        logger.info(String.format("Systime '%s', workday '%s', yesterday '%s'"
                ,dateUtils.fullDateString(sysTime), dateUtils.fullDateString(workday), dateUtils.fullDateString(yesterday)));
        if (    // текущий день запуска - рабочий
                truncate(sysTime, Calendar.DATE).equals(workday)
                && // вчера - не рабоочий день
                        !calendarDayRepository.isWorkday(yesterday)) {
            return true;
        } else {
            return false;
        }
    }

}