package ru.rbt.barsgl.gwt.client.checkCardsRem;

import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.client.comp.DataListBoxEx;
import ru.rbt.barsgl.gwt.core.dialogs.DialogManager;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.ui.DatePickerBox;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.createFilialAuthListBox;

/**
 * Created by akichigi on 15.12.16.
 */
public class CheckCardRemFilterDlg extends DlgFrame {
    private DataListBoxEx filial;
    private DatePickerBox date;

    public CheckCardRemFilterDlg(){
        super();
        setCaption("Выбор параметров проверки остатков");
        ok.setText("Выбрать");
    }

    @Override
    public Widget createContent(){
        Grid grid = new Grid(2, 2);

        grid.setText(0, 0, "Филиал");
        grid.setWidget(0, 1, filial = createFilialAuthListBox("", "110", true, true));

        grid.setText(1, 0, "Дата проводок");
        grid.setWidget(1, 1, date = new DatePickerBox(null));

        grid.getElement().getStyle().setMarginBottom(8, Style.Unit.PX);
        return grid;
    }

    @Override
    protected boolean onClickOK() throws Exception {
        if (filial.getValue() == null){
            DialogManager.message("Предупреждение", "Не выбран филиал");
            return false;
        }
        if (date.getValue() == null){
            DialogManager.message("Предупреждение", "Не заполнена дата проводок");
            return false;
        }

        params = new String[]{DateTimeFormat.getFormat("yyyy-MM-dd").format(date.getValue()),
                             (String)filial.getValue()};
        return true;
    }
}
