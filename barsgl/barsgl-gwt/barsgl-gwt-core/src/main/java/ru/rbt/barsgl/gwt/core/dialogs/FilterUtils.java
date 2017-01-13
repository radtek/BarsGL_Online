package ru.rbt.barsgl.gwt.core.dialogs;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by akichigi on 28.07.16.
 */
public class FilterUtils {
    public static List<FilterItem> combineFilterCriteria(List<FilterItem> a, List<FilterItem> b){
        if ((a == null) & (b == null)) return null;
        if (a == null) return b;
        else if (b == null) return a;
        else {
            List<FilterItem> r = new ArrayList<>();
            r.addAll(a);
            r.addAll(b);
            return  r;
        }
    }
}
