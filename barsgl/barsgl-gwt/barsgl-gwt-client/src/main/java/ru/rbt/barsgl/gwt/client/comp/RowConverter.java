package ru.rbt.barsgl.gwt.client.comp;

import ru.rbt.barsgl.gwt.core.datafields.Row;

/**
 * Created by Ivan Sevastyanov
 */
public interface RowConverter {

    String getKey(Row row);
    String getText(Row row);
}
