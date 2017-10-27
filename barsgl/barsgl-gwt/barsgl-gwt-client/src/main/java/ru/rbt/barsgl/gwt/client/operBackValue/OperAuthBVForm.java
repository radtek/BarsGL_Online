package ru.rbt.barsgl.gwt.client.operBackValue;

import com.google.gwt.user.client.ui.Image;
import com.google.web.bindery.event.shared.HandlerRegistration;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.comp.GLComponents;
import ru.rbt.barsgl.gwt.client.gridForm.MDForm;
import ru.rbt.barsgl.gwt.client.operation.OperationDlg;
import ru.rbt.barsgl.gwt.core.SecurityChecker;
import ru.rbt.barsgl.gwt.core.actions.Action;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DialogManager;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.events.CommonEvents;
import ru.rbt.barsgl.gwt.core.events.CommonEventsHandler;
import ru.rbt.barsgl.gwt.core.events.LocalEventBus;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.statusbar.StatusBarManager;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.ClientDateUtils;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.*;
import ru.rbt.barsgl.shared.filter.FilterCriteria;
import ru.rbt.barsgl.shared.operation.BackValueWrapper;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.security.gwt.client.CommonEntryPoint;
import ru.rbt.shared.enums.SecurityActionCode;


import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.*;
import static ru.rbt.barsgl.gwt.client.comp.GLComponents.getArrayValuesList;
import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.*;
import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.DATE;
import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.STRING;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

/**
 * Created by er17503 on 24.07.2017.
 */
public class OperAuthBVForm extends MDForm {
    public static final String FORM_NAME = "Авторизованные операции";



    private String _detailSelect =  "select * from V_GL_PDLINK ";

    private GridAction _modeOperationAction;
    private GridAction _editPostDateAction;
    private GridAction _changeFanModeAction;

    private Column vDate;
    private Column procDate;

    private Column detailGloid;

    private BVOperChoiceDlg.ModeType _mode = BVOperChoiceDlg.ModeType.STANDARD;
    private BVOperChoiceDlg.SpecType _spec = BVOperChoiceDlg.SpecType.MANUAL;
    private boolean _owner = false;

    private boolean _detailFanMode = true;
    private AuthBVSqlBuilder _builder;


    public OperAuthBVForm() {
        super(FORM_NAME, null, "Проводки по операции", true);
        setLazyDetailRefresh(true);

        _builder = new AuthBVSqlBuilder();
        _builder.setConsolidateFAN(! _detailFanMode);
        setSql(_builder.buildSql(_mode, _spec, _owner));

        reconfigure();
        doActionVisibility();
        _modeOperationAction.execute();
    }

    private void reconfigure() {
        masterActionBar.addAction(new SimpleDlgAction(masterGrid, DlgMode.BROWSE, 10));
        masterActionBar.addAction(createPreview());
        masterActionBar.addAction(_modeOperationAction = createOperationChoiceAction());
        masterActionBar.addAction(_editPostDateAction = createPostDateAction());

        masterActionBar.addAction(_changeFanModeAction = createFanModeAction());
    }

    private void doActionVisibility(){
        _editPostDateAction.setVisible(SecurityChecker.checkActions(SecurityActionCode.OperHand3, SecurityActionCode.OperHand3Super));
        _changeFanModeAction.setVisible(_spec == BVOperChoiceDlg.SpecType.FAN);
    }

    private GridAction createPreview(){
        return new GridAction(masterGrid, null, "Просмотр", new Image(ImageConstants.INSTANCE.preview()), 10, true) {
            @Override
            public void execute() {
              final Row row = grid.getCurrentRow();
                if (row == null) return;

                OperationDlg dlg = new OperationDlg("Просмотр бухгалтерской операции GL", FormAction.PREVIEW, masterTable.getColumns());
                dlg.setDlgEvents(this);
                dlg.show(masterRowToWrapper());
            }
        };
    }

