package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.common.repository.od.OperdayRepository;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;

import static ru.rbt.audit.entity.AuditRecord.LogCode.ReplAfterBufferRelease;

public class ReplicateManualTask extends AbstractJobHistoryAwareTask {

    private static final Logger logger = Logger.getLogger(ReplicateManualTask.class.getName());

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @Inject
    private BankCalendarDayRepository calendarDayRepository;

    @EJB
    private OperdayRepository operdayRepository;

    @Inject
    private ru.rbt.ejbcore.util.DateUtils dateUtils;

    @Override
    protected void initExec(String jobName, Properties properties) {

    }

    @Override
    protected boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {

        Date currDate = operdayController.getOperday().getCurrentDate();

        Date workDayBefor = calendarDayRepository
                .getWorkdayBefore(operdayController.getOperday().getCurrentDate()).getId().getCalendarDate();

        try {
            beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence, connection) -> {
                auditController.info(ReplAfterBufferRelease,
                        String.format("Запуск задачи репликации, предыдущий рабочий день %s, опердень %s"
                                , dateUtils.onlyDateString(workDayBefor), dateUtils.onlyDateString(currDate)));
                operdayRepository.executeNativeUpdate("call REPLICATE_MANUAL()");

                auditController.info(ReplAfterBufferRelease, String.format("Репликация прошла успешно, предыдущий рабочий день %s, опердень %s"
                        , dateUtils.onlyDateString(workDayBefor), dateUtils.onlyDateString(currDate)));

                return true;
            });
        } catch (Throwable e) {
            auditController.error(ReplAfterBufferRelease, String.format("Ошибка репликации, предыдущий рабочий день %s, опердень %s"
                    , dateUtils.onlyDateString(workDayBefor), dateUtils.onlyDateString(currDate)), null, e);
            return false;
        }

        return true;
    }

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        return true;
    }

    @Override
    protected boolean checkJobStatus(String jobName, Properties properties) {
        return true;
    }

    /*public boolean checkRun(Date sysTime) {
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
    }*/

}