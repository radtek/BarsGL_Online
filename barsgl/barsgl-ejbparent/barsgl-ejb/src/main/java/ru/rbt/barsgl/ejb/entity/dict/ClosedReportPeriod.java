package ru.rbt.barsgl.ejb.entity.dict;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by er18837 on 14.08.2017.
 */
@Entity
@Table(name = "GL_CRPRD")
public class ClosedReportPeriod extends BaseEntity<Date> {

    @Id
    @Column(name = "PRD_LDATE")
    @Temporal(TemporalType.DATE)
    private Date lastDate;

    @Column(name = "PRD_CUTDATE")
    @Temporal(TemporalType.DATE)
    private Date cutDate;

    @Column(name = "USER")
    String user;

    @Column(name = "OTS")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTimestamp;

    public ClosedReportPeriod() {
    }

    public ClosedReportPeriod(Date lastDate, Date cutDate, String user, Date createTimestamp) {
        this.lastDate = lastDate;
        this.cutDate = cutDate;
        this.user = user;
        this.createTimestamp = createTimestamp;
    }

    @Override
    public Date getId() {
        return lastDate;
    }

    public Date getLastDate() {
        return lastDate;
    }

    public void setLastDate(Date lastDate) {
        this.lastDate = lastDate;
    }

    public Date getCutDate() {
        return cutDate;
    }

    public void setCutDate(Date cutDate) {
        this.cutDate = cutDate;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public Date getCreateTimestamp() {
        return createTimestamp;
    }

    public void setCreateTimestamp(Date createTimestamp) {
        this.createTimestamp = createTimestamp;
    }
}
