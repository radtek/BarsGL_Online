package ru.rbt.barsgl.gwt.client.account_ofr;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.check.CheckLongInterval;
import ru.rbt.barsgl.gwt.client.check.CheckNotEmptyString;
import ru.rbt.barsgl.gwt.client.check.CheckStringExactLength;
import ru.rbt.barsgl.gwt.client.check.ConvertStringToShort;
import ru.rbt.barsgl.gwt.client.comp.*;
import ru.rbt.barsgl.gwt.client.dictionary.OfrSymbolFormDlg;
import ru.rbt.barsgl.gwt.client.gridForm.GridFormDlgBase;
import ru.rbt.security.gwt.client.operday.IDataConsumer;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.ColumnsBuilder;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.ui.DatePickerBox;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.gwt.core.utils.DialogUtils;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static ru.rbt.barsgl.gwt.core.comp.Components.*;
import static ru.rbt.barsgl.gwt.client.comp.GLComponents.*;
import ru.rbt.barsgl.gwt.core.comp.Components;
import static ru.rbt.security.gwt.client.operday.OperDayGetter.getOperday;
import static ru.rbt.barsgl.gwt.core.datafields.Column.Sort.ASC;
import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.LONG;
import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.STRING;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.*;

/**
 * Created by ER18837 on 10.11.15.
 */
public class OfrAccountDlg extends DlgFrame {

	private final String LABEL_WIDTH = "120px"; 
	private final String BUTTON_WIDTH = "110px"; 
	private final String FIELD_WIDTH = "160px"; 
	private final String LIST_WIDTH = "280px"; 

	private ManualAccountWrapper account = new ManualAccountWrapper();

    private DataListBox mBranchList;
    private DataListBox mAcod;
    private TxtBox mSq;
    private DataListBox mCustType;
    private TxtBox mAcc2;
    private TxtBox mSymbolOfr;
    private TxtBox mAcid;

    private DatePickerBox mDateOpen;
    private TxtBox mDateOperDay;
    private CheckBox mEnablePlCode;
    
    private Grid g0;
    private ChangeHandler changeHandler;


    public OfrAccountDlg() {
        super();
        ok.setText(TEXT_CONSTANTS.formInput_save());
        cancel.setText(TEXT_CONSTANTS.formInput_cancel());
        setCaption("Ввод нового счета ОФР");
    }

    private void clear(){
        mSq.clear();
        mAcc2.clear();
        mSymbolOfr.clear();
        mAcid.clear();
        mEnablePlCode.setValue(false);
        mBranchList.setSelectedIndex(0);
        mCustType.setSelectedIndex(0);
    }


    @Override
    public Widget createContent() {

        VerticalPanel mainVP = new VerticalPanel();

        g0 = new Grid(4, 4);
        mainVP.add(g0);
        g0.setWidget(0, 0, createAlignLabel("Отделение"));
        g0.setWidget(0, 1, mBranchList = createBranchListBox("001", LIST_WIDTH, false));

        g0.setWidget(1, 0, createAlignLabel("ACOD"));
//        g0.setWidget(1, 1, createAlignList(mAcod = createAcodListBox("", null)));
        g0.setWidget(1, 2, Components.createLabel("SQ", "40px"));
        g0.setWidget(1, 3, mSq = Components.createTxtIntBox(2));
        g0.setWidget(2, 0, createAlignLabel("Тип собственности"));
        g0.setWidget(2, 1, mCustType = createCustTypeListBox("0", LIST_WIDTH, false));

        Grid g1 = new Grid(2, 4);
        mainVP.add(g1);
        g1.setWidget(0, 0, createAlignLabel("Дата открытия"));
        g1.setWidget(0, 1, createAlignField(mDateOpen = Components.createDateBox()));
        g1.setWidget(0, 2, createAlignLabel("Текущий опердень"));
        g1.setWidget(0, 3, mDateOperDay = Components.createTxtBox(10));
        mDateOperDay.setEnabled(false);

        Grid g2 = new Grid(1, 4);
        mainVP.add(g2);
        g2.setWidget(0, 0, createMidasButton());
        g2.setWidget(0, 1, createAlignField(mAcid = Components.createTxtIntBox(20, "150px")));

        Grid g3 = new Grid(3, 4);
        mainVP.add(g3);
        g3.setWidget(0, 0, createAlignLabel("Балансовый счет"));
        g3.setWidget(0, 1, Components.createAlignWidget(mAcc2 = createTxtIntBox(5), "80px"));
        g3.setWidget(1, 0, createAlignLabel("Символ ОФР"));
        g3.setWidget(1, 0, createOfrButton());	// TODO сделать позже
        g3.setWidget(1, 1, mSymbolOfr = Components.createTxtIntBox(5));

        mAcid.setEnabled(false);
        mAcc2.setEnabled(false);
        mSymbolOfr.setEnabled(false);

        g3.setWidget(1, 2, mEnablePlCode = new CheckBox("Символ на редактирование"));
        setEnablePlCodeHendler();

        getOperday(new IDataConsumer<OperDayWrapper>() {
            @Override
            public void accept(OperDayWrapper wrapper) {
                Date operday = setOperday(wrapper.getCurrentOD());
                mAcod = createAcodListBox("", operday, LIST_WIDTH);
                g0.setWidget(1, 1, createAlignList(mAcod));
                mAcod.addChangeHandler(changeHandler);
            }
        });

        setChangeHandlers();
        return mainVP;
    }

