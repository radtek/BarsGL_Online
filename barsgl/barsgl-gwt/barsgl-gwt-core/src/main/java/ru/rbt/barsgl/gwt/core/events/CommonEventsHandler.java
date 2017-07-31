package ru.rbt.barsgl.gwt.core.events;

import com.google.gwt.event.shared.EventHandler;

/**
 * Created by er17503 on 31.07.2017.
 */
public interface CommonEventsHandler extends EventHandler {
    void event(String id, Object data);
}
