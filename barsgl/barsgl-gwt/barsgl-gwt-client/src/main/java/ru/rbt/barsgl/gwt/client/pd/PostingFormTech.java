package ru.rbt.barsgl.gwt.client.pd;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.account.AccountCloseDlg;
import ru.rbt.barsgl.gwt.client.dict.EditableDictionary;
import ru.rbt.barsgl.gwt.client.operation.NewOperationAction;
import ru.rbt.barsgl.gwt.client.operation.OperationDlg;
import ru.rbt.barsgl.gwt.client.quickFilter.AccountBaseQuickFilterAction;
import ru.rbt.barsgl.gwt.client.quickFilter.AccountQuickFilterParams;
import ru.rbt.barsgl.gwt.client.quickFilter.DateQuickFilterAction;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.InputMethod;
import ru.rbt.barsgl.shared.enums.PostingChoice;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.security.gwt.client.operday.IDataConsumer;
import ru.rbt.security.gwt.client.operday.OperDayGetter;
import ru.rbt.shared.enums.SecurityActionCode;
import ru.rbt.barsgl.shared.operation.ManualTechOperationWrapper;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.*;
import static ru.rbt.barsgl.gwt.client.operation.OperationDlgBase.Side.CREDIT;
import static ru.rbt.barsgl.gwt.client.operation.OperationDlgBase.Side.DEBIT;
import static ru.rbt.barsgl.gwt.client.quickFilter.DateQuickFilterParams.DateFilterField.CREATE_DATE;
import static ru.rbt.security.gwt.client.operday.OperDayGetter.getOperday;
import static ru.rbt.barsgl.gwt.client.security.AuthWherePart.getSourceAndFilialPart;
import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.*;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.addDays;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;
import static ru.rbt.barsgl.shared.dict.FormAction.*;
import static ru.rbt.barsgl.shared.enums.PostingChoice.PST_ALL;
import static ru.rbt.barsgl.shared.enums.PostingChoice.PST_SINGLE;

/**
 * Created by ER18837 on 14.03.16.
 */
public class PostingFormTech extends EditableDictionary<ManualTechOperationWrapper> {
    public static final String FORM_NAME = "Проводки (учёт по техническим счетам)";
    public static final int DAYS_EDIT = 30;

    PopupPanel sidePanel;
    private Column colFilialDr;
    private Column colFilialCr;
    private Column colCurrency;
    private Column colAccount;
    private Column colAcc2;
    private Column colCustomer;
    private Column colDealSource;
    private Column colDealId;
    private Column colDateOpen;
    private Column colDateClose;
    private Column colPrfcntr;
    private Column colAccType;
    private Column colInvisible;

    protected Column colProcDate;
    protected Column colPostDate;
    protected Column colValueDate;
    protected Column colGloid;


    private Date operday, editday;

    private int podIndex, invisibleIndex, idDrIndex, idCrIndex, fanIndex, fanTypeIndex;

    AccountQuickFilterParams quickFilterParams;
    GridAction quickFilterAction;

    public PostingFormTech() {
        super(FORM_NAME, true);
        reconfigure();
    }

