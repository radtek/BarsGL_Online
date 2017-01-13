package ru.rbt.barsgl.shared.monitoring;

import java.io.Serializable;

/**
 * Created by akichigi on 07.12.16.
 */
public class ReplTableItem implements Serializable {
    private String name;
    private String type;
    private String status;
    private Integer count;

    public ReplTableItem(){}

    public ReplTableItem(String name, String type, String status, Integer count) {
        this.name = name;
        this.type = type;
        this.status = status;
        this.count = count;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}
