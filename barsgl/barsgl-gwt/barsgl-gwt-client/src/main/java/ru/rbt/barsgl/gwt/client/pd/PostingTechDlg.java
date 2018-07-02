package ru.rbt.barsgl.gwt.client.pd;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.web.bindery.event.shared.HandlerRegistration;
import ru.rbt.barsgl.gwt.client.check.CheckNotEmptyString;
import ru.rbt.barsgl.gwt.client.check.CheckNotNullDate;
import ru.rbt.barsgl.gwt.client.comp.CachedListEnum;
import ru.rbt.barsgl.gwt.client.comp.DataListBox;
import ru.rbt.barsgl.gwt.client.comp.DataListBoxEx;
import ru.rbt.barsgl.gwt.client.comp.ICallMethod;
import ru.rbt.barsgl.gwt.client.dict.dlg.EditableDialog;
import ru.rbt.barsgl.gwt.client.dictionary.AccTechFormDlg;
import ru.rbt.barsgl.gwt.client.dictionary.AccountTypeTechFormDlg;
import ru.rbt.barsgl.gwt.client.gridForm.GridFormDlgBase;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.SecurityChecker;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.events.DataListBoxEvent;
import ru.rbt.barsgl.gwt.core.events.DataListBoxEventHandler;
import ru.rbt.barsgl.gwt.core.events.LocalEventBus;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.ui.*;
import ru.rbt.barsgl.shared.ClientDateUtils;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.InputMethod;
import ru.rbt.barsgl.shared.enums.PostingChoice;
import ru.rbt.barsgl.shared.operation.ManualTechOperationWrapper;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;
import ru.rbt.security.gwt.client.operday.IDataConsumer;
import ru.rbt.security.gwt.client.operday.OperDayGetter;
import ru.rbt.shared.user.AppUserWrapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.*;
import static ru.rbt.barsgl.gwt.core.comp.Components.*;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.check;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.ifEmpty;
import static ru.rbt.security.gwt.client.operday.OperDayGetter.getOperday;
import static ru.rbt.shared.enums.SecurityActionCode.TechOperPstChng;
import static ru.rbt.shared.enums.SecurityActionCode.TechOperPstChngDate;

/**
 * Created by ER18837 on 05.04.16.
 */
public class PostingTechDlg extends EditableDialog<ManualTechOperationWrapper> {

    private final String LABEL_WIDTH = "130px";
    private final String LABEL2_WIDTH = "85px";
    private final String FIELD2_WIDTH = "80px";

    private final String BUTTON_WIDTH = "75px";
    private final String LABELS_WIDTH = "80px";
    private final String FIELDS_WIDTH = "185px";
    private final String SUM_WIDTH = "145px";
    private final String LABEL_DEP_WIDTH = "110px";
    private final String LONG_DEP_WIDTH = "400px";
    private final String TEXT_WIDTH = "80px";

    public enum Side {DEBIT, CREDIT};

    private Long idOperation;
    protected String inputMethod;
    private boolean isFan;
    private boolean isStorno;
    private boolean isInvisible;
    private DatePickerBox mDateValue;
    protected TxtBox mDateOperDay;
    protected Date operday;
    private DataListBox mDealSource;
    private TxtBox mDealId;
    private TxtBox mSubDealId;
    private DatePickerBox mDateOperation;
    private TxtBox mDtAccountType;
    private TxtBox mCrAccountType;
    private AreaBox mNarrativeRU;
    private AreaBox mNarrativeEN;
    private DataListBox mDepartment;
    private DataListBox mProfitCenter;
    private DataListBoxEx mDtCurrency;
    private DataListBoxEx mDtFilial;
    private TxtBox mDtAccount;
    private DecBox mDtSum;
    private DataListBoxEx mCrCurrency;
    private DataListBoxEx mCrFilial;
    private TxtBox mCrAccount;
    private DecBox mCrSum;
    private CheckBox mCheckCorrection;
    protected String _reasonOfDeny;

    private Boolean isAsyncListsCached;
    private HandlerRegistration registration;
    private Timer timer;

    private int asyncListCount = 7; /*count async lists:  mDtCurrency; mCrCurrency; mDtFilial; mCrFilial;
                                                          mDepartment; mProfitCenter; mDealSource*/

