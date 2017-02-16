package ru.rbt.barsgl.gwt.client.accountPl;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.client.check.*;
import ru.rbt.barsgl.gwt.client.comp.DataListBox;
import ru.rbt.barsgl.gwt.client.comp.DataListBoxEx;
import ru.rbt.barsgl.gwt.client.compLookup.LookUpAcc2;
import ru.rbt.barsgl.gwt.client.compLookup.LookUpPlcode;
import ru.rbt.barsgl.gwt.client.compLookup.LookupBoxBase;
import ru.rbt.barsgl.gwt.client.dict.dlg.EditableDialog;
import ru.rbt.barsgl.gwt.client.dictionary.AccountTypePlFormDlg;
import ru.rbt.barsgl.gwt.client.gridForm.GridFormDlgBase;
import ru.rbt.barsgl.gwt.client.operday.IDataConsumer;
import ru.rbt.barsgl.gwt.core.datafields.Row;
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
import static ru.rbt.barsgl.gwt.client.operday.OperDayGetter.getOperday;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.*;


/**
 * Created by ER18837 on 28.11.16.
 */
public class PlAccountDlg extends EditableDialog<ManualAccountWrapper> {

    private final String BUTTON_WIDTH = "120px";
    private final String LABEL_WIDTH = "130px";
    private final String LIST_WIDTH = "290px";
    private final String TEXT_WIDTH = "80px";
    private final String FIELD_WIDTH = "120px";
    private final String LABEL_WIDTH2 = "180px";
    private final String FIELD_WIDTH2 = "170px";
    private final String LONG_WIDTH = "390px";

    private DataListBoxEx mBranch;
    private	DataListBox mCustomerType;
    private	DataListBox mTerm;
    private LookupBoxBase mPlcode;
    private LookupBoxBase mAcc2;
    private TxtBox mAccountType;
    private DatePickerBox mDateOpen;
    private DatePickerBox mDateClose;
    private TxtBox mDateOperDay;
    private AreaBox mAccountDesc;

    private Button mAccountTypeButton;

    private String accountTypeDesc = null;
    private String acod = null;
    private String sq = null;

    private Date operday;
    private Long accountId;
    private String bsaAcid;

    private boolean fl707;
    private String trueAcc2 = "";
    

