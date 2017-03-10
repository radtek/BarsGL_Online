package ru.rbt.barsgl.ejb.entity.gl;

import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejbcore.mapping.BaseEntity;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by er23851 on 06.03.2017.
 * Сущность для хранения проводок по техническим счетам
 */
@Entity
@Table(name = "GL_PDTH")
public class GlPdTh extends BaseEntity<Long> {

    /**
     * Posting ID
     */
    @Id
    @Column(name = "ID")
    private Long id;

    /**
     * Posting date
     */
    @Column(name = "POD")
    @Temporal(TemporalType.DATE)
    private Date pod;

    /**
     * Value date
     */
    @Column(name = "VALD")
    @Temporal(TemporalType.DATE)
    private Date vald;

    /**
     * BSA Account ID
      */
    @Column(name = "BSAACID",length = 20)
    private String bsaAcid;     // 20

    /**
     * Currency code
     */
    @ManyToOne
    @JoinColumn(name = "CCY")
    private BankCurrency ccy;

    /**
     * Posting amount
     */
    @Column(name = "AMNT")
    private BigDecimal amount;

    /**
     * Posting amount in base currency
     */
    @Column(name = "AMNTBC")
    private BigDecimal amountBC;

    /**
     * Source of posting
     */
    @Column(name = "PBR",length = 8)
    private String pbr;

    /**
     * Visible/invisible
     */
    @Column(name = "INVISIBLE",length = 1)
    protected String invisible;   // 1

    /**
     * Posting narrative
     */
    @Column(name = "PNAR",length = 30)      // 30
    private String pnar;

    /**
     * Correspond postiong ID
     */
    @Column(name = "PCID", nullable = false)
    private Long pcId;

    /**
     * Depatment code
     */
    @Column(name = "DPMT",length = 3)
    private String department;

    /**
     * Russian narrative long
     */
    @Column(name = "RNARLNG",length = 300)
    private String rusNarrLong;

    /**
     * Russian narrative short
     */
    @Column(name = "RNARSHT",length = 100)
    private String rusNarrShort;

    /**
     * Ref to GL_OPER.GLOID
     */
    @Column(name = "GLO_REF")
    private Long glOperationId;

    /**
     * Event type
     */
    @Column(name = "EVTP",length = 20)
    private String eventType;

    /**
     * Processing date
     */
    @Column(name = "PROCDATE")
    @Temporal(TemporalType.DATE)
    private Date procDate;

    /**
     * ID of Deal
     */
    @Column(name = "DEAL_ID",length = 20)
    private String dealId;                  // 20

    /**
     * ID of subdeal
     */
    @Column(name = "SUBDEALID",length = 20)
    private String subdealId;               // 20

    /**
     * ID события
     */
    @Column(name = "EVT_ID",length = 20)
    private String eventId;                 // 20

    /**
     * ID платежа
     */
    @Column(name = "PMT_REF",length = 20)
    private String paymentRef;              // 20

    /**
     * Признак корректирующей проводки
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "FCHNG",length = 1)
    private YesNo isCorrection;            // 1

    /**
     * Profit centre
     */
    @Column(name = "PRFCNTR",length = 3)
    private String profitCenter;            // 4

    /**
     * Длинное наменование на английском языке
     */
    @Column(name = "NRT",length = 300)
    private String narrative;               // 300

    /**
     * Ref to GL_ACC.ID
     */
    @Column(name="GLACID")
    private Long glAcID;

    @Transient
    private GLOperation.OperSide operSide;

    public GlPdTh(){}

    public GlPdTh(GLOperation.OperSide os)
    {
        this.operSide = os;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getPod() {
        return pod;
    }

    public void setPod(Date pod) {
        this.pod = pod;
    }

    public Date getVald() {
        return vald;
    }

    public void setVald(Date vald) {
        this.vald = vald;
    }

    public String getBsaAcid() {
        return bsaAcid;
    }

    public void setBsaAcid(String bsaAcid) {
        this.bsaAcid = bsaAcid;
    }

    public BankCurrency getCcy() {
        return ccy;
    }

    public void setCcy(BankCurrency ccy) {
        this.ccy = ccy;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmountBC() {
        return amountBC;
    }

    public void setAmountBC(BigDecimal amountBC) {
        this.amountBC = amountBC;
    }

    public String getPbr() {
        return pbr;
    }

    public void setPbr(String pbr) {
        this.pbr = pbr;
    }

    public String getInvisible() {
        return invisible;
    }

    public void setInvisible(String invisible) {
        this.invisible = invisible;
    }

    public String getPnar() {
        return pnar;
    }

    public void setPnar(String pnar) {
        this.pnar = pnar;
    }

    public Long getPcId() {
        return pcId;
    }

    public void setPcId(Long pcId) {
        this.pcId = pcId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getRusNarrLong() {
        return rusNarrLong;
    }

    public void setRusNarrLong(String rusNarrLong) {
        this.rusNarrLong = rusNarrLong;
    }

    public String getRusNarrShort() {
        return rusNarrShort;
    }

    public void setRusNarrShort(String rusNarrShort) {
        this.rusNarrShort = rusNarrShort;
    }

    public Long getGlOperationId() {
        return glOperationId;
    }

    public void setGlOperationId(Long glOperationId) {
        this.glOperationId = glOperationId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Date getProcDate() {
        return procDate;
    }

    public void setProcDate(Date procDate) {
        this.procDate = procDate;
    }

    public String getDealId() {
        return dealId;
    }

    public void setDealId(String dealId) {
        this.dealId = dealId;
    }

    public String getSubdealId() {
        return subdealId;
    }

    public void setSubdealId(String subdealId) {
        this.subdealId = subdealId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getPaymentRef() {
        return paymentRef;
    }

    public void setPaymentRef(String paymentRef) {
        this.paymentRef = paymentRef;
    }

    public YesNo getIsCorrection() {
        return isCorrection;
    }

    public void setIsCorrection(YesNo isCorrection) {
        this.isCorrection = isCorrection;
    }

    public String getProfitCenter() {
        return profitCenter;
    }

    public void setProfitCenter(String profitCenter) {
        this.profitCenter = profitCenter;
    }

    public String getNarrative() {
        return narrative;
    }

    public void setNarrative(String narrative) {
        this.narrative = narrative;
    }

    public Long getGlAcID() {
        return glAcID;
    }

    public void setGlAcID(Long glAcID) {
        this.glAcID = glAcID;
    }

    public GLOperation.OperSide getOperSide() {
        return operSide;
    }

    public void setOperSide(GLOperation.OperSide operSide) {
        this.operSide = operSide;
    }
}
