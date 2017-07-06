package ru.rbt.barsgl.ejb.entity.gl;

import ru.rbt.barsgl.shared.enums.BackValuePostStatus;
import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by er18837 on 23.06.2017.
 */
@Entity
@Table(name="GL_OPEREXT")
//@MappedSuperclass
public class GLOperationExt extends BaseEntity<Long> {

    public enum BackValueReason {
        OverDepth("1"),
        ClosedPeriod("2");

        private final String value;

        BackValueReason(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
        public static BackValueReason parseType(String value) {
            for (BackValueReason postingType : values()) {
                if (postingType.getValue().equals(value)) return postingType;
            }
            return null;
        }
    }

    public GLOperationExt(){}

    public GLOperationExt(Long gloid, Date postdatePlan){
        this.id = gloid;
        this.postDatePlan = postdatePlan;
    }

    @Id
    @Column(name="GLOID")
    private long id;

    @Override
    public Long getId() {
        return id;
    }

    @OneToOne(fetch = FetchType.LAZY)
//    @MapsId
    @JoinColumn(name="GLOID", insertable = false, updatable = false)
    private GLOperation oper;


    @Temporal(TemporalType.DATE)
    @Column(name = "POSTDATE_PLAN")
    private Date postDatePlan;

    @Column(name = "MNL_RSNCODE")
    private String manualReason;

    @Temporal(TemporalType.DATE)
    @Column(name = "BV_CUTDATE")
    private Date depthCutDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "PRD_LDATE")
    private Date closeLastDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "PRD_CUTDATE")
    private Date closeCutDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "OTS_AUTO")
    private Date createTimestamp;

    @Column(name = "MNL_NRT")
    private String manualNarrative;

    @Enumerated(EnumType.STRING)
    @Column(name = "MNL_STATUS")
    private BackValuePostStatus manualStatus;

    @Column(name = "USER_AU3")
    private String confirmName;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "OTS_AU3")
    private Date confirmTimestamp;

    public Date getPostDatePlan() {
        return postDatePlan;
    }

    public void setPostDatePlan(Date postDatePlan) {
        this.postDatePlan = postDatePlan;
    }

    public String getManualReason() {
        return manualReason;
    }

    public void setManualReason(String manualReason) {
        this.manualReason = manualReason;
    }

    public Date getDepthCutDate() {
        return depthCutDate;
    }

    public void setDepthCutDate(Date depthCutDate) {
        this.depthCutDate = depthCutDate;
    }

    public Date getCloseLastDate() {
        return closeLastDate;
    }

    public void setCloseLastDate(Date closeLastDate) {
        this.closeLastDate = closeLastDate;
    }

    public Date getCloseCutDate() {
        return closeCutDate;
    }

    public void setCloseCutDate(Date closeCutDate) {
        this.closeCutDate = closeCutDate;
    }

    public String getManualNarrative() {
        return manualNarrative;
    }

    public void setManualNarrative(String manualNarrative) {
        this.manualNarrative = manualNarrative;
    }

    public String getConfirmName() {
        return confirmName;
    }

    public void setConfirmName(String confirmName) {
        this.confirmName = confirmName;
    }

    public Date getConfirmTimestamp() {
        return confirmTimestamp;
    }

    public void setConfirmTimestamp(Date confirmTimestamp) {
        this.confirmTimestamp = confirmTimestamp;
    }

    public BackValuePostStatus getManualStatus() {
        return manualStatus;
    }

    public void setManualStatus(BackValuePostStatus manualStatus) {
        this.manualStatus = manualStatus;
    }

    public Date getCreateTimestamp() {
        return createTimestamp;
    }

    public void setCreateTimestamp(Date createTimestamp) {
        this.createTimestamp = createTimestamp;
    }
}