    @Override
    public void beforeCreateContent(){
        isAsyncListsCached = (Boolean) LocalDataStorage.getParam("isAsyncListsCached");
        if (isAsyncListsCached != null && isAsyncListsCached) return;
        registration =  LocalEventBus.addHandler(DataListBoxEvent.TYPE, dataListBoxCreatedEventHandler());
        //save in local storage sign that async list is already cached
        LocalDataStorage.putParam("isAsyncListsCached", true);
    }

    private DataListBoxEventHandler dataListBoxCreatedEventHandler() {
        return new DataListBoxEventHandler(){

            @Override
            public void completeLoadData(String dataListBoxId) {
                //Вызывается после заполнения списка значениями
                asyncListCount--;

                if (asyncListCount == 0) {
                    registration.removeHandler();
                }
            }
        };
    }

    public PostingTechDlg(String title, FormAction action, Columns columns) {
        super(columns, action);
        setCaption(title);
        if (action == FormAction.PREVIEW){
            ok.setVisible(false);
        }
    }


    private DataListBoxEx createFilialListBox(String name, String filial, String width) {
        return createCachedFilialListBox(name, filial, width, false, true);
    }

    protected void getOperDay() {
        getOperday(new IDataConsumer<OperDayWrapper>() {
            @Override
            public void accept(OperDayWrapper wrapper) {
                String operDayStr = wrapper.getCurrentOD();
                mDateOperDay.setValue(operDayStr);
                operday = DateTimeFormat.getFormat(OperDayGetter.dateFormat).parse(operDayStr);
            }
        });
    }

    @Override
    public Widget createContent() {
        VerticalPanel mainVP = new VerticalPanel();
        mainVP.setSpacing(15);

        mainVP.add(createHeader());

        HorizontalPanel hp3 = new HorizontalPanel();
        hp3.setSpacing(0);
        hp3.add(createOneSide("Дебет", PostingTechDlg.Side.DEBIT));
        hp3.add(createOneSide("Кредит", PostingTechDlg.Side.CREDIT));
        mainVP.add(hp3);

        Grid grid = new Grid(2,2);
        grid.setWidget(0,1,mCheckCorrection = new CheckBox("исправительная проводка"));
        mainVP.add(grid);

        mainVP.add(createDescriptions());
        mainVP.add(createDepartments(false));

        mDateOperDay.setReadOnly(true);

        return mainVP;
    }

    private Grid createDescriptions() {
        Grid grid = new Grid(2,2);
        grid.setWidget(0, 0, createLabel("Основание ENG", LABEL_DEP_WIDTH));
        grid.setWidget(0, 1, mNarrativeEN = createAreaBox(LONG_DEP_WIDTH, "50px"));
        grid.setWidget(1, 0, new Label("Основание RUS"));
        grid.setWidget(1, 1, mNarrativeRU = createAreaBox(LONG_DEP_WIDTH, "50px"));
        return grid;
    }

    private Grid createDepartments(boolean withCheck) {
        Grid grid = new Grid(2,4);
        grid.setWidget(0, 0, createLabel("Подразделение", LABEL_DEP_WIDTH));
        grid.setWidget(0, 1, mDepartment = createCachedDepartmentListBox(CachedListEnum.Department.name(), null, "250px", true));
        grid.setWidget(1, 0, createLabel("Профит центр"));
        grid.setWidget(1, 1, createAlignWidget(mProfitCenter = createCachedProfitCenterListBox(CachedListEnum.ProfitCenter.name(), null, "250px"), "260px"));
        if (withCheck)
            grid.setWidget(1, 2, new CheckBox("Основание проверено"));
        return grid;
    }

