package ru.rbt.barsgl.ejbcore.util.reflection;


import org.apache.log4j.Logger;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Класс-утилита для вызова методов объекта через reflection
 */
public final class ObjectHolder<T> implements Serializable {

    private final Logger _log;
    private final T _obj;

    /**
     * Создает новый экземпляр  {@link ObjectHolder}, который содержит заданный объект
     * @param obj Объект для инициализации экземпляра {@link ObjectHolder}. Не может быть <code>null</code>
     * @throws IllegalArgumentException Если значение параметра <code>obj</code> есть <code>null</code>
     */
    public ObjectHolder(T obj) {
        if (obj == null) {
            throw new IllegalArgumentException("Object cannot be null");
        }
        _obj = obj;
        _log = Logger.getLogger(_obj.getClass());
    }

    /**
     * Возвращает хранимый объект
     * @return Объект, связанный с данным экземпляром {@link ObjectHolder}
     */
    public T getObject() {
        return _obj;
    }

    /**
     * Вызывает заданный метод хранимого объекта.<br/>
     * Вызов этого метода эквивалентен вызову <code>invokeMethod(getObject().getClass(), name, args)</code>
     * @param name Имя метода
     * @param args Типизированные аргументы метода
     * @return Значение, полученное как результат вызова метода
     * @throws Throwable В случае возникновения ошибки вызова
     * @see #invokeMethod(Class, String, TypedValue[])
     */
    public TypedValue invokeMethod(String name, TypedValue[] args) throws Throwable {
        return invokeMethod(_obj.getClass(), name, args);
    }

    /**
     * Вызывает заданный метод хранимого объекта, выполняя рефлексивный поиск нужного метода в заданном классе<br/>
     * @param cls Класс, в котором искать метод
     * @param name Имя метода
     * @param args Типизированные аргументы метода
     * @return Значение, полученное как результат вызова метода
     * @throws Throwable В случае возникновения ошибки вызова
     * @see #invokeMethod(String, TypedValue[])
     */
    public TypedValue invokeMethod(Class cls, String name, TypedValue[] args) throws Throwable {
        // (1) get arguments and their types
        Object[] values = new Object[0];
        Class[] types = new Class[0];
        if (args != null) {
            values = new Object[args.length];
            types = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                values[i] = args[i].getValue();
                types[i] = args[i].getType();
            }
        }
        // (2) perform method call
        try {
            Method method = cls.getMethod(name, types);
            method.setAccessible(true);
            Object retval = method.invoke(_obj, values);
            return new TypedValue(retval, method.getReturnType());
        } catch (Throwable th) {
            while (th instanceof InvocationTargetException) {
                th = th.getCause();
            }
            if (_log.isDebugEnabled()) {
                // debug
                String sign = "";
                for (int i = 0; i < types.length; i++) {
                    if (i > 0) {
                        sign += ",";
                    }
                    sign += (types[i] == null) ? "null" : types[i].getName();
                }
                _log.error("Unable to invoke method " + name + "(" + sign + ")", th);
            } else {
                _log.error("Unable to invoke method " + name + "(...)", th);
            }
            throw th;
        }
    }

    public static TypedValue[] convertToTypedValues(Method method, Object[] args) {
        Class[] arg_types = method.getParameterTypes();
        TypedValue[] result = new TypedValue[arg_types.length];
        for (int i = 0; i < arg_types.length; i++) {
            result[i] = new TypedValue(args[i], arg_types[i]);
        }
        return result;
    }
}
