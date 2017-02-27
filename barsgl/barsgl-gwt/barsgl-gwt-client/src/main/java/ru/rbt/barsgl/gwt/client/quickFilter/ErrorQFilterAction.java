package ru.rbt.barsgl.gwt.client.quickFilter;

import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;

import java.util.Date;

/**
 * Created by akichigi on 21.02.17.
 */
public class ErrorQFilterAction extends QuickFilterAction {
    private Column colDealSource;
    private Column colOperDay;

    public ErrorQFilterAction(GridWidget grid, Column colDealSource, Column colOperDay){
        super(grid, null, "Быстрый фильтр", new Image(ImageConstants.INSTANCE.quickfilter()), 10);
        this.colDealSource = colDealSource;
        this.colOperDay = colOperDay;
    }

    @Override
    public DlgFrame getFilterDialog() {
        return new ErrorQFilterDlg();
    }

    @Override
    public IQuickFilterParams getFilterParams() {
        return new ErrorQFilterParams(colDealSource, colOperDay);
    }

    @Override
    public Object[] getInitialFilterParams(Date operday, Date prevday) {
        return new Object[]{"", prevday};
    }
}
