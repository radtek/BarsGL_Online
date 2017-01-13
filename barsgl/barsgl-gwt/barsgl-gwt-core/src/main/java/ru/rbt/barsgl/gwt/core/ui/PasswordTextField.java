package ru.rbt.barsgl.gwt.core.ui;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.user.client.ui.PasswordTextBox;

/**
 * Created by ER21006 on 13.01.2015.
 */
public abstract class PasswordTextField extends PasswordTextBox {

    private static String STYLE_DEF = "input_def";
    private static String STYLE_NORMAL = "input_normal";
    private static String STYLE_SELECTED = "input_selected";

    public PasswordTextField() {
        super();
        setStyleName(STYLE_DEF);
    }

    public PasswordTextField(String text, int length, boolean isDefStyle) {
        super();
        setText(text);
        setMaxLength(length);

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
                PasswordTextField.this
                        .setStyleName(PasswordTextField.STYLE_NORMAL);
                PasswordTextField.this.doOnBlur();
            }
        });
        addFocusHandler(new FocusHandler() {
            public void onFocus(FocusEvent event) {
                PasswordTextField.this
                        .setStyleName(PasswordTextField.STYLE_SELECTED);
            }
        }); 
    }

    @Override
    public void setEnabled(boolean enabled){
    	super.setEnabled(enabled);
    	getElement().getStyle().setBackgroundColor(enabled ? "white" : "#f3f1e8" );    	
    }
    
    @Override
    public void setReadOnly(boolean readOnly) {
    	super.setReadOnly(readOnly);
        getElement().getStyle().setBackgroundColor(readOnly ? "#f3f1e8" : "white" );
    } 
    
    
    public abstract void doOnBlur();

}
