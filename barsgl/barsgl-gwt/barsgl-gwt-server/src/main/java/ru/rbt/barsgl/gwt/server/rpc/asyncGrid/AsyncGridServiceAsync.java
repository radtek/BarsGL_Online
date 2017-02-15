package ru.rbt.barsgl.gwt.server.rpc.asyncGrid;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.Export.ExcelExportHead;
import ru.rbt.barsgl.shared.enums.Repository;

import java.io.Serializable;
import java.util.List;

/**
 * Created by akichigi on 02.04.15.
 */
public interface AsyncGridServiceAsync {
    void getAsyncCount(String sql, List<FilterItem> filterCriteria, AsyncCallback<Integer> callback);
    void getAsyncRows(String sql, Columns columns, int start, int pageSize, List<FilterItem> filterCriteria,
                      List<SortItem> sortCriteria, AsyncCallback<List<Row>> callback);
    void selectOne(String sql, Serializable[] params, AsyncCallback<Row> callback);
    void Debug(String msg,  AsyncCallback<Void> callback);
    void export2Excel(String sql, Columns columns, List<FilterItem> filterCriteria,
                      List<SortItem> sortCriteria, ExcelExportHead head, AsyncCallback<String> callback);

    void getAsyncCount(Repository repository, String sql, List<FilterItem> filterCriteria, AsyncCallback<Integer> callback);
    void getAsyncRows(Repository repository, String sql, Columns columns, int start, int pageSize, List<FilterItem> filterCriteria,
                      List<SortItem> sortCriteria, AsyncCallback<List<Row>> callback);
    void selectOne(Repository repository, String sql, Serializable[] params, AsyncCallback<Row> callback);
    void export2Excel(Repository repository, String sql, Columns columns, List<FilterItem> filterCriteria,
                      List<SortItem> sortCriteria, ExcelExportHead head, AsyncCallback<String> callback);
}
