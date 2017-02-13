package ru.rbt.security.gwt.client.formmanager;

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.user.client.ui.Widget;

/**
 * Created by akichigi on 15.08.16.
 */
public class FormEvent extends GwtEvent<FormEventHandler> {
    private Widget form;

    public static Type<FormEventHandler> TYPE = new Type<FormEventHandler>();

    public FormEvent(Widget form){
        super();
        this.form = form;
    }

    @Override
    public Type<FormEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(FormEventHandler handler) {
        handler.show(form);
    }
}
