package ru.rbt.barsgl.ejb.entity.acc;

import javax.persistence.Column;
import java.io.Serializable;

/**
 * Created by er18837 on 22.02.2018.
 */
public class GLAccParam implements Serializable {
    private String acid;
    private String bsaAcid;

    public GLAccParam(String acid, String bsaacid) {
        this.acid = acid;
        this.bsaAcid = bsaacid;
    }

    public GLAccParam(AccRlnId id) {
        this.acid = id.getAcid();
        this.bsaAcid = id.getBsaAcid();
    }

    public String getAcid() {
        return acid;
    }

    public void setAcid(String acid) {
        this.acid = acid;
    }

    public String getBsaAcid() {
        return bsaAcid;
    }

    public void setBsaAcid(String bsaAcid) {
        this.bsaAcid = bsaAcid;
    }
}
