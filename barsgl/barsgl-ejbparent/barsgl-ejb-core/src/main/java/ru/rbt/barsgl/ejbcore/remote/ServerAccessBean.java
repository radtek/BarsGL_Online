package ru.rbt.barsgl.ejbcore.remote;

import org.apache.commons.lang3.reflect.MethodUtils;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.remote.http.ExceptionInfo;
import ru.rbt.barsgl.ejbcore.remote.http.Serializer;
import ru.rbt.barsgl.ejbcore.remote.http.ServiceRequest;
import ru.rbt.barsgl.ejbcore.remote.http.ServiceResponse;
import ru.rbt.barsgl.ejbcore.util.reflection.TypedValue;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;

/**
 * Created by Ivan Sevastyanov
 */
@Stateless(mappedName = "ServerAccessBean")
public class ServerAccessBean implements ServerAccess, ServerAccessEJBLocal, ServerAccessEJBRemote {

    private static final Logger logger = Logger.getLogger(ServerAccessBean.class.getName());

    @Inject
    private Instance<Object> services;

    @Resource
    private SessionContext sessionContext;

    @Override
    public <T> T invoke(Class clazz, String method, Object[] params) {
        final Instance<T> filtered = services.select(clazz);
        for (Object bean : filtered) {
            logger.log(Level.FINE, format("About to check bean '%s' on compatibility to '%s'", bean, clazz));
            if (clazz.isAssignableFrom(bean.getClass())) {
                logger.log(Level.FINE, format("Accepting to bean %s#%s(%s) from remote call", bean, method, null != params ? params : ""));
                try {
                    return (T) MethodUtils.invokeMethod(bean, method, params);
                } catch (Throwable e) {
                    logger.log(Level.SEVERE, "error invoking server bean:", e);
                    throw new DefaultApplicationException(e.getMessage(), e);
                }
            }
        }
        throw new DefaultApplicationException(format("Service for class '%s' has not found on Instance", clazz.getName()));
    }

    @Override
    public <T> T invoke(String className, String method, Object[] params) {
        try {
            Class clazz = Class.forName(className);
            return invoke(clazz, method, params);
        } catch (ClassNotFoundException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public byte[] invoke(byte[] data) {
        try {
            // (0) сохраняем контекст EJB чтобы им можно было воспользоваться на всем протяжении выполнения запроса
            //   ServerContext.setEntityManager(entityManager);
            // (1) десериализуем запрос
            ServiceRequest request = (ServiceRequest) Serializer.readObject(data);
            /*// (2) получаем контекст аутентификации
            authContext = ApplicationContext.getAuthContext();
            if (authContext.getMode() != AuthContextMode.SERVER) {
                throw new AuthException("Invalid authentication context type: " + authContext.getClass().getName());
            }
            authContext.setSessionId(request.getSessionId());

            // (3) обновляем временную метку сессии
            ApplicationContext.getAuthService().pingSession(request.getSessionId());*/

            // (4) вызываем сервис
            /*Object service = ApplicationContext.getService(request.getServiceName());
            if (!ApplicationContext.isRemotelyAccessible(request.getServiceName())) {
                throw new SecurityException("The service is not remotely accessible: " + request.getServiceName());
            }
            if ((authContext.getCurrentSession() == null) && ApplicationContext.isAuthRequired(request.getServiceName())) {
                throw new SecurityException("Valid authentication required to access methods of service '" + request.getServiceName() + "'");
            }

            // (5) устанавливаем парметры клиентского запроса
            ServerContext.setClientParameters(request.getClientParameters());

            ObjectHolder<Object> holder = new ObjectHolder<Object>(service);
            TypedValue result = holder.invokeMethod(service.getClass(), request.getMethodName(), request.getArguments());

            // (6) чтобы сработали ограничения БД раньше, чем automatic commit
            entityManager.flush();*/

            Object result = invoke(request.getServiceName()
                    , request.getMethodName()
                    , convertToParameters(request.getArguments()));

            // (7) serialize result
            return Serializer.writeObject(new ServiceResponse(result == null ? null : result));
        } catch (Throwable th) {
            // откатываем все изменения
            sessionContext.setRollbackOnly();
            // transmit error info to the caller
            ExceptionInfo info = new ExceptionInfo(th);
            try {
                return Serializer.writeObject(new ServiceResponse(info));
            } catch (Throwable th2) {
                String token = "" + System.currentTimeMillis();
                token = token.substring(token.length() - 5);
                logger.log(Level.SEVERE, "!!! WARNING !!! Failed to serialize exception info. See below the original error (" + token + ")", th2);
                logger.log(Level.SEVERE, "INVOCATION ERROR (" + token + ")", th);
                return null;
            }
        } finally {
            // (5) clear authentication context
            /*if (authContext != null) {
                authContext.removeSessionId();
            }*/
            // unset thread-local variables to clean container thread pool
            //   ServerContext.setEntityManager(null);
        }
    }

    private Object[] convertToParameters(TypedValue[] values) {
        if (null == values || 0 == values.length) {
            return new Object[]{};
        } else {
            ArrayList list = new ArrayList(values.length);
            for (TypedValue value : values) {
                list.add(value.getValue());
            }
            return list.toArray();
        }
    }

}