    private void reconfigure() {

    	quickFilterParams = createQuickFilterParams();
        abw.addAction(quickFilterAction = new DateQuickFilterAction(grid, colProcDate, colValueDate, colPostDate, CREATE_DATE, false));
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        abw.addSecureAction(createPreview(), SecurityActionCode.TechOperLook);
        abw.addSecureAction(editPostingTech(), SecurityActionCode.TechOperPstChng);
        abw.addSecureAction(new PostingFormTech.DeleteAction(), SecurityActionCode.TechOperPstMakeInvisible);

        getOperday(new IDataConsumer<OperDayWrapper>() {
            @Override
            public void accept(OperDayWrapper wrapper) {
                operday = DateTimeFormat.getFormat(OperDayGetter.dateFormat).parse(wrapper.getCurrentOD());
                editday = addDays(operday, -DAYS_EDIT);
            }
        });

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
        Table result = new Table();
        Column col;

        HashMap<Serializable, String> yesNoList = getYesNoList();
        result.addColumn(new Column("GLO_REF", LONG, "ID операции", 70));
        result.addColumn(col = new Column("INP_METHOD", STRING, "Способ ввода", 50));
        col.setList(getEnumLabelsList(InputMethod.values()));

        idDrIndex = result.addColumn(new Column("PCID", LONG, "ID проводки", 80));
        //invisibleIndex = result.addColumn(col = new Column("INVISIBLE", STRING, "Отменена", 40));
        //col.setList(yesNoList);
        result.addColumn(new Column("SRC_PST", STRING, "Источник сделки", 60));
        //result.addColumn(new Column("PBR", STRING, "Система-источник", 80, false, false));
        result.addColumn(new Column("DEAL_ID", STRING, "ИД сделки", 120));
        Column colSubDealID;
        result.addColumn(colSubDealID = new Column("SUBDEALID", STRING, "ИД субсделки", 120));
        colSubDealID.setVisible(false);
        result.addColumn(new Column("PMT_REF", STRING, "ИД платежа", 120, false, false));

        //result.addColumn(new Column("PREF", STRING, "ИД сделки/ платежа", 120));

        result.addColumn(colProcDate = new Column("PROCDATE", DATE, "Дата опердня", 80));
        colProcDate.setFormat("dd.MM.yyyy");
        result.addColumn(colValueDate = new Column("VALD", DATE, "Дата валютирования", 80));
        colValueDate.setFormat("dd.MM.yyyy");
        podIndex = result.addColumn(colPostDate = new Column("POD", DATE, "Дата проводки", 80));
        colPostDate.setFormat("dd.MM.yyyy");

        result.addColumn(new Column("ACCTYPE_DR", DECIMAL, "AccType ДБ", 80, false, false, Column.Sort.ASC, "000000000"));
        result.addColumn(new Column("BSAACID_DR", STRING, "Счет ДБ", 160));
        result.addColumn(colFilialDr = new Column("FILIAL_DR", STRING, "Филиал ДБ (опер)", 60, false, false));
        result.addColumn(new Column("CCY_DR", STRING, "Валюта ДБ", 60));
        result.addColumn(new Column("AMNT_DR", DECIMAL, "Сумма ДБ", 100));
        result.addColumn(new Column("AMNTBC_DR", DECIMAL, "Сумма в руб. ДБ", 100));

        result.addColumn(new Column("ACCTYPE_CR", DECIMAL, "AccType КР", 80,false, false, Column.Sort.ASC, "000000000"));
        result.addColumn(new Column("BSAACID_CR", STRING, "Счет КР", 160));
        result.addColumn(colFilialCr = new Column("FILIAL_CR", STRING, "Филиал КР (опер)", 60, false, false));
        result.addColumn(new Column("CCY_CR", STRING, "Валюта КР", 60));
        result.addColumn(new Column("AMNT_CR", DECIMAL, "Сумма КР", 100));
        result.addColumn(new Column("AMNTBC_CR", DECIMAL, "Сумма в руб. КР", 100));

        result.addColumn(new Column("NRT", STRING, "Основание ENG", 500, false, false));
        result.addColumn(new Column("RNARSHT", STRING, "Основание короткое", 200, false, false));

        result.addColumn(col = new Column("FCHNG", STRING, "Исправительная", 40));
        col.setList(yesNoList);
        result.addColumn(new Column("PRFCNTR", STRING, "Профит центр", 60));
        result.addColumn(new Column("DEPT_ID", STRING, "Подразделение", 60));

        invisibleIndex = result.addColumn(colInvisible = new Column("INVISIBLE", STRING, "Подавлена", 40));

        colFilialDr.setVisible(false);
        colFilialCr.setVisible(false);

        return result;
    }

