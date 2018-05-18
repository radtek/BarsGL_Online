package ru.rbt.barsgl.ejb.entity.dict.dwh;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by er22317 on 14.02.2018.
 */
@Entity
@Table(name = "DWH_IMBCBBRP_INF")
public class BranchsInf extends BaseEntity<String> implements Comparable<BranchsInf>{
    @Id
    @Column(name = "A8BRCD")
    private String id;

    @Column(name = "A8LCCD")
    private String A8LCCD;

    @Column(name = "A8BICN")
    private String A8BICN;

    @Column(name = "A8BRNM")
    private String A8BRNM;

    @Column(name = "BBRRI")
    private String BBRRI;

    @Column(name = "BCORI")
    private String BCORI;

    @Column(name = "BCBBR")
    private String BCBBR;

    @Column(name = "BR_HEAD")
    private String BR_HEAD;

    @Column(name = "BR_OPER")
    private String BR_OPER;

    @Column(name = "FCC_CODE")
    private String FCC_CODE;

    @Temporal(TemporalType.DATE)
    @Column(name = "VALID_FROM")
    private Date validFrom;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getA8LCCD() {
        return A8LCCD;
    }

    public void setA8LCCD(String a8LCCD) {
        A8LCCD = a8LCCD;
    }

    public String getA8BICN() {
        return A8BICN;
    }

    public void setA8BICN(String a8BICN) {
        A8BICN = a8BICN;
    }

    public String getA8BRNM() {
        return A8BRNM;
    }

    public void setA8BRNM(String a8BRNM) {
        A8BRNM = a8BRNM;
    }

    public String getBBRRI() {
        return BBRRI;
    }

    public void setBBRRI(String BBRRI) {
        this.BBRRI = BBRRI;
    }

    public String getBCORI() {
        return BCORI;
    }

    public void setBCORI(String BCORI) {
        this.BCORI = BCORI;
    }

    public String getBCBBR() {
        return BCBBR;
    }

    public void setBCBBR(String BCBBR) {
        this.BCBBR = BCBBR;
    }

    public String getBR_HEAD() {
        return BR_HEAD;
    }

    public void setBR_HEAD(String BR_HEAD) {
        this.BR_HEAD = BR_HEAD;
    }

    public String getBR_OPER() {
        return BR_OPER;
    }

    public void setBR_OPER(String BR_OPER) {
        this.BR_OPER = BR_OPER;
    }

    public String getFCC_CODE() {
        return FCC_CODE;
    }

    public void setFCC_CODE(String FCC_CODE) {
        this.FCC_CODE = FCC_CODE;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }


    public int compareTo(BranchsInf branchs){
        return this.getId().compareTo(branchs.getId());
    }
}
