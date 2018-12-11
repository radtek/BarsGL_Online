package ru.rbt.barsgl.ejb.entity.gl;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by er18837 on 30.06.2017.
 */
public class BackValueParameters implements Serializable{
    //================= параметры отсечения BACK VALUE ===================

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

    public boolean isStornoInvisible() {
        return isStornoInvisible;
    }

    public void setStornoInvisible(boolean stornoInvisible) {
        isStornoInvisible = stornoInvisible;
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
    private boolean isStornoInvisible;
    private Date closeCutDate;
    private Date closeLastDate;

    public boolean isCancelForbidden(Date postDate) {
        return (null != closeLastDate && !postDate.after(closeLastDate)
            || (null != depthCutDate && postDate.before(depthCutDate)));
    }
}
