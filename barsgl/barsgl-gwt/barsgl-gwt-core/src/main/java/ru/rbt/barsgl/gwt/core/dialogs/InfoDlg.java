package ru.rbt.barsgl.gwt.core.dialogs;

/**
 * Created by ER18837 on 08.12.15.
 */
public class InfoDlg extends MessageDlg {

    public InfoDlg () {
        super();
        InitFrame();
    }

    protected void InitFrame(){
        ok.setVisible(false);
    }

    @Override
    protected boolean onClickOK() throws Exception {
        return true;
    }

}
