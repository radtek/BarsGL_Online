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
import ru.rbt.barsgl.gwt.client.dictionary.AccountTypeFormDlg;
import ru.rbt.barsgl.gwt.client.dictionary.CustomerFormDlg;
import ru.rbt.barsgl.gwt.client.gridForm.GridFormDlgBase;
import ru.rbt.security.gwt.client.operday.IDataConsumer;
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

import java.util.Date;
import java.util.HashMap;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.*;
import ru.rbt.barsgl.gwt.core.comp.Components;
import static ru.rbt.security.gwt.client.operday.OperDayGetter.getOperday;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.*;

/**
 * Created by ER18837 on 14.03.16.
 */
public class AccountDlg extends EditableDialog<ManualAccountWrapper> {

    private final String BUTTON_WIDTH = "120px";
    private final String LABEL_WIDTH = "130px";
    private final String FIELD_WIDTH = "120px";
    private final String LABEL_WIDTH2 = "125px";
    private final String FIELD_WIDTH2 = "135px";
    private final String TEXT_WIDTH = "80px";
    private final String LONG_WIDTH = "390px";

    private DataListBoxEx mBranch;
    private DataListBoxEx mCurrency;
    private TxtBox mAccountType;
    private	TxtBox mSQ;
    private	TxtBox mCustomerNumber;
    private	DataListBox mTerm;
    private	TxtBox mDealId;
    private	TxtBox mSubdealId;
    private	DataListBox mDealSource;
    private DatePickerBox mDateOpen;
    private DatePickerBox mDateClose;
    private TxtBox mDateOperDay;
    private AreaBox mAccountDesc;
    private	TxtBox mCustomerName;
    private	TxtBox mCustomerType;

    private Button mCustomerButton;
    private Button mAccountTypeButton;

    private String accountTypeDesc = null;

    private Date operday;
    private Long accountId;
    private String bsaAcid;
    private String acc2;

