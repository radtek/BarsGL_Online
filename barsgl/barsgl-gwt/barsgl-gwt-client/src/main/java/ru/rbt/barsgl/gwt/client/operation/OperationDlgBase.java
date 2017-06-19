package ru.rbt.barsgl.gwt.client.operation;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.datepicker.client.DateBox;
import ru.rbt.barsgl.gwt.client.comp.CachedListEnum;
import ru.rbt.barsgl.gwt.client.comp.DataListBox;
import ru.rbt.barsgl.gwt.client.comp.DataListBoxEx;
import ru.rbt.barsgl.gwt.client.comp.ICallMethod;
import ru.rbt.barsgl.gwt.client.dict.dlg.EditableDialog;
import ru.rbt.barsgl.gwt.client.dictionary.AccCustomerFormDlg;
import ru.rbt.barsgl.gwt.client.gridForm.GridFormDlgBase;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.ui.*;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;

import java.util.Date;
import java.util.HashMap;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.*;
import static ru.rbt.barsgl.shared.dict.FormAction.CREATE;
import static ru.rbt.barsgl.shared.dict.FormAction.UPDATE;


/**
 * Created by ER18837 on 16.03.16.
 */
public abstract class OperationDlgBase extends EditableDialog<ManualOperationWrapper> {
    public enum Side {DEBIT, CREDIT};

    protected final String LABEL_WIDTH = "130px";
    protected final String LABEL2_WIDTH = "85px";
    protected final String FIELD2_WIDTH = "80px";

    protected final String LONG_WIDTH = "380px";
    protected final String BUTTON_WIDTH = "75px";
    protected final String LABELS_WIDTH = "80px";
    protected final String FIELDS_WIDTH = "185px";
    protected final String SUM_WIDTH = "145px";
    protected final String LABEL_DEP_WIDTH = "110px";
    protected final String LONG_DEP_WIDTH = "400px";

    protected HashMap<String, IBoxValue> mapParam;

    private static final DateBox.DefaultFormat DATE_FORMAT = new DateBox.DefaultFormat(DateTimeFormat.getFormat("dd.MM.yyyy"));

    protected DataListBoxEx mDtCurrency;
    protected DataListBoxEx mDtFilial;
    protected TxtBox mDtAccount;
    protected TxtBox mDtSum;

    protected DataListBoxEx mCrCurrency;
    protected DataListBoxEx mCrFilial;
    protected TxtBox mCrAccount;
    protected TxtBox mCrSum;

    protected AreaBox mNarrativeRU;
    protected AreaBox mNarrativeEN;
    protected DataListBox mDepartment;
    protected DataListBox mProfitCenter;

    protected TxtBox mNum1;
    protected DatePickerBox mDate1;
    protected TxtBox mNum2;
    protected DatePickerBox mDate2;
    
    protected Button mDtButton;
    protected Button mCrButton;

    protected CheckBox mCheckFields;

    protected Long id;

    public OperationDlgBase(String title, FormAction action, Columns columns) {
        super(columns, action);
        setCaption(title);
    }

    public OperationDlgBase(){
    }

    @Override
    protected ManualOperationWrapper createWrapper() {
        return new ManualOperationWrapper();
    }

    protected Grid createOneSide(String label, final Side side, boolean withSum) {
        DataListBoxEx mCurrency;
        DataListBoxEx mFilial;
        TxtBox mAccount;
        //TxtBox mSum = null;
        BtnTxtBox mSum = null;
        Button mButton;

        boolean isDebit = side.equals(Side.DEBIT);
        Grid grid = new Grid(withSum ? 5 : 4, 2);

        grid.setWidget(0, 0, createAlignWidget(new HTML("<b>" + label + "</b>"), LABELS_WIDTH));

        grid.setWidget(1, 0, createLabel("Валюта"));
        grid.setWidget(1, 1, mCurrency = createCachedCurrencyListBox(CachedListEnum.Currency.name() + "_" + label,  "RUR", FIELD2_WIDTH, false, false));
        grid.setWidget(2, 0, createLabel(("Филиал")));
        grid.setWidget(2, 1, mFilial = createFilialListBox(CachedListEnum.Filials.name() + "_" +label, null, FIELD2_WIDTH));

        grid.setWidget(3, 0, createLabel("Счет"));
        grid.setWidget(3, 0, createAlignWidget(mButton = createBsaAcidButton("Счет", BUTTON_WIDTH, isDebit), LABELS_WIDTH));
        if (side.equals(Side.DEBIT))
            grid.setWidget(3, 1, createAlignWidget(mAccount = createTxtBox(20, SUM_WIDTH), FIELDS_WIDTH));
        else
            grid.setWidget(3, 1, mAccount = createTxtBox(20, SUM_WIDTH));
        mAccount.setName(side.name());

        mAccount.addChangeHandler(createAccountChangeHandler(side));

        if (withSum) {
            grid.setWidget(4, 0, createLabel("Сумма"));
           // grid.setWidget(4, 1, mSum = createTextBoxForSumma(20, SUM_WIDTH));
            grid.setWidget(4, 1, mSum = createBtnTextBoxForSumma(20, SUM_WIDTH, new Image(ImageConstants.INSTANCE.coins()), "Конвертация по курсу ЦБ", new ICallMethod() {
                @Override
                public void method() {
                    btnClick(side);
                }
            }));
        }

        if (isDebit) {
            mDtAccount = mAccount;
            mDtCurrency = mCurrency;
            mDtFilial = mFilial;
            mDtSum = mSum;
            mDtButton = mButton;
        } else {
            mCrAccount = mAccount;
            mCrCurrency = mCurrency;
            mCrFilial = mFilial;
            mCrSum = mSum;
            mCrButton = mButton;
        }
        return grid;
    }

