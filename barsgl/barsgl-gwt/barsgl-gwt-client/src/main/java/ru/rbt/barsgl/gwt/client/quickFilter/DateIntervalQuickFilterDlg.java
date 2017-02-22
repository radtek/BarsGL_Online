package ru.rbt.barsgl.gwt.client.quickFilter;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.ui.DatePickerBox;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.createDateBox;
import static ru.rbt.barsgl.gwt.client.comp.GLComponents.createLabel;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;

/**
 * Created by akichigi on 03.06.16.
 */
public class DateIntervalQuickFilterDlg extends DlgFrame {
    protected DatePickerBox mDateBegin;
    protected DatePickerBox mDateEnd;

    private DateIntervalQuickFilterParams filterParams;

    public DateIntervalQuickFilterDlg() {
        super();
        ok.setText(TEXT_CONSTANTS.applyBtn_caption());
        cancel.setText(TEXT_CONSTANTS.btn_cancel());
        setCaption("Выбор периода просмотра");
    }

       @Override
    public Widget createContent() {

        SimplePanel panel = new SimplePanel();

        panel.add(createDatePanel());

        return panel;
    }

    @Override
    protected void fillContent() {
        filterParams = (DateIntervalQuickFilterParams)params;

        mDateBegin.clear();
        mDateEnd.clear();
        if (null != filterParams) {
            mDateBegin.setValue(filterParams.getDateBegin());
            mDateEnd.setValue(filterParams.getDateEnd());
        }
    }

    @Override
    protected boolean onClickOK() throws Exception {
        if (null != filterParams) {
            filterParams.setDateBegin(mDateBegin.getValue());
            filterParams.setDateEnd(mDateEnd.getValue());
        }
        params = filterParams;
        return true;
    }

    protected Widget createDatePanel() {
        Grid g1 = new Grid(1, 4);
        g1.setWidget(0, 0, createLabel("Дата начала"));
        g1.setWidget(0, 1, mDateBegin = createDateBox());
        g1.setWidget(0, 2, createLabel("Дата окончания"));
        g1.setWidget(0, 3, mDateEnd = createDateBox());
        return g1;
    }

}
