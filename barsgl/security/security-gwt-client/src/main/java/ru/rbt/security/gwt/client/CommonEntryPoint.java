/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2017
 */
package ru.rbt.security.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import java.util.Date;
import ru.rbt.security.gwt.server.rpc.monitoring.MonitorService;
import ru.rbt.security.gwt.server.rpc.monitoring.MonitorServiceAsync;
import ru.rbt.security.gwt.server.rpc.operday.info.OperDayInfoService;
import ru.rbt.security.gwt.server.rpc.operday.info.OperDayInfoServiceAsync;

/**
 *
 * @author Andrew Samsonov
 */
public class CommonEntryPoint implements EntryPoint {

  public static OperDayInfoServiceAsync operDayService;
  public static MonitorServiceAsync monitorService;

  public static Date CURRENT_WORKDAY;
  
  @Override
  public void onModuleLoad() {
    monitorService = GWT.create(MonitorService.class);
    operDayService = GWT.create(OperDayInfoService.class);
  }
  
}
