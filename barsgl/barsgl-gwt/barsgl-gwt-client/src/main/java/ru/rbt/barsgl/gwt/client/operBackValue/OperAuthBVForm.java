package ru.rbt.barsgl.gwt.client.operBackValue;

import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.grid.gwt.client.gridForm.GridForm;

/**
 * Created by er17503 on 24.07.2017.
 */
public class OperAuthBVForm extends GridForm {
    public static final String FORM_NAME = "Авторизованные операции";

    public OperAuthBVForm() {
        super(FORM_NAME, true);
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