    private Grid createOneSide(String label, final Side side) {
        DataListBoxEx mCurrency;
        DataListBoxEx mFilial;
        TxtBox mAccount;
        TxtBox mAccountType;

        boolean isDebit = side.equals(Side.DEBIT);
        Grid grid = new Grid( 6, 2);

        grid.setWidget(0, 0, createAlignWidget(new HTML("<b>" + label + "</b>"), LABELS_WIDTH));
        grid.setWidget(1, 0, createLabel("Валюта"));
        grid.setWidget(1, 1, mCurrency = createCachedCurrencyListBox(CachedListEnum.Currency.name() + "_" + label,  "RUR", FIELD2_WIDTH, false, false));
        grid.setWidget(2, 0, createLabel(("Филиал")));
        grid.setWidget(2, 1, mFilial = createFilialListBox(CachedListEnum.Filials.name() + "_" + label /*+ "_Digit"*/, null, FIELD2_WIDTH));
        if (isDebit) {
            grid.setWidget(3, 0, createDtAccountTypeButton("AccType", BUTTON_WIDTH));
        }
        else{
            grid.setWidget(3, 0, createCrAccountTypeButton("AccType", BUTTON_WIDTH));
        }
        grid.setWidget(3, 1, mAccountType = createTxtIntBox(9, TEXT_WIDTH));

        grid.setWidget(4, 0, createLabel("Счет"));
        if (side.equals(Side.DEBIT))
            grid.setWidget(4, 1, createAlignWidget(mAccount = createTxtBox(20, SUM_WIDTH), FIELDS_WIDTH));
        else
            grid.setWidget(4, 1, mAccount = createTxtBox(20, SUM_WIDTH));
        mAccount.setEnabled(false);
        mAccount.setName(side.name());

        grid.setWidget(5, 0, createLabel("Сумма"));

        DecBox mSum;
        grid.setWidget(5,1, mSum = createDecBoxForSumma(20, SUM_WIDTH));

        if (isDebit) {
            mDtAccount = mAccount;
            mDtCurrency = mCurrency;
            mDtFilial = mFilial;
            mDtSum = mSum;
            mDtAccountType = mAccountType;
        } else {
            mCrAccount = mAccount;
            mCrCurrency = mCurrency;
            mCrFilial = mFilial;
            mCrSum = mSum;
            mCrAccountType = mAccountType;
        }
        return grid;
    }

