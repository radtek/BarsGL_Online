/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2017
 */
package ru.rbt.tasks.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import ru.rbt.tasks.gwt.server.rpc.job.TimerJobService;
import ru.rbt.tasks.gwt.server.rpc.job.TimerJobServiceAsync;

/**
 *
 * @author Andrew Samsonov
 */
public class TimerEntryPoint implements EntryPoint {

  public static TimerJobServiceAsync timerJobService;

  public void onModuleLoad() {
        timerJobService = GWT.create(TimerJobService.class);
  }
  
}
