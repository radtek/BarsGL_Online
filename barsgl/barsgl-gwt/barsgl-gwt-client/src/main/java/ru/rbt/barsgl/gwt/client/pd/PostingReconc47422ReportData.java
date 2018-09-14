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

import static ru.rbt.barsgl.gwt.client.security.AuthWherePart.getFilialPart;

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
    private String whereClause = "";

    private final String sql =
            "SELECT NDOG, PROCESSED, DEAL_ID, SUBDEALID, PMT_REF, PROCDATE, VALD, POD, CCY, CBCC, " +
                    " ACID_DR, BSAACID_DR, AMNT_DR, AMNTBC_DR," +
                    " ACID_CR, BSAACID_CR, AMNT_CR, AMNTBC_CR," +
                    " PBR_DR, RNARLNG_DR, PBR_CR, RNARLNG_CR, ACID_TECH, BSAACID_TECH," +
                    " INVISIBLE_DC, PCID, ID_REF, LWD, OPERDAY, STATE, STATE_ORDER" +
                    " FROM V_GL_REP47422" +
                    " WHERE 1=1 " + whereClause
                    + getFilialPart("AND", "CBCC");

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
        filterItems.add(new FilterItem(colDate, FilterCriteria.GE, wrapper.getDateFrom()));
        filterItems.add(new FilterItem(colDate, FilterCriteria.LE, wrapper.getDateTo()));

        if(wrapper.isRegister()) {
            whereClause = " and (LWD = 'Y' or PROCCESSED = 'N') ";
        }
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
        List<SortItem> items = new ArrayList<>();
        items.add(new SortItem("STATE_ORDER", Column.Sort.ASC));
        items.add(new SortItem("NDOG", Column.Sort.ASC));
        items.add(new SortItem("ID_REF", Column.Sort.ASC));
        items.add(new SortItem("POD", Column.Sort.ASC));
        items.add(new SortItem("PCID", Column.Sort.ASC));
        return items;
    }

    private Table prepareTable() {
        Table result = new Table();

        result.addColumn(colContract = new Column("NDOG", Column.Type.STRING, "Номер договора", 10));
        result.addColumn(colState = new Column("PROCESSED", Column.Type.STRING, "Обработана", 60));
        result.addColumn(new Column("DEAL_ID", Column.Type.STRING, "ИД сделки", 100));
        result.addColumn(new Column("SUBDEALID", Column.Type.STRING, "ИД субсделки", 100));
        result.addColumn(new Column("PMT_REF", Column.Type.STRING, "ИД платежа", 100));
        result.addColumn(new Column("PROCDATE", Column.Type.DATE, "Дата опердня", 80));
        result.addColumn(new Column("VALD", Column.Type.DATE, "Дата валютирования", 80));
        result.addColumn(colDate = new Column("POD", Column.Type.DATE, "Дата проводки", 80));
        result.addColumn(colCurrency = new Column("CCY", Column.Type.STRING, "Валюта", 60));
        result.addColumn(colFilial = new Column("CBCC", Column.Type.STRING, "Филиал", 60));
        result.addColumn(new Column("ACID_DR", Column.Type.STRING, "Счет Midas ДБ", 160));
        result.addColumn(new Column("BSAACID_DR", Column.Type.STRING, "Счет ДБ", 160));
        result.addColumn(new Column("AMNT_DR", Column.Type.DECIMAL, "Сумма ДБ", 120));
        result.addColumn(new Column("AMNTBC_DR", Column.Type.DECIMAL, "Сумма в руб. ДБ", 140));
        result.addColumn(new Column("ACID_CR", Column.Type.STRING, "Счет Midas КР", 160));
        result.addColumn(new Column("BSAACID_CR", Column.Type.STRING, "Счет КР", 160));
        result.addColumn(new Column("AMNT_CR", Column.Type.DECIMAL, "Сумма КР", 120));
        result.addColumn(new Column("AMNTBC_CR", Column.Type.DECIMAL, "Сумма в руб. КР", 140));
        result.addColumn(new Column("PBR_DR", Column.Type.STRING, "Источник ДБ", 80));
        result.addColumn(new Column("RNARLNG_DR", Column.Type.STRING, "Описание проводки ДБ", 200));
        result.addColumn(new Column("PBR_CR", Column.Type.STRING, "Источник КР", 80));
        result.addColumn(new Column("RNARLNG_CR", Column.Type.STRING, "Описание проводки КР", 200));
        result.addColumn(new Column("ACID_TECH", Column.Type.STRING, "Техсчет Midas", 160));
        result.addColumn(new Column("BSAACID_TECH", Column.Type.STRING, "Техсчет ЦБ", 160));
        result.addColumn(new Column("INVISIBLE_DC", Column.Type.STRING, "Отменена ДБ,КР", 60));
        result.addColumn(new Column("PCID", Column.Type.LONG, "PCID", 100));
        result.addColumn(new Column("ID_REF", Column.Type.LONG, "ID регистра", 80));
        result.addColumn(new Column("STATE_ORDER", Column.Type.INTEGER, "Сортировка", 60, false, true));
//        result.addColumn(new Column("LWD", Column.Type.STRING, "Опер.регистр", 60, true, true));
        result.addColumn(new Column("OPERDAY", Column.Type.DATE, "Дата опердня обработки", 80));
        result.addColumn(new Column("STATE", Column.Type.STRING, "Статус", 80));

        return result;
    }
}
