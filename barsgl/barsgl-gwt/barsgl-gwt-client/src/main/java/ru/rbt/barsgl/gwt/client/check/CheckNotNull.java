package ru.rbt.barsgl.gwt.client.check;

import ru.rbt.barsgl.gwt.core.utils.AppPredicate;

/**
 * Created by Ivan Sevastyanov
 */
public class CheckNotNull implements AppPredicate <Object> {

    @Override
    public boolean check(Object target) {
        return null != target;
    }
}
