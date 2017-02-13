package ru.rbt.barsgl.gwt.client.operday;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.security.gwt.client.CommonEntryPoint;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.widgets.GridDataProvider;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ivan Sevastyanov on 27.10.2016.
 */
public class EmbeddableGridWidget extends GridWidget {

    private final GridDataProvider dataProvider;
    private final List<FilterItem> filter = new ArrayList<>();
    private final List<SortItem> sort = new ArrayList<>();
    private final String sql;

    public EmbeddableGridWidget (final Table table, final String sql) {
        super(table);
        this.sql = sql;
        dataProvider = new GridDataProvider(true) {
            @Override
            protected void getServerCount(AsyncCallback<Integer> callback) {
                CommonEntryPoint.asyncGridService.getAsyncCount(sql, filter, callback);
            }

            @Override
            protected void getServerData(int start, int pageSize, AsyncCallback<List<Row>> callback) {
                CommonEntryPoint.asyncGridService.getAsyncRows(sql, table.getColumns(), start, pageSize, filter, sort, callback);
            }
        };
        setDataProvider(dataProvider);
    }

    public void setFilter(List<FilterItem> filter) {
        this.filter.clear();
        this.filter.addAll(filter);
    }

    public void setSorting(List<SortItem> sort) {
        sort.clear(); sort.addAll(sort);
    }

}
