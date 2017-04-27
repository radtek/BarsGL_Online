package ru.rbt.barsgl.gwt.client.compLookup;

import ru.rbt.grid.gwt.client.gridForm.GridForm;
import ru.rbt.barsgl.gwt.client.gridForm.GridFormDlgBase;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by ER18837 on 29.11.16.
 */
public class LookUpBranch extends LookupBoxBase {

    public LookUpBranch(String width) {
        this();
        setWidth(width);
    }

    public LookUpBranch() {
        super();
        setMaxLength(3);
        setVisibleLength(3);
    }

    @Override
    protected GridFormDlgBase getDialog() {
        return new GridFormDlgBase("Выбор бранча") {
            @Override
            protected GridForm getGridForm() {
                return new GridForm("Выбор бранча Midas") {
                    @Override
                    protected String prepareSql() {
                        return "select A8BRCD, A8BRNM from IMBCBBRP";
                    }

                    @Override
                    public ArrayList<SortItem> getInitialSortCriteria() {
                        ArrayList<SortItem> list = new ArrayList<SortItem>();
                        list.add(new SortItem("A8BRCD", Column.Sort.ASC));
                        return list;
                    }

                    protected Table prepareTable() {
                        Table result = new Table();
                        Column col;

                        result.addColumn(new Column("A8BRCD", Column.Type.STRING, "Код", 70));
                        result.addColumn(new Column("A8BRNM", Column.Type.STRING, "Название", 350));

                        return result;
                    }
                };
            }

            @Override
            protected boolean setResultList(HashMap<String, Object> result) {
                setValue(result.get("A8BRCD").toString());
                return true;
            }

            @Override
            protected Object[] getInitialFilterParams() {
                return null;
            }

            @Override
            protected void onSetResult() {
                LookUpBranch.this.onSetResult();
            }
        };
    }
}
