package ru.rbt.barsgl.gwt.client.quickFilter;

import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;

import java.util.Date;

/**
 * Created by akichigi on 03.06.16.
 */
public class DateIntervalQuickFilterAction extends QuickFilterAction {
    protected Column dateColumn;
    public DateIntervalQuickFilterAction(GridWidget grid, Column dateColumn) {
        super(grid, null, "Интервал дат", new Image(ImageConstants.INSTANCE.quickfilter()), 10);
        this.dateColumn = dateColumn;
    }

    @Override
    public DlgFrame getFilterDialog() {
        return new DateIntervalQuickFilterDlg();
    }

    @Override
    public IQuickFilterParams getFilterParams() {
        return new DateIntervalQuickFilterParams(dateColumn);
    }

    @Override
    public Object[] getInitialFilterParams(Date operday, Date prevday) { return new Object[]{operday, operday}; }
}
