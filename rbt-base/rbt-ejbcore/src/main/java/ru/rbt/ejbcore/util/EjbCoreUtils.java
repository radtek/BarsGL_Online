package ru.rbt.ejbcore.util;

import java.util.Arrays;

public class EjbCoreUtils {

    public static <T extends Enum> boolean containsEnum(T value, T ... values) {
        return null != values && Arrays.stream(values).anyMatch(p -> p == value);
    }
}
