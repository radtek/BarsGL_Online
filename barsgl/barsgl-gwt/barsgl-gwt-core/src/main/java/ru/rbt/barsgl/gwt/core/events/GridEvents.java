package ru.rbt.barsgl.gwt.core.events;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by akichigi on 23.04.15.
 */
public class GridEvents extends GwtEvent<GridEventsHandler> {
    public static Type<GridEventsHandler>  TYPE = new Type<GridEventsHandler>();

    private String eventId;
    private GridEvents.EventType eventType;
    private Object eventParam;

    public GridEvents(String id, GridEvents.EventType type, Object param ){
        super();
        eventId = id;
        eventType = type;
        eventParam = param;
    }

    @Override
    public GwtEvent.Type<GridEventsHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(GridEventsHandler handler) {
        handler.onEvent(eventId, eventType, eventParam);
    }

    //типы событий
    public enum EventType {
       // SORT,
        LOAD_DATA,
        FILTER,
        MASTER_ROW_CHANGED,
        LAZY_MASTER_ROW_CHANGED
    }
}
