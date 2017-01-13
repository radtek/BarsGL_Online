package ru.rbt.barsgl.shared.monitoring;

import java.io.Serializable;

/**
 * Created by akichigi on 06.12.16.
 */
public class BufferItem implements Serializable {
    private Integer total_wait;
    private Integer total_moved;

    public BufferItem(){}

    public BufferItem(Integer total_wait, Integer total_moved) {
        this.total_wait = total_wait;
        this.total_moved = total_moved;
    }

    public Integer getTotal_wait() {
        return total_wait;
    }

    public Integer getTotal_moved() {
        return total_moved;
    }

    public Integer getTotal() {
        return total_wait + total_moved;
    }
}
