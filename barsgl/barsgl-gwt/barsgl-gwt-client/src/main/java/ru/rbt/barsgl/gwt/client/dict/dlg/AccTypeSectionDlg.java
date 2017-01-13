package ru.rbt.barsgl.gwt.client.dict.dlg;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.client.dict.AccTypeSection;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterShowEvent;
import ru.rbt.barsgl.gwt.core.ui.AreaBox;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.dict.AccTypeSectionWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;

import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

/**
 * Created by akichigi on 12.08.16.
 */
public class AccTypeSectionDlg extends EditableDialog<AccTypeSectionWrapper> implements IAfterShowEvent {
    public final static String EDIT = "Редактирование раздела";
    public final static String CREATE = "Ввод нового раздела";
    public final static String DELETE = "Удаление раздела";
    private TxtBox code;
    private AreaBox name;

    public AccTypeSectionDlg(){
        setAfterShowEvent(this);
    }

    @Override
    public Widget createContent() {
        Grid grid = new Grid(2, 2);
        grid.setWidget(0, 0, new Label(AccTypeSection.FIELD_SECTION));

        grid.setWidget(0, 1, code = new TxtBox());
        code.setMaxLength(3);
        code.setWidth("30px");
        code.setMask("[0-9]");

        grid.setWidget(1, 0, new Label(AccTypeSection.FIELD_NAME));
        grid.setWidget(1, 1, name = new AreaBox());
        name.setHeight("50px");
        name.setWidth("370px");

        return grid;
    }

    @Override
    public void clearContent(){
        code.clear();
        code.setReadOnly(false);
        name.clear();
        name.setReadOnly(false);
    }

    @Override
    protected void fillContent() {
        row = (Row) params;

        if (action == FormAction.UPDATE || action == FormAction.DELETE) {
            int ind = columns.getColumnIndexByCaption(AccTypeSection.FIELD_SECTION);
            if (ind >= 0) {
                code.setValue((String) row.getField(ind).getValue());
            }
            ind = columns.getColumnIndexByCaption(AccTypeSection.FIELD_NAME);
            if (ind >= 0) {
                name.setValue((String) row.getField(ind).getValue());
            }

            code.setReadOnly(true);
        }
        if (action == FormAction.DELETE) {
            name.setReadOnly(true);
        }
    }

    @Override
    protected AccTypeSectionWrapper createWrapper() {
        return new AccTypeSectionWrapper();
    }

    @Override
    protected void setFields(AccTypeSectionWrapper cnw) {
        checkRequeredString(code.getValue(), AccTypeSection.FIELD_SECTION);
        try{
            int val = Integer.parseInt(code.getValue());
            if (val < 1) throw new Exception("Неверный код");
        }catch(Exception e){
            showInfo("Ошибка", Utils.Fmt("Неверное значение в поле {0}. {1}", AccTypeSection.FIELD_SECTION, e.getMessage()));
            throw new IllegalArgumentException("column");
        }

        checkRequeredString(name.getValue(), AccTypeSection.FIELD_NAME);
        try{
            if (name.getValue().length() > 255) throw new Exception("Количество символов превышает 255");
        }catch(Exception e){
            showInfo("Ошибка", Utils.Fmt("Неверное значение в поле {0}. {1}", AccTypeSection.FIELD_NAME, e.getMessage()));
            throw new IllegalArgumentException("column");
        }
        cnw.setSection(Utils.fillUp(code.getValue(), 3));
        cnw.setSectionName(name.getValue());
    }

    @Override
    public void afterShow() {
        code.setFocus(action == FormAction.CREATE);
        name.setFocus(action == FormAction.UPDATE);
    }
}
