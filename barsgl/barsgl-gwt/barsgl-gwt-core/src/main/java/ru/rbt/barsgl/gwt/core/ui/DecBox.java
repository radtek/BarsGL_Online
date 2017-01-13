package ru.rbt.barsgl.gwt.core.ui;

import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.ui.Widget;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DecBox extends BaseBox<BigDecimal>{
	private int scale = -1;
    //private String decimalSeparator = LocaleInfo.getCurrentLocale().getNumberConstants().decimalSeparator();
    private String decimalSeparator = ".";
    public DecBox(){
        this(BigDecimal.ZERO);
    }
    
    public DecBox(BigDecimal value) {
		super(value);
		addHandler_KeyPress();

	}

    private void addHandler_KeyPress() {
        textBox.addKeyPressHandler(new KeyPressHandler() {
            @Override
            public void onKeyPress(KeyPressEvent event) {
                char ch = event.getCharCode();
                char ds = decimalSeparator.charAt(0);
                boolean isPoint = (ch == ds)
                        && (textBox.getText().indexOf(ds) < 0);

                boolean isSign = (ch == '-')
                        && (textBox.getText().indexOf('-') < 0)
                        && (textBox.getCursorPos() == 0);

                if (!Character.isDigit(ch) && !isSign && !isPoint) {
                    textBox.cancelKey();
                }
            }
        });
    }
    
    public void setScale(int scale){
        this.scale = scale < -1 ? -1 : scale;
        setValue(value);
    }

    public int getScale(){
        return scale;
    }
    
	@Override
	public void setValue(BigDecimal value) {
		if (scale != -1)
			this.value = value != null ? value.setScale(scale, RoundingMode.HALF_UP) : null;
			else
				this.value = value;
		textBox.setText(makeVisibleString());
	}

	@Override
	public BigDecimal getValue() {
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

	@Override
	protected String makeVisibleString() {
		 return value != null ? value.toPlainString() : "";
	}

	@Override
	protected void saveValue() {
		setValue(textBox.getText().trim().isEmpty() ? null : new BigDecimal(textBox.getText().trim()));
	}

	@Override
	protected boolean validate(String text) {
		String tmp = text.trim();
        if (tmp.isEmpty()) return true;

        try {
            new BigDecimal(tmp);

            return true;

        } catch (Exception e) {
            return false;
        }
	}
}
