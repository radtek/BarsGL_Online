package ru.rbt.barsgl.gwt.client.Export;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Image;
import ru.rbt.security.gwt.client.CommonEntryPoint;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.dialogs.FilterUtils;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.utils.DialogUtils;
import ru.rbt.barsgl.gwt.core.utils.UUID;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.Utils;

import java.util.List;

/**
 * Created by akichigi on 28.07.16.
 */
public class Export2ExcelAction extends GridAction {
    private GridWidget grid;
    private String sql;
    protected int exportMaxRows = 4999;
    private List<FilterItem> linkDetailFilterCriteria = null;

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
        DialogUtils.showInfo("Начало выгрузки таблицы в Excel. Ожидайте...");
        List<SortItem> sortItems = grid.getSortCriteria();
        List<FilterItem> filterItems = FilterUtils.combineFilterCriteria(grid.getFilterCriteria(), linkDetailFilterCriteria);

        CommonEntryPoint.asyncGridService.export2Excel(sql, grid.getTable().getColumns(), filterItems, sortItems,
                new ExportActionCallback(this, UUID.randomUUID().replace("-", "")));
    }

    class ExportActionCallback implements AsyncCallback<String> {

        private final String localFileName;
        private GridAction rootAction;

        public ExportActionCallback(GridAction rootAction, String fileName) {
            this.rootAction = rootAction;
            this.localFileName = fileName;
        }

        @Override
        public void onFailure(Throwable throwable) {
            DialogUtils.showInfo("Ошибка", throwable.getMessage());
            rootAction.setEnable(true);
        }

        @Override
        public void onSuccess(String fileName) {
            String url = GWT.getHostPageBaseURL()
                    + "service/ExportFileHandler?filename=" + fileName + "&newfilename=" + localFileName + ".xlsx";
            Window.open(url, "_self", "disabled");

            rootAction.setEnable(true);
        }
    }
}
