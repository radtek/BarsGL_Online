package ru.rbt.barsgl.gwt.client.operation;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.*;
import com.google.web.bindery.event.shared.HandlerRegistration;
import ru.rbt.barsgl.gwt.client.check.*;
import ru.rbt.barsgl.gwt.client.comp.CachedListEnum;
import ru.rbt.barsgl.gwt.client.comp.DataListBox;
import ru.rbt.security.gwt.client.operday.IDataConsumer;
import ru.rbt.security.gwt.client.operday.OperDayGetter;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.events.DataListBoxEvent;
import ru.rbt.barsgl.gwt.core.events.DataListBoxEventHandler;
import ru.rbt.barsgl.gwt.core.events.LocalEventBus;
import ru.rbt.barsgl.gwt.core.ui.DatePickerBox;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.shared.ClientDateUtils;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.InputMethod;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;
import ru.rbt.barsgl.shared.user.AppUserWrapper;

import java.math.BigDecimal;
import java.util.Date;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.*;
import static ru.rbt.security.gwt.client.operday.OperDayGetter.getOperday;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.*;

/**
 * Created by akichigi on 19.03.15.
 */
public class OperationDlg extends OperationDlgBase {

    protected DataListBox mDealSource;
    protected TxtBox mDealId;
    protected TxtBox mSubDealId;

    protected TxtBox mDateOperDay;
    protected DatePickerBox mDateOperation;
    protected DatePickerBox mDateValue;

    protected TxtBox mSumRu;
    protected CheckBox mCheckSumRu;
    protected CheckBox mCheckCorrection;

    protected boolean isRurDebit = false;
    protected boolean isRurCredit = false;

    protected Date operday;
    protected String _reasonOfDeny;

    private int asyncListCount = 7; /*count async lists:  mDtCurrency; mCrCurrency; mDtFilial; mCrFilial;
                                                          mDepartment; mProfitCenter; mDealSource*/
    private HandlerRegistration registration;

    public OperationDlg(String title, FormAction action, Columns columns) {
        super(title, action, columns);

        /*Boolean isAsyncListsCached = (Boolean) LocalDataStorage.getParam("isAsyncListsCached");
        if (isAsyncListsCached != null && isAsyncListsCached) return;
        registration =  LocalEventBus.addHandler(DataListBoxEvent.TYPE, dataListBoxCreatedEventHandler());
        //save in local storage sign that async list is already cached
        LocalDataStorage.putParam("isAsyncListsCached", true);*/
    }

    private DataListBoxEventHandler dataListBoxCreatedEventHandler() {
        return new DataListBoxEventHandler(){

            @Override
            public void completeLoadData(String dataListBoxId) {
                // repeat setting values on async lists
                ManualOperationWrapper operation = (ManualOperationWrapper)params;

                if (mDtCurrency.getId().equalsIgnoreCase(dataListBoxId)) {
                    mDtCurrency.setSelectValue(ifEmpty(operation.getCurrencyDebit(), "RUR"));
                } else if (mCrCurrency.getId().equalsIgnoreCase(dataListBoxId)) {
                    mCrCurrency.setSelectValue(ifEmpty(operation.getCurrencyCredit(), "RUR"));
                } else if (mDealSource.getId().equalsIgnoreCase(dataListBoxId)) {
                    mDealSource.setSelectValue(operation.getDealSrc());
                } else if (mDtFilial.getId().equalsIgnoreCase(dataListBoxId)) {
                    mDtFilial.setSelectValue(operation.getFilialDebit());
                } else if (mCrFilial.getId().equalsIgnoreCase(dataListBoxId)){
                    mCrFilial.setSelectValue(operation.getFilialCredit());
                } else if (mDepartment.getId().equalsIgnoreCase(dataListBoxId)){
                    mDepartment.setSelectValue(operation.getDeptId());
                } else if (mProfitCenter.getId().equalsIgnoreCase(dataListBoxId)){
                    mProfitCenter.setSelectValue(operation.getProfitCenter());
                }

                asyncListCount--;

                if (asyncListCount == 0) {
                    registration.removeHandler();
                }
            }
        };
    }


