package ru.rbt.barsgl.gwt.client.operation;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.web.bindery.event.shared.HandlerRegistration;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.check.*;
import ru.rbt.barsgl.gwt.client.comp.CachedListEnum;
import ru.rbt.barsgl.gwt.client.comp.DataListBox;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.DialogManager;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.events.DataListBoxEvent;
import ru.rbt.barsgl.gwt.core.events.DataListBoxEventHandler;
import ru.rbt.barsgl.gwt.core.events.LocalEventBus;
import ru.rbt.barsgl.gwt.core.ui.DatePickerBox;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.shared.ClientDateUtils;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.InputMethod;
import ru.rbt.barsgl.shared.operation.CurExchangeWrapper;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;
import ru.rbt.grid.gwt.client.GridEntryPoint;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.security.gwt.client.operday.IDataConsumer;
import ru.rbt.security.gwt.client.operday.OperDayGetter;
import ru.rbt.shared.user.AppUserWrapper;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.*;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.*;
import static ru.rbt.security.gwt.client.operday.OperDayGetter.getOperday;

/**
 * Created by akichigi on 19.03.15.
 */
public class OperationDlg extends OperationDlgBase {

    protected DataListBox mDealSource;

    public TxtBox getmDealId() {
        return mDealId;
    }

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
    private Boolean isAsyncListsCached;
    private Timer timer;

    public OperationDlg(String title, FormAction action, Columns columns) {
        super(title, action, columns);
    }

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

