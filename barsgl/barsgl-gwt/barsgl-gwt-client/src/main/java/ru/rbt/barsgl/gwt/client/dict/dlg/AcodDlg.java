package ru.rbt.barsgl.gwt.client.dict.dlg;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ValueBox;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.client.dict.Acod;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterShowEvent;
import ru.rbt.barsgl.gwt.core.ui.AreaBox;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.gwt.core.ui.ValuesBox;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.dict.AcodWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;

import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

/**
 * Created by akichigi on 24.10.16.
 */
public class AcodDlg extends EditableDialog<AcodWrapper> implements IAfterShowEvent {
    public final static String EDIT = "Редактирование Acod";
    public final static String CREATE = "Ввод нового Acod";

    private TxtBox acod;
    private AreaBox acc2dscr;
   // private TxtBox type;
    private ValuesBox type;
    private AreaBox sqdscr;
    private AreaBox ename;
    private AreaBox rname;

    private Long id;

    public AcodDlg(){
        setAfterShowEvent(this);
    }

    @Override
    public Widget createContent() {
        Grid grid = new Grid(6, 2);

        grid.setWidget(0, 0, new Label(Acod.FIELD_ACOD));
        grid.setWidget(0, 1, acod = new TxtBox());
        acod.setMaxLength(4);
        acod.setWidth("30px");
        acod.setMask("[0-9]");

        grid.setWidget(1, 0, new Label(Acod.FIELD_ACC2DSCR));
        grid.setWidget(1, 1, acc2dscr = new AreaBox());
        acc2dscr.setHeight("50px");
        acc2dscr.setWidth("370px");

        grid.setWidget(2, 0, new Label(Acod.FIELD_TYPE));
      /*  grid.setWidget(2, 1, type = new TxtBox());
        type.setMaxLength(1);
        type.setWidth("10px");*/
        type = new ValuesBox();
        type.addItem(null, "");
        type.addItem("А", "А");
        type.addItem("П", "П");
        type.setWidth("30px");
        grid.setWidget(2, 1, type);

        grid.setWidget(3, 0, new Label(Acod.FIELD_SQDSCR));
        grid.setWidget(3, 1, sqdscr = new AreaBox());
        sqdscr.setHeight("50px");
        sqdscr.setWidth("370px");

        grid.setWidget(4, 0, new Label(Acod.FIELD_ENAME));
        grid.setWidget(4, 1, ename = new AreaBox());
        ename.setHeight("50px");
        ename.setWidth("370px");

        grid.setWidget(5, 0, new Label(Acod.FIELD_RNAME));
        grid.setWidget(5, 1, rname = new AreaBox());
        rname.setHeight("50px");
        rname.setWidth("370px");

        return grid;
    }

    @Override
    public void clearContent(){
        acod.clear();
        acod.setReadOnly(false);
        acc2dscr.clear();
        acc2dscr.setReadOnly(false);
       // type.clear();
        type.setReadOnly(false);
        sqdscr.clear();
        sqdscr.setReadOnly(false);
        ename.clear();
        ename.setReadOnly(false);
        rname.clear();
        rname.setReadOnly(false);
    }

    @Override
    protected AcodWrapper createWrapper() {
        return new AcodWrapper();
    }

    @Override
    protected void fillContent() {
        row = (Row) params;
        id = -1L;
        if (action == FormAction.UPDATE || action == FormAction.DELETE) {
            id = (Long)row.getField(0).getValue();

            int ind = columns.getColumnIndexByCaption(Acod.FIELD_ACOD);
            if (ind >= 0) {
                acod.setValue((String) row.getField(ind).getValue());
            }
            ind = columns.getColumnIndexByCaption(Acod.FIELD_ACC2DSCR);
            if (ind >= 0) {
                acc2dscr.setValue((String) row.getField(ind).getValue());
            }

            ind = columns.getColumnIndexByCaption(Acod.FIELD_TYPE);
            if (ind >= 0) {
                type.setValue((String) row.getField(ind).getValue());
            }

            ind = columns.getColumnIndexByCaption(Acod.FIELD_SQDSCR);
            if (ind >= 0) {
                sqdscr.setValue((String) row.getField(ind).getValue());
            }

            ind = columns.getColumnIndexByCaption(Acod.FIELD_ENAME);
            if (ind >= 0) {
                ename.setValue((String) row.getField(ind).getValue());
            }

            ind = columns.getColumnIndexByCaption(Acod.FIELD_RNAME);
            if (ind >= 0) {
                rname.setValue((String) row.getField(ind).getValue());
            }

            acod.setReadOnly(true);
        }
        if (action == FormAction.DELETE) {
            acc2dscr.setReadOnly(true);
            type.setReadOnly(true);
            sqdscr.setReadOnly(true);
            ename.setReadOnly(true);
            rname.setReadOnly(true);
        }
    }

