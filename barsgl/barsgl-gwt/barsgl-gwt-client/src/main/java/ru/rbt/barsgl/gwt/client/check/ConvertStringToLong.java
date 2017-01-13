package ru.rbt.barsgl.gwt.client.check;

import ru.rbt.barsgl.gwt.core.utils.AppFunction;

/**
 * Created by Ivan Sevastyanov
 */
public class ConvertStringToLong implements AppFunction<String,Long> {
    @Override
    public Long apply(String from) { return Long.parseLong(from); }
}
