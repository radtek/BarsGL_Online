package ru.rbt.barsgl.gwt.client.pd;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.client.check.CheckNotEmptyString;
import ru.rbt.barsgl.gwt.client.check.CheckNotNullDate;
import ru.rbt.barsgl.gwt.client.comp.CachedListEnum;
import ru.rbt.barsgl.gwt.client.comp.DataListBox;
import ru.rbt.barsgl.gwt.client.comp.DataListBoxEx;
import ru.rbt.barsgl.gwt.client.comp.ICallMethod;
import ru.rbt.barsgl.gwt.client.dict.dlg.EditableDialog;
import ru.rbt.barsgl.gwt.client.dictionary.AccountTypeTechFormDlg;
import ru.rbt.barsgl.gwt.client.gridForm.GridFormDlgBase;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.events.DataListBoxEventHandler;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.ui.AreaBox;
import ru.rbt.barsgl.gwt.core.ui.BtnTxtBox;
import ru.rbt.barsgl.gwt.core.ui.DatePickerBox;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
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
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.check;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.ifEmpty;
import static ru.rbt.security.gwt.client.operday.OperDayGetter.getOperday;

/**
 * Created by ER18837 on 05.04.16.
 */
public class PostingTechViewDlg extends EditableDialog<ManualTechOperationWrapper> {

    protected final String LABEL_WIDTH = "130px";
    protected final String LABEL2_WIDTH = "85px";
    protected final String FIELD2_WIDTH = "80px";

    protected final String LONG_WIDTH = "380px";
    protected final String BUTTON_WIDTH = "75px";
    protected final String LABELS_WIDTH = "80px";
    protected final String FIELDS_WIDTH = "185px";
    protected final String SUM_WIDTH = "145px";
    protected final String LABEL_DEP_WIDTH = "110px";
    protected final String LONG_DEP_WIDTH = "400px";
    protected final String TEXT_WIDTH = "80px";

    public enum Side {DEBIT, CREDIT};

    protected PostingChoice postingChoice;
    protected Long idOperation;
    protected String inputMethod;
    protected String pdMode;
    protected boolean isFan;
    protected boolean isStorno;
    protected boolean isCorrection;
    protected boolean isInvisible;
    protected DatePickerBox mDateValue;
    protected TxtBox mDateOperDay;
    protected Date operday;
    protected DataListBox mDealSource;
    protected TxtBox mDealId;
    protected TxtBox mSubDealId;
    protected DatePickerBox mDateOperation;
    protected boolean isRurDebit = false;
    protected boolean isRurCredit = false;
    protected Boolean isAsyncListsCached;
    protected HandlerRegistration registration;
    protected Timer timer;
    protected TxtBox mAccountType;
    protected Button mDrAccountTypeButton;
    protected Button mCrAccountTypeButton;
    protected String accountTypeDesc = null;
    protected AreaBox mAccountDesc;
    protected TxtBox mCustomerName;
    protected TxtBox mCustomerType;
    protected DatePickerBox mDateOpen;
    protected BtnTxtBox mSum = null;
    protected TxtBox mDtAccountType;
    protected TxtBox mCrAccountType;
    protected AreaBox mNarrativeRU;
    protected AreaBox mNarrativeEN;
    protected DataListBox mDepartment;
    protected DataListBox mProfitCenter;
    protected CheckBox mCheckFields;
    protected DataListBoxEx mDtCurrency;
    protected DataListBoxEx mDtFilial;
    protected TxtBox mDtAccount;
    protected TxtBox mDtSum;
    protected DataListBoxEx mCrCurrency;
    protected DataListBoxEx mCrFilial;
    protected TxtBox mCrAccount;
    protected TxtBox mCrSum;
    protected CheckBox mCheckCorrection;

    protected String _reasonOfDeny;


    private String acc2;
    private int asyncListCount = 7; /*count async lists:  mDtCurrency; mCrCurrency; mDtFilial; mCrFilial;
                                                          mDepartment; mProfitCenter; mDealSource*/


