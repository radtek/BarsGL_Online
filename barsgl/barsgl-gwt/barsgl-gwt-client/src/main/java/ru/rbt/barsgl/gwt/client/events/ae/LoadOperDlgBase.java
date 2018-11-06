package ru.rbt.barsgl.gwt.client.events.ae;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.web.bindery.event.shared.HandlerRegistration;
import ru.rbt.barsgl.gwt.client.loadFile.LoadFileAnyDlg;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.check.CheckFileExtention;
import ru.rbt.barsgl.gwt.client.check.CheckNotEmptyString;
import ru.rbt.barsgl.gwt.client.comp.DataListBox;
import ru.rbt.barsgl.gwt.client.dictionary.BatchPostingFormDlg;
import ru.rbt.barsgl.gwt.client.gridForm.GridFormDlgBase;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterShowEvent;
import ru.rbt.barsgl.gwt.core.events.DataListBoxEvent;
import ru.rbt.barsgl.gwt.core.events.DataListBoxEventHandler;
import ru.rbt.barsgl.gwt.core.events.LocalEventBus;
import ru.rbt.barsgl.gwt.core.ui.RichAreaBox;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.BatchPostAction;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.shared.user.AppUserWrapper;

import java.util.HashMap;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.*;
import ru.rbt.barsgl.gwt.core.comp.Components;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.*;
import ru.rbt.security.gwt.client.security.SecurityEntryPoint;

/**
 * Created by akichigi on 20.06.16.
 */
abstract public class LoadOperDlgBase extends LoadFileAnyDlg {
    private final static String LIST_DELIMITER = "#";

    private DataListBox mSource;
    private DataListBox mDepartment;
    private CheckBox excludeOper;

    private Hidden source;
    private Hidden department;
    private Hidden movementOff;

    private Long idPackage = null;

    private int asyncListCount = 2; /*count async lists: mDepartment; mSource*/
    private HandlerRegistration registration;
    private Timer timer;

    public LoadOperDlgBase(){
        super();
        ok.setText("Передать на подпись");
    }

    protected abstract boolean isExcludeVisible();

    @Override
    public void beforeCreateContent() {
        registration =  LocalEventBus.addHandler(DataListBoxEvent.TYPE, createDataListBoxEventHandler());
    }

    protected DataListBoxEventHandler createDataListBoxEventHandler(){
        return new DataListBoxEventHandler() {

            @Override
            public void completeLoadData(String dataListBoxId) {
                asyncListCount--;

                if (asyncListCount == 0) {
                    registration.removeHandler();

                    AppUserWrapper wrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
                    if (wrapper != null){
                        mDepartment.setValue(wrapper.getBranch());
                    }
                }
            }
        };
    }

    @Override
    protected Panel createDataPanel(Panel parentPanel, Panel hiddenPanel) {
        Grid g = new Grid(2, 2);
        parentPanel.add(g);
        mSource = createDealSourceAuthListBox("", FIELD_WIDTH);
        g.setWidget(0, 0, Components.createLabel("Источник сделки", LABEL_WIDTH));
        g.setWidget(0, 1, mSource);

        mDepartment = createDepartmentListBox("", FIELD_WIDTH, true);
        g.setWidget(1, 0, Components.createLabel("Подразделение", LABEL_WIDTH));
        g.setWidget(1, 1, mDepartment);

        parentPanel.add(excludeOper = new CheckBox("Исключение создания проводки в АБС по контролируемым счетам"));
        excludeOper.setVisible(isExcludeVisible());

        source = new Hidden();
        source.setName("source");
        hiddenPanel.add(source);

        department = new Hidden();
        department.setName("department");
        hiddenPanel.add(department);

        movementOff = new Hidden();
        movementOff.setName("movement_off");
        hiddenPanel.add(movementOff);

        parentPanel.add(hidden);
        return parentPanel;
    }

    @Override
    protected boolean acceptResponse(String[] list) {
        idPackage = parseLong(list[1], "пакет", ":");
        Long all = parseLong(list[2], "всего", ":");
        Long err = parseLong(list[3], "с ошибкой", ":");
        boolean isError = (null != err) && (err > 0);
        boolean load = (idPackage != null);
        errorButton.setEnabled(isError);
        showButton.setEnabled(load);
        deleteButton.setEnabled(load);
        boolean isOk = load && !isError;
        return isOk;
    }

    @Override
    protected void onClickDelete(ClickEvent clickEvent) {
        ManualOperationWrapper wrapper = new ManualOperationWrapper();
        wrapper.setPkgId((Long) idPackage);
        wrapper.setAction(BatchPostAction.DELETE);

        AppUserWrapper appUserWrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
        wrapper.setUserId(appUserWrapper.getId());

        BarsGLEntryPoint.operationService.processPackageRq(wrapper, new AuthCheckAsyncCallback<RpcRes_Base<ManualOperationWrapper>>() {
            @Override
            public void onSuccess(RpcRes_Base<ManualOperationWrapper> wrapper) {
                if (wrapper.isError()) {
                    showInfo("Ошибка", wrapper.getMessage());
                } else {
                    switchControlsState(true);
                    deleteButton.setEnabled(false);
                    showButton.setEnabled(false);
                    errorButton.setEnabled(false);
                    clearResult();
                    idPackage = null;
                    showInfo("Информация", wrapper.getMessage());
                }
            }
        });
    }

    @Override
    protected void onClickUpload(ClickEvent clickEvent) {
        CheckNotEmptyString check = new CheckNotEmptyString();
        source.setValue(check((String) mSource.getValue()
                , "Источник сделки", "поле не заполнено", check));
        department.setValue((String) mDepartment.getValue());
        movementOff.setValue(excludeOper.getValue().toString());
    }

    @Override
    protected void switchControlsState(boolean state) {
        uploadButton.setEnabled(state);
        mSource.setEnabled(state);
        excludeOper.setEnabled(state);
        mDepartment.setEnabled(state);
        anchorExample.setEnabled(state);
    }

    @Override
    protected void onClickShow(ClickEvent clickEvent) {
        createShowDlg(false);
    }

    @Override
    protected void onClickError(ClickEvent clickEvent) {
        createShowDlg(true);
    }

    private void createShowDlg(final boolean error) {
        try {
            GridFormDlgBase dlg = new BatchPostingFormDlg(error) {
                @Override
                protected boolean setResultList(HashMap<String, Object> result) {
                    return true;
                }

                @Override
                protected Object[] getInitialFilterParams() {
                    return new Object[] {idPackage};
                }
            };
            dlg.setModal(true);
            dlg.show();
        } catch (Exception e) {
            Window.alert(e.getMessage());
        }
    }

    @Override
    protected boolean onClickOK() throws Exception {
        params = idPackage;
        return idPackage != null;
    }

    @Override
    protected void fillContent(){
        if (asyncListCount > 0) {
            showPreload(true);
            timer = new Timer() {
                @Override
                public void run() {
                    if (asyncListCount == 0) {
                        timer.cancel();
                        showPreload(false);
                    }
                }
            };

            timer.scheduleRepeating(500);
        }
    }

}
