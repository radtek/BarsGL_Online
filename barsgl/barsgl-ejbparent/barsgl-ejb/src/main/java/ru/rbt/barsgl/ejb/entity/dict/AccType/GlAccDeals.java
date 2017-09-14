package ru.rbt.barsgl.ejb.entity.dict.AccType;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by er22317 on 17.07.2017.
 */
@Entity
@Table(name = "GL_ACCDEALS")
public class GlAccDeals extends BaseEntity<String> {

    @Id
    @Column(name = "ACC2")
    private String acc2;

    @Column(name = "FLAG_OFF")
    private String flag_off;

    @Column(name = "DTR")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dtr;

    @Column(name = "USRR")
    private String usrr;

    @Column(name = "DTM")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dtm;

    @Column(name = "USRM")
    private String usrm;

    public GlAccDeals(){}

    //только для create
    public GlAccDeals(String acc2, String flag_off) {
        this.acc2 = acc2;
        this.flag_off = flag_off;
        this.dtr = new Date();
        this.usrr = "sys";
    }

    @Override
    public String getId() {
        return acc2;
    }

    public String getAcc2() {
        return acc2;
    }

    public void setAcc2(String acc2) {
        this.acc2 = acc2;
    }

    public String getFlag_off() {
        return flag_off;
    }

    public void setFlag_off(String flag_off) {
        this.flag_off = flag_off;
    }

    public Date getDtr() {
        return dtr;
    }

    public void setDtr(Date dtr) {
        this.dtr = dtr;
    }

    public String getUsrr() {
        return usrr;
    }

    public void setUsrr(String usrr) {
        this.usrr = usrr;
    }

    public Date getDtm() {
        return dtm;
    }

    public void setDtm(Date dtm) {
        this.dtm = dtm;
    }

    public String getUsrm() {
        return usrm;
    }

    public void setUsrm(String usrm) {
        this.usrm = usrm;
    }

}
