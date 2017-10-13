package ru.rbt.barsgl.gwt.core.actions;

import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.core.dialogs.FilterDlg;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.events.CommonEvents;
import ru.rbt.barsgl.gwt.core.events.GridEvents;
import ru.rbt.barsgl.gwt.core.events.LocalEventBus;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;

import java.util.ArrayList;


public class FilterAction extends GridAction {
    private FilterDlg dlg;
    private boolean _fireEvent;
    public FilterAction(GridWidget grid) {
        super(grid, null, "Фильтр: Откл.", new Image(ImageConstants.INSTANCE.filter()), 10);
    }

    @Override
    public void execute() {
        _fireEvent = false;
       showDlg();
    }

    public void execute(boolean fireButtonClickEvent) {
        _fireEvent = fireButtonClickEvent;
       showDlg();
    }

    private void showDlg(){
        if (dlg == null) dlg = new FilterDlg();
        dlg.setDlgEvents(this);
        dlg.setAfterCancelEvent(this);
        dlg.show(grid);
    }

    @Override
    public void onDlgOkClick(Object prms){
        dlg.hide();
        ArrayList<FilterItem> filterCriteria = (ArrayList<FilterItem>)prms;
        LocalEventBus.fireEvent(new GridEvents(grid.getId(), GridEvents.EventType.FILTER, filterCriteria));
        if (_fireEvent) LocalEventBus.fireEvent(new CommonEvents("FilterOKed", grid));
    }

    public void clearFilterCriteria(boolean force){
        if (dlg == null) dlg = new FilterDlg();
        dlg.clearAllFilterCriteria(force);
    }

    @Override
    public void afterCancel() {
        if (_fireEvent) LocalEventBus.fireEvent(new CommonEvents("FilterCanceled", grid));
    }
}
