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


/**
 * Created by ER18837 on 05.02.16.
 */
public abstract class AccTechFormDlg extends GridFormDlgBase {

    private Column colBsaAcid;
    private Column colAccType;
    private Column colAcc2;
    private Column colAcid;
    private Column colDealSrc;
    private Column colDealId;
    private Column colCustNo;
    private Column colCustName;
    private Column colFilial;
    private Column colCurrency;
    private Column colDateOpen;
    private Column colDateClose;
    private Column colRlnType;

    protected AccCustomerQuickFilterAction quickFilterAction;
    protected AccountQuickFilterParams quickFilterParams;

    public AccTechFormDlg(boolean preview) {
        super("Выбор счета (технические счета)");
        setInitialFilter();
        ok.setVisible(!preview);
    }

    @Override
    protected GridForm getGridForm() {
        return new AccCustomerGridForm();
    }

    @Override
    protected String getGridWidth() {
        return "1000px";
    }

    @Override
    protected String getGridHeight() {
        return "600px";
    }

    protected abstract Object[] getInitialFilterParams();

    protected void setInitialFilter() {
        if (null == quickFilterAction)
            return;
        AccountQuickFilterParams filterParams = (AccountQuickFilterParams)quickFilterAction.getFilterParams();
        filterParams.setInitialFilterParams(getInitialFilterParams());
    }

    class AccCustomerGridForm extends GridForm {

        public AccCustomerGridForm() {
            super("Выбор счета", true);
            quickFilterParams = createQuickFilterParams();
            abw.addAction(quickFilterAction = new AccCustomerQuickFilterAction(grid, quickFilterParams) );
            abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        }

        @Override
        protected String prepareSql() {
            return "select * from V_GL_ACC_TH";
        }

        @Override
        protected Table prepareTable() {
            Table result = new Table();
            Column col;

            result.addColumn(colAccType = new Column("ACCTYPE", Column.Type.DECIMAL, "Account Type", 80,true, false, Column.Sort.ASC, "000000000"));
            result.addColumn(colBsaAcid = new Column("BSAACID", Column.Type.STRING, "Счет ЦБ", 80));
            result.addColumn(colAcid = new Column("ACID", Column.Type.STRING, "Счет Midas", 80));
            result.addColumn(colAcc2 = new Column("ACC2", Column.Type.STRING, "Балансовый счет", 60, false, false));
            result.addColumn(colCustNo = new Column("CUSTNO", Column.Type.STRING, "Номер клиента", 40));
            result.addColumn(colCustName = new Column("CUSTNAME", Column.Type.STRING, "Имя клиента", 180));
            result.addColumn(colFilial = new Column("CBCC", Column.Type.STRING, "Филиал", 30, false, false));
            result.addColumn(colCurrency = new Column("CCY", Column.Type.STRING, "Валюта", 20));
            result.addColumn(colDateOpen = new Column("DTO", Column.Type.DATE, "Дата открытия", 40));
            result.addColumn(colDateClose = new Column("DTC", Column.Type.DATE, "Дата закрытия", 40, false, false));
            result.addColumn(colDealSrc = new Column("DEALSRS", Column.Type.STRING, "Источник сделки", 40, false, false));
            result.addColumn(colDealId = new Column("DEALID", Column.Type.STRING, "Номер сделки", 60));
            result.addColumn(colRlnType = new Column("RLNTYPE", Column.Type.STRING, "Тип счета", 30, false, false));
            result.addColumn(colCustName = new Column("DESCRIPTION", Column.Type.STRING, "Описание", 180));

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
            AccountQuickFilterParams params = (AccountQuickFilterParams)quickFilterAction.getFilterParams();
            params.setInitialFilterParams(initialFilterParams);
            String ccyN = params.getCurrencyN();
            String filialN = params.getFilialN();
            String bsaAcid = params.getAccount();
            Date currentDate = params.getDateFrom();

            ArrayList<FilterItem> list = new ArrayList<FilterItem>();
            list.add(new FilterItem(colDateOpen, FilterCriteria.LE, currentDate, true));
            list.add(new FilterItem(colDateClose, FilterCriteria.GE, currentDate, true));

            return list;
        }

        @Override
        protected Object[] getInitialFilterParams() {
            return AccTechFormDlg.this.getInitialFilterParams();
        }
    }

    private AccountQuickFilterParams createQuickFilterParams() {
        return new AccountQuickFilterParams(colFilial, colCurrency, colAccType, colAcc2, colCustNo, colDealSrc, colDealId, colDateOpen, colDateClose) {
            @Override
            protected boolean isNumberCodeFilial() {
                return true;
            }

            @Override
            protected boolean isNumberCodeCurrency() {
                return true;
            }
        };
    }

    class AccCustomerQuickFilterAction extends AccountBaseQuickFilterAction {
        public AccCustomerQuickFilterAction(GridWidget grid, AccountQuickFilterParams params) {
            super(grid, params);
        }

        @Override
        public Object[] getInitialFilterParams(Date operday, Date prevday) {
            return AccTechFormDlg.this.getInitialFilterParams();
        }

    }

}

