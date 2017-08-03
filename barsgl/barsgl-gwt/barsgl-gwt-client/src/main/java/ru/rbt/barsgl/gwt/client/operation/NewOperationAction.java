package ru.rbt.barsgl.gwt.client.operation;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Image;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
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
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

/**
 * Created by ER18837 on 17.02.16.
 */
public class NewOperationAction extends GridAction {
     private OperationHandsDlg dlg;
    private boolean isExtended;

    public NewOperationAction(GridWidget grid, ImageResource imageRecource) {
        this(grid, imageRecource, false);
    }

    public NewOperationAction(GridWidget grid, ImageResource imageRecource, boolean isExtended) {
        this(grid, null, "Ввести операцию", new Image(imageRecource), 10);
        this.isExtended = isExtended;
    }

    public NewOperationAction(GridWidget grid, String name, String hint, Image image, double separator) {
        super(grid, name, hint, image, separator);
    }

    protected Object getParams() {return null;}
    
    @Override
    public void execute() {
        if (isExtended) {
            dlg = new OperationExtDlg("Ввод операции GL по шаблону", FormAction.CREATE, grid.getTable().getColumns(), BatchPostStep.HAND1);
        }
        else {
            dlg = new OperationHandsDlg("Ввод бухгалтерской операции GL", FormAction.CREATE, grid.getTable().getColumns(), BatchPostStep.HAND1);
        }
        dlg.setDlgEvents(this);
        dlg.setAfterCancelEvent(new IAfterCancelEvent() {
            @Override
            public void afterCancel() {
                grid.refresh();
            }
        });

        Object params = getParams();
        dlg.show(params);
    }

    @Override
    public void onDlgOkClick(Object prms){
        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
        ManualOperationWrapper operationWrapper = (ManualOperationWrapper) prms;

        operationWrapper.setStatus(BatchPostStatus.NONE);
        operationWrapper.setAction( dlg.getOperationAction() == OperationHandsDlg.ButtonOperAction.OK ?
                BatchPostAction.SAVE : BatchPostAction.SAVE_CONTROL);
        operationWrapper.setNoCheckAccDeals(false);

        BarsGLEntryPoint.operationService.processOperationRq(operationWrapper, new AuthCheckAsyncCallback<RpcRes_Base<ManualOperationWrapper>>() {
            @Override
            public void onFailureOthers(Throwable throwable) {
                WaitingManager.hide();

                showInfo("Системная ошибка", "Возможено, операция не сохранена\nПроверьте наличие проводок по операции в нижней части окна" + throwable.getLocalizedMessage());
            }

            @Override
            public void onSuccess(RpcRes_Base<ManualOperationWrapper> operationWrappers) {
                final ManualOperationWrapper w1 = operationWrappers.getResult();
                final StringBuffer isAccDealOk = new StringBuffer();
                log.info("operationWrappers.isError()= "+ operationWrappers.isError());
                if (operationWrappers.isError()) {
                    if (w1.getErrorList().getErrorCode().equals("FIELDS_DEAL_SUBDEAL")){
                        showConfirm("Несоответствие параметров сделки !!!", w1.getErrorList().getErrorMessage(0),
                            new IDlgEvents() {
                            @Override
                            public void onDlgOkClick(Object p) throws Exception {
                                w1.setNoCheckAccDeals(true);
                                w1.getErrorList().clear();
                                isAccDealOk.append("Y");
                                log.info("onDlgOkClick = " + w1.isNoCheckAccDeals() + " " + isAccDealOk);
                                OperationRq(w1);
                            }
                        }
                        , new IAfterCancelEvent() {
                            @Override
                            public void afterCancel() {
                                dlg.mDealId.setFocus(true);
                            }
                        } , null);
//                        log.info("after onDlgOkClick = "+isAccDealOk);
//                        if (isAccDealOk.length() > 0) {
//                            log.info("isAccDealOk.length() > 0"+w1.isNoCheckAccDeals()+" "+isAccDealOk);
//                            OperationRq(w1);
//                        }else dlg.mDealId.setFocus(true);
                    }
//                    else if (true){
//                        showConfirm("Красное сальдо !!!!", operationWrappers.getMessage(), new IDlgEvents() {
//                            @Override
//                            public void onDlgOkClick(Object p) throws Exception {
//                                w1.getErrorList().clear();
//                                OperationRq(w1);
//                                isTestOk.append("Y");
//                            }
//                        }, null);
//                    }
                    else{
                        showInfo("Ошибка", operationWrappers.getMessage());
                    }
                    if (operationWrappers.getResult().isBalanceError()) {
                        final ManualOperationWrapper w1 = operationWrappers.getResult();
                        showConfirm("Красное сальдо !!!!", operationWrappers.getMessage(), new IDlgEvents() {
                                    @Override
                                    public void onDlgOkClick(Object p) throws Exception {
                                        w1.setNoCheckBalance(true);
                                        w1.setBalanceError(false);
                                        w1.getErrorList().clear();
                                        operationService.processOperationRq(w1, new AuthCheckAsyncCallback<RpcRes_Base<ManualOperationWrapper>>()
                                        {
                                            @Override
                                            public void onSuccess(RpcRes_Base<ManualOperationWrapper> w2) {
                                                if (w2.isError())
                                                {
                                                    showInfo("Ошибка", w2.getMessage());
                                                }
                                                else {
                                                    dlg.hide();
                                                    showInfo("Информация", w2.getMessage());
                                                    grid.refresh();
                                                }
                                            }
                                        });
                                    }
                                }
                                , null);
                    }
                    else {
                        showInfo("Ошибка", operationWrappers.getMessage());
                    }
                } else {
                    showInfo("Информация", operationWrappers.getMessage());
                    dlg.hide();
                    grid.refresh(); // TODO refreshAction.execute();
                }
                WaitingManager.hide();
            }
        });

    }

//    private void OperationRq(ManualOperationWrapper w1) {
//        operationService.processOperationRq(w1, new AuthCheckAsyncCallback<RpcRes_Base<ManualOperationWrapper>>() {
//            @Override
//            public void onSuccess(RpcRes_Base<ManualOperationWrapper> w2) {
//                if (w2.isError()) {
//                    showInfo("Ошибка", w2.getMessage());
//                } else {
//                    dlg.hide();
//                    showInfo("Информация", w2.getMessage());
//                    grid.refresh();
//                }
//            }
//        });
//    }
}
