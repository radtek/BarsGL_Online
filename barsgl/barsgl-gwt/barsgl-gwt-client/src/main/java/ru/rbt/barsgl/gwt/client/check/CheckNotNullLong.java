package ru.rbt.barsgl.gwt.client.check;

import ru.rbt.barsgl.gwt.core.utils.AppPredicate;

/**
 * Created by Ivan Sevastyanov
 */
public class CheckNotNullLong implements AppPredicate<String> {
    @Override
    public boolean check(String target) {
        try {
            if (null != target && !target.trim().isEmpty()) {
                final Long interm = Long.parseLong(target);
                return true;
            } else {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
