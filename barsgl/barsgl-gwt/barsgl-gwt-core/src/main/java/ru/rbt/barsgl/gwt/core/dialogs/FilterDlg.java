package ru.rbt.barsgl.gwt.core.dialogs;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;

import java.util.ArrayList;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;

/**
 * Created by akichigi on 01.04.15.
 */
public class FilterDlg extends DlgFrame {

    private FilterFrame filterFrame;
    private boolean allowNullValue = false;

    public FilterDlg() {
        super();
        setCaption("Фильтр");
        InitFrame();
    }

    private void InitFrame(){
        ok.setText(TEXT_CONSTANTS.applyBtn_caption());
        btnPanel.insert(createClearButton(), btnPanel.getWidgetCount() - 1);
    }

    private Button createClearButton() {
        cancel = new Button(TEXT_CONSTANTS.filterFrm_clear());

        cancel.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                filterFrame.clearAllFilterCriteria();
            }
        });

        cancel.addStyleName("dlg-button");

        return cancel;
    }

    @Override
    public Widget createContent() {
        return filterFrame = new FilterFrame();
    }

    @Override
    protected void fillContent(){
        GridWidget grid = (GridWidget)params;
        filterFrame.fillContent(grid.getTable().getColumns(), grid.getFilterCriteria());
    }

    @Override
    public boolean onClickOK() throws Exception {
        params = filterFrame.getFilterConditions();
        if ((params != null) && (!allowNullValue)) checkForNullValues();
        return true;
    }

    private void checkForNullValues() throws Exception {
        for(FilterItem item: (ArrayList<FilterItem>)params){
           if (item.needValue() && item.getValue() == null){
               throw new Exception("Значения полей фильтра должны быть заполнены.");
           }
        }
    }

    public void setAllowNullValue(boolean allow){
        allowNullValue = allow;
    }
}
