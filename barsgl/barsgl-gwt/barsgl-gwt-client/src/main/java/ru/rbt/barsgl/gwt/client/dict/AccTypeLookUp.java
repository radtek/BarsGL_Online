package ru.rbt.barsgl.gwt.client.dict;

import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.forms.IDisposable;

import java.util.ArrayList;

/**
 * Created by akichigi on 29.08.16.
 */
public class AccTypeLookUp extends AccType {
    @Override
    protected ArrayList<FilterItem> getInitialFilterCriteria(Object[] initialFilterParams){
        return null;
    }

    @Override
    protected Table prepareTable(){
        Table table = super.prepareTable();
        table.getColumn("ACCTYPE").setWidth(28);
        table.getColumn("PL_ACT").setVisible(false);
        table.getColumn("FL_CTRL").setVisible(false);

        return table;
    }



}


