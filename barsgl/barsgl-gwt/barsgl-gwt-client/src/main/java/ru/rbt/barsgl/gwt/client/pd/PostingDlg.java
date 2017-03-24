package ru.rbt.barsgl.gwt.client.pd;

import com.google.gwt.i18n.client.DateTimeFormat;
import ru.rbt.barsgl.gwt.client.check.CheckNotEmptyString;
import ru.rbt.barsgl.gwt.client.check.CheckNotNullDate;
import ru.rbt.barsgl.gwt.client.comp.DataListBoxEx;
import ru.rbt.barsgl.gwt.client.operation.OperationDlg;
import ru.rbt.security.gwt.client.operday.IDataConsumer;
import ru.rbt.security.gwt.client.operday.OperDayGetter;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.SecurityChecker;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.shared.ClientDateUtils;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.InputMethod;
import ru.rbt.barsgl.shared.enums.PostingChoice;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;
import ru.rbt.barsgl.shared.user.AppUserWrapper;

import java.util.Date;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.createCachedFilialListBox;
import static ru.rbt.security.gwt.client.operday.OperDayGetter.getOperday;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.*;
import static ru.rbt.barsgl.shared.enums.PostingChoice.*;
import static ru.rbt.barsgl.shared.enums.SecurityActionCode.*;

/**
 * Created by ER18837 on 05.04.16.
 */
public class PostingDlg extends OperationDlg {
    private PostingChoice postingChoice;
    private Long idParentOperation;
    private String inputMethod;
    private String pdMode;
    private boolean isFan;
    private boolean isStorno;
    private boolean isCorrection;
    private boolean isInvisible;

    public PostingDlg(String title, FormAction action, Columns columns) {
        super(title, action, columns);
        if (action == FormAction.PREVIEW){
            ok.setVisible(false);
        }
    }

   @Override
    protected DataListBoxEx createFilialListBox(String name, String filial, String width) {
        return createCachedFilialListBox(name, filial, width, true, true);
    }

    @Override
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
    protected void fillUp(){
    	if (null == params)
    		return;
        row = (Row) params;
        
        getOperDay();

        mDealSource.setSelectValue(getFieldText("SRC_PST"));
        String dealId = getFieldText("DEAL_ID");
        String paymentRef = getFieldText("PMT_REF");
        mDealId.setValue(!isEmpty(dealId) ? dealId : paymentRef);
        mSubDealId.setValue(getFieldText("SUBDEALID"));

        mDateOperation.setValue((Date)getFieldValue("POSTDATE"));
        mDateValue.setValue((Date)getFieldValue("VDATE"));

        mDtCurrency.setSelectValue(getFieldText("CCY_DR"));
        mDtFilial.setSelectValue(getFieldText("CBCC_DR"));
        mDtAccount.setValue(getFieldText("BSAACID_DR"));
        mDtSum.setValue(ifEmpty(getFieldValue("AMT_DR"), ""));

        mCrCurrency.setSelectValue(getFieldText("CCY_CR"));
        mCrFilial.setSelectValue(getFieldText("CBCC_CR"));
        mCrAccount.setValue(getFieldText("BSAACID_CR"));
        mCrSum.setValue(ifEmpty(getFieldValue("AMT_CR"), ""));

        mSumRu.setValue(ifEmpty(getFieldValue("AMTRU"), ""));
        mCheckSumRu.setValue(false);

        mNarrativeEN.setValue(getFieldText("NRT"));
        mNarrativeRU.setValue(getFieldText("RNARLNG"));
        mDepartment.setSelectValue(getFieldText("DPMT"));
        mProfitCenter.setSelectValue(getFieldText("PRFCNTR"));

        pdMode = getFieldText("PDMODE");
        idParentOperation = getFieldValue("PAR_GLO");

        isStorno = "Y".equals(getFieldText("STRN"));
        isCorrection = "Y".equals(getFieldText("FCHNG"));
        mCheckCorrection.setValue(isCorrection);

        disableAll();

        isFan = "Y".equals(getFieldText("FAN"));
        inputMethod = getFieldText("INP_METHOD");
        postingChoice = getPostingCoice();
        if (action == FormAction.UPDATE) {
	        setEnabled(!"AE".equals(inputMethod), postingChoice, isStorno && isCorrection, isFan );
            ok.setText("Сохранить");
        } else if (action == FormAction.OTHER) {
            isInvisible = "Y".equals(getFieldText("INVISIBLE"));
            ok.setText(isInvisible ? "Восстановить" : "Подавить");
        }
    }
    