    @Override
    public Widget createContent() {
        VerticalPanel mainVP = new VerticalPanel();

        Grid g0 = new Grid(3, 3);
        mainVP.add(g0);
        g0.setWidget(0, 0, createLabel("Отделение", LABEL_WIDTH));
        g0.setWidget(0, 1, mBranch = createBranchAuthListBox("", LIST_WIDTH, true));
        g0.setWidget(1, 0, createLabel("Тип собственности", LABEL_WIDTH));
        g0.setWidget(1, 1, mCustomerType = createCustTypeListBox("", LIST_WIDTH, true));
        g0.setWidget(2, 0, createLabel("Код срока", LABEL_WIDTH));
        g0.setWidget(2, 1, mTerm = createTermListBox("", LIST_WIDTH, true));

        Grid g1 = new Grid(2, 4);
        mainVP.add(g1);
        g1.setWidget(0, 2, createLabel("Символ доходов/расходов", LABEL_WIDTH2));
        g1.setWidget(0, 3, mPlcode = new LookUpPlcode(TEXT_WIDTH) {
            @Override
            protected void onSetResult() {
                mAcc2.setFocus(true);
                acc2Change();
            }
        });
        g1.setWidget(1, 0, mAccountTypeButton = createAccountTypeButton("Accounting Type", BUTTON_WIDTH));
        g1.setWidget(1, 1, createAlignWidget(mAccountType = createTxtIntBox(9, TEXT_WIDTH), FIELD_WIDTH));
        g1.setWidget(1, 2, createLabel("Б/счет 2-го порядка", LABEL_WIDTH2));
        g1.setWidget(1, 3, mAcc2 = new LookUpAcc2(TEXT_WIDTH){
            @Override
            protected Object[] getInitialFilterParams() {
                return new Object[] {"Y"};
            }

            @Override
            protected void onSetResult() { plcodeChange(); }
        });

        Grid g2 = new Grid(2, 2);
        mainVP.add(g2);
        g2.setWidget(0, 0, createLabel("Название счета", LABEL_WIDTH));
        g2.setWidget(0, 1, mAccountDesc = createAreaBox(LONG_WIDTH, "60px"));

        Grid g4 = new Grid(2, 4);
        mainVP.add(g4);
        g4.setWidget(0, 0, createLabel("Дата открытия", LABEL_WIDTH));
        g4.setWidget(0, 1, createAlignWidget(mDateOpen = createDateBox(), FIELD_WIDTH2));
        g4.setWidget(1, 0, createLabel("Дата закрытия", LABEL_WIDTH));
        g4.setWidget(1, 1, createAlignWidget(mDateClose = createDateBox(null), FIELD_WIDTH2));

        g4.setWidget(0, 2, createLabel("Текущий опердень", LABEL_WIDTH));
        g4.setWidget(0, 3, mDateOperDay = createTxtBox(10));

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
        account.setBranch(check((String) mBranch.getValue()
                , "Отделение", "обязательно для заполнения", new CheckNotEmptyString()));
        account.setCustomerNumber((String)mBranch.getParam("CNUM"));
        account.setCompanyCode((String)mBranch.getParam("CBCCN"));
        if (action == FormAction.CREATE) {
            account.setCbCustomerType(check((String) mCustomerType.getValue()
                    , "Тип собственности", "обязательно для заполнения", new CheckNotNullLong(), new ConvertStringToShort()));
            account.setTerm(check((String)mTerm.getValue()
                    , "Код срока", "обязательно для заполнения", new CheckNotNullLong(), new ConvertStringToShort()));
            account.setAccountCode(check(acod
                    , "Account code", "обязательно для заполнения", new CheckNotNullLong(), new ConvertStringToShort()));
            account.setAccountSequence(check(sq
                    , "Account sequence", "обязательно для заполнения", new CheckNotNullLong(), new ConvertStringToShort()));
        } else {
            account.setCbCustomerType(null == mCustomerType.getValue() ? null : Short.parseShort((String)mCustomerType.getValue()));
            account.setTerm(null == mTerm.getValue() ? null : Short.parseShort((String)mTerm.getValue()));
            account.setAccountCode(null == acod ? null : Short.parseShort(acod));
            account.setAccountSequence(null == sq ? null : Short.parseShort(sq));
        }

        account.setAccountType(check(mAccountType.getValue()
                , "Accounting type", "обязательно для заполнения и должно содержать только цифры"
                , new CheckNotNullLong(), new ConvertStringToLong()));
        account.setBalanceAccount2(check((String) mAcc2.getValue()
                , "Б/счет 2-го порядка", "обязательно для заполнения", new CheckNotEmptyString()));
        account.setPlCode(check((String) mPlcode.getValue()
                , "Счет доходов/расходов", "обязательно для заполнения", new CheckNotEmptyString()));

        account.setCurrency("RUR");
        account.setDealSource(DealSource.MNL.getLabel());

        account.setDescription(check(mAccountDesc.getValue(),
                "Наименование счета", "обязательно для заполнения, не более 255 символов \n(Для разблокировки нажмите на кнопку 'Accounting Type')",
                new CheckStringLength(1, 255)));

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
        mBranch.setValue(null);
        mBranch.setEnabled(true);
        mAccountType.setValue(null);
        mCustomerType.setValue(null);
        mTerm.setValue(null);
        mAcc2.setValue(null);
        mPlcode.setValue(null);
        mAccountDesc.setValue(null);
        mDateOpen.setValue(null);
        mDateClose.setValue(null);
    }

