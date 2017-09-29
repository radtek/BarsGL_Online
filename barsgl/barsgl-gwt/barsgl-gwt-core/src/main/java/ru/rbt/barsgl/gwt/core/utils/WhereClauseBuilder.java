package ru.rbt.barsgl.gwt.core.utils;

import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.criteria.*;
import ru.rbt.barsgl.shared.filter.IFilterItem;

import java.io.Serializable;
import java.util.ArrayList;
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

    public static Criterion filterCriteriaAdapter(List<? extends IFilterItem> filterCriteria){
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
}