    protected PostingChoice getPostingCoice() {
        return PST_SINGLE;
    }
    
    protected void setEnabled(boolean isManual, PostingChoice postingChoice, boolean disableCorrection, boolean isFan) {

        boolean allPd = postingChoice != PST_ONE_OF;
        boolean singlePd = postingChoice != PST_ALL;
        boolean manualAll = allPd && isManual;

    	if (isFan) {
            mProfitCenter.setEnabled(allPd);
    	} 
    	else {
            if (SecurityChecker.checkActions(OperPstChngDate, OperPstChngDateArcRight)) {
                mDateValue.setEnabled(allPd);
                mDateOperation.setEnabled(allPd);
            }
            if (SecurityChecker.checkAction(OperPstChng)) {
                mDealId.setEnabled(manualAll);
                mSubDealId.setEnabled(manualAll);

                mNarrativeEN.setEnabled(singlePd);
                mNarrativeRU.setEnabled(singlePd);

                mProfitCenter.setEnabled(allPd);
                mCheckCorrection.setEnabled(allPd && !disableCorrection);
            }
    	}
    }

    protected void disableAll() {
        mDealSource.setEnabled(false);

        mDateOperation.setEnabled(false);
        mDateValue.setEnabled(false);

        mDtCurrency.setEnabled(false);
        mDtFilial.setEnabled(false);
        mDtAccount.setEnabled(false);
        mDtSum.setEnabled(false);

        mCrCurrency.setEnabled(false);
        mCrFilial.setEnabled(false);
        mCrAccount.setEnabled(false);
        mCrSum.setEnabled(false);

        mSumRu.setEnabled(false);
        mCheckSumRu.setEnabled(false);
        mDepartment.setEnabled(false);

        mNarrativeEN.setEnabled(false);
        mNarrativeRU.setEnabled(false);
        mProfitCenter.setEnabled(false);

        mDealId.setEnabled(false);
        mSubDealId.setEnabled(false);
        mCheckCorrection.setEnabled(false);
    }

    @Override
    protected boolean onClickOK() throws Exception {
        boolean res = super.onClickOK();
        if (!res)
            return false;

        if (postingChoice == PST_ALL) {
            showConfirm("Будут изменены все связанные по операции проводки.\n Продолжить?", this.getDlgEvents(), params);
            return false;
        }
        else
            return true;
    }

    @Override
    protected void setFields(ManualOperationWrapper operation) {
        operation.setId(idParentOperation);

        operation.setDealSrc(mDealSource.getText());
        operation.setDealId(mDealId.getValue());
        operation.setSubdealId(mSubDealId.getValue());
        operation.setPaymentRefernce(mDealId.getValue());

        check(mDateOperation.getValue(), "Дата проводки", "поле не заполнено", new CheckNotNullDate());
        check(mDateValue.getValue(), "Дата валютирования", "поле не заполнено", new CheckNotNullDate());
        operation.setPostDateStr(DateTimeFormat.getFormat(operation.dateFormat).format(mDateOperation.getValue()));
        operation.setValueDateStr(DateTimeFormat.getFormat(operation.dateFormat).format(mDateValue.getValue()));

        operation.setFilialCredit((String) mCrFilial.getValue());
        operation.setFilialDebit((String) mDtFilial.getValue());
        operation.setAccountCredit(mCrAccount.getValue());
        operation.setAccountDebit(mDtAccount.getValue());

        operation.setNarrative(mNarrativeEN.getValue());
        operation.setRusNarrativeLong(check(mNarrativeRU.getValue()
                , "Основание RUS", "поле не заполнено", new CheckNotEmptyString()));
        operation.setDeptId(mDepartment.getText());
        operation.setProfitCenter((String) mProfitCenter.getValue());
        operation.setCorrection(mCheckCorrection.getValue());
        operation.setStorno(isStorno);
        operation.setFan(isFan);

        operation.setInputMethod(InputMethod.valueOf(inputMethod));
        operation.setPdMode(pdMode);
        operation.setInvisible(action == FormAction.OTHER ? !isInvisible : isInvisible);

        // Для проверки прав по филиалам
        AppUserWrapper wrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
        operation.setUserId(wrapper.getId());
        operation.setPostDateStr(ClientDateUtils.Date2String(mDateOperation.getValue()));
    }
}

