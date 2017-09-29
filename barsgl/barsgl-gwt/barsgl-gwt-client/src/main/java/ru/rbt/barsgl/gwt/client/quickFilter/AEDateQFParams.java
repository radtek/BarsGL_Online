package ru.rbt.barsgl.gwt.client.quickFilter;

import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.shared.filter.FilterCriteria;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by akichigi on 02.08.16.
 */
public class AEDateQFParams implements IQuickFilterParams {
    public enum DateFilterField {
        PKG_DATE("Дата пакета"), VALUE_DATE("Дата валютирования");
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

    public AEDateQFParams(Column fieldPkg, Column fieldValue, DateFilterField filterField) {
        this.filterField = filterField;
        DateFilterField.PKG_DATE.setColumn(fieldPkg);
        DateFilterField.VALUE_DATE.setColumn(fieldValue);
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
