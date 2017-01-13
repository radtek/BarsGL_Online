package ru.rbt.barsgl.gwt.client.dict.dlg;

import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.client.comp.enm.EnumListBox;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.utils.AppPredicate;
import ru.rbt.barsgl.gwt.core.utils.DialogUtils;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.dict.StamtUnloadParamWrapper;
import ru.rbt.barsgl.shared.enums.StamtUnloadParamType;
import ru.rbt.barsgl.shared.enums.StamtUnloadParamTypeCheck;

import static ru.rbt.barsgl.gwt.client.dict.StamtUnloadParamDict.*;

/**
 * Created by ER21006 on 19.01.2016.
 */
public class StamtUnloadParamDlg extends EditableDialog<StamtUnloadParamWrapper> {

    public final static String EDIT = "Редактирование настройки";
    public final static String CREATE = "Создание настройки";
    public final static String DELETE = "Удаление настройки";

    private TextBox account;
    private EnumListBox<StamtUnloadParamType> paramTypeListBox;
    private EnumListBox<StamtUnloadParamTypeCheck> paramTypeListBoxCheck;
    private EnumListBox<StamtUnloadParamTypeCheck> paramTypeListBoxCheckBln;

    public StamtUnloadParamDlg(String caption, Columns columns, FormAction action) {
        super(columns, action);
        setCaption(caption);
    }

    @Override
    protected StamtUnloadParamWrapper createWrapper() {
        return new StamtUnloadParamWrapper();
    }

    @Override
    protected void setFields(StamtUnloadParamWrapper cnw) {
        cnw.setAccount(checkAccountLength(checkRequeredString(account.getText(), FIELD_ACC)));
        StamtUnloadParamType parmType = paramTypeListBox.getValue();
        cnw.setParamType(checkRequiredFieldValue(parmType, FIELD_ACC));
        StamtUnloadParamTypeCheck parmTypeCheck = paramTypeListBoxCheck.getValue();
        cnw.setParamTypeCheck(checkRequiredFieldValue(parmTypeCheck, FIELD_ACC));
        cnw.setParamTypeCheckBln(paramTypeListBoxCheckBln.getValue());
    }

    @Override
    public Widget createContent() {
        Grid grid = new Grid(5, 2);
        grid.setWidget(0, 0, new HTML("<b>" + new Label("Настройка:") + "</b>"));
        grid.setWidget(1, 0, new Label(FIELD_ACC));
        grid.setWidget(1, 1, account = createTextBox(20));
        grid.setWidget(2, 0, new Label(FIELD_ACCTYPE));
        grid.setWidget(2, 1, paramTypeListBox = new EnumListBox<>(StamtUnloadParamType.values()));
        grid.setWidget(3, 0, new Label(FIELD_INCLUDE));
        grid.setWidget(3, 1, paramTypeListBoxCheck = new EnumListBox<>(StamtUnloadParamTypeCheck.values()));
        grid.setWidget(4, 0, new Label(FIELD_INCLUDE_BLN));
        grid.setWidget(4, 1, paramTypeListBoxCheckBln = new EnumListBox<>(StamtUnloadParamTypeCheck.values()));

        return grid;
    }

    @Override
    protected void fillContent() {
        if (FormAction.CREATE != action) {
            row = (Row) params;
            int ind = columns.getColumnIndexByCaption(FIELD_ACC);
            if (ind >= 0) {
                account.setText((String) row.getField(ind).getValue());
            }
            ind = columns.getColumnIndexByCaption(FIELD_ACCTYPE);
            if (ind >= 0) {
                paramTypeListBox.setValue(parseParamTypeLabel((String) row.getField(ind).getValue()));
            }
            ind = columns.getColumnIndexByCaption(FIELD_INCLUDE);
            if (ind >= 0) {
                paramTypeListBoxCheck.setValue(parseParamTypeCheckLabel((String) row.getField(ind).getValue()));
            }
            ind = columns.getColumnIndexByCaption(FIELD_INCLUDE_BLN);
            if (ind >= 0) {
                paramTypeListBoxCheckBln.setValue(parseParamTypeCheckLabel((String) row.getField(ind).getValue()));
            }
        }

        if (FormAction.UPDATE == action) {
            prepareForUpdate();
        }
        if (FormAction.DELETE == action) {
            prepareForDelete();
        }

    }

    private static StamtUnloadParamType parseParamTypeLabel(String label) {
        for (StamtUnloadParamType type : StamtUnloadParamType.values()) {
            if (type.getLabel().equals(label)) return type;
        }
        return null;
    }

    private static StamtUnloadParamTypeCheck parseParamTypeCheckLabel(String label) {
        for (StamtUnloadParamTypeCheck type : StamtUnloadParamTypeCheck.values()) {
            if (type.getLabel().equals(label)) return type;
        }
        return null;
    }

    private void prepareForUpdate() {
        account.setReadOnly(true);
        paramTypeListBox.setEnabled(false);
        paramTypeListBoxCheck.setEnabled(true);
    }

    private void prepareForDelete() {
        account.setReadOnly(true);
        paramTypeListBox.setEnabled(false);
        paramTypeListBoxCheck.setEnabled(false);
    }

    private String checkAccountLength(String account) {
        final int size = StamtUnloadParamType.A == paramTypeListBox.getValue() ? 20 : 5;
        final boolean isDigits = null != account && account.matches("\\d{"+ size +"}");
        return DialogUtils.check(account, FIELD_ACC, "Строка должна содержать только цифры.\nДлина строки должна быть " + size
                , new AppPredicate<String>() {
                    @Override
                    public boolean check(String target) {
                        return isDigits;
                    }
                });
    }

}
