package ru.rbt.barsgl.gwt.client.check;

import ru.rbt.barsgl.gwt.core.utils.AppPredicate;

/**
 * Created by ER18837 on 08.02.16.
 */
public class CheckStringLength implements AppPredicate<String> {

    private int minLength;
    private int maxLength;

    public CheckStringLength(int minLength,  int maxLength){
        this.minLength = minLength;
        this.maxLength = maxLength;
    };

    @Override
    public boolean check(String target) {
        if (null != target) {
            return (target.length() >= minLength && target.length() <= maxLength);
        } else {
            return false;
        }
    }
}

