package ru.rbt.security.gwt.client.security;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import ru.rbt.security.gwt.client.formmanager.FormManagerUI;
import ru.rbt.barsgl.gwt.core.ui.FieldTextBox;
import ru.rbt.barsgl.gwt.core.ui.PasswordTextField;

import java.util.Date;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;

public class LoginForm {

    /**
     * Наименование cookie: Имя пользователя
     */
    private static final String COOKIE_LOGIN = "fw_userName";
    /**
     * Наименование cookie: Сохранить данные авторизации
     */
    private static final String COOKIE_ISSAVE = "fw_isSave";
    /**
     * Таблица
     */
    private final FlexTable table;
    /**
     * Поле ввода логина
     */
    private final FieldTextBox textBoxLogin;
    /**
     * Поле ввода пароля
     */
    private final PasswordTextField textBoxPassword;
    /**
     * Кнопка "ОК"
     */
    private final Button buttonLogin;
    /**
     * Флажок "Сохранить данные авторизации"
     */
    private final CheckBox checkSaveUser;
    /**
     * Диалоговое окно
     */
    private DialogBox loginDialog;

    private static boolean isLoginFormInquired = false;

    /**
     * Конструктор формы
     *
     */
    public LoginForm() {
        FormManagerUI.setBrowserWindowTitle(TEXT_CONSTANTS.window_title());
        loginDialog = new DialogBox(false, true);
        loginDialog.setGlassEnabled(true);
        loginDialog.setText(TEXT_CONSTANTS.logFrm_info());

        this.table = new FlexTable();
        this.table.setSize("200px", "100px");

        String user = Cookies.getCookie(COOKIE_LOGIN) == null ? "" : Cookies
                .getCookie(COOKIE_LOGIN);

        this.textBoxLogin = new FieldTextBox(user, 20, false) {
            @Override
            public void doOnBlur() {
            }
        };
        this.textBoxPassword = new PasswordTextField(user, 20, false) {
            @Override
            public void doOnBlur() {
            }
        };

        this.textBoxLogin.setWidth("100px");
        this.textBoxPassword.setWidth("100px");

        this.buttonLogin = new Button(TEXT_CONSTANTS.btn_login());
        this.buttonLogin.setWidth("100px");
        this.table.setText(2, 0, TEXT_CONSTANTS.logFrm_login());
        this.table.setWidget(2, 1, this.textBoxLogin);

        this.table.setText(3, 0, TEXT_CONSTANTS.logFrm_password());
        this.table.setWidget(3, 1, this.textBoxPassword);

        this.table.getFlexCellFormatter().setColSpan(0, 0, 2);
        this.table.getFlexCellFormatter().setColSpan(5, 0, 2);

        this.table.setWidget(5, 0, this.buttonLogin);

        this.buttonLogin.setStyleName("login-button");
        this.table.getFlexCellFormatter().setHeight(1, 0, "10px");

        this.table.getFlexCellFormatter().setWordWrap(2, 0, false);
        this.table.getFlexCellFormatter().setWordWrap(3, 0, false);

        this.table.getFlexCellFormatter().setStyleName(2, 0, "filter-td");
        this.table.getFlexCellFormatter().setStyleName(3, 0, "filter-td");
        this.table.getFlexCellFormatter().setStyleName(4, 0, "filter-td");

        this.table.getFlexCellFormatter().setStyleName(4, 1, "filter-td");

        this.checkSaveUser = new CheckBox(TEXT_CONSTANTS.logFrm_save_data());


        String isCheck = Cookies.getCookie(COOKIE_ISSAVE);
        this.checkSaveUser.setValue(isCheck != null);

        this.table.setWidget(4, 1, this.checkSaveUser);

        Window.enableScrolling(false);
        Window.addResizeHandler(new ResizeHandler() {
            @Override
            public void onResize(ResizeEvent event) {
                if(loginDialog != null && loginDialog.isShowing()) {
                    loginDialog.center();
                }
            }
        });

        loginDialog.add(table);
    }

    public static boolean isLoginFormInquired() {
        return isLoginFormInquired;
    }

    public static void setIsLoginFormInquired(boolean isLoginFormInquired) {
        LoginForm.isLoginFormInquired = isLoginFormInquired;
    }

    /**
     * Установить фокус на поле ввода логина
     */
    public void focus() {
        this.textBoxLogin.setFocus(true);
    }

    /**
     * Показать
     */
    public void show() {
        loginDialog.center();
        loginDialog.show();
    }

    public void hide() {
        loginDialog.hide();
    }

    public void disableFields () {
        buttonLogin.setEnabled(false);
        textBoxLogin.setEnabled(false);
        textBoxPassword.setEnabled(false);
        checkSaveUser.setEnabled(false);
    }

    public void enableFields() {
        buttonLogin.setEnabled(true);
        textBoxLogin.setEnabled(true);
        textBoxPassword.setEnabled(true);
        checkSaveUser.setEnabled(true);
    }

    public void setEnterHandler(final LoginFormHandler loginFormHandler) {
        this.buttonLogin.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                loginFormHandler.login();
            }
        });
        this.textBoxLogin.addKeyPressHandler(new KeyPressHandler() {
            @Override
            public void onKeyPress(KeyPressEvent event) {
                if (13 == event.getNativeEvent().getKeyCode()) {
                    loginFormHandler.login();
                }
            }
        });
        this.textBoxPassword.addKeyPressHandler(new KeyPressHandler() {
            @Override
            public void onKeyPress(KeyPressEvent event) {
                if (13 == event.getNativeEvent().getKeyCode()) {
                    loginFormHandler.login();
                }
            }
        });
    }

    public String getUserName() {
        return textBoxLogin.getText();
    }

    public String getPassword() {
        return textBoxPassword.getText();
    }

    public boolean isSaveUser() {
        return checkSaveUser.getValue();
    }

    public void saveCookies() {
        Date d = new Date();
        // todo избавиться от этого
        Date dt = new Date(d.getYear(), d.getMonth() + 1, d.getDay());
        Cookies.setCookie(COOKIE_LOGIN, textBoxLogin.getText(), dt);
        Cookies.setCookie(COOKIE_ISSAVE, "1", dt);
    }

    public void removeCookies() {
        Cookies.removeCookie(COOKIE_LOGIN);
        Cookies.removeCookie(COOKIE_ISSAVE);
    }
}