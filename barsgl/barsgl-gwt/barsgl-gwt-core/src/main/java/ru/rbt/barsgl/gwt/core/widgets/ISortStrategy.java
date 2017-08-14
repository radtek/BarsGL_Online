package ru.rbt.barsgl.gwt.core.widgets;

import com.google.gwt.user.cellview.client.ColumnSortEvent;

import java.util.ArrayList;

/**
 * Created by akichigi on 21.04.15.
 */
public interface ISortStrategy {
     ArrayList<SortItem> getSortCriteria(ColumnSortEvent event);
}
