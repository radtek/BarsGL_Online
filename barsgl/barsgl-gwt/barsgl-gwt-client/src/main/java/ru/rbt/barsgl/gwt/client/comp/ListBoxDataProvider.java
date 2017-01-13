package ru.rbt.barsgl.gwt.client.comp;

import ru.rbt.barsgl.gwt.core.datafields.Columns;

/**
 * Created by Ivan Sevastyanov
 */
public interface ListBoxDataProvider {

    void provide(DataListBox listBox);
    void setSelectValue(String selectValue);
    Columns getColumns();
}
