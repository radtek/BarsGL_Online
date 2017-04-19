package ru.rbt.barsgl.ejb.entity.etl;

import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by ER18837 on 29.04.15.
 * @deprecated не используется
 */
@Entity
@Table(name = "GL_ETLACC")
public class EtlAccount extends BaseEntity<EtlAccountId> {

/*
    @ManyToOne
    @JoinColumn(name = "ID_PKG", nullable = false)
    private EtlPackage etlPackage;

    @Column(name = "BSAACID")
    private String id;
*/

    @EmbeddedId
    EtlAccountId id;

    @Column(name = "ACCTYPE")
    private Long accountType;

    @Column(name = "CLINO")
    private String customerNumber;

    @ManyToOne
    @JoinColumn(name = "CCY")
    private BankCurrency currency;

    @Column(name = "DTO")
    @Temporal(TemporalType.DATE)
    private Date dateOpen;

    @Column(name = "DTC")
    @Temporal(TemporalType.DATE)
    private Date dateClose;

    @Column(name = "CBCC")
    private String filial;

    @Column(name = "BRANCH")
    private String branch;

    @Column(name = "PLCODE")
    private String plCode;

/*
    @Column(name = "TERM")
    private String period;

    @Column(name = "CBCUSTTYPE")
    private String cbCustType;
*/

    @Column(name = "ECODE")
    private Integer errorCode;

    @Column(name = "EMSG")
    private String errorMessage;

    @Override
    public EtlAccountId getId() {
        return id;
    }

    public String getAccount() {return id.getBsaAcid(); }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public BankCurrency getCurrency() {
        return currency;
    }

    public Date getDateOpen() {
        return dateOpen;
    }

    public Date getDateClose() {
        return dateClose;
    }

    public String getFilial() {
        return filial;
    }

    public String getBranch() {
        return branch;
    }

    public String getPlCode() {
        return plCode;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public void setId(EtlAccountId id) {
        this.id = id;
    }

    public Long getAccountType() {
        return accountType;
    }

    public void setAccountType(Long accountType) {
        this.accountType = accountType;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public void setCurrency(BankCurrency currency) {
        this.currency = currency;
    }

    public void setDateOpen(Date dateOpen) {
        this.dateOpen = dateOpen;
    }

    public void setDateClose(Date dateClose) {
        this.dateClose = dateClose;
    }

    public void setFilial(String filial) {
        this.filial = filial;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public void setPlCode(String plCode) {
        this.plCode = plCode;
    }

}