    protected Table prepareTable2() {
        Table result = new Table();
        Column col;
        Column colSubdeal;

        HashMap<Serializable, String> yesNoList = getYesNoList();
        result.addColumn(colGloid = new Column("GLO_REF", LONG, "ID операции", 70));
        result.addColumn(col = new Column("INP_METHOD", STRING, "Способ ввода", 50));
        col.setList(getEnumLabelsList(InputMethod.values()));
         result.addColumn(new Column("PCID", LONG, "ID проводки", 80));
        result.addColumn(new Column("SRC_PST", STRING, "Источник сделки", 60));
        result.addColumn(colDealId = new Column("DEAL_ID", STRING, "ИД сделки", 120));
        result.addColumn(colSubdeal = new Column("SUBDEALID", STRING, "ИД субсделки", 120));
        col.setVisible(false);
        result.addColumn(new Column("PMT_REF", STRING, "ИД платежа", 120, false, false));
        result.addColumn(colProcDate = new Column("PROCDATE", DATE, "Дата опердня", 80));
        colProcDate.setFormat("dd.MM.yyyy");
        result.addColumn(colValueDate = new Column("VALD", DATE, "Дата валютирования", 80));
        colValueDate.setFormat("dd.MM.yyyy");
        podIndex = result.addColumn(colPostDate = new Column("POD", DATE, "Дата проводки", 80));
        colPostDate.setFormat("dd.MM.yyyy");

        result.addColumn(new Column("ACCTYPE_DR", DECIMAL, "AccType ДБ", 80, false, false, Column.Sort.ASC, "000000000"));
        result.addColumn(colAccount = new Column("BSAACID_DR", STRING, "Счет ДБ", 160));
        result.addColumn(colCurrency = new Column("CCY_DR", STRING, "Валюта ДБ", 60));
        result.addColumn(new Column("AMNT_DR", DECIMAL, "Сумма ДБ", 100));
        result.addColumn(new Column("AMNTBC_DR", DECIMAL, "Сумма в руб. ДБ", 100));
        result.addColumn(new Column("ACCTYPE_CR", DECIMAL, "AccType КР", 80,false, false, Column.Sort.ASC, "000000000"));
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
        result.addColumn(colCustomer = new Column("DEPT_ID", STRING, "Подразделение", 60));

        colDealSource = new Column("SRC_PST", STRING, "Источник сделки", 60);
        colAcc2 = new Column("ACC2", STRING, "Источник сделки", 60);
        colDateOpen = new Column("VALD", DATE, "Дата открытия", 80);
        colDateClose = new Column("VALD", DATE, "Дата закрытия", 80);
        colFilialDr = new Column("FILIAL_DR", STRING, "Филиал", 80);
        colFilialCr = new Column("FILIAL_CR", STRING, "Филиал", 80);
        colPrfcntr = new Column("PRFCNTR", STRING, "Профит центр", 80);
        colDealSource.setVisible(false);
        colFilialDr.setVisible(false);
        colFilialCr.setVisible(false);

        result.addColumn(colDealSource);
        result.addColumn(colFilialDr);
        result.addColumn(colFilialCr);

        return result;
    }

    protected GridAction createPreview(){
        return new GridAction(this.getGrid(), null, "Просмотр", new Image(ImageConstants.INSTANCE.preview()), 10, true) {
            @Override
            public void execute() {
                Row row = grid.getCurrentRow();
                if ((row == null)) {
                    return;
                }

                PostingTechViewDlg dlg = new PostingTechViewDlg("Просмотр проводки по техническим счетам", FormAction.PREVIEW, this.grid.getTable().getColumns());
                dlg.setDlgEvents(this);
                dlg.show(row);
            }
        };
    }

    private GridAction createPostingTech() {
        return (GridAction) commonLazyAction("PostingTechDlg", "Создание ручной проводки по тех. счетам", CREATE, new Columns() ,
                "Ручная проводка не создана",
                "Ошибка создания проводки",
                "Проводка создана успешно");
    }
    private GridAction editPostingTech() {
        return (GridAction) commonLazyAction("PostingTechDlg", "Редактирование ручной проводки по тех. счетам", UPDATE, table.getColumns(),
                "Ручная проводка не изменена",
                "Ошибка изменения проводки",
                "Проводка изменена успешно");
    }

    private GridAction closePostingTech() {
        return (GridAction) otherAction(new AccountCloseDlg("Закрытие счета GL", OTHER, grid.getTable().getColumns()),
                "Счет не изменен",
                "Ошибка изменения проводки",
                "Счет изменен успешно",
                ImageConstants.INSTANCE.close24());
    }


    protected void save(ManualTechOperationWrapper cnw, FormAction action, AsyncCallback<RpcRes_Base<ManualTechOperationWrapper>> asyncCallbackImpl) throws Exception {
        switch(action) {
            case CREATE:
                BarsGLEntryPoint.operationService.saveTechOperation(cnw, asyncCallbackImpl);
                break;
            case UPDATE:
                BarsGLEntryPoint.operationService.updateTechPostings(cnw, asyncCallbackImpl);
                break;
        }
        refreshAction.execute();
    }

