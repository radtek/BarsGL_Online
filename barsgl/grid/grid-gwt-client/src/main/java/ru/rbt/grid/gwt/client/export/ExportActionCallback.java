package ru.rbt.grid.gwt.client.export;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import ru.rbt.barsgl.gwt.core.actions.Action;
import ru.rbt.barsgl.gwt.core.utils.DialogUtils;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;

/**
 * Created by akichigi on 20.03.17.
 */
public class ExportActionCallback extends AuthCheckAsyncCallback<String> {

    private final String localFileName;
    private Action rootAction;

    public ExportActionCallback(Action rootAction, String fileName) {
        this.rootAction = rootAction;
        this.localFileName = fileName;
    }

    @Override
    public void onFailureOthers(Throwable throwable) {
        DialogUtils.showInfo("Ошибка", throwable.getMessage());
        rootAction.setEnable(true);
    }

    @Override
    public void onSuccess(String fileName) {
        String url = GWT.getHostPageBaseURL()
                + "service/ExportFileHandler?filename=" + fileName + "&newfilename=" + localFileName + ".xlsx";
        Window.open(url, "_self", "disabled");

        rootAction.setEnable(true);
    }

    @Override
    public void afterReauthorize(Throwable afterException){
        rootAction.setEnable(true);
    }
}
