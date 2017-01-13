package ru.rbt.barsgl.gwt.server.rpc.sync;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import ru.rbt.barsgl.ejb.job.BackgroundJobsController;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.jobs.TimerJobHistoryWrapper;

/**
 * Created by Ivan Sevastyanov on 01.11.2016.
 */
@RemoteServiceRelativePath("service/PdSyncService")
public interface PdSyncService extends RemoteService {
    RpcRes_Base<TimerJobHistoryWrapper> execPdSync() throws Exception;
}
