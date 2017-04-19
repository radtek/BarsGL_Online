package ru.rbt.barsgl.ejb.entity.gl;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by ER18837 on 02.03.15.
 */
@Entity
@Table(name = "GL_BSACCLK")
public class GLBsaAccLock extends BaseEntity<String> {

    @Id
    @Column(name = "BSAACID")
    private String bsaAcid;

    @Column(name = "UPD_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    public GLBsaAccLock() {
    }

    public GLBsaAccLock(String bsaAcid, Date updateDate) {
        this.bsaAcid = bsaAcid;
        this.updateDate = updateDate;
    }

    @Override
    public String getId() {
        return bsaAcid;
    }

    public void setBsaAcid(String bsaAcid) {
        this.bsaAcid = bsaAcid;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public String getBsaAcid() {
        return bsaAcid;
    }

    public Date getUpdateDate() {
        return updateDate;
    }
}
