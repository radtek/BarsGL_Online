package ru.rbt.barsgl.gwt.core.events;

import ru.rbt.barsgl.gwt.core.actions.Action;

import com.google.gwt.event.shared.EventHandler;

public interface ActionEventHandler extends EventHandler {
	public void doActionChange(Action action, Action.ChangeReason reason);
}
