package ru.rbt.barsgl.ejb.entity.card;

import ru.rbt.barsgl.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by ER22317 on 23.09.2016.
 */
@Entity
@Table(name = "GL_EXLCARD")
public class CardXls  extends BaseEntity<Long> {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ID_PKG")
    private Long packageId;

    @Column(name = "NROW")
    private Integer rowNumber;

    @Column(name = "DC")
    private String dc;

    @Temporal(TemporalType.DATE)
    @Column(name = "VDATE")
    private Date valueDate;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "CNUM")
    private String cnum;

    @Column(name = "MD_ACC")
    private String mdacc;

    @Column(name = "BSAACID")
    private String bsaacid;

    @Column(name = "CARD")
    private String card;

    @Column(name = "ECODE")
    private String ecode;

    @Column(name = "EMSG")
    private String emsg;

    @Column(name = "PNAR")
    private String pnar;

    @Column(name = "RNRTL")
    private String rnrtl;

    @Override
    public Long getId() {
        return id;
    }

    public Long getPackageId() {
        return packageId;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public String getDc() {
        return dc;
    }

    public Date getValueDate() {
        return valueDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCnum() {
        return cnum;
    }

    public String getMdacc() {
        return mdacc;
    }

    public String getBsaacid() {
        return bsaacid;
    }

    public String getCard() {
        return card;
    }

    public String getEcode() {
        return ecode;
    }

    public String getEmsg() {
        return emsg;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }

    public void setDc(String dc) {
        this.dc = dc;
    }

    public void setValueDate(Date valueDate) {
        this.valueDate = valueDate;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setCnum(String cnum) {
        this.cnum = cnum;
    }

    public void setMdacc(String mdacc) {
        this.mdacc = mdacc;
    }

    public void setBsaacid(String bsaacid) {
        this.bsaacid = bsaacid;
    }

    public void setCard(String card) {
        this.card = card;
    }

    public void setEcode(String ecode) {
        this.ecode = ecode;
    }

    public void setEmsg(String emsg) {
        this.emsg = emsg;
    }

    public String getRnrtl() {
        return rnrtl;
    }

    public void setRnrtl(String rnrtl) {
        this.rnrtl = rnrtl;
    }
    public String getPnar() {
        return pnar;
    }

    public void setPnar(String pnar) {
        this.pnar = pnar;
    }


}
