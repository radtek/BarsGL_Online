package ru.rbt.barsgl.gwt.client.quickFilter;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.client.comp.DataListBox;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.ui.DatePickerBox;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.createDateBox;
import static ru.rbt.barsgl.gwt.client.comp.GLComponents.createDealSourceListBox;
import static ru.rbt.barsgl.gwt.client.comp.GLComponents.createLabel;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.ifEmpty;

/**
 * Created by akichigi on 21.02.17.
 */
public class ErrorQFilterDlg extends DlgFrame {
    private ErrorQFilterParams filterParams;
    private DataListBox dealSource;
    private DatePickerBox operDay;

    public ErrorQFilterDlg(){
        super();
        ok.setText(TEXT_CONSTANTS.applyBtn_caption());
        cancel.setText(TEXT_CONSTANTS.btn_cancel());
        setCaption("Выбор параметров обработки ошибок");
    }

    @Override
    public Widget createContent(){
        Grid grid = new Grid(2, 2);
        grid.setWidget(0, 0, createLabel("Источник сделки", "120px"));
        grid.setWidget(0, 1, dealSource = createDealSourceListBox("", "105px"));
        grid.setWidget(1, 0, createLabel("Дата опердня"));
        grid.setWidget(1, 1, operDay = createDateBox());

        return grid;
    }

    @Override
    protected void fillContent(){
        filterParams = (ErrorQFilterParams) params;
        if (null != filterParams){
            dealSource.setSelectValue(ifEmpty(filterParams.getDealSource(), ""));
            operDay.setValue(filterParams.getOperDay());
        }
    }

    @Override
    protected boolean onClickOK() throws Exception {
        if (null != filterParams) {
            filterParams.setDealSource((String) dealSource.getValue());
            filterParams.setOperDay(operDay.getValue());
        }
        params = filterParams;
        return true;
    }
}
