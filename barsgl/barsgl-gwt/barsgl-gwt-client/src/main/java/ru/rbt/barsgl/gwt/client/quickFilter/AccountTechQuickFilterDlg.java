package ru.rbt.barsgl.gwt.client.quickFilter;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;
import ru.rbt.barsgl.gwt.client.comp.DataListBox;
import ru.rbt.barsgl.gwt.client.comp.DataListBoxEx;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.events.DataListBoxEvent;
import ru.rbt.barsgl.gwt.core.events.DataListBoxEventHandler;
import ru.rbt.barsgl.gwt.core.events.LocalEventBus;
import ru.rbt.barsgl.gwt.core.ui.DatePickerBox;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.*;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.ifEmpty;

/**
 * Created by ER18837 on 24.05.16.
 */
public class AccountTechQuickFilterDlg extends DlgFrame {

    private final String hint = " может содержать начальные символы " +
            "\nи спецсимволы:" +
            "\n              '_'   (заменяет 1 любой символ)" +
            "\n              '%' (заменяет любое количество символов)";

    protected final String LABEL_WIDTH = "120px";
    protected final String FIELD_WIDTH = "70px";
    protected final String ACCOUNT_WIDTH = "145px";

    protected TxtBox mAccType;
    protected DataListBoxEx mCurrency;
    protected DataListBoxEx mFilial;

    protected AccountTechQuickFilterParams filterParams;

    private int asyncListCount = 2; /*count async lists: mDealSource; mCurrency; mFilial*/
    private HandlerRegistration registration;
    private Timer timer;


    public AccountTechQuickFilterDlg() {
        super();
        ok.setText(TEXT_CONSTANTS.applyBtn_caption());
        cancel.setText(TEXT_CONSTANTS.btn_cancel());
        setCaption("Выбор параметров технического счета");
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
    public Widget createContent() {

        VerticalPanel mainVP = new VerticalPanel();
        Label lAccount, lCustomerNumber;

        Grid grid = new Grid(3, 2);
        int row = 0;

        grid.setWidget(row, 0, createLabel("Accounting Type", LABEL_WIDTH));
        grid.setWidget(row++, 1, mAccType = createTxtBox(20, FIELD_WIDTH));

        grid.setWidget(row, 0, createLabel("Филиал"));
        grid.setWidget(row++, 1, mFilial = createFilialAuthListBox("", FIELD_WIDTH, false, true));

        grid.setWidget(row, 0, createLabel("Валюта", LABEL_WIDTH));
        grid.setWidget(row++, 1, mCurrency = createCurrencyListBox("", FIELD_WIDTH, false, true));

        mainVP.add(grid);
        
        return mainVP;
    }

    protected void fillUp(){
        filterParams = (AccountTechQuickFilterParams)params;
        if (null != filterParams) {
            mCurrency.setSelectValue(ifEmpty(filterParams.getCurrency(), ""));
            mFilial.setSelectValue(ifEmpty(filterParams.getFilial(), ""));
            mAccType.setValue(filterParams.getAcctype());
        }
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

    @Override
    protected boolean onClickOK() throws Exception {
        if (null != filterParams) {
            filterParams.setFilial((String)mFilial.getValue());
            filterParams.setFilialN((String)mFilial.getParam("CBCCN"));
            filterParams.setCurrency((String)mCurrency.getValue());
            filterParams.setCurrencyN((String)mCurrency.getParam("CCYN"));
            filterParams.setAcctype(mAccType.getValue());
        }
        params = filterParams;
        return true;
    }

}
