package ru.rbt.barsgl.ejb.entity.gl;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by Ivan Sevastyanov on 11.02.2016.
 */
@Embeddable
public class GLBalanceExcludeId implements Serializable {

    @Column(name = "BSAACID")
    private String bsaacid;

    @Temporal(TemporalType.DATE)
    @Column(name = "DAT")
    private Date dtFrom;

    public String getBsaacid() {
        return bsaacid;
    }

    public void setBsaacid(String bsaacid) {
        this.bsaacid = bsaacid;
    }

    public Date getDtFrom() {
        return dtFrom;
    }

    public void setDtFrom(Date dtFrom) {
        this.dtFrom = dtFrom;
    }
}