    private GridAction createOperationChoiceAction() {
        return new GridAction(masterGrid, null, "Выбор операции Backvalue", new Image(ImageConstants.INSTANCE.site_map()), 10) {
            BVOperChoiceDlg dlg;

            @Override
            public void execute() {
                dlg = new BVOperChoiceDlg();
                dlg.setDlgEvents(this);
                Object[] prms = new Object[] {_mode, _owner, _spec};
                dlg.show(prms);
            }

            @Override
            public void onDlgOkClick(Object prms){
                dlg.hide();

                _mode = (BVOperChoiceDlg.ModeType)((Object[])prms)[0];
                _owner = (Boolean)((Object[])prms)[1];
                _spec = (BVOperChoiceDlg.SpecType)((Object[])prms)[2];

                StatusBarManager.ChangeStatusBarText(getStatusBarText(), StatusBarManager.MessageReason.MSG);
                changeFanMode(_changeFanModeAction, _detailFanMode = true);

                doActionVisibility();
                _builder.setConsolidateFAN(! _detailFanMode);
                setSql(_builder.buildSql(_mode, _spec, _owner));
                changeColumnVisibility();
                // Window.alert("Before Filter SQL: " + masterSql);
                filterTuning();
            }

            private String getStatusBarText(){
                String pattern = "Тип обработки: [{0}]. Специфика: [{1}]";
                return Utils.Fmt(pattern, _mode.getLabel(), _spec.getLabel());
            }

            HandlerRegistration registration;

            private void filterTuning(){
                ArrayList<FilterItem> list = new ArrayList<FilterItem>();
                masterFilterAction.clearFilterCriteria(true);

                registration =  LocalEventBus.addHandler(CommonEvents.TYPE, filterCanceledEventHandler());
                list.add(new FilterItem(vDate, FilterCriteria.LT, CommonEntryPoint.CURRENT_OPER_DAY, false, false, true));
                list.add(new FilterItem(procDate, FilterCriteria.GE, CommonEntryPoint.CURRENT_WORKDAY, false, false, true));
                grid.setInitialFilterCriteria(list);
                masterFilterAction.execute(true);
            }

            private CommonEventsHandler filterCanceledEventHandler(){
                return new CommonEventsHandler() {
                    @Override
                    public void event(String id, Object data) {
                        if (id.equalsIgnoreCase("FilterCanceled")) {
                            grid.setInitialFilterCriteria(null);
                            masterRefreshAction.setFilterCriteria(null);
                            masterRefreshAction.execute();
                        }

                        if (registration != null) registration.removeHandler();
                    }
                };
            }
        };
    }

