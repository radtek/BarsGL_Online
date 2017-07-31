package ru.rbt.barsgl.gwt.core.events;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by er17503 on 31.07.2017.
 */
public class CommonEvents extends GwtEvent<CommonEventsHandler> {
    public static Type<CommonEventsHandler>  TYPE = new Type<CommonEventsHandler>();

    private String id;
    private Object data;

    public CommonEvents(String id, Object data){
        this.id = id;
        this.data = data;
    }

    @Override
    public Type<CommonEventsHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(CommonEventsHandler handler) {
         handler.event(id, data);
    }
}
