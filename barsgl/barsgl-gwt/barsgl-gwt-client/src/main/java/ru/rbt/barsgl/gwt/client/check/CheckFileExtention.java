package ru.rbt.barsgl.gwt.client.check;

import ru.rbt.barsgl.gwt.core.utils.AppPredicate;

/**
 * Created by ER18837 on 09.03.16.
 */
public class CheckFileExtention implements AppPredicate<String> {

    private String ext;

    public CheckFileExtention(String ext) {
        this.ext = ext;
    }

    @Override
    public boolean check(String target) {
        if (null != target) {
            int point = target.trim().lastIndexOf(".");
            if (point > 0)
                return target.trim().substring(point+1).equals(ext);
        }
        return false;
    }
}
