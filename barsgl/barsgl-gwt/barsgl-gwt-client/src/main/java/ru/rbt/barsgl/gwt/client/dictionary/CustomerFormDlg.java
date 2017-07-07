package ru.rbt.barsgl.gwt.client.dictionary;

import ru.rbt.barsgl.gwt.client.gridForm.GridFormDlgBase;
import ru.rbt.grid.gwt.client.gridForm.GridForm;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.shared.filter.FilterCriteria;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;

import java.util.ArrayList;

import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;

/**
 * Created by ER18837 on 03.02.16.
 */
public abstract class CustomerFormDlg extends GridFormDlgBase {

    private String custNo;
    private Column colCustNo;

    public CustomerFormDlg() {
        super("Список клиентов");
    }

    @Override
    protected GridForm getGridForm() {
        return new CustomerGridForm();
    }

    class CustomerGridForm extends GridForm {

        public CustomerGridForm() {
            super("Выбор клиента");
        }

        @Override
        protected String prepareSql() {
            return "select CUSTNO, CUSTNAME, NAME, CTYPE, CTYPENAME, BRANCH, FILIAL " +
                    "from V_GL_CUSTOMER ";      // не проверяем филиал!
        }

        @Override
        protected Table prepareTable() {
            Table result = new Table();
            Column col;

            result.addColumn(colCustNo = new Column("CUSTNO", Column.Type.STRING, "Номер клиента", 30));
            result.addColumn(new Column("CUSTNAME", Column.Type.STRING, "Имя клиента", 120));
            result.addColumn(new Column("NAME", Column.Type.STRING, "Имя клиента (на английском)", 80));
            result.addColumn(new Column("FILIAL", Column.Type.STRING, "Филиал", 30));
            result.addColumn(new Column("CTYPE", Column.Type.STRING, "Тип собственности", 20, true, false));
            result.addColumn(new Column("CTYPENAME", Column.Type.STRING, "Имя собственности", 20, false, false));
            return result;
        }

        @Override
        public ArrayList<SortItem> getInitialSortCriteria() {
            ArrayList<SortItem> list = new ArrayList<SortItem>();
            list.add(new SortItem("CUSTNO", Column.Sort.ASC));
            return list;
        }

        @Override
        public ArrayList<FilterItem> getInitialFilterCriteria(Object[] initialFilterParams) {
            custNo = (String)initialFilterParams[0];

            ArrayList<FilterItem> list = new ArrayList<FilterItem>();
            if (!isEmpty(custNo)) list.add(new FilterItem(colCustNo,
                    custNo.length() == 8 ? FilterCriteria.EQ : FilterCriteria.START_WITH, custNo));
            return list;
        }

        @Override
        protected Object[] getInitialFilterParams() {
            return CustomerFormDlg.this.getInitialFilterParams();
        }
    }
}
