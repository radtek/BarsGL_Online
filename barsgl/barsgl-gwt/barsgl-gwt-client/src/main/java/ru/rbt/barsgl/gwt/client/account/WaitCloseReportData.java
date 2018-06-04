package ru.rbt.barsgl.gwt.client.account;

import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.grid.gwt.client.export.IExportData;

import java.util.List;

/**
 * Created by er22317 on 23.03.2018.
 */
public class WaitCloseReportData implements IExportData {
    String _begin, _end;
    Boolean _isAll;
    private final String sql ="select BSAACID, CCY, PKG_ACC.GET_BALANCE(ACID, BSAACID, CCY) BAL, DEALID, SUBDEALID, DEALSRC, "+
            "case when IS_ERRACC='0' then 'закрытие сделки' "+
            " when IS_ERRACC='1' then 'отмена сделки' "+
            " when IS_ERRACC='2' then 'изменение сделки' "+
            "else 'ХХХ' end IS_ERRACC, "+
            "OPERDAY, EXCLDATE " +
            "from GL_ACWAITCLOSE {0} order by OPERDAY";

    public WaitCloseReportData(String begin, String end, Boolean isAll){
        _begin = begin;
        _end = end;
        _isAll = isAll;
    }

    @Override
    public String sql() {
        return  Utils.Fmt(sql, _isAll? "": "where OPERDAY BETWEEN TO_DATE('"+_begin+"', 'YYYY-MM-DD') AND TO_DATE('"+_end+"', 'YYYY-MM-DD')");
    }

    @Override
    public Columns columns() {
        return table().getColumns();
    }

    @Override
    public List<FilterItem> masterFilterItems() {
        return null;
    }

    @Override
    public List<FilterItem> detailFilterItems() {
        return null;
    }

    @Override
    public List<SortItem> sortItems() {
        return null;
    }

    private Table table() {
        Table result = new Table();
        Column col;
        result.addColumn(new Column("BSAACID", Column.Type.STRING, "Счет", 100));
        result.addColumn(new Column("CCY", Column.Type.STRING, "Валюта", 100));
        result.addColumn(new Column("BAL", Column.Type.DECIMAL, "Остаток", 100));
        result.addColumn(new Column("DEALID", Column.Type.STRING, "Номер сделки ", 100));
        result.addColumn(new Column("SUBDEALID", Column.Type.STRING, "Номер субсделки", 100));
        result.addColumn(new Column("DEALSRC", Column.Type.STRING, "Источник сделки", 100));
        result.addColumn(new Column("IS_ERRACC", Column.Type.STRING, "Способ постановки", 100));
        result.addColumn(col = new Column("OPERDAY", Column.Type.DATE, "Дата ОД постановки", 100));
        col.setFormat("dd.MM.yyyy");
        result.addColumn(col = new Column("EXCLDATE", Column.Type.DATE, "Дата исключения", 100));
        col.setFormat("dd.MM.yyyy");

        return result;
    }
}
