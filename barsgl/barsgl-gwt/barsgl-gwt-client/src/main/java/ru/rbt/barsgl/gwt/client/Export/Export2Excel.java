package ru.rbt.barsgl.gwt.client.Export;

import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.dialogs.FilterUtils;
import ru.rbt.barsgl.gwt.core.utils.DialogUtils;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.Export.ExcelExportHead;

import java.util.List;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;

/**
 * Created by akichigi on 20.03.17.
 */
public class Export2Excel {
    private String sql;
    private Columns columns;
    private List<FilterItem> masterFilterItems;
    private List<FilterItem> detailFilterItems;
    private List<SortItem> sortItems;
    private ExcelExportHead head;
    private ExportActionCallback callback;

    public Export2Excel(String sql, Columns columns, List<FilterItem> masterFilterItems,
                        List<FilterItem> detailFilterItems, List<SortItem> sortItems, ExcelExportHead head,
                        ExportActionCallback callback){

        this.sql = sql;
        this.columns = columns;
        this.masterFilterItems = masterFilterItems;
        this.detailFilterItems = detailFilterItems;
        this.sortItems = sortItems;
        this.head = head;
        this.callback = callback;
    }

    public void export() {

        DialogUtils.showInfo(TEXT_CONSTANTS.export2Excel());
        List<FilterItem> filterItems = FilterUtils.combineFilterCriteria(masterFilterItems, detailFilterItems);

        BarsGLEntryPoint.asyncGridService.export2Excel(sql, columns, filterItems, sortItems, head, callback);
    }
}
