package ru.rbt.barsgl.ejbcore.page;

import ru.rbt.barsgl.shared.criteria.Criteria;
import ru.rbt.barsgl.shared.criteria.Criterion;
import ru.rbt.barsgl.shared.criteria.CriterionColumn;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Ivan Sevastyanov
 */
public class WhereInterpreter {
    private static Pattern pattern = Pattern.compile("@\\d");

    public static SQL interpret(final Criterion criterion, final String alias) {
        String sql = "";
        List<Object> params = new ArrayList<>();
        sql = sqlFragment(criterion, sql, params, alias);
        return new SQL(sql, params.toArray());
    }

    private static String sqlFragment(final Criterion criterion, String sql, List<Object> params, final String alias) {
        sql = null2String(sql);
        String criteriaSQL = "";
        if (criterion instanceof CriterionColumn) {
            CriterionColumn cl = (CriterionColumn) criterion;
            switch (cl.getOperator()) {
                case EQ:
                    params.add(cl.getValue());
                    criteriaSQL = getColumnName(cl, alias) + " = ?";
                    break;
                case IN:
                    List<Object> values = (List<Object>) cl.getValue();
                    if (null == values || values.isEmpty())
                        break;
                    criteriaSQL += getColumnName(cl, alias) + " IN (";
                    for (Object value : values) {
                        params.add(value);
                        criteriaSQL += "?,";
                    }
                    criteriaSQL = criteriaSQL.replaceAll(",$", "") + ")";
                    break;
                case NOT_IN:
                    values = (List<Object>) cl.getValue();
                    if (null == values || values.isEmpty())
                        break;
                    criteriaSQL += getColumnName(cl, alias) + " NOT IN (";
                    for (Object value : values) {
                        params.add(value);
                        criteriaSQL += "?,";
                    }
                    criteriaSQL = criteriaSQL.replaceAll(",$", "") + ")";
                    break;
                case GE:
                    params.add(cl.getValue());
                    criteriaSQL = getColumnName(cl, alias) + " >= ?";
                    break;
                case LE:
                    params.add(cl.getValue());
                    criteriaSQL = getColumnName(cl, alias) + " <= ?";
                    break;
                case IS_NULL:
                    criteriaSQL = getColumnName(cl, alias) + " IS NULL";
                    break;
                case NOT_NULL:
                    criteriaSQL = getColumnName(cl, alias) + " IS NOT NULL";
                    break;
                case NE:
                    params.add(cl.getValue());
                    criteriaSQL = getColumnName(cl, alias) + " <> ?";
                    break;
                case GT:
                    params.add(cl.getValue());
                    criteriaSQL = getColumnName(cl, alias) + " > ?";
                    break;
                case LT:
                    params.add(cl.getValue());
                    criteriaSQL = getColumnName(cl, alias) + " < ?";
                    break;
                case BETWEEN:
                    values = (List<Object>) criterion.getValue();
                    if (null == values || 2 > values.size()) {
                        throw new IllegalArgumentException("Illegal 'BETWEEN' criterion");
                    }
                    params.add(values.get(0));
                    params.add(values.get(1));
                    criteriaSQL = getColumnName(cl, alias) + " BETWEEN ? AND ? ";
                    break;
                case LIKE:
                    params.add(cl.getValue());
                    criteriaSQL = getColumnName(cl, alias) + " LIKE ?";
                    break;
                case LIKE_NOCASE:
                    params.add(((String) cl.getValue()).toLowerCase());
                    criteriaSQL = "LOWER(" + getColumnName(cl, alias) + ") LIKE ?";
                    break;
                default:
                    throw new IllegalArgumentException(criterion.toString());
            }
            sql = replaceFirst(sql, criteriaSQL);
        } else if (criterion instanceof Criteria) {
            Criteria group = (Criteria) criterion;
            if (group.getValue().isEmpty()) return sql;

            for (int i = 1; i <= group.getValue().size(); i++) {
                criteriaSQL += (" @" + i) + " " + group.getLogic().name();
            }
            criteriaSQL = criteriaSQL.replaceAll(group.getLogic().name() + "$", "");
            sql = replaceFirst(sql, "(" + criteriaSQL + ")");
            for (Criterion c : group.getValue()) {
                sql = sqlFragment(c, sql, params, alias);
            }
        }
        return sql;
    }

    private static String null2String(String str) {
        return isEmpty(str) ? "" : str;
    }

    private static String replaceFirst(String sql, String replacement) {
        if (sql.isEmpty() || !sql.contains("@")) {
            return replacement;
        } else {
            Matcher matcher = pattern.matcher(sql);
            return matcher.replaceFirst(replacement);
        }
    }

    private static boolean isEmpty(String target) {
        return null == target || target.trim().isEmpty();
    }

    private static String getColumnName(final CriterionColumn criterionColumn, String alias) {
        return !isEmpty(alias) ? alias + "." + criterionColumn.getColumn() : criterionColumn.getColumn();
    }
}
