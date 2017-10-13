package ru.rbt.barsgl.gwt.client.dictionary;

import ru.rbt.grid.gwt.client.gridForm.GridForm;
import ru.rbt.barsgl.gwt.client.gridForm.GridFormDlgBase;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.shared.filter.FilterCriteria;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;

import java.util.ArrayList;
import java.util.Date;

import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;

/**
 * Created by ER18837 on 01.02.16.
 */
public abstract class AccountTypeFormDlg extends GridFormDlgBase {

    protected Column colDateBegin;
    protected Column colDateEnd;
    protected Column colCustNo;
    protected Column colAccType;
    protected Column colCtype;
    protected Column colTerm;
    protected Column colCtrl;

    public AccountTypeFormDlg() {
        super("Выбор Accounting Type");
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
        return new AccountTypeGridForm();
    }

    class AccountTypeGridForm extends GridForm {

        public AccountTypeGridForm() {
            super("План счетов по Accounting Type");
            reconfigure();
        }

        private void reconfigure() {
            abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        }

        @Override
        protected String prepareSql() {
            return "select ACCTYPE, CUSTYPE, TERM, ACC2, ACOD, SQ, DTB, DTE, ACCNAME, TERMNAME, CTYPENAME, CUSTNO, CUSTNAME, CTYPE, FL_CTRL" +
                    " from V_GL_ACCUST where not ACC2 like '706%'" //ACOD not in ('7920','7919','7903','7904','7907','7908')
                    ;
        }

        @Override
        protected Table prepareTable() {
            Table result = new Table();

            result.addColumn(colAccType = new Column("ACCTYPE", Column.Type.STRING, "Accounting Typе", 30));
            result.addColumn(colCtype = new Column("CUSTYPE", Column.Type.STRING, "Тип собств", 20, true, false));
            result.addColumn(new Column("CTYPENAME", Column.Type.STRING, "Имя собств", 20, false, false));
            result.addColumn(colTerm = new Column("TERM", Column.Type.STRING, "Код срока", 20));
            result.addColumn(new Column("TERMNAME", Column.Type.STRING, "Наименование кода срока", 40));
            result.addColumn(new Column("ACC2", Column.Type.STRING, "Б/счет 2-го порядка", 20, false, false));
            result.addColumn(new Column("ACOD", Column.Type.STRING, "ACOD Midas", 20, false, false));
            result.addColumn(new Column("SQ", Column.Type.STRING, "SQ Midas", 20, false, false));
            result.addColumn(new Column("ACCNAME", Column.Type.STRING, "Наименование Accounting Typе", 80));
            result.addColumn(colCustNo = new Column("CUSTNO", Column.Type.STRING, "Номер клиента", 40, false, false));
            result.addColumn(new Column("CUSTNAME", Column.Type.STRING, "Имя клиента", 40, false, false));
            result.addColumn(new Column("CTYPE", Column.Type.STRING, "Тип собств клиента", 20, false, false));
            result.addColumn(colCtrl =new Column("FL_CTRL", Column.Type.STRING, "Контролируемый", 20, false, false));
            result.addColumn(colDateBegin = new Column("DTB", Column.Type.DATE, "Дата начала", 25));
            result.addColumn(colDateEnd = new Column("DTE", Column.Type.DATE, "Дата конца", 25));
            return result;
        }

        @Override
        public ArrayList<SortItem> getInitialSortCriteria() {
            ArrayList<SortItem> list = new ArrayList<SortItem>();
            list.add(new SortItem("ACCTYPE", Column.Sort.ASC));
            list.add(new SortItem("CUSTYPE", Column.Sort.ASC));
            list.add(new SortItem("TERM", Column.Sort.ASC));
            list.add(new SortItem("DTB", Column.Sort.ASC));
            return list;
        }

        @Override
        public ArrayList<FilterItem> getInitialFilterCriteria(Object[] initialFilterParams) {
            Date currentDate = (Date)initialFilterParams[0];
            String accType = (String)initialFilterParams[1];
            String term = (String)initialFilterParams[2];
            String custNo = (String)initialFilterParams[3];

            ArrayList<FilterItem> list = new ArrayList<FilterItem>();
            list.add(new FilterItem(colDateBegin, FilterCriteria.LE, currentDate, true, true));
            list.add(new FilterItem(colDateEnd, FilterCriteria.IS_NULL, null, true, true));
            list.add(new FilterItem(colCtrl, FilterCriteria.NE, "Y", true, true));
            list.add(new FilterItem(colCustNo, FilterCriteria.EQ, custNo, true, true));

            if (!isEmpty(accType)) list.add(new FilterItem(colAccType, FilterCriteria.START_WITH, accType));
            if (!isEmpty(term) && !term.equals("00")) list.add(new FilterItem(colTerm, FilterCriteria.EQ, term));
            return list;
        }

        @Override
        protected Object[] getInitialFilterParams() {
            return AccountTypeFormDlg.this.getInitialFilterParams();
        }
    }
}
