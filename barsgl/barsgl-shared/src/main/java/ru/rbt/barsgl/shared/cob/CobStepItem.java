package ru.rbt.barsgl.shared.cob;

import com.google.gwt.user.client.rpc.IsSerializable;
import ru.rbt.barsgl.shared.enums.CobStepStatus;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Created by ER18837 on 10.03.17.
 */
public class CobStepItem implements Serializable, IsSerializable {
    private Integer phaseNo;
    private String phaseName;
    private CobStepStatus status;
    private BigDecimal estimation;
    private BigDecimal duration;
    private BigDecimal percent;
    private Integer intEstimation;
    private Integer intDuration;
    private Integer intPercent;
    private String message;

    public CobStepItem() {
    }

    public CobStepItem(Integer phaseNo, String phaseName) {
        this.phaseNo = phaseNo;
        this.phaseName = phaseName;
    }

    public Integer getPhaseNo() {
        return phaseNo;
    }

    public String getPhaseName() {
        return phaseName;
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

    public BigDecimal getPercent() {
        return percent;
    }

    public void setPercent(BigDecimal pescent) {
        this.percent = pescent;
    }

    public Integer getIntEstimation() {
        return intEstimation;
    }

    public void setIntEstimation(Integer intEstimation) {
        this.intEstimation = intEstimation;
    }

    public Integer getIntDuration() {
        return intDuration;
    }

    public void setIntDuration(Integer intDuration) {
        this.intDuration = intDuration;
    }

    public Integer getIntPercent() {
        return intPercent;
    }

    public void setIntPercent(Integer intPercent) {
        this.intPercent = intPercent;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
