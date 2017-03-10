/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2016
 */
package ru.rbt.barsgl.gwt.server.rpc.properties;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import ru.rbt.barsgl.shared.RpcRes_Base;

/**
 *
 * @author Andrew Samsonov
 */
@RemoteServiceRelativePath("service/PropertiesService")
public interface PropertiesService extends RemoteService {

  RpcRes_Base<String> getEnvProperty(String propertyName) throws Exception;

  RpcRes_Base<String> getSysProperty(String propertyName) throws Exception;

  RpcRes_Base<String> getDbProperyString(String propertyName) throws Exception;

  RpcRes_Base<Long> getDbProperyLong(String propertyName) throws Exception;

}
