package ru.rbt.barsgl.gwt.client.comp;

import ru.rbt.barsgl.gwt.core.datafields.Row;

import java.io.Serializable;

/**
 * Created by ER18837 on 24.03.16.
 */
public interface IDataListBox {
    Serializable getValue();
    void setValue(Serializable value);
    void setSelectValue(String value);
    void addItem(Serializable key, String value, Row row);
}
