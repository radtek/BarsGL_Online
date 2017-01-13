package ru.rbt.barsgl.gwt.client.compLookup;

import ru.rbt.barsgl.gwt.client.dict.BssForm;
import ru.rbt.barsgl.gwt.client.gridForm.GridForm;
import ru.rbt.barsgl.gwt.client.gridForm.GridFormDlgBase;
import ru.rbt.barsgl.shared.Utils;

import java.util.HashMap;

/**
 * Created by ER18837 on 29.11.16.
 */
public class LookUpAcc2 extends LookupBoxBase {

    public LookUpAcc2(String width) {
        this();
        setWidth(width);
    }

    public LookUpAcc2() {
        super();
        setMaxLength(5);
        setVisibleLength(5);
        setMask("[0-9]");
    }

    protected Object[] getInitialFilterParams() {
        return null;
    };

    @Override
    protected GridFormDlgBase getDialog() {
        return new GridFormDlgBase("Справочник балансовых счетов 2-го порядка") {
            @Override
            protected GridForm getGridForm() {
                return new BssForm() {
                    @Override
                    protected Object[] getInitialFilterParams() {
                        return LookUpAcc2.this.getInitialFilterParams();
                    }
                };
            }

            @Override
            protected boolean setResultList(HashMap<String, Object> result) {
                if (null != result) {
                    setValue(Utils.value((String) result.get("ACC2")));
                }
                return true;
            }

            @Override
            protected Object[] getInitialFilterParams() {
                return null;
            }
            
            @Override
            protected void onSetResult() {
            	LookUpAcc2.this.onSetResult();
            }

        };
    }
}
