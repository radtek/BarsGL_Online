package ru.rbt.barsgl.gwt.client.events.ae;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.client.quickFilter.DateHistoryQuickFilterParams;
import ru.rbt.barsgl.gwt.client.quickFilter.DateIntervalQuickFilterDlg;
import ru.rbt.barsgl.gwt.core.ui.ValuesBox;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;

/**
 * Created by ER18837 on 20.01.17.
 */
public class OperHistoryDlg extends DateIntervalQuickFilterDlg {
    private CheckBox _ownMessages;
    private ValuesBox _messageTypes;
    protected Widget _choicePanel;

    private DateHistoryQuickFilterParams filterParams;

    public OperHistoryDlg() {
        super();
        ok.setText(TEXT_CONSTANTS.applyBtn_caption());
        cancel.setText(TEXT_CONSTANTS.btn_cancel());
        setCaption("Выбор периода просмотра");
    }

    @Override
    public Widget createContent() {

        VerticalPanel mainVP = new VerticalPanel();

        mainVP.add(createDatePanel());
        mainVP.add(_choicePanel = createChoicePanel());
        mainVP.add(_ownMessages = new CheckBox("Только свои"));

        return mainVP;
    }

    @Override
    protected void fillContent() {
        filterParams = (DateHistoryQuickFilterParams) params;

        mDateBegin.clear();
        mDateEnd.clear();

        if (null != filterParams) {
            mDateBegin.setValue(filterParams.getDateBegin());
            mDateEnd.setValue(filterParams.getDateEnd());
            _ownMessages.setValue(filterParams.getOwnMessages());
            _messageTypes.setValue(filterParams.getMsgType());
        }

    }

    @Override
    protected boolean onClickOK() throws Exception {
        if (null != filterParams) {
            filterParams.setDateBegin(mDateBegin.getValue());
            filterParams.setDateEnd(mDateEnd.getValue());
            filterParams.setMsgType((StepChoiceDlg.MessageType) _messageTypes.getValue());
            filterParams.setOwnMessages(_ownMessages.getValue());
        }
        params = filterParams;
        return true;
    }

    protected Widget createChoicePanel() {
        Grid g1 = new Grid(1, 2);
        g1.setText(0, 0, "Состояние");
        g1.setWidget(0, 1, _messageTypes = new ValuesBox());
        initMessageType();
//        g1.getElement().getStyle().setMarginBottom(8, Style.Unit.PX);
        return g1;
    }

    private void initMessageType(){
        for (StepChoiceDlg.MessageType value: StepChoiceDlg.MessageType.values()){
            _messageTypes.addItem(value, value.getLabel());
        }
    }


}