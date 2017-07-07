package ru.rbt.barsgl.gwt.client.pd;

import ru.rbt.barsgl.gwt.client.operation.OperationPostingForm;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.shared.filter.FilterCriteria;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.enums.OperState;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.*;

/**
 * Created by akichigi on 21.05.15.
 */
public class PDForm extends OperationPostingForm {
    public static final String FORM_NAME = "Операции и полупроводки";

    public PDForm(){
        super(FORM_NAME, null, null, true);
    }

    @Override
    public ArrayList<SortItem> getInitialDetailSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("POST_TYPE", Column.Sort.ASC));
        return list;
    }

    @Override
    protected Table prepareDetailTable() {
        Table result = new Table();

        result.addColumn(colGloid = new Column("GLO_REF", LONG, "GLO_REF", 70, false, true));
        result.addColumn(new Column("POST_TYPE", STRING, "Тип проводки", 90));
        result.addColumn(new Column("PCID", LONG, "ID проводки", 100));// No Space
        result.addColumn(new Column("ID", LONG, "ID полупроводки", 100));// No Space
        result.addColumn(new Column("BSAACID", STRING, "Счет ЦБ", 160));
        result.addColumn(new Column("CCY", STRING, "Валюта", 90));
        result.addColumn(new Column("AMNT", DECIMAL, "Сумма в валюте счета", 140));
        result.addColumn(new Column("AMNTBC", DECIMAL, "Сумма в руб.", 140));
        result.addColumn(new Column("PBR", STRING, "Система- источник", 90));
        result.addColumn(new Column("PNAR", STRING, "Описание", 300));
        result.addColumn(new Column("INVISIBLE", STRING, "Признак видимости", 90));
        result.addColumn(new Column("PREF", STRING, "ИД платежа / сделки", 160));
        result.addColumn(new Column("MO_NO", STRING, "Мем.ордер", 120));

        return result;
    }

    @Override
    protected String prepareDetailSql() {
        return  "select GLO_REF, POST_TYPE, PCID, ID, BSAACID, CCY, cast( (DECIMAL(AMNT)/INTEGER(POWER(10,CC.NBDP))) as decimal(19,2)) as AMNT, "
                + "AMNTBC * 0.01 as AMNTBC, PBR, PNAR, "
                + "CASE WHEN INVISIBLE = 0 THEN 'Y' ELSE 'N' END AS INVISIBLE, "
                + "PREF, MO_NO " +
                "from V_GL_PD GL left join CURRENCY CC on GL.CCY=CC.GLCCY" ;
    }

    @Override
    public ArrayList<FilterItem> createLinkFilterCriteria(Row row) {

        ArrayList<FilterItem> list = new ArrayList<FilterItem>();
        list.add(new FilterItem(colGloid, FilterCriteria.EQ, row == null ? -1 : row.getField(0).getValue()));

        return list;
    }
}
