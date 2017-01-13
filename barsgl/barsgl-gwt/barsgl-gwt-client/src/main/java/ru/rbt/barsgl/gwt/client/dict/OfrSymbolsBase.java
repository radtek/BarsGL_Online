package ru.rbt.barsgl.gwt.client.dict;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.FormAction;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by ER18837 on 21.01.16.
 */
public class OfrSymbolsBase extends EditableDictionary<Serializable> {

    protected Column colAcod;
    protected Column colSQ;
    protected Column colCustType;
    protected Column colOfrSymbol;
    protected Column colDateBegin;
    protected Column colDateEnd;

    public OfrSymbolsBase(String title) {
        super(title);
    }

    @Override
    protected String prepareSql() {
        return "select ID, HBITEM, HBCTYP, HBMIAC, HBMISQ, DAT, DATTO from IMBCBHBPN";
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();
        Column col;

        result.addColumn(new Column("ID", Column.Type.STRING, "ID", 20, false, true));
        result.addColumn(colAcod = new Column("HBMIAC", Column.Type.STRING, "ACOD Midas", 20));
        result.addColumn(colSQ = new Column("HBMISQ", Column.Type.STRING, "SQ Midas", 20));
        result.addColumn(colCustType = new Column("HBCTYP", Column.Type.STRING, "Тип собственности", 20));
        result.addColumn(colOfrSymbol = new Column("HBITEM", Column.Type.STRING, "Символ доходов/расходов", 20, true, false, Column.Sort.ASC, ""));
        result.addColumn(colDateBegin = new Column("DAT", Column.Type.DATE, "Дата начала", 30));
        result.addColumn(colDateEnd = new Column("DATTO", Column.Type.DATE, "Дата конца", 30));
        return result;
    }

    @Override
    protected void save(Serializable cnw, FormAction action, AsyncCallback<RpcRes_Base<Serializable>> asyncCallbackImpl) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem(colAcod.getName(), Column.Sort.ASC));
        list.add(new SortItem(colCustType.getName(), Column.Sort.ASC));
        list.add(new SortItem(colOfrSymbol.getName(), Column.Sort.ASC));
        list.add(new SortItem(colSQ.getName(), Column.Sort.ASC));
        list.add(new SortItem(colDateBegin.getName(), Column.Sort.ASC));
        return list;
    }

}
