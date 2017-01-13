package ru.rbt.barsgl.gwt.core.ui;

import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.ui.Widget;

public class TxtBox extends BaseBox<String>{

	public TxtBox(){
        this(null);
    }
	
	public TxtBox(String value) {
		super((value == null || value.isEmpty()) ? null : value);
		textBox.addHandler_KeyPressMask();
	}

	@Override
	public void setValue(String value) {
		this.value = (value == null || value.isEmpty()) ? null : value;
        textBox.setText(makeVisibleString());
	}

	@Override
	public String getValue() {		
		return value;
	}

	@Override
	public boolean hasValue() {
		return value != null;
	}

	@Override
	protected Widget configure() {		
		return createTextBox();
	}
	
	public void setMask(String mask){
		textBox.setValidRegMask(mask);
	}


	@Override
	protected String makeVisibleString() {		
		return value == null ? "" : value;
	}

	@Override
	protected void saveValue() {	
		value = textBox.getText().isEmpty() ? null : textBox.getText();
	}

	@Override
	protected boolean validate(String text) {
		if (text.isEmpty()) return true;

        String mask = textBox.getValidRegMask();
        if (mask.isEmpty()) return true;

        char[] ch = text.toCharArray();
        try {
            RegExp p = RegExp.compile(mask);                       
            for(char c: ch)
            {
                if (!p.test(Character.toString(c))) return false;
            }

            return true;

        } catch (Exception e) {
            return false;
        }
	}
}
