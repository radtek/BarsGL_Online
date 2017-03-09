package ru.rbt.barsgl.gwt.core.events;

import com.google.gwt.event.shared.GwtEvent;
import ru.rbt.barsgl.gwt.core.statusbar.StatusBarManager;

/**
 * Created by akichigi on 17.03.15.
 */
public class StatusBarEvent extends GwtEvent<StatusBarEventHandler> {

    private StatusBarManager.MessageReason reason;
    private String msg;

    public static Type<StatusBarEventHandler> TYPE = new Type<StatusBarEventHandler>();

    public StatusBarEvent(String msg, StatusBarManager.MessageReason reason){
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
