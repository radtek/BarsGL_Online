package ru.rbt.barsgl.common.security.antlr;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import ru.rbt.barsgl.shared.criteria.Criteria;
import ru.rbt.barsgl.shared.criteria.Criterion;
import ru.rbt.barsgl.shared.criteria.CriterionColumn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.rbt.barsgl.shared.criteria.CriteriaLogic.AND;

public class InMemoryInterpreter {

    public static <T> List<T> filter(final List<T> orig, Criterion criterion, Class<T> clazz) {
        return Arrays.asList(Iterables.toArray(Iterables.filter(orig, createFrom(criterion)), clazz));
    }

    private static <T> Predicate<T> createFrom(Criterion criterion) {
        if (null == criterion) return Predicates.alwaysTrue();
        if (criterion instanceof CriterionColumn) {
            CriterionColumn cl = (CriterionColumn) criterion;
            switch (cl.getOperator()) {
                case EQ:
                     return ColumnPredicate.EQ(cl.getColumn(), cl.getValue());
                case GE:
                    return ColumnPredicate.GE(cl.getColumn(), cl.getValue());
                case LE:
                    return ColumnPredicate.LE(cl.getColumn(), cl.getValue());
                case IN:
                    return ColumnPredicate.IN(cl.getColumn(), (List) cl.getValue());
                case NOT_IN:
                    return ColumnPredicate.NOT_IN(cl.getColumn(), (List) cl.getValue());
                case IS_NULL:
                    return ColumnPredicate.IS_NULL(cl.getColumn());
                case NOT_NULL:
                    return ColumnPredicate.NOT_NULL(cl.getColumn());
                case NE:
                    return ColumnPredicate.NE(cl.getColumn(), cl.getValue());
                case GT:
                    return ColumnPredicate.GT(cl.getColumn(), cl.getValue());
                case LT:
                    return ColumnPredicate.LT(cl.getColumn(), cl.getValue());
                case LIKE:
                    return ColumnPredicate.LIKE(cl.getColumn(), cl.getValue());
                case LIKE_NOCASE:
                    return ColumnPredicate.LIKE_NO_CASE(cl.getColumn(), cl.getValue());
                case BETWEEN:
                    if (null == cl.getValue()
                            || !(cl.getValue() instanceof List)
                            || 2 != ((List)cl.getValue()).size()) {
                        throw new IllegalArgumentException();
                    }
                    List values = (List) cl.getValue();
                    return ColumnPredicate.BETWEEN(cl.getColumn(), values.get(0), values.get(1));
                default:
                    throw new IllegalArgumentException(criterion.toString());
            }
        } else {
            Criteria group = (Criteria) criterion;
            List<Predicate<T>> pp = new ArrayList<Predicate<T>>(group.getValue().size());
            for (Criterion local : group.getValue()) {
                pp.add((Predicate<T>) createFrom(local));
            }
            if (group.getLogic() == AND) {
                return Predicates.and(pp);
            } else {
                return Predicates.or(pp);
            }
        }

    }
}
