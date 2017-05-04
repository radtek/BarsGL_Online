package ru.rbt.security.gwt.server.rpc.monitoring;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import ru.rbt.barsgl.shared.RpcRes_Base;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by akichigi on 06.12.16.
 */
@RemoteServiceRelativePath("service/MonitorService")
public interface MonitorService extends RemoteService{
//    RpcRes_Base<MonitoringWrapper> getInfo() throws Exception;
    RpcRes_Base<HashMap> getBuff() throws Exception;
    RpcRes_Base<ArrayList> getOper() throws Exception;
    RpcRes_Base<HashMap> getRepl() throws Exception;
}


