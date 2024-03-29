package ru.rbt.security.gwt.client.security;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.dialogs.DialogManager;
import ru.rbt.shared.LoginResult;


/**
 * Created by Ivan Sevastyanov
 */
public class LoginFormHandler {

    private LoginForm loginForm;

    public LoginFormHandler(LoginForm loginForm) {
        this.loginForm = loginForm;
    }

    public void login() {
        loginForm.disableFields();

        SecurityEntryPoint.authSrv.login(loginForm.getUserName(), loginForm.getPassword()
                , new AsyncCallback<LoginResult>() {
            @Override
            public void onFailure(Throwable caught) {
                Window.alert("ERROR on login:\n" + caught.getMessage());
                loginForm.enableFields();
            }

            @Override
            public void onSuccess(LoginResult result) {
                if (LoginResult.LoginResultStatus.FAILED == result.getLoginResultStatus()) {
                   // Window.alert(result.getMessage());
                    DialogManager.error("Доступ", result.getMessage());
                    loginForm.enableFields();
                } else {
                    loginForm.hide();
                    LoginForm.setIsLoginFormInquired(false);
                    SecurityEntryPoint.prepare(result);
                    if (LoginResult.LoginResultStatus.LIMIT == result.getLoginResultStatus()){
                        DialogManager.error("Доступ", "ВНИМАНИЕ!!! Установлен режим ограниченного доступа");
                    }

                }
                if (loginForm.isSaveUser()) {
                    loginForm.saveCookies();
                } else {
                    loginForm.removeCookies();
                }
            }
        });
    }

    public static void logoff() {
        SecurityEntryPoint.authSrv.logoff("", new AsyncCallback<LoginResult>() {
            @Override
            public void onFailure(Throwable caught) {
                Window.alert("ERROR on logoff:\n" + caught.getMessage());
            }

            @Override
            public void onSuccess(LoginResult result) {
                LocalDataStorage.clear();
                RootPanel.get().clear();
                if (LoginResult.LoginResultStatus.FAILED == result.getLoginResultStatus()) {
                    Window.alert(result.getMessage());
                }
                else {
                	SecurityEntryPoint.checkSession();
                }
            }
        });
    }
}
