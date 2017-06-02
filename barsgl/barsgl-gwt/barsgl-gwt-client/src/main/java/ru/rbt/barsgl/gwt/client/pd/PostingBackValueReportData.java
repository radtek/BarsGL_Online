package ru.rbt.barsgl.gwt.client.pd;

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
                    "FROM (" +

                    "SELECT P.PCID, P.BSAACID, CAST(DECIMAL(P.AMNT)/INTEGER(POWER(10,C.NBDP)) AS DECIMAL(22,2)) AMNT, " +
                    "CAST(DECIMAL(P.AMNTBC) * 0.01 AS DECIMAL(22,2)) AMNTBC, P.CCY," +
                    "O.SRC_PST, O.PROCDATE, P.POD, P.INVISIBLE, B.USER_NAME " +
                    "FROM GL_OPER O " +
                    "JOIN GL_POSTING PP ON PP.GLO_REF=O.GLOID " +
                    "JOIN PD P ON P.PCID=PP.PCID " +
                    "JOIN PDEXT5 P5 ON P5.ID=P.ID " +
                    "LEFT JOIN GL_BATPST B ON B.ID=O.PST_REF " +
                    "JOIN CURRENCY C ON C.GLCCY = P.CCY " +
                    "WHERE (O.PROCDATE BETWEEN DATE('{0}') AND DATE('{0}') + {1} DAYS) AND P.POD < '{0}' " +
                    "UNION ALL " +
                    "SELECT P.PCID, P.BSAACID, CAST(DECIMAL(P.AMNT)/INTEGER(POWER(10,C.NBDP)) AS DECIMAL(22,2)) AMNT, " +
                    "CAST(DECIMAL(P.AMNTBC) * 0.01 AS DECIMAL(22,2)) AMNTBC, P.CCY," +
                    "O.SRC_PST, O.PROCDATE, P.POD, P.INVISIBLE, B.USER_NAME " +
                    "FROM GL_OPER O " +
                    "JOIN GL_PD P ON P.GLO_REF=O.GLOID " +
                    "LEFT JOIN GL_BATPST B ON B.ID=O.PST_REF " +
                    "JOIN CURRENCY C ON C.GLCCY=P.CCY " +
                    "WHERE (O.PROCDATE BETWEEN DATE('{0}') AND DATE('{0}') + {1} DAYS) AND P.POD < '{0}'" +
                    ") v ";


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
