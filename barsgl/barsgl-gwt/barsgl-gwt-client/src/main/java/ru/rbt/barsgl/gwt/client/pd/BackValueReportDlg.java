package ru.rbt.barsgl.gwt.client.pd;

import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.dialogs.DialogManager;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterShowEvent;
import ru.rbt.barsgl.gwt.core.ui.DatePickerBox;
import ru.rbt.barsgl.gwt.core.ui.IntBox;

import java.util.Date;


/**
 * Created by er17503 on 30.05.2017.
 */
public class BackValueReportDlg  extends DlgFrame implements IAfterShowEvent {
    private DatePickerBox date;
    private IntBox limit;
    private Date currentOD;

    public BackValueReportDlg(){
        super();
        setCaption("Выбор параметров отчета по операциям BackValue");
        ok.setText("Выбрать");
    }

    @Override
    public Widget createContent(){
        VerticalPanel vpanel = new VerticalPanel();
        Grid grid = new Grid(2, 2);
        grid.setWidget(0, 0, new Label("Дата отчета"));
        grid.setWidget(0, 1, date = new DatePickerBox(currentOD = (Date) LocalDataStorage.getParam("current_od_date")));
        grid.setWidget(1, 0, new Label("Лимит поиска (дн.)"));
        grid.setWidget(1, 1, limit = new IntBox(30));
        limit.setWidth("30px");
        limit.setMaxLength(3);
        grid.getCellFormatter().getElement(0, 0).getStyle().setWidth(150, Style.Unit.PX);
        grid.getElement().getStyle().setMarginBottom(8, Style.Unit.PX);
        vpanel.add(grid);
        StringBuilder html = new StringBuilder();
        html.append("<p>Сформированный период поиска:</p>");
        html.append("<pre style='font-weight: bold'>Дата начала поиска    = Дата отчета<br>Дата окончания поиска = Дата отчета + Лимит поиска</pre>");
        html.append("<p>В отчет включаются все операции BackValue по состоянию на <br> Дату отчета, созданные в данном периоде</p>");
        HTMLPanel hpanel = new HTMLPanel(html.toString());
        vpanel.add(hpanel);
        return vpanel;
    }

    private void check() throws Exception {
        if (date.getValue() == null)
            throw new Exception("Не заполнена дата отчета");
        if (currentOD.compareTo(date.getValue()) == -1)
            throw new Exception("Дата отчета должна быть не больше даты текущего операционного дня");

        if (!limit.hasValue())
            throw new Exception("Не заполнен лимит поиска");
        Integer val = limit.getValue();
        if (val <  0 || val > 100)
            throw new Exception("Лимит поиска должен быть в диапазоне от 0 до 100");
    }

    @Override
    protected boolean onClickOK() throws Exception {
        try{
            check();
            params = new Object[] {DateTimeFormat.getFormat("yyyy-MM-dd").format(date.getValue()), limit.getValue().toString()};
            return true;
        } catch (Exception ex){
            DialogManager.message("Предупреждение", ex.getMessage());
            return false;
        }
    }

    @Override
    public void afterShow() {
        date.setFocus(true);
    }
}
