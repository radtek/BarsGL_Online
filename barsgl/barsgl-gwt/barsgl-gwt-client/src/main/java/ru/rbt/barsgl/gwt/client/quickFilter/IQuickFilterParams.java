package ru.rbt.barsgl.gwt.client.quickFilter;

import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;

import java.util.ArrayList;

/**
 * Created by ER18837 on 27.04.16.
 */
public interface IQuickFilterParams {
    public void setInitialFilterParams(Object[] params);
    public ArrayList<FilterItem> getFilter();
}
