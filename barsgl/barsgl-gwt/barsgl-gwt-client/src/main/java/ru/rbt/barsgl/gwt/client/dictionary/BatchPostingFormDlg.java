package ru.rbt.barsgl.gwt.client.dictionary;

import ru.rbt.barsgl.gwt.client.gridForm.GridFormDlgBase;
import ru.rbt.barsgl.gwt.client.events.ae.BatchPostingErrorForm;
import ru.rbt.grid.gwt.client.gridForm.GridForm;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.dialogs.FilterCriteria;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;

import java.util.ArrayList;

/**
 * Created by ER18837 on 04.03.16.
 */
public abstract class BatchPostingFormDlg extends GridFormDlgBase {

    private final static String FORM_NAME_ERROR = "Строки с ошибкой, загруженные из файла";
    private final static String FORM_NAME = "Строки загруженные из файла";
    private boolean _error;

    public BatchPostingFormDlg(boolean error) {
        super(error ? FORM_NAME_ERROR : FORM_NAME);
        _error = error;
        ok.setVisible(false);
    }

    protected String getGridWidth() {
        return "1000px";
    }
    protected String getGridHeight() {
        return "400px";
    }

    @Override
    protected GridForm getGridForm() {
        return new  BatchPostingErrorForm() {
            @Override
            public ArrayList<SortItem> getInitialSortCriteria() {
                ArrayList<SortItem> list = new ArrayList<SortItem>();
                list.add(new SortItem(colPackage.getName(), Column.Sort.DESC));
                list.add(new SortItem(colRow.getName(), Column.Sort.ASC));

                return list;
            }

            @Override
            public ArrayList<FilterItem> getInitialFilterCriteria(Object[] initialFilterParams) {
                Long idPackage = (Long)initialFilterParams[0];

                ArrayList<FilterItem> list = new ArrayList<FilterItem>();
                list.add(new FilterItem(colPackage, FilterCriteria.EQ, idPackage, true));
                if (_error) {
                    list.add(new FilterItem(colError, FilterCriteria.EQ, 1, false));
                }

                return list;
            }

            @Override
            public Object[] getInitialFilterParams() {
                return BatchPostingFormDlg.this.getInitialFilterParams();
            }
        };
    }
}
