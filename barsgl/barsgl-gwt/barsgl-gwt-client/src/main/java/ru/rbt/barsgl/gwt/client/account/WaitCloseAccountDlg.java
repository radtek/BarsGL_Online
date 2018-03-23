package ru.rbt.barsgl.gwt.client.account;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.core.comp.Components;
import ru.rbt.barsgl.gwt.core.dialogs.DialogManager;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterShowEvent;
import ru.rbt.barsgl.gwt.core.ui.DatePickerBox;

import java.util.Date;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.addDays;

/**
 * Created by er22317 on 22.03.2018.
 */
public class WaitCloseAccountDlg extends DlgFrame implements IAfterShowEvent {
    protected DatePickerBox mDateBegin;
    protected DatePickerBox mDateEnd;
    private CheckBox chkAllAccounts;

    public WaitCloseAccountDlg(){
        super();
        setCaption("Выбор периода просмотра");
        ok.setText(TEXT_CONSTANTS.applyBtn_caption());
        cancel.setText(TEXT_CONSTANTS.btn_cancel());
    }

    @Override
    public Widget createContent(){
        VerticalPanel mainVP = new VerticalPanel();
        mainVP.add(createDatePanel());
        mainVP.add(createCheckPanel());
        return mainVP;
    }
    @Override
    protected void fillContent() {
        if (null != params) {
            mDateBegin.setValue(DateTimeFormat.getFormat("yyyy-MM-dd").parse( ((Object[])params)[0].toString()) );
            mDateEnd.setValue(DateTimeFormat.getFormat("yyyy-MM-dd").parse( ((Object[])params)[1].toString()));
            chkAllAccounts.setValue((Boolean)((Object[])params)[2]);
        }else{
            mDateBegin.setValue(addDays(new Date(), -10));
            mDateEnd.setValue(new Date());
//            mDateBegin.clear();
//            mDateEnd.clear();
//            editday = addDays(operday, -DAYS_EDIT);
        }
    }
    @Override
    public void afterShow() {

    }

    @Override
    protected boolean onClickOK() throws Exception {
        try{
            check();
            params = new Object[] {DateTimeFormat.getFormat("yyyy-MM-dd").format(mDateBegin.getValue()),
                                   DateTimeFormat.getFormat("yyyy-MM-dd").format(mDateEnd.getValue()),
                                   chkAllAccounts.getValue()};
            return true;
        } catch (Exception ex){
            DialogManager.message("Предупреждение", ex.getMessage());
            return false;
        }
    }

    private void check() throws Exception {
        if (mDateBegin.getValue() == null || mDateEnd.getValue() == null)
            throw new Exception("Не заполнена дата отчета");
        if (mDateBegin.getValue().compareTo(mDateEnd.getValue()) > 0)
            throw new Exception("Дата начала отчета должна быть не больше даты окончания отчета");
    }

    private Widget createDatePanel() {
        Grid g1 = new Grid(1, 4);
        g1.setWidget(0, 0, Components.createLabel("Дата начала"));
        g1.setWidget(0, 1, mDateBegin = Components.createDateBox());
        g1.setWidget(0, 2, Components.createLabel("Дата окончания"));
        g1.setWidget(0, 3, mDateEnd = Components.createDateBox());
        return g1;
    }

    private Widget createCheckPanel() {
        HorizontalPanel panel = new HorizontalPanel();
        panel.setSpacing(10);
        chkAllAccounts = new CheckBox("Все счета");
        chkAllAccounts.addValueChangeHandler(new ValueChangeHandler<Boolean>(){
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> valueChangeEvent) {
                mDateBegin.setReadOnly(valueChangeEvent.getValue());
                mDateEnd.setReadOnly(valueChangeEvent.getValue());
            }
        });
        panel.add(chkAllAccounts);
        return panel;
    }

}
