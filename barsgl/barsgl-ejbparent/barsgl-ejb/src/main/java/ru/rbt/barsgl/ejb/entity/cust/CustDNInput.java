package ru.rbt.barsgl.ejb.entity.cust;

import ru.rbt.barsgl.shared.enums.YesNoType;
import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.ejbcore.mapping.YesNo;

import javax.persistence.*;

/**
 * Created by er18837 on 15.12.2017.
 */
@Entity
@Table(name = "GL_CUDENO2")
public class CustDNInput extends BaseEntity<Long>{

    @Id
    @Column(name = "MESSAGE_ID")
    private Long id;

    @Column(name = "CUST_NUM", nullable = false, length = 8)
    private String custNo;

    @Column(name = "BRANCHCODE", nullable = false, length = 3)
    private String branch;

    @Column(name = "FCTYPE", nullable = false, length = 1)
    private String fcCustType;

    @Column(name = "CBTYPE", nullable = false, length = 3)
    private String fcCbType;

    @Column(name = "RESIDENT", nullable = false, length = 1)
    private String resident;

    @Column(name = "NAME_ENG", nullable = false, length = 50)
    private String nameEng;

    @Column(name = "NAME_RUS", nullable = false, length = 50)
    private String nameRus;

    @Column(name = "LEGAL_FORM", length = 20)
    private String legalForm;


    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
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

    public String getFcCustType() {
        return fcCustType;
    }

    public void setFcCustType(String fcCustType) {
        this.fcCustType = fcCustType;
    }

    public String getFcCbType() {
        return fcCbType;
    }

    public void setFcCbType(String fcCbType) {
        this.fcCbType = fcCbType;
    }

    public String getResident() {
        return resident;
    }

    public void setResident(String resident) {
        this.resident = resident;
    }

    public String getNameEng() {
        return nameEng;
    }

    public void setNameEng(String nameEng) {
        this.nameEng = nameEng;
    }

    public String getNameRus() {
        return nameRus;
    }

    public void setNameRus(String nameRus) {
        this.nameRus = nameRus;
    }

    public String getLegalForm() {
        return legalForm;
    }

    public void setLegalForm(String legalForm) {
        this.legalForm = legalForm;
    }
}