    private int asyncListCount = 4; /*count async lists:  mBranch; mCurrency; mDealSource; mTerm*/
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
                }
            }
        };
    }

    @Override
    public Widget createContent() {
        VerticalPanel mainVP = new VerticalPanel();

        Grid g0 = new Grid(2, 4);
        mainVP.add(g0);
        g0.setWidget(0, 0, Components.createLabel("Отделение", LABEL_WIDTH));
        g0.setWidget(0, 1, mBranch = createBranchAuthListBox("", "250px", true));
        g0.setWidget(0, 2, Components.createLabel("Валюта", "50px"));
        g0.setWidget(0, 3, mCurrency = createCurrencyListBox("RUR", "70px"));

        Grid g11 = new Grid(1, 4);
        mainVP.add(g11);
        g11.setWidget(0, 0, mCustomerButton = createCustomerButton("Клиент", BUTTON_WIDTH));
        g11.setWidget(0, 1, mCustomerNumber = Components.createTxtIntBox(8, TEXT_WIDTH));
        g11.setWidget(0, 2, Components.createLabel("Тип собств", "75px"));
        g11.setWidget(0, 3, mCustomerType = Components.createTxtBox(70, "220px"));

        Grid g12 = new Grid(1, 2);
        mainVP.add(g12);
        g12.setWidget(0, 0, Components.createLabel("", LABEL_WIDTH));
        g12.setWidget(0, 1, mCustomerName = Components.createTxtBox(200, LONG_WIDTH));

        Grid g13 = new Grid(2, 5);
        mainVP.add(g13);
        g13.setWidget(0, 2, Components.createLabel("Код срока", "75px"));
        g13.setWidget(0, 3, mTerm = createTermListBox("00", "220px", false));
        g13.setWidget(1, 0, mAccountTypeButton = createAccountTypeButton("Accounting Type", BUTTON_WIDTH));
        g13.setWidget(1, 1, mAccountType = Components.createTxtIntBox(9, TEXT_WIDTH));

        Grid g2 = new Grid(2, 2);
        mainVP.add(g2);
        g2.setWidget(0, 0, Components.createLabel("Название счета", LABEL_WIDTH));
        g2.setWidget(0, 1, mAccountDesc = Components.createAreaBox(LONG_WIDTH, "60px"));

        Grid g3 = new Grid(3, 4);
        mainVP.add(g3);
        g3.setWidget(0, 0, Components.createLabel("Источник сделки", LABEL_WIDTH));
        g3.setWidget(0, 1, Components.createAlignWidget(mDealSource = createDealSourceAuthListBox("", TEXT_WIDTH), FIELD_WIDTH));
        g3.setWidget(1, 0, Components.createLabel("SQ Midas", LABEL_WIDTH));
        g3.setWidget(1, 1, mSQ = Components.createTxtIntBox(2, TEXT_WIDTH));
        g3.setWidget(0, 2, Components.createLabel("N сделки/платежа", LABEL_WIDTH2));
        g3.setWidget(0, 3, mDealId = Components.createTxtBox(20, FIELD_WIDTH2));
        g3.setWidget(1, 2, Components.createLabel("N субсделки"));
        g3.setWidget(1, 3, mSubdealId = Components.createTxtBox(20, FIELD_WIDTH2));

        Grid g4 = new Grid(2, 4);
        mainVP.add(g4);
        g4.setWidget(0, 0, Components.createLabel("Дата открытия", LABEL_WIDTH));
        g4.setWidget(0, 1, Components.createAlignWidget(mDateOpen = Components.createDateBox(), FIELD_WIDTH));
        g4.setWidget(1, 0, Components.createLabel("Дата закрытия", LABEL_WIDTH));
        g4.setWidget(1, 1, Components.createAlignWidget(mDateClose = Components.createDateBox(null), FIELD_WIDTH));

        g4.setWidget(0, 2, Components.createLabel("Текущий опердень", LABEL_WIDTH2));
        g4.setWidget(0, 3, mDateOperDay = Components.createTxtBox(10));

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
        account.setBsaAcid(bsaAcid);
        account.setBalanceAccount2(acc2);
        account.setBranch(check((String) mBranch.getValue()
                , "Отделение", "обязательно для заполнения", new CheckNotEmptyString()));
        account.setCurrency(check((String) mCurrency.getValue()
                , "Валюта", "обязательно для заполнения", new CheckNotEmptyString()));
        account.setAccountType(check(mAccountType.getValue()
                , "Accounting type", "обязательно для заполнения и должно содержать только цифры"
                , new CheckNotNullLong(), new ConvertStringToLong()));
        account.setCustomerNumber(check(mCustomerNumber.getValue(),
                "Клиент", "обязательно для заполнения", new CheckNotEmptyString()));
        account.setDescription(check(mAccountDesc.getValue(),
                "Наименование счета", "обязательно для заполнения, не более 255 символов \n(Для разблокировки нажмите на кнопку 'Accounting Type')",
                new CheckStringLength(1, 255)));
//            account.setDealId(check(mDealId.getValue(),
//                    "Номер сделки", "обязательно для заполнения", new CheckNotEmptyString()));
        account.setDealId(mDealId.getValue());
        account.setSubDealId(mSubdealId.getValue());
        account.setDealSource(check((String) mDealSource.getValue(),
                "Источник сделки", "обязательно для заполнения", new CheckNotEmptyString()));
        String term = (String)mTerm.getValue();
        account.setTerm(null != term && !term.trim().isEmpty() ? Short.parseShort(term) : null);
        String sq = mSQ.getValue();
        account.setAccountSequence(null != sq && !sq.trim().isEmpty() ? Short.parseShort(sq) : null);

        ConvertDateToString convertDate = new ConvertDateToString(ManualAccountWrapper.dateFormat);
        account.setDateOpenStr(check(mDateOpen.getValue()
                , "Дата открытия", "обязательно для заполнения"
                , new CheckNotNullDate(), convertDate));
        account.setDateCloseStr(convertDate.apply(mDateClose.getValue()));
    }

    @Override
    public void clearContent() {
        accountId = null;
        bsaAcid = null;
        acc2 = null;
        mBranch.setValue(null);
        mBranch.setEnabled(true);
        mCurrency.setValue("RUR");
        mCurrency.setEnabled(true);
        mAccountType.setValue(null);
        mCustomerNumber.setValue(null);
        mCustomerType.setValue(null);
        mCustomerName.setValue(null);
        mAccountDesc.setValue(null);
        mDealId.setValue(null);
        mSubdealId.setValue(null);
        mDealSource.setValue(null);
        mTerm.setValue("00");
        mSQ.setValue(null);    // TODO SQ
        mDateOpen.setValue(null);
        mDateClose.setValue(null);
    }

    protected void fillUp(){
        if (action == FormAction.UPDATE) {
            row = (Row) params;

            accountId = getFieldValue("ID");
            bsaAcid = getFieldValue("BSAACID");
            acc2 = getFieldValue("ACC2");
            mBranch.setValue(getFieldValue("BRANCH"));
            mCurrency.setValue(getFieldValue("CCY"));
            mAccountType.setValue(getFieldText("ACCTYPE"));
            mCustomerNumber.setValue(getFieldText("CUSTNO"));
            mCustomerType.setValue(getFieldText("CBCUSTTYPE"));
            mCustomerName.setValue(null);

            mAccountDesc.setValue(getFieldText("DESCRIPTION"));
            mDealId.setValue(getFieldText("DEALID"));
            mSubdealId.setValue(getFieldText("SUBDEALID"));
            mDealSource.setValue(getFieldValue("DEALSRS"));
            String term = "00" + getFieldText("TERM");
            mTerm.setValue(term.substring(term.length()-2, term.length()));
            mSQ.setValue(getFieldText("SQ"));
            mDateOpen.setValueSrv((Date)getFieldValue("DTO"));
            mDateClose.setValueSrv((Date)getFieldValue("DTC"));

            boolean isPlAccount = !isEmpty((String)getFieldValue("PLCODE"));
            if (isPlAccount)
                setControlsDisabled();
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

            timer.scheduleRepeating(500);
        }
    }

    private void setControlsEnabled(){
        mBranch.setEnabled(action == FormAction.CREATE);
        mCurrency.setEnabled(action == FormAction.CREATE);

        mCustomerButton.setEnabled(action == FormAction.CREATE);
        mCustomerNumber.setEnabled(action == FormAction.CREATE);
        mCustomerType.setEnabled(false);
        mCustomerName.setEnabled(false);
        mTerm.setEnabled(action == FormAction.CREATE);

        mAccountTypeButton.setEnabled(action == FormAction.CREATE);
        mAccountType.setEnabled(false);
        mAccountDesc.setEnabled(action == FormAction.UPDATE);

        mDealSource.setEnabled(action == FormAction.CREATE);
        mDealId.setEnabled(true);
        mSubdealId.setEnabled(true);
        mSQ.setEnabled(false);

        mDateOpen.setEnabled(true);
        mDateClose.setEnabled(action == FormAction.UPDATE);
        mDateOperDay.setEnabled(false);

        ok.setEnabled(true);
    }

    private void setControlsDisabled(){
        mBranch.setEnabled(false);
        mCurrency.setEnabled(false);

        mCustomerButton.setEnabled(false);
        mCustomerNumber.setEnabled(false);
        mCustomerType.setEnabled(false);
        mCustomerName.setEnabled(false);
        mTerm.setEnabled(false);

        mAccountTypeButton.setEnabled(false);
        mAccountType.setEnabled(false);
        mAccountDesc.setEnabled(false);

        mDealSource.setEnabled(false);
        mDealId.setEnabled(false);
        mSubdealId.setEnabled(false);
        mSQ.setEnabled(false);

        mDateOpen.setEnabled(false);
        mDateClose.setEnabled(false);
        mDateOperDay.setEnabled(false);

        ok.setEnabled(false);
    }

    private void setOperday(final String operDayStr) {
        operday = DateTimeFormat.getFormat(ManualAccountWrapper.dateFormat).parse(operDayStr);
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
                    final String custNo = mCustomerNumber.getValue();
                    if (isEmpty(custNo) || custNo.length() < 8) {
                        showInfo("Предупреждение", "Необходимо задать номер клиента (8 символов)");
                        return;
                    }
                    GridFormDlgBase dlg = new AccountTypeFormDlg() {

                        @Override
                        protected Object[] getInitialFilterParams() {
                            return new Object[] {operday, mAccountType.getValue(), mTerm.getValue(), custNo};
                        }

                        @Override
                        protected boolean setResultList(HashMap<String, Object> result) {
                            String acctype = result.get("ACCTYPE").toString();
                            Date dateFrom = (Date)result.get("DTB");
                            Date dateTo = (Date)result.get("DTE");

                            Date dateOpen = (null != mDateOpen.getValue()) ? mDateOpen.getValue() : operday;
                            if (dateOpen.before(dateFrom) || (null != dateTo && dateOpen.after(dateTo))) {
                                showInfo("Ошибка", "Accounting Type " + acctype + " недействителен на дату " + mDateOpen.getText());
                                return false;
                            }
                            mAccountType.setValue(acctype);
                            mTerm.setValue((String)result.get("TERM"));
                            accountTypeDesc = (String)result.get("ACCNAME");
                            mAccountDesc.setValue(accountTypeDesc);
                            mAccountDesc.setEnabled(true);
                            mCustomerType.setValue(getCustomerTypeName(result));
                            mCustomerName.setValue((String)result.get("CUSTNAME"));
                            acc2 = (String)result.get("ACC2");
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
                            return new Object[] {mCustomerNumber.getValue()};
                        }

                        @Override
                        protected boolean setResultList(HashMap<String, Object> result) {
                            if (null != result) {
                                mCustomerNumber.setValue((String)result.get("CUSTNO"));
                                mCustomerName.setValue((String)result.get("CUSTNAME"));
                                mCustomerType.setValue(getCustomerTypeName(result));
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
                mSQ.setEnabled(!srcKP);
                if (srcKP)
                    mSQ.clear();
            }
        });

        mCustomerNumber.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent changeEvent) {
                mCustomerName.clear();
                mCustomerType.clear();
                clearAccountType();
            }
        });

        mAccountType.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent changeEvent) {
                mAccountDesc.clear();
                mAccountDesc.setEnabled(false);
            }
        });

        mTerm.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent changeEvent) {
                clearAccountType();
            }
        });
    }

    void clearAccountType() {
        boolean isCustomerNumber = (mCustomerNumber.getText().length() == 8);
        mAccountType.clear();
        mAccountDesc.clear();
        mAccountDesc.setEnabled(false);
        mAccountType.setEnabled(isCustomerNumber);
    }
}

