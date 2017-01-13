package ru.rbt.barsgl.gwt.core.events;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by akichigi on 05.04.16.
 */
public class DataListBoxEvent extends GwtEvent<DataListBoxEventHandler> {
    private String dataListBoxId;

    public static Type<DataListBoxEventHandler> TYPE = new Type<DataListBoxEventHandler>();

    public DataListBoxEvent(String dataListBoxId){
        super();
        this.dataListBoxId = dataListBoxId;
    }

    @Override
    public Type<DataListBoxEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(DataListBoxEventHandler handler) {
        handler.completeLoadData(dataListBoxId);
    }
}
