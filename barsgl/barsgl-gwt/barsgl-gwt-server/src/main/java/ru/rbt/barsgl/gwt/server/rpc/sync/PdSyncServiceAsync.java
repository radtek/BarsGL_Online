package ru.rbt.barsgl.gwt.server.rpc.sync;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.jobs.TimerJobHistoryWrapper;

/**
 * Created by Ivan Sevastyanov on 01.11.2016.
 */
public interface PdSyncServiceAsync {
    void  execPdSync(AsyncCallback<RpcRes_Base<TimerJobHistoryWrapper>> callback);
}
