package ru.rbt.barsgl.gwt.client.pd;

import com.google.gwt.user.client.Timer;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.events.DataListBoxEvent;
import ru.rbt.barsgl.gwt.core.events.DataListBoxEventHandler;
import ru.rbt.barsgl.gwt.core.events.LocalEventBus;
import ru.rbt.barsgl.shared.dict.FormAction;

/**
 * Created by er23851 on 05.05.2017.
 */
public class PostingTechViewDlg extends PostingTechDlg {

    private int asyncListCount = 7;

    public PostingTechViewDlg(String title, FormAction action, Columns columns)
    {
        super(title,action,columns);
    }

}
