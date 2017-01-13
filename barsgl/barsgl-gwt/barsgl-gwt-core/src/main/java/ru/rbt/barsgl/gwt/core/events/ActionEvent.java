package ru.rbt.barsgl.gwt.core.events;

import ru.rbt.barsgl.gwt.core.actions.Action;

import com.google.gwt.event.shared.GwtEvent;

public class ActionEvent extends GwtEvent<ActionEventHandler> {

	private Action action;
	private Action.ChangeReason reason;
	
	public static Type<ActionEventHandler> TYPE = new Type<ActionEventHandler>();
	
	public ActionEvent(Action action, Action.ChangeReason reason) {
		super();
		this.action = action;
		this.reason = reason;
	}
	
	@Override
	public Type<ActionEventHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(ActionEventHandler handler) {
		handler.doActionChange(action, reason);
	}

}
