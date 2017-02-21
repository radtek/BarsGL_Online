package ru.rbt.barsgl.gwt.client.events.ae;

/**
 * Created by akichigi on 03.02.17.
 */
public class LoadFileFactory {
    public enum LoadType {FILE, CARD}
    public static LoadFileDlgBase create(LoadType loadType){
        switch (loadType){
            case FILE: return new LoadFileDlg();
            case CARD: return new LoadCardDlg();
            default:return null;
        }
    }
}