    @Override
    public Widget createContent() {
        VerticalPanel mainVP = new VerticalPanel();
        mainVP.setSpacing(15);

        mainVP.add(createHeader());

        HorizontalPanel hp3 = new HorizontalPanel();
        hp3.setSpacing(0);
        hp3.add(createOneSide("Дебет", OperationDlgBase.Side.DEBIT, true));
        mAccount.addChangeHandler(create_mAccount_ChangeHandler());
        hp3.add(createOneSide("Кредит", OperationDlgBase.Side.CREDIT, true));
        mAccount.addChangeHandler(create_mAccount_ChangeHandler());
        mainVP.add(hp3);

        mainVP.add(createSumRu());

        mainVP.add(createDescriptions());
        mainVP.add(createDepartments(false));

        mDateOperDay.setReadOnly(true);

        setEnableSumRuHandler();
        setChangeHandlers();

        return mainVP;
    }

//    List glAccDeals = Arrays.asList(new String[]{"45201"});
//45204810620150000063
    protected ChangeHandler create_mAccount_ChangeHandler() {
        return new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent changeEvent) {
                setDeals(((TextBox) changeEvent.getSource()).getValue());
            }
        };
    }

    @Override
    public void setDeals(final String account){
        String smDealId = mDealId.getValue();
        String smSubDealId = mSubDealId.getValue();
        if (!(smDealId == null || smDealId.isEmpty()) || !(smSubDealId == null || smSubDealId.isEmpty()))
            return;
//                log.info("mAccount");
//                log.info("mAccount = "+mAccount.getValue());

        if (null == account || account.length() < 20 || ((ArrayList)LocalDataStorage.getParam("Acc2ForDeals")).indexOf(account.substring(0,5)) < 1) return;
        GridEntryPoint.asyncGridService.selectFirst("select DEALID, SUBDEALID from gl_acc where bsaacid=?",new Serializable[]{account}, new AuthCheckAsyncCallback<Row>() {
            @Override
            public void onFailureOthers(Throwable throwable) {
                WaitingManager.hide();
                Window.alert("Операция не удалась. select DEALID, SUBDEALID from gl_acc where bsaacid="+account+"\n Ошибка: " + throwable.getLocalizedMessage());
                WaitingManager.hide();
            }
            @Override
            public void onSuccess(Row row) {
                if (row.getFieldsCount() != 0) {
//                    log.info("row.getField(0) = "+row.getField(0).getValue().toString());
//                    log.info("row.getField(1) = "+row.getField(1).getValue().toString());
                    mDealId.setValue(row.getField(0).getValue() == null? "":row.getField(0).getValue().toString());
                    mSubDealId.setValue(row.getField(1).getValue() == null? "":row.getField(1).getValue().toString());
                }
                WaitingManager.hide();
            }
        });

    };



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
        grid.setWidget(0, 3, mDealSource =  createCachedDealSourceAuthListBox(CachedListEnum.AuthDealSources.name(), null, FIELD2_WIDTH));
        grid.setWidget(1, 2, createAlignWidget(createLabel("N сделки/ платежа"), LABEL2_WIDTH));
        grid.setWidget(1, 3, mDealId = createTxtBox(20, SUM_WIDTH));
        grid.setWidget(2, 2, createAlignWidget(createLabel("N субсделки"), LABEL2_WIDTH));
        grid.setWidget(2, 3, mSubDealId = createTxtBox(20, SUM_WIDTH));

        return grid;
    }

    protected Grid createSumRu() {
        Grid grid = new Grid(2,3);

        grid.setWidget(0, 0, createLabel("Сумма в рублях", LABEL2_WIDTH));
        grid.setWidget(0, 1, mSumRu = createTextBoxForSumma(20, SUM_WIDTH));
        grid.setWidget(0, 2, mCheckSumRu = new CheckBox("без проводки по курсовой разнице"));
        grid.setWidget(1, 2, mCheckCorrection = new CheckBox("исправительная проводка"));

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
        operation.setDeptId((String) mDepartment.getValue());
        operation.setProfitCenter((String) mProfitCenter.getValue());
        operation.setCorrection(mCheckCorrection.getValue());
        operation.setInputMethod(InputMethod.M);

        // Для проверки прав по филиалам
        AppUserWrapper wrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
        operation.setUserId(wrapper.getId());
        operation.setPostDateStr(ClientDateUtils.Date2String(mDateOperation.getValue()));
    }

    protected void fillUp(){
        ManualOperationWrapper operation = (ManualOperationWrapper)params;
        if (operation != null){
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
        }

        getOperDay();

        setControlsEnabled();
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

            timer.scheduleRepeating(500);
        }
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
        return mDateOperation.getValue();
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
                        mSumRu.setValue(mDtSum.getValue());
					} else {
				        mCheckSumRu.setValue(false, true);
					}
				}
                correctSum(mDtSum, mCrSum);
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
                correctSum(mCrSum, mDtSum);
			}
    	});
    }

    protected void checkSide(BigDecimal sum, BigDecimal sumRu, String currency, String bsaSecond, String side) {
    	boolean trueSum;
    	if ("RUR".equals(currency)) {
    		trueSum = (sum.signum() > 0);
    	} else {
    		trueSum = (sum.signum() > 0) || (null != sumRu && sumRu.signum() > 0 &&
                      (bsaSecond.startsWith("706") || bsaSecond.startsWith("526")));
    	}
    	if (!trueSum) {
            showInfo("Ошибка", side + " сумма по валютному счету может равняться 0 только при условии\nотражения курсовой разницы (или финансового результата по сделке) и\n\n" +
                     "установленном признаке 'Без проводки по курсовой разнице'");
            throw new IllegalArgumentException("column");
        }
    }

    @Override
    protected void btnClick(Side side) {
        exchange(side.equals(Side.DEBIT));
    }

    private void exchange(boolean isDebit){
        if (mDateOperation.getValue() == null){
            showInfo("Ошибка", "Не заполнено поле 'Дата проводки'");
            return;
        }

        if (((String)mDtCurrency.getValue()).equalsIgnoreCase((String) mCrCurrency.getValue())){
            showInfo("Ошибка", "Для конвертации валюта дебета не должна быть равна валюте кредита");
            return;
        }

        if (!(((String)mDtCurrency.getValue()).equalsIgnoreCase("RUR") || ((String)mCrCurrency.getValue()).equalsIgnoreCase("RUR"))){
            showInfo("Ошибка", "Валюта дебета или кредита должна быть RUR");
            return;
        }

        String sum = isDebit ? mCrSum.getValue() : mDtSum.getValue();
        CheckNotZeroBigDecimal checkBigDecimal = new CheckNotZeroBigDecimal();

        if (!checkBigDecimal.check(sum)) {
            showInfo("Ошибка", Utils.Fmt("Сумма в валюте {0} должна быть заполнена и не равна нулю",
                     isDebit ? "кредита" : "дебета"));
            return;
        }
        calculateSum(createCurExchangeWrapper(isDebit), isDebit);
    }


    private CurExchangeWrapper createCurExchangeWrapper(boolean isDebit){
        CurExchangeWrapper wrapper = new CurExchangeWrapper();
        wrapper.setDate(DateTimeFormat.getFormat("dd.MM.yyyy").format(mDateOperation.getValue()));
        wrapper.setSourceCurrency(isDebit ? (String) mCrCurrency.getValue() : (String) mDtCurrency.getValue());
        wrapper.setSourceSum(new BigDecimal(isDebit ? mCrSum.getValue() : mDtSum.getValue()));
        wrapper.setTargetCurrency(isDebit ? (String) mDtCurrency.getValue() : (String) mCrCurrency.getValue());

        return wrapper;
    }

    private void calculateSum(CurExchangeWrapper wrapper, final boolean isDebit){
        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
        BarsGLEntryPoint.operationService.exchangeCurrency(wrapper, new AuthCheckAsyncCallback<RpcRes_Base<CurExchangeWrapper>>() {
            @Override
            public void onSuccess(RpcRes_Base<CurExchangeWrapper> res) {
                if (res.isError()) {
                    DialogManager.error("Ошибка", "Конвертация валюты не удалась.\nОшибка: " + res.getMessage());
                } else {
                    CurExchangeWrapper res_wrapper = res.getResult();
                    setSum(res_wrapper, isDebit);
                    showInfo(res.getMessage());
                }
                WaitingManager.hide();
            }
        });
    }

    private void setSum(CurExchangeWrapper wrapper, boolean isDebit){
        if (isDebit){
            mDtSum.setValue(wrapper.getTargetSum().toPlainString());
        }else {
            mCrSum.setValue(wrapper.getTargetSum().toPlainString());
        }
    }

    private void correctSum(TxtBox boxA, TxtBox boxB){
        CheckNotZeroBigDecimal checkBigDecimal = new CheckNotZeroBigDecimal();

        if (checkBigDecimal.check(boxA.getValue()) && !checkBigDecimal.check(boxB.getValue()) &&
           ((String)mDtCurrency.getValue()).equalsIgnoreCase((String) mCrCurrency.getValue())){
            boxB.setValue(boxA.getValue());
        }
    }
}