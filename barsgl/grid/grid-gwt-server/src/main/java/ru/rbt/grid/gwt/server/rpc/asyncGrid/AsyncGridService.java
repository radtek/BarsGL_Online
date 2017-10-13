package ru.rbt.grid.gwt.server.rpc.asyncGrid;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.Export.ExcelExportHead;
import ru.rbt.barsgl.shared.Repository;

import java.io.Serializable;
import java.util.List;

/**
 * Created by akichigi on 02.04.15.
 */

@RemoteServiceRelativePath("service/AsyncGridService")
public interface AsyncGridService extends RemoteService {
    Integer getAsyncCount(String sql, List<FilterItem> filterCriteria) throws Throwable;
    List<Row> getAsyncRows(String sql, Columns columns, int start, int pageSize, List<FilterItem> filterCriteria,
                           List<SortItem> sortCriteria) throws Throwable;
    Row selectOne(String sql, Serializable[] params) throws Throwable;
    Row selectFirst(String sql, Serializable[] params) throws Throwable;
    void Debug(String msg) throws Exception;
    String export2Excel(String sql, Columns columns, List<FilterItem> filterCriteria,
                        List<SortItem> sortCriteria, ExcelExportHead head) throws Throwable;
    Integer getAsyncCount(Repository repository, String sql, List<FilterItem> filterCriteria) throws Throwable;
    List<Row> getAsyncRows(Repository repository, String sql, Columns columns, int start, int pageSize, List<FilterItem> filterCriteria,
                           List<SortItem> sortCriteria) throws Throwable;
    Row selectOne(Repository repository, String sql, Serializable[] params) throws Throwable;
    String export2Excel(Repository repository, String sql, Columns columns, List<FilterItem> filterCriteria,
                        List<SortItem> sortCriteria, ExcelExportHead head) throws Throwable;
}
