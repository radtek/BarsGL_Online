package ru.rbt.barsgl.gwt.core.dialogs;

/**
 * Created by akichigi on 17.04.15.
 */
public enum  FilterCriteria {
    EQ("равно", true), NE("не равно", true), GE("больше равно", true), LE("меньше равно", true),
    GT("больше", true), LT("меньше", true), HAVE("содержит", true), START_WITH("начинается с", true), LIKE("шаблон", true),
    IS_NULL("пусто", false), NOT_NULL("не пусто", false), IS_EMPTY("пустая строка", false), NOT_EMPTY("не пустая строка", false);

    private String value;
    private boolean isBinary;

    FilterCriteria(String value, boolean isBinary) {
        this.value = value;
        this.isBinary = isBinary;
    }

    public String getValue() {
        return value;
    }

    public boolean isBinary() {
        return isBinary;
    }
}
