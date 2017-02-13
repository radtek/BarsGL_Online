package ru.rbt.barsgl.gwt.client.dict.dlg;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.security.gwt.client.operday.IDataConsumer;
import ru.rbt.security.gwt.client.operday.OperDayGetter;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.ui.DatePickerBox;
import ru.rbt.barsgl.gwt.core.ui.ValuesBox;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;

import java.util.Date;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.createDateBox;
import static ru.rbt.barsgl.gwt.client.comp.GLComponents.createLabel;
import static ru.rbt.barsgl.gwt.client.dict.dlg.OfrQuickFilterParams.SymbolState.*;
import static ru.rbt.security.gwt.client.operday.OperDayGetter.getOperday;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;

/**
 * Created by ER18837 on 18.01.16.
 */
public class OfrQuickFilterDlg extends DlgFrame {

    private DatePickerBox mDate;
    private ValuesBox mState;

    private Date operday;
    private Date prevday;

    public OfrQuickFilterDlg() {
        super();
        ok.setText(TEXT_CONSTANTS.applyBtn_caption());
        cancel.setText(TEXT_CONSTANTS.btn_cancel());
        setCaption("Быстрый фильтр для символов ОФР");
    }

    @Override
    public Widget createContent() {

        VerticalPanel mainVP = new VerticalPanel();

        Grid g1 = new Grid(2, 2);
        mainVP.add(g1);
        g1.setWidget(0, 0, createLabel("Дата:"));
        g1.setWidget(0, 1, mDate = createDateBox());
        g1.setWidget(1, 0, createLabel("Состояние:"));
        g1.setWidget(1, 1, mState = createStateBox());
        mState.setWidth("150px");
        mDate.setWidth("150px");

        getOperday(new IDataConsumer<OperDayWrapper>() {
            @Override
            public void accept(OperDayWrapper wrapper) {
                operday = DateTimeFormat.getFormat(OperDayGetter.dateFormat).parse(wrapper.getCurrentOD());
                prevday = DateTimeFormat.getFormat(OperDayGetter.dateFormat).parse(wrapper.getPreviousOD());
            }
        });

        return mainVP;
    }

    @Override
    protected void fillContent() {
        OfrQuickFilterParams filterParams = (OfrQuickFilterParams)params;
        mDate.clear();
        if (null != filterParams) {
            mState.setValue(filterParams.getReportState());
            if (null != filterParams.getVisibleDate())
                mDate.setValue(filterParams.getVisibleDate());
        }
    }

    private ValuesBox createStateBox(){
        ValuesBox listBox = new ValuesBox();
        listBox.addItem(OfrQuickFilterParams.SymbolState.All, "Все");
        listBox.addItem(Active, "Действующие");
        listBox.addItem(Inactive, "Недействующие");
        return listBox;
    }

    @Override
    protected boolean onClickOK() throws Exception {
    	OfrQuickFilterParams.SymbolState state = (OfrQuickFilterParams.SymbolState)mState.getValue();
    	Date reportDate = mDate.getValue();
        if (null == reportDate && !state.equals(All)) {
        	reportDate = operday;
        }
        params = new OfrQuickFilterParams(mDate.getValue(), reportDate, state);
        return true;
    }


}
