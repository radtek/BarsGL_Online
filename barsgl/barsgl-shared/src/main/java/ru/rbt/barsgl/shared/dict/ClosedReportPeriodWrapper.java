package ru.rbt.barsgl.shared.dict;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by er18837 on 14.08.2017.
 */
public class ClosedReportPeriodWrapper implements Serializable, IsSerializable {
    public final String dateFormat = "dd.MM.yyyy";

    private String lastDateStr;
    private String cutDateStr;

    private Date lastDate;      // используется только на сервере
    private Date cutDate;       // используется только на сервере

    public String getDateFormat() {
        return dateFormat;
    }

    public String getLastDateStr() {
        return lastDateStr;
    }

    public void setLastDateStr(String lastDateStr) {
        this.lastDateStr = lastDateStr;
    }

    public String getCutDateStr() {
        return cutDateStr;
    }

    public void setCutDateStr(String cutDateStr) {
        this.cutDateStr = cutDateStr;
    }

    public Date getLastDate() {
        return lastDate;
    }

    public void setLastDate(Date lastDate) {
        this.lastDate = lastDate;
    }

    public Date getCutDate() {
        return cutDate;
    }

    public void setCutDate(Date cutDate) {
        this.cutDate = cutDate;
    }
}
