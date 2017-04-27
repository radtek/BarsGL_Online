package ru.rbt.grid.gwt.client.export;

import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.utils.DialogUtils;
import ru.rbt.barsgl.gwt.core.utils.UUID;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.Export.ExcelExportHead;
import ru.rbt.barsgl.shared.Utils;

import java.util.List;

/**
 * Created by akichigi on 28.07.16.
 */
public class Export2ExcelAction extends GridAction implements IExportData {
    private GridWidget grid;
    private String sql;
    protected int exportMaxRows = 4999;
    private List<FilterItem> linkDetailFilterCriteria = null;
    private String formTitle;

    public Export2ExcelAction(GridWidget grid, String sql) {
        super(grid, "", "Экспорт в Excel", new Image(ImageConstants.INSTANCE.excel()), 10, true);
        this.grid = grid;
        this.sql = sql;
    }

    public void setLinkDetailFilterCriteria(List<FilterItem> criteria){
        linkDetailFilterCriteria = criteria;
    }

    @Override
    public void execute() {
        Row row = grid.getCurrentRow();
        if (row == null) return;

        int count = grid.getRowCount();
        //String msg = "Количество строк для выгрузки ({0}) превышает допустимое значение ({1})\nи может занять неопределенное количество времени и ресурсов!\n Продолжить выгрузку?";
        String msg = "Количество строк для выгрузки  превышает допустимое значение ({0})!";
        if (count > exportMaxRows){
          /*  DialogUtils.showConfirm(Utils.Fmt(msg, count, exportMaxRows), new IDlgEvents() {
                @Override
                public void onDlgOkClick(Object prms) throws Exception {
                    beginExportToExcel();
                }
            }, null);*/
            DialogUtils.showInfo(Utils.Fmt(msg, exportMaxRows));
        } else{
            beginExportToExcel();
        }
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    private void beginExportToExcel(){
        setEnable(false);
        ExcelExportHead head = new Export2ExcelHead(formTitle, grid.getFilterCriteria()).createExportHead();
        Export2Excel e2e = new Export2Excel(this, head, new ExportActionCallback(this, UUID.randomUUID().replace("-", "")));
        e2e.export();
    }

    public void setFormTitle(String formTitle) {
        this.formTitle = formTitle;
    }

    @Override
    public String sql() {
        return sql;
    }

    @Override
    public Columns columns() {
        return grid.getTable().getColumns();
    }

    @Override
    public List<FilterItem> masterFilterItems() {
        return grid.getFilterCriteria();
    }

    @Override
    public List<FilterItem> detailFilterItems() {
        return linkDetailFilterCriteria;
    }

    @Override
    public List<SortItem> sortItems() {
        return  grid.getSortCriteria();
    }
}
