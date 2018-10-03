/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.gwt.client.dict.dlg;

import com.google.gwt.dom.client.InputElement;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.utils.AppPredicate;
import ru.rbt.barsgl.shared.dict.FormAction;

import java.io.Serializable;
import java.math.BigDecimal;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.check;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;

/**
 *
 * @author Andrew Samsonov
 */
public abstract class EditableDialog<T extends Serializable> extends DlgFrame {

    public final static String REQUIRED = " обязательно для заполнения";
    protected TextBox txtCode;
    protected Columns columns;
    protected Row row;
    private Object[] intiParams; // для передачи дополнительных параметров диалогу

    private String wrapperCaption;
    protected FormAction action;

    public EditableDialog(){
        super();
    }

    public EditableDialog(Columns columns, FormAction action) {
        super();
        this.columns = columns;
        setFormAction(action);
    }

    public void setFormAction(FormAction action){
        this.action = action;
        switch (action) {
            case CREATE:
                ok.setText("Создать");
                break;
            case UPDATE:
                ok.setText(TEXT_CONSTANTS.formData_Update());
                break;
            case DELETE:
                ok.setText(TEXT_CONSTANTS.formData_Delete());
                break;
        }
    }

    public void setColumns(Columns columns){
        this.columns = columns;
    }

    @Override
    public void setCaption(String caption) {
        wrapperCaption = caption;
        super.setCaption(caption);
    }

    public String getCaption() {
        return wrapperCaption;
    }


    protected Boolean beforeReturn(Object prm){
        return true;
    }

    @Override
    protected boolean onClickOK() throws Exception {
        T wrapper = createWrapper();
        try {
            setFields(wrapper);
            params = wrapper;
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().equals("column")) {
                return false;
            } else {
                throw e;
            }
        }
        return beforeReturn(wrapper);
    }

    protected String checkRequeredString(String value, String columnCaption) {
        return check(value, columnCaption, REQUIRED, new AppPredicate<String>() {
            @Override
            public boolean check(String target) {
                return (target != null) ? !target.trim().isEmpty() : false;
            }
        });
    }

    protected <T extends Serializable> T checkRequiredFieldValue(T value, String columnCaption) {
        return check(value, columnCaption, REQUIRED, new AppPredicate<T>() {
            @Override
            public boolean check(T target) {
                return null != target;
            }
        });
    }

    protected TextBox createTextBox(int length) {
        TextBox res = new TextBox();
        res.setMaxLength(length);
        res.setVisibleLength(length);
        return res;
    }

    protected TextArea createTextBoxArea(int length) {
        TextArea res = new TextArea();
        res.setHeight("50px");
        res.setWidth("370px");
        ((InputElement)res.getElement().cast()).setMaxLength(length);
        return res;
    }

    protected abstract T createWrapper();
    protected abstract void setFields(T cnw);
    public void clearContent() {};

    protected <U extends Serializable> U getFieldValue(String fieldName) {
        int ind = columns.getColumnIndexByName(fieldName);
        if (ind >= 0)
            return (U) row.getField(ind).getValue();
        else
            return null;
    }

    protected String getFieldText(String fieldName) {
        Object value = getFieldValue(fieldName);
        if (null != value)
            return value.toString().trim();
        else
            return null;
    }

    protected BigDecimal getBigDecimal(String fieldName) {
        String value = getFieldText(fieldName);
        if (null != value && value.length() > 0)
            return new BigDecimal(value);
        else
            return null;
    }


    public Object[] getIntiParams() {
        return intiParams;
    }

    public void setIntiParams(Object[] intiParams) {
        this.intiParams = intiParams;
    }
}
