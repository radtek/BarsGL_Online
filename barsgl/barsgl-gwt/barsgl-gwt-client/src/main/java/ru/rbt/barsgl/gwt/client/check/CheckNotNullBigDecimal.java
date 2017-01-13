package ru.rbt.barsgl.gwt.client.check;

import ru.rbt.barsgl.gwt.core.utils.AppPredicate;

import java.math.BigDecimal;

/**
 * Created by Ivan Sevastyanov
 */
public class CheckNotNullBigDecimal implements AppPredicate<String> {

    @Override
    public boolean check(String target) {
        try {
            if (null != target && !target.trim().isEmpty()) {
                final BigDecimal dec = new BigDecimal(target);
                return true;
            } else {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
