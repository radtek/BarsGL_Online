package ru.rbt.barsgl.gwt.client.check;

import ru.rbt.barsgl.gwt.core.utils.AppPredicate;

import java.math.BigDecimal;

/**
 * Created by ER18837 on 08.02.16.
 */
public class CheckNotZeroBigDecimal implements AppPredicate<String> {
    @Override
    public boolean check(String target) {
        try {
            if (null != target && !target.trim().isEmpty()) {
                final BigDecimal dec = new BigDecimal(target);
                return dec.compareTo(BigDecimal.ZERO) != 0;
            } else {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
