package ru.rbt.barsgl.gwt.client.pd;

import com.google.gwt.user.client.Window;
import ru.rbt.grid.gwt.client.export.IExportData;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.Utils;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by akichigi on 13.04.17.
 */
public class PostingBackValueReportData implements IExportData {
    private final String sql =
            "SELECT  BSAACID, CCY, AMNT, AMNTBC, SRC_PST, USER_NAME, PROCDATE, POD, CASE WHEN INVISIBLE = 0 then '' ELSE 'Y' END DELETED, PCID " +
                    "FROM V_GL_BVREPORT " +
                    "WHERE (PROCDATE BETWEEN TO_DATE('{0}', 'YYYY-MM-DD') AND TO_DATE('{0}', 'YYYY-MM-DD') + '{1}') AND POD < TO_DATE('{0}', 'YYYY-MM-DD')";

    private String date;
    private String limit;

    public PostingBackValueReportData(String date, String limit){
       this.date = date;
       this.limit = limit;
    }

    @Override
    public String sql() {
        return  Utils.Fmt(sql, date, limit);
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
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("PCID", Column.Sort.ASC));
        return list;
    }

    private Table table() {
        Table result = new Table();
        Column col;

        result.addColumn(new Column("BSAACID", Column.Type.STRING, "Счет", 100));
        result.addColumn(new Column("CCY", Column.Type.STRING, "Валюта", 100));
        result.addColumn(new Column("AMNT", Column.Type.DECIMAL, "Сумма в валюте счета", 100));
        result.addColumn(new Column("AMNTBC", Column.Type.DECIMAL, "Сумма в рублях", 100));
        result.addColumn(new Column("SRC_PST", Column.Type.STRING, "Источник", 100));
        result.addColumn(col = new Column("PROCDATE", Column.Type.DATE, "Дата создания", 100));
        col.setFormat("dd.MM.yyyy");
        result.addColumn(col = new Column("POD", Column.Type.DATE, "Дата проводки", 100));
        col.setFormat("dd.MM.yyyy");
        result.addColumn(new Column("DELETED", Column.Type.STRING, "Отменена", 100));
        result.addColumn(new Column("USER_NAME", Column.Type.STRING, "Логин 1-й руки", 100));
        result.addColumn(new Column("PCID", Column.Type.LONG, "ID связи (PCID)", 100));

        return result;
    }
}
