package ru.rbt.barsgl.gwt.client.pd;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;
import ru.rbt.barsgl.gwt.client.check.CheckDateNotAfter;
import ru.rbt.barsgl.gwt.client.check.CheckNotNullDate;
import ru.rbt.barsgl.gwt.client.comp.CachedListEnum;
import ru.rbt.barsgl.gwt.client.comp.DataListBoxEx;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterShowEvent;
import ru.rbt.barsgl.gwt.core.events.DataListBoxEvent;
import ru.rbt.barsgl.gwt.core.events.DataListBoxEventHandler;
import ru.rbt.barsgl.gwt.core.events.LocalEventBus;
import ru.rbt.barsgl.gwt.core.ui.DatePickerBox;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.gwt.core.ui.ValuesBox;
import ru.rbt.barsgl.gwt.core.utils.AppPredicate;
import ru.rbt.barsgl.gwt.core.utils.DialogUtils;
import ru.rbt.barsgl.shared.enums.ProcessingType;
import ru.rbt.barsgl.shared.operation.Rep47422Wrapper;
import ru.rbt.barsgl.shared.operday.DatesWrapper;
import ru.rbt.security.gwt.client.operday.IDataConsumer;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.*;
import static ru.rbt.barsgl.gwt.core.comp.Components.createDateBox;
import static ru.rbt.barsgl.gwt.core.comp.Components.createLabel;
import static ru.rbt.barsgl.gwt.core.comp.Components.createTxtBox;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.check;
import static ru.rbt.security.gwt.client.operday.OperDayGetter.getRep47422Dates;

/**
 * Created by er18837 on 03.08.2018.
 */
public class Rep47422Dlg extends DlgFrame implements IAfterShowEvent {
    private final String parDates = "rep47422dates";
    private final String parLists = "rep47422lists";
    private final String FWIDTH = "120px";
    private final RegExp regExp = RegExp.compile("\\d{3}/\\d{4}\\w/\\d{2}");

    private TxtBox mContract;
    private DataListBoxEx mCurrency;
    private DataListBoxEx mFilial;
    private ValuesBox mStatus;
    private CheckBox mRegister;
    private DatePickerBox mDateFrom;
    private DatePickerBox mDateTo;

    private DatesWrapper datesWrapper;

    private int asyncListCount = 2; /*count async lists:  mCurrency; mFilial; mDates*/
    private Boolean isAsyncListsLoaded;
    private HandlerRegistration registration;
    private Timer timer;

    public Rep47422Dlg() {
        super();
        setCaption("Фильтр для реконсиляционного отчета");
        ok.setText("Выбрать");
    }

    @Override
    public void beforeCreateContent() {
        isAsyncListsLoaded = (Boolean) LocalDataStorage.getParam(parLists);
        if (isAsyncListsLoaded == null || !isAsyncListsLoaded) {
            registration = LocalEventBus.addHandler(DataListBoxEvent.TYPE, dataListBoxCreatedEventHandler());
            //save in local storage sign that async list is already cached
            LocalDataStorage.putParam(parLists, true);
        }

        datesWrapper = (DatesWrapper) LocalDataStorage.getParam(parDates);
        if(datesWrapper != null)
            return;
        // TODO сделать метод дл получения допустимых дат
        getRep47422Dates(new IDataConsumer<DatesWrapper>() {
            @Override
            public void accept(DatesWrapper wrapper) {
                datesWrapper = wrapper;
                LocalDataStorage.putParam(parDates, datesWrapper);
            }
        });
    }

    private DataListBoxEventHandler dataListBoxCreatedEventHandler() {

        return new DataListBoxEventHandler(){

            @Override
            public void completeLoadData(String dataListBoxId) {
                asyncListCount--;

                if (asyncListCount == 0) {
                    registration.removeHandler();
                    isAsyncListsLoaded = true;
                }
            }
        };
    }

