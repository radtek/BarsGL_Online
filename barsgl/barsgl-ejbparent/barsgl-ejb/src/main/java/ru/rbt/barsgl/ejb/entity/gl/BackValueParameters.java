package ru.rbt.barsgl.ejb.entity.gl;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by er18837 on 30.06.2017.
 */
public class BackValueParameters implements Serializable{
    //================= параметры отсечения BACK_VALUE ===================

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Date getDepthCutDate() {
        return depthCutDate;
    }

    public void setDepthCutDate(Date depthCutDate) {
        this.depthCutDate = depthCutDate;
    }

    public Date getCloseCutDate() {
        return closeCutDate;
    }

    public void setCloseCutDate(Date closeCutDate) {
        this.closeCutDate = closeCutDate;
    }

    public Date getCloseLastDate() {
        return closeLastDate;
    }

    public void setCloseLastDate(Date closeLastDate) {
        this.closeLastDate = closeLastDate;
    }

    private String reason;
    private Date depthCutDate;
    private Date closeCutDate;
    private Date closeLastDate;
}
