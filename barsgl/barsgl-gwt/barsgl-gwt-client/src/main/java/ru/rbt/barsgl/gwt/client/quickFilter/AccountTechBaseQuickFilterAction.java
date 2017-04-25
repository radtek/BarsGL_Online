package ru.rbt.barsgl.gwt.client.quickFilter;

import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;

/**
 * Created by ER18837 on 20.07.16.
 */
public abstract class AccountTechBaseQuickFilterAction extends QuickFilterAction {

    protected AccountTechQuickFilterParams quickFilterParams;
    protected DlgFrame quickFilterDlg;

    public AccountTechBaseQuickFilterAction(GridWidget grid, AccountTechQuickFilterParams quickFilterParams) {
        super(grid, null, "Быстрый фильтр", new Image(ImageConstants.INSTANCE.quickfilter()), 10);
        this.quickFilterParams = quickFilterParams;
    }

    @Override
    public DlgFrame getFilterDialog() {
        if (quickFilterDlg == null)
            quickFilterDlg = new AccountTechQuickFilterDlg();
        return quickFilterDlg;
    }

    @Override
    public IQuickFilterParams getFilterParams() {
        return quickFilterParams;
    }

}