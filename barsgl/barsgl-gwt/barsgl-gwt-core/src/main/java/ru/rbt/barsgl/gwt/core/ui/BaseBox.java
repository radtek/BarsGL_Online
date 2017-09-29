package ru.rbt.barsgl.gwt.core.ui;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import java.io.Serializable;

public abstract class BaseBox<T extends Serializable> extends Composite implements IBoxValue<T>{	
	protected FieldTextBox textBox;
    protected T value;
    
    public BaseBox(T value){
    	this.value = value;
    	initWidget(configure());
    }
         
    protected FieldTextBox createTextBox(){    	
    	 return textBox = new FieldTextBox(makeVisibleString(), -1, false, false) {
             @Override
             public void doOnBlur() {
                 save();
             }
         };
    }
      
    private void save() {
        if (validate()) {
           saveValue();
        }
        else
        {
            value = null;
            textBox.setText("");
            textBox.setCursorPos(0);
            textBox.setFocus(true);
        }
    }
        
    protected abstract Widget configure();
    protected abstract String makeVisibleString();
    protected abstract void saveValue();
    protected abstract boolean validate(String text);

    @Override
    public String getText() {
        return textBox.getText();
    }

    @Override
	public boolean validate() {
    	return validate(textBox.getText());
	}
    
    @Override
    public String toString(){
		return makeVisibleString();    	
    }
    
    @Override
    public void setWidth(String width){
        textBox.setWidth(width);
    }

    public void setMaxLength(int length) {
        textBox.setMaxLength(length);
    }

    public void setVisibleLength(int length) {
        textBox.setVisibleLength(length);
    }

    public int getMaxLength() {
        return textBox.getMaxLength();
    }

    public int getVisibleLength() {
        return textBox.getVisibleLength();
    }

    public void setFocus(boolean focus) {
        textBox.setFocus(focus);
        if (focus) textBox.setCursorPos(0);
    }

    @Override
    public void setEnabled(boolean enabled) {
        textBox.setEnabled(enabled);
        textBox.getElement().getStyle().setBackgroundColor(enabled ? "white" : "#f3f1e8" );
    }

    @Override
    public boolean isEnabled() {
        return textBox.isEnabled();
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        textBox.setReadOnly(readOnly);
        textBox.getElement().getStyle().setBackgroundColor(readOnly ? "#f3f1e8" : "white" );
    } 
    
    public void clear() {
        textBox.setText("");
        value = null;
    }

    public HandlerRegistration addKeyPressHandler(KeyPressHandler handler) {
       return textBox.addKeyPressHandler(handler);
    }
 
    public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
    	return textBox.addKeyDownHandler(handler);
    }

    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler){return textBox.addValueChangeHandler(handler);}

    public HandlerRegistration addKeyUpHandler(KeyUpHandler handler){return textBox.addKeyUpHandler(handler);}

    public HandlerRegistration addChangeHandler(final ChangeHandler handler) {
       return textBox.addChangeHandler(new ChangeHandler() {

            @Override
            public void onChange(ChangeEvent changeEvent) {
                save();
                handler.onChange(changeEvent);
            }
        });
    }

    public HandlerRegistration addBlurHandler(BlurHandler handler){
        return textBox.addBlurHandler(handler);
    }
    public HandlerRegistration addFocusHandler(FocusHandler handler){
        return textBox.addFocusHandler(handler);
    };

    public void cancelKey() {
        textBox.cancelKey();
    }

    public void setName(String name) {
        textBox.setName(name);
    }

    public String getName() {
        return textBox.getName();
    }

    public void setTabStopOff(){
        textBox.setTabIndex(-1);
    }

    public void setCursorPos(int pos){
        textBox.setCursorPos(pos);
    }

    public void selectAll(){
        textBox.selectAll();
    }
}
