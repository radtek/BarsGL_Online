package ru.rbt.barsgl.shared.dict;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by akichigi on 24.08.16.
 */
public class ActParmWrapper implements Serializable,IsSerializable {
    private String accType;
    private String cusType;
    private String term;
    private String acc2;
    private String plcode;
    private String acod;
    private String ac_sq;
    private String dtb; //date
    private String dte; //date

    public String getAccType() {
        return accType;
    }

    public void setAccType(String accType) {
        this.accType = accType;
    }

    public String getCusType() {
        return cusType;
    }

    public void setCusType(String cusType) {
        this.cusType = cusType;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public String getAcc2() {
        return acc2;
    }

    public void setAcc2(String acc2) {
        this.acc2 = acc2;
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

    public String getDtb() {
        return dtb;
    }

    public void setDtb(String dtb) {
        this.dtb = dtb;
    }

    public String getDte() {
        return dte;
    }

    public void setDte(String dte) {
        this.dte = dte;
    }
}
