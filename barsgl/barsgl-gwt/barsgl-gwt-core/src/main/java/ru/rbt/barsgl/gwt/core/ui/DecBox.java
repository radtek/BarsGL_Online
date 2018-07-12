package ru.rbt.barsgl.gwt.core.ui;

import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DecBox extends BaseBox<BigDecimal>{
	private int scale = -1;
	//    private String decimalSeparator = ".";
	private char ds = LocaleInfo.getCurrentLocale().getNumberConstants().decimalSeparator().charAt(0);
	private final String separators = ".,юб<>/?";
	private String decFmt = ".00000000000000000000000000000000";

	public DecBox(){
		this(null);
	}

	public DecBox(BigDecimal value) {
		super(value);
		addHandler_KeyPress();
		addHandler_OnFocus();
	}

	private void addHandler_KeyPress() {
		textBox.addKeyPressHandler(new KeyPressHandler() {
			@Override
			public void onKeyPress(KeyPressEvent event) {
				char ch = event.getCharCode();

				String val = textBox.getText();
				int pos = textBox.getCursorPos();
				int indPoint = val.indexOf(ds);
				boolean digitOk = Character.isDigit(ch); // && (indPoint < 0 || pos <= indPoint || val.length() - indPoint <= scale);
				boolean pointEn = (indPoint < 0); // && (val.length() - pos <= scale);
				boolean pointOk = pointEn && (ds == ch);
				if (!digitOk && !pointOk || textBox.getMaxLength() > 0 && val.length() >= (textBox.getMaxLength())) {
					textBox.cancelKey();
					if (pointEn && separators.indexOf(ch) >= 0 ) {
						textBox.setText(val.substring(0, pos) + ds + val.substring(pos, val.length()));
						textBox.setCursorPos(pos+1);
					}
				}
			}
		});
	}

	private void addHandler_OnFocus() {
		textBox.addFocusHandler(new FocusHandler() {
			@Override
			public void onFocus(FocusEvent focusEvent) {
				textBox.setText(value != null && value.compareTo(BigDecimal.ZERO) != 0 ? getEditValue() : "");
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
		return value != null ? getViewValue() : "";
	}

	@Override
	protected void saveValue() {
		setValue(textBox.getText().trim().isEmpty() ? null : new BigDecimal(getNormalizedText()));
	}

	@Override
	protected boolean validate(String text) {
		String tmp = getNormalizedText();
		if (tmp.isEmpty()) return true;

		try {
			new BigDecimal(tmp);

			return true;

		} catch (Exception e) {
			return false;
		}
	}

	private String getNormalizedText() {
		return textBox.getText().trim().replace(",", ".").replace("\u00A0", "");
	}

	private String getViewValue() {
    	final String fmt = "#,##0";
		if (scale == 0) {
			return NumberFormat.getFormat(fmt).format(value);
		} else if (scale > 0) {
			return NumberFormat.getFormat(fmt + decFmt.substring(0, scale + 1)).format(value);
		} else {
    		return value.toPlainString();
		}
	}

	private String getEditValue() {
		final String fmt = "0";
		if (scale == 0) {
			return NumberFormat.getFormat(fmt).format(value);
		} else if (scale > 0) {
			return NumberFormat.getFormat(fmt + decFmt.substring(0, scale + 1)).format(value);
		} else {
			return value.toPlainString();
		}
	}

}
