package ru.rbt.barsgl.gwt.client.check;

import ru.rbt.barsgl.gwt.core.utils.AppFunction;

/**
 * Created by ER18837 on 10.11.15.
 */
public class ConvertStringToShort implements AppFunction<String,Short> {
    @Override
    public Short apply(String from) {
        return Short.parseShort(from);
    }
}
