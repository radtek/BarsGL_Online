package ru.rbt.barsgl.gwt.client.check;

import ru.rbt.barsgl.gwt.core.utils.AppPredicate;

import java.util.Date;

/**
 * Created by ER18837 on 15.03.16.
 */
public class CheckNotNullDate implements AppPredicate<Date> {
    @Override
    public boolean check(Date target) {
        return null != target;
    }
}
