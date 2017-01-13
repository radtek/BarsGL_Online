package ru.rbt.barsgl.ejbcore.util.reflection;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Describes a strictly typed value.<br/>
 * This class is essentially a type-value pair, where "type" part
 * is not <code>null</code>.<br/>
 * When "value" part is also not <code>null</code>, then the
 * "type" part may not neccessarily be equal to
 * <code>value.getClass()</code>, but it should be possible to
 * treat "value" part as if it is of type "type".<br/>
 * For example, the "type" part may indicate <code>long</code>,
 * but "value" part may contain an <code>int</code>. Another
 * example is when "type" part indicates primitive
 * <code>boolean</code> and "value" part contains
 * {@link java.lang.Boolean} data. Yet another example is when
 * "type" is a superclass for <code>value.getClass()</code>. <br/>
 * {@link TypedValue} objects are primarily used for method
 * invocation of remote services.
 */
public final class TypedValue implements Serializable {

    private static final Map<String, Class<?>> primitiveTypes;

    static {
        final HashMap<String, Class<?>> types = new HashMap<String, Class<?>>();

        types.put(boolean.class.getName(), boolean.class);

        types.put(byte.class.getName(), byte.class);
        types.put(char.class.getName(), char.class);

        types.put(short.class.getName(), short.class);
        types.put(int.class.getName(), int.class);
        types.put(long.class.getName(), long.class);

        types.put(float.class.getName(), float.class);
        types.put(double.class.getName(), double.class);

        primitiveTypes = Collections.unmodifiableMap(types);
    }

    private Object _value;

    private String _type;

    /**
     * Default constructor required for serialization
     */
    public TypedValue() {
    }

    /**
     * Constructs a new instance of {@link TypedValue} for a specified
     * type-value pair
     * @param value The value
     * @param type The type
     */
    public TypedValue(Object value, Class type) {
        _value = value;
        _type = type.getName();
    }

    /**
     * Returns the "value" part
     * @return This {@link TypedValue}'s "value" part
     */
    public Object getValue() {
        return _value;
    }

    /**
     * Returns this {@link TypedValue}'s type's name
     * @return The name of this {@link TypedValue}'s type
     */
    public String getTypeName() {
        return _type;
    }

    /**
     * Returns the "type" part
     * @return This {@link TypedValue}'s "type" part
     * @throws ClassNotFoundException If the specified class can not
     * be loaded
     */
    public Class getType() throws ClassNotFoundException {
        try {
            return Class.forName(_type);
        } catch (ClassNotFoundException e) {
            final Class<?> primitiveType = primitiveTypes.get(_type);
            if (primitiveType != null) {
                return primitiveType;
            } else {
                throw e;
            }
        }
    }

    /**
     * Constructs string representation for debugging
     * @return This {@link TypedValue}'s string representation
     */
    public String toString() {
        return "{" + _value + ": " + _type + "}";
    }

}
