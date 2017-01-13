package ru.rbt.barsgl.gwt.core.datafields;

import java.io.Serializable;
import java.util.ArrayList;

public class Rows implements Serializable {
	private static final long serialVersionUID = 7507508478832180385L;

    private ArrayList<Row> rows = new ArrayList<Row>();

    public Rows() {}

    public int addRow(Row row) {
        return rows.add(row) ? rows.indexOf(row) : -1;
    }

    public Row removeRow(int index) {
        return ((index >= 0) && (index < getRowCount())) ? rows.remove(index) : null;
    }

    public Row replaceRow(Row row, int index) {
        return ((index >= 0) && (index < getRowCount())) ? rows.set(index, row) : null;
    }

    public Row getRow(int index) {
        return ((index >= 0) && (index < getRowCount())) ? rows.get(index) : null;
    }

    public int getRowCount () {
        return rows.size();
    }

    public void removeAll () {
        rows.clear();
    }
    
    public void applyChanges () {
        for (Row r : rows) {
            r.applyChanges();
        }
    }

    public void cancelChanges () {
        for (Row r : rows) {
            r.cancelChanges();
        }
    }
}