    @Override
    public Widget createContent() {

        VerticalPanel mainVP = new VerticalPanel();
        mainVP.setSpacing(15);

        mainVP.add(createHeader());

        HorizontalPanel hp3 = new HorizontalPanel();
        hp3.setSpacing(0);
        hp3.add(createOneSide("Дебет", OperationDlgBase.Side.DEBIT, true));
        hp3.add(createOneSide("Кредит", OperationDlgBase.Side.CREDIT, true));
        mainVP.add(hp3);

        mainVP.add(createSumRu());

        mainVP.add(createDescriptions());
        mainVP.add(createDepartments(false));

        mDateOperDay.setReadOnly(true);
        getOperDay();

        setEnableSumRuHandler();
        setChangeHandlers();

        return mainVP;
    }

    protected void setControlsEnabled(){
    }


    protected void getOperDay() {
        getOperday(new IDataConsumer<OperDayWrapper>() {
            @Override
            public void accept(OperDayWrapper wrapper) {
                setOperday(wrapper.getCurrentOD());
            }
        });
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
        //grid.setWidget(0, 3, mDealSource =  createDealSourceAuthListBox("", FIELD2_WIDTH));
        grid.setWidget(0, 3, mDealSource =  createCachedDealSourceAuthListBox(CachedListEnum.AuthDealSources.name(), null, FIELD2_WIDTH));
        grid.setWidget(1, 2, createAlignWidget(createLabel("N сделки/ платежа"), LABEL2_WIDTH));
        grid.setWidget(1, 3, mDealId = createTxtBox(20, SUM_WIDTH));
        grid.setWidget(2, 2, createAlignWidget(createLabel("N субсделки"), LABEL2_WIDTH));
        grid.setWidget(2, 3, mSubDealId = createTxtBox(20, SUM_WIDTH));

        return grid;
    }

    protected Grid createSumRu() {
        Grid grid = new Grid(2,4);
        //g3.setWidget(0, 0, createLabel("", "40px"));
        grid.setWidget(0, 0, createLabel("Сумма в рублях", LABEL2_WIDTH));
        grid.setWidget(0, 1, mSumRu = createTextBoxForSumma(20, SUM_WIDTH));
        grid.setWidget(0, 2, mCheckSumRu = new CheckBox("Без расчета курсовой разницы"));
        grid.setWidget(1, 2, mCheckCorrection = new CheckBox("Исправительная проводка"));
        return grid;
    }

    @Override
    protected ManualOperationWrapper createWrapper() {
        return new ManualOperationWrapper();
    }

    @Override
    protected void setFields(ManualOperationWrapper operation) {
        operation.setId(id);
        if (mDealSource.isEnabled()) {
            operation.setDealSrc(check(mDealSource.getText()
                    , "Источник", "поле не заполнено", new CheckNotEmptyString()));
        } else {
            operation.setDealSrc(mDealSource.getText());
        }
//            operation.setDealId(check(mDealId.getValue()
//                    , "Номер сделки", "поле не заполнено", new CheckNotEmptyString()));
        operation.setDealId(mDealId.getValue());            // номер сделки
        operation.setSubdealId(mSubDealId.getValue());

        operation.setPaymentRefernce(mDealId.getValue());   // номер платежа

        check(mDateOperation.getValue(), "Дата проводки", "поле не заполнено", new CheckNotNullDate());
        check(mDateValue.getValue(), "Дата валютирования", "поле не заполнено", new CheckNotNullDate());
        operation.setPostDateStr(DateTimeFormat.getFormat(operation.dateFormat).format(mDateOperation.getValue()));
        operation.setValueDateStr(DateTimeFormat.getFormat(operation.dateFormat).format(mDateValue.getValue()));

        operation.setCurrencyCredit((String) mCrCurrency.getValue());
        operation.setCurrencyDebit((String) mDtCurrency.getValue());
        // operation.setFilialCredit((String) mCrFilial.getValue());
        //operation.setFilialDebit((String) mDtFilial.getValue());
        operation.setFilialDebit(check((String) mDtFilial.getValue()
                , "Филиал (дебет)", "поле не заполнено", new CheckNotEmptyString()));

        operation.setFilialCredit(check((String) mCrFilial.getValue()
                , "Филиал (кредит)", "поле не заполнено", new CheckNotEmptyString()));

        operation.setAccountCredit(check(mCrAccount.getValue()
                , "Счет (кредит)", "длина строки < 20 символов", new CheckStringExactLength(20)));
        operation.setAccountDebit(check(mDtAccount.getValue()
                , "Счет (дебет)", "длина строки < 20 символов", new CheckStringExactLength(20)));

        operation.setAmountCredit(check(mCrSum.getValue(),
                "Кредит: сумма", "поле должно быть заполнено числом или числом с точкой"
                , new CheckNotNullBigDecimal(), new ConvertStringToBigDecimal()));
        operation.setAmountDebit(check(mDtSum.getValue(),
                "Дебит: сумма", "поле должно быть заполнено числом или числом с точкой"
                , new CheckNotNullBigDecimal(), new ConvertStringToBigDecimal()));
        if (mCheckSumRu.getValue()) {
            BigDecimal sumRu = check(mSumRu.getValue(),
                    "Сумма в рублях:", "поле должно быть заполнено числом > 0"
                    , new CheckNotZeroBigDecimal(), new ConvertStringToBigDecimal());
            operation.setAmountRu(sumRu);
        }

        checkSide(operation.getAmountDebit(), operation.getAmountRu(),
                operation.getCurrencyDebit(), operation.getAccountCredit(), "Дебет:" );
        checkSide(operation.getAmountCredit(), operation.getAmountRu(),
                operation.getCurrencyCredit(), operation.getAccountDebit(), "Кредит:");

        operation.setNarrative(check(mNarrativeEN.getValue()
                , "Основание ENG", "поле не заполнено", new CheckNotEmptyString()));
        operation.setRusNarrativeLong(check(mNarrativeRU.getValue()
                , "Основание RUS", "поле не заполнено", new CheckNotEmptyString()));
        operation.setDeptId(check((String) mDepartment.getValue()
                , "Подразделение", "поле не заполнено", new CheckNotEmptyString()));
        operation.setProfitCenter((String) mProfitCenter.getValue());
        operation.setCorrection(mCheckCorrection.getValue());
        operation.setInputMethod(InputMethod.M);

        // Для проверки прав по филиалам
        AppUserWrapper wrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
        operation.setUserId(wrapper.getId());
        operation.setPostDateStr(ClientDateUtils.Date2String(mDateOperation.getValue()));
    }


