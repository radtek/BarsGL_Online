package ru.rbt.tasks.ejb.job;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.tasks.ejb.repository.JobHistoryRepository;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.job.BackgroundJobService;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.barsgl.ejbcore.job.TimerJobRepository;
import ru.rbt.barsgl.ejbcore.mapping.job.CalendarJob;
import ru.rbt.barsgl.ejbcore.mapping.job.TimerJob;
import ru.rbt.barsgl.shared.enums.JobStartupType;
import ru.rbt.barsgl.shared.jobs.TimerJobHistoryWrapper;
import ru.rbt.barsgl.shared.jobs.TimerJobWrapper;

import javax.ejb.AccessTimeout;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MINUTES;
import javax.inject.Inject;
import static ru.rbt.audit.entity.AuditRecord.LogCode.JobControl;

/**
 * Created by ER21006 on 07.04.2016.
 */
@Singleton
@AccessTimeout(value = 15, unit = MINUTES)
public class BackgroundJobsController {

    private static final Logger logger = Logger.getLogger(BackgroundJobsController.class.getName());

    @EJB
    private BackgroundJobService backgroundJobService;

    @EJB
    private AuditController auditController;

    @Inject
    private TimerJobRepository timerJobRepository;

    @EJB
    private JobHistoryRepository historyRepository;

    @EJB
    private OperdayController operdayController;

    public List<TimerJob> getTimerJobs(boolean exclusive) {
        logger.info(format("Получение всех задач в режиме exclusive: '%s'", exclusive));
        return backgroundJobService.getTimerJobs(exclusive);
    }

    public void startupAll(){
        auditController.info(JobControl, "Запуск всех автоматических задач");
        backgroundJobService.startupAll();
    }

    public void shutdownAll(){
        auditController.info(JobControl, "Остановка всех автоматических задач");
        backgroundJobService.shutdownAll();
    }

    public void refreshJobsStatus(){
        auditController.info(JobControl, "Обновление всех автоматических задач");
        backgroundJobService.refreshJobsStatus();
    }

    public void setStartupType(String jobName, JobStartupType startupType){
        auditController.info(JobControl, format("Изменение типа запуска для задачи '%s', новый тип '%s'", jobName, startupType));
        backgroundJobService.setStartupType(jobName, startupType);
    }

    public void startupJob(TimerJob job) {
        auditController.info(JobControl, format("Запуск задачи '%s' в фоновом режиме", job.getName()));
        backgroundJobService.startupJob(job);
    }

    public void startupJob(String timerJobName){
        auditController.info(JobControl, format("Запуск задачи '%s' в фоновом режиме", timerJobName));
        backgroundJobService.startupJob(timerJobName);
    }

    public void shutdownJob(TimerJob job){
        auditController.info(JobControl, format("Остановка задачи '%s'", job.getName()));
        backgroundJobService.shutdownJob(job);
        auditController.info(JobControl, format("Задача '%s' остановлена", job.getName()));
    }

    public void shutdownJob(String timerJobName){
        auditController.info(JobControl, format("Остановка задачи '%s'", timerJobName));
        backgroundJobService.shutdownJob(timerJobName);
        auditController.info(JobControl, format("Задача '%s' остановлена", timerJobName));
    }

    public ParamsAwareRunnable findParamsAwareRunnableJob(Class<? extends ParamsAwareRunnable> clazz){
        return backgroundJobService.findParamsAwareRunnableJob(clazz);
    }

    public void executeJob(TimerJob timerJob) throws Exception{
        auditController.info(JobControl, format("Запуск задачи '%s' в синхронном режиме", timerJob.getName()));
        backgroundJobService.executeJob(timerJob);
        auditController.info(JobControl, format("Задача '%s' отработала в синхронном режиме", timerJob.getName()));
    }

    public void executeJob(String jobName, Properties properties) throws Exception{
        auditController.info(JobControl, format("Запуск задачи '%s' в синхронном режиме", jobName));
        backgroundJobService.executeJob(jobName, properties);
        auditController.info(JobControl, format("Задача '%s' отработала в синхронном режиме", jobName));
    }

    public List<TimerJobWrapper> updateJob(Long id, String props, JobStartupType type, String scheduleExpression) {
        TimerJob job = getJob(id);
        auditController.info(JobControl, format("Редактирование задачи '%s': свойства '%s', тип '%s', расписание '%s'"
                , job.getName(), props, type, scheduleExpression));
        job.setProperties(props);
        job.setStartupType(type);
        if (job instanceof CalendarJob) {
            ((CalendarJob) job).setScheduleExpression(scheduleExpression);
        }

        timerJobRepository.save(job);

        return getAllJobs();
    }

    public List<TimerJobWrapper> getAllJobs() {
        return timerJobRepository.select(TimerJob.class, "from TimerJob j")
                .stream().map(this::wrap).collect(Collectors.toList());
    }

    public TimerJob getJob(Long id) {
        return timerJobRepository.findById(TimerJob.class, id);
    }

    public TimerJob getJob(String name) {
        return timerJobRepository.selectOne(TimerJob.class, "from TimerJob j where j.name = ?1", name);
    }

    public TimerJobHistoryWrapper createTimerJobHistory(String jobName) {
        return historyRepository.createHeader(jobName, operdayController.getOperday().getCurrentDate()).toWrapper();
    }

    public void executeJobAsync(String jobName, Properties properties, long delay) throws Exception {
        backgroundJobService.executeJobAsync(jobName, properties, delay);
    }

    private TimerJobWrapper wrap(TimerJob timerJob) {
        TimerJobWrapper wrapper = new TimerJobWrapper();
        wrapper.setId(timerJob.getId());
        wrapper.setName(timerJob.getName());
        wrapper.setProperties(timerJob.getProperties());
        wrapper.setDescription(timerJob.getDescription());
        wrapper.setJobType(timerJob.getSchedulingType());
        wrapper.setStartupType(timerJob.getStartupType());
        wrapper.setState(timerJob.getState().getLabel());
        wrapper.setInterval(timerJob.getInterval());
        if (timerJob instanceof CalendarJob) {
            wrapper.setScheduleExpression(((CalendarJob) timerJob).getScheduleExpression());
        }
        return wrapper;
    }
}
