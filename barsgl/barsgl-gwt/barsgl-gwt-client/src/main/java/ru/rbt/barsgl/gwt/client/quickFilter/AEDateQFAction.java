package ru.rbt.barsgl.gwt.client.quickFilter;

import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;

import java.util.Date;

/**
 * Created by akichigi on 02.08.16.
 */
public class AEDateQFAction extends QuickFilterAction {
    private Column colPkgDate, colValueDate;
    private AEDateQFParams.DateFilterField filterColumn;
    private boolean fromPrevDay;

    public AEDateQFAction(GridWidget grid, Column fieldPkgDate, Column fieldValueDate, AEDateQFParams.DateFilterField filterField, boolean fromPrevDay) {
        super(grid, null, "Быстрый фильтр", new Image(ImageConstants.INSTANCE.quickfilter()), 10);
        this.colPkgDate = fieldPkgDate;
        this.colValueDate = fieldValueDate;
        this.filterColumn = filterField;
        this.fromPrevDay = fromPrevDay;
    }

    @Override
    public DlgFrame getFilterDialog() {
        return new AEDateQFDlg();
    }

    @Override
    public IQuickFilterParams getFilterParams() {
        return new AEDateQFParams(colPkgDate, colValueDate, filterColumn);
    }

    @Override
    public Object[] getInitialFilterParams(Date operday, Date prevday) {
        return new Object[] {fromPrevDay ? prevday : operday, operday};
    }
}
