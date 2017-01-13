package ru.rbt.barsgl.gwt.server.rpc.job;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import ru.rbt.barsgl.shared.jobs.TimerJobWrapper;

import java.util.List;
import java.util.Map;

/**
 * Created by akichigi on 10.03.15.
 */
@RemoteServiceRelativePath("service/TimerJobService")
public interface TimerJobService extends RemoteService {

    List<TimerJobWrapper> getAllJobs() throws Exception;
    List<TimerJobWrapper> save(TimerJobWrapper wrapper) throws Exception;

    List<TimerJobWrapper> startupAll() throws Exception;
    List<TimerJobWrapper> shutdownAll() throws Exception;
    List<TimerJobWrapper> startupJob(Long id) throws Exception;
    List<TimerJobWrapper> shutdownJob(Long id) throws Exception;
    List<TimerJobWrapper> executeJob(Long id) throws Exception;
    List<TimerJobWrapper> executeJob(String jobName, Map<String,String> params) throws Exception;
    void flushCache() throws Exception;
}
