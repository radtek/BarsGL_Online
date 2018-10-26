package ru.rbt.barsgl.gwt.client.account;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.dictionary.BatchPostingFormDlg;
import ru.rbt.barsgl.gwt.client.gridForm.GridFormDlgBase;
import ru.rbt.barsgl.gwt.client.loadFile.LoadFileAnyDlg;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.dialogs.IDlgEvents;
import ru.rbt.barsgl.gwt.server.upload.UploadFileType;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.operation.AccountBatchWrapper;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.shared.user.AppUserWrapper;

import java.util.HashMap;

import static ru.rbt.barsgl.gwt.client.account.AccountBatchErrorForm.ViewType.V_LOAD;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showConfirm;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;
import static ru.rbt.barsgl.shared.operation.AccountBatchWrapper.AccountBatchAction.DELETE;

/**
 * Created by er18837 on 17.10.2018.
 */
public class LoadAccountDlg extends LoadFileAnyDlg {
    public static final String TITLE = "Загрузка счетов из Excel файла";
    private Long idPackage = null;

    public LoadAccountDlg(){
        super();
        ok.setText("Открыть счета");
    }

    @Override
    protected String getFileUploadName() {
        return "excel-upload003";
    }

    @Override
    protected String getServletUploadName() {
        return "service/UploadFileHandler";
    }

    @Override
    protected String getUploadType() {
        return UploadFileType.Account.name();
    }

    @Override
    protected String getExampleName() {
        return "example_acc.xlsx";
    }

    @Override
    protected void onClickDelete(ClickEvent clickEvent) {
        showConfirm("Вы уверены, что хотите удалить пакет?",
                new IDlgEvents() {
                    @Override
                    public void onDlgOkClick(Object p) throws Exception {
                        executeCommand(DELETE);
                    }
                },
                null);
    }

    private void executeCommand(final AccountBatchWrapper.AccountBatchAction batchAction) {
        AccountBatchWrapper wrapper = new AccountBatchWrapper();
        wrapper.setPackageId((Long) idPackage);
        wrapper.setAction(batchAction);

        AppUserWrapper appUserWrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
        wrapper.setUserId(appUserWrapper.getId());

        BarsGLEntryPoint.accountService.processAccountBatchRq(wrapper, new AuthCheckAsyncCallback<RpcRes_Base<AccountBatchWrapper>>() {
            @Override
            public void onSuccess(RpcRes_Base<AccountBatchWrapper> wrapper) {
                if (wrapper.isError()) {
                    showInfo("Ошибка", wrapper.getMessage());
                } else {
                    switchControlsState(true);
                    switchButtonState(false, false);
                    loadingResult.clear();
                    idPackage = null;
                    showInfo("Информация", wrapper.getMessage());
                }
            }
        });

    }

    @Override
    protected void onClickUpload(ClickEvent clickEvent) {

    }

    @Override
    protected void onClickShow(ClickEvent clickEvent) {
        try {
            GridFormDlgBase dlg = new AccountBatchFormDlg(V_LOAD) {
                @Override
                public AccountBatchErrorForm.ViewType getViewType() {
                    return V_LOAD;
                }

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
    protected void onClickError(ClickEvent clickEvent) {

    }

    @Override
    protected boolean acceptResponse(String[] list) {
        idPackage = parseLong(list[1], ":");
        Long all = parseLong(list[2], ":");
        switchButtonState(idPackage != null, false);
        return idPackage != null;
    }

    @Override
    protected boolean onClickOK() throws Exception {
        params = idPackage;
        showConfirm("Вы уверены, что хотите открыть счета?", this.getDlgEvents(), params);
        return false;
    }


    @Override
    public void afterShow() {
        switchControlsState(true);
        errorButton.setVisible(false);

        showButton.setEnabled(false);
        deleteButton.setEnabled(false);
        ok.setEnabled(false);
        loadingResult.clear();
    }

}
