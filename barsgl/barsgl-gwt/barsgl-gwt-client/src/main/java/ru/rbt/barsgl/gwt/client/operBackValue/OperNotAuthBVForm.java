package ru.rbt.barsgl.gwt.client.operBackValue;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.web.bindery.event.shared.HandlerRegistration;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.comp.GLComponents;
import ru.rbt.barsgl.gwt.client.operation.OperationDlg;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.comp.PopupMenuBuilder;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.*;
import ru.rbt.barsgl.gwt.core.events.CommonEvents;
import ru.rbt.barsgl.gwt.core.events.CommonEventsHandler;
import ru.rbt.barsgl.gwt.core.events.LocalEventBus;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.statusbar.StatusBarManager;
import ru.rbt.barsgl.gwt.core.widgets.IGridRowChanged;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.ClientDateUtils;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.*;
import ru.rbt.barsgl.shared.filter.FilterCriteria;
import ru.rbt.barsgl.shared.operation.BackValueWrapper;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.grid.gwt.client.gridForm.GridForm;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.security.gwt.client.CommonEntryPoint;
import ru.rbt.shared.user.AppUserWrapper;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.*;
import static ru.rbt.barsgl.gwt.client.security.AuthWherePart.getSourceAndFilialPart;
import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.*;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

/**
 * Created by er17503 on 24.07.2017.
 */
public class OperNotAuthBVForm extends GridForm {
    private static final String _title = "Неавторизованные операции";
    private String _select = "select * from (" +
                             "select op.*, ex.POSTDATE_PLAN, ex.MNL_RSNCODE, ex.BV_CUTDATE, ex.PRD_LDATE, ex.PRD_CUTDATE, ex.MNL_NRT, " +
                             "ex.MNL_STATUS, ex.USER_AU3, ex.OTS_AU3, ex.OTS_AUTO, " +
                             "u.SURNAME || ' ' ||  LEFT(u.FIRSTNAME, 1) || '.' || LEFT(COALESCE(u.PATRONYMIC, ''), 1) || " +
                             "case when COALESCE(u.PATRONYMIC, '') = '' then '' else '.' end as AUTHOR\n" +
                             "from (select a.*, a.AC_DR || ' ' || a.AC_CR as dr_cr from V_GL_OPERCUST as a) op \n" +
                             "join GL_OPEREXT ex on op.GLOID = ex.GLOID \n" +
                             "left join GL_USER u on ex.USER_AU3 = u.USER_NAME) v ";

    private GridAction _modeChoiceAction;
    private GridAction _waitingReasonAction;
    private GridAction _operAuthorizationAction;
    private GridAction _waitingReasonListAction;
    private GridAction _operAuthorizationListAction;
    private GridAction _statisticsAction;

    private BVModeChoiceDlg.ModeType _mode = BVModeChoiceDlg.ModeType.NONE;
    private BVModeChoiceDlg.StateType _state = BVModeChoiceDlg.StateType.ALL;
    private Boolean _owner = false;

    private String user;

    private Column vDate;
    private Column source;
    private Column status;

    private  WaitingReasonDlg reasonDlg;
    private BVPostDateAuthListDlg postDateAuthListDlg;

    public OperNotAuthBVForm() {
        super(_title, true);

        AppUserWrapper wrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
        user = wrapper == null ? "" : wrapper.getUserName();

        setSql(sql());
        //Window.alert(sql());
        reconfigure();
        doActionVisibility();

        setRowChangeEventHandler();
        _modeChoiceAction.execute();
    }


    private void reconfigure() {
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        abw.addAction(createPreview());
        abw.addAction(_modeChoiceAction = createModeChoice());
        abw.addAction(_waitingReasonAction = createWaitingReasonAction());
        abw.addAction(_operAuthorizationAction = createOperAuthorizationAction());
        abw.addAction(_waitingReasonListAction = createWaitingReasonListAction());
        abw.addAction(_operAuthorizationListAction = createOperAuthorizationListAction());
        abw.addAction(_statisticsAction = createStatisticsAction());
    }

