package ru.rbt.barsgl.gwt.serverutil;

import ru.rbt.barsgl.ejbcore.remote.ServerAccess;
import ru.rbt.barsgl.shared.LoginParams;
import ru.rbt.barsgl.shared.NotAuthorizedUserException;
import ru.rbt.shared.LoginResult;
import ru.rbt.shared.ctx.UserRequestHolder;

import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;
import static ru.rbt.barsgl.shared.enums.AuthorizationInfoPath.USER_LOGIN_RESULT;
import static ru.rbt.barsgl.shared.enums.AuthorizationInfoPath.USER_NAME;

/**
 * Created by Ivan Sevastyanov
 */
public abstract class AbstractLocalServerAccess implements ServerAccess {

    public enum TrustedBeanMethods {
        LOGIN_BEAN("AuthorizationServiceGwtSupport", "login")
        , LOGOFF_BEAN("AuthorizationServiceGwtSupport", "logoff")
        , AUDIT_ERROR_BEAN("AuditController", "error")
        , AUDIT_WARN_BEAN("AuditController", "warning")
        , SESS_REGISTRATOR_BEAN("SessionSupportBean", "registerHttpSession")
        , SESS_UNREGISTRATOR_BEAN("SessionSupportBean", "unregisterHttpSession")
        , SESS_CHECKSTORE_BEAN("SessionSupportBean", "checkSessionInStore");

        private final String beanName;
        private final String methodName;

        TrustedBeanMethods(String beanName, String methodName) {
            this.beanName = beanName;
            this.methodName = methodName;
        }

        public String getBeanName() {
            return beanName;
        }

        public String getMethodName() {
            return methodName;
        }
    }

    public static final Logger logger = Logger.getLogger(AbstractLocalServerAccess.class.getName());

    /**
     * Проверяем при каждом вызове серверного метода, только если это не попытка входа в систему
     * @param bean название серверного бина
     * @param method название метода
     * @throws NotAuthorizedUserException
     */
    protected void checkSession(String bean, String method) throws NotAuthorizedUserException {
        if (Arrays.stream(TrustedBeanMethods.values())
                .anyMatch(p -> p.getBeanName().equals(bean) && p.getMethodName().equals(method))) {
            return;
        }
        if (null == GwtServerUtils.getRequest()) {
            return;
        }
        if (null == getLoginParams()) {
            throw new NotAuthorizedUserException();
        }
        invalidateUnregisteredHttpSession();
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

    private HttpSession getSession() {
        return GwtServerUtils.getRequest().getSession();
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

    private void invalidateUnregisteredHttpSession() {
        HttpSession session = getSession();
        if (null != session) {
            try {
                boolean sessionInstore = invoke("ru.rbt.barsgl.ejb.monitoring.SessionSupportBean", "checkSessionInStore", session.getId());
                if (!sessionInstore)
                    session.invalidate();
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

}
