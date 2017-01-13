package ru.rbt.barsgl.gwt.core.utils;

import ru.rbt.barsgl.shared.Utils;

import java.util.EnumSet;
import java.util.List;

/**
 * Created by akichigi on 31.05.16.
 */
public class WhereClauseBuilder {
    public static String getWhereOperatorClause(String field, List<String> values, String operator){
        if (field.isEmpty() || values.isEmpty() || values.contains("*")) return  "";

        if (values.size() == 1) return Utils.Fmt(" {0}='{1}' ", field, values.get(0));

        String s = "";
        for( int i = 0; i < values.size(); i++){

            s += field +  "='" + values.get(i) + "'" + (i == values.size()-1 ? "" : " " + operator + " ");
        }
        return "(" + s + ")";
    }

    public static String getWhereInClause(EnumSet<?> statusSet, String field){
        if (statusSet == null || statusSet.isEmpty() || field.isEmpty()) return "";
        Object[] statusArray = statusSet.toArray();
        String s = "";
        for (int i = 0;  i < statusArray.length; i++){
            s +=  "'" + statusArray[i] + "'" + (i == statusArray.length-1 ? "" : " , ");
        }
        return "(" + field + " in (" + s + "))";
    }
}
