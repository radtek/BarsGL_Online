package ru.rbt.barsgl.gwt.client.dict.dlg;

import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.client.dict.AccType;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterShowEvent;
import ru.rbt.barsgl.gwt.core.ui.AreaBox;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.dict.AccTypeWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.BoolType;

import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

/**
 * Created by akichigi on 23.08.16.
 */
public class AccTypeDlg extends EditableDialog<AccTypeWrapper> implements IAfterShowEvent {
    public final static String EDIT = "Редактирование AccType";
    public final static String CREATE = "Ввод нового AccType";
    public final static String DELETE = "Удаление AccType";
    private final static String postfix = " - раздел {0}, продукт {1}, подпродукт {2}, модификатор {3}";
    private TxtBox code;
    private AreaBox name;
    private CheckBox pl_act;
    private CheckBox fl_ctrl;
    private String section;
    private String product;
    private String subproduct;
    private String modifier;
    private String caption;

    public AccTypeDlg() {
        setAfterShowEvent(this);
    }

    @Override
    public Widget createContent() {
        Grid grid = new Grid(2, 2);
        grid.setWidget(0, 0, new Label(AccType.FIELD_ACCTYPE));

        grid.setWidget(0, 1, code = new TxtBox());
        code.setMaxLength(9);
        code.setWidth("70px");
        code.setMask("[0-9]");
        code.setReadOnly(true);

        grid.setWidget(1, 0, new Label(AccType.FIELD_ACCTYPENAME));
        grid.setWidget(1, 1, name = new AreaBox());
        name.setHeight("50px");
        name.setWidth("370px");

        pl_act = new CheckBox(AccType.FIELD_PL_ACT);
        fl_ctrl = new CheckBox(AccType.FIELD_FL_CTRL);

        VerticalPanel panel = new VerticalPanel();
        panel.add(grid);
        panel.add(pl_act);
        panel.add(fl_ctrl);

        return panel;
    }

    @Override
    public void clearContent(){
        code.clear();
        name.clear();
        name.setReadOnly(false);
        pl_act.setValue(false);
        pl_act.setEnabled(true);
        fl_ctrl.setValue(false);
        fl_ctrl.setEnabled(true);
    }

    @Override
    protected void fillContent() {
        row = (Row) params;
        caption = getCaption();
        int ind;

        section = (String)getIntiParams()[0];
        product = (String)getIntiParams()[1];
        subproduct = (String)getIntiParams()[2];
        modifier = (String)getIntiParams()[3];

        setCaption(Utils.Fmt(caption + postfix, section, product, subproduct, modifier));
        if (action == FormAction.CREATE){
            code.setValue(section + product + subproduct + modifier);
        } else
        if (action == FormAction.UPDATE || action == FormAction.DELETE) {
            ind = columns.getColumnIndexByCaption(AccType.FIELD_ACCTYPE);
            if (ind >= 0) {
                code.setValue((String) row.getField(ind).getValue());
            }
            ind = columns.getColumnIndexByCaption(AccType.FIELD_ACCTYPENAME);
            if (ind >= 0) {
                name.setValue((String) row.getField(ind).getValue());
            }
            ind = columns.getColumnIndexByCaption(AccType.FIELD_PL_ACT);
            if (ind >= 0) {
                pl_act.setValue(BoolType.Y.name().equals(row.getField(ind).getValue()));
            }
            ind = columns.getColumnIndexByCaption(AccType.FIELD_FL_CTRL);
            if (ind >= 0) {
                fl_ctrl.setValue(BoolType.Y.name().equals(row.getField(ind).getValue()));
            }
        }

        if (action == FormAction.DELETE) {
            name.setReadOnly(true);
            pl_act.setEnabled(false);
            fl_ctrl.setEnabled(false);
        }
    }


    @Override
    protected AccTypeWrapper createWrapper() {
        return new AccTypeWrapper();
    }

    @Override
    protected void setFields(AccTypeWrapper cnw) {
        cnw.setSection(section);
        cnw.setProduct(product);
        cnw.setSubProduct(subproduct);
        cnw.setModifier(modifier);
        cnw.setAcctype(checkRequeredString(code.getValue(), AccType.FIELD_ACCTYPE));

        checkRequeredString(name.getValue(), AccType.FIELD_ACCTYPENAME);
        try{
            if (name.getValue().length() > 255) throw new Exception("Количество символов превышает 255");
        }catch(Exception e){
            showInfo("Ошибка", Utils.Fmt("Неверное значение в поле {0}. {1}", AccType.FIELD_ACCTYPENAME, e.getMessage()));
            throw new IllegalArgumentException("column");
        }
        cnw.setAcctypeName(name.getValue());
        cnw.setFl_ctrl(fl_ctrl.getValue() ? BoolType.Y : BoolType.N);
        cnw.setPl_act(pl_act.getValue() ? BoolType.Y : BoolType.N);
    }

    @Override
    public void afterShow() {
        name.setFocus(action == FormAction.CREATE ||action == FormAction.UPDATE);
    }
}
