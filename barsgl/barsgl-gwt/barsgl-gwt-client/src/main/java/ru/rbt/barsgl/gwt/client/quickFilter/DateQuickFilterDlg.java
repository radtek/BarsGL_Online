package ru.rbt.barsgl.gwt.client.quickFilter;

import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.ui.DatePickerBox;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.createDateBox;
import static ru.rbt.barsgl.gwt.client.comp.GLComponents.createLabel;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;

/**
 * Created by ER18837 on 29.03.16.
 */
public class DateQuickFilterDlg extends DlgFrame {

    protected DatePickerBox mDateBegin;
    protected DatePickerBox mDateEnd;
    private RadioButton[] choiceButton;
    private DateQuickFilterParams filterParams;

    public DateQuickFilterDlg() {
        super();
        ok.setText(TEXT_CONSTANTS.applyBtn_caption());
        cancel.setText(TEXT_CONSTANTS.btn_cancel());
        setCaption("Выбор периода просмотра");
    }

    @Override
    public Widget createContent() {

        VerticalPanel mainVP = new VerticalPanel();

        mainVP.add(createChoicePanel());
        mainVP.add(createDatePanel());

        return mainVP;
    }

    @Override
    protected void fillContent() {
        filterParams = (DateQuickFilterParams)params;

        mDateBegin.clear();
        mDateEnd.clear();
        if (null != filterParams) {
            mDateBegin.setValue(filterParams.getDateBegin());
            mDateEnd.setValue(filterParams.getDateEnd());
            for (DateQuickFilterParams.DateFilterField field: DateQuickFilterParams.DateFilterField.values()) {
                choiceButton[field.ordinal()].setVisible(null != field.getColumn());
            }
            choiceButton[filterParams.getFilterField().ordinal()].setValue(true);
        } 
    }

    @Override
    protected boolean onClickOK() throws Exception {
    	DateQuickFilterParams.DateFilterField filterField = DateQuickFilterParams.DateFilterField.CREATE_DATE;
    	int i = 0;
        for (RadioButton button: choiceButton) {
        	if (button.getValue()) {
        		filterField = DateQuickFilterParams.DateFilterField.values()[i];
        		break;
        	}
        	i++;
        }
        if (null != filterParams) {
            filterParams.setDateBegin(mDateBegin.getValue());
            filterParams.setDateEnd(mDateEnd.getValue());
            filterParams.setFilterField(filterField);
        } 
        params = filterParams;
        return true;
    }

    private Widget createChoicePanel() {
        final String choiceGroupName = "dateChoice";
        HorizontalPanel panel = new HorizontalPanel();
        panel.setSpacing(10);
        choiceButton = new RadioButton[DateQuickFilterParams.DateFilterField.values().length];
        for (DateQuickFilterParams.DateFilterField field: DateQuickFilterParams.DateFilterField.values()) {
            panel.add(choiceButton[field.ordinal()] = new RadioButton(choiceGroupName, field.getTitle()));
        }
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
