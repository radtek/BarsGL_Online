package ru.rbt.barsgl.gwt.client.events.ae;

import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterShowEvent;
import ru.rbt.barsgl.gwt.core.ui.AreaBox;
import ru.rbt.barsgl.gwt.core.ui.ValuesBox;

import java.util.List;

/**
 * Created by akichigi on 06.03.17.
 */
public class ErrorHandlingEditListDlg extends DlgFrame implements IAfterShowEvent {
    private ValuesBox comment;
    private List<Row> rows;
    private AreaBox commentBox;

    public ErrorHandlingEditListDlg(){
        super();
        setCaption("Редактирование списка сообщений");
        setAfterShowEvent(this);
    }


    @Override
    protected boolean onClickOK() throws Exception {
        return false;
    }

    @Override
    public void afterShow() {

    }
}
