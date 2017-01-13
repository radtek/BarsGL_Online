package ru.rbt.barsgl.gwt.client.quickFilter;

import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.dialogs.FilterCriteria;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by akichigi on 03.06.16.
 */
public class DateIntervalQuickFilterParams implements IQuickFilterParams{
    private Date dateBegin;
    private Date dateEnd;
    private Column dateColumn;

    public DateIntervalQuickFilterParams(Column dateColumn){
        this.dateColumn = dateColumn;
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

    @Override
    public void setInitialFilterParams(Object[] params) {
        this.dateBegin = (Date)params[0];
        this.dateEnd = (Date)params[1];
    }

    @Override
    public ArrayList<FilterItem> getFilter() {
        ArrayList<FilterItem> list = new ArrayList<FilterItem>();
        list.add(new FilterItem(dateColumn, FilterCriteria.GE, dateBegin));
        list.add(new FilterItem(dateColumn, FilterCriteria.LE, dateEnd));
        return list;
    }
}