    @Override
    protected void fillContent() {
        clear();
        if ((Boolean) params){
            //Если уже создана форма, то пересоздать только Acod и OperDay
            getOperday(new IDataConsumer<OperDayWrapper>() {
                @Override
                public void accept(OperDayWrapper wrapper) {
                    Date operday = setOperday(wrapper.getCurrentOD());
                    mAcod.setSelectedIndex(0);
                    mAcod = createAcodListBox("", operday, LIST_WIDTH);
                }
            });
        }
    }

    @Override
    public boolean onClickOK() {
        params = account;
        if (!setAccountMidasParameters(account))
            return false;
        if (!setAccountBsaParameters(account, true))
            return false;
        String msg = mEnablePlCode.getValue() ?
                "Данный символ с заданными атрибутами отсутствует в настроечной таблице!!!\n" +
                "Вы действительно хотите продолжить операцию открытия счета ЦБ?" :
                "Вы действительно хотите открыть счет с заданными параметрами?";
//        return Window.confirm(msg);
        
        showConfirm(msg, this.getDlgEvents(), params);
        return false;
    }

    private boolean setAccountMidasParameters(ManualAccountWrapper wrapper) {
        try {
            wrapper.setBranch(check(mBranchList.getValue().toString()
                    , "Отделение", "обязательно для заполнения", new CheckNotEmptyString()));
            wrapper.setAccountCode(check(mAcod.getValue().toString()
                    , "ACOD", "обязательно для заполнения", new CheckNotEmptyString(), new ConvertStringToShort()));
            wrapper.setAccountSequence(check(mSq.getValue()
                    , "SQ", "обязательно для заполнения (число от 1 до 99)"
                    , new CheckLongInterval(1, 99), new ConvertStringToShort()));
            wrapper.setCbCustomerType(check(mCustType.getValue().toString(),
                    "Тип собственности", "обязательно для заполнения", new CheckNotEmptyString(), new ConvertStringToShort()));
            wrapper.setDateOpenStr(DateTimeFormat.getFormat(account.dateFormat).format(mDateOpen.getValue()));
            wrapper.setCurrency("RUR");
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().equals("column")) {
                return false;
            } else {
                throw e;
            }
        } catch (Exception e) {
            Window.alert("Exception: " + e.getMessage());  // TODO DEBUG
        }
        return true;
    }

    private boolean setAccountBsaParameters(ManualAccountWrapper wrapper, boolean fill) {
        try {
            if (fill) {
                wrapper.setAcid(check(mAcid.getValue(),
                        "Счет Midas", "обязательно для заполнения", new CheckNotEmptyString()));
                wrapper.setBalanceAccount2(check(mAcc2.getValue(),
                        "Балансовый счет", "обязательно для заполнения", new CheckNotEmptyString()));
                String msg = mEnablePlCode.getValue() ? "" : "\nВозможен ручной ввод (Символ на редактирование)";
                wrapper.setPlCode(check(mSymbolOfr.getValue(),
                        "Символ ОФР", "обязательно для заполнения (5 цифр)" + msg, new CheckStringExactLength(mSymbolOfr.getMaxLength())));
            } else {
                wrapper.setAcid("");
                wrapper.setBalanceAccount2("");
                wrapper.setPlCode("");
            }
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().equals("column")) {
                return false;
            } else {
                throw e;
            }
        }
        return true;
    }


    private Label createAlignLabel(String text) {
        return Components.createLabel(text, LABEL_WIDTH);
    }

    private DataListBox createAlignList(DataListBox list) {
        list.setWidth(LIST_WIDTH);
        return list;
    }

    private Widget createAlignField(Widget widget) {
        return Components.createAlignWidget(widget, FIELD_WIDTH);
    }

    /**
     * Заполняет список кодов Майдас
     * @param acod
     * @return
     */
    private DataListBox createAcodListBox(String acod, Date dateOpen, String width) {
    	String dateStr = DateTimeFormat.getFormat("yyyy-MM-dd").format(dateOpen);

        String sql = "select A5ACCD as ACOD, A5ACCD || ' ' || A5ACCN as ANAME from (\n" +
                "select distinct ac.A5ACCD, ac.A5ACCN\n" +
                " from SDACODPD ac join IMBCBHBPN pl on pl.HBMIAC=ac.A5ACCD" +
                " and '" + dateStr + "' between pl.DAT and pl.DATTO" +
                " and A5ACCD not in ('7903', '7904', '7907', '7908')) T ";
        Columns columns = new ColumnsBuilder().addColumn("ACOD", LONG).addColumn("ANAME", STRING).build();
        List<SortItem> sort = new ArrayList<>();
        sort.add(new SortItem("ACOD", ASC));

        DataListBox box = new DataListBox(
                new ListBoxSqlDataProvider(acod, sql, columns, null, sort, new StringRowConverter(0, 1)));
        box.setWidth(width);
        return box;
    }

    private void setEnablePlCodeHendler() {
        mEnablePlCode.setValue(false);
        mEnablePlCode.setEnabled(true);

        mEnablePlCode.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> valueChangeEvent) {
                boolean toOn = valueChangeEvent.getValue();
                if (toOn) {     // разрешить редактирование
                    CheckNotEmptyString chkEmpty = new CheckNotEmptyString();
                    boolean isMidas = chkEmpty.check(mAcid.getValue());
                    boolean isAcc2 = chkEmpty.check(mAcc2.getValue());
                    boolean isPlCode = chkEmpty.check(mSymbolOfr.getValue());

                    if (isPlCode && isAcc2) {
                    	DialogUtils.showInfo("Все атрибуты счета определены. "
                                + "\nЕсли уверены в правильности атрибутов, нажмите кнопку «Ввод» "
                                + "\nдля запуска функции генерации счета ЦБ");
                        mEnablePlCode.setValue(false);
                    } else if (!isAcc2) {
                    	DialogUtils.showInfo("Не все атрибуты счета определены. "
                                + "\nЗаполните недостающие атрибуты и нажмите кнопку «Счет Midas» "
                                + "\nдля заполнения расчетных полей");
                        mEnablePlCode.setValue(false);
                    } else {
                        setSymbolOfrEnabled(true);
                    }
                } else {
                	mSymbolOfr.clear();
                    setSymbolOfrEnabled(false);
                }
            }
        });
    }

    private void setSymbolOfrEnabled(boolean enable) {
        mSymbolOfr.setEnabled(enable);
        mBranchList.setEnabled(!enable);
        mAcod.setEnabled(!enable);
        mCustType.setEnabled(!enable);
        mSq.setEnabled(!enable);
        mDateOpen.setEnabled(!enable);
    }
    
    private Button createMidasButton() {
    	Button btn = new Button(); 
    	btn.setText("Счет Midas");
        btn.addStyleName("dlg-button");
        btn.setWidth(BUTTON_WIDTH);
        btn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                if (!setAccountMidasParameters(account))
                	return;
                setAccountBsaParameters(account, false);

                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
                BarsGLEntryPoint.operationService.getOfrAccountParameters(account, new AuthCheckAsyncCallback<RpcRes_Base<ManualAccountWrapper>>() {
                    @Override
                    public void onFailureOthers(Throwable throwable) {
                        WaitingManager.hide();
                        showInfo("Ошибка: не удалось получить параметры счета", throwable.getLocalizedMessage());
                    }

                    @Override
                    public void onSuccess(RpcRes_Base<ManualAccountWrapper> accWrapper) {
                        if (accWrapper.isError()) {
                            showInfo("Ошибка при получении параметров счета", accWrapper.getMessage());
                        } else if (!accWrapper.getMessage().isEmpty()) {
                            DialogUtils.showInfo(accWrapper.getMessage());
                        }
                        
                        ManualAccountWrapper wrapper = accWrapper.getResult();
                        fillParameters(wrapper.getCbCustomerType(), wrapper.getAcid(), wrapper.getBalanceAccount2(), wrapper.getPlCode());
                        WaitingManager.hide();
                    }
                });

            }
        });
        return btn;
    }

    private Button createOfrButton() {
        Button btn = new Button();
        btn.setText("Символ ОФР");
        btn.addStyleName("dlg-button");
        btn.setWidth(BUTTON_WIDTH);
        btn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                try {
                    GridFormDlgBase dlg = new OfrSymbolFormDlg(mDateOpen.getValue(), (String)mAcod.getValue(), (String)mCustType.getValue()) {
						@Override
						protected Object[] getInitialFilterParams() {
							return new Object[] {mDateOpen.getValue(), (String)mAcod.getValue(), (String)mCustType.getValue()};
						}
                        @Override
                        protected boolean setResultList(HashMap<String, Object> result) {
                            Date dateFrom = (Date)result.get("DAT");
                            Date dateTo = (Date)result.get("DATTO");
                            Date dateOpen = mDateOpen.getValue();
                            String ofr = result.get("HBITEM").toString();
                            if (mDateOpen.getValue().before(dateFrom) || mDateOpen.getValue().after(dateTo)) {
                                showInfo("Ошибка", "Символ ОФР " + ofr + " недействителен на дату " + mDateOpen.getText());
                                return false;
                            }
                            mSymbolOfr.setValue(ofr);
                            mAcod.setValue(result.get("HBMIAC").toString());
                            mCustType.setValue(result.get("HBCTYP").toString());
                            String sq = result.get("HBMISQ").toString();
                            if (!isEmpty(sq) && !sq.equals("0"))
                                mSq.setValue(sq);
                            mAcid.clear();
                            mAcc2.clear();
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

    private void fillParameters(Short custType, String acid, String acc2, String plcode) {
    	mCustType.setValue(Short.toString(custType));
        mAcid.setValue(acid);
        mAcc2.setValue(acc2);
        mSymbolOfr.setValue(plcode);
    }

    private void clearParameterss() {
        mAcid.clear();
        mAcc2.clear();
        mSymbolOfr.clear();
    }

    private Date setOperday(final String operDayStr) {
    	Date operday = DateTimeFormat.getFormat(account.dateFormat).parse(operDayStr);
        mDateOperDay.setValue(operDayStr);
        mDateOpen.setValue(operday);
        return operday;
    }

    private void setChangeHandlers() {
        changeHandler = new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent changeEvent) {
                clearParameterss();
            }
        };
        mCustType.addChangeHandler(changeHandler);
        mBranchList.addChangeHandler(changeHandler);
        mSq.addChangeHandler(changeHandler);

        mSq.addKeyPressHandler(new KeyPressHandler() {
			@Override
			public void onKeyPress(KeyPressEvent event) {
                clearParameterss();
			}
        });

    }
}
