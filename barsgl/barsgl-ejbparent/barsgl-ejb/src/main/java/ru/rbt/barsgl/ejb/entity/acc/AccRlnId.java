package ru.rbt.barsgl.ejb.entity.acc;


import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

import static ru.rbt.ejbcore.util.StringUtils.leftSpace;

/**
 * Created by ER18837 on 28.04.15.
 */
@Embeddable
public class AccRlnId implements Serializable {
    @Column(name = "ACID", length = 20, columnDefinition = "CHAR")
    private String acid;

    @Column(name = "BSAACID", length = 20, columnDefinition = "CHAR")
    private String bsaAcid;

    public AccRlnId() {
    }

    public AccRlnId(String acid, String bsaAcid) {
        this.acid = leftSpace(acid, 20);
        this.bsaAcid = leftSpace(bsaAcid, 20);
    }

    public String getAcid() {
        return acid;
    }

    public String getBsaAcid() {
        return bsaAcid;
    }

    public void setAcid(String acid) {
        this.acid = leftSpace(acid, 20);
    }

    public void setBsaAcid(String bsaAcid) {
        this.bsaAcid = leftSpace(bsaAcid, 20);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AccRlnId that = (AccRlnId) o;

        if (!acid.equals(that.acid)) return false;
        if (!bsaAcid.equals(that.bsaAcid)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = acid.hashCode();
        result = 31 * result + bsaAcid.hashCode();
        return result;
    }

}
