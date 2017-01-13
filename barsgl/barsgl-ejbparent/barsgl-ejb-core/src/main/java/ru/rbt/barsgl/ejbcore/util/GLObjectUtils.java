package ru.rbt.barsgl.ejbcore.util;

import static ru.rbt.barsgl.ejbcore.util.StringUtils.isEmpty;

/**
 * Created by Ivan Sevastyanov
 */
public class GLObjectUtils {

    public static <T> T requiredNotNull(T target, String claim) {
        if (null == target) {
            throw new IllegalArgumentException(isEmpty(claim) ? "Object is null" : claim);
        } else {
            return target;
        }
    }
}
