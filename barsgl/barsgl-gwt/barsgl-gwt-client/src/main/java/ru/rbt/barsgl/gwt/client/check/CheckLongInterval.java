package ru.rbt.barsgl.gwt.client.check;

import ru.rbt.barsgl.gwt.core.utils.AppPredicate;

/**
 * Created by ER18837 on 19.11.15.
 */
public class CheckLongInterval implements AppPredicate<String> {

    private long minValue;
    private long maxValue;

    public CheckLongInterval(long minValue, long maxValue){
        this.minValue = minValue;
        this.maxValue = maxValue;
    };

    @Override
    public boolean check(String target) {
        try {
            if (null != target && !target.trim().isEmpty()) {
                final Long interm = Long.decode(target);
                return (interm >= minValue) && (interm <= maxValue);
            } else {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
