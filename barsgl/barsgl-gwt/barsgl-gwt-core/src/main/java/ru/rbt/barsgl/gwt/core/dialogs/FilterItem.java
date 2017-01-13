package ru.rbt.barsgl.gwt.core.dialogs;

/**
 * Created by akichigi on 17.04.15.
 */

import com.google.gwt.i18n.client.DateTimeFormat;
import ru.rbt.barsgl.gwt.core.datafields.Column;

import java.io.Serializable;
import java.util.Date;

public class FilterItem implements Serializable {
    private static final long serialVersionUID = -294322225561414413L;
    public static final String DATE_FORMAT = "dd.MM.yyyy";

    private String sqlName;
    private Serializable sqlValue;
    private String name;
    private Serializable value;
    private FilterCriteria criteria;
    private boolean pined;
    private boolean isReadOnly = false;

    public FilterItem(){}

    public FilterItem(Column column, FilterCriteria criteria, Serializable value) {
        this(column, criteria, value, false);
    }

    public FilterItem(Column column, FilterCriteria criteria, Serializable value, boolean pined) {
        this(column.getName(), column.getType(), criteria, value, pined);
    }

    public FilterItem(String name, Column.Type type, FilterCriteria criteria, Serializable value, boolean pined) {
        this.name = name;
        this.value = value;
        this.criteria = criteria;
        this.pined = pined;
        this.sqlName = name;
        this.sqlValue = value;
        if (null != value ) {
            // преобразуем дату в строку для передачи на сервер (из-за возможной ошибки на 1 день)
            switch (type) {
            case DATETIME:
                this.sqlName = "DATE(" + name + ")";
                this.sqlValue = DateTimeFormat.getFormat(DATE_FORMAT).format((Date)value);
                break;
            case DATE:
                this.sqlName = name;
                this.sqlValue = DateTimeFormat.getFormat(DATE_FORMAT).format((Date)value);
                break;
            }
        }
    }

    public String getName() {
        return name;
    }

    public Serializable getValue() {
        return value;
    }

    public FilterCriteria getCriteria() {
        return criteria;
    }

    public boolean needValue() { return criteria.isBinary(); };

    public boolean isPined() {
        return pined;
    }

    public Serializable getSqlValue() {
        return sqlValue;
    }

    public String getSqlName() {
        return sqlName;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public void setReadOnly(boolean isReadOnly) {
        this.isReadOnly = isReadOnly;
    }
}
