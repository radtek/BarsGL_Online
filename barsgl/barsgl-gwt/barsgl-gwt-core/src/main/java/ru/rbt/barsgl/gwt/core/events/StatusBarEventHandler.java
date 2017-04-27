package ru.rbt.barsgl.gwt.core.events;

import com.google.gwt.event.shared.EventHandler;
import ru.rbt.barsgl.gwt.core.statusbar.StatusBarManager;
/**
 * Created by akichigi on 17.03.15.
 */


public interface StatusBarEventHandler extends EventHandler{
    public void message(String msg, StatusBarManager.MessageReason reason);
}
