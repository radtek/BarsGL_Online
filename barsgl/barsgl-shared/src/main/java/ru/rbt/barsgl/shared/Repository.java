package ru.rbt.barsgl.shared;

/**
 * Created by SotnikovAV on 03.11.2016.
 */
public enum Repository {
    BARSGL("BARSGL"),
    BARSGLNOXA("BARSGLNOXA"),
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
