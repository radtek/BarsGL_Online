package ru.rbt.barsgl.ejb.entity.dict;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Created by Ivan Sevastyanov on 31.03.2016.
 */
@Embeddable
public class GLRelationAccountingTypeId implements Serializable {

    @Column(name = "BSAACID")
    private String bsaacid;

    @Column(name = "ACID")
    private String acid;

    public GLRelationAccountingTypeId() {
    }

    public GLRelationAccountingTypeId(String bsaacid, String acid) {
        this.bsaacid = bsaacid;
        this.acid = acid;
    }

    public String getBsaacid() {
        return bsaacid;
    }

    public void setBsaacid(String bsaacid) {
        this.bsaacid = bsaacid;
    }

    public String getAcid() {
        return acid;
    }

    public void setAcid(String acid) {
        this.acid = acid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GLRelationAccountingTypeId that = (GLRelationAccountingTypeId) o;
        return Objects.equals(bsaacid, that.bsaacid) &&
                Objects.equals(acid, that.acid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bsaacid, acid);
    }
}
