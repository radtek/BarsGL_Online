package ru.rbt.barsgl.shared.operation;

import com.google.gwt.user.client.rpc.IsSerializable;
import ru.rbt.barsgl.shared.enums.BackValueAction;
import ru.rbt.barsgl.shared.enums.BackValueMode;
import ru.rbt.barsgl.shared.enums.BackValuePostStatus;
import ru.rbt.barsgl.shared.filter.IFilterItem;

import java.io.Serializable;
import java.util.List;

import static java.lang.String.format;

/**
 * Created by er18837 on 07.07.2017.
 */
public class BackValueWrapper implements Serializable, IsSerializable {
    public final String dateFormat = "dd.MM.yyyy";

    private List<Long> gloIDs;              // видимый список или 1 операция
    private List<? extends IFilterItem> filters;      // критерии фильтра
    private String sql;                     // TODO для выбора по фильтру - может и не надо
    private Boolean ownMessage;             // свои
    private BackValueAction action;         // действие
    private BackValueMode mode;             // режим обработки
    private String postDateStr;             // дата проводки
    private String comment;                 // Комментарийprivate
    private BackValuePostStatus bvStatus;   // текущий статус

    public String getDateFormat() {
        return dateFormat;
    }

    public List<Long> getGloIDs() {
        return gloIDs;
    }

    public void setGloIDs(List<Long> gloIDs) {
        this.gloIDs = gloIDs;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public Boolean getOwnMessage() {
        return ownMessage;
    }

    public void setOwnMessage(Boolean ownMessage) {
        this.ownMessage = ownMessage;
    }

    public BackValueAction getAction() {
        return action;
    }

    public void setAction(BackValueAction action) {
        this.action = action;
    }

    public BackValueMode getMode() {
        return mode;
    }

    public void setMode(BackValueMode mode) {
        this.mode = mode;
    }

    public String getPostDateStr() {
        return postDateStr;
    }

    public void setPostDateStr(String postDateStr) {
        this.postDateStr = postDateStr;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public BackValuePostStatus getBvStatus() {
        return bvStatus;
    }

    public void setBvStatus(BackValuePostStatus bvStatus) {
        this.bvStatus = bvStatus;
    }

    public List<? extends IFilterItem> getFilters() {
        return filters;
    }

    public void setFilters(List<? extends IFilterItem> filters) {
        this.filters = filters;
    }
}
