/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2016
 * Financial Board Project
 */
package ru.rbt.barsgl.gwt.server.rpc.properties;

import ru.rbt.barsgl.ejb.properties.PropertiesServiceSupport;
import ru.rbt.barsgl.gwt.server.rpc.AbstractGwtService;
import ru.rbt.barsgl.shared.RpcRes_Base;

/**
 *
 * @author Andrew Samsonov
 */
public class PropertiesServiceImpl extends AbstractGwtService implements PropertiesService {

  @Override
  public RpcRes_Base<String> getEnvProperty(String propertyName) throws Exception {
    return new RpcRes_Base<>((String) localInvoker.invoke(PropertiesServiceSupport.class, "getEnvProperty", propertyName), false, "");
  }

  @Override
  public RpcRes_Base<String> getSysProperty(String propertyName) throws Exception {
    return new RpcRes_Base<>((String) localInvoker.invoke(PropertiesServiceSupport.class, "getSysProperty", propertyName), false, "");
  }

  @Override
  public RpcRes_Base<String> getDbProperyString(String propertyName) throws Exception {
    return new RpcRes_Base<>((String) localInvoker.invoke(PropertiesServiceSupport.class, "getDbProperyString", propertyName), false, "");
  }

  @Override
  public RpcRes_Base<Long> getDbProperyLong(String propertyName) throws Exception {
    return new RpcRes_Base<>((Long) localInvoker.invoke(PropertiesServiceSupport.class, "getDbProperyLong", propertyName), false, "");
  }
  
}
