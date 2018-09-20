package ru.rbt.barsgl.gwt.client.check;

import ru.rbt.barsgl.gwt.core.utils.AppPredicate;

import java.util.Date;

/**
 * Created by er18837 on 16.08.2018.
 */
public class CheckDateNotAfter implements AppPredicate<Date> {

    private Date limit;

    public CheckDateNotAfter(Date limit) {
        this.limit = limit;
    }

    @Override
    public boolean check(Date target) {
        return (null != target && null != limit && !target.after(limit));
    }
}
