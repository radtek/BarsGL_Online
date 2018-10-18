package ru.rbt.barsgl.ejb.entity.etl;

import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.shared.enums.AccountBatchState;
import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.ejbcore.mapping.YesNo;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by Ivan Sevastyanov on 17.10.2018.
 */
@Entity
@Table(name = "GL_ACBATREQ")
@SequenceGenerator(name = "AccountBatchRequestIdSeq", sequenceName = "SEQ_GL_ACBATREQ", allocationSize = 1)
public class AccountBatchRequest extends BaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "AccountBatchRequestIdSeq")
    @Column(name = "ID_REQ")
    private Long id;

    @JoinColumn(name = "ID_PKG", nullable = false, updatable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private AccountBatchPackage batchPackage;

    @Column(name = "RECNO")
    private Long lineNumber;

    @Column(name = "STATE")
    @Enumerated(EnumType.STRING)
    private AccountBatchState state;

    @Column(name = "BRANCH_IN")
    String inBranch;

    @Column(name = "CCY_IN")
    String inCcy;

    @Column(name = "CUSTNO_IN")
    String inCustno;

    @Column(name = "ACCTYPE_IN")
    String inAcctype;

    @Column(name = "CTYPE_IN")
    String inCtype;

    @Column(name = "TERM_IN")
    String inTerm;

    @Column(name = "ACC2_IN")
    String inAcc2;

    @Column(name = "DEALSRC_IN")
    String inDealsrc;

    @Column(name = "DEALID_IN")
    String inDealid;

    @Column(name = "SUBDEALID_IN")
    String inSubdealid;

    @Column(name = "OPENDATE_IN")
    @Temporal(TemporalType.DATE)
    Date inOpendate;

    @Column(name = "CTYPE_CUS")
    String calcCtype;

    @Column(name = "CTYPE_PARM")
    String calcCtypeParm;

    @Column(name = "TERM_PARM")
    String calcTermParm;

    @Column(name = "ACC2_PARM")
    String calcAcc2Parm;

    @Column(name = "ACOD_PARM")
    String calcAcodParm;

    @Column(name = "ACSQ_PARM")
    String calcAcsqParm;

    @Column(name = "PLCODE_PARM")
    String calcPlcodeParm;

    @Column(name = "CBCC_BR")
    String calcCbcc;

    @JoinColumn(name = "GLACID")
    @ManyToOne(fetch = FetchType.LAZY)
    private GLAccount account;

    @Column(name = "BSAACID")
    private String bsaAcid;

    @Column(name = "OPENDATE")
    @Temporal(TemporalType.DATE)
    private Date openDate;

    @Column(name = "NEWACC")
    @Enumerated(EnumType.STRING)
    private YesNo newAccount;

    @Column(name = "ERROR_MSG")
    private String errorMessage;

    @Column(name = "TS_VALID")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dtValidate;

    @Column(name = "TS_OPEN")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dtOpen;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public AccountBatchPackage getBatchPackage() {
        return batchPackage;
    }

    public void setBatchPackage(AccountBatchPackage batchPackage) {
        this.batchPackage = batchPackage;
    }

    public Long getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Long lineNumber) {
        this.lineNumber = lineNumber;
    }

    public AccountBatchState getState() {
        return state;
    }

    public void setState(AccountBatchState state) {
        this.state = state;
    }

    public String getInBranch() {
        return inBranch;
    }

    public void setInBranch(String inBranch) {
        this.inBranch = inBranch;
    }

    public String getInCcy() {
        return inCcy;
    }

    public void setInCcy(String inCcy) {
        this.inCcy = inCcy;
    }

    public String getInCustno() {
        return inCustno;
    }

    public void setInCustno(String inCustno) {
        this.inCustno = inCustno;
    }

    public String getInAcctype() {
        return inAcctype;
    }

    public void setInAcctype(String inAcctype) {
        this.inAcctype = inAcctype;
    }

    public String getInCtype() {
        return inCtype;
    }

    public void setInCtype(String inCtype) {
        this.inCtype = inCtype;
    }

    public String getInTerm() {
        return inTerm;
    }

    public void setInTerm(String inTerm) {
        this.inTerm = inTerm;
    }

    public String getInAcc2() {
        return inAcc2;
    }

    public void setInAcc2(String inAcc2) {
        this.inAcc2 = inAcc2;
    }

    public String getInDealsrc() {
        return inDealsrc;
    }

    public void setInDealsrc(String inDealsrc) {
        this.inDealsrc = inDealsrc;
    }

    public String getInDealid() {
        return inDealid;
    }

    public void setInDealid(String inDealid) {
        this.inDealid = inDealid;
    }

    public String getInSubdealid() {
        return inSubdealid;
    }

    public void setInSubdealid(String inSubdealid) {
        this.inSubdealid = inSubdealid;
    }

    public Date getInOpendate() {
        return inOpendate;
    }

    public void setInOpendate(Date inOpendate) {
        this.inOpendate = inOpendate;
    }

    public String getCalcCtype() {
        return calcCtype;
    }

    public void setCalcCtype(String calcCtype) {
        this.calcCtype = calcCtype;
    }

    public String getCalcCtypeParm() {
        return calcCtypeParm;
    }

    public void setCalcCtypeParm(String calcCtypeParm) {
        this.calcCtypeParm = calcCtypeParm;
    }

    public String getCalcTermParm() {
        return calcTermParm;
    }

    public void setCalcTermParm(String calcTermParm) {
        this.calcTermParm = calcTermParm;
    }

    public String getCalcAcc2Parm() {
        return calcAcc2Parm;
    }

    public void setCalcAcc2Parm(String calcAcc2Parm) {
        this.calcAcc2Parm = calcAcc2Parm;
    }

    public String getCalcAcodParm() {
        return calcAcodParm;
    }

    public void setCalcAcodParm(String calcAcodParm) {
        this.calcAcodParm = calcAcodParm;
    }

    public String getCalcAcsqParm() {
        return calcAcsqParm;
    }

    public void setCalcAcsqParm(String calcAcsqParm) {
        this.calcAcsqParm = calcAcsqParm;
    }

    public String getCalcPlcodeParm() {
        return calcPlcodeParm;
    }

    public void setCalcPlcodeParm(String calcPlcodeParm) {
        this.calcPlcodeParm = calcPlcodeParm;
    }

    public String getCalcCbcc() {
        return calcCbcc;
    }

    public void setCalcCbcc(String calcCbcc) {
        this.calcCbcc = calcCbcc;
    }

    public GLAccount getAccount() {
        return account;
    }

    public void setAccount(GLAccount account) {
        this.account = account;
    }

    public String getBsaAcid() {
        return bsaAcid;
    }

    public void setBsaAcid(String bsaAcid) {
        this.bsaAcid = bsaAcid;
    }

    public Date getOpenDate() {
        return openDate;
    }

    public void setOpenDate(Date openDate) {
        this.openDate = openDate;
    }

    public YesNo getNewAccount() {
        return newAccount;
    }

    public void setNewAccount(YesNo newAccount) {
        this.newAccount = newAccount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Date getDtValidate() {
        return dtValidate;
    }

    public void setDtValidate(Date dtValidate) {
        this.dtValidate = dtValidate;
    }

    public Date getDtOpen() {
        return dtOpen;
    }

    public void setDtOpen(Date dtOpen) {
        this.dtOpen = dtOpen;
    }
}
