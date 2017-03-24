package ru.rbt.barsgl.gwt.client.quickFilter;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Image;
import ru.rbt.security.gwt.client.operday.IDataConsumer;
import ru.rbt.security.gwt.client.operday.OperDayGetter;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.events.GridEvents;
import ru.rbt.barsgl.gwt.core.events.LocalEventBus;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;

import java.util.ArrayList;
import java.util.Date;

import static ru.rbt.security.gwt.client.operday.OperDayGetter.getOperday;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;

/**
 * Created by ER18837 on 27.04.16.
 */
public abstract class QuickFilterAction extends GridAction {

    protected DlgFrame dlg;
    protected IQuickFilterParams filterParams = null;

    public QuickFilterAction(GridWidget grid, String name, String hint, Image image, double separator) {
        super(grid, name, hint, image, separator);
    }

    abstract public DlgFrame getFilterDialog();
    abstract public IQuickFilterParams getFilterParams();
    abstract public Object[] getInitialFilterParams(Date operday, Date prevday);

    public void beforeFireFilterEvent(IQuickFilterParams filterParams) {};

    @Override
    public void execute() {
        dlg = getFilterDialog();
        dlg.setDlgEvents(this);

        if (null != filterParams) {
            dlg.show(filterParams);
            return;
        }
        getOperday(new IDataConsumer<OperDayWrapper>() {
            @Override
            public void accept(OperDayWrapper wrapper) {
                filterParams = makeFilterParams(wrapper);
                dlg.show(filterParams);
            }
        });
    }

    @Override
    public void onDlgOkClick(Object prms) {
        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
        filterParams = (IQuickFilterParams) prms;
        ArrayList<FilterItem> filterCriteria = filterParams.getFilter();
        dlg.hide();
        beforeFireFilterEvent(filterParams);
        LocalEventBus.fireEvent(new GridEvents(grid.getId(), GridEvents.EventType.FILTER, filterCriteria));
//                refreshAction.execute();
        WaitingManager.hide();
    }

    protected IQuickFilterParams makeFilterParams(OperDayWrapper wrapper) {
        Date operday = DateTimeFormat.getFormat(OperDayGetter.dateFormat).parse(wrapper.getCurrentOD());
        Date prevday = DateTimeFormat.getFormat(OperDayGetter.dateFormat).parse(wrapper.getPreviousOD());
        IQuickFilterParams filterParams = getFilterParams();
        filterParams.setInitialFilterParams(getInitialFilterParams(operday, prevday));
        return filterParams;
    }
}
