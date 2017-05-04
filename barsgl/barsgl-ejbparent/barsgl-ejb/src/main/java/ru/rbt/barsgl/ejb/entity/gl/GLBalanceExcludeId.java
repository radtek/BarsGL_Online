package ru.rbt.barsgl.ejb.entity.gl;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GLBalanceExcludeId that = (GLBalanceExcludeId) o;
        return Objects.equals(bsaacid, that.bsaacid) &&
                Objects.equals(dtFrom, that.dtFrom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bsaacid, dtFrom);
    }
}
