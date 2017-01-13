package ru.rbt.barsgl.gwt.core.datafields;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;

public class Column implements Serializable {
	private static final long serialVersionUID = 5037406166721682635L;

	public enum Type {STRING, BOOLEAN, INTEGER, DECIMAL, DATE, DATETIME, LONG};
    public enum Sort {NONE, ASC, DESC};

    // Видимый заголовок
    private String caption;
    // Имя поля в таблице БД
    private String name;
    // Тип колонки
    private Type type;
    // Возможна ли сортировка колонки
    private Sort sortType;   
    // Формат поля (для полей, у которых есть формат и не устраивает формат по умолчанию)
    private String format;
    // Ширина в пикселях
    private int width;

    // Допустимы ли значения NULL для колонки
    private boolean nullable = true;
    // Видимость колонки
    private boolean visible;
    // Показывать колонку на форме редактирования
    private boolean editable;
    // Колонка только для чтения
    private boolean readonly;
    // Позволяет сортироваться по колонке
    private boolean sortable;
    // Позволяет фильтроваться по колонке
    private boolean filterable;

    //Максимальная длина поля (пример varchar(10))
    private int maxLength = -1;

    //Значение по умолчанию
    private Field<Serializable> defValue = null;

    //Многострочный текст
    private boolean multiLine = false;
    
    private HashMap<Serializable, String> list;

    @SuppressWarnings("unused")
	Column() {}
    
    public Column(String name, Type type) {
    	this(name, type, "", 0);
    }

    public Column(String name, Type type, String caption, int width) {
    	this(name, type, caption, width, true, false);
    }

    public Column(String name, Type type, String caption, int width, boolean visible, boolean readonly) {
    	this(name, type, caption, width, visible, readonly, Sort.NONE, "");
    }

    public Column(String name, Type type, String caption, int width, boolean visible, boolean readonly, Sort sortType, String format) {
    	this.name = name;
    	this.type = type;
    	this.caption = caption;
    	this.width = width;
    	this.visible = visible;
    	this.readonly = readonly;
    	this.sortType = sortType;
    	if (format.isEmpty()) {
    		switch (type) {
			case DATE:
				this.format = "dd.MM.yyyy";
				break;
            case DATETIME:
                this.format = "dd.MM.yyyy HH:mm:ss";
                break;
            case DECIMAL:
                this.format = "#,##0.00";
                break;
            case LONG:
            case INTEGER:
                this.format = "##########";
                break;
			default:
				this.format = "";
				break;
			}
    	} else {
    		this.format = format;
    	}
    	editable = true;
        sortable = visible;
        filterable = true;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getName() {
        return name;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public Type getType() {
        return type;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }

    public Sort getSortType() {
        return sortType;
    }

    public void setSortType(Sort sortType) {
        this.sortType = sortType;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setWidth(int width) {
    	this.width = width;
    }
    
    public int getWidth() {
    	return width;
    }
    
    public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

    public boolean isSortable() {
        return sortable;
    }

    public void setSortable(boolean sortable) {
        this.sortable = sortable;
    }

    public boolean isFilterable() {
        return filterable;
    }

    public void setFilterable(boolean filterable) {
        this.filterable = filterable;
    }

    public boolean isRightValue(Object value) {
        if (value == null)
            return isNullable();
        switch (getType()) {
            case BOOLEAN:
                if (value instanceof Boolean)
                    return true;
                break;
            case INTEGER:
                if (value instanceof Integer)
                    return true;
                break;
            case DECIMAL:
                if (value instanceof BigDecimal)
                    return true;
                break;
            case STRING:
                if (value instanceof String)
                    return true;
                break;
            case DATE:
                if ((value instanceof java.util.Date) || (value instanceof java.sql.Date))
                    return true;
                break;
            case LONG:
                if (value instanceof Long)
                    return true;
                break;
        }
        return false;
    }

    public HashMap<Serializable, String> getList() {
        return list;
    }

    public void setList(HashMap<Serializable, String> list) {
        this.list = list;
    }

    public boolean isList(){
        return list != null;
    }

    public int getMaxLength(){
        return maxLength;
    }

    public void setMaxLength(int length){
        maxLength = length;
    }
    
    public Field<Serializable> getDefValue(){
    	return defValue;
    }
    
    public void setDefValue(Field<Serializable> defValue){
    	this.defValue = defValue;
    }
    
    public boolean isMultiLine(){
    	return multiLine;
    }
    
    public void setMultiLine(boolean multiLine){
    	this.multiLine = multiLine;
    }

    public Type getType(String typeStr) {
        for (Type type : Type.values()) {
            if(type.toString().equals(typeStr))
                return type;
        }
        return null;
    }

    public Column cloneThis() {
        Column column = new Column();
        column.setCaption(getCaption());
        column.setMultiLine(isMultiLine());
        column.setDefValue(getDefValue());
        column.setMaxLength(getMaxLength());
        column.setEditable(isEditable());
        column.setFilterable(isFilterable());
        column.setFormat(getFormat());
        column.setList(getList());
        column.setMultiLine(isMultiLine());
        column.setName(getName());
        column.setNullable(isNullable());
        column.setReadonly(isReadonly());
        column.setSortable(isSortable());
        column.setSortType(getSortType());
        column.setType(getType());
        column.setVisible(isVisible());
        column.setWidth(getWidth());
        return column;
    }
}
