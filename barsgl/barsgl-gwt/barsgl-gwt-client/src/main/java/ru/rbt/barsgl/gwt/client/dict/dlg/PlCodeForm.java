package ru.rbt.barsgl.gwt.client.dict.dlg;

import ru.rbt.grid.gwt.client.gridForm.GridForm;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;

import java.util.ArrayList;

/**
 * Created by akichigi on 03.10.16.
 */
public class PlCodeForm extends GridForm {
    public PlCodeForm() {
        super("Символы ОФР");
        reconfigure();
    }

    private void reconfigure() {
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();
        Column column;
        result.addColumn(column = new Column("ID", Column.Type.LONG, "ID", 10, false, false));
        column.setFilterable(false);
        result.addColumn(new Column("PLCODE", Column.Type.STRING, "Символ", 60));
        result.addColumn(new Column("NAMES", Column.Type.STRING, "Наименование", 120));
        result.addColumn(new Column("NAME3", Column.Type.STRING, "Подраздел", 100));
        result.addColumn(new Column("NAME2", Column.Type.STRING, "Раздел", 110));
        result.addColumn(new Column("NAME1", Column.Type.STRING, "Часть", 150));
        result.addColumn(new Column("NAMEL", Column.Type.STRING, "Описание", 400));
        result.addColumn(new Column("NOTCUR", Column.Type.STRING, "Запрет валюты", 20, false, false));
        result.addColumn(new Column("DAT", Column.Type.DATE, "Дата начала", 85));
        result.addColumn(new Column("DATTO", Column.Type.DATE, "Дата конца", 85));

        return result;
    }

    @Override
    protected String prepareSql() {
        return  "select id, plcode, notcur, dat, datto, name1, name2, name3, names, namel from gl_plcode";
                //" where dat <= (select curdate from gl_OD) and value(datto, date('2029-01-01')) >= (select curdate from gl_OD)";
    }

    @Override
    public ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("plcode", Column.Sort.ASC));
        return list;
    }
}
