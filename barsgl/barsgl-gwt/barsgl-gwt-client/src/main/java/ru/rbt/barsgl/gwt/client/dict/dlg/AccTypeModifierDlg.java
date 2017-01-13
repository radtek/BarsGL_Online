package ru.rbt.barsgl.gwt.client.dict.dlg;

import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.client.dict.AccTypeModifier;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterShowEvent;
import ru.rbt.barsgl.gwt.core.ui.AreaBox;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.dict.AccTypeModifierWrapper;
import ru.rbt.barsgl.shared.dict.AccTypeWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.BoolType;

import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

/**
 * Created by akichigi on 19.08.16.
 */
public class AccTypeModifierDlg extends EditableDialog<AccTypeModifierWrapper> implements IAfterShowEvent {
    public final static String EDIT = "Редактирование модификатора";
    public final static String CREATE = "Ввод нового модификатора";
    public final static String DELETE = "Удаление модификатора";
    private final static String postfix = " - раздел {0}, продукт {1}, подпродукт {2}";
    private TxtBox code;
    private AreaBox name;
    private String section;
    private String product;
    private String subproduct;
    private String caption;

    public AccTypeModifierDlg() {
        setAfterShowEvent(this);
    }

    @Override
    public Widget createContent() {
        Grid grid = new Grid(2, 2);
        grid.setWidget(0, 0, new Label(AccTypeModifier.FIELD_MODIFIER));

        grid.setWidget(0, 1, code = new TxtBox());
        code.setMaxLength(2);
        code.setWidth("30px");
        code.setMask("[0-9]");

        grid.setWidget(1, 0, new Label(AccTypeModifier.FIELD_MODIFNAME));
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
        caption = getCaption();
        int ind;

        section = (String)getIntiParams()[0];
        product = (String)getIntiParams()[1];
        subproduct = (String)getIntiParams()[2];

        setCaption(Utils.Fmt(caption + postfix, section, product, subproduct));

        if (action == FormAction.UPDATE || action == FormAction.DELETE) {
            ind = columns.getColumnIndexByCaption(AccTypeModifier.FIELD_MODIFIER);
            if (ind >= 0) {
                code.setValue((String) row.getField(ind).getValue());
            }
            ind = columns.getColumnIndexByCaption(AccTypeModifier.FIELD_MODIFNAME);
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
    protected AccTypeModifierWrapper createWrapper() {
        return new AccTypeModifierWrapper();
    }

    @Override
    protected void setFields(AccTypeModifierWrapper cnw) {
        checkRequeredString(code.getValue(), AccTypeModifier.FIELD_MODIFIER);
        try{
            int val = Integer.parseInt(code.getValue());
            if (val < 0) throw new Exception("Неверный код");
        }catch(Exception e){
            showInfo("Ошибка", Utils.Fmt("Неверное значение в поле {0}. {1}", AccTypeModifier.FIELD_MODIFIER, e.getMessage()));
            throw new IllegalArgumentException("column");
        }

        checkRequeredString(name.getValue(), AccTypeModifier.FIELD_MODIFNAME);
        try{
            if (name.getValue().length() > 255) throw new Exception("Количество символов превышает 255");
        }catch(Exception e){
            showInfo("Ошибка", Utils.Fmt("Неверное значение в поле {0}. {1}", AccTypeModifier.FIELD_MODIFNAME, e.getMessage()));
            throw new IllegalArgumentException("column");
        }

        cnw.setSection(section);
        cnw.setProduct(product);
        cnw.setSubProduct(subproduct);
        cnw.setModifier(Utils.fillUp(code.getValue(), 2));
        cnw.setModifierName(name.getValue());
    }

    @Override
    public void afterShow() {
        code.setFocus(action == FormAction.CREATE);
        name.setFocus(action == FormAction.UPDATE);
    }
}
