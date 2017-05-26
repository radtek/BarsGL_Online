package ru.rbt.barsgl.gwt.client.operation;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.ui.AreaBox;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.BatchPostStep;
import ru.rbt.barsgl.shared.operation.ManualTechOperationWrapper;

import static ru.rbt.barsgl.gwt.core.comp.Components.createAreaBox;
import static ru.rbt.barsgl.gwt.core.comp.Components.createLabel;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;


/**
 * Created by ER18837 on 18.07.16.
 */
public class OperationTechHandsViewDlg extends OperationTechHandsDlg2 {
    protected AreaBox mReasonOfDeny;

    public OperationTechHandsViewDlg(String title, FormAction action, Columns columns, BatchPostStep step) {
        super(title, action, columns, step);
    }

    @Override
    public Widget createContent() {
        VerticalPanel mainVP = (VerticalPanel)super.createContent();
        Grid grid = new Grid(1,2);
        grid.setWidget(0, 0, createLabel("Причина отказа / Код ошибки", LABEL2_WIDTH));
        grid.setWidget(0, 1, mReasonOfDeny = createAreaBox(LONG_DEP_WIDTH, "70px"));
        mReasonOfDeny.setReadOnly(true);
        mainVP.add(grid);
        return mainVP;
    }

    @Override
    protected void fillUp(){
        ManualTechOperationWrapper operation = (ManualTechOperationWrapper)params;
        super.fillUp();
        mDealId.setValue(isEmpty(operation.getDealId()) ? operation.getPaymentRefernce() : operation.getDealId());

        String errorMessage = operation.getErrorMessage();
        String reason = operation.getReasonOfDeny();
        mReasonOfDeny.setValue((!isEmpty(reason) ? reason + "\n" : "") + errorMessage);
        setControlsDisabled();
    }

    @Override
    protected void setControlsEnabled(){
        super.setControlsEnabled();
        mCrAccTypeButton.setEnabled(false);
        mDtAccTypeButton.setEnabled(false);
        mDtAccType.setEnabled(false);
        mCrAccType.setEnabled(false);
    }
}
