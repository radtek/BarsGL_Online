package ru.rbt.barsgl.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;
import ru.rbt.barsgl.gwt.client.formmanager.FormManagerUI;
import ru.rbt.barsgl.gwt.client.security.LoginForm;
import ru.rbt.barsgl.gwt.client.security.LoginFormHandler;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.SecurityChecker;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.server.rpc.AuthorizationService;
import ru.rbt.barsgl.gwt.server.rpc.AuthorizationServiceAsync;
import ru.rbt.barsgl.gwt.server.rpc.access.AccessService;
import ru.rbt.barsgl.gwt.server.rpc.access.AccessServiceAsync;
import ru.rbt.barsgl.gwt.server.rpc.asyncGrid.AsyncGridService;
import ru.rbt.barsgl.gwt.server.rpc.asyncGrid.AsyncGridServiceAsync;
import ru.rbt.barsgl.gwt.server.rpc.dict.ManualDictionaryService;
import ru.rbt.barsgl.gwt.server.rpc.dict.ManualDictionaryServiceAsync;
import ru.rbt.barsgl.gwt.server.rpc.job.TimerJobService;
import ru.rbt.barsgl.gwt.server.rpc.job.TimerJobServiceAsync;
import ru.rbt.barsgl.gwt.server.rpc.loader.LoaderControlService;
import ru.rbt.barsgl.gwt.server.rpc.loader.LoaderControlServiceAsync;
import ru.rbt.barsgl.gwt.server.rpc.monitoring.MonitorService;
import ru.rbt.barsgl.gwt.server.rpc.monitoring.MonitorServiceAsync;
import ru.rbt.barsgl.gwt.server.rpc.operation.ManualOperationService;
import ru.rbt.barsgl.gwt.server.rpc.operation.ManualOperationServiceAsync;
import ru.rbt.barsgl.gwt.server.rpc.operday.OperDayService;
import ru.rbt.barsgl.gwt.server.rpc.operday.OperDayServiceAsync;
import ru.rbt.barsgl.gwt.server.rpc.properties.PropertiesService;
import ru.rbt.barsgl.gwt.server.rpc.properties.PropertiesServiceAsync;
import ru.rbt.barsgl.gwt.server.rpc.sync.PdSyncService;
import ru.rbt.barsgl.gwt.server.rpc.sync.PdSyncServiceAsync;
import ru.rbt.barsgl.shared.LoginResult;
import ru.rbt.barsgl.shared.NotAuthorizedUserException;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.user.AppUserWrapper;

import java.util.Date;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.shared.NotAuthorizedUserException.NOT_AUTHORIZED_MESSAGE;

/**
 * Created by ER21006 on 13.01.2015.
 */
public class BarsGLEntryPoint implements EntryPoint {

    public static AuthorizationServiceAsync authSrv;
    public static TimerJobServiceAsync timerJobService;
    public static OperDayServiceAsync operDayService;
    public static AsyncGridServiceAsync asyncGridService;
    public static ManualOperationServiceAsync operationService;
    public static ManualDictionaryServiceAsync dictionaryService;
    public static AccessServiceAsync accessService;
    public static PdSyncServiceAsync pdSyncService;
    public static PropertiesServiceAsync propertiesService;
    public static LoaderControlServiceAsync loaderService;
    public static MonitorServiceAsync monitorService;
    private static String DATABASE_VERSION;
    public static Date CURRENT_WORKDAY;
    
    @Override
    public void onModuleLoad() {
        authSrv = GWT.create(AuthorizationService.class);
        timerJobService = GWT.create(TimerJobService.class);
        operDayService = GWT.create(OperDayService.class);
        asyncGridService = GWT.create(AsyncGridService.class);
        operationService = GWT.create(ManualOperationService.class);
        dictionaryService = GWT.create(ManualDictionaryService.class);
        accessService = GWT.create(AccessService.class);
        pdSyncService = GWT.create(PdSyncService.class);
        propertiesService = GWT.create(PropertiesService.class);
        loaderService = GWT.create(LoaderControlService.class);
        monitorService = GWT.create(MonitorService.class);

        checkSession();
        setDatabaseVersion();

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
        if  (LoginForm.isLoginFormInquired()) {
            return;
        }
        LoginForm.setIsLoginFormInquired(true);
        LoginForm loginForm = new LoginForm();
        loginForm.setEnterHandler(new LoginFormHandler(loginForm));
        loginForm.show();
    }

    public class DefaultAppUncaughtExceptionHandler implements GWT.UncaughtExceptionHandler {

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
        return (null != throwable && throwable.getLocalizedMessage().contains(NOT_AUTHORIZED_MESSAGE)) ||
                throwable instanceof NotAuthorizedUserException;
    }

    private static void setDatabaseVersion() {
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

    public static void prepare(LoginResult result){
        AppUserWrapper current_user = (AppUserWrapper) LocalDataStorage.getParam("current_user");
        if (current_user != null && result.getUser().getUserName().equals(current_user.getUserName())){
            FormManagerUI.setBrowserWindowTitle(Utils.Fmt("{0} - [{1}({2})]", TEXT_CONSTANTS.window_title(),
                    current_user.getUserName(), current_user.getSurname()));
        } else{
            LocalDataStorage.clear();
            RootPanel.get().clear();
            SecurityChecker.init(result.getAvailableActions());
            LocalDataStorage.putParam("current_user", result.getUser());
            FormManagerUI.setBrowserWindowTitle(Utils.Fmt("{0} - [{1}({2})]", TEXT_CONSTANTS.window_title(),
                    result.getUser().getUserName(), result.getUser().getSurname()));

            RootPanel.get().add(FormManagerUI.getFormManager(result.getUserMenu()), 0, 0);
        }
    }
}