    @Override
    public Widget createContent() {
        VerticalPanel vpanel = new VerticalPanel();

        Grid grid = new Grid(8, 2);

        grid.setWidget(0, 0, createLabel("Номер договора"));
        grid.setWidget(0, 1, mContract = createTxtBox(12, FWIDTH));
        mContract.setTitle("000/0000L/00");
        grid.setWidget(1, 0, createLabel("Валюта"));
        grid.setWidget(1, 1, mCurrency = createCachedCurrencyListBox(CachedListEnum.Currency.name(), null, FWIDTH, false, true));
        grid.setWidget(2, 0, createLabel(("Филиал")));
        grid.setWidget(2, 1, mFilial = createFilialAuthListBox(null, FWIDTH, false, true));
        grid.setWidget(3, 0, createLabel("Статус"));
        grid.setWidget(3, 1, mStatus = new ValuesBox(getEnumLabelsOnlyList(ProcessingType.values())));
        mStatus.setWidth(FWIDTH);

        grid.setWidget(5, 0, createLabel("Оперативный регистр"));
        grid.setWidget(5, 1, mRegister = new CheckBox());

        grid.setWidget(6, 0, createLabel("Дата начала"));
        grid.setWidget(6, 1, mDateFrom = createDateBox());
        grid.setWidget(7, 0, createLabel("Дата конца"));
        grid.setWidget(7, 1, mDateTo = createDateBox());
        mDateFrom.setWidth(FWIDTH);
        mDateTo.setWidth(FWIDTH);

        grid.getCellFormatter().getElement(0, 0).getStyle().setWidth(150, Style.Unit.PX);
        grid.getElement().getStyle().setMarginBottom(8, Style.Unit.PX);

        vpanel.add(grid);

        mRegister.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> valueChangeEvent) {
                mRegisterChange(valueChangeEvent.getValue());
            }
        });

        return vpanel;
    }

    @Override
    protected void fillContent() {
        if (isReady()) {
            //если закончена обработка списков
            fillUp();
        } else {
            showPreload(true);
            timer = new Timer() {
                @Override
                public void run() {
                    if (isReady()) {
                        timer.cancel();
                        fillUp();
                        showPreload(false);
                    }
                }
            };

            timer.scheduleRepeating(500);
        }
    }

    private boolean isReady() {
        return  (isAsyncListsLoaded != null && isAsyncListsLoaded && datesWrapper != null);
    }

    private void fillUp(){
        mRegister.setValue(true);
        mRegisterChange(true);
    };

    private void mRegisterChange(boolean toOn) {
        mDateFrom.setEnabled(!toOn);
        mDateTo.setEnabled(!toOn);
        if (toOn) {
            mDateFrom.setValue(datesWrapper.getDateFrom());
            mDateTo.setValue(datesWrapper.getDateTo());
        }
    }

    private void checkUp() throws Exception {
        if (!DialogUtils.isEmpty(mContract.getValue())) {
            check(mContract.getValue(), "Номер договора", "формат '000/0000L/00', где 0 – цифры от 0 до 9, L – любые буквы",
                    new AppPredicate<String>() {
                        @Override
                        public boolean check(String target) {
                            return regExp.test(target);
                        }
                    });
        }
        check(mDateFrom.getValue(), "Дата начала", "поле не заполнено", new CheckNotNullDate());
        check(mDateTo.getValue(), "Дата конца", "поле не заполнено", new CheckNotNullDate());
        check(mDateFrom.getValue(), "Дата начала", "дата не может быть больше даты конца", new CheckDateNotAfter(mDateTo.getValue()));
        check(mDateTo.getValue(), "Дата конца", "дата не может быть больше предыдущего операционного дня", new CheckDateNotAfter(datesWrapper.getDateTo()));
    }

    @Override
    protected boolean onClickOK() throws Exception {
        try{
            checkUp();
            Rep47422Wrapper wrapper = new Rep47422Wrapper();
            wrapper.setContract(mContract.getValue());
            wrapper.setCurrency((String) mCurrency.getValue());
            wrapper.setFilial((String) mFilial.getValue());
            wrapper.setProcType(ProcessingType.valueOf((String) mStatus.getValue()));

            wrapper.setRegister(mRegister.getValue());
            wrapper.setDateFrom(mDateFrom.getValue());
            wrapper.setDateTo(mDateTo.getValue());

            params = wrapper;
            return true;
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().equals("column")) {
                return false;
            } else {
                throw e;
            }
        }
    }


    @Override
    public void afterShow() {
        mContract.setFocus(true);
    }
}
