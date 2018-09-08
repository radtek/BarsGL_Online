package ru.rbt.barsgl.ejb.entity.gl;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by er18837 on 23.08.2018.
 */
@Entity
@Table(name = "GL_REG47422")
@SequenceGenerator(name = "Register47422IdSeq", sequenceName = "GL_REG47422_SEQ", allocationSize = 1)
public class Reg47422Journal extends BaseEntity<Long> {

    enum Reg47422State {LOAD, CHANGE, PROC_GL, PROC_ACC, ERRSRC, ERRPROC, WT47416};
    enum Reg47422Valid {Y, N, U}

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "Register47422IdSeq")
    private Long id;

    @Column(name = "PD_ID")
    private Long pdId;

    @Column(name = "PCID")
    private Long pcId;

    @Column(name = "INVISIBLE")
    private String invisible;

    @Column(name = "POD")
    @Temporal(TemporalType.DATE)
    private Date pod;

    @Column(name = "VALD")
    @Temporal(TemporalType.DATE)
    private Date vald;

    @Column(name = "PROCDATE")
    @Temporal(TemporalType.DATE)
    private Date procDate;

    @Column(name = "PBR")
    private String pbr;

    @Column(name = "ACID")
    private String acid;

    @Column(name = "BSAACID")
    private String bsaAcid;

    @Column(name = "CCY")
    private String ccy;

    @Column(name = "AMNT")
    private Long amount;

    @Column(name = "AMNTBC")
    private Long amountBC;

    @Column(name = "DC")
    private String debitCredit;

    @Column(name = "CBCC")
    private String filial;

    @Column(name = "RNARLNG")
    private String rusNarrLong;

    @Column(name = "NDOG")
    private String contract;

    @Column(name = "PMT_REF")
    private String paymentRef;

    @Column(name = "GLO_REF")
    private Long glOperationId;

    @Column(name = "OPERDAY")
    @Temporal(TemporalType.DATE)
    private Date operday;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATE")
    private Reg47422State state;

    @Enumerated(EnumType.STRING)
    @Column(name = "VALID")
    private Reg47422Valid valid;

    @Column(name = "ID_REF")
    private Long parentId;

    @Column(name = "TS", insertable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date loadDate;

    public Long getId() {
        return id;
    }

    public Long getPdId() {
        return pdId;
    }

    public void setPdId(Long pdId) {
        this.pdId = pdId;
    }

    public Long getPcId() {
        return pcId;
    }

    public void setPcId(Long pcId) {
        this.pcId = pcId;
    }

    public String getInvisible() {
        return invisible;
    }

    public void setInvisible(String invisible) {
        this.invisible = invisible;
    }

    public Date getPod() {
        return pod;
    }

    public void setPod(Date pod) {
        this.pod = pod;
    }

    public Date getVald() {
        return vald;
    }

    public void setVald(Date vald) {
        this.vald = vald;
    }

    public Date getProcDate() {
        return procDate;
    }

    public void setProcDate(Date procDate) {
        this.procDate = procDate;
    }

    public String getPbr() {
        return pbr;
    }

    public void setPbr(String pbr) {
        this.pbr = pbr;
    }

    public String getAcid() {
        return acid;
    }

    public void setAcid(String acid) {
        this.acid = acid;
    }

    public String getBsaAcid() {
        return bsaAcid;
    }

    public void setBsaAcid(String bsaAcid) {
        this.bsaAcid = bsaAcid;
    }

    public String getCcy() {
        return ccy;
    }

    public void setCcy(String ccy) {
        this.ccy = ccy;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Long getAmountBC() {
        return amountBC;
    }

    public void setAmountBC(Long amountBC) {
        this.amountBC = amountBC;
    }

    public String getDebitCredit() {
        return debitCredit;
    }

    public void setDebitCredit(String debitCredit) {
        this.debitCredit = debitCredit;
    }

    public String getFilial() {
        return filial;
    }

    public void setFilial(String filial) {
        this.filial = filial;
    }

    public String getRusNarrLong() {
        return rusNarrLong;
    }

    public void setRusNarrLong(String rusNarrLong) {
        this.rusNarrLong = rusNarrLong;
    }

    public String getContract() {
        return contract;
    }

    public void setContract(String contract) {
        this.contract = contract;
    }

    public Long getGlOperationId() {
        return glOperationId;
    }

    public void setGlOperationId(Long glOperationId) {
        this.glOperationId = glOperationId;
    }

    public Date getOperday() {
        return operday;
    }

    public void setOperday(Date operday) {
        this.operday = operday;
    }

    public Reg47422State getState() {
        return state;
    }

    public void setState(Reg47422State state) {
        this.state = state;
    }

    public Reg47422Valid getValid() {
        return valid;
    }

    public void setValid(Reg47422Valid valid) {
        this.valid = valid;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Date getLoadDate() {
        return loadDate;
    }

    public void setLoadDate(Date loadDate) {
        this.loadDate = loadDate;
    }
}
