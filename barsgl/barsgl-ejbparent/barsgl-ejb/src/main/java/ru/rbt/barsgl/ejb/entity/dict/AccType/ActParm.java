package ru.rbt.barsgl.ejb.entity.dict.AccType;

import ru.rbt.barsgl.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by akichigi on 24.08.16.
 */

@Entity
@Table(name = "GL_ACTPARM")
public class ActParm extends BaseEntity<ActParmId> {
    @EmbeddedId
    private ActParmId id;

    @Column(name = "PLCODE")
    private String plcode;

    @Column(name = "ACOD")
    private String acod;

    @Column(name = "AC_SQ")
    private String ac_sq;

    @Column(name = "DTE")
    @Temporal(TemporalType.DATE)
    private Date dte;

    public ActParm(){}

    public ActParm(ActParmId id, String plcode, String acod, String ac_sq, Date dte) {
        this.id = id;
        this.plcode = plcode;
        this.acod = acod;
        this.ac_sq = ac_sq;
        this.dte = dte;
    }

    @Override
    public ActParmId getId() {
        return id;
    }

    public String getPlcode() {
        return plcode;
    }

    public void setPlcode(String plcode) {
        this.plcode = plcode;
    }

    public String getAcod() {
        return acod;
    }

    public void setAcod(String acod) {
        this.acod = acod;
    }

    public String getAc_sq() {
        return ac_sq;
    }

    public void setAc_sq(String ac_sq) {
        this.ac_sq = ac_sq;
    }

    public Date getDte() {
        return dte;
    }

    public void setDte(Date dte) {
        this.dte = dte;
    }
}
