package ru.rbt.shared.enums;

/**
 * Created by SotnikovAV on 03.11.2016.
 */
public enum Repository {
    BARSGL("BARSGL"),
    BARSREP("BARSREP");

    private String type;

    Repository(String type) {
        this.type = type;
    }
    public String Repository() {
        return type;
    }
    public String getParamDesc() {
        return type;
    }
}
