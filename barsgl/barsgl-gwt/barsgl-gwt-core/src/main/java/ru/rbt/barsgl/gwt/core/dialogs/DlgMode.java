package ru.rbt.barsgl.gwt.core.dialogs;

public enum DlgMode {
	BROWSE("Свойства"), EDIT("Правка"), NEW("Новый");
    private String value;

    DlgMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
