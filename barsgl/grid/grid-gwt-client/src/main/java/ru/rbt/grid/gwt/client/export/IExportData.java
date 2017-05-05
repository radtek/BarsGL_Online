package ru.rbt.grid.gwt.client.export;

import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;

import java.util.List;

/**
 * Created by akichigi on 21.03.17.
 */
public interface IExportData {
    String sql();
    Columns columns();
    List<FilterItem> masterFilterItems();
    List<FilterItem> detailFilterItems();
    List<SortItem> sortItems();
}
