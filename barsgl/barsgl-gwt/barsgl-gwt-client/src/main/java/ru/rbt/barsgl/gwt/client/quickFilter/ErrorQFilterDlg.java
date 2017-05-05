package ru.rbt.barsgl.gwt.client.quickFilter;

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

import static ru.rbt.barsgl.gwt.core.comp.Components.createDateBox;
import static ru.rbt.barsgl.gwt.client.comp.GLComponents.createDealSourceListBox;
import static ru.rbt.barsgl.gwt.core.comp.Components.createLabel;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.ifEmpty;

/**
 * Created by akichigi on 21.02.17.
 */
public class ErrorQFilterDlg extends DlgFrame {
    private ErrorQFilterParams filterParams;
    private DataListBox dealSource;
    private DatePickerBox operDay;

    private int asyncListCount = 1; /*count async lists: dealSource*/
    private HandlerRegistration registration;
    private Timer timer;

    public ErrorQFilterDlg(){
        super();
        ok.setText(TEXT_CONSTANTS.applyBtn_caption());
        cancel.setText(TEXT_CONSTANTS.btn_cancel());
        setCaption("Выбор параметров обработки ошибок");
    }

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
                }
            }
        };
    }

    @Override
    public Widget createContent(){
        Grid grid = new Grid(2, 2);
        grid.setWidget(0, 0, createLabel("Источник сделки", "120px"));
        grid.setWidget(0, 1, dealSource = createDealSourceListBox("", "105px"));
        grid.setWidget(1, 0, createLabel("Дата опердня"));
        grid.setWidget(1, 1, operDay = createDateBox());

        return grid;
    }

    protected void fillUp(){
        filterParams = (ErrorQFilterParams) params;
        if (null != filterParams){
            dealSource.setSelectValue(ifEmpty(filterParams.getDealSource(), ""));
            operDay.setValue(filterParams.getOperDay());
        }
    }

    @Override
    protected void fillContent(){
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

    @Override
    protected boolean onClickOK() throws Exception {
        if (null != filterParams) {
            filterParams.setDealSource((String) dealSource.getValue());
            filterParams.setOperDay(operDay.getValue());
        }
        params = filterParams;
        return true;
    }
}
