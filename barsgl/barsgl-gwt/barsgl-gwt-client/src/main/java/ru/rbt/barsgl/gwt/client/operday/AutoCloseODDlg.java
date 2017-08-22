package ru.rbt.barsgl.gwt.client.operday;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.ui.DatePickerBox;
import ru.rbt.barsgl.gwt.core.ui.TimeBox;
import ru.rbt.barsgl.shared.ClientDateUtils;
import ru.rbt.barsgl.shared.operday.LwdBalanceCutWrapper;

import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;


/**
 * Created by er17503 on 18.08.2017.
 */
public class AutoCloseODDlg extends DlgFrame {
    private DatePickerBox date;
    private TimeBox time;
    private LwdBalanceCutWrapper wrapper;

    @Override
    public Widget createContent(){
        Grid grid = new Grid(2, 2);

        grid.setText(0, 0, "Дата опердня");
        grid.setWidget(0, 1, date = new DatePickerBox());
        date.setWidth("100px");

        grid.setText(1, 0, "Время закрытия");
        grid.setWidget(1, 1, time = new TimeBox());

        return grid;
    }


    @Override
    protected void fillContent() {
        clearContent();
        wrapper = (LwdBalanceCutWrapper) params;
        if (wrapper != null){
            date.setValue(ClientDateUtils.String2Date(wrapper.getRunDateStr()));
            time.setTime(wrapper.getCutTimeStr());
        }
    }

    private void clearContent(){
        date.clear();
        time.clear();
    }

    private void checkUp() throws Exception{
        if (!date.hasValue()) throw new Exception("Не заполено поле 'Дата опердня'");
    }

    @Override
    protected boolean onClickOK() throws Exception {
        try {
            checkUp();
            wrapper = new LwdBalanceCutWrapper();
            wrapper.setRunDateStr(ClientDateUtils.Date2String(date.getValue()));
            wrapper.setCutTimeStr(time.toString());
            params =  wrapper;
        } catch (Exception e) {
            showInfo("Ошибка", e.getMessage());
            return false;
        }
        return true;
    }
}
