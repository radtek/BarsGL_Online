package ru.rbt.barsgl.gwt.client.pd;

import com.google.gwt.user.client.*;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.utils.DialogUtils;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.enums.ProcessingType;
import ru.rbt.barsgl.shared.filter.FilterCriteria;
import ru.rbt.barsgl.shared.operation.Reconc47422Wrapper;
import ru.rbt.grid.gwt.client.export.IExportData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by er18837 on 06.08.2018.
 */
public class PostingReconc47422ReportData implements IExportData {
    private Table table;
    private Column colContract;
    private Column colCurrency;
    private Column colFilial;
    private Column colState;
    private Column colDate;

    private List<FilterItem> filterItems;

    private final String sql =
            "SELECT NDOG, DEAL_ID, SUBDEALID, PMT_REF, PROCDATE, VALD, POD, CCY," +
                    " ACID_DR, BSAACID_DR, AMNT_DR, AMNTBC_DR," +
                    " ACID_CR, BSAACID_CR, AMNT_CR, AMNTBC_CR," +
                    " RNARLNG_DR, RNARLNG_CR, CBCC, ACID_TECH, BSAACID_TECH, PCID, OPERDAY, STATE, STATE_ORDER, " +
                    " CASE WHEN STATE LIKE 'PROC%' THEN '" + ProcessingType.PROCESSED.getValue() + "' ELSE '" + ProcessingType.UNPROCESSED.getValue() + "' END STATE_DESCR " +
                    " FROM V_GL_REP47422 " +
                    " ORDER BY NDOG, STATE_ORDER, POD, PCID";


    public PostingReconc47422ReportData(Reconc47422Wrapper wrapper) {
        this.table = prepareTable();
        filterItems = new ArrayList<>();
        if (!DialogUtils.isEmpty(wrapper.getContract())) {
            filterItems.add(new FilterItem(this.colContract, FilterCriteria.HAVE, wrapper.getContract()));
        }
        if (!DialogUtils.isEmpty(wrapper.getCurrency())) {
            filterItems.add(new FilterItem(this.colCurrency, FilterCriteria.EQ, wrapper.getCurrency()));
        }
        if (!DialogUtils.isEmpty(wrapper.getFilial())) {
            filterItems.add(new FilterItem(this.colFilial, FilterCriteria.EQ, wrapper.getFilial()));
        }
        if (!wrapper.getProcType().equals(ProcessingType.ALL)) {
            filterItems.add(new FilterItem(this.colState, FilterCriteria.EQ, wrapper.getProcType().getValue()));
        }
//        filterItems.add(new FilterItem(colDate, FilterCriteria.GE, wrapper.getContract()));
//        filterItems.add(new FilterItem(colDate, FilterCriteria.LE, wrapper.getContract()));
    }

    @Override
    public String sql() {
        return sql;
    }

    @Override
    public Columns columns() {
        return table.getColumns();
    }

    @Override
    public List<FilterItem> masterFilterItems() {
        return filterItems;
    }

    @Override
    public List<FilterItem> detailFilterItems() {
        return null;
    }

    @Override
    public List<SortItem> sortItems() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
//        list.add(new SortItem("NDOG", Column.Sort.ASC));
        list.add(new SortItem("STATE_ORDER", Column.Sort.ASC));
//        list.add(new SortItem("PCID", Column.Sort.ASC));
        return list;
    }

    private Table prepareTable() {
        Table result = new Table();

        result.addColumn(colContract = new Column("NDOG", Column.Type.STRING, "Номер договора", 10));
        result.addColumn(colState = new Column("STATE_DESCR", Column.Type.STRING, "Обработана", 60));
        result.addColumn(new Column("DEAL_ID", Column.Type.STRING, "ИД сделки", 100));
        result.addColumn(new Column("SUBDEALID", Column.Type.STRING, "ИД субсделки", 100));
        result.addColumn(new Column("PMT_REF", Column.Type.STRING, "ИД платежа", 100));
        result.addColumn(new Column("PROCDATE", Column.Type.DATE, "Дата опердня", 80));
        result.addColumn(new Column("VALD", Column.Type.DATE, "Дата валютирования", 80));
        result.addColumn(colDate = new Column("POD", Column.Type.DATE, "Дата проводки", 80));
        result.addColumn(colCurrency = new Column("CCY", Column.Type.STRING, "Валюта", 60));
        result.addColumn(new Column("ACID_DR", Column.Type.STRING, "Счет Midas ДБ", 160));
        result.addColumn(new Column("BSAACID_DR", Column.Type.STRING, "Счет ДБ", 160));
        result.addColumn(new Column("AMNT_DR", Column.Type.DECIMAL, "Сумма ДБ", 120));
        result.addColumn(new Column("AMNTBC_DR", Column.Type.DECIMAL, "Сумма в руб. ДБ", 140));
        result.addColumn(new Column("ACID_CR", Column.Type.STRING, "Счет Midas КР", 160));
        result.addColumn(new Column("BSAACID_CR", Column.Type.STRING, "Счет КР", 160));
        result.addColumn(new Column("AMNT_CR", Column.Type.DECIMAL, "Сумма КР", 120));
        result.addColumn(new Column("AMNTBC_CR", Column.Type.DECIMAL, "Сумма в руб. КР", 140));
        result.addColumn(new Column("RNARLNG_DR", Column.Type.STRING, "Описание проводки ДБ", 200));
        result.addColumn(new Column("RNARLNG_CR", Column.Type.STRING, "Описание проводки КР", 200));
        result.addColumn(colFilial = new Column("CBCC", Column.Type.STRING, "Филиал", 60));
        result.addColumn(new Column("ACID_TECH", Column.Type.STRING, "Техсчет Midas", 160));
        result.addColumn(new Column("BSAACID_TECH", Column.Type.STRING, "Техсчет ЦБ", 160));
        result.addColumn(new Column("PCID", Column.Type.LONG, "PCID", 100));
        result.addColumn(new Column("STATE", Column.Type.STRING, "Статус", 80));
        result.addColumn(new Column("STATE_ORDER", Column.Type.INTEGER, "Сортировка", 100, true, true));
        result.addColumn(new Column("OPERDAY", Column.Type.DATE, "Дата опердня обработки", 80));

        return result;
    }
}
