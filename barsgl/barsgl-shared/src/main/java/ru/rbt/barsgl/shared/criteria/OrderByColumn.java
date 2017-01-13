package ru.rbt.barsgl.shared.criteria;

import java.io.Serializable;

/**
 * Created by Ivan Sevastyanov
 */
public class OrderByColumn implements Serializable {

    private final String column;
    private final OrderByType order;

    public OrderByColumn(String column, OrderByType order) {
        this.column = column;
        this.order = order;
    }

    public String getColumn() {
        return column;
    }

    public OrderByType getOrder() {
        return order;
    }
}
