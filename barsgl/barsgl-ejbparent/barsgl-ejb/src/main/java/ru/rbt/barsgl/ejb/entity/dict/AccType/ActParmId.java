package ru.rbt.barsgl.ejb.entity.dict.AccType;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import static ru.rbt.ejbcore.util.StringUtils.rightPad;

/**
 * Created by akichigi on 24.08.16.
 */

@Embeddable
public class ActParmId implements Serializable {
    @Column(name = "ACCTYPE")
    private String accType;

    @Column(name = "CUSTYPE")
    private String cusType;

    @Column(name = "TERM")
    private String term;

    @Column(name = "ACC2")
    private String acc2;

    @Column(name = "DTB")
    @Temporal(TemporalType.DATE)
    private Date dtb;

    public ActParmId(){}

    public ActParmId(String accType, String cusType, String term, String acc2, Date dtb) {
        this.accType = accType;
        this.cusType = rightPad(cusType, 3, " ");
        this.term = term;
        this.acc2 = acc2;
        this.dtb = dtb;
    }

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
        this.cusType = rightPad(cusType, 3, " ");
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

    public Date getDtb() {
        return dtb;
    }

    public void setDtb(Date dtb) {
        this.dtb = dtb;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActParmId actParmId = (ActParmId) o;
        return Objects.equals(accType, actParmId.accType) &&
                Objects.equals(cusType, actParmId.cusType) &&
                Objects.equals(term, actParmId.term) &&
                Objects.equals(acc2, actParmId.acc2) &&
                Objects.equals(dtb, actParmId.dtb);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accType, cusType, term, acc2, dtb);
    }
}
