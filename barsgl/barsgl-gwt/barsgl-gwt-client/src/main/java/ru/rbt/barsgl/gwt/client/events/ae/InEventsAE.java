/*
 * ООО "Артком Системы" 2015
 * BARS GL
 */
package ru.rbt.barsgl.gwt.client.events.ae;

import ru.rbt.grid.gwt.client.gridForm.GridForm;
import ru.rbt.barsgl.gwt.client.quickFilter.AEDateQFAction;
import ru.rbt.barsgl.gwt.client.quickFilter.AEDateQFParams;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.getYesNoList;
import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.*;

/**
 *
 * @author Andrew Samsonov
 */
public class
InEventsAE extends GridForm {
  public static final String FORM_NAME = "Входящие сообщения АЕ";
  protected Column colPkgDate;
  protected Column colValueDate;

  public InEventsAE() {
    super(FORM_NAME, true);
    reconfigure();
  }

  private void reconfigure() {
    AEDateQFAction quickFilterAction;
    abw.addAction(quickFilterAction = new AEDateQFAction(grid, colPkgDate, colValueDate, AEDateQFParams.DateFilterField.PKG_DATE, false));
    abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
    quickFilterAction.execute();
  }

  @Override
  public ArrayList<SortItem> getInitialSortCriteria() {
    ArrayList<SortItem> list = new ArrayList<SortItem>();
    list.add(new SortItem("ID_PKG", Column.Sort.DESC));
    return list;
  }

  @Override
  protected Table prepareTable() {
    Table result = new Table();
    Column col;
    HashMap<Serializable, String> yesNoList = getYesNoList();

    result.addColumn(new Column("ID_PKG", Column.Type.LONG, "ID пакета", 70, true, false, Column.Sort.DESC, ""));
    result.addColumn(colPkgDate = new Column("DT_LOAD", Column.Type.DATE, "Дата пакета", 80));
    result.addColumn(new Column("ID", Column.Type.LONG, "ID сообщения AE", 70, false, false));
    result.addColumn(new Column("ECODE", Column.Type.INTEGER, "Статус сообщения", 80));
    result.addColumn(new Column("ID_PST", Column.Type.STRING, "ИД сообщ AE", 80));
    result.addColumn(new Column("SRC_PST", Column.Type.STRING, "Источник сделки", 80));
    result.addColumn(new Column("EVTP", Column.Type.STRING, "Тип события", 70));    
    result.addColumn(colValueDate = new Column("VDATE", Column.Type.DATE, "Дата валютирования", 80));
    result.addColumn(new Column("DEAL_ID", Column.Type.STRING, "ИД сделки", 70));
    result.addColumn(new Column("PMT_REF", Column.Type.STRING, "ИД платежа", 70));
    result.addColumn(new Column("AC_DR", Column.Type.STRING, "Счет Дебета", 160));
    result.addColumn(new Column("CCY_DR", STRING, "Валюта ДБ", 60));
    result.addColumn(new Column("AMT_DR", DECIMAL, "Сумма ДБ", 100));
    result.addColumn(new Column("AMTRU_DR", DECIMAL, "Сумма в рублях ДБ", 100, false, false));
    result.addColumn(new Column("AC_CR", Column.Type.STRING, "Счет Кредита", 160));
    result.addColumn(new Column("CCY_CR", STRING, "Валюта КР", 60));
    result.addColumn(new Column("AMT_CR", DECIMAL, "Сумма КР", 100));
    result.addColumn(new Column("AMTRU_CR", DECIMAL, "Сумма в рублях КР", 100, false, false));
    result.addColumn(new Column("ACCKEY_DR", Column.Type.STRING, "Атрибуты счета Дебета", 550));
    result.addColumn(new Column("ACCKEY_CR", Column.Type.STRING, "Атрибуты счета Кредита", 550));
    result.addColumn(col = new Column("FAN", STRING, "Веер", 50, false, false));
    col.setList(yesNoList);
    result.addColumn(col = new Column("STRN", STRING, "Сторно", 50, false, false));
    col.setList(yesNoList);
    result.addColumn(new Column("STRNRF", LONG, "Сторно операция", 70, false, false));
    result.addColumn(new Column("RNRTS", STRING, "Назначение", 100, false, false));
    result.addColumn(new Column("EMSG", Column.Type.STRING, "Описание ошибки", 1200));

    return result;
  }

  @Override
  protected String prepareSql() {
    return "select * from (" +
           "select PST.ID_PKG, DATE(PKG.DT_LOAD) as DT_LOAD, PST.ECODE, PST.ID, PST.ID_PST, PST.SRC_PST, PST.EVTP, PST.VDATE, PST.DEAL_ID, PST.PMT_REF, " +
           "PST.AC_DR, PST.CCY_DR, PST.AMT_DR, PST.AMTRU_DR, PST.ACCKEY_DR, " +
           "PST.AC_CR, PST.CCY_CR, PST.AMT_CR, PST.AMTRU_CR, PST.ACCKEY_CR, PST.EMSG, " +
           "PST.RNRTS, PST.FAN, PST.STRN, PST.STRNRF " +
           "from GL_ETLPKG PKG, GL_ETLPST PST where " +
           "PST.ID_PKG = PKG.ID_PKG " +
            ") v ";
  }
}
