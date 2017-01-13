package ru.rbt.barsgl.shared.monitoring;

import java.io.Serializable;

/**
 * Created by akichigi on 07.12.16.
 */
public class OperTableItem implements Serializable {
    private String state;
    private String fun;
    private Integer count;

    public OperTableItem() {}

    public OperTableItem(String state, String fun, Integer count) {
        this.state = state;
        this.fun = fun;
        this.count = count;
    }

    public String getState() {
        return state;
    }

    public String getFun() {
        return fun;
    }

    public Integer getCount() {
        return count;
    }
}
