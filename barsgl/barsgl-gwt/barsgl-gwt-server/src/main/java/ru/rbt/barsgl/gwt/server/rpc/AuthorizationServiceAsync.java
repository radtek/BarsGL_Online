package ru.rbt.barsgl.gwt.server.rpc;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.shared.LoginResult;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.access.UserMenuWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.SecurityActionCode;
import ru.rbt.barsgl.shared.user.AppUserWrapper;

/**
 * Created by ER21006 on 13.01.2015.
 */
public interface AuthorizationServiceAsync {

    void login(String user, String password, AsyncCallback<LoginResult> paramAsyncCallback);
    void checkSession(AsyncCallback<LoginResult> paramAsyncCallback);
    void logoff(String user, AsyncCallback<LoginResult> paramAsyncCallback);

    void createUser(AppUserWrapper wrapper, FormAction action, AsyncCallback<RpcRes_Base<AppUserWrapper>> callback);
    void getDatabaseVersion(AsyncCallback<RpcRes_Base<String>> callback);
    void getUserMenu(String userName, AsyncCallback<UserMenuWrapper> callback);
    void getSecurityActionCode(String name, AsyncCallback<SecurityActionCode> callback); //Stub

}
