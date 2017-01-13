package ru.rbt.barsgl.ejb.entity.flx;

import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejbcore.mapping.BaseEntity;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by Ivan Sevastyanov on 23.11.2016.
 */
@Entity
@Table(name = "GL_NDSPST")
public class FanNdsPosting extends BaseEntity<Long> {

    @Id
    @Column(name = "ID_PST")
    private Long id;

    @Column(name = "EVT_ID")
    private Long evtId;

    @Column(name = "PMT_REF")
    private String paymentReference;

    @Temporal(TemporalType.DATE)
    @Column(name = "VDATE")
    private Date valueDate;

    @Column(name = "PAR_RF")
    private String fanReferences;

    @Column(name = "NRT")
    private String narrativeEn;

    @Column(name = "RNRTL")
    private String narrativeRuLong;

    @Column(name = "RNRTS")
    private String narrativeRuShort;

    @Column(name = "AC_DR")
    private String accountDebit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CCY_DR")
    private BankCurrency currencyDebit;

    @Column(name = "AMT_DR")
    private BigDecimal amountDebit;

    @Column(name = "AC_CR")
    private String accountCredit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CCY_CR")
    private BankCurrency currencyCredit;

    @Column(name = "AMT_CR")
    private BigDecimal amountCredit;

    @Column(name = "EVTP")
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "PROCESSED")
    private YesNo processed;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Long getEvtId() {
        return evtId;
    }

    public void setEvtId(Long evtId) {
        this.evtId = evtId;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public Date getValueDate() {
        return valueDate;
    }

    public void setValueDate(Date valueDate) {
        this.valueDate = valueDate;
    }

    public String getFanReferences() {
        return fanReferences;
    }

    public void setFanReferences(String fanReferences) {
        this.fanReferences = fanReferences;
    }

    public String getNarrativeEn() {
        return narrativeEn;
    }

    public void setNarrativeEn(String narrativeEn) {
        this.narrativeEn = narrativeEn;
    }

    public String getNarrativeRuLong() {
        return narrativeRuLong;
    }

    public void setNarrativeRuLong(String narrativeRuLong) {
        this.narrativeRuLong = narrativeRuLong;
    }

    public String getNarrativeRuShort() {
        return narrativeRuShort;
    }

    public void setNarrativeRuShort(String narrativeRuShort) {
        this.narrativeRuShort = narrativeRuShort;
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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public YesNo getProcessed() {
        return processed;
    }

    public void setProcessed(YesNo processed) {
        this.processed = processed;
    }
}
