package ru.rbt.barsgl.gwt.client.pd;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.account.AccountCloseDlg;
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
import ru.rbt.barsgl.shared.enums.InputMethod;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.getEnumLabelsList;
import static ru.rbt.barsgl.gwt.client.comp.GLComponents.getYesNoList;
import static ru.rbt.barsgl.gwt.client.operation.OperationDlgBase.Side.CREDIT;
import static ru.rbt.barsgl.gwt.client.operation.OperationDlgBase.Side.DEBIT;
import static ru.rbt.barsgl.gwt.client.security.AuthWherePart.getSourceAndFilialPart;
import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.*;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;
import static ru.rbt.barsgl.shared.dict.FormAction.*;

/**
 * Created by ER18837 on 14.03.16.
 */
public class PostingFormTech2 extends EditableDictionary<ManualAccountWrapper> {
    public static final String FORM_NAME = "Проводки (учёт по техническим счетам)";
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

    protected Column colProcDate;
    protected Column colPostDate;
    protected Column colValueDate;
    protected Column colGloid;

    private int podIndex, invisibleIndex, idDrIndex, idCrIndex, fanIndex, fanTypeIndex;

    AccountQuickFilterParams quickFilterParams;
    GridAction quickFilterAction;

    public PostingFormTech2() {
        super(FORM_NAME, true);
        reconfigure();
    }

    private void reconfigure() {

    	quickFilterParams = createQuickFilterParams();
        abw.addAction(quickFilterAction = new AccountQuickFilterAction(grid, quickFilterParams) );
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        //abw.addSecureAction(editAccount(), SecurityActionCode.AccChng);
        //abw.addSecureAction(createAccount(), SecurityActionCode.AccInp);
        //abw.addSecureAction(closeAccount(), SecurityActionCode.AccClose);
        //abw.addSecureAction(createNewOperation(), SecurityActionCode.AccOperInp);
        quickFilterAction.execute();
    }

    @Override
    protected String prepareSql() {
        return "SELECT * FROM V_GL_PDTH "
                + getSourceAndFilialPart("where", "SRC_PST", "FILIAL_DR");
    }

    @Override
    public ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("GLO_REF", Column.Sort.DESC));
        return list;
    }

    protected Table prepareTable() {
        /*Table result = new Table();

        result.addColumn(new Column("ACCTYPE", DECIMAL, "Accounting Type", 80, true, false, Column.Sort.ASC, "000000000"));// No Space
        result.addColumn(colCurrency = new Column("CCY", STRING, "Валюта", 60));
        result.addColumn(colFilial = new Column("CBCC", STRING, "Филиал", 60));
        result.addColumn(colAccount = new Column("BSAACID", STRING, "Псевдосчёт", 160));
        result.addColumn(new Column("BALANCE", DECIMAL, "Остаток", 120));
        result.addColumn(new Column("BRANCH", STRING, "Отделение", 60));
        result.addColumn(colCustomer = new Column("CUSTNO", STRING, "Номер клиента", 70));
        result.addColumn(new Column("DESCRIPTION", STRING, "Название счета", 380));
        result.addColumn(colDateOpen = new Column("DTO", DATE, "Дата открытия", 80));
        result.addColumn(colDateClose = new Column("DTC", DATE, "Дата закрытия", 80));
        result.addColumn(new Column("ID", LONG, "ИД счета", 60, true, true, Column.Sort.NONE, ""));*/

        Table result = new Table();
        Column col;

        HashMap<Serializable, String> yesNoList = getYesNoList();
        result.addColumn(new Column("GLO_REF", LONG, "ID операции", 70));
        result.addColumn(col = new Column("INP_METHOD", STRING, "Способ ввода", 50));
        col.setList(getEnumLabelsList(InputMethod.values()));
        result.addColumn(new Column("PCID", LONG, "ID проводки", 80));
        result.addColumn(new Column("SRC_PST", STRING, "Источник сделки", 60));
        result.addColumn(new Column("DEAL_ID", STRING, "ИД сделки", 120));
        result.addColumn(new Column("SUBDEALID", STRING, "ИД субсделки", 120));
        result.addColumn(new Column("PMT_REF", STRING, "ИД платежа", 120, false, false));
        result.addColumn(colProcDate = new Column("PROCDATE", DATE, "Дата опердня", 80));
        colProcDate.setFormat("dd.MM.yyyy");
        result.addColumn(colValueDate = new Column("VALD", DATE, "Дата валютирования", 80));
        colValueDate.setFormat("dd.MM.yyyy");
        podIndex = result.addColumn(colPostDate = new Column("POD", DATE, "Дата проводки", 80));
        colPostDate.setFormat("dd.MM.yyyy");
        result.addColumn(new Column("ACCTYPE_DR", STRING, "Тип счёта ДБ", 160));
        result.addColumn(new Column("BSAACID_DR", STRING, "Счет ДБ", 160));
        result.addColumn(new Column("CCY_DR", STRING, "Валюта ДБ", 60));
        result.addColumn(new Column("AMNT_DR", DECIMAL, "Сумма ДБ", 100));
        result.addColumn(new Column("AMNTBC_DR", DECIMAL, "Сумма в руб. ДБ", 100));
        result.addColumn(new Column("ACCTYPE_CR", STRING, "Тип счёта КР", 160));
        result.addColumn(new Column("BSAACID_CR", STRING, "Счет КР", 160));
        result.addColumn(new Column("CCY_CR", STRING, "Валюта КР", 60));
        result.addColumn(new Column("AMNT_CR", DECIMAL, "Сумма КР", 100));
        result.addColumn(new Column("AMNTBC_CR", DECIMAL, "Сумма в руб. КР", 100));
        result.addColumn(new Column("NRT", STRING, "Описание", 500, false, false));
        result.addColumn(new Column("RNARSHT", STRING, "Основание RUS", 200, false, false));
        result.addColumn(col = new Column("STRN", STRING, "Сторно", 40));
        col.setList(yesNoList);
        result.addColumn(col = new Column("FCHNG", STRING, "Исправительная", 40));
        col.setList(yesNoList);
        result.addColumn(new Column("DEPT_ID", STRING, "Подразделение", 60));


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
            super(PostingFormTech2.this.grid, ImageConstants.INSTANCE.oper_go());
            isDebit = side.equals(DEBIT);
        }

        @Override
        protected Object getParams() {
            Row row = grid.getCurrentRow();
            if (row == null) return null;

            Columns columns = grid.getTable().getColumns();
            String bsaAcid = (String)row.getField(columns.getColumnIndexByName("BSAACID_DR")).getValue();
            String ccy = (String)row.getField(columns.getColumnIndexByName("CCY_DR")).getValue();
            String filial = (String)row.getField(columns.getColumnIndexByName("FILIAL_DR")).getValue();
            String dealId = (String)row.getField(columns.getColumnIndexByName("DEAL_ID")).getValue();

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
