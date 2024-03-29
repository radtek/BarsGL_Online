package ru.rbt.barsgl.gwt.client.check;

import ru.rbt.barsgl.gwt.core.utils.AppPredicate;

public class CheckNotEmptyString implements AppPredicate<String>{

    @Override
    public boolean check(String target) {
        if (null != target) {
            return !target.trim().isEmpty();
        } else {
            return false;
        }
    }
}
