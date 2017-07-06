package ru.rbt.barsgl.gwt.client.dictionary;

import ru.rbt.grid.gwt.client.gridForm.GridForm;
import ru.rbt.barsgl.gwt.client.gridForm.GridFormDlgBase;
import ru.rbt.barsgl.gwt.client.quickFilter.AccountBaseQuickFilterAction;
import ru.rbt.barsgl.gwt.client.quickFilter.AccountQuickFilterParams;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.dialogs.FilterCriteria;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;

import java.util.ArrayList;
import java.util.Date;

import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;


/**
 * Created by ER18837 on 05.02.16.
 */
public abstract class AccTechFormDlg extends GridFormDlgBase {

    private Column colBsaAcid;
    private Column colAccType;
    private Column colFilial;
    private Column colCurrency;
    private Column colDateOpen;
    private Column colDateClose;

    public AccTechFormDlg() {
        super("Выбор счета (технические счета)");
    }

    @Override
    protected GridForm getGridForm() {
        return new AccTechGridForm(this.getEditMode());
    }

    @Override
    protected String getGridWidth() {
        return "1000px";
    }

    @Override
    protected String getGridHeight() {
        return "600px";
    }

    protected abstract boolean getEditMode();

    class AccTechGridForm extends GridForm {

        public AccTechGridForm(boolean editMode) {
            super("Выбор счета");
            reconfigure();
            ok.setVisible(editMode);
        }

        private void reconfigure() {
            abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        }

        @Override
        protected String prepareSql() {
            return "select right('00000000' || ACCTYPE, 9) as ACCTYPE, BSAACID, CCY, CBCC, DESCRIPTION as ACCNAME, ACOD, SQ as AC_SQ, DTO, coalesce(DTC, Date('2029-01-01')) as DTC from GL_ACC " +
                    "where RLNTYPE='9'";
        }

        @Override
        protected Table prepareTable() {
            Table result = new Table();
            result.addColumn(colAccType = new Column("ACCTYPE", Column.Type.STRING, "AccTypе", 30));
            result.addColumn(colBsaAcid = new Column("BSAACID", Column.Type.STRING, "Псевдосчёт ЦБ", 60));
            result.addColumn(colCurrency = new Column("CCY", Column.Type.STRING, "Валюта", 20));
            result.addColumn(colFilial = new Column("CBCC", Column.Type.STRING, "Филиал", 20));
            result.addColumn(new Column("ACCNAME", Column.Type.STRING, "Наименование счёта", 130));
            result.addColumn(new Column("ACOD", Column.Type.STRING, "ACOD Midas", 20, false, false));
            result.addColumn(new Column("AC_SQ", Column.Type.STRING, "SQ Midas", 20, false, false));
            result.addColumn(colDateOpen = new Column("DTO", Column.Type.DATE, "Дата открытия", 30));
            result.addColumn(colDateClose = new Column("DTC", Column.Type.DATE, "Дата закрытия", 30, false, false));

            return result;
        }

        @Override
        public ArrayList<SortItem> getInitialSortCriteria() {
            ArrayList<SortItem> list = new ArrayList<SortItem>();
            list.add(new SortItem("ACCTYPE", Column.Sort.ASC));
            return list;
        }

        @Override
        public ArrayList<FilterItem> getInitialFilterCriteria(Object[] initialFilterParams) {
            Date postDate = (Date)initialFilterParams[0];
            Date valueDate = (Date)initialFilterParams[1];
            String accType = (String)initialFilterParams[2];
            String filial = (String)initialFilterParams[3];
            String currency = (String)initialFilterParams[4];
            String bsaacid = (String)initialFilterParams[5];

            ArrayList<FilterItem> list = new ArrayList<FilterItem>();
            if (!isEmpty(accType)) list.add(new FilterItem(colAccType, FilterCriteria.START_WITH, accType));
            if (!isEmpty(filial)) list.add(new FilterItem(colFilial, FilterCriteria.EQ, filial));
            if (!isEmpty(currency))  list.add(new FilterItem(colCurrency, FilterCriteria.EQ, currency));
            if (!isEmpty(bsaacid)) list.add(new FilterItem(colBsaAcid, FilterCriteria.EQ, bsaacid));
            FilterItem item;
            if (valueDate != null) {
                list.add(item = new FilterItem(colDateOpen, FilterCriteria.LE, valueDate));
                item.setReadOnly(true);
            }
            if (postDate != null){
                list.add(item = new FilterItem(colDateClose, FilterCriteria.GE, postDate));
                item.setReadOnly(true);
            }

            return list;
        }

        @Override
        protected Object[] getInitialFilterParams() {
            return AccTechFormDlg.this.getInitialFilterParams();
        }
    }
}

