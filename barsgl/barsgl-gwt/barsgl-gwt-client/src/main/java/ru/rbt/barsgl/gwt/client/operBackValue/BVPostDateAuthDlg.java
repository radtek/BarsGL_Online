package ru.rbt.barsgl.gwt.client.operBackValue;

import ru.rbt.barsgl.gwt.client.operation.OperationDlg;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.shared.dict.FormAction;

/**
 * Created by er17503 on 01.08.2017.
 */
public class BVPostDateAuthDlg extends OperationDlg {
    public BVPostDateAuthDlg(String title, FormAction action, Columns columns) {
        super(title, action, columns);
        ok.setText("Подтвердить");
    }

    @Override
    protected void setControlsDisabled(){
        super.setControlsDisabled();
        mDateOperation.setEnabled(true);
        ok.setVisible(true);
    }
}
