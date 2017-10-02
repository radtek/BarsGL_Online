package ru.rbt.barsgl.gwt.client.gridForm;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.core.actions.Action;
import ru.rbt.barsgl.gwt.core.actions.FilterAction;
import ru.rbt.barsgl.gwt.core.actions.RefreshAction;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.dialogs.FilterUtils;
import ru.rbt.barsgl.gwt.core.forms.BaseForm;
import ru.rbt.barsgl.gwt.core.forms.IDisposable;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.statusbar.StatusBarManager;
import ru.rbt.barsgl.gwt.core.widgets.*;
import ru.rbt.grid.gwt.client.GridEntryPoint;
import ru.rbt.grid.gwt.client.export.Export2ExcelAction;
import ru.rbt.shared.enums.SecurityActionCode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by akichigi on 21.05.15.
 */
public abstract class MDForm extends BaseForm implements IDisposable, ILinkFilterCriteria {
    protected GridWidget masterGrid;
    protected Table masterTable;
    private String masterSql;
    private FilterAction  masterFilterAction;
    protected ActionBarWidget masterActionBar;
    protected RefreshAction masterRefreshAction;
    private Export2ExcelAction masterExport2Excel;
    private Export2ExcelAction detailExport2Excel;


    protected GridWidget detailGrid;
    protected Table detailTable;
    private String detailSql;
    private FilterAction  detailFilterAction;
    protected ActionBarWidget detailActionBar;
    protected RefreshAction detailRefreshAction;

    private List<FilterItem> detailLinkFilterCriteria = null;

    protected MDWidget mdWidget;

    public MDForm(String formTitle, String masterTitle, String detailTitle){
        this(formTitle, masterTitle, detailTitle, false);
    }

    public MDForm(String formTitle, String masterTitle, String detailTitle, boolean delayLoad){
        super();
        super.title.setText(formTitle);

        super.setContent(makeContent(masterTitle, detailTitle, delayLoad));
    }

    protected ArrayList<SortItem> getInitialMasterSortCriteria(){
        return null;
    }
    protected ArrayList<SortItem> getInitialDetailSortCriteria(){
        return null;
    };

    public Widget makeContent(String masterTitle, String detailTitle, boolean delayLoad) {
        masterTable = prepareMasterTable();
        masterSql = prepareMasterSql();

        //System.out.println(masterSql);
        detailTable = prepareDetailTable();
        detailSql = prepareDetailSql();

        masterGrid = createMasterGrid(delayLoad);
//        masterGrid.setSortStrategy(null); //отключена сортировка
        masterGrid.setSortEvents(masterRefreshAction = createMasterRefreshAction());
        detailGrid = createDetailGrid();
//        detailGrid.setSortStrategy(null); //отключена сортировка

        detailGrid.setSortEvents(detailRefreshAction = createDetailRefreshAction());

        masterActionBar = new ActionBarWidget();
        detailActionBar = new ActionBarWidget();

        masterActionBar.addAction(masterRefreshAction);
        masterActionBar.addAction(masterFilterAction = createMasterFilterAction());
        masterActionBar.addSecureAction(masterExport2Excel = new Export2ExcelAction(masterGrid, masterSql), SecurityActionCode.OperToExcel);
        masterExport2Excel.setFormTitle(title.getText());

        detailActionBar.addAction(detailRefreshAction);
        detailActionBar.addAction(detailFilterAction = createDetailFilterAction());
        detailActionBar.addSecureAction(detailExport2Excel = new Export2ExcelAction(detailGrid, detailSql), SecurityActionCode.OperToExcel);
        detailExport2Excel.setFormTitle(title.getText());

        mdWidget = new MDWidget(masterGrid, masterActionBar, masterTitle,
                                detailGrid, detailActionBar, detailTitle);

        mdWidget.setLinkFilterCriteria(this);

        return mdWidget;
    }


    private GridWidget createMasterGrid(boolean delayLoad){
        return new GridWidget(masterTable, new GridDataProvider(delayLoad) {
            @Override
            protected void getServerCount(AsyncCallback<Integer> callback) {
                GridEntryPoint.asyncGridService.getAsyncCount(masterSql, (null == masterGrid ? null : masterGrid.getFilterCriteria()), callback);
            }
            @Override
            protected void getServerData(int start, int pageSize, AsyncCallback<List<Row>> callback) {
                List<SortItem> sortItems = getMasterSortCriteria();
                List<FilterItem> filterItems = (null == masterGrid) ? null : masterGrid.getFilterCriteria();
                refreshMasterParams(filterItems, sortItems);
                GridEntryPoint.asyncGridService.getAsyncRows(masterSql, masterTable.getColumns(), start, pageSize,
                        filterItems, sortItems, callback);
            }
        }, 30);
    }