    @Override
    protected void fillContent() {
        ManualOperationWrapper operation = (ManualOperationWrapper)params;

        id = operation.getId();
        mDealSource.setSelectValue(operation.getDealSrc());
        mDealId.setValue(operation.getDealId());
        mSubDealId.setValue(operation.getSubdealId());

    	mDateOperation.setValue(ClientDateUtils.String2Date(operation.getPostDateStr()));
        mDateValue.setValue(ClientDateUtils.String2Date(operation.getValueDateStr()));

    	mDtCurrency.setSelectValue(ifEmpty(operation.getCurrencyDebit(), "RUR"));
    	mDtFilial.setSelectValue(operation.getFilialDebit());
    	mDtAccount.setValue(operation.getAccountDebit());
    	mDtSum.setValue(getSumma(operation.getAmountDebit()));

    	mCrCurrency.setSelectValue(ifEmpty(operation.getCurrencyCredit(), "RUR"));
    	mCrFilial.setSelectValue(operation.getFilialCredit());
    	mCrAccount.setValue(operation.getAccountCredit());
    	mCrSum.setValue(getSumma(operation.getAmountCredit()));

    	BigDecimal amountRu = operation.getAmountRu();
    	boolean withoutDiff = !operation.getCurrencyDebit().equals(operation.getCurrencyCredit())
    			&& (null != amountRu);
    	mCheckSumRu.setValue(withoutDiff);
    	mSumRu.setValue(withoutDiff ? getSumma(amountRu) : "");

    	mNarrativeEN.setValue(operation.getNarrative());
    	mNarrativeEN.setValue(operation.getNarrative());
        mNarrativeRU.setValue(operation.getRusNarrativeLong());
    	mDepartment.setSelectValue(operation.getDeptId());
        mProfitCenter.setSelectValue(operation.getProfitCenter());
        mCheckCorrection.setValue(operation.isCorrection());

        _reasonOfDeny = operation.getReasonOfDeny();
        setControlsEnabled();
    }

    protected void setControlsDisabled(){
            mDateOperation.setEnabled(false);
            mDateValue.setEnabled(false);

            mDealSource.setEnabled(false);
            mDealId.setEnabled(false);
            mSubDealId.setEnabled(false);

            mDtCurrency.setEnabled(false);
            mDtFilial.setEnabled(false);
            mDtAccount.setEnabled(false);
            mDtSum.setEnabled(false);

            mCrCurrency.setEnabled(false);
            mCrFilial.setEnabled(false);
            mCrAccount.setEnabled(false);
            mCrSum.setEnabled(false);

            mSumRu.setEnabled(false);

            mDtButton.setEnabled(true);
            mCrButton.setEnabled(true);

            mCheckSumRu.setEnabled(false);
            mCheckCorrection.setEnabled(false);

            mNarrativeEN.setEnabled(false);
            mNarrativeRU.setEnabled(false);
            mDepartment.setEnabled(false);
            mProfitCenter.setEnabled(false);
    }

