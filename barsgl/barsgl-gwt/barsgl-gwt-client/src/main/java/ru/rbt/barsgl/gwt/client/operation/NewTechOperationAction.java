package ru.rbt.barsgl.gwt.client.operation;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterCancelEvent;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.BatchPostAction;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.BatchPostStep;
import ru.rbt.barsgl.shared.operation.ManualTechOperationWrapper;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

/**
 * Created by ER18837 on 17.02.16.
 */
public class NewTechOperationAction extends GridAction {

    private OperationTechHandsDlg2 dlg;
    private boolean isExtended;

    public NewTechOperationAction(GridWidget grid, ImageResource imageRecource) {
        this(grid, null, "Ввести операцию", new Image(imageRecource), 10);
        this.isExtended = isExtended;
    }

    public NewTechOperationAction(GridWidget grid, String name, String hint, Image image, double separator) {
        super(grid, name, hint, image, separator);
    }

    protected Object getParams() {return null;}
    
    @Override
    public void execute() {
        dlg = new OperationTechHandsDlg2("Ввод бухгалтерской операции по техсчетам", FormAction.CREATE, grid.getTable().getColumns(), BatchPostStep.HAND1);
        dlg.setDlgEvents(this);
        dlg.setAfterCancelEvent(new IAfterCancelEvent() {
            @Override
            public void afterCancel() {
                if (grid!=null) grid.refresh();
            }
        });
        Object params = getParams();
        dlg.show(params);
    }

    @Override
    public void onDlgOkClick(Object prms){

        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
        ManualTechOperationWrapper operationWrapper = (ManualTechOperationWrapper) prms;

        operationWrapper.setStatus(BatchPostStatus.NONE);
        operationWrapper.setAction( dlg.getOperationAction() == OperationTechHandsDlg2.ButtonOperAction.OK ?
                BatchPostAction.SAVE : BatchPostAction.SAVE_CONTROL);

        BarsGLEntryPoint.operationService.processTechOperationRq(operationWrapper, new AuthCheckAsyncCallback<RpcRes_Base<ManualTechOperationWrapper>>() {
            @Override
            public void onFailureOthers(Throwable throwable) {
                WaitingManager.hide();
                showInfo("Системная ошибка", "Возможено, операция не сохранена\nПроверьте наличие проводок по операции в нижней части окна" + throwable.getLocalizedMessage());
            }

            @Override
            public void onSuccess(RpcRes_Base<ManualTechOperationWrapper> operationWrappers) {
                if (operationWrappers.isError()) {
                    showInfo("Ошибка", operationWrappers.getMessage());
                } else {
                    showInfo("Информация", operationWrappers.getMessage());
                    dlg.hide();
                    grid.refresh(); // TODO refreshAction.execute();
                }
                WaitingManager.hide();
            }
        });
    }
}
