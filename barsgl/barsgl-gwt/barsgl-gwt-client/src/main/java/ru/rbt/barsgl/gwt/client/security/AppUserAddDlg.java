package ru.rbt.barsgl.gwt.client.security;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import ru.rbt.barsgl.gwt.client.check.CheckNotEmptyString;
import ru.rbt.barsgl.gwt.client.comp.DataListBox;
import ru.rbt.barsgl.gwt.client.dict.dlg.EditableDialog;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterShowEvent;
import ru.rbt.barsgl.gwt.core.ui.DatePickerBox;
import ru.rbt.barsgl.gwt.core.ui.PasswordTextField;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.shared.ClientDateUtils;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.user.AppUserWrapper;

import java.util.Date;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.createDepartmentListBox;
import static ru.rbt.barsgl.gwt.client.comp.GLComponents.createFilialListBox;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.check;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;
import static ru.rbt.barsgl.shared.enums.UserExternalType.E;
import static ru.rbt.barsgl.shared.enums.UserExternalType.L;
import static ru.rbt.barsgl.shared.enums.UserLocked.N;
import static ru.rbt.barsgl.shared.enums.UserLocked.Y;
import static ru.rbt.barsgl.shared.enums.YesNoType.Yes;


/**
 * Created by ER18837 on 29.09.15.
 */
public class AppUserAddDlg extends EditableDialog<AppUserWrapper> implements IAfterShowEvent {
    private final String fakePassword = "~/`:^.|,';"; 

	public final static String EDIT = "Редактирование пользователя";
    public final static String CREATE = "Ввод нового пользователя";
    public final static String DELETE = "Удаление пользователя";

    private RadioButton rbLocalUser;
    private RadioButton rbAdUser;
    private CheckBox cbLocked;
    
    private TxtBox txtUserName;
    private PasswordTextField txtUserPassword;
    private PasswordTextField txtUserPassword2;

    private TxtBox txtSurname;
    private TxtBox txtFirstName;
    private TxtBox txtPatronymic;

    private TxtBox txtDateInput;    
    private DatePickerBox dpDateClose;
    
    private DataListBox dlbBranchList;
    private DataListBox dlbFilialList;

    private String pwdMD5;

    public AppUserAddDlg(String caption, FormAction action, Columns columns) {
        super(columns, action);
        setCaption(caption);
    }

