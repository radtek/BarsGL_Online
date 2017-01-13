package ru.rbt.barsgl.gwt.client.dict;

import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;

/**
 * Created by akichigi on 19.09.16.
 */
public class AcodForm extends Acod {

    @Override
    protected void reconfigure() {
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();
        Column col;
        result.addColumn(col = new Column("ID", Column.Type.LONG, "ID", 80, false, false));
        col.setEditable(false);
        col.setFilterable(false);

        result.addColumn(new Column("ACOD", Column.Type.STRING, FIELD_ACOD, 55));
        result.addColumn(new Column("ACC2DSCR", Column.Type.STRING, FIELD_ACC2DSCR, 140));
        result.addColumn(new Column("TYPE", Column.Type.STRING, FIELD_TYPE, 35));
        result.addColumn(new Column("SQDSCR", Column.Type.STRING, FIELD_SQDSCR, 150));
        result.addColumn(new Column("ENAME", Column.Type.STRING, FIELD_ENAME, 150));
        result.addColumn(new Column("RNAME", Column.Type.STRING, FIELD_RNAME, 250));

        return result;
    }
}
