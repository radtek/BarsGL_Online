package ru.rbt.barsgl.gwt.core.events;

import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.google.web.bindery.event.shared.SimpleEventBus;
import com.google.web.bindery.event.shared.Event.Type;

public final class LocalEventBus {
	private static final SimpleEventBus eventBus = new SimpleEventBus();
	
	public static final <H> HandlerRegistration addHandler(Type<H> type, H handler) {
		return eventBus.addHandler(type, handler);
	}
	
	public static final void fireEvent(Event<?> event) {
		eventBus.fireEvent(event);
	}

	private LocalEventBus() {}
}
