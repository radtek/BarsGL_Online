package ru.rbt.barsgl.gwt.server.rpc.monitoring;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.monitoring.MonitoringWrapper;

/**
 * Created by akichigi on 06.12.16.
 */
@RemoteServiceRelativePath("service/MonitorService")
public interface MonitorService extends RemoteService{
    RpcRes_Base<MonitoringWrapper> getInfo() throws Exception;
}


