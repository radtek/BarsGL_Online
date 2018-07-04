package ru.rbt.barsgl.gwt.client.check;

import ru.rbt.barsgl.gwt.core.utils.AppPredicate;

import java.math.BigDecimal;

/**
 * Created by Ivan Sevastyanov
 */
public class CheckNotNullBigDecimal implements AppPredicate<BigDecimal> {

    @Override
    public boolean check(BigDecimal target) {
        return null != target;
    }
}
