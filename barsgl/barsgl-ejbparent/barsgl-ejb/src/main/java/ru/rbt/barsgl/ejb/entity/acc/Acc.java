package ru.rbt.barsgl.ejb.entity.acc;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by ER18837 on 28.04.15.
 */
@Entity
@Table(name = "ACC")
public class Acc extends BaseEntity<String> {
    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "BRCA")
    private String branch;

    @Column(name = "CNUM")
    private Integer customerNumberD;

    @Column(name = "CCY")
    private String currency;

    @Column(name = "ACOD")
    private Short accountCode;

    @Column(name = "ACSQ")
    private Short accountSequence;

    @Column(name = "DACO")
    @Temporal(TemporalType.DATE)
    private Date dateOpen;

    @Column(name = "DACC")
    @Temporal(TemporalType.DATE)
    private Date dateClose;

    @Column(name = "ANAM")
    private String accountName;

    //    Далее по умолчанию
    @Column(name = "ATYP")
    private String type;

    @Column(name = "STYP")
    private String sybtype;

    @Column(name = "ACST")
    private String status;

    @Column(name = "LCD")
    @Temporal(TemporalType.DATE)
    private Date dateChange;

    @Column(name = "CHTP")
    private String typeChange;

    @Column(name = "ODED")
    @Temporal(TemporalType.DATE)
    private Date dateOverdraft;

    @Column(name = "ODLN")
    private Long limitOverdraft;

    @Column(name = "STFQ")
    private String frequency;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public void setCustomerNumberD(Integer customerNumberD) {
        this.customerNumberD = customerNumberD;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setAccountCode(Short accountCode) {
        this.accountCode = accountCode;
    }

    public void setAccountSequence(Short accountSequence) {
        this.accountSequence = accountSequence;
    }

    public void setDateOpen(Date dateOpen) {
        this.dateOpen = dateOpen;
    }

    public void setDateClose(Date dateClose) {
        this.dateClose = dateClose;
    }


    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setSybtype(String sybtype) {
        this.sybtype = sybtype;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setDateChange(Date dateChange) {
        this.dateChange = dateChange;
    }

    public void setTypeChange(String typeChange) {
        this.typeChange = typeChange;
    }

    public void setDateOverdraft(Date dateOverdraft) {
        this.dateOverdraft = dateOverdraft;
    }

    public void setLimitOverdraft(Long limitOverdraft) {
        this.limitOverdraft = limitOverdraft;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }
}