    @Override
    protected void setFields(AcodWrapper cnw) {
        checkRequeredString(acod.getValue(), Acod.FIELD_ACOD);
        try{
            if (acod.getValue().length() > 4) throw new Exception("Количество символов превышает 4");
        }catch(Exception e){
            showInfo("Ошибка", Utils.Fmt("Неверное значение в поле {0}. {1}", Acod.FIELD_ACOD, e.getMessage()));
            throw new IllegalArgumentException("column");
        }

        if (FormAction.CREATE == action){
            try{
                if (Integer.parseInt(acod.getValue()) < 1000) throw new Exception("Запрещено создавать Acod в диапазоне 0001-0999");
            }catch(Exception e){
                showInfo("Ошибка", Utils.Fmt("Неверное значение в поле {0}. {1}", Acod.FIELD_ACOD, e.getMessage()));
                throw new IllegalArgumentException("column");
            }
        }
        
        checkRequeredString(acc2dscr.getValue(), Acod.FIELD_ACC2DSCR);
        try{
            if (acc2dscr.getValue().length() > 164) throw new Exception("Количество символов превышает 164");
        }catch(Exception e){
            showInfo("Ошибка", Utils.Fmt("Неверное значение в поле {0}. {1}", Acod.FIELD_ACC2DSCR, e.getMessage()));
            throw new IllegalArgumentException("column");
        }

        checkRequeredString((String)type.getValue(), Acod.FIELD_TYPE);
       /* checkRequeredString(type.getValue(), Acod.FIELD_TYPE);

        try{
            if (type.getValue().length() > 1) throw new Exception("Количество символов превышает 1");
        }catch(Exception e){
            showInfo("Ошибка", Utils.Fmt("Неверное значение в поле {0}. {1}", Acod.FIELD_TYPE, e.getMessage()));
            throw new IllegalArgumentException("column");
        }*/

        checkRequeredString(sqdscr.getValue(), Acod.FIELD_SQDSCR);
        try{
            if (sqdscr.getValue().length() > 255) throw new Exception("Количество символов превышает 255");
        }catch(Exception e){
            showInfo("Ошибка", Utils.Fmt("Неверное значение в поле {0}. {1}", Acod.FIELD_SQDSCR, e.getMessage()));
            throw new IllegalArgumentException("column");
        }

        checkRequeredString(ename.getValue(), Acod.FIELD_ENAME);
        try{
            if (ename.getValue().length() > 164) throw new Exception("Количество символов превышает 164");
        }catch(Exception e){
            showInfo("Ошибка", Utils.Fmt("Неверное значение в поле {0}. {1}", Acod.FIELD_ENAME, e.getMessage()));
            throw new IllegalArgumentException("column");
        }

        checkRequeredString(rname.getValue(), Acod.FIELD_RNAME);
        try{
            if (rname.getValue().length() > 255) throw new Exception("Количество символов превышает 255");
        }catch(Exception e){
            showInfo("Ошибка", Utils.Fmt("Неверное значение в поле {0}. {1}", Acod.FIELD_RNAME, e.getMessage()));
            throw new IllegalArgumentException("column");
        }
        cnw.setId(id);
        cnw.setAcod(Utils.fillUp(acod.getValue(), 4));
        cnw.setAcc2dscr(acc2dscr.getValue());
        cnw.setType((String)type.getValue());
        cnw.setSqdscr(sqdscr.getValue());
        cnw.setEname(ename.getValue());
        cnw.setRname(rname.getValue());
    }

    @Override
    public void afterShow() {
        acod.setFocus(action == FormAction.CREATE);
        acc2dscr.setFocus(action == FormAction.UPDATE);
    }
}
