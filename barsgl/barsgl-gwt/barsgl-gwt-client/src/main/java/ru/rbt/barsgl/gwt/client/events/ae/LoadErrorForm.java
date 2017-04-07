package ru.rbt.barsgl.gwt.client.events.ae;

import ru.rbt.barsgl.gwt.client.quickFilter.DateQuickFilterAction;
import ru.rbt.barsgl.gwt.client.gridForm.GridForm;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;

import java.util.ArrayList;

import static ru.rbt.barsgl.gwt.client.quickFilter.DateQuickFilterParams.DateFilterField.CREATE_DATE;
import static ru.rbt.barsgl.gwt.client.security.AuthWherePart.getSourcePart;

/**
 * Created by ER18837 on 25.03.16.
 */
public class LoadErrorForm extends GridForm {
    public static final String FORM_NAME = "Ошибки обработки операций";
    protected Column colProcDate;
    protected Column colPostDate;
    protected Column colValueDate;

    public LoadErrorForm() {
        super(FORM_NAME, true);
        reconfigure();
    }

    private void reconfigure() {
        GridAction quickFilterAction;
        abw.addAction(quickFilterAction = new DateQuickFilterAction(grid, colProcDate, colValueDate, colPostDate, CREATE_DATE, true));
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        quickFilterAction.execute();
    }

    @Override
    protected String prepareSql() {
        return "select * from V_GL_LOADERR " + getSourcePart("where", "SRC_PST");
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();

        result.addColumn(new Column("ID_PKG", Column.Type.LONG, "ID пакета", 60, false, false));
        result.addColumn(new Column("CR_DT", Column.Type.DATETIME, "Дата время обработки", 130));
        result.addColumn(colProcDate = new Column("DT_FILTER", Column.Type.DATE, "Дата обработки", 80, false, false));
        result.addColumn(new Column("CURDATE", Column.Type.DATE, "Опер.день обработки", 80, false, false));
        result.addColumn(new Column("LWD_STATUS", Column.Type.STRING, "Баланс пред.дня", 80, false, false));

        result.addColumn(new Column("PST_REF", Column.Type.LONG, "ID сообщения АЕ", 70, false, false));
        result.addColumn(new Column("GLOID", Column.Type.LONG, "ID операции", 70, false, false));

        result.addColumn(new Column("INP_METHOD", Column.Type.STRING, "Способ ввода", 40));
        result.addColumn(new Column("STATE", Column.Type.STRING, "Статус", 70));
        result.addColumn(new Column("ID_PST", Column.Type.STRING, "ИД сообщ АЕ", 80));
        result.addColumn(new Column("EVTP", Column.Type.STRING, "Тип события", 80, false, false));

        result.addColumn(new Column("SRC_PST", Column.Type.STRING, "Источник сделки", 60));
        result.addColumn(new Column("DEAL_ID", Column.Type.STRING, "ИД сделки", 100));
        result.addColumn(new Column("SUBDEALID", Column.Type.STRING, "ИД субсделки", 70, false, false));
        result.addColumn(new Column("PMT_REF", Column.Type.STRING, "ИД платежа", 100, false, false));

        result.addColumn(colValueDate = new Column("VDATE", Column.Type.DATE, "Дата валютирования", 80));
        result.addColumn(colPostDate = new Column("POSTDATE", Column.Type.DATE, "Дата проводки", 80));    // фильтр ?

        result.addColumn(new Column("AC_DR", Column.Type.STRING, "Счет ДБ", 160));
        result.addColumn(new Column("CCY_DR", Column.Type.STRING, "Валюта ДБ", 60));
        result.addColumn(new Column("AMT_DR", Column.Type.DECIMAL, "Сумма ДБ", 100));
        result.addColumn(new Column("AC_CR", Column.Type.STRING, "Счет КР", 160));
        result.addColumn(new Column("CCY_CR", Column.Type.STRING, "Валюта КР", 60));
        result.addColumn(new Column("AMT_CR", Column.Type.DECIMAL, "Сумма КР", 100));
        result.addColumn(new Column("AMTRU_CR", Column.Type.DECIMAL, "Сумма в рублях", 100, false, false));

        result.addColumn(new Column("ACCKEY_DR", Column.Type.STRING, "Ключи счета ДБ", 400));
        result.addColumn(new Column("ACCKEY_CR", Column.Type.STRING, "Ключи счета КР", 400));

        result.addColumn(new Column("STRN", Column.Type.STRING, "Сторно", 40));
        result.addColumn(new Column("STRNRF", Column.Type.STRING, "ИД сторно", 80, false, false));
        result.addColumn(new Column("FAN", Column.Type.STRING, "Веер", 40));
        result.addColumn(new Column("PAR_RF", Column.Type.STRING, "ИД веера", 80, false, false));
        result.addColumn(new Column("NRT", Column.Type.STRING, "Назначение", 175));

        result.addColumn(new Column("EMSG", Column.Type.STRING, "Описание ошибки", 1275));

        return result;
    }

    @Override
    protected ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("CR_DT", Column.Sort.DESC));
        return list;
    }
}
