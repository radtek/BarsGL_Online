package ru.rbt.barsgl.gwt.client.events.ae;

/**
 * Created by ER18837 on 24.09.16.
 */
public class LoadFileDlg extends LoadFileDlgBase {
    public static final String TITLE = "Загрузка операций из Excel файла";
    public LoadFileDlg(){
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
        return "Batch";
    }

    @Override
    protected String getExampleName() {
        return "example.xlsx";
    }
}
