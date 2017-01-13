package ru.rbt.barsgl.gwt.core.actions;

import com.google.gwt.user.client.ui.Image;
import com.google.web.bindery.event.shared.HandlerRegistration;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.events.*;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;
import ru.rbt.barsgl.gwt.core.widgets.ISortEvents;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by akichigi on 21.04.15.
 */
public abstract class RefreshAction extends Action implements ISortEvents {
    private GridWidget grid;
    private List<FilterItem> filterCriteria = null;
    private List<SortItem> sortCriteria = null;

    private List<FilterItem> linkMDFilterCriteria = null;
    private HandlerRegistration registration;

    public RefreshAction(GridWidget grid) {
        super(null, "Обновить", new Image(ImageConstants.INSTANCE.refresh24()));
        setSeparator(5);

        this.grid = grid;

        registration = LocalEventBus.addHandler(GridEvents.TYPE, createGridEventsHandler());
    }

    public void onSorted(List<SortItem> sortCriteria){
        this.sortCriteria = sortCriteria;
        execute();
    }

    public List<SortItem> getSortCriteria() {
        return sortCriteria;
    }

    public void setSortCriteria(List<SortItem> sortCriteria) {
        this.sortCriteria = sortCriteria;
    }

    public void setFilterCriteria(List<FilterItem> filterCriteria) {
        this.filterCriteria = filterCriteria;
    }

    private GridEventsHandler createGridEventsHandler(){
        return new GridEventsHandler() {
            @Override
            public void onEvent(String id, GridEvents.EventType type, Object param) {
                if (id == null || (!id.equalsIgnoreCase(grid.getId()))) return;

                switch (type){
                    case FILTER:
                        RefreshAction.this.filterCriteria = (List<FilterItem>) param;
                        break;
                    case LAZY_MASTER_ROW_CHANGED:
                        RefreshAction.this.linkMDFilterCriteria = (List<FilterItem>) param;
                        break;
                    case MASTER_ROW_CHANGED:
                        RefreshAction.this.linkMDFilterCriteria = (List<FilterItem>) param;
                        break;
                    default: return;
                }
                if (type != GridEvents.EventType.LAZY_MASTER_ROW_CHANGED){
                    execute();
                }
            }
        };
    }

    @Override
    public void execute() {
       onRefresh(filterCriteria, sortCriteria, linkMDFilterCriteria);
    }

    public abstract void onRefresh(List<FilterItem> filterCriteria, List<SortItem> sortCriteria,
                                   List<FilterItem> linkMDFilterCriteria);

    @Override
    public void dispose(){
        registration.removeHandler();
    }
}
