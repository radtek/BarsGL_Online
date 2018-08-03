package ru.rbt.barsgl.gwt.client.pd;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.client.comp.CachedListEnum;
import ru.rbt.barsgl.gwt.client.comp.DataListBoxEx;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterShowEvent;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.createCachedCurrencyListBox;
import static ru.rbt.barsgl.gwt.client.comp.GLComponents.createCachedFilialListBox;
import static ru.rbt.barsgl.gwt.core.comp.Components.createLabel;
import static ru.rbt.barsgl.gwt.core.comp.Components.createTxtBox;

/**
 * Created by er18837 on 03.08.2018.
 */
public class Reconc4496ReportDlg extends DlgFrame implements IAfterShowEvent {
    protected final String FIELD2_WIDTH = "80px";
    protected final String SUM_WIDTH = "145px";

    private TxtBox mContract;
    private DataListBoxEx mCurrency;
    private DataListBoxEx mFilial;
    private TxtBox mStatus;

    public Reconc4496ReportDlg() {
        super();
        setCaption("Установка фильтра для реконсиляционного отчета");
        ok.setText("Выбрать");
    }

    @Override
    public Widget createContent() {
        VerticalPanel vpanel = new VerticalPanel();

        Grid grid = new Grid(4, 2);

        grid.setWidget(0, 0, createLabel("Номер догоаора"));
        grid.setWidget(0, 1, mContract = createTxtBox(20, SUM_WIDTH));
        grid.setWidget(1, 0, createLabel("Валюта"));
        grid.setWidget(1, 1, mCurrency = createCachedCurrencyListBox(CachedListEnum.Currency.name() ,  "RUR", FIELD2_WIDTH, false, false));
        grid.setWidget(2, 0, createLabel(("Филиал")));
        grid.setWidget(2, 1, mFilial = createCachedFilialListBox(CachedListEnum.Filials.name() , null, FIELD2_WIDTH, false, true));
        grid.setWidget(3, 0, createLabel("Статус"));
        grid.setWidget(3, 1, mStatus = createTxtBox(20, SUM_WIDTH));

//        grid.getCellFormatter().getElement(0, 0).getStyle().setWidth(150, Style.Unit.PX);
//        grid.getElement().getStyle().setMarginBottom(8, Style.Unit.PX);
        vpanel.add(grid);

        return vpanel;
    }

    @Override
    protected boolean onClickOK() throws Exception {
        return false;
    }


    @Override
    public void afterShow() {

    }
}
