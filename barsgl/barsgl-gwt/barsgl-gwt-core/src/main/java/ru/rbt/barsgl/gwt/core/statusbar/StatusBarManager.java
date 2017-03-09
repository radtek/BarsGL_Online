/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2017
 */
package ru.rbt.barsgl.gwt.core.statusbar;

import ru.rbt.barsgl.gwt.core.events.LocalEventBus;
import ru.rbt.barsgl.gwt.core.events.StatusBarEvent;

/**
 *
 * @author Andrew Samsonov
 */
public class StatusBarManager {

  public static void ChangeStatusBarText(String text, MessageReason reason) {
    LocalEventBus.fireEvent(new StatusBarEvent(text, reason));
  }
    public enum MessageReason {MSG, INFO, ERROR}

    
}