    private GridWidget createDetailGrid(){
        return new GridWidget(detailTable, new GridDataProvider(true) {
            @Override
            protected void getServerCount(AsyncCallback<Integer> callback) {
//                setDelayLoad(false);
                GridEntryPoint.asyncGridService.getAsyncCount(detailSql,
                        FilterUtils.combineFilterCriteria((null == detailGrid ? null : detailGrid.getFilterCriteria()), detailLinkFilterCriteria), callback);
            }
            @Override
            protected void getServerData(int start, int pageSize, AsyncCallback<List<Row>> callback) {
                List<SortItem> sortItems = getDetailSortCriteria();
                List<FilterItem> filterItems = (null == detailGrid) ? null : detailGrid.getFilterCriteria();
                refreshDetailParams(filterItems, sortItems, detailLinkFilterCriteria);
                GridEntryPoint.asyncGridService.getAsyncRows(detailSql, detailTable.getColumns(), start, pageSize,
                        FilterUtils.combineFilterCriteria(filterItems, detailLinkFilterCriteria), sortItems, callback);
            }
        }, 30);
    }

    private List<FilterItem> getLinkDetailFilterCriteria(){
        return detailLinkFilterCriteria;
    }

    private List<SortItem> getMasterSortCriteria() {
        if (null == masterGrid) // это первый запуск, еще до создания grid
            return getInitialMasterSortCriteria();
        List<SortItem> sortCriteria = masterGrid.getSortCriteria();
        if (null == sortCriteria && !masterGrid.isInitialSortCriteria()) {
            // это первый запуск после создания grid
            masterGrid.setInitialSortCriteria(sortCriteria = getInitialMasterSortCriteria());
        }
        return sortCriteria;
    }

    private List<SortItem> getDetailSortCriteria() {
        if (null == detailGrid) // это первый запуск, еще до создания grid
            return getInitialDetailSortCriteria();
        List<SortItem> sortCriteria = detailGrid.getSortCriteria();
        if (null == sortCriteria && !detailGrid.isInitialSortCriteria()) {
            // это первый запуск после создания grid
            detailGrid.setInitialSortCriteria(sortCriteria = getInitialDetailSortCriteria());
        }
        return sortCriteria;
    }

    private void refreshMasterParams(List<FilterItem> filterCriteria, List<SortItem> sortCriteria) {
        masterGrid.setFilterCriteria(filterCriteria);
        masterRefreshAction.setFilterCriteria(filterCriteria);
        masterGrid.setSortCriteria(sortCriteria);
        masterRefreshAction.setSortCriteria(sortCriteria);
        changeFilterStatus(masterFilterAction, (filterCriteria != null) && (!filterCriteria.isEmpty()));
    }

    private void refreshDetailParams(List<FilterItem> filterCriteria, List<SortItem> sortCriteria, List<FilterItem> linkMDFilterCriteria) {
        detailGrid.setFilterCriteria(filterCriteria);
        detailRefreshAction.setFilterCriteria(filterCriteria);
        detailGrid.setSortCriteria(sortCriteria);
        detailRefreshAction.setSortCriteria(sortCriteria);
        detailLinkFilterCriteria = linkMDFilterCriteria;
        changeFilterStatus(detailFilterAction, (filterCriteria != null) && (!filterCriteria.isEmpty()));
        detailExport2Excel.setLinkDetailFilterCriteria(linkMDFilterCriteria);
    }

    private RefreshAction createMasterRefreshAction(){
        return new RefreshAction(masterGrid) {
            @Override
            public void onRefresh(List<FilterItem> filterCriteria, List<SortItem> sortCriteria,
                                  List<FilterItem> linkMDFilterCriteria) {
                refreshMasterParams(filterCriteria, sortCriteria);
                masterGrid.refresh();
            }
        };
    }

    private RefreshAction createDetailRefreshAction(){
        return new RefreshAction(detailGrid) {
            @Override
            public void onRefresh(List<FilterItem> filterCriteria, List<SortItem> sortCriteria,
                                  List<FilterItem> linkMDFilterCriteria) {
                refreshDetailParams(filterCriteria, sortCriteria, linkMDFilterCriteria);
                detailGrid.refresh();
            }
        };
    }

    private void changeFilterStatus(Action action, boolean on){
        action.setImage(on ? new Image(ImageConstants.INSTANCE.filterOn()) : new Image(ImageConstants.INSTANCE.filter()));
        action.setHint(on ? "Фильтр: Вкл." : "Фильтр: Откл.");
    }

    private FilterAction createMasterFilterAction(){
        return new FilterAction(masterGrid);
    }

    private FilterAction createDetailFilterAction(){
        return new FilterAction(detailGrid);
    }

	public Serializable getValue(String colName){
		Columns columns = masterGrid.getTable().getColumns();
		return masterGrid.getCurrentRow().getField(columns.getColumnIndexByName(colName)).getValue();
	}

    protected abstract Table prepareMasterTable();
    protected abstract String prepareMasterSql();

    protected abstract Table prepareDetailTable();
    protected abstract String prepareDetailSql();

    @Override
    public void dispose() {
        StatusBarManager.ChangeStatusBarText("", StatusBarManager.MessageReason.MSG);
        masterActionBar.dispose();
        detailActionBar.dispose();
    }

    public void setLazyDetailRefresh(boolean lazy){
        mdWidget.setLazyRefresh(lazy);
    }

    public GridWidget getMasterGrid() {
        return masterGrid;
    }

    public GridWidget getDetailGrid() {
        return detailGrid;
    }
}
