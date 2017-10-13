package ru.rbt.barsgl.gwt.core.ui;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.TimeZone;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.datepicker.client.DatePicker;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;

import java.util.Date;

public class DatePickerBox extends BaseBox<Date> implements HasAllFocusHandlers, HasCloseHandlers<PopupPanel> {
	public static final DateTimeFormat DATE_FORMAT = DateTimeFormat.getFormat("dd.MM.yyyy");
	
	private  DatePicker datePicker;
	private  PopupPanel popupPanel;	
	private  PushButton button;

	private TimeZone timeZone;

	public DatePickerBox(){
		this(new Date());
	}

	public DatePickerBox(Date value) {
		this(value, null);
	}

	public DatePickerBox(Date value, TimeZone tz) {
		super(value);
		this.timeZone = tz;

		addHandler_Click();
		addHandler_Change();
		addHandler_KeyPress();
	}

	private void setDatePickerDate() {
		try {
			if (validate()) {
				value = DATE_FORMAT.parse(textBox.getText().trim());
			}
			else{
				value = new Date();
			}
			datePicker.setValue(value, true);
			textBox.setText(DATE_FORMAT.format(value));

		} catch (Exception e) {
			value = new Date();
			datePicker.setValue(value, true);
			textBox.setText("");
		}
	}
	
	private void addHandler_Click() {
		button.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				setDatePickerDate(); 

				datePicker.setYearAndMonthDropdownVisible(true);
				datePicker.setCurrentMonth(value);
				popupPanel.show();
				int x = textBox.getAbsoluteLeft() + textBox.getOffsetWidth() + 21
						- popupPanel.getOffsetWidth();
				int y = textBox.getAbsoluteTop() + textBox.getOffsetHeight() + 2;

				popupPanel.setPopupPosition(x, y);
			}
		});
	}
	
	private void addHandler_Change() {
		this.datePicker.addValueChangeHandler(new ValueChangeHandler<Date>() {
			@Override
			public void onValueChange(ValueChangeEvent<Date> event) {
				value = event.getValue();
				textBox.setFocus(true);
				textBox.setText(DATE_FORMAT.format(value));
				popupPanel.hide();
			}
		});
	}
	
	private void addHandler_KeyPress() {
		textBox.addKeyPressHandler(new KeyPressHandler() {
			@Override
			public void onKeyPress(KeyPressEvent event) {
				char ch = event.getCharCode();
				if ((ch != '.') && (!Character.isDigit(ch))) {
					textBox.cancelKey();
				}
			}
		});
	}
	
	@Override
	public void setValue(Date value) {
		this.value = value;
		textBox.setText(makeVisibleString());		
	}

	public void setValueSrv(Date value) {
		if (null != value) {
			String valueStr = DATE_FORMAT.format(value, timeZone);
			textBox.setText(valueStr);		
			this.value = DATE_FORMAT.parse(valueStr);	// двойное преобразование переводит часовой пояс
		} else {
			setValue(null);
		}
	}

	@Override
	public Date getValue() {
		return value;
	}

	@Override
	public boolean hasValue() {
		return value != null;
	}

	@Override
	protected Widget configure() {
		createTextBox();
		
		textBox.setWidth("75px");
        textBox.setMaxLength(10);
        
		button = new PushButton(new Image(ImageConstants.INSTANCE.calendar()));
		button.setWidth("16px");
		button.setHeight("16px");   

		popupPanel = new PopupPanel(true);
		datePicker = new DatePicker();
		
		popupPanel.add(datePicker);
		popupPanel.setStyleName("dateBoxPicker_panel");

		Grid grid = new Grid(1, 2);
		grid.setCellSpacing(0);
		grid.setWidget(0, 0, textBox);
		grid.setWidget(0, 1, button);

		return grid;
	}

	@Override
	public void setWidth(String width) {
		// TODO надо сделать нормально!
		if (button.isVisible() && (width.length() > 2) && (width.substring(width.length()-2).equals("px"))) {
			int w = Integer.parseInt(width.substring(0, width.length()-2)) - 31;
			String wid = Integer.toString(w) + "px";
			textBox.setWidth(wid);
		} else {
			textBox.setWidth(width);
		}
	}

	@Override
	protected String makeVisibleString() {
		return value != null ? DATE_FORMAT.format(value) : "";
	}

	@Override
	protected void saveValue() {
		value =  textBox.getText().trim().isEmpty() ? null : DATE_FORMAT.parse(textBox.getText().trim());
	}

	@Override
	protected boolean validate(String text) {
		String tmp = text.trim();
		if (tmp.isEmpty()) return true;

		if (tmp.length() != 10) return false; 
		try {
			RegExp p = RegExp.compile("(\\d|0[1-9]|[12][0-9]|3[01])[- /.](\\d|0[1-9]|1[012])[- /.](19|20)\\d\\d");
			if (! p.test(textBox.getText())) return false;

			DATE_FORMAT.parse(tmp);

			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public HandlerRegistration addCloseHandler(CloseHandler<PopupPanel> handler) {
		return popupPanel.addCloseHandler(handler);
	}

	@Override
	public void setEnabled(boolean enabled) {		
		super.setEnabled(enabled);
		
		button.setVisible(true);
		button.setEnabled(enabled);	
	}
	
	@Override
	public void setReadOnly(boolean readOnly){
		super.setReadOnly(readOnly);			
		button.setVisible(!readOnly);		
	}

	public void setButtonVisible(boolean visible){
		button.setVisible(visible);
	}

	public String getText() {
		return textBox.getText();
	}
}
