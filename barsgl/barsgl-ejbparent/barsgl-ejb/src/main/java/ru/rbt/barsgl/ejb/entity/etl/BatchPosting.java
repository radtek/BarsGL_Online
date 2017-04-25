package ru.rbt.barsgl.ejb.entity.etl;

import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejbcore.mapping.BaseEntity;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.InputMethod;
import ru.rbt.barsgl.shared.enums.InvisibleType;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by ER18837 on 26.02.16.
 */
@Entity
@Table(name = "GL_BATPST")
public class BatchPosting extends BaseEntity<Long> {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

/*
    @ManyToOne
    @JoinColumn(name = "ID_PKG", nullable = false)
    private BatchPackage batchPackage;
*/

    @Column(name = "ID_PKG")
    private Long packageId;

    @Column(name = "SRC_PST")
    private String sourcePosting;

    @Column(name = "DEAL_ID")
    private String dealId;

    @Column(name = "PMT_REF")
    private String paymentRefernce;

    @Column(name = "DEPT_ID")
    private String deptId;

    @Temporal(TemporalType.DATE)
    @Column(name = "VDATE")
    private Date valueDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "POSTDATE")
    private Date postDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "OTS")
    private Date createTimestamp;

    @Column(name = "NRT")
    private String narrative;

    @Column(name = "RNRTL")
    private String rusNarrativeLong;

    @Column(name = "RNRTS")
    private String rusNarrativeShort;

    @Column(name = "AC_DR")
    private String accountDebit;

    @Column(name = "CBCC_DR")
    private String filialDebit;

    @ManyToOne
    @JoinColumn(name = "CCY_DR")
    private BankCurrency currencyDebit;

    @Column(name = "AMT_DR")
    private BigDecimal amountDebit;

    @Column(name = "AC_CR")
    private String accountCredit;

    @Column(name = "CBCC_CR")
    private String filialCredit;

    @ManyToOne
    @JoinColumn(name = "CCY_CR", nullable = false)
    private BankCurrency currencyCredit;

    @Column(name = "AMT_CR")
    private BigDecimal amountCredit;

    @Column(name = "AMTRU")
    private BigDecimal amountRu;

    @Column(name = "SUBDEALID")
    private String subDealId;

    @Enumerated(EnumType.STRING)
    @Column(name = "FCHNG")
    private YesNo isCorrection;

    @Column(name = "PRFCNTR")
    private String profitCenter;

    @Column(name = "USER_NAME")
    private String userName;

    @Column(name = "NROW")
    private Integer rowNumber;

    @Column(name = "ECODE")
    private Integer errorCode;

    @Column(name = "EMSG")
    private String errorMessage;

    @Enumerated (EnumType.STRING)
    @Column(name = "INP_METHOD")
    private InputMethod inputMethod;

    @Temporal(TemporalType.DATE)
    @Column(name = "PROCDATE")
    private Date procDate;

    @Enumerated (EnumType.STRING)
    @Column(name = "INVISIBLE")
    private InvisibleType invisible;

    @Column(name = "HEADBRANCH")
    private String userFilial;

    @Column(name = "USER_AU2")
    private String signerName;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "OTS_AU2")
    private Date signerTamestamp;

    @Column(name = "USER_AU3")
    private String confirmName;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "OTS_AU3")
    private Date confirmTimestamp;

    @Column(name = "USER_CHNG")
    private String changeName;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "OTS_CHNG")
    private Date changeTimestamp;

    @Enumerated (EnumType.STRING)
    @Column(name = "STATE")
    private BatchPostStatus status;

    @ManyToOne
    @JoinColumn(name = "GLOID_REF")
    private GLOperation operation;

    @Column(name = "ID_PAR")
    private Long parentPostingId;

    @Column(name = "ID_PREV")
    private Long historyPostingId;

    @Column(name = "DESCRDENY")
    private String reasonOfDeny;

    @Column(name = "SRV_REF")
    private String movementId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "OTS_SRV")
    private Date receiveTimestamp;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "SEND_SRV")
    private Date sendTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "TECH_ACT")
    private YesNo isTech;

    @Transient
    private boolean controllableDebit;

    @Transient
    private boolean controllableCredit;

    public BatchPosting()
    {
        this.isTech = YesNo.N;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getSourcePosting() {
        return sourcePosting;
    }

    public void setSourcePosting(String sourcePosting) {
        this.sourcePosting = sourcePosting;
    }

    public String getDealId() {
        return dealId;
    }

    public void setDealId(String dealId) {
        this.dealId = dealId;
    }

    public String getPaymentRefernce() {
        return paymentRefernce;
    }

    public void setPaymentRefernce(String paymentRefernce) {
        this.paymentRefernce = paymentRefernce;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public Date getValueDate() {
        return valueDate;
    }

    public void setValueDate(Date valueDate) {
        this.valueDate = valueDate;
    }

    public Date getPostDate() {
        return postDate;
    }

    public void setPostDate(Date postDate) {
        this.postDate = postDate;
    }

    public Date getCreateTimestamp() {
        return createTimestamp;
    }

    public void setCreateTimestamp(Date createTimestamp) {
        this.createTimestamp = createTimestamp;
    }

    public String getNarrative() {
        return narrative;
    }

    public void setNarrative(String narrative) {
        this.narrative = narrative;
    }

    public String getRusNarrativeLong() {
        return rusNarrativeLong;
    }

    public void setRusNarrativeLong(String rusNarrativeLong) {
        this.rusNarrativeLong = rusNarrativeLong;
    }

    public String getRusNarrativeShort() {
        return rusNarrativeShort;
    }

    public void setRusNarrativeShort(String rusNarrativeShort) {
        this.rusNarrativeShort = rusNarrativeShort;
    }

    public String getAccountDebit() {
        return accountDebit;
    }

    public void setAccountDebit(String accountDebit) {
        this.accountDebit = accountDebit;
    }

    public BankCurrency getCurrencyDebit() {
        return currencyDebit;
    }

    public void setCurrencyDebit(BankCurrency currencyDebit) {
        this.currencyDebit = currencyDebit;
    }

    public BigDecimal getAmountDebit() {
        return amountDebit;
    }

    public void setAmountDebit(BigDecimal amountDebit) {
        this.amountDebit = amountDebit;
    }

    public String getAccountCredit() {
        return accountCredit;
    }

    public void setAccountCredit(String accountCredit) {
        this.accountCredit = accountCredit;
    }

    public BankCurrency getCurrencyCredit() {
        return currencyCredit;
    }

    public void setCurrencyCredit(BankCurrency currencyCredit) {
        this.currencyCredit = currencyCredit;
    }

    public BigDecimal getAmountCredit() {
        return amountCredit;
    }

    public void setAmountCredit(BigDecimal amountCredit) {
        this.amountCredit = amountCredit;
    }

    public BigDecimal getAmountRu() {
        return amountRu;
    }

    public void setAmountRu(BigDecimal amountRu) {
        this.amountRu = amountRu;
    }

    public String getSubDealId() {
        return subDealId;
    }

    public void setSubDealId(String subDealId) {
        this.subDealId = subDealId;
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public InputMethod getInputMethod() {
        return inputMethod;
    }

    public void setInputMethod(InputMethod inputMethod) {
        this.inputMethod = inputMethod;
    }

    public Date getProcDate() {
        return procDate;
    }

    public void setProcDate(Date procDate) {
        this.procDate = procDate;
    }

    public String getSignerName() {
        return signerName;
    }

    public void setSignerName(String signerName) {
        this.signerName = signerName;
    }

    public Date getSignerTamestamp() {
        return signerTamestamp;
    }

    public void setSignerTamestamp(Date signerTamestamp) {
        this.signerTamestamp = signerTamestamp;
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

    public String getChangeName() {
        return changeName;
    }

    public void setChangeName(String changeName) {
        this.changeName = changeName;
    }

    public Date getChangeTimestamp() {
        return changeTimestamp;
    }

    public void setChangeTimestamp(Date changeTimestamp) {
        this.changeTimestamp = changeTimestamp;
    }

    public GLOperation getOperation() {
        return operation;
    }

    public void setOperation(GLOperation operation) {
        this.operation = operation;
    }

    public Long getParentPostingId() {
        return parentPostingId;
    }

    public void setParentPostingId(Long parentPostingId) {
        this.parentPostingId = parentPostingId;
    }

    public Long getHistoryPostingId() {
        return historyPostingId;
    }

    public void setHistoryPostingId(Long historyPostingId) {
        this.historyPostingId = historyPostingId;
    }

    public String getReasonOfDeny() {
        return reasonOfDeny;
    }

    public void setReasonOfDeny(String reasonOfDeny) {
        this.reasonOfDeny = reasonOfDeny;
    }

    public String getFilialDebit() {
        return filialDebit;
    }

    public void setFilialDebit(String filialDebit) {
        this.filialDebit = filialDebit;
    }

    public String getFilialCredit() {
        return filialCredit;
    }

    public void setFilialCredit(String filialCredit) {
        this.filialCredit = filialCredit;
    }

    public String getUserFilial() {
        return userFilial;
    }

    public void setUserFilial(String userFilial) {
        this.userFilial = userFilial;
    }

    public BatchPostStatus getStatus() {
        return status;
    }

    public void setStatus(BatchPostStatus status) {
        this.status = status;
    }

    public InvisibleType getInvisible() {
        return invisible;
    }

    public void setInvisible(InvisibleType invisible) {
        this.invisible = invisible;
    }

    public String getMovementId() {
        return movementId;
    }

    public void setMovementId(String movementId) {
        this.movementId = movementId;
    }

    public Date getReceiveTimestamp() {
        return receiveTimestamp;
    }

    public void setReceiveTimestamp(Date receiveTimestamp) {
        this.receiveTimestamp = receiveTimestamp;
    }

    public Date getSendTimestamp() {
        return sendTimestamp;
    }

    public void setSendTimestamp(Date sendTimestamp) {
        this.sendTimestamp = sendTimestamp;
    }

    public Long getPackageId() {
        return packageId;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
    }

    public boolean isControllableDebit() {
        return controllableDebit;
    }

    public boolean isControllableCredit() {
        return controllableCredit;
    }

    public void setControllableDebit(boolean controllableDebit) {
        this.controllableDebit = controllableDebit;
    }

    public void setControllableCredit(boolean controllableCredit) {
        this.controllableCredit = controllableCredit;
    }

    public boolean isControllable() {
        return controllableDebit || controllableCredit;
    }

    public YesNo getIsTech() {
        return isTech;
    }

    public void setIsTech(YesNo isTech) {
        this.isTech = isTech;
    }
}
