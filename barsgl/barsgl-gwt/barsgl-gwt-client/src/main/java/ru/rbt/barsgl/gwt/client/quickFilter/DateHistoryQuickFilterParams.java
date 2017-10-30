package ru.rbt.barsgl.gwt.client.quickFilter;

import ru.rbt.barsgl.gwt.client.events.ae.StepChoiceDlg;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.shared.filter.FilterCriteria;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;

import java.util.ArrayList;

/**
 * Created by ER18837 on 23.01.17.
 */
public class DateHistoryQuickFilterParams extends DateIntervalQuickFilterParams {
    private Column histColumn;

    private StepChoiceDlg.MessageType msgType;
    private Boolean ownMessages;

    public DateHistoryQuickFilterParams(Column dateColumn, Column histColumn) {
        super(dateColumn);
        this.histColumn = histColumn;
        this.msgType = StepChoiceDlg.MessageType.ALL;
        this.ownMessages = true;
    }

    @Override
    public ArrayList<FilterItem> getFilter() {
        ArrayList<FilterItem> list = new ArrayList<>();
        list.add(new FilterItem(dateColumn, FilterCriteria.GE, dateBegin));
        list.add(new FilterItem(dateColumn, FilterCriteria.LE, dateEnd));
        if (null != histColumn) {
            list.add(new FilterItem(histColumn, FilterCriteria.EQ, " "));
        }
        return list;
    }

    public StepChoiceDlg.MessageType getMsgType() {
        return msgType;
    }

    public void setMsgType(StepChoiceDlg.MessageType msgType) {
        this.msgType = msgType;
    }

    public Boolean getOwnMessages() {
        return ownMessages;
    }

    public void setOwnMessages(Boolean ownMessages) {
        this.ownMessages = ownMessages;
    }
}
