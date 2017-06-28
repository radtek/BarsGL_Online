package ru.rbt.barsgl.gwt.client.operation;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import ru.rbt.security.gwt.client.operday.IDataConsumer;
import ru.rbt.security.gwt.client.operday.OperDayGetter;
import ru.rbt.barsgl.gwt.client.pd.PostingTechDlg;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterCancelEvent;
import ru.rbt.barsgl.gwt.core.dialogs.IDlgEvents;
import ru.rbt.barsgl.gwt.core.dialogs.ReasonDlg;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.BatchPostStep;
import ru.rbt.barsgl.shared.operation.ManualTechOperationWrapper;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;

import static ru.rbt.security.gwt.client.operday.OperDayGetter.getOperday;

/**
 * Created by akichigi on 10.06.16.
 */
public class OperationTechHandsDlg extends PostingTechDlg {
    public enum ButtonOperAction {NONE, OK, OTHER}
    private ButtonOperAction operationAction;

    public OperationTechHandsDlg(String title, FormAction action, Columns columns, BatchPostStep step) {
        super(title, action, columns);
        inputMethod = "Manual";
        operationAction = ButtonOperAction.NONE;
        if ((action == FormAction.CREATE || action == FormAction.UPDATE) &&
            (step.isInputStep() || step.isControlStep())){
            btnPanel.insert(createSignButton(step.isInputStep() ? "Передать на подпись" : "Подписать"), 0);
        }
        else if (action == FormAction.SEND) {
            ok.setText("Передать на подпись");
        }
        else if (action == FormAction.SIGN){
            ok.setText("Подписать");
        }
        else if (action == FormAction.RETURN){
            ok.setText("Вернуть на доработку");
        }
        else if (action == FormAction.CONFIRM){
            ok.setText("Отказать");
            btnPanel.insert(createSignButton("Подтвердить"), 0);
        }
        else if (action == FormAction.PREVIEW){
            ok.setVisible(false);
        }

        getOperDay();
    }

    @Override
    protected void getOperDay() {
        getOperday(new IDataConsumer<OperDayWrapper>() {
            @Override
            public void accept(OperDayWrapper wrapper) {
                String operDayStr = wrapper.getCurrentOD();
                mDateOperDay.setValue(operDayStr);
                operday = DateTimeFormat.getFormat(OperDayGetter.dateFormat).parse(operDayStr);
            }
        });
    }

    private Button createSignButton(String caption){
        Button button = new Button(caption);
        button.addStyleName("dlg-button");
        button.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                operationAction = ButtonOperAction.OTHER;
                try {
                    if (OperationTechHandsDlg.super.onClickOK()){
                        doOnOkClick();
                    }
                } catch (Exception e) {
                    Window.alert(e.getMessage());
                }
            }
        });
        return button;
    }

    public ButtonOperAction getOperationAction() {
        return operationAction;
    }

    @Override
    protected boolean onClickOK() throws Exception {
        operationAction = ButtonOperAction.OK;
        return super.onClickOK();
    }

    @Override
    protected void setControlsEnabled(){
        if ( action == FormAction.DELETE || action == FormAction.SEND || action == FormAction.SIGN ||
             action == FormAction.RETURN || action == FormAction.CONFIRM || action == FormAction.PREVIEW){
            //TODO блокировка полей на UPDATE (HAND2)
            setControlsDisabled();
        }
    }

    private Boolean _exitFlag = false;

    @Override
    protected Boolean beforeReturn(final Object prm){
        ManualTechOperationWrapper wrapper = (ManualTechOperationWrapper) prm;
        wrapper.setReasonOfDeny(_reasonOfDeny);

        if (action == FormAction.RETURN && !_exitFlag){
            final ReasonDlg dlg = new ReasonDlg();
            dlg.setCaption("Введите причину возврата");
            dlg.setDlgEvents(new IDlgEvents() {
                @Override
                public void onDlgOkClick(Object prms) throws Exception {
                    ok.setEnabled(true);
                    cancel.setEnabled(true);
                    _reasonOfDeny = (String) prms;
                    _exitFlag = true;
                    dlg.hide();
                    ok.click();
                }
            });
            dlg.setAfterCancelEvent(new IAfterCancelEvent() {
                @Override
                public void afterCancel() {
                    ok.setEnabled(true);
                    cancel.setEnabled(true);
                }
            });

            ok.setEnabled(false);
            cancel.setEnabled(false);
            dlg.show(_reasonOfDeny);

            return _exitFlag;
        } else {
            return true;
        }
    }
}
