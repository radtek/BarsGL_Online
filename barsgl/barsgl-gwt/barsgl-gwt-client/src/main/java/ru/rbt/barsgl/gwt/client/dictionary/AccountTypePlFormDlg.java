package ru.rbt.barsgl.gwt.client.dictionary;

import com.google.gwt.user.client.Window;
import ru.rbt.barsgl.gwt.client.gridForm.GridFormDlgBase;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.shared.filter.FilterCriteria;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.grid.gwt.client.gridForm.GridForm;

import java.util.ArrayList;
import java.util.Date;

import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;

/**
 * Created by ER18837 on 30.11.16.
 */
public abstract class AccountTypePlFormDlg extends GridFormDlgBase {

    protected Column colDateBegin;
    protected Column colDateEnd;
    protected Column colCustNo;
    protected Column colAccType;
    protected Column colCtype;
    protected Column colTerm;
    protected Column colAcc2;
    protected Column colPlcode;
    protected Column colAcod;
    protected Column colSQ;
    protected Column colCtrl;

    public AccountTypePlFormDlg() {
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
            return  "select * from ( " +
                    "select ACCTYPE, TRIM(CUSTYPE) CUSTYPE, TERM, ACC2, trim(PLCODE) PLCODE, ACOD, SQ, DTB, DTE, ACCNAME, TERMNAME, CTYPENAME, FL_CTRL " +
                    "from V_GL_ACTPL where trim(PLCODE) is not null and ACOD not in ('7920','7919','7903','7904','7907','7908')) v";

        }

        @Override
        protected Table prepareTable() {
            Table result = new Table();

            result.addColumn(colAccType = new Column("ACCTYPE", Column.Type.STRING, "Accounting Typе", 30));
            result.addColumn(colCtype = new Column("CUSTYPE", Column.Type.STRING, "Тип собств", 20, true, false));
            result.addColumn(new Column("CTYPENAME", Column.Type.STRING, "Имя собств", 20, false, false));
            result.addColumn(colTerm = new Column("TERM", Column.Type.STRING, "Код срока", 20));
            result.addColumn(new Column("TERMNAME", Column.Type.STRING, "Наименование кода срока", 40));
            result.addColumn(colAcc2 = new Column("ACC2", Column.Type.STRING, "Б/счет 2-го порядка", 20));
            result.addColumn(colPlcode = new Column("PLCODE", Column.Type.STRING, "Символ доходов/расходов", 20));
            result.addColumn(colAcod = new Column("ACOD", Column.Type.STRING, "ACOD Midas", 20, false, false));
            result.addColumn(colSQ = new Column("SQ", Column.Type.STRING, "SQ Midas", 20, false, false));
            result.addColumn(new Column("ACCNAME", Column.Type.STRING, "Наименование Accounting Typе", 80));
            result.addColumn(colCtrl =new Column("FL_CTRL", Column.Type.STRING, "Контролируемый", 20, false, false));
            result.addColumn(colDateBegin = new Column("DTB", Column.Type.DATE, "Дата начала", 25, false, false));
            result.addColumn(colDateEnd = new Column("DTE", Column.Type.DATE, "Дата конца", 25, false, false));
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
            String ctype = (String)initialFilterParams[3];
            String acc2 = (String)initialFilterParams[4];
            String plcode = (String)initialFilterParams[5];

            ArrayList<FilterItem> list = new ArrayList<FilterItem>();
            list.add(new FilterItem(colDateBegin, FilterCriteria.LE, currentDate, true));
            list.add(new FilterItem(colDateEnd, FilterCriteria.GE, currentDate, true));
            list.add(new FilterItem(colCtrl, FilterCriteria.NE, "Y", true));
            if (!isEmpty(ctype)) list.add(new FilterItem(colCtype, FilterCriteria.EQ, ctype, true));
            if (!isEmpty(term)) list.add(new FilterItem(colTerm, FilterCriteria.EQ, term, true));
            if (!isEmpty(accType)) list.add(new FilterItem(colAccType, FilterCriteria.START_WITH, accType, true));
            if (!isEmpty(acc2)) list.add(new FilterItem(colAcc2, FilterCriteria.START_WITH, acc2, true));
            if (!isEmpty(plcode)) list.add(new FilterItem(colPlcode, FilterCriteria.START_WITH, plcode, true));

            return list;
        }

        @Override
        protected Object[] getInitialFilterParams() {
            return AccountTypePlFormDlg.this.getInitialFilterParams();
        }
    }
}