    private Button createDtAccountTypeButton(String text, String width) {
        Button btn = new Button();
        btn.setText(text);
        btn.addStyleName("dlg-button");
        btn.setWidth(width);
        btn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                try {
                    GridFormDlgBase dlg = new AccTechFormDlg() {

                        @Override
                        protected Object[] getInitialFilterParams() {

                            return new Object[]{null, null, mDtAccountType.getValue(),
                                    mDtFilial.getParam("CBCC").toString(), mDtCurrency.getParam("CCY").toString(),
                                    mDtAccount.getValue()};

                        }

                        @Override
                        protected boolean getEditMode()
                        {
                            if (action == FormAction.PREVIEW || !mDtAccount.isEnabled()) {
                                return false;
                            }
                            else {
                                return true;
                            }
                        }

                        @Override
                        protected boolean setResultList(HashMap<String, Object> result) {
                            String acctype = Utils.fillUp(result.get("ACCTYPE").toString(),9);
                            mDtAccountType.setValue(acctype);
                            return true;
                        }
                    };
                    dlg.setModal(true);
                    dlg.show();
                } catch (Exception e) {
                    Window.alert(e.getMessage());
                }
            }
        });
        return btn;
    }

    private Button createCrAccountTypeButton(String text, String width) {
        Button btn = new Button();
        btn.setText(text);
        btn.addStyleName("dlg-button");
        btn.setWidth(width);
        btn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                try {
                    GridFormDlgBase dlg = new AccTechFormDlg() {

                        @Override
                        protected Object[] getInitialFilterParams()
                        {
                            return new Object[]{null, null, mCrAccountType.getValue(),
                                    mCrFilial.getParam("CBCC").toString(), mCrCurrency.getParam("CCY").toString(),
                                    mCrAccount.getValue()};
                        }

                        @Override
                        protected boolean getEditMode()
                        {
                            if (action == FormAction.PREVIEW || !mCrAccountType.isEnabled()) {
                                return false;
                            }
                            else {
                                return true;
                            }
                        }

                        @Override
                        protected boolean setResultList(HashMap<String, Object> result) {
                            String acctype = Utils.fillUp(result.get("ACCTYPE").toString(),9);
                            mCrAccountType.setValue(acctype);
                            return true;
                        }
                    };
                    dlg.setModal(true);
                    dlg.show();
                } catch (Exception e) {
                    Window.alert(e.getMessage());
                }
            }
        });
        return btn;
    }

    private Grid createHeader() {
        Grid grid = new Grid(3,4);

        grid.setWidget(0, 0, createLabel("Текущий опердень", LABEL_WIDTH));
        grid.setWidget(0, 1, createAlignWidget(mDateOperDay = createTxtBox(10), "142px"));
        mDateOperDay.setEnabled(false);
        grid.setWidget(1, 0, createLabel("Дата проводки"));
        grid.setWidget(1, 1, mDateOperation = createDateBox());
        grid.setWidget(2, 0, createLabel("Дата валютирования"));
        grid.setWidget(2, 1, mDateValue = createDateBox());

        grid.setWidget(0, 2, createAlignWidget(createLabel("Источник сделки"), LABEL2_WIDTH));
        grid.setWidget(0, 3, mDealSource =  createCachedDealSourceAuthListBox(CachedListEnum.AuthDealSources.name(),null, FIELD2_WIDTH));
        grid.setWidget(1, 2, createAlignWidget(createLabel("N сделки/ платежа"), LABEL2_WIDTH));
        grid.setWidget(1, 3, mDealId = createTxtBox(20, SUM_WIDTH));
        grid.setWidget(2, 2, createAlignWidget(createLabel("N субсделки"), LABEL2_WIDTH));
        grid.setWidget(2, 3, mSubDealId = createTxtBox(20, SUM_WIDTH));

        return grid;
    }

    private void fillUp(){
    	if (null == params)
    		return;
        row = (Row) params;
        
        getOperDay();

        mDealSource.setSelectValue(getFieldText("SRC_PST"));
        String dealId = getFieldText("DEAL_ID");
        String paymentRef = getFieldText("PMT_REF");
        mDealId.setValue(!dealId.isEmpty() ? dealId : paymentRef);
        mSubDealId.setValue(getFieldText("SUBDEALID"));

        mDateValue.setValue((Date)getFieldValue("VALD"));
        mDateOperation.setValue((Date)getFieldValue("POD"));

        mDtCurrency.setSelectValue(getFieldText("CCY_DR"));
        mDtFilial.setSelectValue(getFieldText("FILIAL_DR"));

        String accDtType = Utils.fillUp(getFieldText("ACCTYPE_DR"),9);
        mDtAccountType.setValue(accDtType);
        mDtAccount.setValue(getFieldText("BSAACID_DR"));

        mDtSum.setValue(new BigDecimal(ifEmpty(getFieldValue("AMNT_DR"), "")));

        mCrCurrency.setSelectValue(getFieldText("CCY_CR"));
        mCrFilial.setSelectValue(getFieldText("FILIAL_CR"));

        String accCrType = Utils.fillUp(getFieldText("ACCTYPE_CR"),9);
        mCrAccountType.setValue(accCrType);
        mCrAccount.setValue(getFieldText("BSAACID_CR"));
        mCrSum.setValue(new BigDecimal(ifEmpty(getFieldValue("AMNT_CR"), "")));

        mNarrativeEN.setValue(getFieldText("NRT"));
        mNarrativeRU.setValue(getFieldText("RNARLNG"));
        mDepartment.setSelectValue(getFieldText("DEPT_ID"));
        mProfitCenter.setSelectValue(getFieldText("PRFCNTR"));

        idOperation = getFieldValue("GLO_REF");

        isStorno = "Y".equals(getFieldText("STRN"));

        mCheckCorrection.setValue("Y".equals(getFieldText("FCHNG")));
        mCheckCorrection.setVisible(true);

        enableAll(false);;

        isFan = "Y".equals(getFieldText("FAN"));
        inputMethod = getFieldText("INP_METHOD");
        if (action == FormAction.UPDATE) {
	        setEnabled(!"AE".equals(inputMethod));
            ok.setText("Сохранить");
        } else if (action == FormAction.OTHER) {
            isInvisible = "Y".equals(getFieldText("INVISIBLE"));
            ok.setText(isInvisible ? "Восстановить" : "Подавить");
        }
    }

    protected void setEnabled(boolean isManual) {
        if (SecurityChecker.checkActions(TechOperPstChngDate)) {
            mDateValue.setEnabled(isManual);
            mDateOperation.setEnabled(true);
        }

        if (SecurityChecker.checkAction(TechOperPstChng)) {
            mDealId.setEnabled(isManual);
            mSubDealId.setEnabled(isManual);
            mNarrativeEN.setEnabled(true);
            mNarrativeRU.setEnabled(true);
            mProfitCenter.setEnabled(true);
            mCheckCorrection.setEnabled(isManual);
        }
    }

    protected void setControlsEnabled(){};

    protected void setControlsDisabled()
    {
        enableAll(false);
    }

    private void enableAll(boolean isEnabled) {
        mDealSource.setEnabled(isEnabled);

        mDateOperation.setEnabled(isEnabled);
        mDateValue.setEnabled(isEnabled);

        mDtCurrency.setEnabled(isEnabled);
        mDtFilial.setEnabled(isEnabled);
        mDtAccount.setEnabled(isEnabled);
        mDtSum.setEnabled(isEnabled);
        mDtAccountType.setEnabled(isEnabled);

        mCrCurrency.setEnabled(isEnabled);
        mCrFilial.setEnabled(isEnabled);
        mCrAccount.setEnabled(isEnabled);
        mCrSum.setEnabled(isEnabled);
        mCrAccountType.setEnabled(isEnabled);
        mDepartment.setEnabled(isEnabled);

        mNarrativeEN.setEnabled(isEnabled);
        mNarrativeRU.setEnabled(isEnabled);
        mProfitCenter.setEnabled(isEnabled);

        mDealId.setEnabled(isEnabled);
        mSubDealId.setEnabled(isEnabled);
        mCheckCorrection.setEnabled(isEnabled);
    }

    @Override
    protected boolean onClickOK() throws Exception {
        boolean res = super.onClickOK();
        if (!res)
            return false;
        else
            return true;
    }

    @Override
    protected ManualTechOperationWrapper createWrapper() {
        return new ManualTechOperationWrapper();
    }

    protected void setFields(ManualTechOperationWrapper operation) {
        operation.setId(idOperation);
        operation.setDealSrc(mDealSource.getText());
        operation.setDealId(mDealId.getValue());
        operation.setSubdealId(mSubDealId.getValue());
        operation.setPaymentRefernce(mDealId.getValue());

        check(mDateOperation.getValue(), "Дата проводки", "поле не заполнено", new CheckNotNullDate());
        check(mDateValue.getValue(), "Дата валютирования", "поле не заполнено", new CheckNotNullDate());
        operation.setPostDateStr(DateTimeFormat.getFormat(operation.dateFormat).format(mDateOperation.getValue()));
        operation.setValueDateStr(DateTimeFormat.getFormat(operation.dateFormat).format(mDateValue.getValue()));

        operation.setFilialCredit(mCrFilial.getParam("CBCC").toString());
        operation.setFilialDebit(mDtFilial.getParam("CBCC").toString());
        operation.setAccountCredit(mCrAccount.getValue());
        operation.setAccountDebit(mDtAccount.getValue());
        operation.setAccountTypeDebit(mDtAccountType.getValue());
        operation.setAccountTypeCredit(mCrAccountType.getValue());
        operation.setCurrencyCredit(mCrCurrency.getParam("CCY").toString());
        operation.setCurrencyDebit(mDtCurrency.getParam("CCY").toString());
        operation.setAmountDebit(mDtSum.getValue());
        operation.setAmountCredit(mCrSum.getValue());

        ArrayList<Long> pdList = new ArrayList<Long>();
        operation.setPdIdList(pdList);

        operation.setNarrative(mNarrativeEN.getValue());
        operation.setRusNarrativeLong(check(mNarrativeRU.getValue()
                , "Основание RUS", "поле не заполнено", new CheckNotEmptyString()));
        operation.setDeptId((String) mDepartment.getValue());
        operation.setProfitCenter((String) mProfitCenter.getValue());
        operation.setCorrection(mCheckCorrection.getValue());
        operation.setStorno(isStorno);
        operation.setFan(isFan);

        if (InputMethod.M.getLabel().equalsIgnoreCase(inputMethod))
        {
            operation.setInputMethod(InputMethod.M);
        }
        else if (InputMethod.AE.getLabel().equalsIgnoreCase(inputMethod))
        {
            operation.setInputMethod(InputMethod.AE);
        }
        else if (InputMethod.F.getLabel().equalsIgnoreCase(inputMethod))
        {
            operation.setInputMethod(InputMethod.F);
        }
        else {
            operation.setInputMethod(InputMethod.M);
        }
        operation.setPdMode("");
        operation.setInvisible(action == FormAction.OTHER ? !isInvisible : isInvisible);

        // Для проверки прав по филиалам
        AppUserWrapper wrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
        operation.setUserId(wrapper.getId());
        operation.setPostDateStr(ClientDateUtils.Date2String(mDateOperation.getValue()));
    }

    @Override
    protected void fillContent() {
        if (isAsyncListsCached != null && isAsyncListsCached){
            //если закэшировано
            fillUp();
            return;
        }

        if (asyncListCount == 0) {
            //если закончена обработка списков
            fillUp();
        } else {
            showPreload(true);
            timer = new Timer() {
                @Override
                public void run() {
                    if (asyncListCount == 0) {
                        timer.cancel();
                        fillUp();
                        showPreload(false);
                    }
                }
            };

            timer.scheduleRepeating(100);
        }
    }
}

