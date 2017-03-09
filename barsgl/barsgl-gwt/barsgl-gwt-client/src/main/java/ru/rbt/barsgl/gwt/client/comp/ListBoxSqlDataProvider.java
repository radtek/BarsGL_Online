package ru.rbt.barsgl.gwt.client.comp;

import com.google.gwt.user.client.Window;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.grid.gwt.client.GridEntryPoint;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;

import java.util.List;

import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.trimStr;

/**
 * Created by Ivan Sevastyanov
 */
public class ListBoxSqlDataProvider implements ListBoxDataProvider {

    protected IDataListBox listBox;

    private final boolean withEmptyValue;
    protected String selectValue;
    protected final String sql;
    protected final Columns columns;
    protected final List<FilterItem> filter;
    protected final List<SortItem> sort;
    private final RowConverter converter;

    public ListBoxSqlDataProvider(
    		boolean withEmptyValue, String selectValue, String sql
            , Columns columns, List<FilterItem> filter, List<SortItem> sort, RowConverter converter) {
        this.withEmptyValue = withEmptyValue;
        this.selectValue = trimStr(selectValue);
        this.sql = sql;
        this.columns = columns;
        this.filter = filter;
        this.sort = sort;
        this.converter = converter;
    }

    public ListBoxSqlDataProvider( 
    		String selectValue, String sql
            , Columns columns, List<FilterItem> filter, List<SortItem> sort, RowConverter converter) {
    	this(false, selectValue, sql, columns, filter, sort, converter);
    }

    public ListBoxSqlDataProvider(String sql, Columns columns, RowConverter converter) {
        this(false, null, sql, columns, null, null, converter);
    }

    @Override
    public void provide(final DataListBox listBox) {
        this.listBox = listBox;
        GridEntryPoint.asyncGridService.getAsyncRows(sql, columns, 0, 10000, filter, sort, new AuthCheckAsyncCallback<List<Row>>() {
            @Override
            public void onFailureOthers(Throwable caught) {
                Window.alert("Ошибка получения данных для ListBox.\nОшибка: " + caught.getLocalizedMessage());
            }

            @Override
            public void onSuccess(List<Row> result) {
                listBox.clear();
                initList(result);
                setSelected(selectValue);
                listBox.sendCompleteMessage();
            }
        });
    }


    protected void initList(List<Row> rows) {
        if (withEmptyValue)
            listBox.addItem(null, "", null);
        for (Row row : rows) {
            listBox.addItem(converter.getKey(row), converter.getText(row), row);
        }
    }

    public void setSelected(String value) {
        listBox.setValue(value);
    }

	@Override
	public void setSelectValue(String selectValue) {
		this.selectValue = trimStr(selectValue);
	}

    @Override
    public Columns getColumns() {
        return columns;
    }
}
