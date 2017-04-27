package ru.rbt.barsgl.ejbcore.job;

import org.apache.log4j.Logger;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.mapping.job.CalendarJob;
import ru.rbt.barsgl.ejbcore.mapping.job.IntervalJob;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbcore.mapping.job.TimerJob;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.shared.Assert;
import ru.rbt.barsgl.shared.enums.JobStartupType;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.ejb.Timer;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.*;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static ru.rbt.barsgl.ejbcore.mapping.job.TimerJob.JobState.*;
import static ru.rbt.shared.Assert.assertThat;
import static ru.rbt.shared.Assert.notNull;
import static ru.rbt.barsgl.shared.enums.JobStartupType.AUTO;

/**
 * Created by Ivan Sevastyanov
 */
@Stateless
public class BackgroundJobServiceBean implements BackgroundJobService {

    private static final Logger LOG = Logger.getLogger(BackgroundJobServiceBean.class);

    @Resource
    private SessionContext context;

    @Inject
    private TimerJobRepository timerJobRepository;

    @Inject
    private Instance<ParamsAwareRunnable> jobs;

    @Override
    public List<TimerJob> getTimerJobs(boolean exclusive) {
        try {
            if (exclusive) {
                int cntJobs = timerJobRepository.select("select COUNT(1) CNT from GL_SCHED with UR").get(0).getInteger("CNT");
                int cntLocked = timerJobRepository.executeUpdate("update TimerJob j set j.name = j.name");
                if (0 != cntJobs && 0 == cntLocked) {
                    throw new DefaultApplicationException("Не удалось создать эксклюзивную блокировку при получении списка фоновых задач");
                }
            }
            return timerJobRepository.select(TimerJob.class, "from TimerJob j");
        } catch (Exception e) {
            // TODO Audit.auditFailure(BACKGROUND_TASK_LIST, "Получение списка фоновых задач", e);
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    @Override
    public void startupAll(){
        List<TimerJob> jobs = getTimerJobs(true);
        for (TimerJob job : jobs) {
            if (AUTO == job.getStartupType() ) {
                try {
                    startupJob(job);
                } catch (Exception e) {
                    LOG.error(String.format("Error on starting persisted timer job name '%s' id '%s'"
                            , job.getName(), job.getId()), e);
                    updateJobState(job, ERROR);
                }
            }
        }
    }

    @Override
    public void shutdownAll() {
        List<TimerJob> jobs = getTimerJobs(true);
        for (TimerJob timer : jobs) {
            updateJobState(timer, STOPPED);
            shutdownJob(timer);
        }
    }

    @Override
    public void refreshJobsStatus() {
        final Collection<Timer> timers = getTimers();
        final List<String> startedTimers = new ArrayList<>();
        for (Timer timer : timers) {
            try {
                if (null != timer.getInfo() && timer.getInfo() instanceof JobWrapper) {
                    JobWrapper wrapper = (JobWrapper) timer.getInfo();
                    updateJobState(wrapper.getName(), STARTED);
                    startedTimers.add(wrapper.getName());
                }
            } catch (Exception e){
                e.printStackTrace();
                LOG.error(format("Не удалось обновить состояние таймера (запущенные): %s", e.getMessage()), e);
            }
        }
        // TODO халтура, нужно сделать одним запросом
        final List<TimerJob> jobs = getTimerJobs(false);
        for (TimerJob job : jobs) {
            try {
                if (STARTED == job.getState() && !startedTimers.contains(job.getName())) {
                    LOG.warn(format("Таймер '%s' не запущен.", job.getName()));
                    updateJobState(job, STOPPED);
                }
            } catch (Exception e) {
                LOG.error(format("Не удалось обновить состояние фоновых задач: %s", e.getMessage()), e);
            }
        }
    }

    @Override
    public void setStartupType(String jobName, JobStartupType startupType) {
        TimerJob job = null;
        try {
            job = timerJobRepository.selectFirst(TimerJob.class, "from TimerJob j where j.name = ?1", jobName);
            notNull(job, format("Не найдена задача: '%s'", jobName));
            int cnt = timerJobRepository.executeUpdate("update TimerJob j set j.startupType = ?1 where j.name = ?2 and j.startupType <> ?3", startupType, jobName, startupType);
            if (1 > cnt) {
                LOG.warn(format("Не было обновлено ни одного таймера '%s' для установки типа запуска '%s'", jobName, startupType));
            }
            // TODO Audit.auditSuccess(BACKGROUND_TASK_EDIT, format("Таймер '%s' изменен успешно", jobName), job);
        } catch (Exception e) {
            // TODO Audit.auditFailure(BACKGROUND_TASK_EDIT, format("Ошибка при изменении параметров фоновой задачи: '%s'", jobName), job, e);
            throw e;
        }
    }

    @Override
    public void startupJob(TimerJob timerJob) {

        timerJob = timerJobRepository.findById(TimerJob.class, timerJob.getId());
        timerJob = timerJobRepository.refresh(timerJob);

        Assert.isTrue(JobStartupType.DISABLED != timerJob.getStartupType()
                , format("Задача '%s' находится в режиме запуска '%s'", timerJob.getId(), timerJob.getStartupType()));

        updateJobState(timerJob, STARTED);
        // создаем таймер всегда, сначала остановив его принудительно
        JobDefinition def = from(timerJob);
        stopJob(def);
        Timer timer = createJob(def);
        LOG.info(format("Timer '%s' (id=%s) is STARTED. Next expiration time: '%s'", timerJob.getName(), timerJob.getId(), timer.getNextTimeout()));
        // TODO Audit.auditSuccess(BACKGROUND_TASK_START, format("Таймер '%s' (id=%s) запущен", timerJob.getName(), timerJob.getId()), timerJob);
    }


    @Override
    public void shutdownJob(TimerJob timerJob) {
        try {
            updateJobState(timerJob, STOPPED);
            stopJob(from(timerJob));
            LOG.info(format("Таймер name='%s' (id='%s') остановлен", timerJob.getName(), timerJob.getId()));
            // TODO Audit.auditSuccess(BACKGROUND_TASK_STOP, format("Таймер '%s' (id=%s) остановлен", timerJob.getName(), timerJob.getId()), timerJob);
        } catch (Exception e) {
            // TODO Audit.auditFailure(BACKGROUND_TASK_STOP, format("Ошибка остановки фоновой задачи: '%s' (id=%s)", timerJob.getName(), timerJob.getId()), timerJob, e);
            throw e;
        }
    }

    @Override
    public void shutdownJob(String timerJobName) {
        shutdownJob(findJobByName(timerJobName));
    }

    @Override
    public void startupJob(String timerJobName) {
        startupJob(findJobByName(timerJobName));
    }

    @Timeout
    public void timeout(Timer timer) {
        JobWrapper job = (JobWrapper)timer.getInfo();
        long count = job.getExecCount();
        LOG.debug(format("About to executing of job ('%s'): '%s', '%s'", count, job.getName(), job.getJobDefinition().getWorker().getClass().getName()));
        try {
            job.exec();
            LOG.debug(format("Executing of job ('%s'): '%s', '%s' has SUCCEDED", count, job.getName(), job.getJobDefinition().getWorker().getClass().getName()));
        } catch (Throwable error) {
            LOG.error(format("About to executing of job ('%s'): '%s', '%s' has FAILED", count, job.getName(), job.getJobDefinition().getWorker().getClass().getName()), error);
        }
    }

    public Timer findStartedTimerByName(final String timerName) {
        return context.getTimerService().getTimers().stream().filter( input -> {
            try {
                JobWrapper wr = (JobWrapper) input.getInfo();
                return timerName.equals(wr.getName());
            } catch (Throwable e) {
                LOG.error("Timer " + input + " is not acceptable", e);
                return false;
            }
        }).findAny().orElse(null);
    }

    private static JobDefinition from(TimerJob timerJob) {
        JobDefinition definition;
        if (timerJob instanceof IntervalJob) {
            IntervalJob intervalJob = (IntervalJob) timerJob;
            definition = new JobDefinition(intervalJob.getName()
                    , intervalJob.getDelay()
                    , intervalJob.getInterval()
                    , createWorker(intervalJob.getRunnableClass())
                    , null, false, timerJob.getProperties());
        } else if (timerJob instanceof SingleActionJob) {
            SingleActionJob singleActionJob = (SingleActionJob) timerJob;
            definition = new JobDefinition(singleActionJob.getName()
                    , Optional.ofNullable(singleActionJob.getDelay()).orElse(new Long(0))
                    , -1
                    , createWorker(singleActionJob.getRunnableClass())
                    , null, true, timerJob.getProperties());
        } else {
            CalendarJob calendarJob = (CalendarJob) timerJob;
            definition = new JobDefinition(calendarJob.getName()
                    , calendarJob.getDelay()
                    , -1, createWorker(calendarJob.getRunnableClass())
                    , calendarJob.getScheduleExpression()
                    , false, timerJob.getProperties()
            );
        }
        return definition;
    }

    private static int from(BigDecimal value) {
        return value != null ? value.intValue() : null;
    }

    private static <T extends ParamsAwareRunnable> T createWorker(String runnable) {
        try {
            Class<ParamsAwareRunnable> clazz = (Class<ParamsAwareRunnable>) Class.forName(runnable);
            return (T) clazz.newInstance();
        } catch (Exception e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private void updateJobState(TimerJob timerJob, TimerJob.JobState targetState) {
        updateJobState(timerJob.getName(), targetState);
    }

    private void updateJobState(String timerName, TimerJob.JobState targetState) {
        final int cnt = timerJobRepository.executeUpdate("update TimerJob j set j.state = ?1 where j.name = ?2 and j.state <> ?3"
                , targetState, timerName, targetState);
        if (cnt == 0) {
            LOG.debug(format("Не было обновлено ни одного таймера '%s' для установки статуса '%s'"
                    , timerName, targetState.name()));
        }
    }

    private void stopJob(final JobDefinition job) {
        notNull(job);
        assertThat(!isEmpty(job.getName()));
        Timer target = findStartedTimerByName(job.getName());
        if (null != target) {
            LOG.info(format("About to STOP job: {'%s' : '%s'}", job.getName(), job.getClass().getName()));
            target.cancel();
        } else {
            LOG.warn(format("Job: {'%s' : '%s'} is not found for STOPPING", job.getName(), job.getClass().getName()));
        }
    }

    private Timer createJob(JobDefinition job) {
        try {
            Timer timer;
            if (job.isSingleAction()) {
                timer = context.getTimerService().createSingleActionTimer(job.getDelay(), new TimerConfig(new JobWrapper(job), false));
                LOG.info(format("Single action job successfully created: {'%s' : '%s'}", job.getName(), job.getWorker().getClass().getName()));
                return timer;
            } else if (!isEmpty(job.getCalendarString())) {
                ScheduleExpression expr = ScheduleExpressionParser.parse(job.getCalendarString());
                if (job.getDelay() > 0){
                    Calendar c = Calendar.getInstance();
                    c.add(Calendar.MILLISECOND, (int)job.getDelay());
                    expr.start(c.getTime());
                }
                timer = context.getTimerService().createCalendarTimer(expr, new TimerConfig(new JobWrapper(job), false));
                LOG.info(format("Calendar scheduled job successfully created: {'%s' : '%s' : '%s' : '%s'}"
                        , job.getName(), job.getWorker().getClass().getName(), job.getCalendarString(), expr));
                return timer;
            } else {
                timer = context.getTimerService().createIntervalTimer(job.getDelay(), job.getInterval(), new TimerConfig(new JobWrapper(job), false));
                LOG.info(format("Interval scheduled job successfully created: {'%s' : '%s'}", job.getName(), job.getWorker().getClass().getName()));
                return timer;
            }
        } catch (Throwable th) {
            LOG.error("Failed to start job: {" + job.getName() + " : " + job.getWorker().getClass().getName() + "}", th);
            throw new DefaultApplicationException(th.getMessage(), th);
        }
    }

    private Collection<Timer> getTimers() {
        return context.getTimerService().getTimers();
    }

    public ParamsAwareRunnable findParamsAwareRunnableJob(Class<? extends ParamsAwareRunnable> clazz) {
        /*TODO сделать нармальный лог (возможно java.util.logging...)
        if (LOG.isDebugEnabled()) {
            for (ParamsAwareRunnable job : jobs) {
                LOG.debug(format("Found a job: '%s', '%s'.isAssignableFrom('%s') = '%s'"
                        , job, clazz, job.getClass(), clazz.isAssignableFrom(job.getClass())));
            }
        }*/
        for (ParamsAwareRunnable job : jobs) {
            if (clazz.isAssignableFrom(job.getClass())) return job;
        }
        throw new DefaultApplicationException("Not found: " + clazz);
    }

    @Override
    public void executeJob(TimerJob timerJob) throws Exception {
        final Properties properties = new Properties();
        if (!isEmpty(timerJob.getProperties())) {
            properties.load(new StringReader(timerJob.getProperties()));
        }
        findParamsAwareRunnableJob((Class<? extends ParamsAwareRunnable>) Class.forName(timerJob.getRunnableClass()))
                .run(timerJob.getName(), properties);
    }

    public void executeJob(TimerJob timerJob, Properties properties) throws Exception {
        findParamsAwareRunnableJob((Class<? extends ParamsAwareRunnable>) Class.forName(timerJob.getRunnableClass()))
                .run(timerJob.getName(), properties);
    }

    @Override
    public void executeJob(String jobName, Properties properties) throws Exception {
        executeJob(findJobByName(jobName), properties);
    }

    @Override
    public void executeJob(String jobName) throws Exception {
        executeJob(findJobByName(jobName));
    }
    @Override
    public void executeJobAsync(String jobName, Properties properties, long delayMs) throws Exception {
        TimerJob timerJob = findJobByName(jobName);
        updatePropertiesFromDB(properties, timerJob);
        JobDefinition def = new JobDefinition(jobName, delayMs, -1
                , createWorker(timerJob.getRunnableClass()), null, true, properties);
        stopJob(def);
        Timer timer = createJob(def);
        LOG.info(format("Timer '%s' (id=%s) is STARTED asynchronous. Next expiration time: '%s'"
                , timerJob.getName(), timerJob.getId(), timer.getNextTimeout()));
    }

    /**
     * добавляем только, если нет
     * @param properties
     * @param timerJob
     * @throws IOException
     */
    private void updatePropertiesFromDB (Properties properties, TimerJob timerJob) throws IOException {
        if (!StringUtils.isEmpty(timerJob.getProperties())) {
            Properties jobProperties = new Properties();
            jobProperties.load(new StringReader(timerJob.getProperties()));
            jobProperties.keySet().stream().forEach(p -> {
                if (p instanceof String && null == properties.getProperty((String) p)) {
                    properties.setProperty((String) p, jobProperties.getProperty((String) p));
                }
            });
        }
    }

    private TimerJob findJobByName(String timerJobName) {
        Assert.isTrue(!isEmpty(timerJobName));
        final TimerJob job = timerJobRepository.selectFirst(TimerJob.class, "from TimerJob j where j.name = ?1", timerJobName);
        Assert.isTrue(null != job, format("Задача '%s' не найдена", timerJobName));
        return job;
    }
}
