package ru.rbt.barsgl.gwt.core.dialogs;

/**
 * Created by akichigi on 17.04.15.
 */

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.rpc.IsSerializable;
import ru.rbt.barsgl.gwt.core.datafields.Column;

import java.io.Serializable;
import java.util.Date;

public class FilterItem implements Serializable, IsSerializable {
	private static final long serialVersionUID = -3960734068373764479L;

//	public static final String DATE_FORMAT = "dd.MM.yyyy";
	public static final String DATE_FORMAT = "yyyy-MM-dd";

    private String sqlName;
    private Serializable sqlValue;
    private String name;
    private Serializable value;
    private FilterCriteria criteria;
    private boolean pined;
    private boolean isReadOnly = false;
    private String caption;
    private String strValue;

    public FilterItem(){}

    public FilterItem(Column column, FilterCriteria criteria, Serializable value) {
        this(column, criteria, value, false);
    }

    public FilterItem(Column column, FilterCriteria criteria, Serializable value, boolean pined, boolean readOnly) {
        this(column.getName(), column.getType(), column.getCaption(), criteria, value, pined);
        this.setReadOnly(readOnly);
    }

    public FilterItem(Column column, FilterCriteria criteria, Serializable value, boolean pined) {
        this(column.getName(), column.getType(), column.getCaption(), criteria, value, pined);
    }

    public FilterItem(String name, Column.Type type, String caption, FilterCriteria criteria, Serializable value, boolean pined) {
        this.name = name;
        this.value = value;
        this.criteria = criteria;
        this.pined = pined;
        this.sqlName = name;
        this.caption = caption;
        this.strValue = (null != value) ? value.toString() : "";
        this.sqlValue = value;

        if (null != value ) {
            // преобразуем дату в строку для передачи на сервер (из-за возможной ошибки на 1 день)
            switch (type) {
            case DATETIME:
                this.sqlName = "TRUNC(" + name + ")";
                this.strValue = DateTimeFormat.getFormat(DATE_FORMAT).format((Date)value);
                this.sqlValue = this.strValue;
                break;
            case DATE:
                this.sqlName = name;
                this.strValue = DateTimeFormat.getFormat(DATE_FORMAT).format((Date)value);
                this.sqlValue = this.strValue;
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

    public boolean needValue() { return criteria.isBinary(); }

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

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption){
        this.caption = caption;
    }

    public String getStrValue() {
        return strValue;
    }

    public void setStrValue(String strValue) {
        this.strValue = strValue;
    }
}
