package ru.rbt.barsgl.ejbtest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejbcore.mapping.job.TimerJob;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ivan Sevastyanov
 */
public abstract class AbstractTimerJobTest extends AbstractRemoteTest {

    private static final List<TimerJob> testJobs = new ArrayList<>();
    private static Operday previosOperday = getOperday();

    @BeforeClass
    public static void beforeClassTimer() throws ParseException {
//        jobService.shutdownAll();
        previosOperday = getOperday();
    }

    @AfterClass
    public static void afterClass() {
        for (TimerJob job : testJobs) {
            jobService.shutdownJob(job);
            baseEntityRepository.executeUpdate("delete from TimerJob j where j.id = ?1", job.getId());
        }
        restoreOperday();
    }

    protected static void registerJob(TimerJob timerJob) {
        testJobs.add(timerJob);
    }

    protected static void restoreOperday() {
        baseEntityRepository.executeUpdate(
                "update Operday o set o.currentDate=?1, o.lastWorkdayStatus=?2, o.lastWorkingDay=?3, o.phase=?4"
                , previosOperday.getCurrentDate(), previosOperday.getLastWorkdayStatus(), previosOperday.getLastWorkingDay(), previosOperday.getPhase());
        baseEntityRepository.executeNativeUpdate("update gl_od set prc = 'STARTED'");
        refreshOperdayState();
    }

}
