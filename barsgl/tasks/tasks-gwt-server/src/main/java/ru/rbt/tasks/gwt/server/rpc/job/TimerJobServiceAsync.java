package ru.rbt.tasks.gwt.server.rpc.job;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.shared.jobs.TimerJobWrapper;

import java.util.List;
import java.util.Map;

/**
 * Created by akichigi on 11.03.15.
 */
public interface TimerJobServiceAsync {
    void  getAllJobs(AsyncCallback<List<TimerJobWrapper>> callback);
    void  save(TimerJobWrapper wrapper, AsyncCallback<List<TimerJobWrapper>> callback);

    void  startupAll(AsyncCallback<List<TimerJobWrapper>> callback);
    void  shutdownAll(AsyncCallback<List<TimerJobWrapper>> callback);
    void  startupJob(Long id, AsyncCallback<List<TimerJobWrapper>> callback);
    void  shutdownJob(Long id, AsyncCallback<List<TimerJobWrapper>> callback);
    void  executeJob(Long id, AsyncCallback<List<TimerJobWrapper>> callback);
    void  executeJob(String jobName, Map<String,String> params, AsyncCallback<List<TimerJobWrapper>> callback);
    void  flushCache(AsyncCallback callback);

}
