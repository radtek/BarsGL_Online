package ru.rbt.barsgl.gwt.core.events;

import com.google.gwt.event.shared.EventHandler;

/**
 * Created by akichigi on 23.04.15.
 */
public interface GridEventsHandler extends EventHandler {
    public void onEvent(String id, GridEvents.EventType type, Object param);
}
