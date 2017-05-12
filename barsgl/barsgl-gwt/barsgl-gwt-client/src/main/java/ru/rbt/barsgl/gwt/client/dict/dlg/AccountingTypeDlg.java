/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.gwt.client.dict.dlg;

import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.client.dict.AccountingType;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterShowEvent;
import ru.rbt.barsgl.gwt.core.ui.AreaBox;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.gwt.core.utils.AppPredicate;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.dict.AccTypeWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.BoolType;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.check;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;


/**
 *
 * @author Andrew Samsonov
 */
public class AccountingTypeDlg extends DlgFrame implements IAfterShowEvent {
    public final static String REQUIRED = " обязательно для заполнения";
    public final static String EDIT = "Редактирование AccType";
    public final static String CREATE = "Ввод нового AccType";
    public final static String DELETE = "Удаление AccType";

    private TxtBox code;
    private AreaBox name;
    private CheckBox pl_act;
    private CheckBox fl_ctrl;
    private CheckBox tech_act;

    private FormAction action;
    private AccTypeWrapper wrapper;
    private Columns columns;

    public AccountingTypeDlg(Columns columns){
        this.columns = columns;
    }

    public void setFormAction(FormAction action){
        this.action = action;
        String caption = "";
        switch (action) {
            case CREATE:
                ok.setText("Создать");
                caption = CREATE;
                break;
            case UPDATE:
                ok.setText(TEXT_CONSTANTS.formData_Update());
                caption = EDIT;
                break;
            case DELETE:
                ok.setText(TEXT_CONSTANTS.formData_Delete());
                caption = DELETE;
                break;
        }
        setCaption(caption);
    }

    @Override
        public Widget createContent() {
        Grid grid = new Grid(2, 2);
        grid.setWidget(0, 0, new Label(AccountingType.FIELD_ACCTYPE));

        grid.setWidget(0, 1, code = new TxtBox());
        code.setMaxLength(9);
        code.setWidth("70px");
        code.setMask("[0-9]");
        code.setReadOnly(true);

        grid.setWidget(1, 0, new Label(AccountingType.FIELD_ACCTYPENAME));
        grid.setWidget(1, 1, name = new AreaBox());
        name.setHeight("50px");
        name.setWidth("370px");

        pl_act = new CheckBox(AccountingType.FIELD_PL_ACT);
        fl_ctrl = new CheckBox(AccountingType.FIELD_FL_CTRL);
        tech_act = new CheckBox(AccountingType.FIELD_TECH_ACT);

        VerticalPanel panel = new VerticalPanel();
        panel.add(grid);
        panel.add(pl_act);
        panel.add(fl_ctrl);
        panel.add(tech_act);

        setAfterShowEvent(this);

        return panel;
    }

    public void clearContent(){
        code.clear();
        name.clear();
        name.setReadOnly(false);
        pl_act.setValue(false);
        pl_act.setEnabled(true);
        fl_ctrl.setValue(false);
        fl_ctrl.setEnabled(true);
        tech_act.setValue(false);
        tech_act.setEnabled(true);
    }

    private String value(String val){
        return  val == null ? val : val.trim();
    }

    @Override
    protected void fillContent() {
        clearContent();
        Row row = (Row) params;

        int ind;

        if (action == FormAction.UPDATE || action == FormAction.DELETE) {
            ind = columns.getColumnIndexByCaption(AccountingType.FIELD_ACCTYPE);
            if (ind >= 0) {
                code.setValue((String) row.getField(ind).getValue());
            }
            ind = columns.getColumnIndexByCaption(AccountingType.FIELD_ACCTYPENAME);
            if (ind >= 0) {
                name.setValue((String) row.getField(ind).getValue());
            }
            ind = columns.getColumnIndexByCaption(AccountingType.FIELD_PL_ACT);
            if (ind >= 0) {
                pl_act.setValue(BoolType.Y.name().equals(row.getField(ind).getValue()));
            }
            ind = columns.getColumnIndexByCaption(AccountingType.FIELD_FL_CTRL);
            if (ind >= 0) {
                fl_ctrl.setValue(BoolType.Y.name().equals(row.getField(ind).getValue()));
            }
            ind = columns.getColumnIndexByCaption(AccountingType.FIELD_TECH_ACT);
            if (ind >= 0) {
                tech_act.setValue(BoolType.Y.name().equals(row.getField(ind).getValue()));
            }
        }

        if (action == FormAction.DELETE) {
            name.setReadOnly(true);
            pl_act.setEnabled(false);
            fl_ctrl.setEnabled(false);
        }
    }

    private void setWrapperFields(){
        wrapper.setAcctype(code.getValue());
        wrapper.setAcctypeName(checkRequeredString(name.getValue(), AccountingType.FIELD_ACCTYPENAME));
        try{
            if (name.getValue().length() > 255) throw new Exception("Количество символов превышает 255");
        }catch(Exception e){
            showInfo("Ошибка", Utils.Fmt("Неверное значение в поле {0}. {1}", AccountingType.FIELD_ACCTYPENAME, e.getMessage()));
            throw new IllegalArgumentException("column");
        }

        wrapper.setFl_ctrl(fl_ctrl.getValue() ? BoolType.Y : BoolType.N);
        wrapper.setPl_act(pl_act.getValue() ? BoolType.Y : BoolType.N);
    }

    private String checkRequeredString(String value, String columnCaption) {
        return check(value, columnCaption, REQUIRED, new AppPredicate<String>() {
            @Override
            public boolean check(String target) {
                return (target != null) ? !target.trim().isEmpty() : false;
            }
        });
    }


    @Override
    protected boolean onClickOK() throws Exception {
        try {
            wrapper = new AccTypeWrapper();
            setWrapperFields();
            params = wrapper;
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().equals("column")) {
                return false;
            } else {
                throw e;
            }
        }
        return true;
    }

    @Override
    public void afterShow() {
        name.setFocus(action == FormAction.CREATE || action == FormAction.UPDATE);
    }
}
