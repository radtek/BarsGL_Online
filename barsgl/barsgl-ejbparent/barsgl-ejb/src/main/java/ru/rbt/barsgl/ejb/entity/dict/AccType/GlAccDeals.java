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
    @Temporal(TemporalType.DATE)
    private Date dtr;

    @Column(name = "USRR")
    private String usrr;

    @Column(name = "DTM")
    @Temporal(TemporalType.DATE)
    private Date dtm;

    @Column(name = "USRM")
    private String usrm;

    public GlAccDeals(String acc2, String flag_off, String usrr) {
        this.acc2 = acc2;
        this.flag_off = flag_off;
//        this.dtr = dtr;
        this.usrr = usrr;
//        this.dtm = dtm;
//        this.usrm = usrm;
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
//    public void setFlag_off(boolean flag_off) {
//        this.flag_off = flag_off? "Y": "N";
//    }

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

//    BSS.ACC1NAM ||' '|| BSS.ACC2NAM name,
}
