package ru.rbt.barsgl.gwt.core.ui;

/**
 * Created by akichigi on 29.04.15.
 */
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

public class AreaBox extends Composite implements IBoxValue<String>{
    private final String STYLE_NORMAL = "area_normal";
    private final String STYLE_SELECTED = "area_selected";

    private TextArea textBox;
    private String value;

    public AreaBox(){
        this(null);
    }

    public AreaBox(String value){
        this.value = value;
        initWidget(configure());

        addFocusBlurHandler();
    }

    private Widget configure(){
        textBox = new TextArea();
        setValue(value);

        textBox.setStyleName(STYLE_NORMAL);
        textBox.setSize("250px", "100px");
        return textBox;
    }

    private void addFocusBlurHandler() {
        textBox.addBlurHandler(new BlurHandler() {
            public void onBlur(BlurEvent event) {
                textBox.setStyleName(STYLE_NORMAL);
                value = textBox.getText().isEmpty() ? null : textBox.getText();
            }
        });
        textBox.addFocusHandler(new FocusHandler() {
            public void onFocus(FocusEvent event) {
                textBox.setStyleName(STYLE_SELECTED);
            }
        });
    }

    @Override
    public void setValue(String value) {
        this.value = (value == null || value.isEmpty()) ? null : value;
        textBox.setText(value == null ? "" : value);
        textBox.setFocus(true);
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getText() {
        return value;
    }

    @Override
    public boolean hasValue() {
        return value != null;
    }

    @Override
    public boolean validate() {
        return true;
    }

    @Override
    public String toString(){
        return textBox.getText();
    }

    @Override
    public void setWidth(String width){
        textBox.setWidth(width);
    }

    @Override
    public void setHeight(String height) {
        textBox.setHeight(height);
    }

    @Override
    public void setSize(String width, String height){
        textBox.setSize(width, height);
    }

    public void setFocus(boolean focus) {
        textBox.setFocus(focus);
        if (focus) textBox.setCursorPos(0);
    }

    @Override
    public void setEnabled(boolean enabled) {
        textBox.setEnabled(enabled);
        textBox.getElement().getStyle().setBackgroundColor(enabled ? "white" : "#f3f1e8");
    }

    @Override
    public boolean isEnabled() {
        return textBox.isEnabled();
    }

    public void setReadOnly(boolean readOnly){
    	textBox.setReadOnly(readOnly);
    	textBox.getElement().getStyle().setBackgroundColor(readOnly ? "#f3f1e8" : "white" );
    }
    
    public void clear() {
        textBox.setText("");
        value = null;        
    }

    public void setTextWrap(boolean wrap){
        if (wrap){
        	textBox.getElement().removeAttribute("wrap");            
        }else {
        	textBox.getElement().setAttribute("wrap", "off");
        }
    }
    
    public void addChangeHandler(ChangeHandler handler) {
    	textBox.addChangeHandler(handler);
    }
   
    public void addKeyDownHandler(KeyDownHandler handler) {
    	textBox.addKeyDownHandler(handler);
    }

    public void addKeyPressHandler(KeyPressHandler handler) {
    	textBox.addKeyPressHandler(handler);
    }

}

