package ru.rbt.barsgl.shared.operday;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by er18837 on 16.08.2017.
 */
public class LwdBalanceCutWrapper implements Serializable {
    public final String dateFormat = "dd.MM.yyyy";
    public final String timeFormat = "HH:mm";

    String runDateStr;
    String cutTimeStr;

    Date runDate;               // используется на сервере

    public LwdBalanceCutWrapper() {
    }

    public LwdBalanceCutWrapper(String runDateStr, String cutTimeStr) {
        this.runDateStr = runDateStr;
        this.cutTimeStr = cutTimeStr;
    }

    public String getRunDateStr() {
        return runDateStr;
    }

    public void setRunDateStr(String runDateStr) {
        this.runDateStr = runDateStr;
    }

    public String getCutTimeStr() {
        return cutTimeStr;
    }

    public void setCutTimeStr(String cutTimeStr) {
        this.cutTimeStr = cutTimeStr;
    }

    public Date getRunDate() {
        return runDate;
    }

    public void setRunDate(Date runDate) {
        this.runDate = runDate;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public String getTimeFormat() {
        return timeFormat;
    }
}
