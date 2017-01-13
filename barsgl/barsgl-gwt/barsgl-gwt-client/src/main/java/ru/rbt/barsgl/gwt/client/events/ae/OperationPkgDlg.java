package ru.rbt.barsgl.gwt.client.events.ae;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.client.operation.OperationHandsDlg;
import ru.rbt.barsgl.gwt.client.operday.OperDayGetter;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterCancelEvent;
import ru.rbt.barsgl.gwt.core.dialogs.IDlgEvents;
import ru.rbt.barsgl.gwt.core.dialogs.ReasonDlg;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.shared.dict.FormAction;

import java.util.Date;


/**
 * Created by akichigi on 21.06.16.
 */
public class OperationPkgDlg extends DlgFrame {
    private TxtBox _pkgID;
    private TxtBox _fileName;
    private TxtBox _postDate;
    private FormAction _action;

    private OperationHandsDlg.ButtonOperAction operationAction;

    public OperationPkgDlg(String caption, FormAction action){
        super();
        _action = action;
        setCaption(caption);
        operationAction = OperationHandsDlg.ButtonOperAction.NONE;

        if (action == FormAction.DELETE) {
            ok.setText("Удалить");
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
    }

    private Button createSignButton(String caption){
        Button button = new Button(caption);
        button.addStyleName("dlg-button");
        button.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {

                try {
                    if (onClickOK()) {
                        operationAction = OperationHandsDlg.ButtonOperAction.OTHER;
                        doOnOkClick();
                    }
                } catch (Exception e) {
                    Window.alert(e.getMessage());
                }
            }
        });
        return button;
    }

    @Override
    public Widget createContent() {
        Grid grid = new Grid(3, 2);

        grid.setWidget(0, 0, new Label("Номер пакета"));
        grid.setWidget(0, 1, _pkgID = new TxtBox());
        _pkgID.setReadOnly(true);

        grid.setWidget(1, 0, new Label("Имя файла"));
        grid.setWidget(1, 1, _fileName = new TxtBox());
        _fileName.setReadOnly(true);

        grid.setWidget(2, 0, new Label("Дата проводки"));
        grid.setWidget(2, 1, _postDate = new TxtBox());
        _postDate.setReadOnly(true);

        return grid;
    }

    @Override
    protected void fillContent() {
        Object[] obj = (Object[])params;
        _pkgID.setValue(obj[0].toString());
        _fileName.setValue(obj[1].toString());
        _postDate.setValue(DateTimeFormat.getFormat(OperDayGetter.dateFormat).format((Date)obj[2]));
    }

    private Boolean _exitFlag = false;
    private String _reasonOfDeny = null;

    @Override
    protected boolean onClickOK() throws Exception {
        params = _reasonOfDeny;
        operationAction = OperationHandsDlg.ButtonOperAction.OK;

        if (_action == FormAction.RETURN && !_exitFlag){
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
            dlg.show();

            return _exitFlag;
        } else {
            return true;
        }
    }

    public OperationHandsDlg.ButtonOperAction getOperationAction() {
        return operationAction;
    }
}
