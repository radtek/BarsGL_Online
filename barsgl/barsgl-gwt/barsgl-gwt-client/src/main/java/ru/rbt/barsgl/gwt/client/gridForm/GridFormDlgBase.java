package ru.rbt.barsgl.gwt.client.gridForm;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.IDlgEvents;

import java.util.HashMap;

/**
 * Created by ER18837 on 10.12.15.
 */
public abstract class GridFormDlgBase extends DlgFrame implements IDlgEvents {
    private final static String OK_CAPTION = "Выбрать";

    private DockLayoutPanel dataPanel;
    protected GridForm gridForm;

    public GridFormDlgBase(String title) {
        super();
        setCaption(title);
        setDlgEvents(this);
        setOkButtonCaption(OK_CAPTION);
    }

    protected abstract GridForm getGridForm();

    protected abstract boolean setResultList(final HashMap<String, Object> result);

    protected abstract Object[] getInitialFilterParams();
    protected void onSetResult() {};

    protected String getGridWidth() {
        return "600px";
    }

    protected String getGridHeight() {
        return "400px";
    }

    @Override
    public Widget createContent() {
        dataPanel = new DockLayoutPanel(Style.Unit.PCT);
        gridForm = getGridForm();
//        gridForm.getGrid().refresh();
        dataPanel.add(gridForm);
        dataPanel.setWidth(getGridWidth());
        dataPanel.setHeight(getGridHeight());
        return dataPanel;
    }

    @Override
    protected void fillContent() {
        super.fillContent();
        gridForm.getGrid().refresh();
    }

    @Override
    protected boolean onClickOK() throws Exception {
        Row row = gridForm.getGrid().getCurrentRow();
        if (null != row && row.getFieldsCount() > 0) {
            HashMap<String, Object> paramsMap = new HashMap<String, Object> (row.getFieldsCount());
            for (int i = 0; i < row.getFieldsCount(); i++) {
                String colName = gridForm.getTable().getColumns().getColumnByIndex(i).getName();
                paramsMap.put(colName, row.getField(i).getValue());
            }
            params = paramsMap;
            return true;
        }
        return false;
    }

    @Override
    public void onDlgOkClick(Object prms) {
        if (setResultList((HashMap<String, Object>) prms)) {
            onSetResult();
            this.hide();
        }
    }
}
