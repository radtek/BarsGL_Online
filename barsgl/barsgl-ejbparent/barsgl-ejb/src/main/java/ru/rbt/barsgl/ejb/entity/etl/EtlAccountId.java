package ru.rbt.barsgl.ejb.entity.etl;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;

/**
 * Created by ER18837 on 29.04.15.
 * @deprecated не используется
 */
@Embeddable
@MappedSuperclass
public class EtlAccountId implements Serializable {

    @Column(name = "ID_PKG")
    private Long idPackage;

    @Column(name = "BSAACID")
    private String bsaAcid;

    public EtlAccountId() {
    }

    public EtlAccountId(Long idPackage, String bsaAcid) {
        this.idPackage = idPackage;
        this.bsaAcid = bsaAcid;
    }

    public Long getIdPackage() {
        return idPackage;
    }

    public String getBsaAcid() {
        return bsaAcid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EtlAccountId that = (EtlAccountId) o;

        if (!idPackage.equals(that.idPackage)) return false;
        if (!bsaAcid.equals(that.bsaAcid)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = idPackage.hashCode();
        result = 31 * result + bsaAcid.hashCode();
        return result;
    }

}