package ru.rbt.barsgl.gwt.client.operBackValue;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.ui.DatePickerBox;

import java.util.Date;

/**
 * Created by er17503 on 02.08.2017.
 */
public class BVPostDateAuthListDlg extends DlgFrame {
    private DatePickerBox _postDate;

    public BVPostDateAuthListDlg(){
         ok.setText("Подтвердить");
         setCaption("Выбор даты проводки");
         dlg.setWidth("100px");
    }

    @Override
    public Widget createContent() {
        Grid grid = new Grid(1, 2);
        grid.setWidget(0, 0, new Label("Дата проводки"));
        grid.setWidget(0, 1, _postDate = new DatePickerBox());

        return grid;
    }

    @Override
    protected void fillContent(){
        _postDate.setValue((Date) params);
    }

    private boolean check(){
        return _postDate.hasValue();
    }

    @Override
    protected boolean onClickOK() throws Exception {
        params = _postDate.getValue();
        return check();
    }
}
