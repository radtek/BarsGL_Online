package ru.rbt.barsgl.gwt.client.operation;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.security.gwt.client.operday.IDataConsumer;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.ui.IBoxValue;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.BatchPostStep;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static ru.rbt.security.gwt.client.operday.OperDayGetter.getOperday;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

/**
 * Created by ER18837 on 22.03.16.
 */
public class OperationExtDlg extends OperationHandsDlg {

    public OperationExtDlg(String title, FormAction action, Columns columns, BatchPostStep step) {
        super(title, action, columns, step);
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

        mainVP.add(createParams(true));
        mainVP.add(createDescriptions());
        mainVP.add(createDepartments(true));

        mDateOperDay.setReadOnly(true);
        getOperday(new IDataConsumer<OperDayWrapper>() {
            @Override
            public void accept(OperDayWrapper wrapper) {
                setOperday(wrapper.getCurrentOD());
            }
        });

        setEnableSumRuHandler();
        setChangeHandlers();
        setCheckHandler();

        return mainVP;
    }

    protected void setCheckHandler() {
/*
        ChangeHandler descriptionHandler = new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent changeEvent) {
                mCheckFields.setValue(false);
            }
        };

        mNarrativeEN.addChangeHandler(descriptionHandler);
        mNarrativeRU.addChangeHandler(descriptionHandler);
        mNum1.addChangeHandler(descriptionHandler);
        mDate1.addChangeHandler(descriptionHandler);
        mNum2.addChangeHandler(descriptionHandler);
        mDate2.addChangeHandler(descriptionHandler);
        mDealId.addChangeHandler(descriptionHandler);
        mSubDealId.addChangeHandler(descriptionHandler);
*/

        mCheckFields.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> valueChangeEvent) {
                boolean toOn = valueChangeEvent.getValue();
                if (toOn) {
                    mCheckFields.setValue(fillDescriptions());
                }
            }
        });

    }

    protected boolean fillDescriptions() {
        Set<String> errSet = new HashSet<String>();
        boolean err = !fillDescription(mNarrativeEN, errSet) | !fillDescription(mNarrativeRU, errSet);
        if (err) {
        	String errString = errSet.toString(); 
            showInfo("Для формирования оснований не хватает полей: " 
            		+ errString.substring(1, errString.length()-1)
                    + "\nЗаполните поля или исключите ссылки из текста оснований");
            return false;
        } else {
        	return true;
        } 
    }

    protected boolean fillDescription(IBoxValue boxValue, Set<String> errSet) {
        String pattern = (String)boxValue.getValue();
        if (isEmpty(pattern))
        	return true;
        for (Map.Entry<String, IBoxValue> entry: mapParam.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().getText();
            if (pattern.contains(key)) {
                if (isEmpty(value))
                    errSet.add(key);
                else {
                	pattern = pattern.replace(key, value);
                }
            }

        }
        boxValue.setValue(pattern);
        return errSet.isEmpty();
    }

    @Override
    protected void setFields(ManualOperationWrapper operation) {
        super.setFields(operation);
        if (!mCheckFields.getValue()) {
        	boolean checked = fillDescriptions();
            mCheckFields.setValue(checked);
            if (!checked) {
//            	showInfo("Для создания операции флажок 'Основание проверено' должен быть установлен)");
            	throw new IllegalArgumentException("column");
            }
        }
    }
}
