package ru.rbt.grid.gwt.client.export;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.dialogs.FilterUtils;
import ru.rbt.barsgl.gwt.core.utils.DialogUtils;
import ru.rbt.barsgl.shared.Export.ExcelExportHead;
import ru.rbt.barsgl.shared.Repository;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.grid.gwt.client.GridEntryPoint;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;

import java.util.List;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;

/**
 * Created by akichigi on 20.03.17.
 */
public class Export2Excel {
    private ExcelExportHead head;
    private ExportActionCallback callback;
    private IExportData data;

    public Export2Excel(IExportData data, ExcelExportHead head,
                        ExportActionCallback callback){

        this.data = data;
        this.head = head;
        this.callback = callback;
    }

    public void export() {
        export(false);
    }

    public void export(boolean allrows) {
        DialogUtils.showInfo(TEXT_CONSTANTS.export2Excel());
        List<FilterItem> filterItems = FilterUtils.combineFilterCriteria(data.masterFilterItems(), data.detailFilterItems());
        GridEntryPoint.asyncGridService.export2Excel(Repository.BARSGL, data.sql(), data.columns(), filterItems, data.sortItems(), head, allrows, callback);
    }

    public void exportSort(boolean allrows) {
        DialogUtils.showInfo(TEXT_CONSTANTS.export2Excel());
        List<FilterItem> filterItems = FilterUtils.combineFilterCriteria(data.masterFilterItems(), data.detailFilterItems());
        GridEntryPoint.asyncGridService.export2ExcelSort(Repository.BARSGL, data.sql(), data.columns(), filterItems, data.sortItems(), head, allrows, callback);
    }

    public void exportExists(AsyncCallback<RpcRes_Base<Boolean>> cb) {
        List<FilterItem> filterItems = FilterUtils.combineFilterCriteria(data.masterFilterItems(), data.detailFilterItems());
        GridEntryPoint.asyncGridService.export2ExcelExists(Repository.BARSGL, data.sql(), filterItems, cb);
    }
}