    @Override
    protected void fillContent() {
        clearContent();
        setControlsEnabled();

        if (action == FormAction.UPDATE) {
            row = (Row) params;

            accountId = getFieldValue("ID");
            bsaAcid = getFieldValue("BSAACID");
            mBranch.setValue(getFieldValue("BRANCH"));
            mAccountType.setValue(getFieldText("ACCTYPE"));
            mCustomerType.setValue(getFieldText("CBCUSTTYPE"));
            String term = "00" + getFieldText("TERM");
            mTerm.setValue(term.substring(term.length()-2, term.length()));
            
            mAcc2.setValue(getFieldText("ACC2"));
            mPlcode.setValue(getFieldText("PLCODE"));

            acod = getFieldText("ACOD");
            sq = getFieldText("SQ");

            mAccountDesc.setValue(getFieldText("DESCRIPTION"));
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

    private void setControlsEnabled(){
        boolean isCreate = (action == FormAction.CREATE);
        boolean isUpdate = (action == FormAction.UPDATE);

        mBranch.setEnabled(isCreate);

        mCustomerType.setEnabled(isCreate);
        mTerm.setEnabled(isCreate);
        mAcc2.setEnabled(isCreate);
        mPlcode.setEnabled(isCreate);

        mAccountTypeButton.setEnabled(isCreate);
        mAccountType.setEnabled(isCreate);
        mAccountDesc.setEnabled(isUpdate);

        mDateOpen.setEnabled(true);
        mDateClose.setEnabled(isUpdate);
        mDateOperDay.setEnabled(false);
    }

    private void setOperday(final String operDayStr) {
        operday = DateTimeFormat.getFormat(ManualAccountWrapper.dateFormat).parse(operDayStr);
        mDateOperDay.setValue(operDayStr);
        if (null == mDateOpen.getValue())
            mDateOpen.setValue(operday);
    }

    private String getCustomerTypeName(HashMap<String, Object> result) {
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
                    GridFormDlgBase dlg = new AccountTypePlFormDlg() {

                        @Override
                        protected Object[] getInitialFilterParams() {
                        	String acc2 = mAcc2.getValue();
                        	fl707 = false;
                        	if (!isEmpty(acc2) && acc2.length() >= 3 && acc2.charAt(2) == '7') {
                        		fl707 = true;
                        		acc2 = replaceAcc2(acc2, "6");
                        	}

                            return new Object[] {operday, mAccountType.getValue(), mTerm.getValue(),
                                    mCustomerType.getValue(), acc2, mPlcode.getValue()};
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
                            mCustomerType.setValue((String)result.get("CUSTYPE"));
                            mTerm.setValue((String)result.get("TERM"));
                            String acc2 = (String)result.get("ACC2");
                            trueAcc2 = fl707 ? replaceAcc2(acc2, "7") : acc2;  
                            mAcc2.setValue(trueAcc2);
                            mPlcode.setValue((String)result.get("PLCODE"));
                            acod = (String)result.get("ACOD");
                            sq = (String)result.get("SQ");

                            accountTypeDesc = (String)result.get("ACCNAME");
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

    private void setChangeHandlers() {

        mCustomerType.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent changeEvent) {
                clearAccountType();
            	clearAcc2Plcode();
            }
        });

        mTerm.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent changeEvent) {
                clearAccountType();
            	clearAcc2Plcode();
            }
        });

        mAccountType.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent changeEvent) {
                mAccountDesc.clear();
                mAccountDesc.setEnabled(false);
                String accType = mAccountType.getValue();
                if (!isEmpty(accType) && accType.length() == 9) {
                	clearAcc2Plcode();
                }
            }
        });

        mAcc2.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent changeEvent) {
                acc2Change();
            }
        });

        mPlcode.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent changeEvent) {
                plcodeChange();
            }
        });
    }

    private void clearAccountType() {
        mAccountType.clear();
        trueAcc2 = "";
        mAccountDesc.clear();
        mAccountDesc.setEnabled(false);
    }
    
    private void clearAcc2Plcode() {
    	mAcc2.clear();
    	mPlcode.clear();
    }

    private void acc2Change() {
        if (!isEmpty(mAcc2.getValue())) {
            String acc2 = mAcc2.getValue();
            boolean flChange707 = !isEmpty(trueAcc2) && acc2.length() == 5 && acc2.charAt(2) == '7' &&
                    replaceAcc2(acc2, "6").equals(trueAcc2);
            if (!flChange707)
                clearAccountType();
        }
    }

    private void plcodeChange() {
        if (!isEmpty(mPlcode.getValue())) {
            clearAccountType();
        }
    }

    private String replaceAcc2(String acc2, String symbol) {
    	int len = acc2.length();
    	if (len < 3)
    		return acc2;
    	else
    		return acc2.substring(0, 2) + symbol + (len > 3 ? acc2.substring(3, len) : "");
    }


}


