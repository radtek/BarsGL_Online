package ru.rbt.barsgl.ejb.entity.cob;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;

/**
 * Created by ER18837 on 10.03.17.
 */
@Embeddable
@MappedSuperclass
public class CobStatId implements Serializable {

    @Column(name = "ID_COB")
    private Long idCob;

    @Column(name = "PHASE_NO")
    private Integer phaseNo;

    public CobStatId() {
    }

    public CobStatId(Long idCob, Integer phaseNo) {
        this.idCob = idCob;
        this.phaseNo = phaseNo;
    }

    public Long getIdCob() {
        return idCob;
    }

    public void setIdCob(Long idCob) {
        this.idCob = idCob;
    }

    public Integer getPhaseNo() {
        return phaseNo;
    }

    public void setPhaseNo(Integer phaseNo) {
        this.phaseNo = phaseNo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CobStatId that = (CobStatId) o;

        if (!idCob.equals(that.idCob)) return false;
        if (!phaseNo.equals(that.phaseNo)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = idCob.hashCode();
        result = 31 * result + phaseNo.hashCode();
        return result;
    }

}
