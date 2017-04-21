/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2017
 */
package ru.rbt.security.gwt.client.security;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;
import ru.rbt.security.gwt.client.formmanager.FormManagerUI;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.SecurityChecker;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import ru.rbt.shared.LoginResult;
import ru.rbt.barsgl.shared.NotAuthorizedUserException;
import static ru.rbt.barsgl.shared.NotAuthorizedUserException.NOT_AUTHORIZED_MESSAGE;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.shared.user.AppUserWrapper;
import ru.rbt.security.gwt.client.formmanager.IMenuBuilder;
import ru.rbt.security.gwt.server.rpc.auth.AuthorizationService;
import ru.rbt.security.gwt.server.rpc.auth.AuthorizationServiceAsync;

/**
 *
 * @author Andrew Samsonov
 */
public class SecurityEntryPoint {

  public static AuthorizationServiceAsync authSrv;

  private static String DATABASE_VERSION;

  private static IMenuBuilder MENU_BUILDER;
  
  public static void init(IMenuBuilder menuBuilder) {
    MENU_BUILDER = menuBuilder;
    authSrv = GWT.create(AuthorizationService.class);

//    checkSession();

//    setDatabaseVersion();

    GWT.setUncaughtExceptionHandler(new DefaultAppUncaughtExceptionHandler());
  }

  public static void checkSession() {
    authSrv.checkSession(createCheckSessionCallback());
  }

  private static AsyncCallback<LoginResult> createCheckSessionCallback() {
    return new AsyncCallback<LoginResult>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert(caught.getMessage());
      }

      @Override
      public void onSuccess(LoginResult result) {
        if (LoginResult.LoginResultStatus.FAILED == result.getLoginResultStatus()) {
          showLoginForm();
        } else {
          LocalDataStorage.clear();
          prepare(result);
        }
      }
    };
  }

  public static void showLoginForm() {
    if (LoginForm.isLoginFormInquired()) {
      return;
    }
    LoginForm.setIsLoginFormInquired(true);
    LoginForm loginForm = new LoginForm();
    loginForm.setEnterHandler(new LoginFormHandler(loginForm));
    loginForm.show();
  }

  public static void prepare(LoginResult result) {
    AppUserWrapper current_user = (AppUserWrapper) LocalDataStorage.getParam("current_user");
    if (current_user != null && result.getUser().getUserName().equals(current_user.getUserName())) {
      FormManagerUI.setBrowserWindowTitle(Utils.Fmt("{0} - [{1}({2})]", TEXT_CONSTANTS.window_title(),
              current_user.getUserName(), current_user.getSurname()));
    } else {
      LocalDataStorage.clear();
      RootPanel.get().clear();
      SecurityChecker.init(result.getAvailableActions());
      LocalDataStorage.putParam("current_user", result.getUser());
      FormManagerUI.setBrowserWindowTitle(Utils.Fmt("{0} - [{1}({2})]", TEXT_CONSTANTS.window_title(),
              result.getUser().getUserName(), result.getUser().getSurname()));
              
      RootPanel.get().add(FormManagerUI.getFormManager(result.getUserMenu(), MENU_BUILDER), 0, 0);
    }
  }

  public static class DefaultAppUncaughtExceptionHandler implements GWT.UncaughtExceptionHandler {

    @Override
    public void onUncaughtException(Throwable throwable) {
      WaitingManager.hide();
      if (isNotAuthorizedUserException(throwable)) {
        showLoginForm();
      } else {
        Window.alert("Операция не удалась.\n Ошибка: " + throwable.getLocalizedMessage());
      }
    }

  }

  public static boolean isNotAuthorizedUserException(Throwable throwable) {
    return (null != throwable && throwable.getLocalizedMessage().contains(NOT_AUTHORIZED_MESSAGE))
            || throwable instanceof NotAuthorizedUserException;
  }

  public static void setDatabaseVersion() {
    authSrv.getDatabaseVersion(new AsyncCallback<RpcRes_Base<String>>() {
      @Override
      public void onFailure(Throwable caught) {
        if (WaitingManager.isWaiting()) {
          WaitingManager.hide();
        }
      }

      @Override
      public void onSuccess(RpcRes_Base<String> result) {
        DATABASE_VERSION = result.getResult();
      }
    });
  }

  public static String getDatabaseVersion() {
    return DATABASE_VERSION;
  }

}
