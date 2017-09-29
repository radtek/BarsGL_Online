package ru.rbt.barsgl.gwt.client.operBackValue.dict;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;
import ru.rbt.barsgl.gwt.client.comp.DataListBox;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.events.DataListBoxEvent;
import ru.rbt.barsgl.gwt.core.events.DataListBoxEventHandler;
import ru.rbt.barsgl.gwt.core.events.LocalEventBus;
import ru.rbt.barsgl.gwt.core.ui.DatePickerBox;
import ru.rbt.barsgl.gwt.core.ui.IntBox;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.DealSource;
import ru.rbt.security.gwt.client.CommonEntryPoint;

import java.util.Date;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.createDealSourceListBox;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

/**
 * Created by er17503 on 16.08.2017.
 */
public class BVDepthDlg extends DlgFrame {
    private DataListBox dealSource;
    private IntBox depth;
    private DatePickerBox beginDate;
    private DatePickerBox endDate;

    private FormAction action;

    private int asyncListCount = 1; /* dealSource */
    private HandlerRegistration registration;
    private Timer timer;

    @Override
    public void beforeCreateContent() {
        registration =  LocalEventBus.addHandler(DataListBoxEvent.TYPE, dataListBoxCreatedEventHandler());
    }

    private DataListBoxEventHandler dataListBoxCreatedEventHandler() {
        return new DataListBoxEventHandler(){

            @Override
            public void completeLoadData(String dataListBoxId) {
                asyncListCount--;

                if (asyncListCount == 0) {
                    registration.removeHandler();
                    deleteKplusTP();
                }
            }
        };
    }

    private void deleteKplusTP(){
        dealSource.removeItem(DealSource.KondorPlus.getLabel());
    }

    @Override
    public Widget createContent(){
        Grid grid = new Grid(4, 2);

        grid.setText(0, 0, "Система источник");
        grid.setWidget(0, 1, dealSource = createDealSourceListBox("", "100px"));

        grid.setText(1, 0, "Глубина backvalue");
        grid.setWidget(1, 1, depth = new IntBox());
        depth.setWidth("100px");

        grid.setText(2, 0, "Дата начала");
        grid.setWidget(2, 1, beginDate = new DatePickerBox());
        beginDate.setWidth("100px");

        grid.setText(3, 0, "Дата окончания");
        grid.setWidget(3, 1, endDate = new DatePickerBox());
        endDate.setWidth("100px");

        return grid;
    }

    @Override
    protected void fillContent() {
        if (asyncListCount == 0) {
            //если закончена обработка списков
            fillUp();
        } else {
            showPreload(true);
            timer = new Timer() {
                @Override
                public void run() {
                    if (asyncListCount == 0) {
                        timer.cancel();
                        fillUp();
                        showPreload(false);
                    }
                }
            };

            timer.scheduleRepeating(500);
        }
    }

    private void fillUp(){
        ok.setText(action == FormAction.DELETE ? "Удалить" : "Сохранить");
        clearContent();
        setContent();
        setEnableState();
    }

    private void clearContent(){
        dealSource.setValue(null);
        depth.clear();
        beginDate.clear();
        endDate.clear();
    }

    private void setContent(){
        if (action == FormAction.CREATE){
//            depth.setValue(1);
            beginDate.setValue(CommonEntryPoint.CURRENT_OPER_DAY);
            return;
        }

        Object[] objs = (Object[]) params;
        dealSource.setValue((String)objs[0]);
        depth.setValue((Integer) objs[1]);
        beginDate.setValue((Date) objs[2]);
        endDate.setValue((Date) objs[3]);
    }

    private void setEnableState(){
        dealSource.setEnabled(action == FormAction.CREATE);
        depth.setEnabled(action != FormAction.DELETE && !(action == FormAction.UPDATE && beginDate.getValue().compareTo(CommonEntryPoint.CURRENT_OPER_DAY) == -1));
        beginDate.setEnabled(action == FormAction.CREATE);
        endDate.setEnabled(action != FormAction.DELETE);
    }

    private void checkUp() throws Exception {
        if (dealSource.getValue() == null) throw new Exception("Не заполено поле 'Система источник'");
        if (!depth.hasValue() || depth.getValue() < 2) throw new Exception("Значение поля 'Глубина backvalue' должно быть больше 1");
        if (!beginDate.hasValue()) throw new Exception("Не заполено поле 'Дата начала'");
        if (beginDate.getValue().compareTo(CommonEntryPoint.CURRENT_OPER_DAY) == -1) throw new Exception("Дата начала не должна быть меньше текущего ОД");
        if (endDate.hasValue() && endDate.getValue().compareTo(beginDate.getValue()) == -1) throw new Exception("Дата окончания должна быть больше даты начала");
    }

    @Override
    protected boolean onClickOK() throws Exception {
        try {
            checkUp();
            params = new Object[]{dealSource.getValue(), depth.getValue(), beginDate.getValue(), endDate.getValue() };
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
