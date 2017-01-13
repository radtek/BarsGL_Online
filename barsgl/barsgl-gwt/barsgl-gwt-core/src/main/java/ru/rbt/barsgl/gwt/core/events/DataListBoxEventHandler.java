package ru.rbt.barsgl.gwt.core.events;

import com.google.gwt.event.shared.EventHandler;
/**
 * Created by akichigi on 05.04.16.
 */
public interface DataListBoxEventHandler extends EventHandler {
    void completeLoadData(String dataListBoxId);
}
