package ru.rbt.barsgl.gwt.client.dictionary;

import ru.rbt.grid.gwt.client.gridForm.GridForm;
import ru.rbt.barsgl.gwt.client.gridForm.GridFormDlgBase;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.dialogs.FilterCriteria;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;

import java.util.ArrayList;

import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;

/**
 * Created by ER18837 on 01.02.16.
 */
public abstract class AccountTypeTechFormDlg extends GridFormDlgBase {

    protected Column colAccType;
    protected Column colCtrl;

    public AccountTypeTechFormDlg() {
        super("Выбор Accounting Type по техническим счетам");
    }
    @Override
    protected String getGridWidth() {
        return "800px";
    }

    @Override
    protected String getGridHeight() {
        return "400px";
    }

    @Override
    protected GridForm getGridForm() {
        return new AccountTypeGridForm(this.getEditMode());
    }

    protected abstract boolean getEditMode();


    class AccountTypeGridForm extends GridForm {

        public AccountTypeGridForm(boolean editMode) {
            super("Выбор Accounting Type по техническим счетам");
            reconfigure();
            ok.setVisible(editMode);

        }

        private void reconfigure() {
            abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        }

        @Override
        protected String prepareSql() {
            return "select * from GLVD_ACTYPE_TH"; //ACOD not in ('7920','7919','7903','7904','7907','7908')
        }

        @Override
        protected Table prepareTable() {
            Table result = new Table();

            result.addColumn(colAccType = new Column("ACCTYPE", Column.Type.STRING, "Accounting Typе", 30));
                        result.addColumn(new Column("ACTYP_NAME", Column.Type.STRING, "Наименование Accounting Typе", 80));
            result.addColumn(colCtrl = new Column("FL_CTRL", Column.Type.STRING, "Контролируемый", 20, false, false));

                        return result;
        }

        @Override
        public ArrayList<FilterItem> getInitialFilterCriteria(Object[] initialFilterParams) {
            initialFilterParams = getInitialFilterParams();
            ArrayList<FilterItem> list = new ArrayList<FilterItem>();

            if ((initialFilterParams!=null) && (initialFilterParams.length>0)) {
                if (initialFilterParams[0]!=null) {
                    String accType = initialFilterParams[0].toString();

                    if (!isEmpty(accType)) list.add(new FilterItem(colAccType, FilterCriteria.START_WITH, accType));
                }
            }
            return list;
        }

        @Override
        protected Object[] getInitialFilterParams() {
            return AccountTypeTechFormDlg.this.getInitialFilterParams();
        }


        @Override
        public ArrayList<SortItem> getInitialSortCriteria() {
            ArrayList<SortItem> list = new ArrayList<SortItem>();
            list.add(new SortItem("ACCTYPE", Column.Sort.ASC));
            return list;
        }



    }
}
