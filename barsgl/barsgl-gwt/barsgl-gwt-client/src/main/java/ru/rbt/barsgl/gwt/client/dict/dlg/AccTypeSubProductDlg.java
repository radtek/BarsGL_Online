package ru.rbt.barsgl.gwt.client.dict.dlg;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.client.dict.AccTypeSubProduct;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterShowEvent;
import ru.rbt.barsgl.gwt.core.ui.AreaBox;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.dict.AccTypeSubProductWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;

import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

/**
 * Created by akichigi on 15.08.16.
 */
public class AccTypeSubProductDlg extends EditableDialog<AccTypeSubProductWrapper> implements IAfterShowEvent {
    public final static String EDIT = "Редактирование подпродукта";
    public final static String CREATE = "Ввод нового подпродукта";
    public final static String DELETE = "Удаление подпродукта";
    private final static String postfix = " - раздел {0}, продукт {1}";
    private TxtBox code;
    private AreaBox name;
    private String section;
    private String product;
    private String caption;

    public AccTypeSubProductDlg() {
        setAfterShowEvent(this);
    }

    @Override
    public Widget createContent() {
        Grid grid = new Grid(2, 2);
        grid.setWidget(0, 0, new Label(AccTypeSubProduct.FIELD_SUBPRODUCT));

        grid.setWidget(0, 1, code = new TxtBox());
        code.setMaxLength(2);
        code.setWidth("30px");
        code.setMask("[0-9]");

        grid.setWidget(1, 0, new Label(AccTypeSubProduct.FIELD_NAME));
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
        setCaption(Utils.Fmt(caption + postfix, section, product));

        if (action == FormAction.UPDATE || action == FormAction.DELETE) {
            ind = columns.getColumnIndexByCaption(AccTypeSubProduct.FIELD_SUBPRODUCT);
            if (ind >= 0) {
                code.setValue((String) row.getField(ind).getValue());
            }
            ind = columns.getColumnIndexByCaption(AccTypeSubProduct.FIELD_NAME);
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
    protected AccTypeSubProductWrapper createWrapper() {
        return new AccTypeSubProductWrapper();
    }

    @Override
    protected void setFields(AccTypeSubProductWrapper cnw) {
        checkRequeredString(code.getValue(), AccTypeSubProduct.FIELD_SUBPRODUCT);
        try{
            int val = Integer.parseInt(code.getValue());
            if (val < 0) throw new Exception("Неверный код");
        }catch(Exception e){
            showInfo("Ошибка", Utils.Fmt("Неверное значение в поле {0}. {1}", AccTypeSubProduct.FIELD_SUBPRODUCT, e.getMessage()));
            throw new IllegalArgumentException("column");
        }

        checkRequeredString(name.getValue(), AccTypeSubProduct.FIELD_NAME);
        try{
            if (name.getValue().length() > 255) throw new Exception("Количество символов превышает 255");
        }catch(Exception e){
            showInfo("Ошибка", Utils.Fmt("Неверное значение в поле {0}. {1}",AccTypeSubProduct.FIELD_NAME, e.getMessage()));
            throw new IllegalArgumentException("column");
        }

        cnw.setSection(section);
        cnw.setProduct(product);
        cnw.setSubProduct(Utils.fillUp(code.getValue(), 2));
        cnw.setSubProductName(name.getValue());
    }

    @Override
    public void afterShow() {
        code.setFocus(action == FormAction.CREATE);
        name.setFocus(action == FormAction.UPDATE);
    }
}
