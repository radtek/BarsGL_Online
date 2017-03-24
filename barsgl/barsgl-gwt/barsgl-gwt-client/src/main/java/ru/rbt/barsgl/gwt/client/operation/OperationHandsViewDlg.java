package ru.rbt.barsgl.gwt.client.operation;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.ui.AreaBox;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.BatchPostStep;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;

import ru.rbt.barsgl.gwt.core.comp.Components;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;


/**
 * Created by ER18837 on 18.07.16.
 */
public class OperationHandsViewDlg extends OperationHandsDlg {
    protected AreaBox mReasonOfDeny;

    public OperationHandsViewDlg(String title, FormAction action, Columns columns, BatchPostStep step) {
        super(title, action, columns, step);
    }

    @Override
    public Widget createContent() {
        VerticalPanel mainVP = (VerticalPanel)super.createContent();
        Grid grid = new Grid(1,2);
        grid.setWidget(0, 0, Components.createLabel("Причина отказа / Код ошибки", LABEL2_WIDTH));
        grid.setWidget(0, 1, mReasonOfDeny = Components.createAreaBox(LONG_DEP_WIDTH, "70px"));
        mReasonOfDeny.setReadOnly(true);
        mainVP.add(grid);
        return mainVP;
    }

    @Override
    protected void fillUp(){
        ManualOperationWrapper operation = (ManualOperationWrapper)params;
        super.fillUp();
        mDealId.setValue(isEmpty(operation.getDealId()) ? operation.getPaymentRefernce() : operation.getDealId());

        String errorMessage = operation.getErrorMessage();
        String reason = operation.getReasonOfDeny();
        mReasonOfDeny.setValue((!isEmpty(reason) ? reason + "\n" : "") + errorMessage);
    }

    @Override
    protected void setControlsEnabled(){
        super.setControlsEnabled();
        mDtButton.setEnabled(true);
        mCrButton.setEnabled(true);
    }
}
