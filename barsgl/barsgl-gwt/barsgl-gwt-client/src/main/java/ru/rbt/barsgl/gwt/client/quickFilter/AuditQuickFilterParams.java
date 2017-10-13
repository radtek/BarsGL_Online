package ru.rbt.barsgl.gwt.client.quickFilter;

import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.shared.filter.FilterCriteria;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;

import java.util.ArrayList;

/**
 * Created by ER18837 on 27.04.16.
 */
public class AuditQuickFilterParams extends DateQuickFilterParams {
	private static final String TIME_STR = " 00:00:00";
    private boolean onlyErrors;
    private Column colLogLevel;
    private Column colLogCode;

    public boolean isOnlyErroros() {
        return onlyErrors;
    }

    public void setOnlyErroros(boolean onlyErroros) {
        this.onlyErrors = onlyErroros;
    }

    public AuditQuickFilterParams(Column colCreateDate, Column colLogLevel, Column colLogCode) {
        super(colCreateDate, null, null, DateFilterField.CREATE_DATE);
        this.colLogLevel = colLogLevel;
        this.colLogCode = colLogCode;
        onlyErrors = true;
    }

    @Override
    public ArrayList<FilterItem> getFilter() {
        ArrayList<FilterItem> list = new ArrayList<>();
        list.add(new FilterItem(filterField.getColumn(), FilterCriteria.GE, dateBegin));
        list.add(new FilterItem(filterField.getColumn(), FilterCriteria.LE, dateEnd));
        if (onlyErrors) {
            list.add(new FilterItem(colLogLevel, FilterCriteria.NE, "Info"));
            list.add(new FilterItem(colLogLevel, FilterCriteria.NE, "Warning"));
            list.add(new FilterItem(colLogCode, FilterCriteria.NE, "Operday"));
            list.add(new FilterItem(colLogCode, FilterCriteria.NE, "OpenOperday"));
            list.add(new FilterItem(colLogCode, FilterCriteria.NE, "RecalcWTAC"));
        }
        return list;
    }

}
