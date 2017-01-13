package ru.rbt.barsgl.ejbtest.sys;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejbcore.mapping.job.CalendarJob;
import ru.rbt.barsgl.ejbcore.mapping.job.IntervalJob;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbcore.mapping.job.TimerJob;
import ru.rbt.barsgl.ejbtest.AbstractTimerJobTest;
import ru.rbt.barsgl.ejbtesting.job.TestingJob;
import ru.rbt.barsgl.ejbtesting.job.service.TestingJobTwo;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static ru.rbt.barsgl.ejbcore.mapping.job.TimerJob.JobState.STARTED;
import static ru.rbt.barsgl.ejbcore.mapping.job.TimerJob.JobState.STOPPED;
import static ru.rbt.barsgl.shared.enums.JobStartupType.AUTO;
import static ru.rbt.barsgl.shared.enums.JobStartupType.MANUAL;

/**
 * Created by Ivan Sevastyanov
 * Тест запуска и выполнения заданий
 * @notdoc
 */
public class TimerJobTest extends AbstractTimerJobTest {

    private static final List<TimerJob> testJobs = new ArrayList<>();

    @Test public void testSingleActionJob() throws Exception {

        SingleActionJob singleActionJob = new SingleActionJob();
        singleActionJob.setDelay(0L);
        singleActionJob.setDescription("test single action job");
        singleActionJob.setRunnableClass(TestingJob.class.getName());
        singleActionJob.setStartupType(AUTO);
        singleActionJob.setState(STOPPED);
        singleActionJob.setName(System.currentTimeMillis() + "");

        singleActionJob = (SingleActionJob) baseEntityRepository.save(singleActionJob);

        registerJob(singleActionJob);

        jobService.startupJob(singleActionJob);

        Thread.sleep(100L);

        Assert.assertEquals(1, jobRegistrator.getRunCount(singleActionJob.getName()));
        singleActionJob = (SingleActionJob) baseEntityRepository.findById(SingleActionJob.class, singleActionJob.getId());
        Assert.assertNotNull(singleActionJob);
        Assert.assertEquals(STARTED, singleActionJob.getState());

        jobService.executeJob(singleActionJob);
        Assert.assertEquals(2, jobRegistrator.getRunCount(singleActionJob.getName()));

    }

    @Test public void testIntervalActionJob() throws InterruptedException {
        IntervalJob intervalJob = new IntervalJob();
        intervalJob.setDelay(1L);
        intervalJob.setDescription("test interval job");
        intervalJob.setRunnableClass(TestingJobTwo.class.getName());
        intervalJob.setStartupType(AUTO);
        intervalJob.setState(STOPPED);
        intervalJob.setName(System.currentTimeMillis() + "");
        intervalJob.setInterval(new Long(2000L));

        intervalJob = (IntervalJob) baseEntityRepository.save(intervalJob);
        intervalJob = (IntervalJob) baseEntityRepository.findById(IntervalJob.class, intervalJob.getId());
        Assert.assertNotNull(intervalJob.getInterval());

        registerJob(intervalJob);

        jobService.startupJob(intervalJob);

        Thread.sleep(1500L);

        Assert.assertEquals(1, jobRegistrator.getRunCount(intervalJob.getName()));
        intervalJob = (IntervalJob) baseEntityRepository.findById(IntervalJob.class, intervalJob.getId());
        Assert.assertNotNull(intervalJob);
        Assert.assertEquals(STARTED, intervalJob.getState());

        Thread.sleep(3000L);

        int runCount = jobRegistrator.getRunCount(intervalJob.getName());
        Assert.assertTrue("" + runCount, 1 < runCount);

    }

    @Test public void testCalendarActionJob() throws InterruptedException {

        CalendarJob calendarJob = new CalendarJob();
        calendarJob.setDelay(1L);
        calendarJob.setDescription("test calendar job");
        calendarJob.setRunnableClass(TestingJobTwo.class.getName());
        calendarJob.setStartupType(AUTO);
        calendarJob.setState(STOPPED);
        calendarJob.setName(System.currentTimeMillis() + "");
        calendarJob.setScheduleExpression("month=*;second=*/3;minute=*;hour=*");

        calendarJob = (CalendarJob) baseEntityRepository.save(calendarJob);
        registerJob(calendarJob);

        jobService.startupJob(calendarJob);

        Thread.sleep(4000L);

        Assert.assertEquals(1, jobRegistrator.getRunCount(calendarJob.getName()));
        calendarJob = (CalendarJob) baseEntityRepository.findById(CalendarJob.class, calendarJob.getId());
        Assert.assertNotNull(calendarJob);
        Assert.assertEquals(STARTED, calendarJob.getState());

        Thread.sleep(4000L);

        int runCount = jobRegistrator.getRunCount(calendarJob.getName());
        Assert.assertTrue("" + runCount, 2 <= runCount);

    }

