package ru.rbt.barsgl.common;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * User: oklinchaev Date: 19.11.2009 Time: 13:46:24
 */
public class ReflectionUtils {

    private ReflectionUtils() {
    }

    private static final Map<Class<?>, Class<?>> primitiveClassMap;

    static {
        Map<Class<?>, Class<?>> map = new HashMap<Class<?>, Class<?>>();
        map.put(Boolean.TYPE, Boolean.class);
        map.put(Character.TYPE, Character.class);
        map.put(Byte.TYPE, Byte.class);
        map.put(Short.TYPE, Short.class);
        map.put(Integer.TYPE, Integer.class);
        map.put(Long.TYPE, Long.class);
        map.put(Float.TYPE, Float.class);
        map.put(Double.TYPE, Double.class);
        map.put(Void.TYPE, null);
        primitiveClassMap = Collections.unmodifiableMap(map);
    }

    /**
     * Maps primitive classes (<code>boolean.class, byte.class, int.class,...</code> to corresponding
     * boxing classes(<code>Boolean, Byte, Integer, ...</code>
     * @return mapped boxing class (null for void class)
     */
    private static Map<Class<?>, Class<?>> getPrimitiveClassMap() {
        return primitiveClassMap;
    }

    /**
     * Возвращает "непримитивный" класс вместо переданного
     * @param cls класс, который может быть "примитивным" (int, char и т.п.)
     * @return соответствующий "непримитивный" класс. Если переданный класс не является примитивным - возвращает его. (int -&gt; Integer; char -&gt; Character; String -&gt; String)
     * Никогда не возвращает null.
     */
    public static Class<?> toNonPrimitiveClass(Class cls) {
        if (cls.isPrimitive()) {
            Class<?> r = getPrimitiveClassMap().get(cls);
            if (r == null) {
                Logger.getLogger(ReflectionUtils.class.getName()).log(Level.WARNING, "Could not convert to non primitive class: {0}", cls.getName());
                return cls;
            }
            return r;
        }
        return cls;
    }

    /**
     * Определяет класс свойства
     * @param start исходный класс
     * @param propertyPath имя свойства (nested свойства вроде account.name поддерживаются)
     * @return Класс свойства
     * @throws RuntimeException если свойство не было найдено
     */
    public static Class<?> resolvePropertyClass(final Class<?> start, String propertyPath) {
        String path[] = propertyPath.split("\\.");
        Class<?> curr = start;

        for (String item : path) {
            String methods[] = new String[]{
                    "get" + camelize(item),
                    "is" + camelize(item)
            };
            Method m = null;
            for (String method : methods) {
                try {
                    m = curr.getMethod(method);
                } catch (NoSuchMethodException e) {
                    // ignore
                }
            }
            if (m == null) {
                throw new RuntimeException("Can not parse path " + propertyPath + " for " + start);
            }
            curr = m.getReturnType();
        }
        return curr;
    }

    /**
     * Считывает значение свойства на объекте
     * @param o Объект, с которого нужно считать свойство
     * @param property имя свойства (nested свойства поддерживаются)
     * @param throwException кидать ли exception в случае ошибки или возвращать null (если один из getter-ов выбросил исключение - в любом случае данный метод выкинет RuntimeException)
     * @return значение свойства или null
     * @throws RuntimeException если один из getter-ов выбросил исключение или если произошла ошибка и параметр throwException = true
     */
    public static Object getBeanProperty(Object o, String property, boolean throwException) {
        Throwable throwable = null;
        if (o == null) {
            return null;
        }
        String nestedProp = null;
        int idx = property.indexOf('.');
        if (idx != -1) {
            nestedProp = property.substring(idx + 1);
            property = property.substring(0, idx);
        }
        String methods[] = new String[]{
                "get" + camelize(property),
                "is" + camelize(property)
        };
        for (String getterName : methods) {
            try {
                Method m = o.getClass().getMethod(getterName);
                Object res = m.invoke(o);
                if (nestedProp != null) {
                    return getBeanProperty(res, nestedProp, throwException);
                } else {
                    return res;
                }
            } catch (NoSuchMethodException e) {
                throwable = e;
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                if (throwException) {
                    throw new RuntimeException(e);
                }
            }
        }
        if (throwException) {
            throw new RuntimeException("Can not get property '" + property + "' on class " + o.getClass() + " : " + o, throwable);
        }
        return null;
    }

    /**
     * Записывает новое значение свойства в объект
     * @param o объект
     * @param property имя свойства для записи (nested свойства НЕ ПОДДЕРЖИВАЮТСЯ)
     * @param value значение
     */
    public static void setBeanProperty(Object o, String property, Object value) {
        if (o == null) {
            throw new NullPointerException();
        }
        StringBuilder sb = new StringBuilder();
        String setterName = "set" + camelize(property);
        Method ms[] = o.getClass().getMethods();
        Class valueClass = value == null ? null : ReflectionUtils.toNonPrimitiveClass(value.getClass());
        for (Method m : ms) {
            if (m.getName().equals(setterName) && m.getParameterTypes().length == 1) {
                Class pType = toNonPrimitiveClass(m.getParameterTypes()[0]);
                if (value == null || pType.isAssignableFrom(valueClass)) {
                    try {
                        m.invoke(o, value);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                    return;
                }
                sb.append(m).append(" ");
            }
        }
        throw new RuntimeException("Can not set property '" + property + "' on " + o.getClass() + " to " + value + ". Tried methods: " + sb);
    }

    /**
     * Make first letter uppercase: e.g. transforms "stringValue" to "StringValue".
     *
     * @param val value to transform. Empty string and null are also supported.
     * @return transformed string
     */
    public static String camelize(String val) {
        if (val == null || val.length() == 0) {
            return val;
        }
        return val.substring(0, 1).toUpperCase() + val.substring(1);
    }

    /**
     * Make first letter lowercase: e.g. transforms "StringValue" to "stringValue".
     *
     * @param val value to transform. Empty string and null are also supported.
     * @return transformed string
     */
    public static String decamelize(String val) {
        if (val == null || val.length() == 0) {
            return val;
        }
        return val.substring(0, 1).toLowerCase() + val.substring(1);
    }
}
