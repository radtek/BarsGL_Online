package ru.rbt.barsgl.shared.criteria;

import ru.rbt.barsgl.shared.Builder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ER21006 on 19.01.2016.
 */
public class CriteriaBuilder implements Builder<Criteria> {

    private final CriteriaLogic logic;
    private List<Criterion> criterions = new ArrayList<>();

    private CriteriaBuilder(CriteriaLogic logic) {
        this.logic = logic;
    }

    public static CriteriaBuilder create(CriteriaLogic logic) {
        return new CriteriaBuilder(logic);
    }

    @Override
    public Criteria build() {
        return new Criteria(logic, criterions);
    }

    public CriteriaBuilder appendLE(String columnName, Object value) {
        criterions.add(CriterionColumn.LE(columnName, value));
        return this;
    }

    public CriteriaBuilder appendLT(String columnName, Object value) {
        criterions.add(CriterionColumn.createCriterion(columnName, Operator.LT, value));
        return this;
    }

    public CriteriaBuilder appendEQ(String columnName, Object value) {
        criterions.add(CriterionColumn.EQ(columnName, value));
        return this;
    }

}
