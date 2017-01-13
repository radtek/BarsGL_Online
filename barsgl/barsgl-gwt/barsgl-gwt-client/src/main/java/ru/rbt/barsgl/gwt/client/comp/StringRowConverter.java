package ru.rbt.barsgl.gwt.client.comp;

import ru.rbt.barsgl.gwt.core.datafields.Row;

/**
 * Created by ER18837 on 28.10.15.
 */
public class StringRowConverter implements RowConverter {
    private int keyFieldIndex;
    private int textFieldIndex;

    public StringRowConverter() {
        this.keyFieldIndex = 0;
        this.textFieldIndex = 0;
    }

    public StringRowConverter(int keyFieldIndex, int textFieldIndex) {
        this.keyFieldIndex = keyFieldIndex;
        this.textFieldIndex = textFieldIndex;
    }

    @Override
    public String getKey(Row row) {
        return row.getFieldsCount() > keyFieldIndex ? row.getField(keyFieldIndex).getValue().toString().trim() : "";
    }

    @Override
    public String getText(Row row) {
        return row.getFieldsCount() > textFieldIndex ? row.getField(textFieldIndex).getValue().toString() : "";
    }
}
