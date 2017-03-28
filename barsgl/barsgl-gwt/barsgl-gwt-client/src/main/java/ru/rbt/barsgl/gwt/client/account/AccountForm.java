package ru.rbt.barsgl.gwt.client.account;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.dict.EditableDictionary;
import ru.rbt.barsgl.gwt.client.operation.NewOperationAction;
import ru.rbt.barsgl.gwt.client.operation.OperationDlg;
import ru.rbt.barsgl.gwt.client.quickFilter.AccountBaseQuickFilterAction;
import ru.rbt.barsgl.gwt.client.quickFilter.AccountQuickFilterParams;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.SecurityActionCode;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;

import java.util.ArrayList;
import java.util.Date;

import static ru.rbt.barsgl.gwt.client.operation.OperationDlgBase.Side.CREDIT;
import static ru.rbt.barsgl.gwt.client.operation.OperationDlgBase.Side.DEBIT;
import static ru.rbt.barsgl.gwt.client.security.AuthWherePart.getSourceAndFilialPart;
import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.*;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;
import static ru.rbt.barsgl.shared.dict.FormAction.*;

/**
 * Created by ER18837 on 14.03.16.
 */
public class AccountForm extends EditableDictionary<ManualAccountWrapper> {
    public static final String FORM_NAME = "Лицевые счета";
    PopupPanel sidePanel;

    private Column colFilial;
    private Column colCurrency;
    private Column colAccount;
    private Column colAcc2;
    private Column colCustomer;
    private Column colDealSource;
    private Column colDealId;
    private Column colDateOpen;
    private Column colDateClose;

    AccountQuickFilterParams quickFilterParams;
    GridAction quickFilterAction;
    
    public AccountForm() {
        super(FORM_NAME, true);
        reconfigure();
    }

    private void reconfigure() {

    	quickFilterParams = createQuickFilterParams();
        abw.addAction(quickFilterAction = new AccountQuickFilterAction(grid, quickFilterParams) );
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        abw.addSecureAction(editAccount(), SecurityActionCode.AccChng);
        abw.addSecureAction(createAccount(), SecurityActionCode.AccInp);
        abw.addSecureAction(closeAccount(), SecurityActionCode.AccClose);
        abw.addSecureAction(createNewOperation(), SecurityActionCode.AccOperInp);
        quickFilterAction.execute();
    }

    @Override
    protected String prepareSql() {
        return "select ID, BSAACID, CBCC, CBCCN, BRANCH, CCY, CUSTNO, ACCTYPE, CBCUSTTYPE, TERM, GL_SEQ, " +
                "ACC2, PLCODE, ACOD, SQ, ACID, PSAV, DEALSRS, DEALID, SUBDEALID, DESCRIPTION, " +
                "DTO, DTC, DTR, DTM, OPENTYPE, GLOID, GLO_DC, BALANCE " +
                "from V_GL_ACC"
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

        result.addColumn(colAccount = new Column("BSAACID", STRING, "Счет ЦБ", 160));
        result.addColumn(colCurrency = new Column("CCY", STRING, "Валю та", 40));
        result.addColumn(new Column("BALANCE", DECIMAL, "Остаток", 120));
        result.addColumn(new Column("PSAV", STRING, "А/ П", 25));
        result.addColumn(new Column("ACID", STRING, "Счет Midas", 160));
        result.addColumn(colDealSource = new Column("DEALSRS", STRING, "Источник сделки", 70));
        result.addColumn(colDealId = new Column("DEALID", STRING, "ИД сделки", 120));
        result.addColumn(new Column("SUBDEALID", STRING, "ИД субсделки", 160));
        result.addColumn(new Column("CBCCN", STRING, "Код филиала", 60, false, false));
        result.addColumn(colFilial = new Column("CBCC", STRING, "Филиал", 40));
        result.addColumn(new Column("BRANCH", STRING, "Отделение", 40));
        result.addColumn(colCustomer = new Column("CUSTNO", STRING, "Номер клиента", 70));

        result.addColumn(new Column("ACCTYPE", DECIMAL, "Accounting Type", 80, true, false, Column.Sort.NONE, "#"));// No Space
        result.addColumn(new Column("CBCUSTTYPE", DECIMAL, "Тип соб", 40, true, true, Column.Sort.NONE, "#,##"));
        result.addColumn(new Column("TERM", DECIMAL, "Код срока", 40, true, true, Column.Sort.NONE, "#,##"));
        result.addColumn(colAcc2 = new Column("ACC2", STRING, "Счет 2-го порядка", 60, false, false));
        result.addColumn(new Column("PLCODE", STRING, "Символ ОФР", 30, false, false));

        result.addColumn(new Column("ACOD", DECIMAL, "Midas Acod", 50, false, false));
        result.addColumn(new Column("SQ", DECIMAL, "Midas SQ", 40, false, false));
        result.addColumn(new Column("DESCRIPTION", STRING, "Название счета", 380));
        result.addColumn(colDateOpen = new Column("DTO", DATE, "Дата открытия", 80));
        result.addColumn(colDateClose = new Column("DTC", DATE, "Дата закрытия", 80));
        result.addColumn(new Column("DTR", DATE, "Дата регистрации", 80, false, false));
        result.addColumn(new Column("DTM", DATE, "Дата изменения", 80, false, false));
        result.addColumn(new Column("OPENTYPE", STRING, "Способ открытия", 70, false, false));

        result.addColumn(new Column("ID", LONG, "ИД счета", 60, true, true, Column.Sort.DESC, ""));

        return result;
    }

