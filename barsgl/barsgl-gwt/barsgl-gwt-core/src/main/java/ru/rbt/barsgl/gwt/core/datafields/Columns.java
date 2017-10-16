package ru.rbt.barsgl.gwt.core.datafields;

import com.google.gwt.user.client.Window;

import java.io.Serializable;
import java.util.ArrayList;

public class Columns implements Serializable {
	private static final long serialVersionUID = -9050945573314357260L;

	private ArrayList<Column> columns = new ArrayList<Column>();

    public Columns() {}

    public int addColumn(Column column) {
        if (column.getName() == null || column.getName().trim().isEmpty() ||
                getColumnIndexByName(column.getName()) != -1)
            return -1;
        return columns.add(column) ? columns.indexOf(column) : -1;
    }

    public int addColumn(String name, Column.Type type) {
        return addColumn(new Column(name, type));
    }

    public Column removeColumn(int index) {
        return ((index >= 0) && (index < getColumnCount())) ? columns.remove(index) : null;
    }

    public Column replaceColumn(Column column, int index) {
        return ((index >= 0) && (index < getColumnCount())) ? columns.set(index, column) : null;
    }

    public Column getColumnByIndex (int index) {
        return ((index >= 0) && (index < getColumnCount())) ? columns.get(index) : null;
    }

    public Column getColumnByName(String name){
        return getColumnByIndex(getColumnIndexByName(name));
    }

    public Column getColumnByCaption(String caption){
        return getColumnByIndex(getColumnIndexByCaption(caption));
    }

    public int getColumnCount () {
        return columns.size();
    }

    public void removeAll () {
        columns.clear();
    }

    public int getColumnIndexByName (String name) {
        for (Column c : columns) {
            if (c.getName().equals(name))
                return columns.indexOf(c);
        }
        return -1;
    }

    public int getColumnIndexByCaption (String caption) {
        for (Column c : columns) {
            if (c.getCaption().equals(caption))
                return columns.indexOf(c);
        }
        return -1;
    }
}
