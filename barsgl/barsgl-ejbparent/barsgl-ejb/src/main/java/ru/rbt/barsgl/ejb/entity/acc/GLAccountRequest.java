package ru.rbt.barsgl.ejb.entity.acc;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by ER18837 on 16.10.15.
 */
@Entity
@Table(name = "GL_ACOPENRQ")
public class GLAccountRequest extends BaseEntity<String> {

    public enum RequestStatus {NEW, OK};

    @Id
    @Column(name = "REQUEST_ID")
    private String id;

    // --- Основные ключи ---

    @Column(name = "BRANCH")
    private String branchFlex;

    @Column(name = "CCY")
    private String currency;

    @Column(name = "CUSTOMER_NO")
    private String customerNumber;

    @Column(name = "ACCOUNTING_TYPE")
    private String accountType;

    @Column(name = "CUSTOMER_CBTYPE")
    private String cbCustomerType;

    @Column(name = "TERM")
    private String term;

    @Column(name = "AC_DEALS_RS")
    private String dealSource;

    @Column(name = "DEAL_ID")
    private String dealId;

    @Column(name = "SUBDEAL_ID")
    private String subDealId;

    @Column(name = "OPEN_DATE")
    @Temporal(TemporalType.DATE)
    private Date dateOpen;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private RequestStatus status;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBranchFlex() {
        return branchFlex;
    }

    public void setBranchFlex(String branchFlex) {
        this.branchFlex = branchFlex;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public String getDealSource() {
        return dealSource;
    }

    public void setDealSource(String dealSource) {
        this.dealSource = dealSource;
    }

    public String getDealId() {
        return dealId;
    }

    public void setDealId(String dealId) {
        this.dealId = dealId;
    }

    public String getSubDealId() {
        return subDealId;
    }

    public void setSubDealId(String subDealId) {
        this.subDealId = subDealId;
    }

    public Date getDateOpen() {
        return dateOpen;
    }

    public void setDateOpen(Date dateOpen) {
        this.dateOpen = dateOpen;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getCbCustomerType() {
        return cbCustomerType;
    }

    public void setCbCustomerType(String cbCustomerType) {
        this.cbCustomerType = cbCustomerType;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }
}
