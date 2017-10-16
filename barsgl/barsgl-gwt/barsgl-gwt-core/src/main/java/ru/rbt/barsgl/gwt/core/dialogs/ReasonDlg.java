package ru.rbt.barsgl.gwt.core.dialogs;

import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.core.ui.AreaBox;

/**
 * Created by akichigi on 16.06.16.
 */
public class ReasonDlg extends DlgFrame implements IAfterShowEvent {
    protected AreaBox box;

    @Override
    public Widget createContent() {
        box = new AreaBox();
        setAfterShowEvent(this);
        return box;
    }

    protected boolean check(){
        return (box.getValue() != null && !box.getValue().trim().isEmpty());
    }

    @Override
    protected void fillContent(){
        box.setValue((String) params);
    }

    @Override
    protected boolean onClickOK() throws Exception {
        params = box.getValue();
        return check();
    }

    @Override
    public void afterShow() {
        box.setFocus(true);
    }

}
