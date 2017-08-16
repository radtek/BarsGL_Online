package ru.rbt.barsgl.gwt.client.operBackValue.dict;

import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.grid.gwt.client.gridForm.GridForm;

/**
 * Created by er17503 on 15.08.2017.
 */
public class BVDepthForm extends GridForm{
    public static final String FORM_NAME = "Глубина backvalue";

    public BVDepthForm(){
        super(FORM_NAME);
        reconfigure();
    }

    private void reconfigure(){

    }

    @Override
    protected Table prepareTable() {
        return null;
    }

    @Override
    protected String prepareSql() {
        return null;
    }
}
