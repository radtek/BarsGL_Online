package ru.rbt.barsgl.gwt.core.widgets;

import ru.rbt.barsgl.gwt.core.datafields.Field;

/**
 * Created by akichigi on 11.03.15.
 */
public interface ICellValueEvent {
     String getDisplayValue(String name, Field field, String defValue);
}