    private GridAction createAccount() {
        return (GridAction) commonLazyAction("AccountDlg", "Открытие счета GL", CREATE, table.getColumns(),
                "Счет не создан",
                "Ошибка создания счета",
                "Счет создан успешно");
    }
    private GridAction editAccount() {
        return (GridAction) commonLazyAction("AccountDlg", "Редактирование счета GL", UPDATE, table.getColumns(),
                "Счет не изменен",
                "Ошибка изменения счета",
                "Счет изменен успешно");
    }

    private GridAction closeAccount() {
        return (GridAction) otherAction(new AccountCloseDlg("Закрытие счета GL", OTHER, grid.getTable().getColumns()),
                "Счет не изменен",
                "Ошибка изменения счета",
                "Счет изменен успешно",
                ImageConstants.INSTANCE.close24());
    }

    @Override
    protected void save(ManualAccountWrapper cnw, FormAction action, AsyncCallback<RpcRes_Base<ManualAccountWrapper>> asyncCallbackImpl) throws Exception {
        switch(action) {
            case CREATE:
                BarsGLEntryPoint.operationService.saveAccount(cnw, asyncCallbackImpl);
                break;
            case UPDATE:
                BarsGLEntryPoint.operationService.updateAccount(cnw, asyncCallbackImpl);
                break;
            case OTHER:
                BarsGLEntryPoint.operationService.closeAccount(cnw, asyncCallbackImpl);
                break;
        }
    }

    @Override
    protected String getSuccessMessage(ManualAccountWrapper wrapper, FormAction action) {
        switch(action) {
            case CREATE:
                return "<pre>Счет ЦБ:      " + wrapper.getBsaAcid() + "</pre>"
                    + "<pre>Счет Midas:   " + wrapper.getAcid() + "</pre>"
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

    class NewOperationAccAction extends NewOperationAction {

        private boolean isDebit;
        public NewOperationAccAction(OperationDlg.Side side) {
            super(AccountForm.this.grid, ImageConstants.INSTANCE.oper_go());
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
            String dealId = (String)row.getField(columns.getColumnIndexByName("DEALID")).getValue();
            String subDealId = (String)row.getField(columns.getColumnIndexByName("SUBDEALID")).getValue();
            String dealSounce = (String)row.getField(columns.getColumnIndexByName("DEALSRS")).getValue();

            ManualOperationWrapper wrapper = new ManualOperationWrapper();
            wrapper.setCurrencyDebit(ccy);
            wrapper.setCurrencyCredit(ccy);
            wrapper.setFilialDebit(filial);
            wrapper.setFilialCredit(filial);
            if (isDebit) {
                wrapper.setAccountDebit(bsaAcid);
            } else {
                wrapper.setAccountCredit(bsaAcid);
            }
            wrapper.setDealSrc(dealSounce);
            wrapper.setDealId(dealId);
            wrapper.setSubdealId(subDealId);
            return wrapper;
        }
    }

    private AccountQuickFilterParams createQuickFilterParams() {
        return new AccountQuickFilterParams(colFilial, colCurrency, colAccount, colAcc2, colCustomer, colDealSource, colDealId, colDateOpen, colDateClose) {
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

    class AccountQuickFilterAction extends AccountBaseQuickFilterAction {
        public AccountQuickFilterAction(GridWidget grid, AccountQuickFilterParams params) {
            super(grid, params);
        }

        @Override
        public Object[] getInitialFilterParams(Date operday, Date prevday) {
            return null;
        }

    }
}
