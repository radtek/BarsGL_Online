package ru.rbt.barsgl.ejb.entity.cust;

import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.ejbcore.mapping.YesNo;

import javax.persistence.*;

/**
 * Created by er18837 on 18.12.2017.
 */
@Entity
@Table(name = "GL_CUDENO3")
public class CustDNMapped extends BaseEntity<Long> {
    public enum CustResult {INSERT, UPDATE, NOCHANGE};

    @Id
    @Column(name = "MESSAGE_ID")
    private Long id;

    @Column(name = "RESULT", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private CustResult result;

    @Column(name = "BBCUST", nullable = false, length = 8)
    private String custNo;

    @Column(name = "BBBRCD", nullable = false, length = 3)
    private String branch;

    @Column(name = "PRCD", nullable = false, length = 1)        // P/C/B
    @Enumerated(EnumType.STRING)
    private Customer.ClientType clientType;

    @Column(name = "BXCTYP", nullable = false, length = 3)
    private String cbType;

    @Column(name = "RECD", nullable = false, length = 1)        // Y/N
    @Enumerated(EnumType.STRING)
    private Customer.Resident resident;

    @Column(name = "BBCNA1", nullable = false, length = 35)
    private String nameEng;

    @Column(name = "BBCRNM", nullable = false, length = 20)
    private String shortNameEng;

    @Column(name = "BXRUNM", nullable = true, length = 80)
    private String nameRus;

    @Column(name = "BBBRCD_OLD", nullable = true, length = 3)
    private String branchOld;

    @Column(name = "BXCTYP_OLD", nullable = true, length = 3)
    private String cbTypeOld;

    @Column(name = "RECD_OLD", nullable = true, length = 1)        // Y/N
    @Enumerated(EnumType.STRING)
    private Customer.Resident residentOld;

    public CustDNMapped() {}

    public CustDNMapped(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() {
        return id;
    }

    public CustResult getResult() {
        return result;
    }

    public void setResult(CustResult result) {
        this.result = result;
    }

    public String getCustNo() {
        return custNo;
    }

    public void setCustNo(String custNo) {
        this.custNo = custNo;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public Customer.ClientType getClientType() {
        return clientType;
    }

    public void setClientType(Customer.ClientType clientType) {
        this.clientType = clientType;
    }

    public String getCbType() {
        return cbType;
    }

    public void setCbType(String cbType) {
        this.cbType = cbType;
    }

    public String getNameEng() {
        return nameEng;
    }

    public void setNameEng(String nameEng) {
        this.nameEng = nameEng;
    }

    public String getShortNameEng() {
        return shortNameEng;
    }

    public void setNameEngShort(String shortNameEng) {
        this.shortNameEng = shortNameEng;
    }

    public String getNameRus() {
        return nameRus;
    }

    public void setNameRus(String nameRus) {
        this.nameRus = nameRus;
    }

    public String getBranchOld() {
        return branchOld;
    }

    public void setBranchOld(String branchOld) {
        this.branchOld = branchOld;
    }

    public String getCbTypeOld() {
        return cbTypeOld;
    }

    public void setCbTypeOld(String cbTypeOld) {
        this.cbTypeOld = cbTypeOld;
    }

    public Customer.Resident getResident() {
        return resident;
    }

    public void setResident(Customer.Resident resident) {
        this.resident = resident;
    }

    public Customer.Resident getResidentOld() {
        return residentOld;
    }

    public void setResidentOld(Customer.Resident residentOld) {
        this.residentOld = residentOld;
    }

    public void setShortNameEng(String shortNameEng) {
        this.shortNameEng = shortNameEng;
    }
}