    @Override
    public void beforeCreateContent(){
        //isAsyncListsCached = (Boolean) LocalDataStorage.getParam("isAsyncListsCached");
        //if (isAsyncListsCached != null && isAsyncListsCached) return;
        //registration =  LocalEventBus.addHandler(DataListBoxEvent.TYPE, dataListBoxCreatedEventHandler());
        //save in local storage sign that async list is already cached
        //LocalDataStorage.putParam("isAsyncListsCached", true);
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

    public PostingTechViewDlg(String title, FormAction action, Columns columns) {
        super(columns, action);
        setCaption(title);
        if (action == FormAction.PREVIEW){
            ok.setVisible(false);
        }
    }

    public PostingTechViewDlg()
    {
        super();
    }

    protected DataListBoxEx createFilialListBox(String name, String filial, String width) {
        return createCachedFilialListBox(name, filial, width, true, true);
    }

    protected Date getAccountDate() {
        return mDateValue.getValue();
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
        //Window.alert("PostingTechDlg: createContent");
        VerticalPanel mainVP = new VerticalPanel();
        mainVP.setSpacing(15);

        mainVP.add(createHeader());

        HorizontalPanel hp3 = new HorizontalPanel();
        hp3.setSpacing(0);
        hp3.add(createOneSide("Дебет", PostingTechViewDlg.Side.DEBIT, true));
        hp3.add(createOneSide("Кредит", PostingTechViewDlg.Side.CREDIT, true));
        mainVP.add(hp3);

        Grid grid = new Grid(2,2);
        grid.setWidget(0,1,mCheckCorrection = new CheckBox("исправительная проводка"));
        mainVP.add(grid);

        mainVP.add(createDescriptions());
        mainVP.add(createDepartments(false));

        mDateOperDay.setReadOnly(true);

        return mainVP;
    }

    protected Grid createDescriptions() {
        Grid grid = new Grid(2,2);
        grid.setWidget(0, 0, createLabel("Основание ENG", LABEL_DEP_WIDTH));
        grid.setWidget(0, 1, mNarrativeEN = createAreaBox(LONG_DEP_WIDTH, "50px"));
        grid.setWidget(1, 0, new Label("Основание RUS"));
        grid.setWidget(1, 1, mNarrativeRU = createAreaBox(LONG_DEP_WIDTH, "50px"));
        return grid;
    }

    protected Grid createDepartments(boolean withCheck) {
        Grid grid = new Grid(2,4);
        grid.setWidget(0, 0, createLabel("Подразделение", LABEL_DEP_WIDTH));
        grid.setWidget(0, 1, mDepartment = createCachedDepartmentListBox(CachedListEnum.Department.name(), null, "250px", true));
        grid.setWidget(1, 0, createLabel("Профит центр"));
        grid.setWidget(1, 1, createAlignWidget(mProfitCenter = createCachedProfitCenterListBox(CachedListEnum.ProfitCenter.name(), null, "250px"), "260px"));
        if (withCheck)
            grid.setWidget(1, 2, mCheckFields = new CheckBox("Основание проверено"));
        return grid;
    }

    protected Grid createOneSide(String label, final Side side, boolean withValute) {
        DataListBoxEx mCurrency;
        DataListBoxEx mFilial;
        TxtBox mAccount;
        TxtBox mAccountType = null;

        boolean isDebit = side.equals(Side.DEBIT);
        Grid grid = new Grid( 6, 2);

        grid.setWidget(0, 0, createAlignWidget(new HTML("<b>" + label + "</b>"), LABELS_WIDTH));

        grid.setWidget(1, 0, createLabel("Валюта"));
        grid.setWidget(1, 1, mCurrency = createCachedCurrencyListBox(CachedListEnum.Currency.name() + "_" + label,  "RUR", FIELD2_WIDTH, false, false));
        grid.setWidget(2, 0, createLabel(("Филиал")));
        grid.setWidget(2, 1, mFilial = createFilialListBox(CachedListEnum.Filials.name() + "_" +label, null, FIELD2_WIDTH));
        if (isDebit) {
            grid.setWidget(3, 0, mDrAccountTypeButton = createDtAccountTypeButton("AccType", BUTTON_WIDTH));
        }
        else{
            grid.setWidget(3, 0, mCrAccountTypeButton = createCrAccountTypeButton("AccType", BUTTON_WIDTH));
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

        grid.setWidget(5,1,
        mSum = createBtnTextBoxForSumma(20, SUM_WIDTH, new Image(ImageConstants.INSTANCE.coins()), "Конвертация по курсу ЦБ", new ICallMethod() {
                @Override
                public void method() {
                }
            }));

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
                    GridFormDlgBase dlg = new AccountTypeTechFormDlg() {

                        @Override
                        protected Object[] getInitialFilterParams() {
                            if (action == FormAction.PREVIEW) {
                                return new Object[]{mDtAccountType.getValue()};
                            }
                            else{
                                return new Object[]{};
                            }
                        }

                        @Override
                        protected boolean getEditMode()
                        {
                            if (action == FormAction.PREVIEW) {
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
                    GridFormDlgBase dlg = new AccountTypeTechFormDlg() {

                        @Override
                        protected Object[] getInitialFilterParams()
                        {
                            if (action == FormAction.PREVIEW) {
                                return new Object[]{mCrAccountType.getValue()};
                            }
                            else{
                                return new Object[]{};
                            }
                        }

                        @Override
                        protected boolean getEditMode()
                        {
                            if (action == FormAction.PREVIEW) {
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


    protected Grid createHeader() {
        Grid grid = new Grid(3,4);

        grid.setWidget(0, 0, createLabel("Текущий опердень", LABEL_WIDTH));
        grid.setWidget(0, 1, createAlignWidget(mDateOperDay = createTxtBox(10), "142px"));
        mDateOperDay.setEnabled(false);
        grid.setWidget(1, 0, createLabel("Дата проводки"));
        grid.setWidget(1, 1, mDateOperation = createDateBox());
        grid.setWidget(2, 0, createLabel("Дата валютирования"));
        grid.setWidget(2, 1, mDateValue = createDateBox());

        grid.setWidget(0, 2, createAlignWidget(createLabel("Источник сделки"), LABEL2_WIDTH));
        grid.setWidget(0, 3, mDealSource =  createCachedDealSourceAuthListBox(CachedListEnum.AuthDealSources.name(), null, FIELD2_WIDTH));
        grid.setWidget(1, 2, createAlignWidget(createLabel("N сделки/ платежа"), LABEL2_WIDTH));
        grid.setWidget(1, 3, mDealId = createTxtBox(20, SUM_WIDTH));
        grid.setWidget(2, 2, createAlignWidget(createLabel("N субсделки"), LABEL2_WIDTH));
        grid.setWidget(2, 3, mSubDealId = createTxtBox(20, SUM_WIDTH));

        return grid;
    }


    protected void fillUp(){

    	if (null == params)
    		return;
        row = (Row) params;

        getOperDay();

        mDealSource.setSelectValue(getFieldText("SRC_PST"));
        String dealId = getFieldText("DEAL_ID");
        String paymentRef = getFieldText("PMT_REF");
        mDealId.setValue(!dealId.isEmpty() ? dealId : paymentRef);
        mSubDealId.setValue(getFieldText("SUBDEALID"));

        mDateOperation.setValue((Date)getFieldValue("PROCDATE"));
        mDateValue.setValue((Date)getFieldValue("VALD"));
        mDateOperDay.setValue(getFieldValue("POD").toString());

        mDtCurrency.setSelectValue(getFieldText("CCY_DR"));
        mDtFilial.setSelectValue(getFieldText("FILIAL_DR"));
        String accDtType = Utils.fillUp(getFieldText("ACCTYPE_DR"),9);
        mDtAccountType.setValue(accDtType);
        mDtAccount.setValue(getFieldText("BSAACID_DR"));

        mDtSum.setValue(ifEmpty(getFieldValue("AMNT_DR"), ""));

        mCrCurrency.setSelectValue(getFieldText("CCY_CR"));
        mCrFilial.setSelectValue(getFieldText("FILIAL_CR"));
        String accCrType = Utils.fillUp(getFieldText("ACCTYPE_CR"),9);
        mCrAccountType.setValue(accCrType);
        mCrAccount.setValue(getFieldText("BSAACID_CR"));
        mCrSum.setValue(ifEmpty(getFieldValue("AMNT_CR"), ""));

        /*mSumRu.setValue(ifEmpty(getFieldValue("AMNTBC_DR"), ""));
        mSumRu.setVisible(false);
        mCheckSumRu.setValue(false);
        mCheckSumRu.setVisible(false);*/

        mNarrativeEN.setValue(getFieldText("NRT"));
        mNarrativeRU.setValue(getFieldText("RNARSHT"));
        mDepartment.setSelectValue(getFieldText("DEPT_ID"));
        mProfitCenter.setSelectValue(getFieldText("PRFCNTR"));
        //mProfitCenter.setVisible(false);

        pdMode = getFieldText("PDMODE");
        idOperation = getFieldValue("GLO_REF");

        isStorno = "Y".equals(getFieldText("STRN"));
        isCorrection = "Y".equals(getFieldText("FCHNG"));
        mCheckCorrection.setValue(isCorrection);
        mCheckCorrection.setVisible(true);

        disableAll();

        isFan = "Y".equals(getFieldText("FAN"));
        inputMethod = getFieldText("INP_METHOD");
        if (action == FormAction.UPDATE) {
	        setEnabled(!"AE".equals(inputMethod), postingChoice, isStorno && isCorrection, isFan );
            ok.setText("Сохранить");
        } else if (action == FormAction.OTHER) {
            isInvisible = "Y".equals(getFieldText("INVISIBLE"));
            ok.setText(isInvisible ? "Восстановить" : "Подавить");
        }
    }

    protected void setEnabled(boolean isManual, PostingChoice postingChoice, boolean disableCorrection, boolean isFan) {

        mDateValue.setEnabled(isManual);
        mDateOperation.setEnabled(isManual);
        mDealId.setEnabled(isManual);
        mSubDealId.setEnabled(isManual);
        mNarrativeEN.setEnabled(true);
        mNarrativeRU.setEnabled(true);
        mProfitCenter.setEnabled(true);
        mCheckCorrection.setEnabled(isManual);

        mDrAccountTypeButton.setEnabled(isManual);
        mCrAccountTypeButton.setEnabled(isManual);
    }

    protected void setControlsDisabled()
    {
        disableAll();
    }

    protected void setControlsEnabled(){};

    protected void enableAll()
    {
        enableAll(true);
    }

    protected void disableAll()
    {
        enableAll(false);
    }

    protected void enableAll(boolean isEnabled) {
        mDealSource.setEnabled(false);

        mDateOperation.setEnabled(false);
        mDateValue.setEnabled(false);

        mDtCurrency.setEnabled(false);
        mDtFilial.setEnabled(false);
        mDtAccount.setEnabled(false);
        mDtSum.setEnabled(false);
        mDtAccountType.setEnabled(false);

        mCrCurrency.setEnabled(false);
        mCrFilial.setEnabled(false);
        mCrAccount.setEnabled(false);
        mCrSum.setEnabled(false);
        mCrAccountType.setEnabled(false);
        mDepartment.setEnabled(false);

        mNarrativeEN.setEnabled(false);
        mNarrativeRU.setEnabled(false);
        mProfitCenter.setEnabled(false);

        mDealId.setEnabled(false);
        mSubDealId.setEnabled(false);
        mCheckCorrection.setEnabled(false);

        mDrAccountTypeButton.setEnabled(false);
        mCrAccountTypeButton.setEnabled(false);

    }

    @Override
    protected boolean onClickOK() throws Exception {
        //Window.alert("PostingTechDlg: onClickOK");
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
        operation.setCurrencyDebit(mCrCurrency.getParam("CCY").toString());
        operation.setAmountDebit(new BigDecimal(mDtSum.getValue()));
        operation.setAmountCredit(new BigDecimal(mCrSum.getValue()));

        ArrayList<Long> pdList = new ArrayList<Long>();
        operation.setPdIdList(pdList);


        operation.setNarrative(mNarrativeEN.getValue());
        operation.setRusNarrativeLong(check(mNarrativeRU.getValue()
                , "Основание RUS", "поле не заполнено", new CheckNotEmptyString()));
        operation.setDeptId(mDepartment.getValue().toString());
        operation.setProfitCenter((String) mProfitCenter.getValue());
        operation.setCorrection(mCheckCorrection.getValue());
        operation.setStorno(isStorno);
        operation.setFan(isFan);

        //Window.alert(inputMethod);
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
        fillUp();
        /*if (isAsyncListsCached != null && isAsyncListsCached){
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

            timer.scheduleRepeating(500);
        }*/
    }

}

