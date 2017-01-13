package ru.rbt.barsgl.gwt.client.compLookup;

import com.google.gwt.user.client.Window;
import ru.rbt.barsgl.gwt.client.gridForm.GridFormDlgBase;
import ru.rbt.barsgl.gwt.core.ui.BtnTxtBox;

/**
 * Created by ER18837 on 29.11.16.
 */
public abstract class LookupBoxBase extends BtnTxtBox {
    private final static String OK_CAPTION = "Выбрать";

    protected abstract GridFormDlgBase getDialog();
    protected void onSetResult() {};

    @Override
    protected void saveValue() {
        value =  textBox.getText().trim();
    }

    @Override
    protected boolean validate(String text) {
        return true;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
        textBox.setText(value);
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public boolean hasValue() {
        return value != null;
    }

    @Override
    public void onBntClick() {
        try {
            GridFormDlgBase dlg = getDialog();
            dlg.setOkButtonCaption(OK_CAPTION);
            dlg.setModal(true);
            dlg.show();
        } catch (Exception e) {
            Window.alert(e.getMessage());
        }
    }
}
