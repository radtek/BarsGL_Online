package ru.rbt.barsgl.ejb.entity.acc;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by ER18837 on 28.04.15.
 */
@Entity
@Table(name = "BSAACC")
public class BsaAcc extends BaseEntity<String> {

    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "BSSAC")
    private String bssAccount;

    @Column(name = "CCY")
    private String currencyD;

    @Column(name = "BSAKEY")
    private String bsaKey;

    @Column(name = "BRCA")
    private String companyCode;

    @Column(name = "BSACODE")
    private String bsaCode;

    @Column(name = "BSAACO")
    @Temporal(TemporalType.DATE)
    private Date dateOpen;

    @Column(name = "BSAACC")
    @Temporal(TemporalType.DATE)
    private Date dateClose;

    @Column(name = "BSATYPE")
    private String passiveActive;

    @Column(name = "BSAGRP")
    private String bsaGroup;

    @Column(name = "BSAACNDAT")
    @Temporal(TemporalType.DATE)
    private Date dateContractOpen;

    @Column(name = "BSAACNNUM")
    private String customerNumber;

    @Column(name = "BSAACTAX")
    @Temporal(TemporalType.DATE)
    private Date dateTax;

    // Далее по умолчанию
    @Column(name = "BSAACREF")
    private String description;

    @Column(name = "BSASUBTYPE")
    private String bsaSubtype;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public void setBssAccount(String bssAccount) {
        this.bssAccount = bssAccount;
    }

    public void setCurrencyD(String currencyD) {
        this.currencyD = currencyD;
    }

    public void setBsaKey(String bsaKey) {
        this.bsaKey = bsaKey;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public void setBsaCode(String bsaCode) {
        this.bsaCode = bsaCode;
    }

    public void setDateOpen(Date dateOpen) {
        this.dateOpen = dateOpen;
    }

    public void setDateClose(Date dateClose) {
        this.dateClose = dateClose;
    }

    public void setPassiveActive(String passiveActive) {
        this.passiveActive = passiveActive;
    }

    public void setBsaGroup(String bsaGroup) {
        this.bsaGroup = bsaGroup;
    }

    public void setDateContractOpen(Date dateContractOpen) {
        this.dateContractOpen = dateContractOpen;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public void setDateTax(Date dateTax) {
        this.dateTax = dateTax;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setBsaSubtype(String bsaSubtype) {
        this.bsaSubtype = bsaSubtype;
    }

    public String getBssAccount() {
        return bssAccount;
    }

    public String getCurrencyD() {
        return currencyD;
    }

    public String getBsaKey() {
        return bsaKey;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public String getBsaCode() {
        return bsaCode;
    }

    public Date getDateOpen() {
        return dateOpen;
    }

    public Date getDateClose() {
        return dateClose;
    }

    public String getPassiveActive() {
        return passiveActive;
    }

    public String getBsaGroup() {
        return bsaGroup;
    }

    public Date getDateContractOpen() {
        return dateContractOpen;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public Date getDateTax() {
        return dateTax;
    }

    public String getDescription() {
        return description;
    }

    public String getBsaSubtype() {
        return bsaSubtype;
    }
}
