package ru.rbt.barsgl.gwt.core.datafields;

import java.io.Serializable;

public class Table implements Serializable {
	private static final long serialVersionUID = 6987074107978838446L;

// *****************************************************************************************************************************************
// COMMON
// *****************************************************************************************************************************************
    protected Columns columns = new Columns();
    private Rows rows = new Rows();

    public Table () {}

    private boolean checkRow(Row row) {
        if ((columns.getColumnCount() == 0) || (row.getFieldsCount() == 0) || (columns.getColumnCount() != row.getFieldsCount()))
            return false;
        for (int i = 0; i < row.getFieldsCount(); i++) {
            if (!columns.getColumnByIndex(i).isRightValue(row.getField(i).getValue()))
                return false;
        }
        return true;
    }

// *****************************************************************************************************************************************
// DATA
// *****************************************************************************************************************************************
    public void removeAll() {
        rows.removeAll();
    }

    public void applyChanges() {
        rows.applyChanges();
    }

    public void cancelChanges() {
        rows.cancelChanges();
    }

// *****************************************************************************************************************************************
// Columns
// *****************************************************************************************************************************************
    public int addColumn(Column column) {
        if (isEmpty()) {
            return columns.addColumn(column);
        } else
        	return -1;
    }

    public Column removeColumn(int index) {
        if (isEmpty()) {
            return columns.removeColumn(index);
        } else
        	return null;
    }

    public Column removeColumn(String name){
       return removeColumn(getColumnIndexByName(name));
    }

    public Column replaceColumn(Column column, int index) {
        if (isEmpty()) {
            return columns.replaceColumn(column, index);
        } else
        	return null;
    }

    private int getColumnIndexByName(String name) {
        return columns.getColumnIndexByName(name);
    }

    public Column getColumn(int index) {
        return columns.getColumnByIndex(index);
    }

    public Column getColumn(String name) {
        return columns.getColumnByIndex(getColumnIndexByName(name));
    }

    public int getColumnCount() {
        return columns.getColumnCount();
    }

    public Columns getColumns(){
        return columns;
    }

// *****************************************************************************************************************************************
// Rows
// *****************************************************************************************************************************************

    public int addRow(Row row) {
        if (checkRow(row)) {
        	return rows.addRow(row);
        }
        return -1;
    }

    public Row removeRow(int index) {
    	return rows.removeRow(index);
    }

    public Row replaceRow(Row row, int index) {
        if (checkRow(row)) {
        	return rows.replaceRow(row, index);
        } else
        	return null;
    }

    public Row getRow(int index) {
        return ((index >= 0) && (index < getRowCount())) ?  rows.getRow(index): null;
    }

    public int getRowCount() {
        return rows.getRowCount();
    }
    
    public boolean isEmpty() {
    	return getRowCount() == 0;
    }

    public void setRows(Table table) {
    	removeAll();
    	addRows(table);
    }
    
    public void addRows(Table table) {
    	for (int i = 0; i < table.getRowCount(); i++) {
    		addRow(table.getRow(i));
    	}
    }
    
// *****************************************************************************************************************************************
// Current
// *****************************************************************************************************************************************
    private int currentPosition = -1;
    
    public int getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(int position) {
        currentPosition = position;
    }

    public void first() {
        setCurrentPosition(0);
    }

    public void last() {
        setCurrentPosition(getRowCount() - 1);
    }

    public boolean next() {
    	if (getCurrentPosition() < (getRowCount() - 1)) {
    		setCurrentPosition(getCurrentPosition() + 1);
    		return true;
    	} else
    		return false;
    }

    public boolean prev() {
    	if (getCurrentPosition() > 0) {
    		setCurrentPosition(getCurrentPosition() - 1);
    		return true;
    	} else
    		return false;
    }

    public Row getCurrentRow() {
        return getRow(getCurrentPosition());
    }

    public Field getFieldByIndex(int index) {
        return getField(getCurrentPosition(), index);
    }

    public Field getFieldByName(String name) {
        return getField(getCurrentPosition(), name);
    }

// *****************************************************************************************************************************************
// Field Cell[row, col]
// *****************************************************************************************************************************************
    public Field getField(int row, int col) {
        Row r = getRow(row);
        if (r != null)
        	return r.getField(col);
        else
            return null;
    }

    public Field getField(int row, String name) {
        return getField(row, getColumnIndexByName(name));
    }

    public Field setField(int row, int col, Field field) {
        Row r = getRow(row);
        if (r != null)
        	return r.replaceField(field, col);
        else
            return null;
    }

    public Field setField(int row, String name, Field field) {
        return setField(row, getColumnIndexByName(name), field);
    }

    public Serializable getValue(int row, int col) {
    	Field f = getField(row, col);
    	if (f != null)
    		return f.getValue();
    	else
    		return null;
    }
    
    public Serializable getValue(int row, String name) {
    	return getValue(row, getColumnIndexByName(name));
    }
    
    public void setValue(int row, int col, Serializable value) {
    	Field f = getField(row, col);
    	if (f != null)
    		f.setValue(value);
    }
    
    public void setValue(int row, String name, Serializable value) {
    	setValue(row, getColumnIndexByName(name), value);
    }
}