    private void doActionVisibility(){
        _operAuthorizationAction.setVisible(_mode == BVModeChoiceDlg.ModeType.SINGLE);
        _waitingReasonAction.setVisible(_mode == BVModeChoiceDlg.ModeType.SINGLE);
        _waitingReasonListAction.setVisible(_mode == BVModeChoiceDlg.ModeType.LIST);
        _operAuthorizationListAction.setVisible(_mode == BVModeChoiceDlg.ModeType.LIST);
        _statisticsAction.setVisible(_mode == BVModeChoiceDlg.ModeType.LIST);
    }

    /*Actions*/

    private GridAction createPreview(){
        return new GridAction(grid, null, "Просмотр", new Image(ImageConstants.INSTANCE.preview()), 10, true) {
            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;

                OperationDlg dlg = new OperationDlg("Просмотр бухгалтерской операции GL", FormAction.PREVIEW, table.getColumns());
                dlg.setDlgEvents(this);
                dlg.show(rowToWrapper());
            }
        };
    }

    private ManualOperationWrapper rowToWrapper(){
        ManualOperationWrapper wrapper = new ManualOperationWrapper();

        wrapper.setId((Long) getValue("GLOID"));

        wrapper.setPostDateStr(ClientDateUtils.Date2String((Date) getValue("POSTDATE")));
        wrapper.setValueDateStr(ClientDateUtils.Date2String((Date) getValue("VDATE")));

        wrapper.setDealSrc((String) getValue("SRC_PST"));
        wrapper.setDealId((String) getValue("DEAL_ID"));
        wrapper.setSubdealId((String) getValue("SUBDEALID"));

        wrapper.setCurrencyDebit((String) getValue("CCY_DR"));
        wrapper.setFilialDebit((String) getValue("CBCC_DR"));
        wrapper.setAccountDebit((String) getValue("AC_DR"));
        wrapper.setAmountDebit((BigDecimal) getValue("AMT_DR"));

        wrapper.setCurrencyCredit((String) getValue("CCY_CR"));
        wrapper.setFilialCredit((String) getValue("CBCC_CR"));
        wrapper.setAccountCredit((String) getValue("AC_CR"));
        wrapper.setAmountCredit((BigDecimal) getValue("AMT_CR"));

        wrapper.setAmountRu((BigDecimal) getValue("AMTRU_DR"));
        wrapper.setCorrection("Y".equals(((String) getValue("FCHNG")))); //TODO некошерно

        wrapper.setNarrative((String) getValue("NRT"));
        wrapper.setRusNarrativeLong((String) getValue("RNRTL"));

        wrapper.setDeptId((String) getValue("DEPT_ID"));
        wrapper.setProfitCenter((String) getValue("PRFCNTR"));

        String errorMessage = (String) getValue("EMSG");
        if (!isEmpty(errorMessage))
            wrapper.getErrorList().addErrorDescription(errorMessage);

        return wrapper;
    }


    private GridAction createModeChoice(){
        return new GridAction(grid, null, "Выбор способа обработки", new Image(ImageConstants.INSTANCE.site_map()), 10) {
            BVModeChoiceDlg dlg;

            @Override
            public void execute() {
                dlg = new BVModeChoiceDlg();
                dlg.setDlgEvents(this);
                Object[] prms = new Object[] {_mode, _owner, _state};
                dlg.show(prms);
            }

            @Override
            public void onDlgOkClick(Object prms){
                dlg.hide();

                _mode = (BVModeChoiceDlg.ModeType)((Object[])prms)[0];
                _owner = (Boolean)((Object[])prms)[1];
                _state = (BVModeChoiceDlg.StateType)((Object[])prms)[2];

                StatusBarManager.ChangeStatusBarText(getStatusBarText(), StatusBarManager.MessageReason.MSG);
                doActionVisibility();
             //   Window.alert("Before: " + sql());
                setSql(sql());
                filterTuning();
             //   Window.alert("After: " + sql());
            }

            private String getStatusBarText(){
                String pattern = "Способ обработки: [{0}]. Состояние: [{1}]";
                return Utils.Fmt(pattern, _mode.getLabel(), _state.getLabel());
            }

            HandlerRegistration registration;

            private void filterTuning(){
                ArrayList<FilterItem> list = new ArrayList<FilterItem>();
                filterAction.clearFilterCriteria(true);

                switch (_mode) {
                    case NONE:
                        grid.setInitialFilterCriteria(null);
                        refreshAction.setFilterCriteria(null);
                        refreshAction.execute();
                        break;
                    case SINGLE:
                        registration =  LocalEventBus.addHandler(CommonEvents.TYPE, filterCanceledEventHandler());
                        list.add(new FilterItem(vDate, FilterCriteria.LT, CommonEntryPoint.CURRENT_OPER_DAY, false, false, true));
                        grid.setInitialFilterCriteria(list);
                        filterAction.execute(true);
                        break;
                    case LIST:
                        registration =  LocalEventBus.addHandler(CommonEvents.TYPE, filterCanceledEventHandler());
                        list.add(new FilterItem(vDate, FilterCriteria.EQ, CommonEntryPoint.CURRENT_WORKDAY, false, true, true));
                        list.add(new FilterItem(source, FilterCriteria.EQ, "", false, true, true));
                        list.add(new FilterItem(status, FilterCriteria.EQ, "CONTROL", false, true, true));
                        grid.setInitialFilterCriteria(list);
                        filterAction.execute(true);
                        break;
                }
            }

            private CommonEventsHandler filterCanceledEventHandler(){
                return new CommonEventsHandler() {
                    @Override
                    public void event(String id, Object data) {
                        if (id.equalsIgnoreCase("FilterCanceled")) {
                            if (_mode == BVModeChoiceDlg.ModeType.LIST){
                                ArrayList<FilterItem> list = new ArrayList<FilterItem>();
                                list.add(new FilterItem(vDate, FilterCriteria.EQ, CommonEntryPoint.CURRENT_WORKDAY, false, true, true));
                                list.add(new FilterItem(source, FilterCriteria.EQ, "", false, true, true));
                                list.add(new FilterItem(status, FilterCriteria.EQ, "CONTROL", false, true, true));
                                grid.setInitialFilterCriteria(list);
                                refreshAction.setFilterCriteria(list);

                            }else {
                                grid.setInitialFilterCriteria(null);
                                refreshAction.setFilterCriteria(null);
                            }
                            refreshAction.execute();
                            if (registration != null) registration.removeHandler();
                        }
                        else if (id.equalsIgnoreCase("FilterOKed")){
                            if (registration != null) registration.removeHandler();
                        }
                    }
                };
            }
        };
    }

    private GridAction createWaitingReasonAction() {
        return new GridAction(grid, null, "Задержать операцию", new Image(ImageConstants.INSTANCE.locked()), 10) {
            @Override
            public void execute() {
                executeWaitingAction(BackValueMode.ONE).execute();
            }
        };
    }

    private GridAction createOperAuthorizationAction() {
        return new GridAction(grid, null, "Авторизовать дату операции", new Image(ImageConstants.INSTANCE.back_value()), 10) {
            BVPostDateAuthDlg dlg;
            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;

                dlg = new BVPostDateAuthDlg("Авторизация даты бухгалтерской операции GL", FormAction.PREVIEW, table.getColumns());
                dlg.setDlgEvents(this);
                dlg.show(rowToWrapper());
            }

            @Override
            public void onDlgOkClick(Object prms){
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
                ManualOperationWrapper manualOperationWrapper = (ManualOperationWrapper) prms;
                List<Long> gloids = new ArrayList<>();
                gloids.add((Long)getFieldByName("GLOID").getValue());
                String postDate = manualOperationWrapper.getPostDateStr();
                BackValueWrapper wrapper = createWrapper(gloids, BackValueAction.SIGN, BackValueMode.ONE, postDate, (String)getFieldByName("MNL_NRT").getValue());
                methodCaller(dlg, "Авторизация даты бухгалтерской операции не удалась.", wrapper, true);
            }
        };
    }

    private GridAction createWaitingReasonListAction(){
        final PopupMenuBuilder builder = new PopupMenuBuilder(abw, "Задержать операцию",  new Image(ImageConstants.INSTANCE.locked()));

        MenuItem itemVisibleList = new MenuItem("Видимый список", new Command() {

            @Override
            public void execute() {
                builder.hidePopup();
                executeWaitingAction(BackValueMode.VISIBLE).execute();
            }
        });

        MenuItem itemAllList = new MenuItem("Полный список", new Command() {

            @Override
            public void execute() {
                builder.hidePopup();
                executeWaitingAction(BackValueMode.ALL).execute();
            }
        });

        builder.addItem(itemVisibleList);
        builder.addSeparator();
        builder.addItem(itemAllList);
        return  builder.toAction(grid);
    }

    private GridAction createOperAuthorizationListAction(){
        final PopupMenuBuilder builder = new PopupMenuBuilder(abw, "Авторизовать дату операции",  new Image(ImageConstants.INSTANCE.back_value()));

        MenuItem itemVisibleList = new MenuItem("Видимый список", new Command() {

            @Override
            public void execute() {
                builder.hidePopup();
                executePostDateAuthListAction(BackValueMode.VISIBLE).execute();
            }
        });

        MenuItem itemAllList = new MenuItem("Полный список", new Command() {

            @Override
            public void execute() {
                builder.hidePopup();
                executePostDateAuthListAction(BackValueMode.ALL).execute();
            }
        });

        builder.addItem(itemVisibleList);
        builder.addSeparator();
        builder.addItem(itemAllList);
        return  builder.toAction(grid);
    }

    private GridAction createStatisticsAction() {
        return new GridAction(grid, null, "Статистика", new Image(ImageConstants.INSTANCE.statistics()), 10) {

            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;

                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
                BackValueWrapper wrapper = createWrapper(null, BackValueAction.STAT, BackValueMode.ALL, "", "");
                methodCaller(null, "Операция получения статистики не удалась.", wrapper, false);
            }
        };
    }

    /*Wrapper*/
    private BackValueWrapper createWrapper(List<Long> gloIDs, BackValueAction action, BackValueMode mode, String postDate, String comment){
        BackValueWrapper wrapper = new BackValueWrapper();
        wrapper.setGloIDs(gloIDs);
        wrapper.setFilters(grid.getFilterCriteria());
        wrapper.setSql(sql());
        wrapper.setAction(action);
        wrapper.setMode(mode);
        wrapper.setPostDateStr(postDate);
        wrapper.setComment(comment);
        wrapper.setBvStatus(BackValuePostStatus.valueOf((String)getFieldByName("MNL_STATUS").getValue())) ;

        return wrapper;
    }

    private GridAction executeWaitingAction(final BackValueMode mode) {
        return new GridAction(grid, "", "", null, 0) {

            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;
                Object value = null;
                if (mode == BackValueMode.ONE){
                    value = getFieldByName("MNL_NRT").getValue();
                }

                reasonDlg = reasonDlg == null ? new WaitingReasonDlg() : reasonDlg;
                reasonDlg.setOkButtonCaption("Подтвердить");
                reasonDlg.setCaption("Основание для задержания операции");
                reasonDlg.setDlgEvents(this);

                reasonDlg.show(value);
            }

            @Override
            public void onDlgOkClick(Object prms) throws Exception{
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                List<Long> gloids = null;
                switch (mode) {
                    case ONE:
                        gloids = new ArrayList<>();
                        gloids.add((Long)getFieldByName("GLOID").getValue());
                        break;
                    case VISIBLE:
                        gloids = rows2GloIDs(grid.getVisibleItems());
                        break;
                    case ALL:
                        gloids = null;
                }

                String postDate = ClientDateUtils.Date2String((Date)getFieldByName("POSTDATE").getValue());
                BackValueWrapper wrapper = createWrapper(gloids, BackValueAction.TO_HOLD, mode, postDate, (String)prms);
                methodCaller(reasonDlg, Utils.Fmt("Задержание операции{0}не удалось.", mode == BackValueMode.ONE ? " " : " списком "), wrapper, true);
            }
        };
    }

    private GridAction executePostDateAuthListAction(final BackValueMode mode) {
        return new GridAction(grid, "", "", null, 0) {

            @Override
            public void execute() {
                if (mode == BackValueMode.ONE) return;

                final Row row = grid.getCurrentRow();
                if (row == null) return;

                postDateAuthListDlg = new BVPostDateAuthListDlg();
                postDateAuthListDlg.setDlgEvents(this);
                postDateAuthListDlg.show((Date)grid.getFieldValue("VDATE"));
            }

            @Override
            public void onDlgOkClick(Object prms) throws Exception{
                final String postDate = ClientDateUtils.Date2String((Date)prms);

                DialogManager.confirm("Авторизация даты", Utils.Fmt("Авторизовать дату проводки {0} для всего списка операций?", postDate), "Да", "Нет", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        caller(postDate);
                    }
                }, null);
            }

            private void caller(String postDate){
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                List<Long> gloids = null;
                if (mode == BackValueMode.VISIBLE) {
                    gloids = rows2GloIDs(grid.getVisibleItems());
                }

                BackValueWrapper wrapper = createWrapper(gloids, BackValueAction.SIGN, mode, postDate, null);
                methodCaller(postDateAuthListDlg, "Авторизация даты бухгалтерской операции списком не удалась.", wrapper, true);
            }
        };
    }

    private List<Long> rows2GloIDs(List<Row> rows){
        List<Long> res = new ArrayList<>();
        for (Row row: rows){
            res.add((Long)row.getField(0).getValue());
        }

        return res;
    }

    /*Method caller*/
    private void methodCaller(final DlgFrame dlg, final String message, final BackValueWrapper wrapper, final boolean isRefredh) {
        BarsGLEntryPoint.operationService.processOperationBv(wrapper, new AuthCheckAsyncCallback<RpcRes_Base<Integer>>() {

            @Override
            public void onSuccess(RpcRes_Base<Integer> res) {
                if (res.isError()){
                    DialogManager.error("Ошибка", message + "\nОшибка: " + res.getMessage());
                } else {
                    if (dlg != null) dlg.hide();
                    if (isRefredh) refreshAction.execute();
                    showInfo(res.getMessage());
                }
                WaitingManager.hide();
            }
        });
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();
        Column col;

        HashMap<Serializable, String> yesNoList = getYesNoList();
        result.addColumn(new Column("GLOID", LONG, "ID операции", 70));// No Space
        result.addColumn(new Column("PST_REF", LONG, "ID запроса", 70, false, false));
        result.addColumn(new Column("ID_PST", STRING, "ИД сообщ АЕ", 70));
        result.addColumn(col = new Column("INP_METHOD", STRING, "Способ ввода", 70, false, false));
        col.setList(getEnumLabelsList(InputMethod.values()));

        result.addColumn(source = new Column("SRC_PST", STRING, "Источник сделки", 70));
        result.addColumn(new Column("DEAL_ID", STRING, "ИД сделки", 120));
        result.addColumn(new Column("SUBDEALID", STRING, "ИД субсделки", 120, false, false));
        result.addColumn(new Column("PMT_REF", STRING, "ИД платежа", 70));

        result.addColumn(col = new Column("STATE", STRING, "Статус", 70, false, false));
        col.setList(getEnumValuesList(OperState.values()));

        result.addColumn(col = new Column("PST_SCHEME", STRING, "Схема проводок", 70, false, false));
        col.setList(getEnumLabelsList(OperType.values()));

        result.addColumn(status = new Column("MNL_STATUS", STRING, "Статус руч. обработки", 120));
        status.setList(GLComponents.getArrayValuesList(new String[]{"", "COMPLETED", "CONTROL", "HOLD", "SIGNEDDATE"}));
        result.addColumn(new Column("MNL_RSNCODE", STRING, "Причина руч. обработки", 120));

        result.addColumn(col = new Column("PROCDATE", DATE, "Дата опердня", 80));
        col.setFormat("dd.MM.yyyy");
        result.addColumn(vDate = new Column("VDATE", DATE, "Дата валютирования", 80));
        vDate.setFormat("dd.MM.yyyy");
        result.addColumn(col = new Column("POSTDATE", DATE, "Дата проводки", 80));
        col.setFormat("dd.MM.yyyy");
        result.addColumn(col = new Column("POSTDATE_PLAN", DATE, "Первичная дата проводки", 80, false, false));
        col.setFormat("dd.MM.yyyy");

        result.addColumn(new Column("AC_DR", STRING, "Счет ДБ", 160));
        result.addColumn(new Column("ACCTYPE_DR", STRING, "Тип счета ДБ", 100, false, false));
        result.addColumn(new Column("ACC2_DR", STRING, "Счет 2 порядка ДБ", 100, false, false));
        result.addColumn(new Column("CCY_DR", STRING, "Валюта ДБ", 60));
        result.addColumn(new Column("AMT_DR", DECIMAL, "Сумма ДБ", 100));
        result.addColumn(new Column("AMTRU_DR", DECIMAL, "Сумма в рублях ДБ", 100, false, false));
        result.addColumn(new Column("RATE_DR", DECIMAL, "Курс ДБ", 100, false, false));
        result.addColumn(new Column("EQV_DR", DECIMAL, "Руб.экв. ДБ", 100, false, false));
        result.addColumn(new Column("CBCC_DR", STRING, "Филиал ДБ", 60));

        result.addColumn(new Column("AC_CR", STRING, "Счет КР", 160));
        result.addColumn(new Column("ACCTYPE_CR", STRING, "Тип счета КР", 100, false, false));
        result.addColumn(new Column("ACC2_CR", STRING, "Счет 2 порядка КР", 100, false, false));
        result.addColumn(new Column("CCY_CR", STRING, "Валюта КР", 60));
        result.addColumn(new Column("AMT_CR", DECIMAL, "Сумма КР", 100));
        result.addColumn(new Column("AMTRU_CR", DECIMAL, "Сумма в рублях КР", 100, false, false));
        result.addColumn(new Column("RATE_CR", DECIMAL, "Курс КР", 100, false, false));
        result.addColumn(new Column("EQV_CR", DECIMAL, "Руб.экв. КР", 100, false, false));
        result.addColumn(new Column("CBCC_CR", STRING, "Филиал КР", 60));

        result.addColumn(new Column("BS_CHAPTER", STRING, "Глава", 50, false, false));
        result.addColumn(col = new Column("FAN", STRING, "Веер", 50));
        col.setList(yesNoList);
        result.addColumn(new Column("PAR_GLO", LONG, "Голова веера", 70, false, false));
        result.addColumn(new Column("PAR_RF", STRING, "ИД веера", 70, false, false));
        result.addColumn(col = new Column("STRN", STRING, "Сторно", 50));
        col.setList(yesNoList);
        result.addColumn(new Column("STRN_GLO", LONG, "Сторно операция", 70, false, false));
        result.addColumn(new Column("EMSG", STRING, "Сообщение об ошибке", 1200));
        result.addColumn(new Column("CUSTNO_DR", STRING, "Клиент ДБ", 400));
        result.addColumn(new Column("CUSTNO_CR", STRING, "Клиент КР", 400));

        result.addColumn(new Column("NRT", STRING, "Основание ENG", 300, false, false));
        result.addColumn(new Column("RNRTL", STRING, "Основание RUS", 300, false, false));
        result.addColumn(new Column("RNRTS", STRING, "Основание короткое", 100, false, false));

        result.addColumn(new Column("DEPT_ID", STRING, "Подразделение", 100, false, false));
        result.addColumn(new Column("PRFCNTR", STRING, "Профит центр", 100, false, false));
        result.addColumn(col = new Column("FCHNG", STRING, "Исправительная", 50, false, false));
        col.setList(yesNoList);

        result.addColumn(new Column("USER_NAME", STRING, "Пользователь", 100, false, false));
        //вычисляемое поле для фильтра по условию "ИЛИ"
        result.addColumn(new Column("DR_CR", STRING, "Счета Дт/Кр", 70, false, false));

        result.addColumn(new Column("OTS_AUTO", Column.Type.DATETIME, "Время авт. обновления", 130, false, false));
        result.addColumn(col = new Column("BV_CUTDATE", DATE, "Пороговая дата backvalue ", 80, false, false));
        col.setFormat("dd.MM.yyyy");
        result.addColumn(col = new Column("PRD_LDATE", DATE, "Закрытый период", 80, false, false));
        col.setFormat("dd.MM.yyyy");
        result.addColumn(col = new Column("PRD_CUTDATE", DATE, "Дата закрытия периода", 80, false, false));
        col.setFormat("dd.MM.yyyy");
        result.addColumn(new Column("MNL_NRT", STRING, "Комментарий руч. обработки", 300, false, false));

        result.addColumn(col = new Column("AUTHOR", STRING, "Исполнитель", 180));
        col.setFilterable(false);
        result.addColumn(new Column("USER_AU3", STRING, "Логин исполнителя", 180, false, false));
        result.addColumn(new Column("OTS_AU3", Column.Type.DATETIME, "Время руч. обработки", 130, false, false));

        return result;
    }

    /*Prepare sql statement*/

    public void setSql(String text){
        sql_select = text;
        setExcelSql(sql_select);
    }

    private String sql(){
        return new StringBuilder()
                .append(_select )
                .append(whereBuilder()).toString();
    }

    private String whereBuilder(){
        String where = getModeWherePart();
        if (where == "") {
            where = getStateWherePart();
        }
        String whereOwher = getOwnerWherePart();
        if (whereOwher != "") {
            where = where + " and " + whereOwher;
        }

        return "where " + where + getAuthWherePart();
    }

    private String getModeWherePart(){
        return _mode == BVModeChoiceDlg.ModeType.NONE ?  "" : "MNL_STATUS in ('CONTROL', 'HOLD')";
    }

    private String getStateWherePart(){
        String res = "";
        switch (_state) {
            case ALL:
                res =  Utils.Fmt("(MNL_STATUS in ('HOLD', 'SIGNEDDATE', 'CONTROL') or (MNL_STATUS='COMPLETED' and PROCDATE='{0}'))",
                       ClientDateUtils.Date2String(CommonEntryPoint.CURRENT_OPER_DAY));
                break;
            case COMPLETED:
                res = Utils.Fmt("MNL_STATUS='COMPLETED' and PROCDATE='{0}'", ClientDateUtils.Date2String(CommonEntryPoint.CURRENT_OPER_DAY));
                break;
            case WORKING:
                res = "MNL_STATUS='SIGNEDDATE' and STATE in ('BLOAD', 'BWTAC')";
                break;
            case NOTCOMPLETED:
                res = "(MNL_STATUS in ('CONTROL', 'HOLD') or (MNL_STATUS='SIGNEDDATE' and STATE not in ('BLOAD', 'POST', 'BWTAC')))";
                break;
        }

        return res;
    }

    private String getOwnerWherePart(){
        return _owner ? Utils.Fmt("USER_AU3='{0}'", user) : "";
    }

    private String getAuthWherePart(){
        return getSourceAndFilialPart("and", "SRC_PST", "CBCC_CR", "CBCC_DR");
    }

    @Override
    protected String prepareSql() {
        return  null;
    }

    @Override
    protected ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("GLOID", Column.Sort.DESC));
        return list;
    }

    private void setRowChangeEventHandler(){
        grid.setRowChangedEvent(new IGridRowChanged() {
            @Override
            public void onRowChanged(Row row) {
                if (_mode == BVModeChoiceDlg.ModeType.NONE || grid.getCurrentRow() == null) return;
                boolean controlFlag = ((String) getFieldByName("MNL_STATUS").getValue()).equals(BackValuePostStatus.CONTROL.name());
                if (_waitingReasonAction.isVisible()) _waitingReasonAction.setEnable(controlFlag);
                if (_waitingReasonListAction.isVisible()) _waitingReasonListAction.setEnable(controlFlag);
            }
        });
    }
}