    private GridAction createPostDateAction(){
        return  new GridAction(masterGrid, null, "Изменить дату проводки", new Image(ImageConstants.INSTANCE.edit24()), 10, true) {
            BVPostDateAuthDlg dlg;
            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;

                dlg = new BVPostDateAuthDlg("Изменение даты проводки", FormAction.PREVIEW, masterTable.getColumns());
                dlg.setDlgEvents(this);
                dlg.show(masterRowToWrapper());
            }

            @Override
            public void onDlgOkClick(Object prms){
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                ManualOperationWrapper manualOperationWrapper = (ManualOperationWrapper) prms;
                List<Long> gloids = new ArrayList<>();
                gloids.add((Long)grid.getCurrentRow().getField(masterTable.getColumns().getColumnIndexByName("GLOID")).getValue());
                String postDate = manualOperationWrapper.getPostDateStr();

                BackValueWrapper wrapper = new BackValueWrapper();
                wrapper.setGloIDs(gloids);
                wrapper.setAction(BackValueAction.EDIT_DATE);
                wrapper.setMode(BackValueMode.ONE);
                wrapper.setPostDateStr(postDate);

                BarsGLEntryPoint.operationService.processOperationBv(wrapper, new AuthCheckAsyncCallback<RpcRes_Base<Integer>>() {

                    @Override
                    public void onSuccess(RpcRes_Base<Integer> res) {
                        if (res.isError()){
                            DialogManager.error("Ошибка", "Изменение даты проводки не удалось.\nОшибка: " + res.getMessage());
                        } else {
                            if (dlg != null) dlg.hide();
                            showInfo(res.getMessage());
                            masterRefreshAction.execute();
                        }
                        WaitingManager.hide();
                    }
                });
            }
        };
    }

    private GridAction createFanModeAction(){
        return  new GridAction(masterGrid, null, "Режим: Детальный", new Image(ImageConstants.INSTANCE.nonauthoriz()), 10) {
            @Override
            public void execute() {
                _detailFanMode = ! _detailFanMode;
                changeFanMode(this, _detailFanMode);
                _builder.setConsolidateFAN(! _detailFanMode);
                setSql(_builder.buildSql(_mode, _spec, _owner));
                masterRefreshAction.execute();
            }
        };
    }

    public void setSql(String text){
        masterSql = text;
        setMasterExcelSql(masterSql);
    }

    @Override
    protected Table prepareMasterTable() {
        Table result = new Table();
        Column col;

        HashMap<Serializable, String> yesNoList = getYesNoList();
        result.addColumn(new Column("GLOID", LONG, "ID операции", 70));
        result.addColumn(new Column("PST_REF", LONG, "ID запроса", 70, false, false));
        result.addColumn(new Column("ID_PST", STRING, "ИД сообщ АЕ", 70));
        result.addColumn(col = new Column("INP_METHOD", STRING, "Способ ввода", 70, false, false));
        col.setList(getEnumLabelsList(InputMethod.values()));

        result.addColumn(new Column("SRC_PST", STRING, "Источник сделки", 70));
        result.addColumn(new Column("DEAL_ID", STRING, "ИД сделки", 120));
        result.addColumn(new Column("SUBDEALID", STRING, "ИД субсделки", 180, false, false));
        result.addColumn(new Column("PMT_REF", STRING, "ИД платежа", 120));

        result.addColumn(col = new Column("STATE", STRING, "Статус", 70, false, false));
        col.setList(getEnumValuesList(OperState.values()));

        result.addColumn(col = new Column("PST_SCHEME", STRING, "Схема проводок", 70, false, false));
        col.setList(getEnumLabelsList(OperType.values()));

        result.addColumn(col = new Column("MNL_STATUS", STRING, "Статус руч. обработки", 120));  //ext
        col.setList(GLComponents.getArrayValuesList(new String[]{"", "COMPLETED", "CONTROL", "HOLD", "SIGNEDDATE"}));
        result.addColumn(new Column("MNL_RSNCODE", STRING, "Причина руч. обработки", 120));  //ext

        result.addColumn(procDate = new Column("PROCDATE", DATE, "Дата опердня", 80));
        procDate.setFormat("dd.MM.yyyy");
        result.addColumn(vDate = new Column("VDATE", DATE, "Дата валютирования", 80));
        vDate.setFormat("dd.MM.yyyy");
        result.addColumn(col = new Column("POSTDATE", DATE, "Дата проводки", 80));
        col.setFormat("dd.MM.yyyy");
        result.addColumn(col = new Column("POSTDATE_PLAN", DATE, "Плановая дата проводки", 80));  //ext
        col.setFormat("dd.MM.yyyy");
        result.addColumn(new Column("PDATE_CHNG", STRING, "Дата проводки изменена", 100, false, false)); //ext

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

        result.addColumn(new Column("OTS_AUTO", Column.Type.DATETIME, "Время авт. обновления", 130, false, false));  //ext
        result.addColumn(col = new Column("BV_CUTDATE", DATE, "Пороговая дата backvalue ", 80, false, false));  //ext
        col.setFormat("dd.MM.yyyy");
        result.addColumn(col = new Column("PRD_LDATE", DATE, "Закрытый период", 80, false, false));  //ext
        col.setFormat("dd.MM.yyyy");
        result.addColumn(col = new Column("PRD_CUTDATE", DATE, "Дата закрытия периода", 80, false, false));  //ext
        col.setFormat("dd.MM.yyyy");
        result.addColumn(new Column("MNL_NRT", STRING, "Комментарий руч. обработки", 300, false, false));  //ext

        result.addColumn(col = new Column("AUTHOR", STRING, "Исполнитель", 180));  //ext
        col.setFilterable(false);
        result.addColumn(new Column("USER_AU3", STRING, "Логин исполнителя", 180, false, false));  //ext
        result.addColumn(new Column("OTS_AU3", Column.Type.DATETIME, "Время руч. обработки", 130, false, false));  //ext
        result.addColumn(new Column("EMSG", STRING, "Сообщение об ошибке", 1000, false, false));

        return result;
    }

    @Override
    protected String prepareMasterSql() {
        return null;
    }

    @Override
    protected Table prepareDetailTable() {
        Table result = new Table();
        Column col;

        HashMap<Serializable, String> yesNoList = getYesNoList();
        result.addColumn(detailGloid = new Column("PAR_GLO", LONG, "ID операции", 70));
        result.addColumn(new Column("GLOID", LONG, "ID операции", 70, false, false));
        result.addColumn(col = new Column("INP_METHOD", STRING, "Способ ввода", 50, false, false));
        col.setList(getEnumLabelsList(InputMethod.values()));

        result.addColumn(col = new Column("POST_TYPE", STRING, "Тип проводки", 40, false, false));
        col.setList(getPostingTypeList());
        result.addColumn(new Column("PCID", LONG, "ID проводки", 80));
        result.addColumn(new Column("ID_DR", LONG, "ID полупроводки ДБ", 80, false, false));
        result.addColumn(new Column("ID_CR", LONG, "ID полупроводки КР", 80, false, false));

        result.addColumn(new Column("MO_NO", STRING, "Мем.ордер", 120));
        result.addColumn(col = new Column("INVISIBLE", STRING, "Отменена", 40));
        col.setList(yesNoList);
        result.addColumn(new Column("SRC_PST", STRING, "Источник сделки", 60, false, false));
        result.addColumn(new Column("PBR", STRING, "Система-источник", 80, false, false));
        result.addColumn(new Column("DEAL_ID", STRING, "ИД сделки", 120, false, false));
        result.addColumn(new Column("SUBDEALID", STRING, "ИД субсделки", 120, false, false));
        result.addColumn(new Column("PMT_REF", STRING, "ИД платежа", 120, false, false));
        result.addColumn(new Column("PREF", STRING, "ИД сделки/ платежа", 120));

        result.addColumn(new Column("PROCDATE", DATE, "Дата создания", 80, false, false));
        result.addColumn(new Column("VDATE", DATE, "Дата валютирования", 80, false, false));
        result.addColumn(new Column("POSTDATE", DATE, "Дата проводки", 80));

        result.addColumn(new Column("ACID_DR", STRING, "Счет Midas ДБ", 160));
        result.addColumn(new Column("BSAACID_DR", STRING, "Счет ДБ", 160));
        result.addColumn(new Column("CBCC_DR", STRING, "Филиал ДБ", 60, false, false));
        result.addColumn(new Column("CCY_DR", STRING, "Валюта ДБ", 60));
        result.addColumn(new Column("AMT_DR", DECIMAL, "Сумма ДБ", 100));
        result.addColumn(new Column("AMTRU_DR", DECIMAL, "Сумма в руб. ДБ", 100));

        result.addColumn(new Column("ACID_CR", STRING, "Счет Midas КР", 160));
        result.addColumn(new Column("BSAACID_CR", STRING, "Счет КР", 160));
        result.addColumn(new Column("CBCC_CR", STRING, "Филиал КР", 60, false, false));
        result.addColumn(new Column("CCY_CR", STRING, "Валюта КР", 60));
        result.addColumn(new Column("AMT_CR", DECIMAL, "Сумма КР", 100));
        result.addColumn(new Column("AMTRU_CR", DECIMAL, "Сумма в руб. КР", 100));
        result.addColumn(new Column("AMTRU", DECIMAL, "Сумма в руб.", 100, false, false));

        result.addColumn(new Column("PNAR", STRING, "Описание", 180));
        result.addColumn(new Column("NRT", STRING, "Основание ENG", 500, false, false));
        result.addColumn(new Column("RNARLNG", STRING, "Основание RUS", 500));
        result.addColumn(new Column("RNARSHT", STRING, "Основание короткое", 200, false, false));

        result.addColumn(col = new Column("STRN", STRING, "Сторно", 40));
        col.setList(yesNoList);
        result.addColumn(new Column("STRN_GLO", LONG, "Сторно операция", 70, false, false));
        result.addColumn(col = new Column("FCHNG", STRING, "Исправительная", 40));
        col.setList(yesNoList);
        result.addColumn(new Column("PRFCNTR", STRING, "Профит центр", 60));
        result.addColumn(new Column("DPMT", STRING, "Подразделение", 60));

        result.addColumn(col = new Column("FAN", STRING, "Веер", 40));
        col.setList(yesNoList);
        result.addColumn(new Column("FAN_TYPE", STRING, "Тип веерной проводки", 40, false, false));
        result.addColumn(new Column("GLO_DR", LONG, "ID операции ДБ", 70, false, false));
        result.addColumn(new Column("GLO_CR", LONG, "ID операции КР", 70, false, false));
        result.addColumn(col = new Column("PDMODE", STRING, "Режим записи", 70, false, false));
        col.setList(getArrayValuesList(new String[] {"BUFFER", "DIRECT"}));
        return result;
    }

    @Override
    protected String prepareDetailSql() {
        return _detailSelect;
    }


    @Override
    public ArrayList<FilterItem> createLinkFilterCriteria(Row row) {
        ArrayList<FilterItem> list = new ArrayList<FilterItem>();
        list.add(new FilterItem(detailGloid, FilterCriteria.EQ, row == null ? -1 : row.getField(0).getValue()));

        return list;
    }

