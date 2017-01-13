package ru.rbt.barsgl.gwt.client.dict.dlg;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.client.dict.AccTypeProduct;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterShowEvent;
import ru.rbt.barsgl.gwt.core.ui.AreaBox;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.dict.AccTypeProductWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;

import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

/**
 * Created by akichigi on 15.08.16.
 */
public class AccTypeProductDlg extends EditableDialog<AccTypeProductWrapper> implements IAfterShowEvent {
    public final static String EDIT = "Редактирование продукта";
    public final static String CREATE = "Ввод нового продукта";
    public final static String DELETE = "Удаление продукта";
    private final static String postfix = " - раздел {0}";
    private TxtBox code;
    private AreaBox name;
    private String section;
    private String caption;

    public AccTypeProductDlg() {
        setAfterShowEvent(this);
    }

    @Override
    public Widget createContent() {
        Grid grid = new Grid(2, 2);
        grid.setWidget(0, 0, new Label(AccTypeProduct.FIELD_PRODUCT));

        grid.setWidget(0, 1, code = new TxtBox());
        code.setMaxLength(2);
        code.setWidth("30px");
        code.setMask("[0-9]");

        grid.setWidget(1, 0, new Label(AccTypeProduct.FIELD_NAME));
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
        setCaption(Utils.Fmt(caption + postfix, section));

        if (action == FormAction.UPDATE || action == FormAction.DELETE) {
            ind = columns.getColumnIndexByCaption(AccTypeProduct.FIELD_PRODUCT);
            if (ind >= 0) {
                code.setValue((String) row.getField(ind).getValue());
            }
            ind = columns.getColumnIndexByCaption(AccTypeProduct.FIELD_NAME);
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
    protected AccTypeProductWrapper createWrapper() {
        return new AccTypeProductWrapper();
    }

    @Override
    protected void setFields(AccTypeProductWrapper cnw) {
        checkRequeredString(code.getValue(), AccTypeProduct.FIELD_PRODUCT);
        try{
            int val = Integer.parseInt(code.getValue());
            if (val < 1) throw new Exception("Неверный код");
        }catch(Exception e){
            showInfo("Ошибка", Utils.Fmt("Неверное значение в поле {0}. {1}", AccTypeProduct.FIELD_PRODUCT, e.getMessage()));
            throw new IllegalArgumentException("column");
        }

        checkRequeredString(name.getValue(), AccTypeProduct.FIELD_NAME);
        try{
            if (name.getValue().length() > 255) throw new Exception("Количество символов превышает 255");
        }catch(Exception e){
            showInfo("Ошибка", Utils.Fmt("Неверное значение в поле {0}. {1}", AccTypeProduct.FIELD_NAME, e.getMessage()));
            throw new IllegalArgumentException("column");
        }

        cnw.setSection(section);
        cnw.setProduct(Utils.fillUp(code.getValue(), 2));
        cnw.setProductName(name.getValue());
    }

    @Override
    public void afterShow() {
        code.setFocus(action == FormAction.CREATE);
        name.setFocus(action == FormAction.UPDATE);
    }
}
