package ru.rbt.barsgl.gwt.client.quickFilter;

import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.shared.filter.FilterCriteria;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by ER18837 on 29.03.16.
 */
public class DateQuickFilterParams implements IQuickFilterParams{
    public enum DateFilterField {
    	CREATE_DATE("Дата опердня"), VALUE_DATE("Дата валютирования"), POST_DATE("Дата проводки");
        private String title;
        private Column column;

        public Column getColumn() {
            return column;
        }

        public void setColumn(Column column) {
            this.column = column;
        }

        DateFilterField(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

    };

    protected Date dateBegin;
    protected Date dateEnd;
    protected DateFilterField filterField;

    public DateQuickFilterParams(Column fieldCreate, Column fieldValue, Column fieldPost, DateFilterField filterField) {
        this.filterField = filterField;
        DateFilterField.CREATE_DATE.setColumn(fieldCreate);
        DateFilterField.VALUE_DATE.setColumn(fieldValue);
        DateFilterField.POST_DATE.setColumn(fieldPost);
    }


    public Date getDateBegin() {
        return dateBegin;
    }

    public void setDateBegin(Date dateBegin) {
        this.dateBegin = dateBegin;
    }

    public Date getDateEnd() {
        return dateEnd;
    }

    public void setDateEnd(Date dateEnd) {
        this.dateEnd = dateEnd;
    }

    public void setFilterField(DateFilterField filterField) {
        this.filterField = filterField;
    }

    public DateFilterField getFilterField() {
        return filterField;
    }

    @Override
    public void setInitialFilterParams(Object[] params) {
        this.dateBegin = (Date)params[0];
        this.dateEnd = (Date)params[1];
    }

    @Override
    public ArrayList<FilterItem> getFilter() {
        ArrayList<FilterItem> list = new ArrayList<>();
        list.add(new FilterItem(filterField.column, FilterCriteria.GE, dateBegin));
        list.add(new FilterItem(filterField.column, FilterCriteria.LE, dateEnd));
        return list;
    }

}
