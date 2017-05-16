package ru.rbt.security.gwt.server.rpc.monitoring;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.shared.RpcRes_Base;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by akichigi on 06.12.16.
 */
public interface MonitorServiceAsync {
//    void getInfo(AsyncCallback<RpcRes_Base<MonitoringWrapper>> callback) throws Exception;
    void getBuff(AsyncCallback<RpcRes_Base<HashMap>> callback) throws Exception;
    void getOper(AsyncCallback<RpcRes_Base<ArrayList>> callback) throws Exception;
    void getRepl(AsyncCallback<RpcRes_Base<HashMap>> callback) throws Exception;
}
