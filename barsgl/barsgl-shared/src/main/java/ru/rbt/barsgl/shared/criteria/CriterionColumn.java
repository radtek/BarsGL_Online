package ru.rbt.barsgl.shared.criteria;

import ru.rbt.shared.Assert;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;
import static ru.rbt.barsgl.shared.criteria.Operator.*;

/**
 * Created by Ivan Sevastyanov
 */
public abstract class CriterionColumn<V> implements Criterion<V> {

    private final String column;
    private final Operator operator;
    private final V value;

    private CriterionColumn(String column, Operator operator, V value) {
        this.column = column;
        this.operator = operator;
        this.value = value;
    }

    public final String getColumn() {
        return column;
    }

    public final Operator getOperator() {
        return operator;
    }

    @Override
    public final V getValue() {
        return value;
    }

    public static <T> CriterionColumn<T> createCriterion(String column, Operator operator, T value) {
        return new CriterionColumn<T>(column, operator, value) {
        };
    }

    public static <V> CriterionColumn<V> GE(String column, V object) {
        return createCriterion(column, GE, object);
    }

    public static <V> CriterionColumn<V> LE(String column, V object) {
        return createCriterion(column, LE, object);
    }

    public static <V> CriterionColumn<List<V>> IN(String column, List<V> object) {
        return createCriterion(column, IN, object);
    }

    public static <V> CriterionColumn<List<V>> NOT_IN(String column, List<V> object) {
        return createCriterion(column, NOT_IN, object);
    }

    public static <V> CriterionColumn<V> EQ(String column, V object) {
        return createCriterion(column, EQ, object);
    }

    public static <V> CriterionColumn<List<V>> BETWEEN(String column, V object1, V object2) {
        List<V> params = Arrays.asList(object1, object2);
        return createCriterion(column, BETWEEN, params);
    }

    public static CriterionColumn<String> LIKE_NOCASE(String column, String string) {
        return createCriterion(column, LIKE_NOCASE, string);
    }

    public static CriterionColumn<String> LIKE(String column, String string) {
        return createCriterion(column, LIKE, string);
    }

    public static CriterionColumn NOT_EQUALS(String column, Object value) {
        return createCriterion(column, NE, value);
    }

    public static CriterionColumn<Object> parseCriteria(String criterionString) {
        Operator operator = null;
        for (Operator current : Operator.values()) {
            if (criterionString.toLowerCase().contains(current.getValue())) {
                operator = current; break;
            }
        }
        if (null == operator) {
            throw new IllegalArgumentException("unparsable predicate string: " + criterionString);
        } else {
            String[] arr = criterionString.split(operator.getValue());
            Object value = null;
            if (null != arr[1]) {
                String valueStr = arr[1].trim();
                // строка в формате 122f, ну или 124.789i
                if (valueStr.matches("^((\\d+)|(\\d+\\.\\d+))[ildf]$")) {
                    value = parseNumber(valueStr);
                } else if (valueStr.matches("^\'.*\'$")) {
                    // строка
                    value = valueStr.replaceAll("\'", "");
                } else if (valueStr.matches("^\\[.*\\]$")) {
                    // дата
                    value = stringToDate(valueStr.replaceAll("(\\[)|(\\])", ""));
                } else if (valueStr.matches("^(true)|(false)$")) {
                    // дата
                    value = Boolean.parseBoolean(valueStr);
                } else {
                    throw new IllegalArgumentException(format("unparsable value '%s' for parameter '%s'", valueStr, arr[0].trim()));
                }
                return CriterionColumn.createCriterion(arr[0].trim(), operator, value);
            } else {
                throw new IllegalArgumentException(format("parameter '%s' is null", arr[0]));
            }
        }
    }

    public static Number parseNumber(String numberString) {
        if (numberString.contains("i")) return Integer.parseInt(numberString.replaceAll("i",""));
        if (numberString.contains("l")) return Long.parseLong(numberString.replaceAll("l",""));
        if (numberString.contains("d")) return Double.parseDouble(numberString.replaceAll("d",""));
        if (numberString.contains("f")) return Float.parseFloat(numberString.replaceAll("f",""));
        throw new IllegalArgumentException(numberString);
    }

    /**
     * Возвращает дату по строке в формате дд.мм.гггг
     * @param date строка в формате дд.мм.гггг
     * @return дата
     */
    /**
     *
     * Возвращает строку с датой формата дд.мм.гггг
     */
    public static Date stringToDate(String date) {
        Assert.isTrue(null != date && !date.trim().isEmpty());
        try {
            return new SimpleDateFormat("dd.MM.yyyy").parse(date);
        } catch (ParseException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
