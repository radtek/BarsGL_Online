package ru.rbt.barsgl.gwt.client.account;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;
import ru.rbt.barsgl.gwt.client.check.*;
import ru.rbt.barsgl.gwt.client.comp.DataListBox;
import ru.rbt.barsgl.gwt.client.comp.DataListBoxEx;
import ru.rbt.barsgl.gwt.client.dict.dlg.EditableDialog;
import ru.rbt.barsgl.gwt.client.dictionary.AccountTypeTechFormDlg;
import ru.rbt.barsgl.gwt.client.dictionary.CustomerFormDlg;
import ru.rbt.barsgl.gwt.client.gridForm.GridFormDlgBase;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.events.DataListBoxEvent;
import ru.rbt.barsgl.gwt.core.events.DataListBoxEventHandler;
import ru.rbt.barsgl.gwt.core.events.LocalEventBus;
import ru.rbt.barsgl.gwt.core.ui.AreaBox;
import ru.rbt.barsgl.gwt.core.ui.DatePickerBox;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.DealSource;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;
import ru.rbt.security.gwt.client.operday.IDataConsumer;

import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.*;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.check;
import static ru.rbt.security.gwt.client.operday.OperDayGetter.getOperday;

/**
 * Created by ER18837 on 14.03.16.
 */
public class AccountTechDlg extends EditableDialog<ManualAccountWrapper> {

    private final String BUTTON_WIDTH = "120px";
    private final String LABEL_WIDTH = "130px";
    private final String FIELD_WIDTH = "120px";
    private final String LABEL_WIDTH2 = "125px";
    private final String FIELD_WIDTH2 = "135px";
    private final String TEXT_WIDTH = "80px";
    private final String LONG_WIDTH = "390px";

    private DataListBoxEx mFilial;
    private DataListBoxEx mCurrency;
    private TxtBox mAccountType;
    private	DataListBox mDealSource;
    private DatePickerBox mDateOpen;
    private DatePickerBox mDateClose;
    private TxtBox mDateOperDay;
    private AreaBox mAccountDesc;
    private Button mAccountTypeButton;

    private String accountTypeDesc = null;

    private Date operday;
    private Long accountId;

    private int asyncListCount = 3; /*count async lists:  mBranch; mCurrency; mDealSource; mTerm*/
    private HandlerRegistration registration;
    private Timer timer;

    private static Logger rootLogger = Logger.getLogger("AccountTechDlg");

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

        Grid g0 = new Grid(2, 4);
        mainVP.add(g0);
        g0.setWidget(0, 0, createLabel("Филиал", LABEL_WIDTH));
        g0.setWidget(0, 1, mFilial = createFilialListBox("","250px"));// createBranchAuthListBox("", "250px", true));
        g0.setWidget(0, 2, createLabel("Валюта", "50px"));
        g0.setWidget(0, 3, mCurrency = createCurrencyListBox("RUR", "70px"));

        Grid g13 = new Grid(1, 2);
        mainVP.add(g13);
        g13.setWidget(0, 0, mAccountTypeButton = createAccountTypeButton("Accounting Type", BUTTON_WIDTH));
        g13.setWidget(0, 1, mAccountType = createTxtIntBox(9, TEXT_WIDTH));

        Grid g2 = new Grid(1, 2);
        mainVP.add(g2);
        g2.setWidget(0, 0, createLabel("Название счета", LABEL_WIDTH));
        g2.setWidget(0, 1, mAccountDesc = createAreaBox(LONG_WIDTH, "60px"));

        Grid g3 = new Grid(1, 2);
        mainVP.add(g3);
        g3.setWidget(0, 0, createLabel("Источник сделки", LABEL_WIDTH));
        g3.setWidget(0, 1, createAlignWidget(mDealSource = createDealSourceListBox("", TEXT_WIDTH), FIELD_WIDTH));

        Grid g4 = new Grid(3, 4);
        mainVP.add(g4);
        g4.setWidget(0, 0, createLabel("Дата открытия", LABEL_WIDTH));
        g4.setWidget(0, 1, createAlignWidget(mDateOpen = createDateBox(), FIELD_WIDTH));
        g4.setWidget(1, 0, createLabel("Дата закрытия", LABEL_WIDTH));
        g4.setWidget(1, 1, createAlignWidget(mDateClose = createDateBox(null), FIELD_WIDTH));

        g4.setWidget(2, 0, createLabel("Текущий опердень", LABEL_WIDTH2));
        g4.setWidget(2, 1, mDateOperDay = createTxtBox(10));

