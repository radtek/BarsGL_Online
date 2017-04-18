package ru.rbt.barsgl.gwt.core.dialogs;

import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.ui.DatePickerBox;

import java.util.Date;

/**
 * Created by akichigi on 17.04.17.
 */
public class DateDlg extends DlgFrame implements IAfterShowEvent{
    private DatePickerBox date;
    private Label dateLabel;
    private Date currentOD;
    public DateDlg(){
        super();
        setCaption("Выбор даты");
        ok.setText("Выбрать");
    }

    @Override
    public Widget createContent(){
        Grid grid = new Grid(2, 2);
        grid.setWidget(0, 0, dateLabel = new Label("Дата"));
        grid.setWidget(0, 1, date = new DatePickerBox(currentOD = (Date) LocalDataStorage.getParam("current_od_date")));
        grid.getCellFormatter().getElement(0, 0).getStyle().setWidth(150, Style.Unit.PX);
        grid.getElement().getStyle().setMarginBottom(8, Style.Unit.PX);
        return grid;
    }

    protected void check() throws Exception {
        if (date.getValue() == null)
            throw new Exception("Не заполнена дата");
        if (currentOD.compareTo(date.getValue()) == -1)
            throw new Exception("Дата должна быть не больше даты текущего операционного дня");
    }

    @Override
    protected boolean onClickOK() throws Exception {
        try{
            check();
            params = DateTimeFormat.getFormat("yyyy-MM-dd").format(date.getValue());
            return true;
        } catch (Exception ex){
            DialogManager.message("Предупреждение", ex.getMessage());
            return false;
        }
    }

    public void setDateLabel(String dateLabel) {
        this.dateLabel.setText(dateLabel);
    }

    @Override
    public void afterShow() {
        date.setFocus(true);
    }
}
