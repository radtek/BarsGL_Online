package ru.rbt.barsgl.ejbtest.service;

/**
 * Created by Ivan Sevastyanov
 */
public interface ServiceAccessSupport {
    <T> T invoke(Class clazz, String method, Object ... params);
    <T> T invoke(String clazzName, String method, Object ... params);
}
