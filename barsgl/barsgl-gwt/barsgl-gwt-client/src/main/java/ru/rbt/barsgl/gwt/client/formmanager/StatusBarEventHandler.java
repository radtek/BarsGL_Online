package ru.rbt.barsgl.gwt.client.formmanager;

import com.google.gwt.event.shared.EventHandler;
/**
 * Created by akichigi on 17.03.15.
 */


public interface StatusBarEventHandler extends EventHandler{
    public void message(String msg, FormManagerUI.MessageReason reason);
}
