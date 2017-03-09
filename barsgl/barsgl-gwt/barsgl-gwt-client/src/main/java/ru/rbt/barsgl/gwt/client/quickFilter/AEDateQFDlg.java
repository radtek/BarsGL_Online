package ru.rbt.barsgl.gwt.client.quickFilter;

import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.ui.DatePickerBox;

import ru.rbt.barsgl.gwt.core.comp.Components;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;

/**
 * Created by akichigi on 02.08.16.
 */
public class AEDateQFDlg extends DlgFrame {
    protected DatePickerBox mDateBegin;
    protected DatePickerBox mDateEnd;
    private RadioButton[] choiceButton;
    private AEDateQFParams filterParams;

    public AEDateQFDlg() {
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
        filterParams = (AEDateQFParams)params;

        mDateBegin.clear();
        mDateEnd.clear();
        if (null != filterParams) {
            mDateBegin.setValue(filterParams.getDateBegin());
            mDateEnd.setValue(filterParams.getDateEnd());
            for (AEDateQFParams.DateFilterField field: AEDateQFParams.DateFilterField.values()) {
                choiceButton[field.ordinal()].setVisible(null != field.getColumn());
            }
            choiceButton[filterParams.getFilterField().ordinal()].setValue(true);
        }
    }

    @Override
    protected boolean onClickOK() throws Exception {
        AEDateQFParams.DateFilterField filterField = AEDateQFParams.DateFilterField.PKG_DATE;
        int i = 0;
        for (RadioButton button: choiceButton) {
            if (button.getValue()) {
                filterField = AEDateQFParams.DateFilterField.values()[i];
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
        choiceButton = new RadioButton[AEDateQFParams.DateFilterField.values().length];
        for (AEDateQFParams.DateFilterField field: AEDateQFParams.DateFilterField.values()) {
            panel.add(choiceButton[field.ordinal()] = new RadioButton(choiceGroupName, field.getTitle()));
        }
        return panel;
    }

    private Widget createDatePanel() {
        Grid g1 = new Grid(1, 4);
        g1.setWidget(0, 0, Components.createLabel("Дата начала"));
        g1.setWidget(0, 1, mDateBegin = Components.createDateBox());
        g1.setWidget(0, 2, Components.createLabel("Дата окончания"));
        g1.setWidget(0, 3, mDateEnd = Components.createDateBox());
        return g1;
    }
}
