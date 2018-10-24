package ru.rbt.barsgl.gwt.client.account;

import com.google.gwt.event.dom.client.ClickEvent;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.loadFile.LoadFileAnyDlg;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.server.upload.UploadFileType;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.operation.AccountBatchWrapper;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.shared.user.AppUserWrapper;

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
    protected boolean acceptResponse(String[] list) {
        return true;
    }

    @Override
    protected void onClickDelete(ClickEvent clickEvent) {
        AccountBatchWrapper wrapper = new AccountBatchWrapper();
        wrapper.setPackageId((Long) idPackage);
        wrapper.setAction(DELETE);

        AppUserWrapper appUserWrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
        wrapper.setUserId(appUserWrapper.getId());

        BarsGLEntryPoint.accountService.processAccountBatchRq(wrapper, new AuthCheckAsyncCallback<RpcRes_Base<AccountBatchWrapper>>() {
            @Override
            public void onSuccess(RpcRes_Base<AccountBatchWrapper> wrapper) {
                if (wrapper.isError()) {
                    showInfo("Ошибка", wrapper.getMessage());
                } else {
                    switchControlsState(true);
                    deleteButton.setEnabled(false);
                    showButton.setEnabled(false);
                    errorButton.setEnabled(false);
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

    }

    @Override
    protected void onClickError(ClickEvent clickEvent) {

    }

    @Override
    protected void switchControlsState(Boolean state) {

    }

    @Override
    protected void parseResponseBody(String[] list) {
        idPackage = parseLong(list[1], ":");
        Long all = parseLong(list[2], ":");
        boolean isError = false; // (null != err) && (err > 0); // TODO
        errorButton.setEnabled(isError);
        showButton.setEnabled(idPackage != null);
        deleteButton.setEnabled(idPackage != null);
        boolean isOk = !(idPackage == null || isError);
    }

    @Override
    protected boolean onClickOK() throws Exception {
        params = idPackage;
        return idPackage != null;
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