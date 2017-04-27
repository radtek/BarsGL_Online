package ru.rbt.grid.gwt.client.gridForm;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.core.actions.Action;
import ru.rbt.barsgl.gwt.core.actions.FilterAction;
import ru.rbt.barsgl.gwt.core.actions.RefreshAction;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Field;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.forms.BaseForm;
import ru.rbt.barsgl.gwt.core.forms.IDisposable;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.ActionBarWidget;
import ru.rbt.barsgl.gwt.core.widgets.GridDataProvider;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.shared.enums.SecurityActionCode;

import java.io.Serializable;
import java.util.List;
import ru.rbt.barsgl.gwt.core.statusbar.StatusBarManager;
import ru.rbt.grid.gwt.client.GridEntryPoint;
import ru.rbt.grid.gwt.client.export.Export2ExcelAction;

/**
 * Created by akichigi on 27.04.15.
 */
public abstract class GridForm extends BaseForm implements IDisposable {
	protected GridWidget grid;
	protected Table table;
	protected String sql_select;
	protected RefreshAction refreshAction;
	protected FilterAction filterAction;
	protected Export2ExcelAction exportToExcel;

	protected ActionBarWidget abw;

	public GridForm(String title) {
		this(title, false);
	}

	public GridForm(String title, boolean delayLoad) {
		super(delayLoad);
		super.title.setText(title);
		exportToExcel.setFormTitle(title);
	}

	/*
	 * public GridForm(String title, int pageSize){ super(pageSize);
	 * super.title.setText(title); }
	 */
	@Override
	public Widget createContent() {
		table = prepareTable();
		sql_select = prepareSql();
		//System.out.println(sql_select);
		abw = new ActionBarWidget();

		DockLayoutPanel panel = new DockLayoutPanel(Style.Unit.MM);
		panel.addNorth(abw, 10);

		panel.add(grid = createGrid(getInitialFilterParams(), delayLoad));

		abw.addAction(refreshAction = createRefreshAction());
		grid.setSortEvents(refreshAction);

		abw.addAction(filterAction = createFilterAction());
        abw.addSecureAction(exportToExcel = new Export2ExcelAction(grid, sql_select), SecurityActionCode.OperToExcel);

		return panel;
	}

	public void setExcelSql(String sql) {
		exportToExcel.setSql(sql);
	}

	protected GridWidget createGrid(final Object[] initialFilterParams, final boolean delayLoad) {
		return new GridWidget(table, new GridDataProvider(delayLoad) {
			@Override
			protected void getServerCount(AsyncCallback<Integer> callback) {
				GridEntryPoint.asyncGridService.getAsyncCount(sql_select, getFilterCriteria(initialFilterParams), callback);
			}

			@Override
			protected void getServerData(int start, int pageSize, AsyncCallback<List<Row>> callback) {
				List<SortItem> sortItems = getSortCriteria();
				List<FilterItem> filterItems = getFilterCriteria(initialFilterParams);
				refreshGridParams(filterItems, sortItems);
				GridEntryPoint.asyncGridService.getAsyncRows(sql_select, table.getColumns(), start, pageSize,
						filterItems, sortItems, callback);
			};
		}, 30);
	}

	protected List<SortItem> getSortCriteria() {
		if (null == grid) // это первый запуск, еще до создания grid
			return getInitialSortCriteria();
		List<SortItem> sortCriteria = grid.getSortCriteria();
		if (null == sortCriteria && !grid.isInitialSortCriteria()) {
			// это первый запуск после создания grid
			grid.setInitialSortCriteria(sortCriteria = getInitialSortCriteria());
		}
		return sortCriteria;
	}

	protected List<FilterItem> getFilterCriteria(Object[] initialFilterParams) {
		if (null == grid) // это первый запуск, еще до создания grid
			return getInitialFilterCriteria(initialFilterParams);
		List<FilterItem> filterCriteria = grid.getFilterCriteria();
		if (null == filterCriteria && !grid.isInitialFilterCriteria()) {
			// это первый запуск после создания grid
			grid.setInitialFilterCriteria(filterCriteria = getInitialFilterCriteria(initialFilterParams));
		}
		return filterCriteria;
	}

	protected void refreshGridParams(List<FilterItem> filterCriteria, List<SortItem> sortCriteria) {
		grid.setFilterCriteria(filterCriteria);
		refreshAction.setFilterCriteria(filterCriteria);
		grid.setSortCriteria(sortCriteria);
		refreshAction.setSortCriteria(sortCriteria);
		changeFilterStatus(filterAction, (filterCriteria != null) && (!filterCriteria.isEmpty()));
	}

	private RefreshAction createRefreshAction(){
		return new RefreshAction(grid) {
			@Override
			public void onRefresh(List<FilterItem> filterCriteria, List<SortItem> sortCriteria,
					List<FilterItem> linkMDFilterCriteria) {
				refreshGridParams(filterCriteria, sortCriteria);
				grid.refresh();
			}
		};
	}

	private void changeFilterStatus(Action action, boolean on) {
		action.setImage(on ? new Image(ImageConstants.INSTANCE.filterOn()) : new Image(ImageConstants.INSTANCE.filter()));
		action.setHint(on ? "Фильтр: Вкл." : "Фильтр: Откл.");
	}

	public Serializable getValue(String colName){
		Columns columns = grid.getTable().getColumns();
		return grid.getCurrentRow().getField(columns.getColumnIndexByName(colName)).getValue();
	}

	public void setSql(String text){
		sql_select = text;
	}

	private FilterAction createFilterAction() {
		return new FilterAction(grid);
	}

	protected abstract Table prepareTable();

	protected abstract String prepareSql();

	protected List<SortItem> getInitialSortCriteria() {
		return null;
	};

	protected List<FilterItem> getInitialFilterCriteria(Object[] initialFilterParams) {
		return null;
	}

	protected Object[] getInitialFilterParams() {
		return null;
	}

	@Override
	public void dispose() {
		StatusBarManager.ChangeStatusBarText("", StatusBarManager.MessageReason.MSG);
		abw.dispose();
	}

	public Table getTable() {
		return this.table;
	}

	public GridWidget getGrid() {
		return this.grid;
	}

	public Field getFieldByName(String name){
		Row row = grid.getCurrentRow();
		if (row == null) return null;

		Columns columns = table.getColumns();
		int ind = columns.getColumnIndexByCaption(name);
		if (ind >= 0) {
		 	return row.getField(ind);
		}else{
			return null;
		}
	}
}
