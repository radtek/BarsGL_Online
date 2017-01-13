package ru.rbt.barsgl.gwt.client.formmanager;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by akichigi on 17.03.15.
 */
public class StatusBarEvent extends GwtEvent<StatusBarEventHandler> {

    private FormManagerUI.MessageReason reason;
    private String msg;

    public static Type<StatusBarEventHandler> TYPE = new Type<StatusBarEventHandler>();

    public StatusBarEvent(String msg, FormManagerUI.MessageReason reason){
        super();
        this.msg = msg;
        this.reason = reason;
    }

    @Override
    public Type<StatusBarEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(StatusBarEventHandler handler) {
        handler.message(msg, reason);
    }
}
