package ru.rbt.barsgl.gwt.client.operBackValue.dict;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.ui.DatePickerBox;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.security.gwt.client.CommonEntryPoint;

import java.util.Date;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

/**
 * Created by er17503 on 16.08.2017.
 */
public class BVPeriodClosingDlg extends DlgFrame {
    private DatePickerBox lastDate;
    private DatePickerBox closeDate;
    private FormAction action;

    @Override
    public Widget createContent(){
        Grid grid = new Grid(2, 2);

        grid.setText(0, 0, "Дата завершения отчетного периода");
        grid.setWidget(0, 1, lastDate = new DatePickerBox());
        lastDate.setWidth("100px");

        grid.setText(1, 0, "Дата закрытия отчетного периода");
        grid.setWidget(1, 1, closeDate = new DatePickerBox());
        closeDate.setWidth("100px");

        return grid;
    }

    @Override
    protected void fillContent() {
        ok.setText(action == FormAction.DELETE ? "Удалить" : "Сохранить");
        clearContent();
        setContent();
        setEnableState();
    }

    private void clearContent(){
        lastDate.clear();
        closeDate.clear();
    }

    private void setContent(){
        if (action == FormAction.CREATE) return;
        Object[] objs = (Object[]) params;
        lastDate.setValue((Date) objs[0]);
        closeDate.setValue((Date) objs[1]);
    }

    private void setEnableState(){
        lastDate.setEnabled(action == FormAction.CREATE);
        closeDate.setEnabled(action != FormAction.DELETE);
    }

    private void checkUp() throws Exception {
        if (!lastDate.hasValue()) throw new Exception("Не заполено поле 'Дата завершения отчетного периода'");
        if (!closeDate.hasValue()) throw new Exception("Не заполено поле 'Дата закрытия отчетного периода'");
        if (closeDate.getValue().compareTo(lastDate.getValue()) == -1  || closeDate.getValue().compareTo(CommonEntryPoint.CURRENT_OPER_DAY) == -1)
            throw new Exception("Дата закрытия отчетного периода должна быть больше даты завершения отчетного периода \nи не меньше текущего ОД");
    }

    @Override
    protected boolean onClickOK() throws Exception {
        try {
            checkUp();
            params = new Object[]{ lastDate.getValue(), closeDate.getValue() };
        } catch (Exception e) {
            showInfo("Ошибка", e.getMessage());
            return false;
        }
        return true;
    }

    public void setAction(FormAction action) {
        this.action = action;
    }
}
