package ru.rbt.barsgl.gwt.core.ui;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.ui.TextBox;

public abstract class FieldTextBox extends TextBox {
    private static String STYLE_DEF = "input_def";
    private static String STYLE_NORMAL = "input_normal";
    private static String STYLE_SELECTED = "input_selected";
    private static String STYLE_MODIFIED = "data_modified";
    private boolean showModified;
    protected String validRegMask;

    public FieldTextBox() {
        setStyleName(STYLE_DEF);
        validRegMask = "";
    }

    public FieldTextBox(String text, int length, boolean isDefStyle) {
        this(text, length, isDefStyle, false);
    }

    public FieldTextBox(String text, int length, boolean isDefStyle, boolean isShowModified) {
        this.showModified = isShowModified;
        setText(text);
        if (length != -1) setMaxLength(length);
        validRegMask = "";

        if (isDefStyle) {
            setStyleName(STYLE_DEF);
        } else {
            setStyleName(STYLE_NORMAL);
            addFocusBlurHandler();
        }
    }

    private void addFocusBlurHandler() {
        addBlurHandler(new BlurHandler() {
            public void onBlur(BlurEvent event) {
                FieldTextBox.this.setStyleName(FieldTextBox.STYLE_NORMAL);
                FieldTextBox.this.doOnBlur();
            }
        });
        addFocusHandler(new FocusHandler() {
            public void onFocus(FocusEvent event) {
                FieldTextBox.this.setStyleName(FieldTextBox.STYLE_SELECTED);
            }
        });
    }

    public void setStyleModified(boolean isModified) {
        if (!showModified)
            return;

        if (isModified)
            this.addStyleName(FieldTextBox.STYLE_MODIFIED);
        else
            this.removeStyleName(FieldTextBox.STYLE_MODIFIED);
    }

    /**
     * @return the validRegMask
     */
    public String getValidRegMask() {
        return validRegMask;
    }

    /**
     * @param validRegMask the validRegMask to set
     */
    public void setValidRegMask(String validRegMask) {
        this.validRegMask = validRegMask;
    }

    public void addHandler_KeyPressMask() {
        this.addKeyPressHandler(new KeyPressHandler() {
            @Override
            public void onKeyPress(KeyPressEvent event) {
                char ch = event.getCharCode();

                if (!checkCharValidByMask(ch)) {
                    FieldTextBox.this.cancelKey();
                }
            }

        });
    }

    private boolean checkCharValidByMask(char ch){
        if (getValidRegMask().length()>0) {
            try {
                String charToString = Character.toString(ch);
                RegExp p = RegExp.compile(validRegMask);
                if (p.test(charToString))
                    return true;
                else
                    return false;
            } catch (Exception e) {
                return false;
            }
        }else{
            return true;
        }
    }

    public abstract void doOnBlur();
}