    protected void btnClick(Side side){

    }

    protected DataListBoxEx createFilialListBox(String name, String filial, String width) {
        return createCachedFilialListBox(name, filial, width, false, true);
    }

    protected Grid createParams(boolean enabled){
        Grid grid = new Grid(2,6);
        Object[] widgets;
        widgets = createParams(grid, 0, enabled);
        mNum1 = (TxtBox)widgets[0]; mDate1 = (DatePickerBox)widgets[1]; 
        widgets = createParams(grid, 1, enabled);
        mNum2 = (TxtBox)widgets[0]; mDate2 = (DatePickerBox)widgets[1]; 
        return grid;
    };

    protected ChangeHandler createAccountChangeHandler(final Side side) {
        return new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent changeEvent) {
                TextBox mAccount = ((TextBox)changeEvent.getSource());
                String account = mAccount.getValue();
                if (null == account || account.length() < 20)
                    return;
                String ccyN = account.substring(5, 8);
                String filialN = "00" + account.substring(11, 13);
                DataListBoxEx mCurrency = (side == Side.DEBIT) ? mDtCurrency : mCrCurrency;
                DataListBoxEx mFilial = (side == Side.DEBIT) ? mDtFilial : mCrFilial;
                mCurrency.setParam("CCYN", ccyN);
                mFilial.setParam("CBCCN", filialN);
            }
        };
    }

    private Object[] createParams(Grid grid, int ind, boolean enabled){
    	String istr = Integer.toString(ind + 1);
        TxtBox mNum;
        DatePickerBox mDate;
        String sNum = "[N" + istr + "]";
        String sDate = "[D" + istr + "]";

        grid.setWidget(ind, 0, createLabel("Поле " + sNum, "80px"));
        grid.setWidget(ind, 1, createLabel("№", "25px"));
        grid.setWidget(ind, 2, createAlignWidget(mNum = createTxtBox(20, "120px"), "165px"));
        grid.setWidget(ind, 3, createLabel("Поле " + sDate, "70px"));
        grid.setWidget(ind, 4, createLabel("Дата", "35px"));
        grid.setWidget(ind, 5, mDate = createDateBox(null));
        mDate.setWidth("115px");

        if (null == mapParam)
        	mapParam = new HashMap<String, IBoxValue>();
        mapParam.put(sNum, mNum);
        mapParam.put(sDate, mDate);

        mNum.setEnabled(enabled);
        mDate.setEnabled(enabled);
        return new Object[] {mNum, mDate};
    };

    protected Grid createDescriptions() {
        Grid grid = new Grid(2,2);
        grid.setWidget(0, 0, createLabel("Основание ENG", LABEL_DEP_WIDTH));
        grid.setWidget(0, 1, mNarrativeEN = createAreaBox(LONG_DEP_WIDTH, "50px"));
        grid.setWidget(1, 0, new Label("Основание RUS"));
        grid.setWidget(1, 1, mNarrativeRU = createAreaBox(LONG_DEP_WIDTH, "50px"));
        return grid;
    }

    protected Grid createDepartments(boolean withCheck) {
        Grid grid = new Grid(2,4);
        grid.setWidget(0, 0, createLabel("Подразделение", LABEL_DEP_WIDTH));
        grid.setWidget(0, 1, mDepartment = createCachedDepartmentListBox(CachedListEnum.Department.name(), null, "250px", true));
        grid.setWidget(1, 0, createLabel("Профит центр"));
        grid.setWidget(1, 1, createAlignWidget(mProfitCenter = createCachedProfitCenterListBox(CachedListEnum.ProfitCenter.name(), null, "250px"), "260px"));
        if (withCheck) 
        	grid.setWidget(1, 2, mCheckFields = new CheckBox("Основание проверено"));
        return grid;
    }

    private Button createBsaAcidButton(String text, String width, final boolean isDebit) {
        Button btn = new Button();
        btn.setText(text);
        btn.addStyleName("dlg-button");
        btn.setWidth(width);
        btn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                try {
                    final TxtBox mAccount = isDebit ? mDtAccount : mCrAccount;
                    final DataListBoxEx mCurrency = isDebit ? mDtCurrency : mCrCurrency;
                    final DataListBoxEx mFilial = isDebit ? mDtFilial : mCrFilial;

                    final String bsaAcid = mAccount.getValue();
                    FormAction action = OperationDlgBase.this.action;
                    boolean editAccount = (CREATE == action || UPDATE == action);
                    GridFormDlgBase dlg = new AccCustomerFormDlg(!editAccount) {
                        @Override
                        protected boolean setResultList(HashMap<String, Object> result) {
                            if (null != result) {
                                mFilial.setParam("CBCCN", (String) result.get("CBCCN"));
                                mCurrency.setParam("CCYN", (String) result.get("CCYN"));
                                mAccount.setValue((String) result.get("BSAACID"));
                            }
                            return true;
                        }

                        @Override
                        protected Object[] getInitialFilterParams() {
                            return new Object[]{mCurrency.getValue(), mCurrency.getParam("CCYN"),
                                    mFilial.getValue(), mFilial.getParam("CBCCN"), bsaAcid, getAccountDate()};
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

    abstract protected Date getAccountDate();
}
