package ru.rbt.barsgl.ejb.entity.acc;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by er18837 on 21.08.2017.
 */
@Embeddable
@MappedSuperclass
public class AccCardId implements Serializable {

    @Column(name = "DAT")
    @Temporal(TemporalType.DATE)
    private Date startDate;

    @Column(name = "BSAACID")
    private String bsaAcid;

    public AccCardId() {
    }

    public AccCardId(String bsaAcid, Date startDate) {
        this.startDate = startDate;
        this.bsaAcid = bsaAcid;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public String getBsaAcid() {
        return bsaAcid;
    }

    public void setBsaAcid(String bsaAcid) {
        this.bsaAcid = bsaAcid;
    }
}
