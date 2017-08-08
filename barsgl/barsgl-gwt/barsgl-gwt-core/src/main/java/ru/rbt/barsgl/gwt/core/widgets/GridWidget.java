package ru.rbt.barsgl.gwt.core.widgets;

import com.google.gwt.cell.client.DateCell;
import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.Handler;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import ru.rbt.barsgl.gwt.core.datafields.*;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.events.GridEvents;
import ru.rbt.barsgl.gwt.core.events.LocalEventBus;
import ru.rbt.barsgl.gwt.core.utils.UUID;
import ru.rbt.barsgl.shared.filter.IFilterItem;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GridWidget extends Composite implements IProviderEvents{
	private DockLayoutPanel panel;
	private DataGrid2<Row> grid;
	private Table table;
	private SimplePager pager;
	private SingleSelectionModel<Row> selector;
	private int pageSize;
    private ISortStrategy sortStrategy;
	private ISortEvents sortEvents;
	private IGridRowChanged rowChangedEvent;
    private String id;

    private List<SortItem> sortCriteria = null;
	private List<SortItem> initialSortCriteria = null;
	private boolean isInitialSortCriteria = false;

	private List<FilterItem> filterCriteria = null;
	private List<FilterItem> initialFilterCriteria = null;
	private boolean isInitialFilterCriteria = false;

	private final static int DEF_PAGE_SIZE = 15;
	private final static int MIN_PAGE_SIZE = 10;
	private final static int MAX_PAGE_SIZE = 50;
	private final static String EMPTY_MESSAGE = "Нет данных";

	private boolean doubleRefreshError = false; //fucking crutch
	private boolean passNull = false; // return double null

	private GridDataProvider provider;

    public ICellValueEvent getCellValueEventHandler() {
        return cellValueEventHandler;
    }

    public void setCellValueEventHandler(ICellValueEvent cellValueEventHandler) {
        this.cellValueEventHandler = cellValueEventHandler;
    }

    private ICellValueEvent cellValueEventHandler;

	public interface TableResources extends DataGrid2.Resources {
		interface TableStyle extends DataGrid2.Style {}
		
		@Override
		@Source({DataGrid2.Style.DEFAULT_CSS, "GridWidget.css"})
		TableStyle dataGridStyle();
	}

	public GridWidget(Table table) {
		this(table, null, DEF_PAGE_SIZE);
	}

	public GridWidget(Table table, GridDataProvider provider) {
		this(table, provider, DEF_PAGE_SIZE);
	}

	public GridWidget(Table table, int pageSize) {
		this(table, null, pageSize);
	}

	public GridWidget(Table table, GridDataProvider provider, int pageSize) {
		this.table = table;

		panel = new DockLayoutPanel(Unit.MM);

		grid = new DataGrid2<Row>(pageSize, (TableResources)GWT.create(TableResources.class));
		grid.setEmptyTableWidget(new Label(EMPTY_MESSAGE));
		setPageSize(pageSize);
		prepareColumns();

		pager = new GridPager(pageSize); //SimplePager();
		pager.setDisplay(grid);

        sortStrategy = new OneColumnSortStrategy();

		id = UUID.randomUUID();


		setDataProvider(provider == null ? createDefProvider() : provider);

		selector = new SingleSelectionModel<Row>();

		selector.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
			@Override
			public void onSelectionChange(SelectionChangeEvent event) {
				Row row = selector.getSelectedObject();

				if (doubleRefreshError)
					doubleRefreshError = false;
				else
					fireRowChangedEvent(row);
			}
		});

		grid.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.BOUND_TO_SELECTION);

		grid.addLoadingStateChangeHandler(new LoadingStateChangeEvent.Handler() {
			@Override
			public void onLoadingStateChanged(LoadingStateChangeEvent event) {

				if ((event.getLoadingState() == LoadingStateChangeEvent.LoadingState.LOADED) && grid.isEmpty()) {
					fireRowChangedEvent(null);
				}
			}
		});

		grid.setSelectionModel(selector);
		grid.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.BOUND_TO_SELECTION);

		grid.addColumnSortHandler(new Handler() {
			@Override
			public void onColumnSort(ColumnSortEvent event) {
				if (sortEvents != null){
					if (sortStrategy != null){
					    sortEvents.onSorted(sortStrategy.getSortCriteria(event));
			    	}
			    }
			}
		});

		panel.addSouth(pager, 10);
		panel.add(grid);
		initWidget(panel);
	}

	private GridDataProvider createDefProvider() {
		return new GridDataProvider() {
			@Override
			protected void getServerCount(AsyncCallback<Integer> callback) {
				if (table == null)
					callback.onSuccess(0);
				else
					callback.onSuccess(table.getRowCount());
			}

			@Override
			protected void getServerData(int start, int pageSize, AsyncCallback<List<Row>> callback) {
				ArrayList<Row> rowsPage = new ArrayList<Row>();
				if (table != null) {
					int count = table.getRowCount();
					if (count > 0) {
						for (int i = 0; i < pageSize; i++) {
							if ((start + i) < count)
								rowsPage.add(table.getRow(start + i));
						}
					}
				}
				callback.onSuccess(rowsPage);
			}
		};
	}

	protected void setDataProvider(GridDataProvider provider) {
		if (this.provider != null)
			this.provider.removeDataDisplay(grid);
		this.provider = provider;
		this.provider.addDataDisplay(grid);
		this.provider.setGridId(id);
		this.provider.setEvents(this);
	}

	public Row getCurrentRow() {
		return selector.getSelectedObject();
	}

	private void prepareColumns() {
		int width = 0;
		for (int i = 0; i < table.getColumnCount(); i++) {
			if (table.getColumn(i).isVisible()) {
				createColumn(i, table.getColumn(i));
				width += table.getColumn(i).getWidth();
			}
		}
		grid.setMinimumTableWidth(width, Unit.PX);
	}
	
	private void createColumn(final int index, final ru.rbt.barsgl.gwt.core.datafields.Column column) {
		Column<Row, ?> col;
		switch (column.getType()) {
			case DECIMAL:
				col = new Column<Row, Number>(new NumberCell(NumberFormat.getFormat(column.getFormat()))){
					private int idx = index;
					@Override
					public BigDecimal getValue(Row object) {
						BigDecimal res = (BigDecimal) object.getField(idx).getValue();
						return res;
					}
				};
				col.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
				break;
			case INTEGER:
				col = new Column<Row, Number>(new NumberCell(NumberFormat.getFormat(column.getFormat()))){
					private int idx = index;
					@Override
					public Integer getValue(Row object) {
						Integer res = (Integer) object.getField(idx).getValue();
						return res;
					}
				};
				col.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
				break;
			case LONG:
				col = new Column<Row, Number>(new NumberCell(NumberFormat.getFormat(column.getFormat()))){
					private int idx = index;
					@Override
					public Long getValue(Row object) {
						Long res = (Long) object.getField(idx).getValue();
						return res;
					}
				};
				col.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
				break;
			case DATETIME:	// стандартное преобразование
				col = new Column<Row, Date>(new DateCell(DateTimeFormat.getFormat(column.getFormat()))){
					private int idx = index;
					@Override
					public Date getValue(Row object) {
						Date res = (Date) object.getField(idx).getValue();
						return res;
					}
				};
				col.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
				break;
			default:	// DATE попадает сюда, преобразование через FieldFormatter
				col = new Column<Row, String>(new TextCell()) {
					private int idx = index;
					private FieldFormatter formatter = new FieldFormatter(column);
					@Override
					public String getValue(Row object) {
						String res = formatter.getDisplayValue(object.getField(idx));
						if (cellValueEventHandler != null)
							res = cellValueEventHandler
									.getDisplayValue(table.getColumn(idx).getName(), object.getField(idx), res);
						return res;
					}
				};
				break;
		}

		TextHeader th = new TextHeader(table.getColumn(index).getCaption());
		col.setSortable(column.isSortable());
		col.setDataStoreName(column.getName());

		grid.addColumn(col, th);
		grid.setColumnWidth(col, table.getColumn(index).getWidth() + "px");
	}
	
	protected void setPageSize(int size) {
		pageSize = size > MAX_PAGE_SIZE ? MAX_PAGE_SIZE : size;
		pageSize = pageSize < MIN_PAGE_SIZE ? MIN_PAGE_SIZE : pageSize;
		grid.setPageSize(pageSize);
	}
	
	public int getPageSize() {
		return pageSize;
	}
	
	public void refresh(Table table) {
		this.table = table;
		refresh();
	}

	public void refresh() {
		//TODO
		//selector.clear(); -решает проблему застревания старых данных в текущей строке при пустом гриде, 
		// но создает проблему фильтрации, когда фильтр возвращает одну строку!!!
		provider.activate();
		if (grid.getKeyboardSelectedRow() != 0)
			doubleRefreshError = true;
		grid.setKeyboardSelectedRow(0); //avoid GWT bug (filter: select one row on page 0)
		if (pager.getPage() == 0){
			provider.onRangeChanged(grid);
		}
		else{
			pager.setPage(0);
		}
	}

	public ISortStrategy getSortStrategy(){
		return sortStrategy;
	}

	public void setSortStrategy(ISortStrategy sortStrategy){
		this.sortStrategy = sortStrategy;
	}

	public void setSortEvents(ISortEvents sortEvents){
		this.sortEvents = sortEvents;
	}

	public void setRowChangedEvent (IGridRowChanged rowChangedEvent){
		this.rowChangedEvent = rowChangedEvent;
	}

	public Table getTable(){
		return table;
	}

	public String getId(){
		return id;
	}

	public void resize(){
		grid.redraw();
	}

	private void fireRowChangedEvent(final Row row){
		if (rowChangedEvent != null) {
			if (row == null) {
				if (passNull) {
					rowChangedEvent.onRowChanged(row);
					passNull = false;
				} else {
					passNull = true;
				}
			} else {
				rowChangedEvent.onRowChanged(row);
				passNull = false;
			}
		}
	}

	@Override
	public void onDataLoad() {
		if (grid.getRowCount() > 0) {
			grid.setKeyboardSelectedRow(0);
		}
		LocalEventBus.fireEvent(new GridEvents(id, GridEvents.EventType.LOAD_DATA, grid.getRowCount()));
	}

	public List<FilterItem> getFilterCriteria() {
		return filterCriteria;
	}

	public void setFilterCriteria(List<FilterItem> filterCriteria) {
		this.filterCriteria = filterCriteria;
	}

	public List<SortItem> getSortCriteria() {
		return sortCriteria;
	}

	public void setSortCriteria(List<SortItem> sortCriteria) {
		this.sortCriteria = sortCriteria;
	}

	public void setInitialFilterCriteria(List<FilterItem> initialFilterCriteria) {
		this.initialFilterCriteria = initialFilterCriteria;
		this.isInitialFilterCriteria = true;
		this.filterCriteria = initialFilterCriteria;
	}

	public boolean isInitialFilterCriteria() {
		return isInitialFilterCriteria;
	}

	public void setInitialSortCriteria(List<SortItem> initialSortCriteria) {
		this.initialSortCriteria = initialSortCriteria;
		this.isInitialSortCriteria = true;
		this.sortCriteria = initialSortCriteria;
	}

	public boolean isInitialSortCriteria() {
		return isInitialSortCriteria;
	}

	public List<FilterItem> getInitialFilterCriteria() {
		return initialFilterCriteria;
	}

	public List<SortItem> getInitialSortCriteria() {
		return initialSortCriteria;
	}	
	
	public int getRowCount() {
		return grid.getRowCount();
	}

	public <U extends Serializable> U getFieldValue(String fieldName) {
		Columns columns = table.getColumns();
		Row row = getCurrentRow();
	    int ind = columns.getColumnIndexByName(fieldName);
	    if (ind >= 0)
	    	return (U) row.getField(ind).getValue();
	    else
	    	return null;
	}

	public String getFieldText(String fieldName) {
	    Object value = getFieldValue(fieldName);
	    if (null != value)
	    	return value.toString().trim();
	    else
	    	return null;
	}

	public List<Row> getVisibleItems(){
	    return grid.getVisibleItems();
	}

	public void rebuildGrid(){
		while (grid.getColumnCount() > 0){
			grid.removeColumn(0);
		}
		prepareColumns();
	}

	private void hideColumn(String columnName){
        ru.rbt.barsgl.gwt.core.datafields.Column col = table.getColumn(columnName);
        col.setVisible(false);
        col.setFilterable(false);
        col.setEditable(false);
    }

    private void showColumn(String columnName){
        ru.rbt.barsgl.gwt.core.datafields.Column col = table.getColumn(columnName);
        col.setVisible(true);
        col.setFilterable(true);
        col.setEditable(true);
    }

	public void hideColumns(String... columnNames){
		for (String columnName: columnNames){
            hideColumn(columnName);
		}
		rebuildGrid();
	}

	public void hideColumns(List<String> columnNames){
		for (String columnName: columnNames){
            hideColumn(columnName);
		}
		rebuildGrid();
	}

	public void showColumns(String... columnNames){
		for (String columnName: columnNames){
            showColumn(columnName);
		}
		rebuildGrid();
	}

	public void showColumns(List<String> columnNames){
		for (String columnName: columnNames){
            showColumn(columnName);
		}
		rebuildGrid();
	}
}
