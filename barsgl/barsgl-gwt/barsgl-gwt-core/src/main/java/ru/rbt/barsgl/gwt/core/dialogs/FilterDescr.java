package ru.rbt.barsgl.gwt.core.dialogs;

import ru.rbt.barsgl.shared.filter.FilterCriteria;

import java.io.Serializable;

/**
 * Created by er18837 on 23.10.2018.
 */
public class FilterDescr {
    public String columnName;
    public FilterCriteria criteria;
    public Serializable value;

    public FilterDescr(String columnName, FilterCriteria criteria, Serializable value) {
        this.columnName = columnName;
        this.criteria = criteria;
        this.value = value;
    }
}
