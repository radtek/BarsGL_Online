package ru.rbt.barsgl.shared.criteria;

/**
 * Created by Ivan Sevastyanov
 */
public enum Operator {

    GE(">="), LE("<="), IS_NULL("is null"), NOT_NULL("is not null"), NE("!="), LIKE_NOCASE("like_nocase"), NOT_IN("not in"), EQ("="),  GT(">"), LT("<"), BETWEEN("between"), LIKE("like"), IN("in"),;
    private String value;

    Operator(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
