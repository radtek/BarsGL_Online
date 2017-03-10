/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2016
 */
package ru.rbt.barsgl.gwt.server.rpc.properties;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.shared.RpcRes_Base;

/**
 *
 * @author Andrew Samsonov
 */
public interface PropertiesServiceAsync {

  void getEnvProperty(String propertyName, AsyncCallback<RpcRes_Base<String>> callback);

  void getSysProperty(String propertyName, AsyncCallback<RpcRes_Base<String>> callback);

  void getDbProperyString(String propertyName, AsyncCallback<RpcRes_Base<String>> callback);

  void getDbProperyLong(String propertyName, AsyncCallback<RpcRes_Base<Long>> callback);

}
