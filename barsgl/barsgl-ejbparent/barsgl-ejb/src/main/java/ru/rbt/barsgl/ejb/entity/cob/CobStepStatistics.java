package ru.rbt.barsgl.ejb.entity.cob;

import ru.rbt.barsgl.ejbcore.mapping.BaseEntity;
import ru.rbt.barsgl.shared.enums.CobStepStatus;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by ER18837 on 10.03.17.
 */
@Entity
@Table(name = "GL_COB_STAT")
public class CobStepStatistics extends BaseEntity<CobStatId> {

    @EmbeddedId
    private CobStatId id;

    @Column(name = "DAT")
    @Temporal(TemporalType.DATE)
    private Date curdate;

    @Column(name = "PHASE_NAME")
    private String phaseName;

    @Column(name = "OTS_START")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startTimestamp;

    @Column(name = "OTS_END")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endTimestamp;

    @Column(name = "DURATION")
    private BigDecimal duration;

    @Column(name = "ESTIMATED")
    private BigDecimal estimated;

    @Column(name = "PARAMETER")
    private BigDecimal parameter;

    @Column(name = "COEF_A")
    private BigDecimal CoefA;

    @Column(name = "COEF_B")
    private BigDecimal CoefB;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private CobStepStatus status;

    @Column(name = "MESSAGE")
    private String message;

    @Column(name = "ERRORMSG")
    private String errorMsg;

    @Override
    public void setId(CobStatId id) {
        this.id = id;
    }

    @Override
    public CobStatId getId() {
        return id;
    }

    public Long getIdCob() {return id.getIdCob();}

    public Integer getPhaseNo() {return id.getPhaseNo();}


    public Date getCurdate() {
        return curdate;
    }

    public void setCurdate(Date curdate) {
        this.curdate = curdate;
    }

    public String getPhaseName() {
        return phaseName;
    }

    public void setPhaseName(String phaseName) {
        this.phaseName = phaseName;
    }

    public Date getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(Date startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public Date getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(Date endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public BigDecimal getDuration() {
        return duration;
    }

    public void setDuration(BigDecimal duration) {
        this.duration = duration;
    }

    public BigDecimal getEstimated() {
        return estimated;
    }

    public void setEstimated(BigDecimal estimated) {
        this.estimated = estimated;
    }

    public BigDecimal getParameter() {
        return parameter;
    }

    public void setParameter(BigDecimal parameter) {
        this.parameter = parameter;
    }

    public BigDecimal getCoefA() {
        return CoefA;
    }

    public void setCoefA(BigDecimal coefA) {
        CoefA = coefA;
    }

    public BigDecimal getCoefB() {
        return CoefB;
    }

    public void setCoefB(BigDecimal coefB) {
        CoefB = coefB;
    }

    public CobStepStatus getStatus() {
        return status;
    }

    public void setStatus(CobStepStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}
