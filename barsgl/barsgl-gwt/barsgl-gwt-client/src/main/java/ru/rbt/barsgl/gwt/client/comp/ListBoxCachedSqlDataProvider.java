package ru.rbt.barsgl.gwt.client.comp;

import com.google.gwt.user.client.Window;
import ru.rbt.barsgl.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.Utils;

import java.util.List;

/**
 * Created by akichigi on 03.11.16.
 */
public class ListBoxCachedSqlDataProvider extends ListBoxSqlDataProvider {
    public static final String CACHED_LIST_PREFIX = "_ListOf_{0}_";
    private String name;
    public ListBoxCachedSqlDataProvider(String name, boolean withEmptyValue, String selectValue, String sql, Columns columns, List<FilterItem> filter, List<SortItem> sort, RowConverter converter) {
        super(withEmptyValue, selectValue, sql, columns, filter, sort, converter);
        this.name = Utils.Fmt(CACHED_LIST_PREFIX, name);
    }

    public ListBoxCachedSqlDataProvider(String name, String selectValue, String sql, Columns columns, List<FilterItem> filter, List<SortItem> sort, RowConverter converter) {
        this(name, false, selectValue, sql, columns, filter, sort, converter);
    }

    public ListBoxCachedSqlDataProvider(String name, String sql, Columns columns, RowConverter converter) {
        this(name, false, null, sql, columns, null, null, converter);
    }

    @Override
    public void provide(final DataListBox listBox) {
        this.listBox = listBox;

        BarsGLEntryPoint.asyncGridService.getAsyncRows(sql, columns, 0, 10000, filter, sort, new AuthCheckAsyncCallback<List<Row>>() {
            @Override
            public void onFailureOthers(Throwable caught) {
                Window.alert("Ошибка получения данных для ListBox.\nОшибка: " + caught.getLocalizedMessage());
            }

            @Override
            public void onSuccess(List<Row> result) {
                initList(result);
                setSelected(selectValue);
                LocalDataStorage.putParam(name, listBox);
                listBox.sendCompleteMessage();
                //System.out.println("Create " + name);
            }
        });
    }
}
