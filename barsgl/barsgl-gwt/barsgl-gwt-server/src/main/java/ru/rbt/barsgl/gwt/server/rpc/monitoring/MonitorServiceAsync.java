package ru.rbt.barsgl.gwt.server.rpc.monitoring;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.monitoring.MonitoringWrapper;

/**
 * Created by akichigi on 06.12.16.
 */
public interface MonitorServiceAsync {
    void getInfo(AsyncCallback<RpcRes_Base<MonitoringWrapper>> callback) throws Exception;


}
