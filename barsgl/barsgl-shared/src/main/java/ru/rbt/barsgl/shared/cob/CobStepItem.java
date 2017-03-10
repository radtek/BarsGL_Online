package ru.rbt.barsgl.shared.cob;

import com.google.gwt.user.client.rpc.IsSerializable;
import ru.rbt.barsgl.shared.enums.CobStepStatus;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Created by ER18837 on 10.03.17.
 */
public class CobStepItem implements Serializable, IsSerializable {
    private String phaseName;
    private CobStepStatus status;
    private BigDecimal estimation;
    private BigDecimal duration;
    private BigDecimal pescent;
    private String message;

    public String getPhaseName() {
        return phaseName;
    }

    public void setPhaseName(String phaseName) {
        this.phaseName = phaseName;
    }

    public CobStepStatus getStatus() {
        return status;
    }

    public void setStatus(CobStepStatus status) {
        this.status = status;
    }

    public BigDecimal getEstimation() {
        return estimation;
    }

    public void setEstimation(BigDecimal estimation) {
        this.estimation = estimation;
    }

    public BigDecimal getDuration() {
        return duration;
    }

    public void setDuration(BigDecimal duration) {
        this.duration = duration;
    }

    public BigDecimal getPescent() {
        return pescent;
    }

    public void setPescent(BigDecimal pescent) {
        this.pescent = pescent;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
