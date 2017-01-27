/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2016
 * Financial Board Project
 */
package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.properties.PropertiesServiceSupport;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Andrew Samsonov
 */
public class PropertiesServiceSupportTest extends AbstractRemoteTest {

  private static final Logger logger = Logger.getLogger(AccountOpenAePostingsTest.class.getName());

  @Test
  public void getEnvProperty() throws Exception {
    String propertyName = "java:app/env/SchedTableName";
    String value = remoteAccess.invoke(PropertiesServiceSupport.class, "getEnvProperty", propertyName);
    logger.log(Level.INFO, "EnvProperty value: {0}", value);
    Assert.assertNotNull(value);
  }

  @Test
  public void getSysProperty() throws Exception {
    String propertyName = "weblogic.Name";
    String value = remoteAccess.invoke(PropertiesServiceSupport.class, "getSysProperty", propertyName);
    logger.log(Level.INFO, "SysProperty value: {0}", value);
    Assert.assertNotNull(value);
  }

  @Test
  public void getDbProperyString() throws Exception {
    String propertyName = "mc.queues.param";
    String value = remoteAccess.invoke(PropertiesServiceSupport.class, "getDbProperyString", propertyName);
    logger.log(Level.INFO, "DbProperyString value: {0}", value);
    Assert.assertNotNull(value);
  }

  @Test
  public void getDbProperyLong() throws Exception {
    String propertyName = "fan.batchsize";
    Long value = remoteAccess.invoke(PropertiesServiceSupport.class, "getDbProperyLong", propertyName);
    logger.log(Level.INFO, "DbProperyInteger value: {0}", value);
    Assert.assertNotNull(value);
  }

}
