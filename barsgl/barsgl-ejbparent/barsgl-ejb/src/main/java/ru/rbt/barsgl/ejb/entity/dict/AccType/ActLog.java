package ru.rbt.barsgl.ejb.entity.dict.AccType;

import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;
import ru.rbt.barsgl.shared.enums.AccLogTarget;
import ru.rbt.barsgl.shared.enums.LogRowAction;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by akichigi on 21.09.16.
 */

@Entity
@Table(name = "GL_ACTLOG")
@SequenceGenerator(name = "ActLogIdSeq", sequenceName = "GL_ACTLOG_SEQ", allocationSize = 1)
public class ActLog extends BaseEntity<Long> {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "ActLogIdSeq")
    private Long id;

    @Column(name = "ACCTYPE")
    private String accType;

    @Column(name = "ACCNAME")
    private String accName;

    @Column(name = "PL_ACT")
    @Enumerated(EnumType.STRING)
    private YesNo plAct;

    @Column(name = "FL_CTRL")
    @Enumerated(EnumType.STRING)
    private YesNo flCtrl;

    @Column(name = "CUSTYPE")
    private String cusType;

    @Column(name = "TERM")
    private String term;

    @Column(name = "ACC2")
    private String acc2;

    @Column(name = "PLCODE")
    private String plCode;

    @Column(name = "ACOD")
    private String acod;

    @Column(name = "AC_SQ")
    private String acsq;

    @Column(name = "DTB")
    @Temporal(TemporalType.DATE)
    private Date dtb;

    @Column(name = "DTE")
    @Temporal(TemporalType.DATE)
    private Date dte;

    @Column(name = "ACTION")
    @Enumerated(EnumType.STRING)
    private LogRowAction action;

    @Column(name = "TARGET")
    @Enumerated(EnumType.STRING)
    private AccLogTarget target;

    @Column(name = "USER_CHG")
    private String userCng;

    @Column(name = "DATE_CHG")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateCng;

    public ActLog(){}

    public ActLog(String accType, String accName, YesNo plAct, YesNo flCtrl,
                  String userCng, LogRowAction action, AccLogTarget target,
                  Date dateCng){
        this.accType = accType;
        this.accName = accName;
        this.plAct = plAct;
        this.flCtrl = flCtrl;
        this.userCng = userCng;
        this.action = action;
        this.target = target;
        this.dateCng = dateCng;
    }

    public ActLog(String accType, String cusType, String term, String acc2, String plCode,
                  String acod, String acsq, Date dtb, Date dte, String userCng, LogRowAction action,
                  AccLogTarget target, Date dateCng){
        this.accType = accType;
        this.cusType = cusType;
        this.term = term;
        this.acc2 = acc2;
        this.plCode = plCode;
        this.acod = acod;
        this.acsq = acsq;
        this.dtb = dtb;
        this.dte = dte;
        this.userCng = userCng;
        this.action = action;
        this.target = target;
        this.dateCng = dateCng;
    }


    @Override
    public Long getId() {
        return id;
    }

    public String getAccType() {
        return accType;
    }

    public void setAccType(String accType) {
        this.accType = accType;
    }

    public String getAccName() {
        return accName;
    }

    public void setAccName(String accName) {
        this.accName = accName;
    }

    public YesNo getPlAct() {
        return plAct;
    }

    public void setPlAct(YesNo plAct) {
        this.plAct = plAct;
    }

    public YesNo getFlCtrl() {
        return flCtrl;
    }

    public void setFlCtrl(YesNo flCtrl) {
        this.flCtrl = flCtrl;
    }

    public String getCusType() {
        return cusType;
    }

    public void setCusType(String cusType) {
        this.cusType = cusType;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public String getAcc2() {
        return acc2;
    }

    public void setAcc2(String acc2) {
        this.acc2 = acc2;
    }

    public String getPlCode() {
        return plCode;
    }

    public void setPlCode(String plCode) {
        this.plCode = plCode;
    }

    public String getAcod() {
        return acod;
    }

    public void setAcod(String acod) {
        this.acod = acod;
    }

    public String getAcsq() {
        return acsq;
    }

    public void setAcsq(String acsq) {
        this.acsq = acsq;
    }

    public Date getDtb() {
        return dtb;
    }

    public void setDtb(Date dtb) {
        this.dtb = dtb;
    }

    public Date getDte() {
        return dte;
    }

    public void setDte(Date dte) {
        this.dte = dte;
    }

    public LogRowAction getAction() {
        return action;
    }

    public void setAction(LogRowAction action) {
        this.action = action;
    }

    public AccLogTarget getTarget() {
        return target;
    }

    public void setTarget(AccLogTarget target) {
        this.target = target;
    }

    public String getUserCng() {
        return userCng;
    }

    public void setUserCng(String userCng) {
        this.userCng = userCng;
    }

    public Date getDateCng() {
        return dateCng;
    }

    public void setDateCng(Date dateCng) {
        this.dateCng = dateCng;
    }
}
