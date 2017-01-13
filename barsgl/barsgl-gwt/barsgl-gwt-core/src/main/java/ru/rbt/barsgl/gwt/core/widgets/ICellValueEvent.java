package ru.rbt.barsgl.gwt.core.widgets;

import ru.rbt.barsgl.gwt.core.datafields.Field;

/**
 * Created by akichigi on 11.03.15.
 */
public interface ICellValueEvent {
    public String getDisplayValue(String name, Field field, String defValue);
}
