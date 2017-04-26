package ru.rbt.barsgl.ejb.entity.gl;

import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.ejbcore.util.StringUtils;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by Ivan Sevastyanov
 */
@MappedSuperclass
public abstract class AbstractPd extends BaseEntity<Long> implements Comparable<AbstractPd> {

    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "PCID", nullable = false)
    private Long pcId;

    @Column(name = "POD")
    @Temporal(TemporalType.DATE)
    private Date pod;

    @Column(name = "VALD")
    @Temporal(TemporalType.DATE)
    private Date vald;

    @Column(name = "ACID")
    private String acid = " ";        // 20

    @Column(name = "BSAACID")
    private String bsaAcid;     // 200

    @ManyToOne
    @JoinColumn(name = "CCY")
    private BankCurrency ccy;

    @Column(name = "AMNT")
    private Long amount;

    @Column(name = "AMNTBC")
    private Long amountBC;

    @Column(name = "AMNTUC")
    protected Long amountUC;

    @Column(name = "PBR")
    private String pbr;         // 7

    @Column(name = "PDRF")
    protected Long pdrf;

    @Column(name = "CPDRF")
    protected Long stornoRef;

    @Column(name = "INVISIBLE")
    protected String invisible;   // 1

    @Column(name = "FINALETRS") // 1
    protected String finaletrs;

    @Column(name = "LASTD")     // 1
    protected String lastd;

    @Column(name = "STATUS")    // 1
    protected String status;

    @Column(name = "ASOC")
    protected Integer asoc;

    @Column(name = "PNAR")      // 30
    private String pnar;

    @Column(name = "CTYPE")     // 3
    protected String ctype;

    // PDEXT
    @Column(table="PDEXT", name = "PREF")
    private String pref;

    @Column(table="PDEXT", name = "DLID")
    private Long dlId;

    @Column(table="PDEXT", name = "DLTYPE")
    protected String dlType;

    @Column(table="PDEXT", name = "DPMT")
    private String department;

    @Column(table="PDEXT", name = "FLEX_EVENT_CODE")
    protected String flexCode;

    // PDEXT2
    @Column(table="PDEXT2", name = "RNARLNG")
    private String rusNarrLong;             // 300

    @Column(table="PDEXT2", name = "RNARSHT")
    private String rusNarrShort;            // 100

    @Column(table="PDEXT2", name = "OREF")
    protected String operReference;           // 2

    @Column(table="PDEXT2", name = "DOCN")
    protected String docNumber;               // 10

    @Column(table="PDEXT2", name = "OREF_SRC")
    protected String operRefSource;           // 1

    // PDEXT3
    @Column(table="PDEXT3", name = "OPERATOR")
    private String operator;                // 35

    @Column(table="PDEXT3", name = "OPER_DEPT")
    private String operatorDepartment;      // 3

    @Column(table="PDEXT3", name = "AUTHORIZER")
    private String authorizer;              // 35

    @Column(table="PDEXT3", name = "AUTH_DEPT")
    private String authorizerDepartment;    // 3

    // PDEXT5
    @Column(table="PDEXT5", name = "GLO_REF")
    private Long glOperationId;

    @Column(table="PDEXT5", name = "EVTP")
    private String eventType;               // 20

    @Column(table="PDEXT5", name = "PROCDATE")
    @Temporal(TemporalType.DATE)
    private Date procDate;

    @Column(table="PDEXT5", name = "DEAL_ID")
    private String dealId;                  // 20

    @Column(table="PDEXT5", name = "SUBDEALID")
    private String subdealId;               // 20

    @Column(table="PDEXT5", name = "EVT_ID")
    private String eventId;                 // 20

    @Column(table="PDEXT5", name = "PMT_REF")
    private String paymentRef;              // 20

    @Enumerated(EnumType.STRING)
    @Column(table="PDEXT5", name = "FCHNG")
    private YesNo isCorrection;            // 1

    @Column(table="PDEXT5", name = "PRFCNTR")
    private String profitCenter;            // 4

    @Column(table="PDEXT5", name = "NRT")
    private String narrative;               // 300

    public AbstractPd() {
        init();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPcId() {
        return pcId;
    }

    public void setPcId(Long pcId) {
        this.pcId = pcId;
    }

    public Long getAmount() {
        return amount;
    }

    public Date getPod() {
        return pod;
    }

    public Date getVald() {
        return vald;
    }

    public String getAcid() {
        return acid;
    }

    public String getBsaAcid() {
        return bsaAcid;
    }

    public BankCurrency getCcy() {
        return ccy;
    }

    public Long getAmountBC() {
        return amountBC;
    }

    public String getPnar() {
        return pnar;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public void setPod(Date pod) {
        this.pod = pod;
    }

    public void setVald(Date vald) {
        this.vald = vald;
    }

    public void setAcid(String acid) {
        this.acid = StringUtils.ifEmpty(acid, " ");
    }

    public void setBsaAcid(String bsaAcid) {
        this.bsaAcid = bsaAcid;
    }

    public void setCcy(BankCurrency ccy) {
        this.ccy = ccy;
    }

    public void setAmountBC(Long amountBC) {
        this.amountBC = amountBC;
    }

    public void setAmountUC(Long amountUC) {
        this.amountUC = amountUC;
    }

    public void setPbr(String pbr) {
        this.pbr = pbr;
    }

    public void setPdrf(Long pdrf) {
        this.pdrf = pdrf;
    }

    public void setInvisible(String invisible) {
        this.invisible = invisible;
    }

    public void setFinaletrs(String finaletrs) {
        this.finaletrs = finaletrs;
    }

    public void setLastd(String lastd) {
        this.lastd = lastd;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setAsoc(Integer asoc) {
        this.asoc = asoc;
    }

    public Integer getAsoc() {
        return asoc;
    }

    public void setPnar(String pnar) {
        this.pnar = pnar;
    }

    public void setCtype(String ctype) {
        this.ctype = ctype;
    }

    public String getCtype() {
        return ctype;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setPref(String pref) {
        this.pref = pref;
    }

    public void setRusNarrShort(String rusNarrShort) {
        this.rusNarrShort = rusNarrShort;
    }

    public void setRusNarrLong(String rusNarrLong) {
        this.rusNarrLong = rusNarrLong;
    }

    public void setAuthorizerDepartment(String authorizerDepartment) {
        this.authorizerDepartment = authorizerDepartment;
    }

    public void setAuthorizer(String authorizer) {
        this.authorizer = authorizer;
    }

    public void setOperatorDepartment(String operatorDepartment) {
        this.operatorDepartment = operatorDepartment;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getAuthorizerDepartment() {
        return authorizerDepartment;
    }

    public String getAuthorizer() {
        return authorizer;
    }

    public String getOperatorDepartment() {
        return operatorDepartment;
    }

    public String getOperator() {
        return operator;
    }

    @Override
    public int compareTo(AbstractPd pd) {
        return this.bsaAcid.compareTo(pd.getBsaAcid());
    }

    public String getInvisible() {
        return invisible;
    }

    public String getRusNarrShort() {
        return rusNarrShort;
    }

    public String getRusNarrLong() {
        return rusNarrLong;
    }

    public String getDepartment() {
        return department;
    }

    public String getPref() {
        return pref;
    }

    public String getOperReference() {
        return operReference;
    }

    public String getDocNumber() {
        return docNumber;
    }

    public String getOperRefSource() {
        return operRefSource;
    }

    public String getPbr() {
        return pbr;
    }

    public Long getStornoRef() {
        return stornoRef;
    }

    public void setStornoRef(Long stornoRef) {
        this.stornoRef = stornoRef;
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

    public void setOperReference(String operReference) {
        this.operReference = operReference;
    }

    public Long getGlOperationId() {
        return glOperationId;
    }

    public void setGlOperationId(Long glOperationId) {
        this.glOperationId = glOperationId;
    }

    public String getNarrative() {
        return narrative;
    }

    public void setNarrative(String narrative) {
        this.narrative = narrative;
    }

    public String getProfitCenter() {
        return profitCenter;
    }

    public void setProfitCenter(String profitCenter) {
        this.profitCenter = profitCenter;
    }

    public YesNo getIsCorrection() {
        return isCorrection;
    }

    public void setIsCorrection(YesNo isCorrection) {
        this.isCorrection = isCorrection;
    }

    public String getPaymentRef() {
        return paymentRef;
    }

    public void setPaymentRef(String paymentRef) {
        this.paymentRef = paymentRef;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    private void init() {
        // PD
        this.acid = " ";
        this.ctype = "";
        this.amountUC = 0L;
        this.pdrf = 0L;
        this.stornoRef = 0L;
        this.invisible = "0";
        this.finaletrs = "";
        this.lastd = "";
        this.status = "";
        this.asoc = 0;

        // PDEXT
        this.dlType = "";
        this.flexCode = "";
        // PDEXT2
        this.operReference = "";
        this.docNumber = "";
        this.operRefSource = "";

    }

    @Override
    public String toString() {
        return getId().toString();
    }
}
