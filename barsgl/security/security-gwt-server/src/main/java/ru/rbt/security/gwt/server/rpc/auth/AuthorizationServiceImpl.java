package ru.rbt.security.gwt.server.rpc.auth;


import ru.rbt.gwt.security.ejb.AuthorizationServiceGwtSupport;
import ru.rbt.barsgl.gwt.serverutil.GwtServerUtils;
import ru.rbt.barsgl.shared.LoginParams;
import ru.rbt.shared.LoginResult;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.shared.access.UserMenuWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.shared.ctx.UserRequestHolder;
import ru.rbt.shared.enums.SecurityActionCode;
import ru.rbt.shared.user.AppUserWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import ru.rbt.barsgl.gwt.core.server.rpc.AbstractGwtService;
import ru.rbt.barsgl.gwt.core.server.rpc.RpcResProcessor;

import static ru.rbt.barsgl.shared.enums.AuthorizationInfoPath.USER_LOGIN_RESULT;
import static ru.rbt.barsgl.shared.enums.AuthorizationInfoPath.USER_NAME;

public class AuthorizationServiceImpl extends AbstractGwtService implements AuthorizationService {

    public AuthorizationServiceImpl() {
//        log.info(format("LOG: '%s' Implementation has created: token: '%d'", this.getClass().getName(), token));
    }

    @Override
    public LoginResult login(String user, String password) throws Exception {
        HttpServletRequest request = getThreadLocalRequest();
        GwtServerUtils.setUserRequest(new UserRequestHolder(user, null != request ? request.getRemoteAddr() : ""));
        try {
            LoginResult result = localInvoker.invoke(AuthorizationServiceGwtSupport.class, "login", user, password);
            if (null != request){
                HttpSession httpSession = request.getSession(true);
                LoginParams params = new LoginParams(result.getUserName(), result.getUserType(), request.getRemoteAddr());
                httpSession.setAttribute(USER_NAME.getPath(), params);  // user
                httpSession.setAttribute(USER_LOGIN_RESULT.getPath(), result);  // login result
                return result;
            } else {
                throw new RuntimeException("Request is not initialized");
            }
        } finally {
            GwtServerUtils.setUserRequest(null);
        }
    }

    @Override
    public LoginResult checkSession() {
        HttpSession httpSession = getThreadLocalRequest().getSession();
        if (null == httpSession) {
            return LoginResult.buildFailed("User is NOT authorized");
        } else {
            LoginParams params = (LoginParams) httpSession.getAttribute(USER_NAME.getPath());
            if (null == params || null == params.getUserName()) {
                return LoginResult.buildFailed("User is NOT authorized");
            } else {
                return (LoginResult) httpSession.getAttribute(USER_LOGIN_RESULT.getPath());
            }
        }
    }

    @Override
    public LoginResult logoff(String user) throws Exception {
        HttpSession httpSession = getThreadLocalRequest().getSession(true);
        LoginParams params = (LoginParams) httpSession.getAttribute(USER_NAME.getPath());
        if (params == null) {
            params = new LoginParams("Not authorized user", "no type", "no host");
        }
        
        LoginResult result = localInvoker.invoke(AuthorizationServiceGwtSupport.class, "logoff", params.getUserName());
//        log.info(result.getMessage());
        params.setUserName("");
        httpSession.setAttribute(USER_NAME.getPath(), params);  // user
        httpSession.invalidate();
        return result;
    }

    @Override
    public RpcRes_Base<AppUserWrapper> createUser(final AppUserWrapper wrapper, final FormAction action) throws Exception {
        return new RpcResProcessor<AppUserWrapper>() {
            @Override
            protected RpcRes_Base<AppUserWrapper> buildResponse() throws Throwable {
                String method = "";
                switch(action) {
                    case CREATE:
                        method = "createUser"; break;
                    case UPDATE:
                        method = "updateUser"; break;
                    default:
                        throw new Throwable("Недопустимое действие!");
                }
                RpcRes_Base<AppUserWrapper> res = localInvoker.invoke(AuthorizationServiceGwtSupport.class, method, wrapper);
                if (res == null) throw new Throwable("Не удалось выполнить действие!");
                return res;
            }
        }.process();
    }

    @Override
    public RpcRes_Base<String> getDatabaseVersion() throws Exception {
        return new RpcRes_Base<String>((String)localInvoker.invoke(AuthorizationServiceGwtSupport.class, "getDatabaseVersion"), false, "");
    }

    @Override
    public UserMenuWrapper getUserMenu(String userName) throws Exception {
        return localInvoker.invoke(AuthorizationServiceGwtSupport.class, "getUserMenu", userName);
    }

    @Override
    public SecurityActionCode getSecurityActionCode(String name) throws Exception {
        return SecurityActionCode.valueOf(name);
    }
}
