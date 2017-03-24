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
import ru.rbt.barsgl.gwt.core.comp.Components;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.ifEmpty;

/**
 * Created by ER18837 on 24.05.16.
 */
public class AccountQuickFilterDlg extends DlgFrame {

    private final String hint = " может содержать начальные символы " +
            "\nи спецсимволы:" +
            "\n              '_'   (заменяет 1 любой символ)" +
            "\n              '%' (заменяет любое количество символов)";

    protected final String LABEL_WIDTH = "120px";
    protected final String FIELD_WIDTH = "70px";
    protected final String ACCOUNT_WIDTH = "145px";

    protected DataListBox mDealSource;
    protected TxtBox mDealId;
    protected DataListBoxEx mCurrency;
    protected DataListBoxEx mFilial;
    protected TxtBox mAccount;
    protected TxtBox mCustomerNumber;
    protected DatePickerBox mDateFrom;
    protected DatePickerBox mDateTo;

    protected AccountQuickFilterParams filterParams;

    private int asyncListCount = 3; /*count async lists: mDealSource; mCurrency; mFilial*/
    private HandlerRegistration registration;
    private Timer timer;


    public AccountQuickFilterDlg() {
        super();
        ok.setText(TEXT_CONSTANTS.applyBtn_caption());
        cancel.setText(TEXT_CONSTANTS.btn_cancel());
        setCaption("Выбор параметров счета");
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

        Grid grid = new Grid(8, 2);
        int row = 0;

        grid.setWidget(row, 0, Components.createLabel("Источник сделки", LABEL_WIDTH));
        grid.setWidget(row++, 1, mDealSource = createDealSourceListBox("", FIELD_WIDTH));

        grid.setWidget(row, 0, Components.createLabel("Номер сделки"));
        grid.setWidget(row++, 1, mDealId = Components.createTxtBox(20, ACCOUNT_WIDTH));

        grid.setWidget(row, 0, lCustomerNumber = Components.createLabel("Номер клиента"));
        grid.setWidget(row++, 1, mCustomerNumber = Components.createTxtBox(8, FIELD_WIDTH));
        lCustomerNumber.setTitle("Номер клиента" + hint);
        mCustomerNumber.setTitle("Номер клиента" + hint);

        grid.setWidget(row, 0, Components.createLabel("Филиал"));
        grid.setWidget(row++, 1, mFilial = createFilialAuthListBox("", FIELD_WIDTH, false, true));

        grid.setWidget(row, 0, Components.createLabel("Валюта", LABEL_WIDTH));
        grid.setWidget(row++, 1, mCurrency = createCurrencyListBox("", FIELD_WIDTH, false, true));

        grid.setWidget(row, 0, lAccount = Components.createLabel("Маска счета"));
        grid.setWidget(row++, 1, mAccount = Components.createTxtBox(20, ACCOUNT_WIDTH));
        lAccount.setTitle("Маска счета" + hint);
        mAccount.setTitle("Маска счета" + hint);

        grid.setWidget(row, 0, Components.createLabel("Дата открытия с"));
        grid.setWidget(row++, 1, mDateFrom = Components.createDateBox());
        grid.setWidget(row, 0, Components.createLabel("Дата открытия по"));
        grid.setWidget(row++, 1, mDateTo = Components.createDateBox());

        mainVP.add(grid);
        
        return mainVP;
    }

    protected void fillUp(){
        filterParams = (AccountQuickFilterParams)params;
        if (null != filterParams) {
            mDealSource.setSelectValue(ifEmpty(filterParams.getDealSource(), ""));
            mDealId.setValue(ifEmpty(filterParams.getDealId(), ""));
            mCurrency.setSelectValue(ifEmpty(filterParams.getCurrency(), ""));
            mFilial.setSelectValue(ifEmpty(filterParams.getFilial(), ""));
            mAccount.setValue(ifEmpty(filterParams.getAccount(), ""));
            mCustomerNumber.setValue(ifEmpty(filterParams.getCustomerNumber(), ""));
            mDateFrom.setValue(filterParams.getDateFrom());
            mDateTo.setValue(filterParams.getDateTo());
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
            filterParams.setDealSource((String)mDealSource.getValue());
            filterParams.setDealId(mDealId.getValue());
            filterParams.setFilial((String)mFilial.getValue());
            filterParams.setFilialN((String)mFilial.getParam("CBCCN"));
            filterParams.setCurrency((String)mCurrency.getValue());
            filterParams.setCurrencyN((String)mCurrency.getParam("CCYN"));
            filterParams.setAccount(mAccount.getValue());
            filterParams.setCustomerNumber(mCustomerNumber.getValue());
            filterParams.setDateFrom(mDateFrom.getValue());
            filterParams.setDateTo(mDateTo.getValue());
        }
        params = filterParams;
        return true;
    }

}
