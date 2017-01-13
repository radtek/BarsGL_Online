package ru.rbt.barsgl.gwt.client.dictionary;

import ru.rbt.barsgl.gwt.client.compLookup.LookupBoxBase;
import ru.rbt.barsgl.gwt.client.gridForm.GridForm;
import ru.rbt.barsgl.gwt.client.gridForm.GridFormDlgBase;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by ER18837 on 03.02.16.
 */
public class BranchLookupBox extends LookupBoxBase {
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
                        list.add(new SortItem("", Column.Sort.ASC));
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
        };

    }
}