    protected void setOperday(final String operDayStr) {
        mDateOperDay.setValue(operDayStr);
        operday = DateTimeFormat.getFormat(OperDayGetter.dateFormat).parse(operDayStr);
        if (action == FormAction.CREATE){
            mDateOperation.setValue(operday);
            mDateValue.setValue(operday);
        }
    }

    @Override
    protected Date getAccountDate() {
        return mDateValue.getValue();
    }

    protected void setEnableSumRuHandler() {
        mSumRu.setEnabled(false);
        mCheckSumRu.setValue(false);
    	mCheckSumRu.setEnabled(true);
    	mCheckSumRu.addValueChangeHandler(new ValueChangeHandler<Boolean>() {

			@Override
			public void onValueChange(ValueChangeEvent<Boolean> valueChangeEvent) {
                boolean toOn = valueChangeEvent.getValue();
                try {
	                if (toOn) {     // разрешить редактирование
	                	CheckNotZeroBigDecimal checkBigDecimal = new CheckNotZeroBigDecimal();
	                    mCheckSumRu.setValue(false);
	                    String dtCurrency = (String)mDtCurrency.getValue();
	                    String crCurrency = (String)mCrCurrency.getValue();
	                    if (dtCurrency.equals(crCurrency)) {
	                		showInfo("Ошибка", "Сумма в рублях вводится только для операции в разных валютах");
	                		return;
	                	} else if ("RUR".equals(dtCurrency)) {
	                        mSumRu.setValue(check(mDtSum.getValue(), "Дебет: сумма", "поле должно быть не 0", checkBigDecimal));
	                    } else if ("RUR".equals(crCurrency)) {
	                        mSumRu.setValue(check(mCrSum.getValue(), "Кредит: сумма", "поле должно быть не 0", checkBigDecimal));
	                	} else if (!checkBigDecimal.check(mDtSum.getValue()) || !checkBigDecimal.check(mCrSum.getValue())) {
                            showInfo("Ошибка", "Сумма в валюте может быть равна 0 только для операций по курсовой разнице");
                            return;
                        } else {
	                        mSumRu.setEnabled(true);
	                    }
	                    mCheckSumRu.setValue(true);
	                    isRurDebit = "RUR".equals(dtCurrency);
	                    isRurCredit = "RUR".equals(crCurrency);
	                } else {
	                	mSumRu.clear();
	                    mSumRu.setEnabled(false);
	                    isRurDebit = false;
	                    isRurCredit = false;
	                }
		    	} catch (IllegalArgumentException e){
		    	}
			}
    	});
    }

    protected void setChangeHandlers() {
    	mDtCurrency.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				if (isRurDebit || "RUR".equals(mDtCurrency.getValue())) 
			        mCheckSumRu.setValue(false, true);
			}
    	});
    	mCrCurrency.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				if (isRurCredit || "RUR".equals(mCrCurrency.getValue())) 
			        mCheckSumRu.setValue(false, true);
			}
    	});
    	mDtSum.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				if (isRurDebit) { 
					if (new CheckNotZeroBigDecimal().check(mDtSum.getValue())) {
						mSumRu.setValue((String)mDtSum.getValue());
					} else {
				        mCheckSumRu.setValue(false, true);
					}
				}
			}
    	});
    	mCrSum.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				if (isRurCredit) { 
					if (new CheckNotZeroBigDecimal().check(mCrSum.getValue())) {
						mSumRu.setValue((String)mCrSum.getValue());
					} else {
				        mCheckSumRu.setValue(false, true);
					}
				}
			}
    	});
    }

    protected void checkSide(BigDecimal sum, BigDecimal sumRu, String currency, String bsaSecond, String side) {
    	boolean trueSum = true;
    	if ("RUR".equals(currency)) {
    		trueSum = (sum.signum() > 0);
    	} else {
    		trueSum = (sum.signum() > 0) || (null != sumRu && sumRu.signum() > 0 && bsaSecond.startsWith("706"));
    	}
    	if (!trueSum) {
            showInfo("Ошибка", side + " сумма может быть равна 0 только для операций по курсовой разнице,\n" +
                    "с установленным признаком 'без расчета курсовой разницы'");
            throw new IllegalArgumentException("column");
        }
    }

}