    @Test public void testOtherFunctions() throws InterruptedException {
        CalendarJob calendarJob = new CalendarJob();
        calendarJob.setDelay(1L);
        calendarJob.setDescription("test calendar job");
        calendarJob.setRunnableClass(TestingJobTwo.class.getName());
        calendarJob.setStartupType(AUTO);
        calendarJob.setState(STOPPED);
        calendarJob.setName(System.currentTimeMillis() + "");
        calendarJob.setScheduleExpression("month=*;second=*/3;minute=*;hour=*");

        calendarJob = (CalendarJob) baseEntityRepository.save(calendarJob);
        registerJob(calendarJob);

        jobService.startupJob(calendarJob);

        Thread.sleep(3000);
        calendarJob = (CalendarJob) baseEntityRepository.findById(CalendarJob.class, calendarJob.getId());

        Assert.assertEquals(STARTED, calendarJob.getState());

        baseEntityRepository.executeUpdate("update TimerJob j set j.state = ?1 where j.id = ?2"
                , STOPPED, calendarJob.getId());
        calendarJob = (CalendarJob) baseEntityRepository.findById(CalendarJob.class, calendarJob.getId());
        Assert.assertEquals(STOPPED, calendarJob.getState());

        jobService.refreshJobsStatus();
        calendarJob = (CalendarJob) baseEntityRepository.findById(CalendarJob.class, calendarJob.getId());
        Assert.assertEquals(STARTED, calendarJob.getState());

        jobService.shutdownAll();
        jobService.refreshJobsStatus();
        calendarJob = (CalendarJob) baseEntityRepository.findById(CalendarJob.class, calendarJob.getId());
        Assert.assertEquals(STOPPED, calendarJob.getState());

        jobService.startupAll();
        jobService.refreshJobsStatus();
        calendarJob = (CalendarJob) baseEntityRepository.findById(CalendarJob.class, calendarJob.getId());
        Assert.assertEquals(STARTED, calendarJob.getState());

        List<TimerJob> jobs = jobService.getTimerJobs(true);
        Assert.assertFalse(jobs.isEmpty());

        Assert.assertEquals(AUTO, calendarJob.getStartupType());
        jobService.setStartupType(calendarJob.getName(), MANUAL);
        calendarJob = (CalendarJob) baseEntityRepository.findById(CalendarJob.class, calendarJob.getId());
        Assert.assertEquals(MANUAL, calendarJob.getStartupType());

    }

    @Test public void testExecAsync() throws Exception {
        SingleActionJob singleActionJob = new SingleActionJob();
        singleActionJob.setDelay(0L);
        singleActionJob.setDescription("test single action job");
        singleActionJob.setRunnableClass(TestingJob.class.getName());
        singleActionJob.setStartupType(AUTO);
        singleActionJob.setState(STOPPED);
        singleActionJob.setName(System.currentTimeMillis() + "");

        singleActionJob = (SingleActionJob) baseEntityRepository.save(singleActionJob);

        registerJob(singleActionJob);

        jobService.executeJobAsync(singleActionJob.getName(), new Properties(), 3000);

        jobService.refreshJobsStatus();

        singleActionJob = (SingleActionJob) baseEntityRepository
                .selectOne(TimerJob.class, "from TimerJob j where j.id = ?1", singleActionJob.getId());
        Assert.assertEquals(TimerJob.JobState.STARTED, singleActionJob.getState());

        Thread.sleep(5000L);

        Assert.assertEquals(1, jobRegistrator.getRunCount(singleActionJob.getName()));

        jobService.refreshJobsStatus();

        singleActionJob = (SingleActionJob) baseEntityRepository
                .selectOne(TimerJob.class, "from TimerJob j where j.id = ?1", singleActionJob.getId());
        Assert.assertEquals(TimerJob.JobState.STOPPED, singleActionJob.getState());
    }


}
