package ru.rbt.barsgl.shared.dict;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by er18837 on 14.08.2017.
 */
public class BVSourceDealWrapper implements Serializable, IsSerializable {
    public final String dateFormat = "dd.MM.yyyy";

    private String sourceDeal;
    private Integer depth;

    private String startDateStr;
    private String endDateStr;

    private Date startDate;     // используется только на сервере
    private Date endDate;       // используется только на сервере

    public String getDateFormat() {
        return dateFormat;
    }

    public String getSourceDeal() {
        return sourceDeal;
    }

    public void setSourceDeal(String sourceDeal) {
        this.sourceDeal = sourceDeal;
    }

    public Integer getDepth() {
        return depth;
    }

    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    public String getStartDateStr() {
        return startDateStr;
    }

    public void setStartDateStr(String startDateStr) {
        this.startDateStr = startDateStr;
    }

    public String getEndDateStr() {
        return endDateStr;
    }

    public void setEndDateStr(String endDateStr) {
        this.endDateStr = endDateStr;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }
}
