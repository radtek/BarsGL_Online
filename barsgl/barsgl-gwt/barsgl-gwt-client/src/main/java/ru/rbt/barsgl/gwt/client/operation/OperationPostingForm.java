package ru.rbt.barsgl.gwt.client.operation;

import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.client.gridForm.MDForm;
import ru.rbt.barsgl.gwt.client.quickFilter.DateQuickFilterAction;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.dialogs.FilterCriteria;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.ClientDateUtils;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.BatchPostStep;
import ru.rbt.barsgl.shared.enums.InputMethod;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.barsgl.shared.enums.OperType;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.*;
import static ru.rbt.barsgl.gwt.client.quickFilter.DateQuickFilterParams.DateFilterField.CREATE_DATE;
import static ru.rbt.barsgl.gwt.client.security.AuthWherePart.getSourceAndFilialPart;
import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.*;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;

/**
 * Created by ER18837 on 19.04.16.
 */
public class OperationPostingForm extends MDForm {
    public static final String FORM_NAME = "Операции";
    protected Column colProcDate;
    protected Column colPostDate;
    protected Column colValueDate;
    protected Column colGloid;

    public OperationPostingForm(){
        this(FORM_NAME, null, "Проводки по операции", true);
    }

    public OperationPostingForm(String formTitle, String masterTitle, String detailTitle, boolean delayLoad) {
        super(formTitle, masterTitle, detailTitle, delayLoad);
        reconfigure();
    }

    protected void reconfigure() {
        DateQuickFilterAction quickFilterAction;
        masterActionBar.addAction(quickFilterAction = new DateQuickFilterAction(masterGrid, colProcDate, colValueDate, colPostDate, CREATE_DATE, false));
        masterActionBar.addAction(new SimpleDlgAction(masterGrid, DlgMode.BROWSE, 10));
        masterActionBar.addAction(createPreview());

        detailActionBar.addAction(new SimpleDlgAction(detailGrid, DlgMode.BROWSE, 10));
        quickFilterAction.execute();
    }

    @Override
    protected String prepareMasterSql() {
        return "select * from V_GL_OPERCUST " + getSourceAndFilialPart("where", "SRC_PST", "CBCC_CR", "CBCC_DR");
    }

