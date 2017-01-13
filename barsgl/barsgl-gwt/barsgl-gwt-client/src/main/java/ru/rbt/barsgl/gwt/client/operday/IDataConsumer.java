package ru.rbt.barsgl.gwt.client.operday;

/**
 * Created by ER18837 on 28.10.15.
 */
public interface IDataConsumer<T> {
    void accept(T t);
}