    @Override
    protected String getSuccessMessage(ManualTechOperationWrapper wrapper, FormAction action) {
        switch(action) {
            case UPDATE:
                return "";
            case OTHER:
                String dateClose = wrapper.getPostDateStr();
                String account = wrapper.getAccountDebit();
                return (dateClose == null ?
                        "Отменено закрытие счета ЦБ '" + account + "'"
                        : "Счет ЦБ '" + account + "' закрыт");
            default: {
                Window.alert("PostingFormTech: getSuccessMessage default");
                return null;
            }
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
            super(PostingFormTech.this.grid, ImageConstants.INSTANCE.oper_go());
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

            ManualTechOperationWrapper wrapper = new ManualTechOperationWrapper();
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
        return new AccountQuickFilterParams(colFilialDr, colCurrency, colAccount, colAcc2,
                colCustomer, colDealSource, colDealId, colDateOpen, colDateClose) {
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

    private boolean checkPostDate(Row row) {
        boolean check = (podIndex >= 0) && !((Date) row.getField(podIndex).getValue()).before(editday);
        if (!check) {
            showInfo("Нельзя изменить проводку, учтенную в балансе более чем " + DAYS_EDIT + " дней назад");
        }
        return check;
    }

    private boolean isInvisible(Row row) {
        return (invisibleIndex >= 0) && "Y".equals((String) row.getField(invisibleIndex).getValue());
    }

    private ArrayList<Long> getPdIdList() {
        ArrayList<Long> pdList = new ArrayList<Long>();
        Row row = grid.getCurrentRow();
        if (null == row)
            return null;
        pdList.add((Long) row.getField(idDrIndex).getValue());
        return pdList;
    }

    class DeleteAction extends GridAction {
        private PostingChoice postingChoice;
        private PostingTechDlg dlg;

        public DeleteAction() {
            super(PostingFormTech.this.grid, null, "Подавить / восстановить проводку", new Image(ImageConstants.INSTANCE.close24()), 10, true);
        }

        @Override
        public void execute() {
            Row row = grid.getCurrentRow();
            int rowCount = grid.getRowCount();
            if ((row == null) || (rowCount < 1)) {
                return;
            }
            if (!checkPostDate(row)) {
                return ;
            }

            boolean isInvisible = isInvisible(row);
            String act = isInvisible ? "Отмена подавления" : "Подавление";

            postingChoice = (rowCount > 1) ? PST_ALL : PST_SINGLE;
            String title = act + ((postingChoice == PST_ALL) ? " связанных проводок" : " проводки");
            dlg = new PostingTechDlg(title, FormAction.OTHER, table.getColumns());
            dlg.setDlgEvents(PostingFormTech.DeleteAction.this);
            dlg.show(row);
        }

        @Override
        public void onDlgOkClick(Object prms) {
            WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

            ManualTechOperationWrapper wrapper = (ManualTechOperationWrapper) prms;
            wrapper.setPdIdList(getPdIdList());

            BarsGLEntryPoint.operationService.suppressPdTh(wrapper, new PostingFormTech.PostingAsyncCallback(postingChoice, dlg) );
        }
    }

    class PostingAsyncCallback extends AuthCheckAsyncCallback<RpcRes_Base<ManualTechOperationWrapper>> {
        final PostingChoice postingChoice;
        private PostingTechDlg dlg;

        public PostingAsyncCallback(PostingChoice postingChoice, PostingTechDlg dlg) {
            this.postingChoice = postingChoice;
            this.dlg = dlg;
        }

        @Override
        public void onFailureOthers(Throwable throwable) {
            WaitingManager.hide();
            showInfo("Ошибка при изменении проводки", throwable.getLocalizedMessage());
        }

        @Override
        public void onSuccess(RpcRes_Base<ManualTechOperationWrapper> wrapper) {
            if (wrapper.isError()) {
                showInfo("Ошибка при изменении проводки", wrapper.getMessage());
            } else {
                showInfo("Проводки изменены успешно");
                dlg.hide();
                refreshAction.execute();
            }
            WaitingManager.hide();
        }
    }
}
