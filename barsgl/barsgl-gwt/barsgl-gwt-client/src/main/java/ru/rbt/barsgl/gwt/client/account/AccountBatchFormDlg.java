package ru.rbt.barsgl.gwt.client.account;

import com.google.gwt.user.client.Window;
import ru.rbt.barsgl.gwt.client.gridForm.GridFormDlgBase;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.shared.filter.FilterCriteria;
import ru.rbt.grid.gwt.client.gridForm.GridForm;

import java.util.ArrayList;

import static ru.rbt.barsgl.gwt.client.account.AccountBatchErrorForm.ViewType.V_ERROR;

/**
 * Created by er18837 on 24.10.2018.
 */
public abstract class AccountBatchFormDlg extends GridFormDlgBase {

    public static final String FORM_NAME = "Строки пакета с запросами по счетам";
    public static final String FORM_NAME_ERROR = "Строки загруженного файла с ошибками";

    public abstract AccountBatchErrorForm.ViewType getViewType();

    public AccountBatchFormDlg(AccountBatchErrorForm.ViewType viewType) {
        super(viewType == V_ERROR ? FORM_NAME_ERROR : FORM_NAME);
        ok.setVisible(false);
    }

    protected String getGridWidth() {
        return "1200px";
    }
    protected String getGridHeight() {
        return "600px";
    }

    @Override
    protected GridForm getGridForm() {
        return new  AccountBatchErrorForm(getViewType() == V_ERROR ? FORM_NAME_ERROR : FORM_NAME) {

            @Override
            public ViewType getViewType() {
                return AccountBatchFormDlg.this.getViewType();
            }

            @Override
            public ArrayList<FilterItem> getInitialFilterCriteria(Object[] initialFilterParams) {
                Long idPackage = (Long)initialFilterParams[0];

                ArrayList<FilterItem> list = new ArrayList<FilterItem>();
                list.add(new FilterItem(_colIdPackage, FilterCriteria.EQ, idPackage, true, true, false));
                if (_viewType == V_ERROR) {
//                    list.add(new FilterItem(_colError, FilterCriteria.NOT_EMPTY, null, false));
                    list.add(new FilterItem(_colState, FilterCriteria.START_WITH, "ERR", true));
                }
                return list;
            }

            @Override
            public Object[] getInitialFilterParams() {
                return AccountBatchFormDlg.this.getInitialFilterParams();
            }
        };
    }

}
