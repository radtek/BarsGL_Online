package ru.rbt.barsgl.gwt.client.dict;

import ru.rbt.barsgl.gwt.client.gridForm.GridForm;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.dialogs.FilterCriteria;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;

import java.util.ArrayList;

import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;

/**
 * Created by akichigi on 29.08.16.
 */
public class BssForm extends GridForm {

    private Column colPl;

    public BssForm() {
        super("Балансовый счет 2-го порядка");
        reconfigure();
    }

    private void reconfigure() {
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();

        result.addColumn(new Column("ACC2", Column.Type.STRING, "Счет", 24));
        result.addColumn(new Column("PSAV", Column.Type.STRING, "П/А", 20));
        result.addColumn(new Column("ACCNAME", Column.Type.STRING, "Наименование", 200));
        result.addColumn(colPl = new Column("PL", Column.Type.STRING, "Счет ОФР", 20, false, false));
        return result;
    }

    @Override
    protected String prepareSql() {
        return "select * from (select acc2, psav, acc1nam || ' ' || acc2nam as accname, " +
                " case when (ACC1 = '706' or ACC1 = '707') then 'Y' else 'N' end as PL" +
                " from BSS) v";
    }

    @Override
    public ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("acc2", Column.Sort.ASC));
        return list;
    }

    @Override
    protected ArrayList<FilterItem> getInitialFilterCriteria(Object[] initialFilterParams) {
    	if (initialFilterParams == null || isEmpty((String)initialFilterParams[0]))
    		return null;
        ArrayList<FilterItem> list = new ArrayList<FilterItem>();
        String pl = (String)initialFilterParams[0];
        list.add(new FilterItem(colPl, FilterCriteria.EQ, pl, true));
        return list;
    }
}
