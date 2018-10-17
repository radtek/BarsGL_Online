package ru.rbt.barsgl.gwt.client.account;

import com.google.gwt.event.dom.client.ClickEvent;
import ru.rbt.barsgl.gwt.client.loadFile.LoadFileAnyDlg;
import ru.rbt.barsgl.gwt.server.upload.UploadFileType;

/**
 * Created by er18837 on 17.10.2018.
 */
public class LoadAccountDlg extends LoadFileAnyDlg {
    public static final String TITLE = "Загрузка счетов из Excel файла";

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
        return false;
    }

    @Override
    protected void onClickDelete(ClickEvent clickEvent) {

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
    protected boolean onClickOK() throws Exception {
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
