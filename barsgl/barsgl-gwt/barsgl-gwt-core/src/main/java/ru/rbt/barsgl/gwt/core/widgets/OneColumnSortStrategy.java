package ru.rbt.barsgl.gwt.core.widgets;

import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortList;
import ru.rbt.barsgl.gwt.core.datafields.Column.Sort;

import java.util.ArrayList;

/**
 * Created by akichigi on 21.04.15.
 */
public class OneColumnSortStrategy implements ISortStrategy {
    @Override
    public ArrayList<SortItem> getSortCriteria(ColumnSortEvent event) {
        ArrayList<SortItem> sortCriteria = null;
        Column<?, ?> col = event.getColumn();
        boolean asc = event.isSortAscending();

        if (col != null && col.isSortable()) {
            sortCriteria = new ArrayList<>();
            sortCriteria.add(new SortItem(event.getColumn().getDataStoreName(), asc ? Sort.ASC : Sort.DESC));

            ColumnSortList.ColumnSortInfo info =  event.getColumnSortList().push(col);
            if (info.isAscending() != asc) event.getColumnSortList().push(col);
        }
        return sortCriteria;
    }
}
