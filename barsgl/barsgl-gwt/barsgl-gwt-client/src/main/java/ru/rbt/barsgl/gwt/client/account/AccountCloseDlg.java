package ru.rbt.barsgl.gwt.client.account;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.client.dict.dlg.EditableDialog;
import ru.rbt.security.gwt.client.operday.IDataConsumer;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.ui.DatePickerBox;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;
import ru.rbt.shared.user.AppUserWrapper;

import java.util.Date;

import ru.rbt.barsgl.gwt.core.comp.Components;
import static ru.rbt.security.gwt.client.operday.OperDayGetter.getOperday;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;

/**
 * Created by ER18837 on 18.09.15.
 */
public class AccountCloseDlg extends EditableDialog<ManualAccountWrapper> {
    private TxtBox mBsaAcid;
    private TxtBox mDateOpen;
    private DatePickerBox mDateClose;
    private boolean isClosed;
    private String dealSource;
    private Long id;

    public AccountCloseDlg(String caption, FormAction action, Columns columns) {
        super(columns, action);
        this.columns = columns;
        ok.setText(TEXT_CONSTANTS.formInput_save());
        cancel.setText(TEXT_CONSTANTS.formInput_cancel());
        setCaption(caption);
    }

    @Override
    public Widget createContent() {

        VerticalPanel mainVP = new VerticalPanel();

        Grid g = new Grid(3, 4);
        mainVP.add(g);
        g.setWidget(0, 0, new Label("Счет ЦБ:"));
        g.setWidget(0, 1, mBsaAcid = Components.createTxtBox(20));
        mBsaAcid.setEnabled(false);
        g.setWidget(1, 0, new Label("Дата открытия:"));
        g.setWidget(1, 1, mDateOpen = Components.createTxtBox(20));
        mDateOpen.setEnabled(false);
        g.setWidget(2, 0, new Label("Дата закрытия:"));
        g.setWidget(2, 1, mDateClose = Components.createDateBox());

        mBsaAcid.setWidth("150px");
        mDateOpen.setWidth("150px");
        mDateClose.setWidth("150px");
        
        return mainVP;
    }

    @Override
    protected ManualAccountWrapper createWrapper() {
        return new ManualAccountWrapper();
    }

    @Override
    protected void setFields(ManualAccountWrapper cnw) {
        cnw.setId(id);
        cnw.setBsaAcid(mBsaAcid.getValue());
        cnw.setDateOpenStr(mDateOpen.getValue());
        cnw.setDateCloseStr(isClosed ? null : DateTimeFormat.getFormat(ManualAccountWrapper.dateFormat).format(mDateClose.getValue()));
        cnw.setDealSource(dealSource);

        AppUserWrapper wrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
        cnw.setUserId(wrapper.getId());
    }

    @Override
    protected void fillContent() {
        row = (Row) params;
        id = (Long) getFieldValue("ID");
        dealSource = getFieldValue("DEALSRS");
        mBsaAcid.setValue(getFieldText("BSAACID"));
        mDateOpen.setValue(DateTimeFormat.getFormat(ManualAccountWrapper.dateFormat).format((Date)getFieldValue("DTO")));
        Date dateClose = (Date)getFieldValue("DTC");
        mDateClose.setEnabled(true);

        isClosed = (dateClose != null);
        if (isClosed) {
            mDateClose.setValueSrv(dateClose);
            mDateClose.setEnabled(false);
            ok.setText("Отменить закрытие счета");
        } else {
            getOperday(new IDataConsumer<OperDayWrapper>() {
                @Override
                public void accept(OperDayWrapper wrapper) {
                    setOperday(wrapper.getCurrentOD());
                }
            });
            ok.setText("Закрыть счет");
        }
    }

    private void setOperday(final String operDayStr) {
        mDateClose.setValue(DateTimeFormat.getFormat(ManualAccountWrapper.dateFormat).parse(operDayStr));
    }
}
