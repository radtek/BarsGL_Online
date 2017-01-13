package ru.rbt.barsgl.ejb.entity.gl;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * Created by Ivan Sevastyanov
 * Идентификатор для журнала backvalue
 */
@Embeddable
public class BackvalueJournalId implements Serializable {

    @Column(name = "ACID")
    private String acid;

    @Column(name = "BSAACID")
    private String bsaAcid;

    @Column(name = "POD")
    @Temporal(TemporalType.DATE)
    private Date postingDate;

    public BackvalueJournalId() {
    }

    public BackvalueJournalId(String acid, String bsaAcid, Date postingDate) {
        this.acid = acid;
        this.bsaAcid = bsaAcid;
        this.postingDate = postingDate;
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

    public Date getPostingDate() {
        return postingDate;
    }

    public void setPostingDate(Date postingDate) {
        this.postingDate = postingDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BackvalueJournalId that = (BackvalueJournalId) o;
        return Objects.equals(acid, that.acid) &&
                Objects.equals(bsaAcid, that.bsaAcid) &&
                Objects.equals(postingDate, that.postingDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(acid, bsaAcid, postingDate);
    }

    @Override
    public String toString() {
        return "BackvalueJournalId{" +
                "acid='" + acid + '\'' +
                ", bsaAcid='" + bsaAcid + '\'' +
                ", postingDate=" + postingDate +
                '}';
    }
}
