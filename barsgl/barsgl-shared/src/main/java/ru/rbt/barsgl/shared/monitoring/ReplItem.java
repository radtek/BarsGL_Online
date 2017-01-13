package ru.rbt.barsgl.shared.monitoring;

import java.io.Serializable;

/**
 * Created by akichigi on 07.12.16.
 */
public class ReplItem implements Serializable {
    private Integer total_wait;
    private Integer total_error;
    private Integer total_done;
    private Integer total_unknown;

    public ReplItem(){}

    public ReplItem(Integer total_wait, Integer total_error, Integer total_done, Integer total_unknown) {
        this.total_wait = total_wait;
        this.total_error = total_error;
        this.total_done = total_done;
        this.total_unknown = total_unknown;
    }

    public Integer getTotal_wait() {
        return total_wait;
    }

    public Integer getTotal_error() {
        return total_error;
    }

    public Integer getTotal_done() {
        return total_done;
    }

    public Integer getTotal_unknown() {
        return total_unknown;
    }

    public Integer getTotal(){
        return total_done + total_error + total_unknown + total_wait;
    }
}
