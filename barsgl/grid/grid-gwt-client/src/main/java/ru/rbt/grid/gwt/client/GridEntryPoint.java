/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2017
 */
package ru.rbt.grid.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import ru.rbt.grid.gwt.server.rpc.asyncGrid.AsyncGridService;
import ru.rbt.grid.gwt.server.rpc.asyncGrid.AsyncGridServiceAsync;

/**
 *
 * @author Andrew Samsonov
 */
public class GridEntryPoint implements EntryPoint {
  public static AsyncGridServiceAsync asyncGridService;

  @Override
  public void onModuleLoad() {
    asyncGridService = GWT.create(AsyncGridService.class);
  }
  
}
