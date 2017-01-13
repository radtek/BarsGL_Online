package ru.rbt.barsgl.shared;

/**
 * Created by ER21006 on 14.01.2016.
 */
public interface HasValue<T> {

    T getValue();
    void setValue(T value);

}
