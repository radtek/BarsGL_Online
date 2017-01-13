package ru.rbt.barsgl.gwt.core.ui;

import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.ui.Widget;

public class IntBox extends BaseBox<Integer>  {

	public IntBox(){
		this(0);
	}
	
	public IntBox(Integer value) {
		super(value);
		addHandler_KeyPress();
	}

	private void addHandler_KeyPress() {
        textBox.addKeyPressHandler(new KeyPressHandler() {
            @Override
            public void onKeyPress(KeyPressEvent event) {
                char ch = event.getCharCode();

                boolean isSign = (ch == '-')
                        && (textBox.getText().indexOf('-') < 0)
                        && (textBox.getCursorPos() == 0);

                if (!Character.isDigit(ch) && !isSign) {
                    textBox.cancelKey();
                }
            }
        });
    }
	
	@Override
	protected Widget configure() {		
		return createTextBox();
	}

	@Override
	public void setValue(Integer value) {
		this.value = value;
        textBox.setText(makeVisibleString());		
	}

	@Override
	public Integer getValue() {
		 return value;
	}

	@Override
	public boolean hasValue() {
		return value != null;
	}
	
	@Override
	protected String makeVisibleString() {		
		return value != null ? value.toString() : "";
	}
	
	@Override
	protected boolean validate(String text) {
		String tmp = text.trim();
		if (tmp.isEmpty()) return true;

		try {

			Integer.parseInt(tmp);

			return true;

		} catch (Exception e) {
			return false;
		}
	}

	@Override
	protected void saveValue() {		
		value = textBox.getText().trim().isEmpty() ? null : Integer.parseInt(textBox.getText().trim());
	}
}
