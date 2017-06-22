package ru.rbt.barsgl.gwt.server.rpc.replication;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.jobs.TimerJobHistoryWrapper;

/**
 * Created by er17503 on 08.06.2017.
 */
public interface ReplServiceAsync {
    void Test(AsyncCallback<RpcRes_Base<TimerJobHistoryWrapper>> callback);
}
