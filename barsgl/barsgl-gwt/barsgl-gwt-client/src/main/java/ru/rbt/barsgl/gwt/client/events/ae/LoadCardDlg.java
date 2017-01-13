package ru.rbt.barsgl.gwt.client.events.ae;

/**
 * Created by ER18837 on 24.09.16.
 */
public class LoadCardDlg extends LoadFileDlgBase {

    public LoadCardDlg() {
        super("Загрузка картотеки из Excel файла");
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
        return "Card";
    }

    @Override
    protected String getExampleName() {
        return "card.xlsx";
    }

}
