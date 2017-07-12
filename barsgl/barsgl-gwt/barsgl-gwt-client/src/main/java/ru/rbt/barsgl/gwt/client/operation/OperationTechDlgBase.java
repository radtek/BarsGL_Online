package ru.rbt.barsgl.gwt.client.operation;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.comp.CachedListEnum;
import ru.rbt.barsgl.gwt.client.comp.DataListBox;
import ru.rbt.barsgl.gwt.client.comp.DataListBoxEx;
import ru.rbt.barsgl.gwt.client.dict.dlg.EditableDialog;
import ru.rbt.barsgl.gwt.client.dictionary.AccTechFormDlg;
import ru.rbt.barsgl.gwt.client.gridForm.GridFormDlgBase;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.ui.AreaBox;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.gwt.core.utils.DialogUtils;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.operation.ManualTechOperationWrapper;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;

import java.util.Date;
import java.util.HashMap;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.*;
import static ru.rbt.barsgl.shared.dict.FormAction.CREATE;
import static ru.rbt.barsgl.shared.dict.FormAction.UPDATE;


/**
 * Created by ER18837 on 16.03.16.
 */
public abstract class OperationTechDlgBase extends EditableDialog<ManualTechOperationWrapper> {
    public enum Side {DEBIT, CREDIT};

    protected final String LABEL_WIDTH = "130px";
    protected final String LABEL2_WIDTH = "85px";
    protected final String FIELD2_WIDTH = "80px";

    protected final String BUTTON_WIDTH = "75px";
    protected final String LABELS_WIDTH = "80px";
    protected final String FIELDS_WIDTH = "185px";
    protected final String SUM_WIDTH = "145px";
    protected final String LABEL_DEP_WIDTH = "110px";
    protected final String LONG_DEP_WIDTH = "400px";

    protected DataListBoxEx mDtCurrency;
    protected DataListBoxEx mDtFilial;
    protected TxtBox mDtAccount;
    protected TxtBox mDtSum;
    protected TxtBox mDtAccType;

    protected DataListBoxEx mCrCurrency;
    protected DataListBoxEx mCrFilial;
    protected TxtBox mCrAccount;
    protected TxtBox mCrSum;
    protected TxtBox mCrAccType;

    protected AreaBox mNarrativeRU;
    protected AreaBox mNarrativeEN;
    protected DataListBox mDepartment;
    protected DataListBox mProfitCenter;

    protected Button mDtAccTypeButton;
    protected Button mCrAccTypeButton;

    protected CheckBox mCheckFields;

    protected Long id;


    public OperationTechDlgBase(String title, FormAction action, Columns columns) {
        super(columns, action);
        setCaption(title);
    }

    @Override
    protected ManualTechOperationWrapper createWrapper() {
        return new ManualTechOperationWrapper();
    }

    protected Grid createOneSide(String label, final Side side, boolean withSum) {
        DataListBoxEx mCurrency;
        DataListBoxEx mFilial;
        TxtBox mAccount;
        TxtBox mAccType;
        TxtBox mSum = null;
        Button mButton;

        boolean isDebit = side.equals(Side.DEBIT);

        Grid grid = new Grid(6, 2);

        grid.setWidget(0, 0, createAlignWidget(new HTML("<b>" + label + "</b>"), LABELS_WIDTH));

        grid.setWidget(1, 0, createLabel("Валюта"));
        grid.setWidget(1, 1, mCurrency = createCachedCurrencyListBox(CachedListEnum.Currency.name() + "_" + label + "_TH",  "RUR", FIELD2_WIDTH, false, false));
        grid.setWidget(2, 0, createLabel(("Филиал")));
        grid.setWidget(2, 1, mFilial =  createFilialListBox(CachedListEnum.Filials.name() + "_" +label + "_TH", null, FIELD2_WIDTH));

        grid.setWidget(3, 0, createAlignWidget(mButton = createAccTypedButton("AccType", BUTTON_WIDTH, isDebit), LABELS_WIDTH));

        if (side.equals(Side.DEBIT))
            grid.setWidget(3, 1, createAlignWidget(mAccType = createTxtBox(10, SUM_WIDTH), FIELDS_WIDTH));
        else
            grid.setWidget(3, 1, mAccType = createTxtBox(10, SUM_WIDTH));

        grid.setWidget(4, 0, createLabel("Счет"));

        if (side.equals(Side.DEBIT))
            grid.setWidget(4, 1, createAlignWidget(mAccount = createTxtBox(20, SUM_WIDTH), FIELDS_WIDTH));
        else
            grid.setWidget(4, 1, mAccount = createTxtBox(20, SUM_WIDTH));
        mAccount.setName(side.name());
        mAccount.setEnabled(false);

        mCurrency.addChangeHandler(createCurrencyChangeHandler(side));
        mFilial.addChangeHandler(createFilialChangeHandler(side));
        mAccType.addChangeHandler(createAccTypeChangeHandler(side));

        if (withSum) {
            grid.setWidget(5, 0, createLabel("Сумма"));
            grid.setWidget(5, 1, mSum = createTextBoxForSumma(20, SUM_WIDTH));
        }

        if (isDebit) {
            mDtAccount = mAccount;
            mDtCurrency = mCurrency;
            mDtFilial = mFilial;
            mDtSum = mSum;
            mDtAccTypeButton = mButton;
            mDtAccType = mAccType;
        } else {
            mCrAccount = mAccount;
            mCrCurrency = mCurrency;
            mCrFilial = mFilial;
            mCrSum = mSum;
            mCrAccTypeButton = mButton;
            mCrAccType = mAccType;
        }
        return grid;
    }

