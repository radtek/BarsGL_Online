package ru.rbt.barsgl.shared;

/**
 * Created by ER21006 on 14.01.2016.
 */
public abstract class AbstractHasValue<T> implements HasValue<T> {

    @Override
    public void setValue(Object value) {
        throw new IllegalAccessError("Not implemented");
    }
}
