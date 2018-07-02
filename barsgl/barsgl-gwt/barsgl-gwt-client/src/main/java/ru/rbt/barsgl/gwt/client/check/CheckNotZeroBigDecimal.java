package ru.rbt.barsgl.gwt.client.check;

import ru.rbt.barsgl.gwt.core.utils.AppPredicate;

import java.math.BigDecimal;

/**
 * Created by ER18837 on 08.02.16.
 */
public class CheckNotZeroBigDecimal implements AppPredicate<BigDecimal> {
    @Override
    public boolean check(BigDecimal target) {
        try {
            if (null != target) {
                return target.compareTo(BigDecimal.ZERO) != 0;
            } else {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