    protected void btnClick(Side side){}

    protected DataListBoxEx createFilialListBox(String name, String filial, String width) {
        return createCachedFilialListBox(name, filial, width, false, true);
    }

    private void clearCorAccount(Side side){
        TxtBox mAccount = (side != Side.DEBIT) ? mDtAccount : mCrAccount;
        mAccount.clear();
    }

    protected ChangeHandler createFilialChangeHandler(final Side side) {
        return new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent changeEvent){
                filialChangeEvent(side, true);

                TxtBox mAccType = (side == Side.DEBIT) ? mDtAccType : mCrAccType;
                updateAccount(side, mAccType);
               /* clearCorAccount(side);*/
            }
        };
    }

    private void filialChangeEvent(final Side side, boolean isChanged){
        if (!isChanged) return;
        if (side.equals(Side.DEBIT)) {
            mCrFilial.setSelectedIndex(mDtFilial.getSelectedIndex());
        }
        else{
            mDtFilial.setSelectedIndex(mCrFilial.getSelectedIndex());
        }
        clearCorAccount(side);
    }

    protected ChangeHandler createCurrencyChangeHandler(final Side side) {
        return new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent changeEvent){
                TxtBox mAccType = (side == Side.DEBIT) ? mDtAccType : mCrAccType;
                updateAccount(side, mAccType);
            }
        };
    }

    protected ChangeHandler createAccTypeChangeHandler(final Side side) {
        return new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent changeEvent) {
                TxtBox mAccType = (side == Side.DEBIT) ? mDtAccType : mCrAccType;
                updateAccount(side, mAccType);
            }
        };
    }

    private void updateAccount(final Side side, TxtBox mAccType)
    {
        if (null == mAccType) return;
        String accType = mAccType.getText();

        if (null != accType && accType.length() > 0) {
            for (char c : accType.toCharArray()) {
                if (!Character.isDigit(c)) {
                    DialogUtils.showInfo("Accounting Type должен содерждать только цифры");
                    mAccType.clear();
                    return;
                }
            }
        }
        DataListBoxEx mCurrency = (side == Side.DEBIT) ? mDtCurrency : mCrCurrency;
        DataListBoxEx mFilial = (side == Side.DEBIT) ? mDtFilial : mCrFilial;
        final TxtBox mAccount = (side == Side.DEBIT) ? mDtAccount : mCrAccount;


        if (null == accType || accType.length() < 9) {
            if (mAccount != null) {
                mAccount.setValue("");
            }
            return;
        }
        String cbccn = null;
        String ccy = null;
        if (mCurrency!=null && mCurrency.getValue() != null) {
            ccy = mCurrency.getParam("CCY") != null ? String.valueOf(mCurrency.getParam("CCY")) : "";
        }
        if (mFilial.getParam("CBCC") != null) {
            cbccn = String.valueOf(mFilial.getParam("CBCC"));
        }

        if ((ccy != null) && (!ccy.isEmpty()) && (cbccn != null) && (!cbccn.isEmpty())) {
            final ManualAccountWrapper accWrapper = new ManualAccountWrapper();
            if (null != mAccType) {
                accWrapper.setAccountType(mAccType.getValue() != null ? Long.parseLong(mAccType.getValue()) : null);
            }
            accWrapper.setCurrency(ccy);
            accWrapper.setFilial(cbccn);

            BarsGLEntryPoint.operationService.findAccount(accWrapper, new AuthCheckAsyncCallback<RpcRes_Base<ManualAccountWrapper>>() {
                @Override
                public void onSuccess(RpcRes_Base<ManualAccountWrapper> wrapper) {
                    if (!wrapper.isError()) {
                        mAccount.setValue(wrapper.getResult().getBsaAcid());
                    } else {
                        mAccount.clear();
                    }
                }
            });
        } else {
            mAccount.clear();
        }
    }

    /**
     * Проверяем данные по дебету и кредиту
     */
    protected boolean checkOneSideData()
    {
        String curDt = "";
        String curCr = "";

        if (mDtCurrency!=null && mDtCurrency.getParam("CCY")!=null) {
            curDt  = mDtCurrency.getParam("CCY").toString();
        }

        if (mCrCurrency!=null && mCrCurrency.getParam("CCY")!=null) {
            curCr = mCrCurrency.getParam("CCY").toString();
        }

        String filialDt = "";
        String filialCr = "";

        if (null != mDtFilial.getValue()) {
            filialDt = mDtFilial.getValue().toString();
        }

        if (null != mCrFilial.getValue()) {
            filialCr = mCrFilial.getValue().toString();
        }

        if (!filialDt.equalsIgnoreCase(filialCr))
        {
            DialogUtils.showInfo("Операции по техсчетам должны проводиться только в рамках одного филиала");
            return false;
        }

        if (!curDt.equals("RUR") && !curCr.equals("RUR"))
        {
            DialogUtils.showInfo("Одна из валют должна быть RUR");
            return false;
        }

        if (mDtAccount.getValue() != null && mCrAccount.getValue()!=null) {
            String bsaccidDt = mDtAccount.getValue();
            String bsaccidCr = mCrAccount.getValue();

            if (bsaccidDt.equalsIgnoreCase(bsaccidCr)) {
                DialogUtils.showInfo("Невозможно осуществить операцию в рамках одного счёта");
                return false;
            }
        }

        return true;
    }

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
        grid.setWidget(0, 1, mDepartment = createCachedDepartmentListBox(CachedListEnum.Department.name() + "_TH", null, "250px", true));
        grid.setWidget(1, 0, createLabel("Профит центр"));
        grid.setWidget(1, 1, createAlignWidget(mProfitCenter = createCachedProfitCenterListBox(CachedListEnum.ProfitCenter.name() + "_TH", null, "250px"), "260px"));
        if (withCheck) 
        	grid.setWidget(1, 2, mCheckFields = new CheckBox("Основание проверено"));
        return grid;
    }

    private Button createAccTypedButton(String text, String width, final boolean isDebit) {
        Button btn = new Button();
        btn.setText(text);
        btn.addStyleName("dlg-button");
        btn.setWidth(width);
        btn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                try {

                    final TxtBox mAccType = isDebit ? mDtAccType : mCrAccType;
                    final TxtBox mAccount = isDebit ? mDtAccount : mCrAccount;
                    final DataListBoxEx mCurrency = isDebit ? mDtCurrency : mCrCurrency;
                    final DataListBoxEx mFilial = isDebit ? mDtFilial : mCrFilial;

                    FormAction action = OperationTechDlgBase.this.action;
                    final boolean editAccount = (CREATE == action || UPDATE == action) && mAccType.isEnabled();
                    GridFormDlgBase dlg = new AccTechFormDlg() {
                        @Override
                        protected boolean getEditMode() {
                            return editAccount;
                        }

                        @Override
                        protected boolean setResultList(HashMap<String, Object> result) {
                            if (null != result) {
                                mAccType.setValue((String)result.get("ACCTYPE"));
                                boolean isFilialChanged = !((String) mFilial.getParam("CBCC")).equals((String) result.get("CBCC"));
                                mFilial.setParam("CBCC", (String) result.get("CBCC"));
                                mCurrency.setParam("CCY", (String) result.get("CCY"));
                                mAccount.setValue((String) result.get("BSAACID"));
                                filialChangeEvent(isDebit ? Side.DEBIT : Side.CREDIT, isFilialChanged);
                            }
                            return true;
                        }

                        @Override
                        protected Object[] getInitialFilterParams() {
                            return new Object[]{ editAccount ? getOperDate() : null,
                                    editAccount ?  getValueDate() : null,
                                    mAccType.getValue(),
                                    (String) mFilial.getParam("CBCC"),
                                    (String) mCurrency.getParam("CCY"),
                                    editAccount ? null : mAccount.getValue()};
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

    abstract protected Date getValueDate();
    abstract protected Date getOperDate();
}
