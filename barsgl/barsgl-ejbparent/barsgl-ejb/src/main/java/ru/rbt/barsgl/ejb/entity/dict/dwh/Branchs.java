package ru.rbt.barsgl.ejb.entity.dict.dwh;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by er22317 on 14.02.2018.
 */
@Entity
@Table(name = "IMBCBBRP")
public class Branchs extends BaseEntity<String> implements Comparable<Branchs>{
    @Id
    @Column(name = "A8BRCD")
    private String id;

    @Column(name = "A8CMCD")
    private String A8CMCD;

    @Column(name = "A8LCCD")
    private String A8LCCD;

    @Column(name = "A8DFAC")
    private String A8DFAC;

    @Column(name = "A8DTAC")
    private String A8DTAC;

    @Column(name = "A8BICN")
    private String A8BICN;

    @Column(name = "A8LCD")
    private Long A8LCD;

    @Column(name = "A8TYLC")
    private String A8TYLC;

    @Column(name = "A8BRNM")
    private String A8BRNM;

    @Column(name = "A8BRSN")
    private String A8BRSN;

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


    public Branchs(){}

    public Branchs(String id, String A8CMCD, String A8LCCD, String A8BICN, String A8BRNM, String BBRRI, String BCORI, String BCBBR, String BR_HEAD, String BR_OPER){
        this.id = id;
        this.A8CMCD = A8CMCD;
        this.A8LCCD = A8LCCD;
        A8DFAC = "1305";
        A8DTAC = "1305";
        this.A8BICN = A8BICN;
        A8LCD = 0l;
        A8TYLC = "I";
        this.A8BRNM = A8BRNM;
        A8BRSN = id;
        this.BBRRI = BBRRI;
        this.BCORI = BCORI;
        this.BCBBR = BCBBR;
        this.BR_HEAD = BR_HEAD;
        this.BR_OPER = BR_OPER;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getA8CMCD() {
        return A8CMCD;
    }

    public void setA8CMCD(String a8CMCD) {
        A8CMCD = a8CMCD;
    }

    public String getA8LCCD() {
        return A8LCCD;
    }

    public void setA8LCCD(String a8LCCD) {
        A8LCCD = a8LCCD;
    }

    public String getA8DFAC() {
        return A8DFAC;
    }

    public void setA8DFAC(String a8DFAC) {
        A8DFAC = a8DFAC;
    }

    public String getA8DTAC() {
        return A8DTAC;
    }

    public void setA8DTAC(String a8DTAC) {
        A8DTAC = a8DTAC;
    }

    public String getA8BICN() {
        return A8BICN;
    }

    public void setA8BICN(String a8BICN) {
        A8BICN = a8BICN;
    }

    public Long getA8LCD() {
        return A8LCD;
    }

    public void setA8LCD(Long a8LCD) {
        A8LCD = a8LCD;
    }

    public String getA8TYLC() {
        return A8TYLC;
    }

    public void setA8TYLC(String a8TYLC) {
        A8TYLC = a8TYLC;
    }

    public String getA8BRNM() {
        return A8BRNM;
    }

    public void setA8BRNM(String a8BRNM) {
        A8BRNM = a8BRNM;
    }

    public String getA8BRSN() {
        return A8BRSN;
    }

    public void setA8BRSN(String a8BRSN) {
        A8BRSN = a8BRSN;
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

    public int compareTo(Branchs b){
        return this.getId().compareTo(b.getId());
    }
}
