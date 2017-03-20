package ru.rbt.barsgl.gwt.client.Export;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.gwt.core.actions.Action;
import ru.rbt.barsgl.gwt.core.utils.DialogUtils;

/**
 * Created by akichigi on 20.03.17.
 */
public class ExportActionCallback implements AsyncCallback<String> {

    private final String localFileName;
    private Action rootAction;

    public ExportActionCallback(Action rootAction, String fileName) {
        this.rootAction = rootAction;
        this.localFileName = fileName;
    }

    @Override
    public void onFailure(Throwable throwable) {
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
}
