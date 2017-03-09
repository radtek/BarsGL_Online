package ru.rbt.barsgl.gwt.client.compLookup;

import ru.rbt.barsgl.gwt.client.dict.dlg.PlCodeForm;
import ru.rbt.grid.gwt.client.gridForm.GridForm;
import ru.rbt.barsgl.gwt.client.gridForm.GridFormDlgBase;
import ru.rbt.barsgl.shared.Utils;

import java.util.HashMap;

/**
 * Created by ER18837 on 29.11.16.
 */
public class LookUpPlcode extends LookupBoxBase {

    public LookUpPlcode(String width) {
        this();
        setWidth(width);
    }

    public LookUpPlcode() {
        super();
        setMaxLength(5);
        setVisibleLength(5);
        setMask("[0-9]");
    }

    @Override
    protected GridFormDlgBase getDialog() {
        return new GridFormDlgBase("Справочник символов ОФР") {
            @Override
            protected GridForm getGridForm() {
                return new PlCodeForm();
            }

            @Override
            protected String getGridWidth() {
                return "900px";
            }

            @Override
            protected boolean setResultList(HashMap<String, Object> result) {
                if (null != result) {
                    setValue(Utils.value((String) result.get("PLCODE")));
                }
                return true;
            }

            @Override
            protected Object[] getInitialFilterParams() {
                return null;
            }

            @Override
            protected void onSetResult() {
                LookUpPlcode.this.onSetResult();
            }
        };
    }
}
