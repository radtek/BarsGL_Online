package ru.rbt.barsgl.ejb.monitoring;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.task.EtlStructureMonitorTask;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.job.BackgroundJobService;

import javax.ejb.EJB;
import javax.ejb.Timer;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;

/**
 * Created by Ivan Sevastyanov
 */
public class MonitoringSupportService {

    private static Date OPER_DAY_CLOSED_NEXT_DATE = null;

    static {
        try {
            OPER_DAY_CLOSED_NEXT_DATE = org.apache.commons.lang3.time.DateUtils.parseDate("2029-01-01", "yyyy-MM-dd");
        } catch (ParseException e) {}
    }


    @EJB
    private CoreRepository repository;

    @EJB
    private BackgroundJobService jobService;

    @EJB
    private OperdayController operdayController;

    public void checkDbConnectionPool() throws SQLException {
        repository.select("SELECT WORKDAY FROM WORKDAY");
    }

    /**
     *
     * @return следующий запуск
     */
    public Date checkEtlMonitorAlive() {
        if (operdayController.getOperday().getPhase() == Operday.OperdayPhase.ONLINE) {
            Timer timer = jobService.findStartedTimerByName(EtlStructureMonitorTask.class.getSimpleName());
            if (null == timer) {
                throw new DefaultApplicationException("timer not available");
            } else {
                return timer.getNextTimeout();
            }
        } else {
            return OPER_DAY_CLOSED_NEXT_DATE;
        }
    }
}
