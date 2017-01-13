package ru.rbt.barsgl.gwt.server.rpc;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import ru.rbt.barsgl.shared.LoginResult;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.access.UserMenuWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.SecurityActionCode;
import ru.rbt.barsgl.shared.user.AppUserWrapper;

/**
 * Created by ER21006 on 13.01.2015.
 */
@RemoteServiceRelativePath("service/AuthorizationService")
public interface AuthorizationService extends RemoteService {

    LoginResult login(String user, String password) throws Exception;
    LoginResult checkSession() throws Exception;
    LoginResult logoff(String user) throws Exception;

    RpcRes_Base<AppUserWrapper> createUser(AppUserWrapper wrapper, FormAction action) throws Exception;
    RpcRes_Base<String> getDatabaseVersion() throws Exception;

    UserMenuWrapper getUserMenu(String userName) throws Exception;
    SecurityActionCode getSecurityActionCode(String name) throws Exception; //Stub
}
