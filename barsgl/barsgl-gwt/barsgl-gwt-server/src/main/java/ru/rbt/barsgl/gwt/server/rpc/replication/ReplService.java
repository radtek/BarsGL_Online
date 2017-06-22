package ru.rbt.barsgl.gwt.server.rpc.replication;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.jobs.TimerJobHistoryWrapper;

/**
 * Created by er17503 on 08.06.2017.
 */
@RemoteServiceRelativePath("service/ReplService")
public interface ReplService extends RemoteService {
    RpcRes_Base<TimerJobHistoryWrapper> Test() throws Exception;
}
