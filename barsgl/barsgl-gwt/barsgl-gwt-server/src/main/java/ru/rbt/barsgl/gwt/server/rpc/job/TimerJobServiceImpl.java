package ru.rbt.barsgl.gwt.server.rpc.job;

import ru.rbt.barsgl.ejb.job.BackgroundJobsController;
import ru.rbt.barsgl.ejbcore.AsyncProcessor;
import ru.rbt.barsgl.ejbcore.CacheController;
import ru.rbt.barsgl.ejbcore.mapping.job.TimerJob;
import ru.rbt.barsgl.ejbcore.repository.PropertiesRepository;
import ru.rbt.barsgl.gwt.server.rpc.AbstractGwtService;
import ru.rbt.barsgl.shared.jobs.TimerJobWrapper;

import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 * Created by akichigi on 11.03.15.
 */
public class TimerJobServiceImpl extends AbstractGwtService implements TimerJobService {

    @Override
    public List<TimerJobWrapper> getAllJobs() throws Exception {
        localInvoker.invoke(BackgroundJobsController.class, "refreshJobsStatus");
        return localInvoker.invoke(BackgroundJobsController.class, "getAllJobs");
    }

    @Override
    public List<TimerJobWrapper> save(TimerJobWrapper wrapper) throws Exception{
        return localInvoker.invoke(BackgroundJobsController.class, "updateJob", wrapper.getId(),
                wrapper.getProperties(), wrapper.getStartupType(), wrapper.getScheduleExpression());
    }

    @Override
    public List<TimerJobWrapper> startupAll() throws Exception{
        localInvoker.invoke(BackgroundJobsController.class, "startupAll");
        return getAllJobs();
    }

    @Override
    public List<TimerJobWrapper> shutdownAll() throws Exception{
        localInvoker.invoke(BackgroundJobsController.class, "shutdownAll");
        return getAllJobs();
    }

    @Override
    public List<TimerJobWrapper> startupJob(Long id) throws Exception{
        localInvoker.invoke(BackgroundJobsController.class, "startupJob", getJob(id));
        return getAllJobs();
    }

    @Override
    public List<TimerJobWrapper> shutdownJob(Long id) throws Exception{
        localInvoker.invoke(BackgroundJobsController.class, "shutdownJob", getJob(id));
        return getAllJobs();
    }

    @Override
    public List<TimerJobWrapper> executeJob(Long id) throws Exception{
        localInvoker.invoke(BackgroundJobsController.class, "executeJob", getJob(id));
        return getAllJobs();
    }

    @Override
    public List<TimerJobWrapper> executeJob(String jobName, Map<String,String> params) throws Exception {
        Properties properties = new Properties();
        for (String key : params.keySet()) {
            properties.setProperty(key, params.getOrDefault(key, ""));
        }
        localInvoker.invoke(BackgroundJobsController.class, "executeJob", jobName, properties);
        return getAllJobs();
    }

    private TimerJob getJob(final Long id) throws Exception{
        return localInvoker.invoke(BackgroundJobsController.class, "getJob", id);
    }

    @Override
    public void flushCache() throws Exception {
        localInvoker.invoke(CacheController.class, "flushAllCaches");
        localInvoker.invoke(AsyncProcessor.class, "initConcurrency");
    }
}
