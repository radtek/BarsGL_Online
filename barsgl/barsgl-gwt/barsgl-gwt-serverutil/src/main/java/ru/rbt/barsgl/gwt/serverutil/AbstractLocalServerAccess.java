package ru.rbt.barsgl.gwt.serverutil;

import ru.rbt.barsgl.ejbcore.remote.ServerAccess;
import ru.rbt.barsgl.shared.LoginParams;
import ru.rbt.barsgl.shared.LoginResult;
import ru.rbt.barsgl.shared.NotAuthorizedUserException;
import ru.rbt.barsgl.shared.ctx.UserRequestHolder;

import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;
import static ru.rbt.barsgl.shared.enums.AuthorizationInfoPath.USER_LOGIN_RESULT;
import static ru.rbt.barsgl.shared.enums.AuthorizationInfoPath.USER_NAME;

/**
 * Created by Ivan Sevastyanov
 */
public abstract class AbstractLocalServerAccess implements ServerAccess {

    public static final String LOGIN_BEAN_NAME = "AuthorizationServiceSupport";
    public static final String LOGIN_BEAN_METHOD = "login";
    public static final String LOGOFF_BEAN_METHOD = "logoff";

    public static final Logger logger = Logger.getLogger(AbstractLocalServerAccess.class.getName());

    /**
     * Проверяем при каждом вызове серверного метода, только если это не попытка входа в систему
     * @param bean название серверного бина
     * @param method название метода
     * @throws NotAuthorizedUserException
     */
    protected void checkSession(String bean, String method) throws NotAuthorizedUserException {
        if (LOGIN_BEAN_NAME.equals(bean)
                && (LOGIN_BEAN_METHOD.equals(method) || LOGOFF_BEAN_METHOD.equals(method))) {
            return;
        }
        if (null == GwtServerUtils.getRequest()) {
            return;
        }
        if (null == getLoginParams()) {
            throw new NotAuthorizedUserException();
        }
    }

    protected LoginParams getLoginParams() {
        LoginParams params;
        try {
            params = (LoginParams) GwtServerUtils.getRequest().getSession(true).getAttribute(USER_NAME.getPath());
            if (null == params || params.getUserName().trim().isEmpty()) {
                params = null;
            }
        } catch (Throwable e) {
            logger.log(Level.WARNING, format("Error on check authorization: '%s:%s'"
                    , e.getClass().getName(), e.getMessage()));
            params = null;
        }
        return params;
    }

    protected LoginResult getLoginResult() {
        LoginResult params;
        try {
            params = (LoginResult) GwtServerUtils.getRequest().getSession(true).getAttribute(USER_LOGIN_RESULT.getPath());
            if (null == params || params.getUserName().trim().isEmpty()) {
                params = null;
            }
        } catch (Throwable e) {
            logger.log(Level.WARNING, format("Error on check authorization: '%s:%s'"
                    , e.getClass().getName(), e.getMessage()));
            params = null;
        }
        return params;
    }

    protected boolean setRequestContext() {
        LoginParams params = getLoginParams();
        LoginResult result = getLoginResult();
        if (null != params && null != result) {
            UserRequestHolder requestHolder = new UserRequestHolder(params.getUserName(), params.getHostName());
            requestHolder.setUserWrapper(result.getUser());
            GwtServerUtils.setUserRequest(requestHolder);
            return true;
        } else {
            return false;
        }
    }

}
