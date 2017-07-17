package ru.rbt.barsgl.shared;

import ru.rbt.barsgl.shared.criteria.*;
import ru.rbt.barsgl.shared.filter.IFilterItem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by akichigi on 17.03.15.
 */
public class Utils {
    // format: "some {0} text {1} here {2}"
    public static String Fmt(String format, final Object... args){
        String retVal = format;
        int i = 0;
        for (final Object current : args) {
            retVal = retVal.replaceAll("\\{" + i + "\\}", current.toString());
            i++;
        }
        return retVal;
    }

    public static String fillUp(String txt, int len){
        int n = len - txt.length();
        String s = "";
        for(int i = 0; i < n; i++)
            s += "0";
        return s + txt;
    }

    public static String value(String val){
        return  val == null ? val : val.trim();
    }

    public static String toStr(String val){ return val == null ? "" : val;}

/*
    public static Criterion filterCriteriaAdapter(List<IFilterItem> filterCriteria){
        if (filterCriteria == null || filterCriteria.isEmpty()) return null;

        List<Criterion> list = new ArrayList<Criterion>();

        for (IFilterItem item: filterCriteria){
            Operator operator;
            Serializable value = item.getSqlValue();
            switch (item.getCriteria()){
                case GE:
                    operator = Operator.GE;
                    break;
                case GT:
                    operator = Operator.GT;
                    break;
                case LT:
                    operator = Operator.LT;
                    break;
                case LE:
                    operator = Operator.LE;
                    break;
                case NE:
                    operator = Operator.NE;
                    break;
                case HAVE:
                    operator = Operator.LIKE;
                    value = "%" + value + "%";
                    break;
                case START_WITH:
                    operator = Operator.LIKE;
                    value = value + "%";
                    break;
                case LIKE:
                    operator = Operator.LIKE;
                    break;
                case IS_NULL:
                    operator = Operator.IS_NULL;
                    break;
                case NOT_NULL:
                    operator = Operator.NOT_NULL;
                    break;
                case IS_EMPTY:
                    operator = Operator.EQ;
                    value = "";
                    break;
                case NOT_EMPTY:
                    operator = Operator.NE;
                    value = "";
                    break;
                default:
                    operator = Operator.EQ;
            }
            list.add(CriterionColumn.createCriterion(item.getSqlName(), operator, value));
        }
        return new Criteria(CriteriaLogic.AND, list);
    }
*/

}
