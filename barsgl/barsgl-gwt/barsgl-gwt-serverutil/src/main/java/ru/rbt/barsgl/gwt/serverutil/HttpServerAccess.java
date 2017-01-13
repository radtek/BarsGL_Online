package ru.rbt.barsgl.gwt.serverutil;

import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.remote.http.*;
import ru.rbt.barsgl.ejbcore.util.reflection.TypedValue;

import java.util.ArrayList;

/**
 * Created by Ivan Sevastyanov
 */
public class HttpServerAccess extends AbstractLocalServerAccess {

    private ServiceInvoker invoker;

    public HttpServerAccess(ServiceInvoker invoker) {
        this.invoker = invoker;
    }

    @Override
    public <T> T invoke(Class clazz, String method, Object... params) throws Exception {
        checkSession(clazz.getSimpleName(), method);
        final ServiceRequest request = new ServiceRequest(clazz.getName(), method, convertToTypedValues(params), "", new ClientParameters());
        try {
            ServiceResponse response = invoker.invokeService(request);
            if (response.isSuccessful()) {
                return (T) response.getResult();
            } else {
                Throwable th = response.getError().createThrowable();
                throw new DefaultApplicationException(th.getMessage(), th);
            }
        } catch (ServiceInvocationException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    @Override
    public <T> T invoke(String className, String method, Object... params) throws Exception {
        checkSession(className, method);
        try {
            Class clazz = Class.forName(className);
            return invoke(clazz, method, params);
        } catch (ClassNotFoundException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    @Override
    public byte[] invoke(byte[] data) throws Exception {
        throw new DefaultApplicationException("Not implemented");
    }

    private static TypedValue[] convertToTypedValues(Object ... params) {
        if (null == params || 0 == params.length) {
            return new TypedValue[0];
        } else {
            ArrayList<TypedValue> list = new ArrayList(params.length);
            for (Object param : params) {
                if (null != param) {
                    list.add(new TypedValue(param, param.getClass()));
                } else {
                    list.add(new TypedValue());
                }
            }
            return list.toArray(new TypedValue[params.length]);
        }
    }

}
