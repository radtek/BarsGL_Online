package ru.rbt.barsgl.gwt.client.quickFilter;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Created by ER18837 on 29.03.16.
 */
public class DateQuickFilterDlg extends DateIntervalQuickFilterDlg {

    private RadioButton[] choiceButton;
    private DateQuickFilterParams filterParams;

    public DateQuickFilterDlg() {
        super();
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


}
