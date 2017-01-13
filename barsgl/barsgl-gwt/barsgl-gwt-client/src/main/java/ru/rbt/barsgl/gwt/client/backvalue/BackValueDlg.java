package ru.rbt.barsgl.gwt.client.backvalue;

import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.check;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import ru.rbt.barsgl.gwt.client.check.CheckNotNullDate;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterShowEvent;
import ru.rbt.barsgl.gwt.core.ui.DatePickerBox;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.shared.ClientDateUtils;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.access.PrmValueWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.PrmValueEnum;

public class BackValueDlg extends DlgFrame implements IAfterShowEvent{
	private TxtBox tbLogin;
	private TxtBox tbUser;
	private TxtBox tbDays;
	private CheckBox cbExpirationDate;
	private DatePickerBox dpStartDate;
	private DatePickerBox dpFinishDate;


	private PrmValueWrapper wrapper;

	@Override
	public Widget createContent() {
		FlexTable table = new FlexTable();
		FlexCellFormatter fmt =  table.getFlexCellFormatter();

		table.setWidget(0, 0, new Label("Пользователь"));

		tbLogin = new TxtBox();
		tbLogin.setWidth("100px");
		tbLogin.setReadOnly(true);
		tbLogin.setTabStopOff();
		table.setWidget(0, 1, tbLogin);

		fmt.setColSpan(0, 2, 2);
		tbUser = new TxtBox();
		tbUser.setWidth("260px");
		tbUser.setReadOnly(true);
		tbUser.setTabStopOff();
		table.setWidget(0, 2, tbUser);

		table.setWidget(1, 0, new Label("Количество дней назад"));

		tbDays = new TxtBox();
		tbDays.setMaxLength(3);
		tbDays.setWidth("30px");
		tbDays.setMask("[0-9]");
		table.setWidget(1, 1, tbDays);

		table.setWidget(1, 2, new Label("Дата начала действия"));

		dpStartDate = new DatePickerBox();
		dpStartDate.setWidth("100px");
		table.setWidget(1, 3, dpStartDate);

		fmt.setColSpan(2, 0, 2);

		table.setWidget(2,0, cbExpirationDate = new CheckBox("Срок действия не ограничен"));
		cbExpirationDate.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				if (cbExpirationDate.getValue()) {
					dpFinishDate.clear();
				} else{
					dpFinishDate.setValue(ClientDateUtils.String2Date(wrapper.getOperDayDateStr()));
				}
				dpFinishDate.setEnabled(!cbExpirationDate.getValue());
			}
		});

		table.setWidget(2, 1, new Label("Дата окончания действия"));

		dpFinishDate = new DatePickerBox();
		dpFinishDate.setWidth("100px");
		table.setWidget(2, 2, dpFinishDate);

		setAfterShowEvent(this);

		return table;
	}

	@Override
	public void afterShow() {
		tbDays.setFocus(true);
	}

	private void clear(){
		tbDays.clear();
		tbLogin.clear();
		tbUser.clear();
		dpFinishDate.clear();
		dpFinishDate.setEnabled(true);
		dpStartDate.clear();
		cbExpirationDate.setValue(false);
	}

	@Override
	protected void fillContent() {
		Object[] obj =  (Object[])params;
		wrapper = (PrmValueWrapper) obj[0];
		clear();

		tbLogin.setValue((String)obj[1]);
		String fio;

		String f1 = (String)obj[2];
		String f2 = (String)obj[3];
		String f3 = (String)obj[4];

		if (f3 == null || f3.trim().length() == 0){
			fio = f1 + " " + f2;
		} else {
			fio = f1 + " " + f2.charAt(0) + "." + f3.charAt(0) + ".";
		}

		tbUser.setValue(fio);
		tbDays.setValue(wrapper.getPrmValue());
		dpStartDate.setValue(ClientDateUtils.String2Date(wrapper.getDateBeginStr()));
		dpFinishDate.setValue(ClientDateUtils.String2Date(wrapper.getDateEndStr()));

		if (wrapper.getAction() == FormAction.UPDATE && dpFinishDate.getValue() == null){
			cbExpirationDate.setValue(true);
			dpFinishDate.setEnabled(false);
		}
	}

	private void setWrapperFields(){
		wrapper.setPrmCode(PrmValueEnum.BackValue);
		wrapper.setDateBeginStr(ClientDateUtils.Date2String(check(dpStartDate.getValue(),
				"Дата начала действия", "поле не заполнено", new CheckNotNullDate())));
		wrapper.setDateEndStr(ClientDateUtils.Date2String(dpFinishDate.getValue()));

		try {
			if (dpFinishDate.getValue() != null && dpStartDate.getValue().compareTo(dpFinishDate.getValue()) == 1) throw new Exception("Дата окончания действия не может быть меньше даты начала");
		}catch (Exception e){
			showInfo("Ошибка", e.getMessage());
			throw new IllegalArgumentException("column");
		}

		try{
			int days = Integer.parseInt(tbDays.getValue());
			if (days > 365 || days < 0) throw new Exception("Допустимые значения 0-365");
			wrapper.setPrmValue(((Integer)days).toString());
		}catch(Exception e){
			showInfo("Ошибка", Utils.Fmt("Неверное значение в поле 'Количество дней назад'. {0}", e.getMessage()));
			throw new IllegalArgumentException("column");
		}
	}

	@Override
	protected boolean onClickOK() throws Exception {
		try {
			setWrapperFields();
			params = wrapper;
		} catch (IllegalArgumentException e) {
			if (e.getMessage() != null && e.getMessage().equals("column")) {
				return false;
			} else {
				throw e;
			}
		}

		return true;
	}
}