    @Override
    public Widget createContent() {
        FlexTable table = new FlexTable();
        FlexCellFormatter fmt = table.getFlexCellFormatter();
    	    	
    	HorizontalPanel userTypePanel = new HorizontalPanel();
    	userTypePanel.setSpacing(5);
    	userTypePanel.add(rbLocalUser = new RadioButton("user_type", "локальный"));
    	userTypePanel.add(rbAdUser = new RadioButton("user_type", "внешний AD"));
    	
    	rbLocalUser.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                txtUserPassword.setEnabled(true);
                txtUserPassword2.setEnabled(true);
            }
        });
    
    	rbAdUser.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                txtUserPassword.setEnabled(false);
                txtUserPassword2.setEnabled(false);
            }
        });
    	    	
    	rbLocalUser.setValue(true);


    	CaptionPanel userTypeCaption = new CaptionPanel("Тип доступа");
    	userTypeCaption.add(userTypePanel);
    	
    	fmt.setColSpan(0, 0, 2);
    	table.setWidget(0, 0, userTypeCaption);
    	
        table.setWidget(1, 0, new Label("Логин"));
        
        txtUserName = new TxtBox();        
        txtUserName.setWidth("120px");
        txtUserName.setMaxLength(255);
        table.setWidget(1, 1, txtUserName);

        table.setWidget(2, 0, new Label("Пароль"));
        
        txtUserPassword = new PasswordTextField("", 64, false) {
            @Override
            public void doOnBlur() {
            }
        };        
        txtUserPassword.setWidth("120px");
        table.setWidget(2, 1, txtUserPassword);

        fmt.setWidth(3, 0, "50px"); 
        table.setWidget(3, 0, new Label("Подтверждение пароля"));
        
        txtUserPassword2 = new PasswordTextField("", 64, false) {
            @Override
            public void doOnBlur() {
            }
        };               
        txtUserPassword2.setWidth("120px");          
        table.setWidget(3, 1, txtUserPassword2);
                     
        table.setWidget(4, 0, new Label("Фамилия"));
        
        fmt.setColSpan(4, 1, 3);
        txtSurname = new TxtBox();        
        txtSurname.setWidth("230px");
        txtSurname.setMaxLength(255);
        table.setWidget(4, 1, txtSurname);
                
        table.setWidget(5, 0, new Label("Имя"));
        
        fmt.setColSpan(5, 1, 3);
        txtFirstName = new TxtBox();        
        txtFirstName.setWidth("230px");
        txtFirstName.setMaxLength(255);
        table.setWidget(5, 1, txtFirstName);
              
        table.setWidget(6, 0, new Label("Отчество"));
        
        fmt.setColSpan(6, 1, 3);
        txtPatronymic = new TxtBox();        
        txtPatronymic.setWidth("230px");
        txtPatronymic.setMaxLength(255);
        table.setWidget(6, 1, txtPatronymic);
        
        fmt.setHorizontalAlignment(1, 2, HasHorizontalAlignment.ALIGN_RIGHT);
        table.setWidget(1, 2, new Label("Дата ввода"));
                  
        txtDateInput = new TxtBox();
        txtDateInput.setEnabled(false);        
        txtDateInput.setWidth("112px");
        table.setWidget(1, 3, txtDateInput);
        
        fmt.setHorizontalAlignment(2, 2, HasHorizontalAlignment.ALIGN_RIGHT);
        table.setWidget(2, 2, new Label("Дата закрытия"));
        
        dpDateClose = new DatePickerBox(null);
        dpDateClose.setWidth("112px");     
        dpDateClose.setEnabled(false);
        table.setWidget(2, 3, dpDateClose);
        
        table.setWidget(7, 0, new Label("Филиал"));
        
        fmt.setColSpan(7, 1, 3);
        table.setWidget(7, 1, dlbFilialList = createFilialListBox(null, "347px"));
        
        table.setWidget(8, 0, new Label("Подразделение"));
        
        fmt.setColSpan(8, 1, 3);
        table.setWidget(8, 1, dlbBranchList = createDepartmentListBox(null, "347px", true));

        fmt.setColSpan(9, 0, 2);
        table.setWidget(9, 0, cbLocked = new CheckBox("Заблокирован"));

        setAfterShowEvent(this);
                
        return table;        
    }

    @Override
    public void afterShow() {
        if (action.equals(FormAction.CREATE)){
            txtUserName.setFocus(true);

            txtUserPassword.setEnabled(rbLocalUser.getValue());
            txtUserPassword2.setEnabled(rbLocalUser.getValue());
        }else{
            dpDateClose.setFocus(true);
        }
    }

    @Override
    public void clearContent() {
        txtUserName.setValue(null);    
        txtUserPassword.setValue(null);
        txtUserPassword2.setValue(null);
        txtSurname.setValue(null);
        txtFirstName.setValue(null);
        txtPatronymic.setValue(null);
        txtDateInput.setValue(null);
        dpDateClose.setValue(null);
        rbLocalUser.setValue(true);
        rbAdUser.setValue(false);
        cbLocked.setValue(false);
        dlbBranchList.setSelectValue(null);
        dlbFilialList.setSelectValue(null);
        pwdMD5 = null;
    }

    @Override
    protected void fillContent() {            	
		row = (Row) params;
		clearContent();
		if (action.equals(FormAction.UPDATE)) {
			int ind = columns.getColumnIndexByName("SEC_TYPE");
			if (ind >= 0) {

				if (((String) row.getField(ind).getValue()).toUpperCase().equals(L.name())) {
					rbLocalUser.setValue(true);
					rbAdUser.setValue(false);
				} else {
					rbAdUser.setValue(true);
					rbLocalUser.setValue(false);
				}
			}

			ind = columns.getColumnIndexByName("LOCKED");
			if (ind >= 0) {
				cbLocked.setValue(((String) row.getField(ind).getValue()).toUpperCase().equals(Yes.getLabel().toUpperCase()));
			}

			ind = columns.getColumnIndexByName("CREATE_DT");
			if (ind >= 0) {
				txtDateInput.setValue(DateTimeFormat.getFormat("dd.MM.yyyy hh:mm:ss").format(
                        (Date) row.getField(ind).getValue()));
			}

			ind = columns.getColumnIndexByName("END_DT");
			if (ind >= 0) {
				dpDateClose.setValue(((Date) row.getField(ind).getValue()));
			}

			ind = columns.getColumnIndexByName("USER_NAME");
			if (ind >= 0) {
				txtUserName.setValue(((String) row.getField(ind).getValue()));
			}
			txtUserName.setEnabled(false);

            ind  = columns.getColumnIndexByName("USER_PWD");
            if (ind >= 0){
                pwdMD5 = (String) row.getField(ind).getValue();
            }
            if (pwdMD5 != null){
                txtUserPassword.setValue(fakePassword);
                txtUserPassword2.setValue(fakePassword);
            }

			if (rbLocalUser.getValue()){
				txtUserPassword.setEnabled(true);				
				txtUserPassword2.setEnabled(true);
			}else{			
				txtUserPassword.setEnabled(false);				
				txtUserPassword2.setEnabled(false);
			}
									
			ind = columns.getColumnIndexByName("SURNAME");
			if (ind >= 0) {
				txtSurname.setValue(((String) row.getField(ind).getValue()));
			}

			ind = columns.getColumnIndexByName("FIRSTNAME");
			if (ind >= 0) {
				txtFirstName.setValue(((String) row.getField(ind).getValue()));
			}

			ind = columns.getColumnIndexByName("PATRONYMIC");
			if (ind >= 0) {
				txtPatronymic.setValue(((String) row.getField(ind).getValue()));
			}

			ind = columns.getColumnIndexByName("FILIAL");
			if (ind >= 0) {				
				dlbFilialList.setSelectValue(((String) row.getField(ind).getValue()));
			}

			ind = columns.getColumnIndexByName("DEPID");
			if (ind >= 0) {
				dlbBranchList.setSelectValue(((String) row.getField(ind).getValue()));
			}
			
			dpDateClose.setEnabled(true);
		}		    	    	         
    }

    @Override
    protected AppUserWrapper createWrapper() {
        return new AppUserWrapper();
    }

    @Override
    protected void setFields(AppUserWrapper user) {
        String pwd = txtUserPassword.getText();
        user.setUserName(check(txtUserName.getText(), "Логин", "поле не заполнено", new CheckNotEmptyString()));
        if (rbLocalUser.getValue()) {
            pwd = check(pwd,
                    "Пароль", "поле не заполнено", new CheckNotEmptyString());
        }
        if (!pwd.equals(txtUserPassword2.getText())) {
            showInfo("Ошибка", "Пароли не совпадают!");
            throw new IllegalArgumentException("column");
        }

        user.setSurname(check(txtSurname.getText(),
                "Фамилия", "поле не заполнено", new CheckNotEmptyString()));
        user.setFirstName(check(txtFirstName.getText(),
                "Имя", "поле не заполнено", new CheckNotEmptyString()));
        user.setPatronymic(txtPatronymic.getText());
        
        user.setLocked(cbLocked.getValue() ? Y : N);
        user.setUserType(rbLocalUser.getValue() ? L : E);
        
        user.setCloseDateStr(ClientDateUtils.Date2String(dpDateClose.getValue()));
        
        user.setFilial(check((String) dlbFilialList.getValue(), 
        		"Филиал", "поле не заполнено", new CheckNotEmptyString()));
        user.setBranch((String) dlbBranchList.getValue());

        if (action == FormAction.CREATE){
            //Create
            user.setUserPassword(pwd.trim().isEmpty() ? null : pwd);
        } else{
            //Edit
            user.setPwdMD5(pwdMD5);

            if (pwd.equals(fakePassword)){
                user.setUserPassword(null);
            } else{
                user.setPwdMD5(null);
                user.setUserPassword(pwd.trim().isEmpty() ? null : pwd);
            }
        }
    }	
}
