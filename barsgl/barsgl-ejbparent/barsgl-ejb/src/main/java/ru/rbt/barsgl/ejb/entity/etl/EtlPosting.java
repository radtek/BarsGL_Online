package ru.rbt.barsgl.ejb.entity.etl;

import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.util.StringUtils;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

import static ru.rbt.ejbcore.mapping.YesNo.Y;

/**
 * Created by Ivan Sevastyanov
 */
@Entity
@Table(name = "GL_ETLPST")
public class EtlPosting extends BaseEntity <Long> {

    //Признак технического счёта
    private static final String _TH = "TH";

    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "ID_PST")
    private String aePostingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_PKG", nullable = false)
    private EtlPackage etlPackage;

    @Column(name = "SRC_PST")
    private String sourcePosting;

    @Column(name = "EVT_ID")
    private String eventId;

    @Column(name = "EVTP")
    private String eventType;

    @Column(name = "DEAL_ID")
    private String dealId;

    @Column(name = "CHNL_NAME")
    private String chnlName;

    @Column(name = "PMT_REF")
    private String paymentRefernce;

    @Column(name = "DEPT_ID")
    private String deptId;

    @Temporal(TemporalType.DATE)
    @Column(name = "VDATE")
    private Date valueDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "OTS")
    private Date operationTimestamp;

    @Column(name = "NRT")
    private String narrative;

    @Column(name = "RNRTL")
    private String rusNarrativeLong;

    @Column(name = "RNRTS")
    private String rusNarrativeShort;

    @Enumerated(EnumType.STRING)
    @Column(name = "STRN")
    private YesNo storno;

    @Column(name = "STRNRF")
    private String stornoReference;

    @Column(name = "AC_DR")
    private String accountDebit;

    @ManyToOne
    @JoinColumn(name = "CCY_DR")
    private BankCurrency currencyDebit;

    @Column(name = "AMT_DR")
    private BigDecimal amountDebit;

    @Column(name = "AMTRU_DR")
    private BigDecimal amountDebitRu;

    @Column(name = "AC_CR")
    private String accountCredit;

    @ManyToOne
    @JoinColumn(name = "CCY_CR")
    private BankCurrency currencyCredit;

    @Column(name = "AMT_CR")
    private BigDecimal amountCredit;

    @Column(name = "AMTRU_CR")
    private BigDecimal amountCreditRu;

    @Enumerated(EnumType.STRING)
    @Column(name = "FAN")
    private YesNo fan;

    @Column(name = "PAR_RF")
    private String parentReference;

    @Column(name="ACCKEY_DR")
    private String accountKeyDebit;

    @Column(name="ACCKEY_CR")
    private String accountKeyCredit;

    @Column(name = "ECODE")
    private Integer errorCode;

    @Column(name = "EMSG")
    private String errorMessage;

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

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getDealId() {
        return dealId;
    }

    public void setDealId(String dealId) {
        this.dealId = dealId;
    }

    public String getChnlName() {
        return chnlName;
    }

    public void setChnlName(String chnlName) {
        this.chnlName = chnlName;
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

    public Date getOperationTimestamp() {
        return operationTimestamp;
    }

    public void setOperationTimestamp(Date operationTimestamp) {
        this.operationTimestamp = operationTimestamp;
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

    public YesNo getStorno() {
        return storno;
    }

    public void setStorno(YesNo storno) {
        this.storno = storno;
    }

    public String getStornoReference() {
        return stornoReference;
    }

    public void setStornoReference(String stornoReference) {
        this.stornoReference = stornoReference;
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

    public BigDecimal getAmountDebitRu() {
        return amountDebitRu;
    }

    public void setAmountDebitRu(BigDecimal amountDebitRu) {
        this.amountDebitRu = amountDebitRu;
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

    public BigDecimal getAmountCreditRu() {
        return amountCreditRu;
    }

    public void setAmountCreditRu(BigDecimal amountCreditRu) {
        this.amountCreditRu = amountCreditRu;
    }

    public YesNo getFan() {
        return fan;
    }

    public void setFan(YesNo fan) {
        this.fan = fan;
    }

    public String getParentReference() {
        return parentReference;
    }

    public void setParentReference(String parentReference) {
        this.parentReference = parentReference;
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

    public EtlPackage getEtlPackage() {
        return etlPackage;
    }

    public void setEtlPackage(EtlPackage etlPackage) {
        this.etlPackage = etlPackage;
    }

    public boolean isStorno() {
        return storno == Y;
    }

    public boolean isFan() {
        return fan == Y;
    }

    public String getAePostingId() {
        return aePostingId;
    }

    public void setAePostingId(String aePostingId) {
        this.aePostingId = aePostingId;
    }

    public String getAccountKeyDebit() {
        return accountKeyDebit;
    }

    public void setAccountKeyDebit(String accountKeyDebit) {
        this.accountKeyDebit = accountKeyDebit;
    }

    public String getAccountKeyCredit() {
        return accountKeyCredit;
    }

    public void setAccountKeyCredit(String accountKeyCredit) {
        this.accountKeyCredit = accountKeyCredit;
    }

    // если валюта - РУБЛЬ, возвращает поле "сумма в валюте", иначе "сумму в рублях"
    public BigDecimal getAmountDebitRUR() {
        return (BankCurrency.RUB.equals(this.currencyDebit)) ? this.amountDebit : this.amountDebitRu;
    }

    public BigDecimal getAmountCreditRUR() {
        return (BankCurrency.RUB.equals(this.currencyCredit)) ? this.amountCredit : this.amountCreditRu;
    }

    public boolean isCurrencyDebitRUR() {
        return BankCurrency.RUB.equals(currencyDebit);
    }

    public boolean isCurrencyCreditRUR() {
        return BankCurrency.RUB.equals(currencyCredit);
    }

    /**
     * Свойство определяющее статус технического счёта
     * @return
     */
    public boolean isTech()
    {
        try {
            AccountKeys keysDb = new AccountKeys(this.getAccountKeyDebit());
            AccountKeys keysCr = new AccountKeys(this.getAccountKeyCredit());
            String glseq1 = StringUtils.substr(keysDb.getGlSequence(), 0, 2);
            String glseq2 = StringUtils.substr(keysCr.getGlSequence(), 0, 2);

            if (glseq1.equals(_TH) && glseq2.equals(_TH)) {
                return true;

            } else {
                return false;
            }
        }
        catch (Exception ex)
        {
            return false;
        }
    }

}
