package ru.rbt.barsgl.ejbcore.job;

import ru.rbt.barsgl.ejbcore.mapping.job.TimerJob;
import ru.rbt.barsgl.shared.enums.JobStartupType;

import javax.ejb.Local;
import javax.ejb.Timer;
import java.util.List;
import java.util.Properties;

/**
 * Created by Ivan Sevastyanov
 */
@Local
public interface BackgroundJobService {

    List<TimerJob> getTimerJobs(boolean exclusive);
    void startupAll();
    void shutdownAll();
    void refreshJobsStatus();
    void setStartupType(String jobName, JobStartupType startupType);
    void startupJob(TimerJob job);
    void startupJob(String timerJobName);
    void shutdownJob(TimerJob job);
    void shutdownJob(String timerJobName);
    ParamsAwareRunnable findParamsAwareRunnableJob(Class<? extends ParamsAwareRunnable> clazz);
    void executeJob(TimerJob timerJob) throws Exception;

    /**
     * Однократный запуск с определенными свойствами
     * @param timerJob
     * @param properties
     * @throws Exception
     */
    void executeJob(TimerJob timerJob, Properties properties) throws Exception;
    void executeJob(String jobName, Properties properties) throws Exception;

    void executeJob(String jobName) throws Exception;

    /**
     * Асинхронный однкократный запуск задачи
     * @param jobName
     * @param properties
     * @param delay задержка мс
     */
    void executeJobAsync(String jobName, Properties properties, long delay) throws Exception;

    Timer findStartedTimerByName(String timerName);

}
