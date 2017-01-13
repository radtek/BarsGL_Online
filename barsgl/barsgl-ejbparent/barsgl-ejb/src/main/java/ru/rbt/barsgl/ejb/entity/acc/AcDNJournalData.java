package ru.rbt.barsgl.ejb.entity.acc;

import ru.rbt.barsgl.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by ER22228 on 30.03.2016
 */
@Entity
@Table(name = "GL_ACDENOLI")
public class AcDNJournalData extends BaseEntity<Long> {

    public AcDNJournalData(){}

    public AcDNJournalData(Long id, String accountNo, String branch, String cbaccountNo, String ccy, String ccyDigital, String description, Status status, Long customerNo, String special, Date openDate, String altAccountNo) {
        this.id = id;
        this.accountNo = accountNo;
        this.branch = branch;
        this.cbaccountNo = cbaccountNo;
        this.ccy = ccy;
        this.ccyDigital = ccyDigital;
        this.description = description;
        this.status = status;
        this.customerNo = customerNo;
        this.special = special;
        this.openDate = openDate;
        this.altAccountNo = altAccountNo;
    }

    public enum OpStatus {OPEN,EXISTS,UPDATED}
    public enum Status {O,C}

    @Id
    @Column(name = "MESSAGE_ID")
    private Long id;

    @Column(name = "ACCOUNT_NO")
    private String accountNo;

    @Column(name = "BRANCH")
    private String branch;

    @Column(name = "CBACCOUNT_NO")
    private String cbaccountNo;

    @Column(name = "CCY")
    private String ccy;

    @Column(name = "CCYDIGITAL")
    private String ccyDigital;

    @Column(name = "DESCRIPTION")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private Status status;

    @Column(name = "CUSTOMER_NO")
    private Long customerNo;

    @Column(name = "SPECIAL")
    private String special;

    @Column(name = "OPENDATE")
    @Temporal(TemporalType.DATE)
    private Date openDate;

    @Column(name = "MIDAS_BRANCH")
    private String midasBranch;

    @Column(name = "PSEUDO_ACID")
    private String pseudoAcid;

    @Enumerated(EnumType.STRING)
    @Column(name = "OP_STATUS")
    private OpStatus opStatus;

    @Column(name = "ALTACCOUNT_NO")
    private String altAccountNo;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getCbaccountNo() {
        return cbaccountNo;
    }

    public void setCbaccountNo(String cbaccountNo) {
        this.cbaccountNo = cbaccountNo;
    }

    public String getCcy() {
        return ccy;
    }

    public void setCcy(String ccy) {
        this.ccy = ccy;
    }

    public String getCcyDigital() {
        return ccyDigital;
    }

    public void setCcyDigital(String ccyDigital) {
        this.ccyDigital = ccyDigital;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Long getCustomerNo() {
        return customerNo;
    }

    public void setCustomerNo(Long customerNo) {
        this.customerNo = customerNo;
    }

    public String getSpecial() {
        return special;
    }

    public void setSpecial(String special) {
        this.special = special;
    }

    public Date getOpenDate() {
        return openDate;
    }

    public void setOpenDate(Date openDate) {
        this.openDate = openDate;
    }

    public String getMidasBranch() {
        return midasBranch;
    }

    public void setMidasBranch(String midasBranch) {
        this.midasBranch = midasBranch;
    }

    public String getPseudoAcid() {
        return pseudoAcid;
    }

    public void setPseudoAcid(String pseudoAcid) {
        this.pseudoAcid = pseudoAcid;
    }

    public OpStatus getOpStatus() {
        return opStatus;
    }

    public void setOpStatus(OpStatus opStatus) {
        this.opStatus = opStatus;
    }

    public String getAltAccountNo() {
        return altAccountNo;
    }

    public void setAltAccountNo(String altAccountNo) {
        this.altAccountNo = altAccountNo;
    }

    @Override
    public String toString() {
        return "AcDNJournalData{" +
                   "id=" + id +
                   ", accountNo='" + accountNo + '\'' +
                   ", branch='" + branch + '\'' +
                   ", cbaccountNo='" + cbaccountNo + '\'' +
                   ", ccy='" + ccy + '\'' +
                   ", ccyDigital='" + ccyDigital + '\'' +
                   ", description='" + description + '\'' +
                   ", status=" + status +
                   ", customerNo=" + customerNo +
                   ", special='" + special + '\'' +
                   ", openDate=" + openDate +
                   ", midasBranch='" + midasBranch + '\'' +
                   ", pseudoAcid='" + pseudoAcid + '\'' +
                   ", opStatus=" + opStatus +
                   '}';
    }
}
