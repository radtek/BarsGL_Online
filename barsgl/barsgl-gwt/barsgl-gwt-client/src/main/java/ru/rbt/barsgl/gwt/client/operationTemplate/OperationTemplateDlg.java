package ru.rbt.barsgl.gwt.client.operationTemplate;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.client.check.CheckNotEmptyString;
import ru.rbt.barsgl.gwt.client.comp.DataListBox;
import ru.rbt.barsgl.gwt.client.operation.OperationDlgBase;
import ru.rbt.security.gwt.client.operday.IDataConsumer;
import ru.rbt.security.gwt.client.operday.OperDayGetter;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.ui.AreaBox;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;

import java.util.Date;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.*;
import ru.rbt.barsgl.gwt.core.comp.Components;
import static ru.rbt.security.gwt.client.operday.OperDayGetter.getOperday;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.*;

/**
 * Created by ER18837 on 16.03.16.
 */
public class OperationTemplateDlg extends OperationDlgBase {
    protected Date operday;

    protected VerticalPanel mainVP;
    protected AreaBox mName;
    protected DataListBox mDealSource;


    @Override
    public Widget createContent() {

        mainVP = new VerticalPanel();
        mainVP.setSpacing(15);

        mainVP.add(createHeader());

        HorizontalPanel hp3 = new HorizontalPanel();
        hp3.setSpacing(0);
        hp3.add(createOneSide("Дебет", Side.DEBIT, false));
        hp3.add(createOneSide("Кредит", Side.CREDIT, false));
        mainVP.add(hp3);

        mainVP.add(createParams(false));

        mainVP.add(createDescriptions());
        mainVP.add(createDepartments(false));

        getOperday(new IDataConsumer<OperDayWrapper>() {
            @Override
            public void accept(OperDayWrapper wrapper) {
                operday = DateTimeFormat.getFormat(OperDayGetter.dateFormat).parse(wrapper.getCurrentOD());
            }
        });

        return mainVP;
    }

    private Grid createHeader(){
        Grid grid = new Grid(2,2);
        grid.setWidget(0, 0, Components.createLabel("Наименование шаблона", LABEL_WIDTH));
        grid.setWidget(0, 1, mName = Components.createAreaBox(LONG_WIDTH, "40px"));
        grid.setWidget(1, 0, Components.createLabel("Источник сделки"));
        grid.setWidget(1, 1, mDealSource = createDealSourceListBox("", "100px"));
        return grid;
    };

    @Override
    protected ManualOperationWrapper createWrapper() {
        return new ManualOperationWrapper();
    }

    @Override
    protected Date getAccountDate() {
        return operday;
    }

    @Override
    public void clearContent() {
        id = null;

        mName.setValue(null);
        mDealSource.setValue(null);

        mDtCurrency.setValue(null);
        mDtFilial.setValue(null);
        mDtAccount.setValue(null);

        mCrCurrency.setValue(null);
        mCrFilial.setValue(null);
        mCrAccount.setValue(null);

        mNarrativeEN.setValue(null);
        mNarrativeRU.setValue(null);
        mDepartment.setValue(null);
        mProfitCenter.setValue(null);
        setContentEnabled(false);
    }
    
    @Override
    protected void fillContent() {
        ok.setEnabled(true);
        if (null == params)
        	return;

        row = (Row) params;

        if (action == FormAction.UPDATE || action == FormAction.DELETE) {

            id = getFieldValue("ID_TMPL");

            mName.setValue(getFieldText("TMPL_NAME"));
            mDealSource.setValue(getFieldText("SRC_PST"));

            mDtCurrency.setSelectValue(getFieldText("CCY_DR"));
            mDtFilial.setSelectValue(getFieldText("CBCC_DR"));
            mDtAccount.setValue(getFieldText("AC_DR"));

            mCrCurrency.setSelectValue(getFieldText("CCY_CR"));
            mCrFilial.setSelectValue(getFieldText("CBCC_CR"));
            mCrAccount.setValue(getFieldText("AC_CR"));

            mNarrativeEN.setValue(getFieldText("NRT"));
            mNarrativeRU.setValue(getFieldText("RNRTL"));
            mDepartment.setSelectValue(getFieldText("DEPT_ID"));
            mProfitCenter.setSelectValue(getFieldText("PRFCNTR"));

            boolean sys = "Y".equals(getFieldText("SYS"));
            ok.setEnabled(!sys);

            setContentEnabled(action == FormAction.DELETE || sys);
        }
    }

    protected void setContentEnabled(boolean readOnly) {
    	boolean enabled = !readOnly;
        mName.setReadOnly(readOnly);
        mDealSource.setReadOnly(readOnly);

        mDtCurrency.setReadOnly(readOnly);
        mDtFilial.setReadOnly(readOnly);
        mDtAccount.setReadOnly(readOnly);
        mDtButton.setEnabled(enabled);

        mCrCurrency.setReadOnly(readOnly);
        mCrFilial.setReadOnly(readOnly);
        mCrAccount.setReadOnly(readOnly);
        mCrButton.setEnabled(enabled);

        mNarrativeEN.setReadOnly(readOnly);
        mNarrativeRU.setReadOnly(readOnly);
        mDepartment.setReadOnly(readOnly);
        mProfitCenter.setReadOnly(readOnly);
    }

    @Override
    protected void setFields(ManualOperationWrapper operation) {
        params = operation;

        operation.setId(id);
        if (action == FormAction.DELETE)
            return;

        operation.setTemplateName(check(mName.getValue()
                , "Наименование шиблона", "поле не заполнено", new CheckNotEmptyString()));
        operation.setDealSrc(check((String)mDealSource.getValue()
                , "Источник", "поле не заполнено", new CheckNotEmptyString()));

        if (isEmpty(mDtAccount.getValue()) && isEmpty(mCrAccount.getValue())) {
            showInfo("Должен быть задан хотя бы один счет");
            throw new IllegalArgumentException("column");
        }

        operation.setCurrencyDebit((String)mDtCurrency.getValue());
        operation.setFilialDebit((String)mDtFilial.getValue());
        operation.setAccountDebit(mDtAccount.getValue());

        operation.setCurrencyCredit((String)mCrCurrency.getValue());
        operation.setFilialCredit((String)mCrFilial.getValue());
        operation.setAccountCredit(mCrAccount.getValue());

        operation.setNarrative(mNarrativeEN.getValue());
        operation.setRusNarrativeLong(check(mNarrativeRU.getValue()
                , "Основание RUS", "поле не заполнено", new CheckNotEmptyString()));

        operation.setDeptId((String)mDepartment.getValue());
        operation.setProfitCenter((String)mProfitCenter.getValue());
        operation.setPostDateStr(DateTimeFormat.getFormat(operation.dateFormat).format(operday));

        operation.setExtended(isExtended(mNarrativeRU.getValue()) || isExtended(mNarrativeEN.getValue()));
        operation.setSystem(false);
    }

    private void checkAccountParams(String account, String ccy, String filial, String side) {
        if (!isEmpty(account) || account.length() != 20 || account.contains("%") || account.contains("_"))
            return;

        if(isEmpty(ccy) || isEmpty(filial)) {
            showInfo("Для счета " + side + " должны быть заданы валюта и филиал");
            throw new IllegalArgumentException("column");
        }
    }
        
    private boolean isExtended(String narrative) {
        return !isEmpty(narrative) && ((
                narrative.contains("[N1]") || narrative.contains("[D1]") ||
                narrative.contains("[N2]") || narrative.contains("[D2]")
                ));
    }
}