    @Override
    protected ArrayList<SortItem> getInitialMasterSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("GLOID", Column.Sort.DESC));
        return list;
    }

    @Override
    protected Table prepareMasterTable() {
        Table result = new Table();
        Column col;

        HashMap<Serializable, String> yesNoList = getYesNoList();
        result.addColumn(new Column("GLOID", LONG, "ID операции", 70));// No Space
        result.addColumn(new Column("PST_REF", LONG, "ID запроса", 70, false, false));
        result.addColumn(new Column("ID_PST", STRING, "ИД сообщ АЕ", 70));
        result.addColumn(col = new Column("INP_METHOD", STRING, "Способ ввода", 70));
        col.setList(getEnumLabelsList(InputMethod.values()));
        result.addColumn(new Column("SRC_PST", STRING, "Источник сделки", 70));
        result.addColumn(new Column("DEAL_ID", STRING, "ИД сделки", 120));
        result.addColumn(new Column("SUBDEALID", STRING, "ИД субсделки", 120, false, false));
        result.addColumn(new Column("PMT_REF", STRING, "ИД платежа", 70));
        result.addColumn(col = new Column("STATE", STRING, "Статус", 70));
        col.setList(getEnumValuesList(OperState.values()));
        result.addColumn(col = new Column("PST_SCHEME", STRING, "Схема проводок", 70));
        col.setList(getEnumLabelsList(OperType.values()));

        result.addColumn(colProcDate = new Column("PROCDATE", DATE, "Дата опердня", 80));
        colProcDate.setFormat("dd.MM.yyyy");
        result.addColumn(colValueDate = new Column("VDATE", DATE, "Дата валютирования", 80));
        colValueDate.setFormat("dd.MM.yyyy");
        result.addColumn(colPostDate = new Column("POSTDATE", DATE, "Дата проводки", 80));
        colPostDate.setFormat("dd.MM.yyyy");

        result.addColumn(new Column("AC_DR", STRING, "Счет ДБ", 160));
        result.addColumn(new Column("CCY_DR", STRING, "Валюта ДБ", 60));
        result.addColumn(new Column("AMT_DR", DECIMAL, "Сумма ДБ", 100));
        result.addColumn(new Column("AMTRU_DR", DECIMAL, "Сумма в рублях ДБ", 100, false, false));
        result.addColumn(new Column("RATE_DR", DECIMAL, "Курс ДБ", 100, false, false));
        result.addColumn(new Column("EQV_DR", DECIMAL, "Руб.экв. ДБ", 100, false, false));
        result.addColumn(new Column("CBCC_DR", STRING, "Филиал ДБ", 60));

        result.addColumn(new Column("AC_CR", STRING, "Счет КР", 160));
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

        return result;
    }

    @Override
    protected String prepareDetailSql() {
        return "select * from V_GL_PDLINK ";
    }

    @Override
    protected ArrayList<SortItem> getInitialDetailSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("POST_TYPE", Column.Sort.ASC));
        return list;
    }

    @Override
    protected Table prepareDetailTable() {
        Table result = new Table();
        Column col;

        HashMap<Serializable, String> yesNoList = getYesNoList();
        result.addColumn(colGloid = new Column("PAR_GLO", LONG, "ID операции", 70));
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
    public ArrayList<FilterItem> createLinkFilterCriteria(Row row) {
        ArrayList<FilterItem> list = new ArrayList<FilterItem>();
        list.add(new FilterItem(colGloid, FilterCriteria.EQ, row == null ? -1 : row.getField(0).getValue()));

        return list;
    }

    protected GridAction createPreview(){
        return new GridAction(masterGrid, null, "Просмотр", new Image(ImageConstants.INSTANCE.preview()), 10, true) {
            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;

                OperationDlg dlg = new OperationHandsViewDlg("Просмотр бухгалтерской операции GL", FormAction.PREVIEW, masterTable.getColumns(), BatchPostStep.NOHAND);
                dlg.setDlgEvents(this);
                dlg.show(rowToWrapper());
            }
        };
    }

    protected ManualOperationWrapper rowToWrapper(){
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
            wrapper.getErrorList().addErrorDescription(null, null, errorMessage, null);

        return wrapper;
    }
    
    private GridAction createReprocessOperation() {
        return new NewOperationAction(masterGrid, ImageConstants.INSTANCE.oper_go()) {
            @Override
            protected Object getParams() {
                Row row = grid.getCurrentRow();
                if (row == null) return null;

                ManualOperationWrapper wrapper = new ManualOperationWrapper();

                wrapper.setDealSrc(masterGrid.getFieldText("SRC_PST"));
                wrapper.setDealId(masterGrid.getFieldText("DEAL_ID"));
                wrapper.setSubdealId(masterGrid.getFieldText("SUBDEALID"));
                wrapper.setPostDateStr(ClientDateUtils.Date2String((Date)masterGrid.getFieldValue("POSTDATE")));
                wrapper.setValueDateStr(ClientDateUtils.Date2String((Date)masterGrid.getFieldValue("VDATE")));

                wrapper.setCurrencyDebit(masterGrid.getFieldText("CCY_DR"));
                wrapper.setFilialDebit(masterGrid.getFieldText("CBCC_DR"));
                wrapper.setAccountDebit(masterGrid.getFieldText("AC_DR"));
                wrapper.setAmountDebit((BigDecimal)masterGrid.getFieldValue("AMT_DR"));

                wrapper.setCurrencyCredit(masterGrid.getFieldText("CCY_CR"));
                wrapper.setFilialCredit(masterGrid.getFieldText("CBCC_CR"));
                wrapper.setAccountCredit(masterGrid.getFieldText("AC_CR"));
                wrapper.setAmountCredit((BigDecimal)masterGrid.getFieldValue("AMT_CR"));
                wrapper.setAmountRu((BigDecimal)masterGrid.getFieldValue("AMTRU_DR"));

                wrapper.setNarrative(masterGrid.getFieldText("NRT"));
                wrapper.setRusNarrativeLong(masterGrid.getFieldText("RNRTL"));
                wrapper.setDeptId(masterGrid.getFieldText("DEPT_ID"));
                wrapper.setProfitCenter(masterGrid.getFieldText("PRFCNTR"));
                wrapper.setCorrection(("Y").equals(masterGrid.getFieldText("FCHNG")));
                wrapper.setInputMethod(InputMethod.valueOf(masterGrid.getFieldText("INP_METHOD")));
                return wrapper;
            }        	
        };
    }

}
