package ru.rbt.barsgl.gwt.client.pd;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.client.check.CheckNotNullDate;
import ru.rbt.barsgl.gwt.client.comp.CachedListEnum;
import ru.rbt.barsgl.gwt.client.comp.DataListBoxEx;
import ru.rbt.barsgl.gwt.core.dialogs.DialogManager;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterShowEvent;
import ru.rbt.barsgl.gwt.core.ui.DatePickerBox;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.gwt.core.ui.ValuesBox;
import ru.rbt.barsgl.shared.ClientDateUtils;
import ru.rbt.barsgl.shared.enums.ProcessingType;
import ru.rbt.barsgl.shared.operation.Reconc47422Wrapper;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.*;
import static ru.rbt.barsgl.gwt.core.comp.Components.createDateBox;
import static ru.rbt.barsgl.gwt.core.comp.Components.createLabel;
import static ru.rbt.barsgl.gwt.core.comp.Components.createTxtBox;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.check;

/**
 * Created by er18837 on 03.08.2018.
 */
public class Reconc47422ReportDlg extends DlgFrame implements IAfterShowEvent {
    protected final String WIDTH1 = "80px";
    protected final String WIDTH2 = "120px";

    private TxtBox mContract;
    private DataListBoxEx mCurrency;
    private DataListBoxEx mFilial;
    private ValuesBox mStatus;
    private CheckBox mNow;
    private DatePickerBox mDateFrom;
    private DatePickerBox mDateTo;

    public Reconc47422ReportDlg() {
        super();
        setCaption("Фильтр для реконсиляционного отчета");
        ok.setText("Выбрать");
    }

    @Override
    public Widget createContent() {
        VerticalPanel vpanel = new VerticalPanel();

        Grid grid = new Grid(8, 2);

        grid.setWidget(0, 0, createLabel("Номер договора"));
        grid.setWidget(0, 1, mContract = createTxtBox(12, WIDTH1));
        grid.setWidget(1, 0, createLabel("Валюта"));
        grid.setWidget(1, 1, mCurrency = createCachedCurrencyListBox(CachedListEnum.Currency.name(), null, WIDTH1, false, true));
        grid.setWidget(2, 0, createLabel(("Филиал")));
        grid.setWidget(2, 1, mFilial = createCachedFilialListBox(CachedListEnum.Filials.name(), null, WIDTH1, false, true));
        grid.setWidget(3, 0, createLabel("Статус"));
        grid.setWidget(3, 1, mStatus = new ValuesBox(getEnumLabelsOnlyList(ProcessingType.values())));
        mStatus.setWidth(WIDTH2);

        grid.setWidget(5, 0, createLabel("Оперативный регистр"));
        grid.setWidget(5, 1, mNow = new CheckBox());

        grid.setWidget(6, 0, createLabel("Дата начала"));
        grid.setWidget(6, 1, mDateFrom = createDateBox());
        grid.setWidget(7, 0, createLabel("Дата конца"));
        grid.setWidget(7, 1, mDateTo = createDateBox());
        mDateFrom.setWidth(WIDTH2);
        mDateTo.setWidth(WIDTH2);

        grid.getCellFormatter().getElement(0, 0).getStyle().setWidth(150, Style.Unit.PX);
        grid.getElement().getStyle().setMarginBottom(8, Style.Unit.PX);

        vpanel.add(grid);

        return vpanel;
    }

    private void checkUp() throws Exception {
        if (!mNow.getValue()) {
            check(mDateFrom.getValue(), "Дата начала", "поле не заполнено", new CheckNotNullDate());
            check(mDateTo.getValue(), "Дата конца", "поле не заполнено", new CheckNotNullDate());
        }
    }

    @Override
    protected boolean onClickOK() throws Exception {
        try{
            checkUp();
            Reconc47422Wrapper wrapper = new Reconc47422Wrapper();
            wrapper.setContract(mContract.getValue());
            wrapper.setCurrency((String) mCurrency.getValue());
            wrapper.setFilial((String) mFilial.getValue());
            wrapper.setProcType(ProcessingType.valueOf((String) mStatus.getValue()));

            wrapper.setBoolNow(mNow.getValue());
            wrapper.setDateFromStr(ClientDateUtils.Date2String(mDateFrom.getValue()));
            wrapper.setDateToStr(ClientDateUtils.Date2String(mDateTo.getValue()));
            params = wrapper;
            return true;
        } catch (Exception ex){
            DialogManager.message("Предупреждение", ex.getMessage());
            return false;
        }
    }


    @Override
    public void afterShow() {
        mContract.setFocus(true);
    }
}
