package ru.rbt.barsgl.shared.enums;

/**
 * Created by Ivan Sevastyanov on 23.05.2016.
 */
public class EnumUtils {

    public static <T extends Enum> boolean contains(T[] values, T value) {
        if (null != values && values.length > 0) {
            for (T const1 : values) {
                if (value == const1) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }
}
