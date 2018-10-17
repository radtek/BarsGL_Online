package ru.rbt.barsgl.gwt.client.loadFile;

import ru.rbt.barsgl.gwt.client.events.ae.LoadCardDlg;
import ru.rbt.barsgl.gwt.client.events.ae.LoadOperDlg;
import ru.rbt.barsgl.gwt.client.events.ae.LoadOperDlgBase;
import ru.rbt.barsgl.gwt.server.upload.UploadFileType;

/**
 * Created by akichigi on 03.02.17.
 */
public class LoadFileFactory {
    public static LoadOperDlgBase create(UploadFileType loadType){
        switch (loadType){
            case Oper: return new LoadOperDlg();
            case Card: return new LoadCardDlg();
//            case Account: return new LoadAccountDlg();
            default:return null;
        }
    }
}
