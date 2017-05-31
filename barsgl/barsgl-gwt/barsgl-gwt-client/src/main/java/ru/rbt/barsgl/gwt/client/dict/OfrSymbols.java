package ru.rbt.barsgl.gwt.client.dict;

import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.client.dict.dlg.OfrQuickFilterDlg;
import ru.rbt.barsgl.gwt.client.dict.dlg.OfrQuickFilterParams;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.dialogs.FilterCriteria;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.events.GridEvents;
import ru.rbt.barsgl.gwt.core.events.LocalEventBus;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;

import java.util.ArrayList;
import java.util.Date;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;

/**
 * Created by ER18837 on 11.01.16.
 */
/*don't used IMBCBHBPN ???*/
public class OfrSymbols extends OfrSymbolsBase {
    public final static String FORM_NAME = "План счетов ОФР по Midas";

    private OfrQuickFilterParams filterParams = null;

    public OfrSymbols() {
        super(FORM_NAME);
        reconfigure();
    }

    private void reconfigure() {
        abw.addAction(createFilterAction());
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
    }

    private GridAction createFilterAction() {
        return new GridAction(grid, null, "Быстрый фильтр", new Image(ImageConstants.INSTANCE.quickfilter()), 10) {
            private OfrQuickFilterDlg dlg;

            @Override
            public void execute() {
            	dlg = new OfrQuickFilterDlg();
            	dlg.setDlgEvents(this);
            	dlg.show(filterParams);
            }

            @Override
            public void onDlgOkClick(Object prms) {
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
                filterParams = (OfrQuickFilterParams)prms;
                ArrayList<FilterItem> filterCriteria = makeReportFilter(filterParams);
                dlg.hide();
                LocalEventBus.fireEvent(new GridEvents(grid.getId(), GridEvents.EventType.FILTER, filterCriteria));
//                refreshAction.execute();
                WaitingManager.hide();
            }
        };
    }

    private ArrayList<FilterItem> makeReportFilter(OfrQuickFilterParams filterParams) {
        ArrayList<FilterItem> list = new ArrayList<FilterItem>();
        Date date = filterParams.getReportDate();
        switch (filterParams.getReportState()) {
            case All:
                if (null != date) {
                    list.add(new FilterItem(colDateBegin, FilterCriteria.LE, date));
                }
                break;
            case Active:
                list.add(new FilterItem(colDateBegin, FilterCriteria.LE, date));
                list.add(new FilterItem(colDateEnd, FilterCriteria.GE, date));
                break;
            case Inactive:
                list.add(new FilterItem(colDateEnd, FilterCriteria.LT, date));
                break;
        }
        return list;
    }

}
