package ru.rbt.barsgl.gwt.client.account;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.dict.EditableDictionary;
import ru.rbt.barsgl.gwt.client.operation.NewTechOperationAction;
import ru.rbt.barsgl.gwt.client.operation.OperationDlg;
import ru.rbt.barsgl.gwt.client.quickFilter.AccountTechBaseQuickFilterAction;
import ru.rbt.barsgl.gwt.client.quickFilter.AccountTechQuickFilterParams;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.utils.DialogUtils;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.operation.ManualTechOperationWrapper;
import ru.rbt.shared.enums.SecurityActionCode;

import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

import static ru.rbt.barsgl.gwt.client.operation.OperationDlgBase.Side.CREDIT;
import static ru.rbt.barsgl.gwt.client.operation.OperationDlgBase.Side.DEBIT;
import static ru.rbt.barsgl.gwt.client.security.AuthWherePart.getSourceAndFilialPart;
import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.*;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;
import static ru.rbt.barsgl.shared.dict.FormAction.*;

/**
 * Created by ER18837 on 14.03.16.
 */
public class AccountFormTech extends EditableDictionary<ManualAccountWrapper> {
    public static final String FORM_NAME = "Лицевые счета (учёт по техническим счетам)";
    PopupPanel sidePanel;

    private static Logger log  = Logger.getLogger("AccountFormTech");

    private Column colFilial;
    private Column colCurrency;
    private Column colCustomer;
    private Column colAccType;
    private Column colDTC;

    AccountTechQuickFilterParams quickFilterParams;
    GridAction quickFilterAction;

    public AccountFormTech() {
        super(FORM_NAME, true);
        reconfigure();
    }

    private void reconfigure() {

    	quickFilterParams = createQuickFilterParams();
        abw.addAction(quickFilterAction = new AccountTechQuickFilterAction(grid, quickFilterParams) );
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        abw.addSecureAction(editAccount(), SecurityActionCode.TechAccChng);
        abw.addSecureAction(createAccount(), SecurityActionCode.TechAccInp);
        abw.addSecureAction(closeAccount(), SecurityActionCode.TechAccClose);
        abw.addSecureAction(createNewOperation(), SecurityActionCode.TechAccOperInp);
        quickFilterAction.execute();
    }

    @Override
    protected String prepareSql() {
        return "select ID, BSAACID, CBCC, CBCCN, BRANCH, CCY, CUSTNO, ACCTYPE, CBCUSTTYPE, TERM, GL_SEQ, " +
                "ACC2, PLCODE, ACOD, SQ, ACID, PSAV, DEALSRS, DEALID, SUBDEALID, DESCRIPTION, " +
                "DTO, DTC, DTR, DTM, OPENTYPE, GLOID, GLO_DC, BALANCE " +
                "from V_GL_ACC_TH"
                + getSourceAndFilialPart("where", "", "CBCC");
    }

