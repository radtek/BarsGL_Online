package ru.rbt.barsgl.gwt.client.quickFilter;

import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;

import java.util.Date;

/**
 * Created by ER18837 on 18.04.16.
 */
public class DateQuickFilterAction extends QuickFilterAction {
    private Column colCreateDate, colValueDate, colPostDate;
    private DateQuickFilterParams.DateFilterField filterColumn;
    private boolean fromPrevDay;

    public DateQuickFilterAction(GridWidget grid, Column fieldCreateDate, Column fieldValueDate, Column fieldPostDate, DateQuickFilterParams.DateFilterField filterField, boolean fromPrevDay) {
        super(grid, null, "Быстрый фильтр", new Image(ImageConstants.INSTANCE.quickfilter()), 10);
        this.colCreateDate = fieldCreateDate;
        this.colValueDate = fieldValueDate;
        this.colPostDate = fieldPostDate;
        this.filterColumn = filterField;
        this.fromPrevDay = fromPrevDay;
    }


    @Override
    public DlgFrame getFilterDialog() {
        return new DateQuickFilterDlg();
    }

    @Override
    public IQuickFilterParams getFilterParams() {
        return new DateQuickFilterParams(colCreateDate, colValueDate, colPostDate, filterColumn);
    }

    @Override
    public Object[] getInitialFilterParams(Date operday, Date prevday) {
        return new Object[] {fromPrevDay ? prevday : operday, operday};
    }
}
