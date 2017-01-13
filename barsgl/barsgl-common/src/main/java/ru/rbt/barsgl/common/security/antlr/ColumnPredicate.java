package ru.rbt.barsgl.common.security.antlr;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import ru.rbt.barsgl.common.ReflectionUtils;

import java.util.List;

//import custis.banking.core.data.DataRecord;

public abstract class ColumnPredicate<T> implements Predicate<T> {

    private final String column;

    protected ColumnPredicate(String column) {
        this.column = column;
    }

    public String getColumn() {
        return column;
    }

    public static <T>Predicate<T> EQ(final String column, final Object value) {
        return new ColumnPredicate<T>(column) {
            @Override
            public boolean apply(T input) {
                if (null == input || null == value)
                    return false;
                else {
                    return value.equals(ReflectionUtils.getBeanProperty(input, column, true));
                }
            }
        };
    }

    public static <T> Predicate<T> GE(final String column, final Object value) {
        return new ColumnPredicate<T>(column) {
            @Override
            public boolean apply(T input) {
                if (null == input || null == value)
                    return false;
                else {
                    if (value instanceof Comparable) {
                        return ((Comparable) value).compareTo(ReflectionUtils.getBeanProperty(input, column, true)) <= 0;
                    } else {
                        return false;
                    }
                }
            }
        };
    }

    public static <T> Predicate<T> GT(final String column, final Object value) {
        return new ColumnPredicate<T>(column) {
            @Override
            public boolean apply(T input) {
                if (null == input || null == value)
                    return false;
                else {
                    if (value instanceof Comparable) {
                        return ((Comparable) value).compareTo(ReflectionUtils.getBeanProperty(input, column, true)) < 0;
                    } else {
                        return false;
                    }
                }
            }
        };
    }

    public static <T> Predicate<T> LE(final String column, final Object value) {
        return new ColumnPredicate<T>(column) {
            @Override
            public boolean apply(T input) {
                if (null == input || null == value)
                    return false;
                else {
                    if (value instanceof Comparable) {
                        return ((Comparable) value).compareTo(ReflectionUtils.getBeanProperty(input, column, true)) >= 0;
                    } else {
                        return false;
                    }
                }
            }
        };
    }

    public static <T> Predicate<T> LT(final String column, final Object value) {
        return new ColumnPredicate<T>(column) {
            @Override
            public boolean apply(T input) {
                if (null == input || null == value)
                    return false;
                else {
                    if (value instanceof Comparable) {
                        return ((Comparable) value).compareTo(ReflectionUtils.getBeanProperty(input, column, true)) > 0;
                    } else {
                        return false;
                    }
                }
            }
        };
    }

    public static <T> Predicate<T> IN(final String column, final List values) {
        if (null == values)
            return Predicates.alwaysFalse();
        else {
            List<Predicate<T>> predicates = Lists.<Object, Predicate<T>>transform(values, new Function<Object, Predicate<T>>() {
                @Override
                public Predicate<T> apply(Object input) {
                    return EQ(column, input);
                }
            });
            return Predicates.or(predicates);
        }
    }

    public static <T> Predicate<T> NOT_IN(final String column, final List values) {
        if (null == values)
            return Predicates.alwaysTrue();
        else {
            List<Predicate<T>> predicates = Lists.<Object, Predicate<T>>transform(values, new Function<Object, Predicate<T>>() {
                @Override
                public Predicate<T> apply(Object input) {
                    return NE(column, input);
                }
            });
            return Predicates.and(predicates);
        }
    }

    public static <T> Predicate<T> IS_NULL(final String column) {
        return new ColumnPredicate<T>(column) {
            @Override
            public boolean apply(T input) {
                return null == ReflectionUtils.getBeanProperty(input, column, true);
            }
        };
    }

    public static <T> Predicate<T> NOT_NULL(final String column) {
        return Predicates.<T>not((Predicate<T>) IS_NULL(column));
    }

    public static <T> Predicate<T> NE(final String column, final Object value) {
        return (Predicate<T>) Predicates.not(EQ(column, value));
    }

    public static <T> Predicate<T> BETWEEN(final String column, final Object from, final Object to) {
        return Predicates.and(GE(column, from), LE(column, to));
    }

    public static <T> Predicate<T> LIKE(final String column, final Object value) {
        return new ColumnPredicate<T>(column) {
            @Override
            public boolean apply(T input) {
                if (null == value || !(value instanceof String)) return false;
                String string = (String) value;
                String regex = string.replace("%", ".*").replace("?", ".").replace("_", ".");
                String colValue = (String) ReflectionUtils.getBeanProperty(input, column, true);
                return colValue == null ? false : colValue.matches(regex);
            }
        };
    }

    public static <T> Predicate<T> LIKE_NO_CASE(final String column, final Object value) {
        return new ColumnPredicate<T>(column) {
            @Override
            public boolean apply(T input) {
                if (null == value || !(value instanceof String)) return false;
                String string = (String) value;
                String regex = string.replace("%", ".*").replace("?", ".").replace("_", ".").toLowerCase();
                String colValue = null;
                colValue = (String) ReflectionUtils.getBeanProperty(input, column, true);
                return colValue == null ? false : colValue.toLowerCase().matches(regex);
            }
        };
    }

}

