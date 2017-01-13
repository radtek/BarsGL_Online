package ru.rbt.barsgl.ejb.entity.card;

import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by ER22317 on 23.09.2016.
 */
@Entity
@Table(name = "GL_CARDPST")
public class CardPst extends BaseEntity<Long> {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ID_PKG")
    private Long packageId;

    @Column(name = "NROW")
    private Integer rowNumber;

    @Temporal(TemporalType.DATE)
    @Column(name = "PDATE")
    private Date valueDate;

    @Column(name = "DEAL_ID")
    private String dealId;

    @Column(name = "SUBDEALID")
    private String subdealId;

    @Column(name = "PMT_REF")
    private String paymentRefernce;

    @Column(name = "AC_DR")
    private String accountDebit;

    @ManyToOne
    @JoinColumn(name = "CCY_DR")
    private BankCurrency currencyDebit;

    @Column(name = "AMT_DR")
    private BigDecimal amountDebit;

    @Column(name = "AC_CR")
    private String accountCredit;

    @ManyToOne
    @JoinColumn(name = "CCY_CR", nullable = false)
    private BankCurrency currencyCredit;

    @Column(name = "AMT_CR")
    private BigDecimal amountCredit;

    @Column(name = "AMTRU")
    private BigDecimal amountRu;

    @Column(name = "NRT")
    private String nrt;

    @Column(name = "RNRTL")
    private String rnrtl;

    @Column(name = "RNRTS")
    private String rnrts;

    @Column(name = "PRFCNTR")
    private String prfcntr;

    @Column(name = "FGHNG")
    private String fghng;


    public String getFghng() {
        return fghng;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }

    public void setValueDate(Date valueDate) {
        this.valueDate = valueDate;
    }

    public void setDealId(String dealId) {
        this.dealId = dealId;
    }

    public void setSubdealId(String subdealId) {
        this.subdealId = subdealId;
    }

    public void setPaymentRefernce(String paymentRefernce) {
        this.paymentRefernce = paymentRefernce;
    }

    public void setAccountDebit(String accountDebit) {
        this.accountDebit = accountDebit;
    }

    public void setCurrencyDebit(BankCurrency currencyDebit) {
        this.currencyDebit = currencyDebit;
    }

    public void setAmountDebit(BigDecimal amountDebit) {
        this.amountDebit = amountDebit;
    }

    public void setAccountCredit(String accountCredit) {
        this.accountCredit = accountCredit;
    }

    public void setCurrencyCredit(BankCurrency currencyCredit) {
        this.currencyCredit = currencyCredit;
    }

    public void setAmountCredit(BigDecimal amountCredit) {
        this.amountCredit = amountCredit;
    }

    public void setAmountRu(BigDecimal amountRu) {
        this.amountRu = amountRu;
    }

    public void setNrt(String nrt) {
        this.nrt = nrt;
    }

    public void setRnrtl(String rnrtl) {
        this.rnrtl = rnrtl;
    }

    public void setRnrts(String rnrts) {
        this.rnrts = rnrts;
    }

    public void setPrfcntr(String prfcntr) {
        this.prfcntr = prfcntr;
    }

    public void setFghng(String fghng) {
        this.fghng = fghng;
    }

    @Override
    public Long getId() {
        return id;
    }

    public Long getPackageId() {
        return packageId;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public Date getValueDate() {
        return valueDate;
    }

    public String getDealId() {
        return dealId;
    }

    public String getSubdealId() {
        return subdealId;
    }

    public String getPaymentRefernce() {
        return paymentRefernce;
    }

    public String getAccountDebit() {
        return accountDebit;
    }

    public BankCurrency getCurrencyDebit() {
        return currencyDebit;
    }

    public BigDecimal getAmountDebit() {
        return amountDebit;
    }

    public String getAccountCredit() {
        return accountCredit;
    }

    public BankCurrency getCurrencyCredit() {
        return currencyCredit;
    }

    public BigDecimal getAmountCredit() {
        return amountCredit;
    }

    public BigDecimal getAmountRu() {
        return amountRu;
    }

    public String getNrt() {
        return nrt;
    }

    public String getRnrtl() {
        return rnrtl;
    }

    public String getRnrts() {
        return rnrts;
    }

    public String getPrfcntr() {
        return prfcntr;
    }

}
