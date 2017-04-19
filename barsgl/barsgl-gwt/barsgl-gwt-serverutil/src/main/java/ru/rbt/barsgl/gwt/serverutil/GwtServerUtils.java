package ru.rbt.barsgl.gwt.serverutil;

import ru.rbt.barsgl.ejbcore.remote.ServerAccess;
import ru.rbt.barsgl.ejbcore.remote.http.HttpServiceInvokerFactory;
import ru.rbt.barsgl.ejbcore.remote.http.ServiceInvokerFactory;
import ru.rbt.barsgl.ejbcore.security.RequestContextBean;
import ru.rbt.shared.ctx.UserRequestHolder;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import ru.rbt.shared.security.RequestContext;

/**
 * Created by Ivan Sevastyanov
 */
public class GwtServerUtils {

    private static final Logger logger = Logger.getLogger(GwtServerUtils.class.getName());

    private static ServiceInvokerFactory remoteInvokerFactory;

    private static ThreadLocal<HttpServletRequest> threadHttpRequest = new ThreadLocal<>();

    public static ServerAccess findServerAccess() {

        try (InputStream stream = GwtServerUtils.class.getClassLoader().getResourceAsStream("application.properties")){
            // application.properties по факту есть только в сборке для GWT debug
            if (null == stream) {
                return findServerAccessEJB();
            } else {
                if (null == remoteInvokerFactory) {
                    synchronized (GwtServerUtils.class) {
                        if (null == remoteInvokerFactory) {
                            Properties properties = new Properties();
                            properties.load(stream);
                            final String applicatioURL = properties.getProperty("applicationUrl");
                            remoteInvokerFactory = new HttpServiceInvokerFactory(applicatioURL);
                        }
                    }
                }
                return new HttpServerAccess(remoteInvokerFactory.createInvoker());
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Ищем как EJB с проверкой аутентификации
     * @return экземпляр ru.rbt.barsgl.ejbcore.remote.ServerAccess
     * @throws NamingException
     */
    private static ServerAccess findServerAccessEJB () throws NamingException {
        final ServerAccess serverAccess = findJndiReference("java:app/barsgl-ejbcore/ServerAccessBean!ru.rbt.barsgl.ejbcore.remote.ServerAccessEJBRemote");

        return new AbstractLocalServerAccess() {
            @Override
            public <T> T invoke(Class clazz, String method, Object... params) throws Exception{
                checkSession(clazz.getSimpleName(), method);
                try {
                    setRequestContext();
                    return serverAccess.invoke(clazz, method, params);
                } finally {
                    GwtServerUtils.setUserRequest(null);
                }
            }

            @Override
            public <T> T invoke(String className, String method, Object... params) throws Exception{
                try {
                    checkSession(Class.forName(className).getSimpleName(), method);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                try {
                    setRequestContext();
                    return serverAccess.invoke(className, method, params);
                } finally {
                    GwtServerUtils.setUserRequest(null);
                }
            }

            @Override
            public byte[] invoke(byte[] data) throws Exception{
                return serverAccess.invoke(data);
            }
        };
    }
    /**
     * Ищем как EJB без проверки аутентификации
     * @return экземпляр ru.rbt.barsgl.ejbcore.remote.ServerAccess
     * @throws NamingException
     */
    public static ServerAccess findServerAccessEJBNoAuth () {
        try {
            return findJndiReference("ServerAccessBean#ru.rbt.barsgl.ejbcore.remote.ServerAccessEJBRemote");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setRequest(HttpServletRequest request) {
        threadHttpRequest.set(request);
    }

    public static HttpServletRequest getRequest() {
        return threadHttpRequest.get();
    }

    private static <T> T findJndiReference(String jndiName) throws NamingException {
        Context ctx = null;
        try {
            ctx = new InitialContext();
            return (T) ctx.lookup(jndiName);
        } finally {
            try {
                if (null != ctx) ctx.close();
            } catch (NamingException ignore) {
                logger.log(Level.WARNING, "Error on closing javax.naming.Context", ignore);
            }
        }
    }

    public static void setUserRequest(UserRequestHolder holder) {
        // устанавливаем контекст вызова только БЕЗ GWT DEBUG
        try (InputStream stream = GwtServerUtils.class.getClassLoader().getResourceAsStream("application.properties")){
            if (null == stream) {
                //read env-ref in application.xml
                RequestContext contextBean = findJndiReference("java:app/env/ejb/ApplicationRequestContext");
                contextBean.setRequest(holder);
            }
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "Error on installing context", e);
            throw new RuntimeException(e);
        }
    }
}
