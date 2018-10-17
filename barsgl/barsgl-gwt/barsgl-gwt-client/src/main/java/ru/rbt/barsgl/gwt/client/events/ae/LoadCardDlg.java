package ru.rbt.barsgl.gwt.client.events.ae;

import ru.rbt.barsgl.gwt.server.upload.UploadFileType;

/**
 * Created by ER18837 on 24.09.16.
 */
public class LoadCardDlg extends LoadOperDlgBase {
    public static final String TITLE = "Загрузка картотеки из Excel файла";
    public LoadCardDlg() {
        super();
        setCaption(TITLE);
    }

    @Override
    protected String getFileUploadName() {
        return "excel-upload002";
    }

    @Override
    protected String getServletUploadName() {
        return "service/UploadFileHandler";
    }

    @Override
    protected boolean isExcludeVisible() {
        return false;
    }

    @Override
    protected String getUploadType() {
        return UploadFileType.Card.name();
    }

    @Override
    protected String getExampleName() {
        return "card.xlsx";
    }

}
