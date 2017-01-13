package ru.rbt.barsgl.gwt.client.check;

import ru.rbt.barsgl.gwt.core.utils.AppFunction;

import java.math.BigDecimal;

/**
 * Created by Ivan Sevastyanov
 */
public class ConvertStringToBigDecimal implements AppFunction<String,BigDecimal> {

    @Override
    public BigDecimal apply(String from) {
        return new BigDecimal(from);
    }
}
