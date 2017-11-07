package ru.rbt.barsgl.gwt.client.dict.dlg;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.client.compLookup.LookUpAcc2;
import ru.rbt.barsgl.gwt.client.compLookup.LookupBoxBase;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.utils.AppPredicate;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.dict.AccDealsWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.BoolType;

import static ru.rbt.barsgl.gwt.client.dict.AccDealsDict.FIELD_ACC2;
import static ru.rbt.barsgl.gwt.client.dict.AccDealsDict.FIELD_FLAG_OFF;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.check;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;


/**
 * Created by er22317 on 18.07.2017.
 */
public class AccDealsDlg extends EditableDialog<AccDealsWrapper>{
    public final static String EDIT = "Редактирование настройки";
    public final static String CREATE = "Создание настройки";
    public final static String DELETE = "Удаление настройки";

    private LookupBoxBase acc2;
    private CheckBox isOff;

    public AccDealsDlg(String caption, Columns columns, FormAction action) {
        super(columns, action);
        setCaption(caption);
    }

    @Override
    protected AccDealsWrapper createWrapper() {
        return new AccDealsWrapper();
    }

    @Override
    protected void setFields(AccDealsWrapper cnw) {
        cnw.setAcc2(checkLength(checkRequeredString(acc2.getValue(), FIELD_ACC2), 5, FIELD_ACC2));
        cnw.setFlagOff(!isOff.getValue());
    }

    @Override
    public Widget createContent() {
        Grid grid = new Grid(3, 2);
        grid.setWidget(0, 0, new HTML("<b>" + new Label("Настройка:") + "</b>"));
        grid.setWidget(1, 0, new Label(FIELD_ACC2));
        grid.setWidget(1, 1, acc2 = new LookUpAcc2("95px"));
        grid.setWidget(2, 0, isOff = new CheckBox(FIELD_FLAG_OFF));

        return grid;
    }

    @Override
    protected void fillContent() {
        clearContent();
        row = (Row) params;
        if (action == FormAction.UPDATE || action == FormAction.DELETE) {
            acc2.setValue(Utils.value((String) row.getField(0).getValue()));
            acc2.setReadOnly(action == FormAction.UPDATE || action == FormAction.DELETE);
            isOff.setValue(row.getField(2).getValue() == null ?  false : row.getField(2).getValue().equals(BoolType.Y.toString()));
            isOff.setEnabled(action != FormAction.DELETE);
        }
    }

    @Override
    public void clearContent(){
        acc2.clear();
        acc2.setReadOnly(false);
        isOff.setValue(false);
        isOff.setEnabled(true);
    }

    @Override
    public String checkRequeredString(String value, String columnCaption) {
        return check(value, columnCaption, REQUIRED, new AppPredicate<String>() {
            @Override
            public boolean check(String target) {
                return (target != null) ? !target.trim().isEmpty() : false;
            }
        });
    }

    private String checkLength(String value, int len, String fieldName){
        try{
            if (value != null && !value.trim().isEmpty() && value.trim().length() != len) throw new Exception( Utils.Fmt("Требуемое количество символов должно быть {0}", len));
        }catch(Exception e){
            showInfo("Ошибка", Utils.Fmt("Неверное значение в поле {0}. {1}", fieldName, e.getMessage()));
            throw new IllegalArgumentException("column");
        }

        return value;
    }
}
