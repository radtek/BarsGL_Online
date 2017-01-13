package ru.rbt.barsgl.gwt.client.quickFilter;

import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.ui.DatePickerBox;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.createDateBox;
import static ru.rbt.barsgl.gwt.client.comp.GLComponents.createLabel;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;

/**
 * Created by ER18837 on 27.04.16.
 */
public class AuditQuickFilterDlg extends DlgFrame {

    protected DatePickerBox mDateBegin;
    protected DatePickerBox mDateEnd;
    private CheckBox chkOnlyError;
    private AuditQuickFilterParams filterParams;

    public AuditQuickFilterDlg() {
        super();
        ok.setText(TEXT_CONSTANTS.applyBtn_caption());
        cancel.setText(TEXT_CONSTANTS.btn_cancel());
        setCaption("Выбор периода просмотра");
    }

    @Override
    public Widget createContent() {

        VerticalPanel mainVP = new VerticalPanel();

        mainVP.add(createDatePanel());
        mainVP.add(createCheckPanel());

        return mainVP;
    }

    @Override
    protected void fillContent() {
        filterParams = (AuditQuickFilterParams)params;

        mDateBegin.clear();
        mDateEnd.clear();
        if (null != filterParams) {
            mDateBegin.setValue(filterParams.getDateBegin());
            mDateEnd.setValue(filterParams.getDateEnd());
            chkOnlyError.setValue(filterParams.isOnlyErroros());
        }
    }

    @Override
    protected boolean onClickOK() throws Exception {
        if (null != filterParams) {
            filterParams.setDateBegin(mDateBegin.getValue());
            filterParams.setDateEnd(mDateEnd.getValue());
            filterParams.setOnlyErroros(chkOnlyError.getValue());
        }
        params = filterParams;
        return true;
    }

    private Widget createCheckPanel() {
        HorizontalPanel panel = new HorizontalPanel();
        panel.setSpacing(10);
        chkOnlyError = new CheckBox("Только ошибки");
        panel.add(chkOnlyError);
        return panel;
    }

    private Widget createDatePanel() {
        Grid g1 = new Grid(1, 4);
        g1.setWidget(0, 0, createLabel("Дата начала"));
        g1.setWidget(0, 1, mDateBegin = createDateBox());
        g1.setWidget(0, 2, createLabel("Дата окончания"));
        g1.setWidget(0, 3, mDateEnd = createDateBox());
        return g1;
    }
}
