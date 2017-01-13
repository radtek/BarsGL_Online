package ru.rbt.barsgl.shared.criteria;

import java.util.Collections;
import java.util.List;

/**
 * Created by Ivan Sevastyanov
 */
public class Criteria implements Criterion<List<Criterion>> {

    private final List<Criterion> criteria;
    private final CriteriaLogic logic;

    public Criteria(CriteriaLogic logic, List<Criterion> criteria) {
        this.logic = logic;
        this.criteria = criteria;
    }

    public static Criterion AND(List<Criterion> criteria) {
        return new Criteria(CriteriaLogic.AND, criteria);
    }

    public static Criterion OR(List<Criterion> criteria) {
        return new Criteria(CriteriaLogic.OR, criteria);
    }

    @Override
    public List<Criterion> getValue() {
        return Collections.unmodifiableList(criteria);
    }

    public CriteriaLogic getLogic() {
        return logic;
    }

}
