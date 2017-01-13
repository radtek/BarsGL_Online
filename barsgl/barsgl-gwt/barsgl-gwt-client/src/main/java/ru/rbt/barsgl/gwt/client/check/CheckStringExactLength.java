package ru.rbt.barsgl.gwt.client.check;

import ru.rbt.barsgl.gwt.core.utils.AppPredicate;

/**
 * Created by ER18837 on 08.12.15.
 */
public class CheckStringExactLength implements AppPredicate<String> {

    private int exactValue;

    public CheckStringExactLength(int exactValue){
        this.exactValue = exactValue;
    };

    @Override
    public boolean check(String target) {
        if (null != target) {
            return (target.length() == exactValue);
        } else {
            return false;
        }
    }
}
