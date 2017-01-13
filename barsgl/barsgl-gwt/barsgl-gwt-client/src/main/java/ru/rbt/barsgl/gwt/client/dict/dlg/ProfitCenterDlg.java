package ru.rbt.barsgl.gwt.client.dict.dlg;

import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.client.dict.ProfitCenter;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.dict.ProfitCenterWrapper;
import ru.rbt.barsgl.shared.enums.BoolType;

/**
 * Created by akichigi on 03.08.16.
 */
public class ProfitCenterDlg extends EditableDialog<ProfitCenterWrapper> {
    public final static String EDIT = "Редактирование профит центра";
    public final static String CREATE = "Ввод нового профит центра";
    public final static String DELETE = "Удаление профит центра";

    private TxtBox _code;
    private TxtBox _name;
    private CheckBox _closed;

    public ProfitCenterDlg(String caption, FormAction action, Columns columns) {
        super(columns, action);
        setCaption(caption);
    }

    @Override
    public Widget createContent() {
        Grid grid = new Grid(4, 2);
        grid.setWidget(0, 0, new HTML("<b>" + new Label("Профит центр:") + "</b>"));

        grid.setWidget(1, 0, new Label(ProfitCenter.FIELD_CODE));
        grid.setWidget(1, 1, _code = new TxtBox());
        _code.setMaxLength(4);
        _code.setVisibleLength(4);

        grid.setWidget(2, 0, new Label(ProfitCenter.FIELD_NAME));
        grid.setWidget(2, 1, _name = new TxtBox());
        _name.setMaxLength(30);
        _name.setVisibleLength(35);

        grid.setWidget(3, 0, _closed = new CheckBox(ProfitCenter.FIELD_CLOSED));

        return grid;
    }

    @Override
    public void clearContent(){
        _code.clear();
        _name.clear();
        _closed.setValue(false);
        _code.setReadOnly(false);
        _name.setReadOnly(false);
        _closed.setEnabled(true);
    }

    @Override
    protected void fillContent() {
        clearContent();
        row = (Row) params;
        int ind = columns.getColumnIndexByCaption(ProfitCenter.FIELD_CODE);
        if (ind >= 0) {
            _code.setValue((String) row.getField(ind).getValue());
        }
        ind = columns.getColumnIndexByCaption(ProfitCenter.FIELD_NAME);
        if (ind >= 0) {
            _name.setValue((String) row.getField(ind).getValue());
        }
        ind = columns.getColumnIndexByCaption(ProfitCenter.FIELD_CLOSED);
        if (ind >= 0) {
            String val = (String) row.getField(ind).getValue();
            _closed.setValue((val != null) && val.toUpperCase().equals(BoolType.Y.name()));
        }
        if (action == FormAction.UPDATE || action == FormAction.DELETE) {
            _code.setReadOnly(true);
        }
        if (action == FormAction.DELETE) {
            _name.setReadOnly(true);
            _closed.setEnabled(false);
        }
    }

    @Override
    protected ProfitCenterWrapper createWrapper() {
        return new ProfitCenterWrapper();
    }

    @Override
    protected void setFields(ProfitCenterWrapper cnw) {
        cnw.setCode(checkRequeredString(_code.getValue(), ProfitCenter.FIELD_CODE));
        cnw.setName(checkRequeredString(_name.getValue(), ProfitCenter.FIELD_NAME));
        cnw.setClosed(_closed.getValue() ? BoolType.Y : BoolType.N);
    }
}
