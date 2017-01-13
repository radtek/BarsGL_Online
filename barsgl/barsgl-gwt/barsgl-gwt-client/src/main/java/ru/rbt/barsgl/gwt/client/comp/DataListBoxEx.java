package ru.rbt.barsgl.gwt.client.comp;

import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ER18837 on 24.03.16.
 */
public class DataListBoxEx extends DataListBox {
    private List<Row> rowList;
    Columns columns;

    public DataListBoxEx(ListBoxDataProvider provider) {
        super(provider);
        columns = provider.getColumns();
    }

    @Override
    public void addItem(Serializable key, String value, Row row) {
        super.addItem(key, value);
        rowList.add(row);
    }

    @Override
    protected void init() {
        rowList = new ArrayList<Row>();
    }

    public Serializable getParam(String columnName) {

        if (null == columns) return null;

        int colIndex = columns.getColumnIndexByName(columnName);
        if (colIndex >= 0)
            return getParam(colIndex);
        else
            return null;
    };

    public Serializable getParam(int index) {
        if (!validate()) return null;

        int idx = list.getSelectedIndex();
        if (idx == -1 || idx >= rowList.size()) return null;

        Row row = rowList.get(idx);
        if (null != row && index >=0 && index < row.getFieldsCount())
            return row.getField(index).getValue();
        else
            return null;
    };

    public Row getRow(){
        if (!validate()) return null;

        int idx = list.getSelectedIndex();
        if (idx == -1 || idx >= rowList.size()) return null;

        return rowList.get(idx);
    }

    public void setParam(String columnName, Serializable param) {
        if (null == columns || null == param) return;

        int colIndex = columns.getColumnIndexByName(columnName);
        if (colIndex < 0)
            return;

        int i = 0;
        for (Row row : rowList) {
        	if (null != row) {
	            if (param.equals(row.getField(colIndex).getValue())) {
	                Serializable value = getKeyByIndex(i);
	                setSelectValue((String) value);
	            }
        	}
            i++;
        }
    };

}
