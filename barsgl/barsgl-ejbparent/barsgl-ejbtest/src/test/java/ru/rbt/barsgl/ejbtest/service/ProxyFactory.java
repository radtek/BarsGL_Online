package ru.rbt.barsgl.ejbtest.service;

import ru.rbt.barsgl.ejbcore.remote.ServerAccess;

import static java.lang.reflect.Proxy.newProxyInstance;

/**
 * Created by Ivan Sevastyanov
 */
public class ProxyFactory {

    public static <T> T createProxy(String className, Class<? extends T> intrfce, ServerAccess remoteAccess) {
        return (T) newProxyInstance(intrfce.getClassLoader()
                , new Class[]{intrfce}, (proxy, method, args) -> remoteAccess.invoke(className, method.getName(), args));
    }

}