    @Override
    public ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("ID", Column.Sort.DESC));
        return list;
    }

    protected Table prepareTable() {
        Table result = new Table();
        result.addColumn(colAccType = new Column("ACCTYPE", Column.Type.STRING, "Accounting Type", 60, true, false, Column.Sort.ASC, ""));// No Space
        colAccType.setFilterable(true);
        result.addColumn(new Column("BSAACID", STRING, "Псевдосчёт", 160));
        result.addColumn(colCurrency = new Column("CCY", STRING, "Валюта", 60));
        result.addColumn(new Column("BALANCE", DECIMAL, "Остаток", 120));
        result.addColumn(new Column("DEALSRS", STRING, "Источник сделки", 60));
        Column colCBCCN;
        result.addColumn(colCBCCN = new Column("CBCCN", STRING, "Код филиала (цифровой)", 60));
        colCBCCN.setVisible(true);
        result.addColumn(colFilial = new Column("CBCC", STRING, "Филиал", 60));
        Column colAcod;
        result.addColumn(colAcod = new Column("ACOD", STRING, "ACOD", 380));
        colAcod.setVisible(false);
        Column colSQ;
        result.addColumn(colSQ = new Column("SQ", STRING, "SQ", 380));
        colSQ.setVisible(false);
        result.addColumn(new Column("DESCRIPTION", STRING, "Название счета", 380));
        result.addColumn(colCustomer = new Column("CUSTNO", STRING, "Номер клиента", 70));
        colCustomer.setVisible(false);
        result.addColumn(new Column("DTO", DATE, "Дата открытия", 80));
        result.addColumn(colDTC = new Column("DTC", DATE, "Дата закрытия", 80));
        Column colID;
        result.addColumn(colID = new Column("ID", LONG, "ID счета", 60, true, true, Column.Sort.NONE, ""));
        colID.setVisible(false);
        Column colDTR;
        result.addColumn(colDTR = new Column("DTR", DATE, "Дата регистрации", 80));
        colDTR.setVisible(false);
        Column colDTM;
        result.addColumn(colDTM = new Column("DTM", DATE, "Дата изменения", 80));
        colDTM.setVisible(false);
        Column colOpenType;
        result.addColumn(colOpenType = new Column("OPENTYPE", STRING, "Способ открытия", 380));
        colOpenType.setVisible(false);
        return result;
    }

    private GridAction createAccount() {
        return (GridAction) commonLazyAction("AccountTechDlg", "Открытие технического счета GL", CREATE, table.getColumns(),
                "Счет не создан",
                "Ошибка создания счета",
                "Счет создан успешно");
    }
    private GridAction editAccount() {
        return (GridAction) commonLazyAction("AccountTechDlg", "Редактирование технического счета GL", UPDATE, grid.getTable().getColumns(),
                "Счет не изменен",
                "Ошибка изменения счета",
                "Счет изменен успешно");
    }

    private GridAction closeAccount() {
        return (GridAction) otherAction(new AccountCloseDlg("Закрытие технического счета GL", OTHER, grid.getTable().getColumns()),
                "Счет не изменен",
                "Ошибка изменения счета",
                "Счет изменен успешно",
                ImageConstants.INSTANCE.close24());
    }

    @Override
    protected void save(ManualAccountWrapper cnw, FormAction action, AsyncCallback<RpcRes_Base<ManualAccountWrapper>> asyncCallbackImpl) throws Exception {
        switch(action) {
            case CREATE:
                BarsGLEntryPoint.accountService.saveTechAccount(cnw, asyncCallbackImpl);
                break;
            case UPDATE:
                BarsGLEntryPoint.accountService.updateTechAccount(cnw, asyncCallbackImpl);
                break;
            case OTHER:
                BarsGLEntryPoint.accountService.closeTechAccount(cnw, asyncCallbackImpl);
                break;
        }
    }

    @Override
    protected String getSuccessMessage(ManualAccountWrapper wrapper, FormAction action) {
        switch(action) {
            case CREATE:
                return "<pre>Счет ЦБ:      " + wrapper.getBsaAcid() + "</pre>"
                    + (isEmpty(wrapper.getDealId()) ? "" :
                    "<pre>Номер сделки: " + wrapper.getDealId() + "</pre>");
            case OTHER:
                String dateClose = wrapper.getDateCloseStr();
                String account = wrapper.getBsaAcid();
                return (dateClose == null ?
                        "Отменено закрытие счета ЦБ '" + account + "'"
                        : "Счет ЦБ '" + account + "' закрыт");
            default:
                return null;
        }
    }

    private GridAction createNewOperation() {

        sidePanel = new PopupPanel(true, true);
        VerticalPanel vp = new VerticalPanel();
        vp.add(new HTML("<b>Ввести операцию</b>"));
        MenuItem itemDebit = new MenuItem("Поставить счет в дебет", new Command() {

            @Override
            public void execute() {
                sidePanel.hide();
                new NewOperationAccAction(DEBIT).execute();
            }
        });
        MenuItem itemCredit = new MenuItem("Поставить счет в кредит", new Command() {

            @Override
            public void execute() {
                sidePanel.hide();
                new NewOperationAccAction(CREDIT).execute();
            }
        });
        MenuBar bar = new MenuBar(true);
        bar.addItem(itemDebit);
        bar.addItem(itemCredit);
        vp.add(bar);
        sidePanel.setWidget(vp);

        return new GridAction(grid, null, "Ввести операцию", new Image(ImageConstants.INSTANCE.oper_go()), 10) {
            @Override
            public void execute() {

                Row row = grid.getCurrentRow();
                if (row!=null)
                {
                    if (!isEmpty(grid.getFieldText(colDTC.getName())))
                    {
                        DialogUtils.showInfo("Нельзя создать операцию по закрытому счёту!");
                        return;
                    }
                }

                final PushButton button = abw.getButton(this);
                sidePanel.setPopupPositionAndShow(new PopupPanel.PositionCallback() {

                    @Override
                    public void setPosition(int i, int i1) {
                        sidePanel.setPopupPosition(button.getAbsoluteLeft(), button.getAbsoluteTop() + button.getOffsetHeight());
                    }
                });
            }
        };
    }

    class NewOperationAccAction extends NewTechOperationAction {

        private boolean isDebit;
        public NewOperationAccAction(OperationDlg.Side side) {
            super(AccountFormTech.this.grid, ImageConstants.INSTANCE.oper_go());
            isDebit = side.equals(DEBIT);
        }

        @Override
        protected Object getParams() {
            Row row = grid.getCurrentRow();
            if (row == null) return null;

            Columns columns = grid.getTable().getColumns();
            String bsaAcid = (String)row.getField(columns.getColumnIndexByName("BSAACID")).getValue();
            String ccy = (String)row.getField(columns.getColumnIndexByName("CCY")).getValue();
            String filial = (String)row.getField(columns.getColumnIndexByName("CBCC")).getValue();
            //String dealId = (String)row.getField(columns.getColumnIndexByName("DEALID")).getValue();
            //String subDealId = (String)row.getField(columns.getColumnIndexByName("SUBDEALID")).getValue();
            String dealSounce = (String)row.getField(columns.getColumnIndexByName("DEALSRS")).getValue();
            String accType = String.valueOf(row.getField(columns.getColumnIndexByName("ACCTYPE")).getValue());

            ManualTechOperationWrapper wrapper = new ManualTechOperationWrapper();
            wrapper.setCurrencyDebit(ccy);
            wrapper.setCurrencyCredit(ccy);
            wrapper.setFilialDebit(filial);
            wrapper.setFilialCredit(filial);
            if (isDebit) {
                wrapper.setAccountDebit(bsaAcid);
                wrapper.setAccountTypeDebit(accType);
                wrapper.setCurrencyDebit(ccy);
                if (!"RUR".equalsIgnoreCase(ccy))
                {
                    wrapper.setCurrencyCredit("RUR");
                }
            } else {
                wrapper.setAccountCredit(bsaAcid);
                wrapper.setAccountTypeCredit(accType);
                wrapper.setCurrencyCredit(ccy);
                if (!"RUR".equalsIgnoreCase(ccy))
                {
                    wrapper.setCurrencyDebit("RUR");
                }
            }
            wrapper.setDealSrc(dealSounce);
            //wrapper.setDealId(dealId);
            //wrapper.setSubdealId(subDealId);
            return wrapper;
        }
    }

    private AccountTechQuickFilterParams createQuickFilterParams() {
        return new AccountTechQuickFilterParams(colFilial, colCurrency, colAccType) {
            @Override
            protected boolean isNumberCodeFilial() {
                return false;
            }

            @Override
            protected boolean isNumberCodeCurrency() {
                return false;
            }
        };
    }

    class AccountTechQuickFilterAction extends AccountTechBaseQuickFilterAction {
        public AccountTechQuickFilterAction(GridWidget grid, AccountTechQuickFilterParams params) {
            super(grid, params);
        }

        @Override
        public Object[] getInitialFilterParams(Date operday, Date prevday) {
            return null;
        }
    }
}