/*
    @Override
    protected ArrayList<SortItem> getInitialMasterSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("GLOID", Column.Sort.DESC));
        return list;
    }
*/

/*
    @Override
    protected ArrayList<SortItem> getInitialDetailSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("POST_TYPE", Column.Sort.ASC));
        return list;
    }
*/

    private ManualOperationWrapper masterRowToWrapper(){
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
        wrapper.setAmountRuCredit((BigDecimal) getValue("AMTRU_CR"));
        wrapper.setCorrection("Y".equals(((String) getValue("FCHNG"))));

        wrapper.setNarrative((String) getValue("NRT"));
        wrapper.setRusNarrativeLong((String) getValue("RNRTL"));

        wrapper.setDeptId((String) getValue("DEPT_ID"));
        wrapper.setProfitCenter((String) getValue("PRFCNTR"));

        String errorMessage = (String) getValue("EMSG");
        if (!isEmpty(errorMessage))
            wrapper.getErrorList().addErrorDescription(errorMessage);

        return wrapper;
    }

    private void changeFanMode(Action action, boolean detail){
        action.setImage(detail ? new Image(ImageConstants.INSTANCE.nonauthoriz()) : new Image(ImageConstants.INSTANCE.authoriz()));
        action.setHint(detail ? "Режим: Детальный" : "Режим: Сводный");
    }

    private void changeColumnVisibility(){
        masterGrid.showColumns("MNL_STATUS", "MNL_RSNCODE", "POSTDATE_PLAN", "AUTHOR");
        masterGrid.showColumns(false, true, true,"PDATE_CHNG", "OTS_AUTO",
                                "BV_CUTDATE", "PRD_LDATE", "PRD_CUTDATE", "MNL_NRT", "USER_AU3", "OTS_AU3");
        if (_mode == BVOperChoiceDlg.ModeType.STANDARD && _spec == BVOperChoiceDlg.SpecType.MANUAL)  return;

        masterGrid.hideColumns("MNL_STATUS", "MNL_RSNCODE", "POSTDATE_PLAN", "AUTHOR", "PDATE_CHNG", "OTS_AUTO",
                                              "BV_CUTDATE", "PRD_LDATE", "PRD_CUTDATE", "MNL_NRT", "USER_AU3", "OTS_AU3");
    }
}


