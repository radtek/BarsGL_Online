package ru.rbt.barsgl.gwt.client.quickFilter;

import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.client.events.ae.OperHistoryDlg;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;

/**
 * Created by er18837 on 23.10.2018.
 */
public class DateOwnHistQuickFilterAction extends DateIntervalQuickFilterAction {
    private Column histColumn;

    public DateOwnHistQuickFilterAction(GridWidget grid, Column dateColumn, Column histColumn) {
        super(grid, dateColumn);
        this.histColumn = histColumn;
    }

    @Override
    public DlgFrame getFilterDialog() {
        return new OperHistoryDlg() {
            @Override
            public Widget createContent() {
                Widget panel = super.createContent();
                _choicePanel.setVisible(false);
                return panel;
            }

        };
    }

    @Override
    public IQuickFilterParams getFilterParams() {
        return new DateHistoryQuickFilterParams(dateColumn, histColumn);
    }

}
