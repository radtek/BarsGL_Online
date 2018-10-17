package ru.rbt.barsgl.gwt.client.events.ae;

import ru.rbt.barsgl.gwt.server.upload.UploadFileType;

/**
 * Created by ER18837 on 24.09.16.
 */
public class LoadOperDlg extends LoadOperDlgBase {
    public static final String TITLE = "Загрузка операций из Excel файла";
    public LoadOperDlg(){
        super();
        setCaption(TITLE);
    }

    @Override
    protected String getFileUploadName() {
        return "excel-upload001";
    }

    @Override
    protected String getServletUploadName() {
        return "service/UploadFileHandler";
    }

    @Override
    protected boolean isExcludeVisible() {
        return true;
    }

    @Override
    protected String getUploadType() {
        return UploadFileType.Oper.name();
    }

    @Override
    protected String getExampleName() {
        return "example.xlsx";
    }
}
