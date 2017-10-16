package ru.rbt.barsgl.gwt.client.quickFilter;

import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.shared.filter.FilterCriteria;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;

import java.util.ArrayList;
import java.util.Date;

import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;

/**
 * Created by akichigi on 21.02.17.
 */
public class ErrorQFilterParams implements IQuickFilterParams {
    private Column colDealSource;
    private Column colOperDay;

    public String getDealSource() {
        return dealSource;
    }

    public void setDealSource(String dealSource) {
        this.dealSource = dealSource;
    }

    public Date getOperDay() {
        return operDay;
    }

    public void setOperDay(Date operDay) {
        this.operDay = operDay;
    }

    private String dealSource;
    private Date operDay;

    public ErrorQFilterParams(Column colDealSource, Column colOperDay){
        this.colDealSource = colDealSource;
        this.colOperDay = colOperDay;
    }


    @Override
    public void setInitialFilterParams(Object[] params) {
        if (null == params)
            return;
        dealSource = (String)params[0];
        operDay = (Date)params[1];
    }

    @Override
    public ArrayList<FilterItem> getFilter() {
        ArrayList<FilterItem> list = new ArrayList<>();

        if (!isEmpty(dealSource))
            list.add(new FilterItem(colDealSource, FilterCriteria.EQ, dealSource));

        if (null != operDay)
            list.add(new FilterItem(colOperDay, FilterCriteria.EQ, operDay));

        return list;
    }
}
