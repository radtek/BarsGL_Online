package ru.rbt.barsgl.gwt.core.dialogs;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;

/**
 * Created by ER18837 on 08.12.15.
 */
public class ConfirmDlg extends MessageDlg {

	private Object outParams;
	
    public ConfirmDlg () {
        super();
        InitFrame();
    }

    @Override
    protected boolean onClickOK() throws Exception {
    	params = outParams;
    	this.hide();
        return true;
    }

    protected void InitFrame(){
        ok.setText(TEXT_CONSTANTS.messageDlg_Continue());
        cancel.setText(TEXT_CONSTANTS.formInput_cancel());
    }

    public void setParams(Object params) {
    	this.outParams = params;
    }

    public Object getParams() {
    	return this.outParams;
    }
}