        setChangeHandlers();
        return mainVP;
    }

    @Override
    protected ManualAccountWrapper createWrapper() {
        return new ManualAccountWrapper();
    }

    @Override
    protected void setFields(ManualAccountWrapper account) {
    	account.setId(accountId);
        account.setFilial(check((String) mFilial.getParam("CBCCN")
                , "Филиал", "обязательно для заполнения", new CheckNotEmptyString()));
        account.setCurrency(check((String) mCurrency.getValue()
                , "Валюта", "обязательно для заполнения", new CheckNotEmptyString()));
        account.setAccountType(check(mAccountType.getValue()
                , "Accounting type", "обязательно для заполнения и должно содержать только цифры"
                , new CheckStringLength(1, 9), new ConvertStringToLong()));
        account.setDescription(check(mAccountDesc.getValue(),
                "Наименование счета", "обязательно для заполнения, не более 255 символов \n(Для разблокировки нажмите на кнопку 'Accounting Type')",
                new CheckStringLength(1, 255)));
//            account.setDealId(check(mDealId.getValue(),
//                    "Номер сделки", "обязательно для заполнения", new CheckNotEmptyString()));
        account.setDealSource(check((String) mDealSource.getValue(),
                "Источник сделки", "обязательно для заполнения", new CheckNotEmptyString()));

        ConvertDateToString convertDate = new ConvertDateToString(ManualAccountWrapper.dateFormat);
        account.setDateOpenStr(check(mDateOpen.getValue()
                , "Дата открытия", "обязательно для заполнения"
                , new CheckNotNullDate(), convertDate));
        account.setDateCloseStr(convertDate.apply(mDateClose.getValue()));
    }

    @Override
    public void clearContent() {
        mFilial.setValue(null);
        mFilial.setEnabled(true);
        mCurrency.setValue("RUR");
        mCurrency.setEnabled(true);
        mAccountType.setValue(null);
        mAccountDesc.setValue(null);
        mDealSource.setValue(null);
        mDateOpen.setValue(null);
        mDateClose.setValue(null);
    }

    protected void fillUp(){

        if (action == FormAction.CREATE)
        {
            mDateOpen.setValue(null);
        }

        if (action == FormAction.UPDATE) {
            row = (Row) params;

            accountId = getFieldValue("ID");
            mFilial.setValue(getFieldValue("CBCC"));
            mCurrency.setValue(getFieldValue("CCY"));
            mAccountType.setValue(getFieldText("ACCTYPE"));

            mAccountDesc.setValue(getFieldText("DESCRIPTION"));
            mDealSource.setValue(getFieldValue("DEALSRS").toString());
            //mDealSource.setValue("K+TP");
            mDateOpen.setValueSrv((Date)getFieldValue("DTO"));
            mDateClose.setValueSrv((Date)getFieldValue("DTC"));
        }
        getOperday(new IDataConsumer<OperDayWrapper>() {
            @Override
            public void accept(OperDayWrapper wrapper) {
                setOperday(wrapper.getCurrentOD());
            }
        });
    }

    @Override
    protected void fillContent() {
        clearContent();
        setControlsEnabled();

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

            timer.scheduleRepeating(800);
        }
    }

    private void setControlsEnabled(){
        mFilial.setEnabled(action == FormAction.CREATE);
        mCurrency.setEnabled(action == FormAction.CREATE);

        mAccountTypeButton.setEnabled(action == FormAction.CREATE);
        mAccountType.setEnabled(true);
        mAccountDesc.setEnabled(action == FormAction.UPDATE);
        mDealSource.setEnabled(action == FormAction.CREATE);

        mDateOpen.setEnabled(true);
        mDateClose.setEnabled(action == FormAction.UPDATE);
        mDateOperDay.setEnabled(false);
    }

    private void setOperday(final String operDayStr) {
        operday = DateTimeFormat.getFormat(ManualAccountWrapper.dateFormat).parse(operDayStr);
        rootLogger.info("OperDay = "+operday);
        mDateOperDay.setValue(operDayStr);
        if (null == mDateOpen.getValue())
            mDateOpen.setValue(operday);
    }

    private String getCustomerTypeName(HashMap<String, Object> result) {
        if (null == result.get("CTYPE"))
            return "";
        String custType = result.get("CTYPE").toString();
        String ctypeName = (String)result.get("CTYPENAME");;
        return custType + ": " + ctypeName;
    }

    private Button createAccountTypeButton(String text, String width) {
        Button btn = new Button();
        btn.setText(text);
        btn.addStyleName("dlg-button");
        btn.setWidth(width);
        btn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                try {
                    GridFormDlgBase dlg = new AccountTypeTechFormDlg() {

                        @Override
                        protected boolean getEditMode() {
                            return true;
                        }

                        @Override
                        protected Object[] getInitialFilterParams() {
                            return new Object[] {mAccountType.getValue()};
                        }

                        @Override
                        protected boolean setResultList(HashMap<String, Object> result) {
                            String acctype = result.get("ACCTYPE").toString();
                            mAccountType.setValue(acctype);
                            accountTypeDesc = (String)result.get("ACTYP_NAME");
                            mAccountDesc.setValue(accountTypeDesc);
                            mAccountDesc.setEnabled(true);
                            return true;
                        }
                    };
                    dlg.setModal(true);
                    dlg.show();
                } catch (Exception e) {
                    Window.alert(e.getMessage());
                }
            }
        });
        return btn;
    }

    private Button createCustomerButton(String text, String width) {
        Button btn = new Button();
        btn.setText(text);
        btn.addStyleName("dlg-button");
        btn.setWidth(width);
        btn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                try {
                    GridFormDlgBase dlg = new CustomerFormDlg() {
                        @Override
                        protected Object[] getInitialFilterParams() {
                            return new Object[] {null};
                        }

                        @Override
                        protected boolean setResultList(HashMap<String, Object> result) {
                            if (null != result) {
                                clearAccountType();
                            }
                            return true;
                        }
                    };
                    dlg.setModal(true);
                    dlg.show();
                } catch (Exception e) {
                    Window.alert(e.getMessage());
                }
            }
        });
        return btn;
    }

    private void setChangeHandlers() {
        mDealSource.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent changeEvent) {
                boolean srcKP = mDealSource.getText().equals(DealSource.KondorPlus.getLabel());
            }
        });

        mAccountType.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent changeEvent) {
                mAccountDesc.clear();
                mAccountDesc.setEnabled(false);
            }
        });
    }

    void clearAccountType() {
        mAccountType.clear();
        mAccountDesc.clear();
        mAccountDesc.setEnabled(false);
    }
}

