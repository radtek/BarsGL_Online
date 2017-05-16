package ru.rbt.barsgl.ejb.entity.acc;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by ER18837 on 28.04.15.
 */
@Entity
@Table(name = "ACCRLN")
public class GlAccRln extends BaseEntity<AccRlnId> {

    @EmbeddedId
    private AccRlnId id;

    @Column(name = "RLNTYPE")
    private String relationType;

    @Column(name = "DRLNO")
    @Temporal(TemporalType.DATE)
    private Date dateOpen;

    @Column(name = "DRLNC")
    @Temporal(TemporalType.DATE)
    private Date dateClose;

    @Column(name = "CTYPE")
    private Short customerType;

    @Column(name = "CNUM")
    private String customerNumber;

    @Column(name = "CCODE")
    private String companyCode;

    @Column(name = "ACC2")
    private String bssAccount;

    @Column(name = "PSAV")
    private String passiveActive;

    @Column(name = "GLACOD")
    private String accountCode;

    @Column(name = "CBCCY")
    private String currencyD;

    @Column(name = "PLCODE")
    private String plCode;

    // Далее по умолчанию
    @Column(name = "INCL")
    private String includeExclude;

    @Column(name = "PAIRBSA")
    private String pairBsa;

    @Column(name = "TRANSACTSRC")
    private String transactSrc;

    @Override
    public AccRlnId getId() {
        return id;
    }

    @Override
    public void setId(AccRlnId id) {
        this.id = id;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }

    public void setDateOpen(Date dateOpen) {
        this.dateOpen = dateOpen;
    }

    public void setDateClose(Date dateClose) {
        this.dateClose = dateClose;
    }

    public void setCustomerType(Short customerType) {
        this.customerType = customerType;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public void setBssAccount(String bssAccount) {
        this.bssAccount = bssAccount;
    }

    public void setPassiveActive(String passiveActive) {
        this.passiveActive = passiveActive;
    }

    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    public void setCurrencyD(String currencyD) {
        this.currencyD = currencyD;
    }

    public void setPlCode(String plCode) {
        this.plCode = plCode;
    }

    public void setIncludeExclude(String includeExclude) {
        this.includeExclude = includeExclude;
    }

    public void setTransactSrc(String transactSrc) {
        this.transactSrc = transactSrc;
    }

    public String getRelationType() {
        return relationType;
    }

    public Date getDateOpen() {
        return dateOpen;
    }

    public Date getDateClose() {
        return dateClose;
    }

    public Short getCustomerType() {
        return customerType;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public String getBssAccount() {
        return bssAccount;
    }

    public String getPassiveActive() {
        return passiveActive;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public String getCurrencyD() {
        return currencyD;
    }

    public String getPlCode() {
        return plCode;
    }

    public String getIncludeExclude() {
        return includeExclude;
    }

    public String getPairBsa() {
        return pairBsa;
    }

    public String getTransactSrc() {
        return transactSrc;
    }

    public void setPairBsa(String pairBsa) {
        this.pairBsa = pairBsa;
    }
}
