package ru.rbt.barsgl.gwt.core.actions;

import com.google.web.bindery.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Image;

import ru.rbt.barsgl.gwt.core.dialogs.IAfterCancelEvent;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterShowEvent;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;
import ru.rbt.barsgl.gwt.core.dialogs.IDlgEvents;
import ru.rbt.barsgl.gwt.core.events.GridEvents;
import ru.rbt.barsgl.gwt.core.events.GridEventsHandler;
import ru.rbt.barsgl.gwt.core.events.LocalEventBus;
import ru.rbt.barsgl.gwt.core.events.GridEvents.EventType;

/**
 * Created by akichigi on 02.04.15.
 */
public abstract class GridAction extends Action implements IDlgEvents, IAfterCancelEvent, IAfterShowEvent{
	private boolean disableOnZeroRows;
    protected GridWidget grid;
    private HandlerRegistration registration;
    private IAfterRefreshEvent afterRefreshEvent;

    public GridAction(GridWidget grid, String name, String hint, Image image, double separator, boolean disableOnZeroRows){
    	super(name, hint, image, separator);
    	this.grid = grid;
    	this.disableOnZeroRows = disableOnZeroRows;
    	registration = LocalEventBus.addHandler(GridEvents.TYPE, createGridEventsHandler());
    }
    
    public GridAction(GridWidget grid, String name, String hint, Image image, double separator){
    	this(grid, name, hint, image, separator, false);
    }

    public boolean isDisableOnZeroRows(){
    	return disableOnZeroRows;
    }
    
    private GridEventsHandler createGridEventsHandler() {
    	return new GridEventsHandler() {
			@Override
			public void onEvent(String id, EventType type, Object param) {
				 if (id == null || (!id.equalsIgnoreCase(grid.getId()))) return;
				 if (type == EventType.LOAD_DATA && disableOnZeroRows) {
					 setEnable((Integer)param != 0);
				 }
				if (type == EventType.LOAD_DATA && afterRefreshEvent != null)
					afterRefreshEvent.afterRefresh((Integer) param);
			}
		};
    }


    @Override
	public void dispose(){
    	registration.removeHandler();
    }
    
    public abstract void execute();

    @Override
    public void onDlgOkClick(Object prms) throws Exception { }

	@Override
	public void afterCancel() {

	}

	@Override
	public void afterShow() {

	}

	public void setAfterRefreshEvent(IAfterRefreshEvent afterRefreshEvent) {
		this.afterRefreshEvent = afterRefreshEvent;
	}
}
