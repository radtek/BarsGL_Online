package ru.rbt.barsgl.gwt.client.operBackValue.dict;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.IGridRowChanged;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.grid.gwt.client.gridForm.GridForm;
import ru.rbt.security.gwt.client.CommonEntryPoint;
import ru.rbt.shared.enums.SecurityActionCode;

import java.util.ArrayList;
import java.util.Date;

import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.DATE;

/**
 * Created by er17503 on 15.08.2017.
 */
public class BVPeriodClosingForm extends GridForm {
    public static final String FORM_NAME = "Закрытие отчетных периодов";
    private GridAction _editAction;
    private GridAction _deleteAction;

    public BVPeriodClosingForm(){
        super(FORM_NAME);
        reconfigure();
        setRowChangeEventHandler();
    }

    private void reconfigure(){
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        abw.addSecureAction(_editAction = editAction(), SecurityActionCode.ReferBackValue);
        abw.addSecureAction(createAction(), SecurityActionCode.ReferBackValue);
        abw.addSecureAction(_deleteAction = deleteAction(), SecurityActionCode.ReferBackValue);
    }


    private GridAction createAction() {
        return new GridAction(grid, null, "Создать отчетный период", new Image(ImageConstants.INSTANCE.new24()), 10) {
            @Override
            public void execute() {

            }
        };
    }

    private GridAction editAction() {
        return new GridAction(grid, null, "Изменить отчетный период", new Image(ImageConstants.INSTANCE.edit24()), 10, true) {
            @Override
            public void execute() {

            }
        };
    }

    private GridAction deleteAction() {
        return new GridAction(grid, null, "Удалить отчетный период", new Image(ImageConstants.INSTANCE.stop()), 10, true) {
            @Override
            public void execute() {

            }
        };
    }

    @Override
    protected Table prepareTable() {
        Table table = new Table();
        Column col;

        table.addColumn(col = new Column("PRD_LDATE", DATE, "Дата завершения отчетного периода", 80));
        col.setFormat("dd.MM.yyyy");
        table.addColumn(col = new Column("PRD_CUTDATE", DATE, "Дата закрытия отчетного периода", 80));
        col.setFormat("dd.MM.yyyy");

        return table;
    }

    @Override
    protected String prepareSql() {
        return "select PRD_LDATE, PRD_CUTDATE from GL_CRPRD";
    }

    @Override
    protected ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("PRD_LDATE", Column.Sort.DESC));
        return list;
    }

    private void setRowChangeEventHandler() {
        grid.setRowChangedEvent(new IGridRowChanged() {
            @Override
            public void onRowChanged(Row row) {
                boolean flag;
                if (grid.getCurrentRow() == null) return;
                if (grid.getRowCount() == 0){
                   flag = true;
                }else{
                   flag = ((Date)getFieldByName("PRD_CUTDATE").getValue()).compareTo(CommonEntryPoint.CURRENT_OPER_DAY) == -1;
                }

                _editAction.setEnable(!flag);
                _deleteAction.setEnable(!flag);
            }
        });
    }

}
