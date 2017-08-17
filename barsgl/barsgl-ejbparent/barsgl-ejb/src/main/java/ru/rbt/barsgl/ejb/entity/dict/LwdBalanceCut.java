package ru.rbt.barsgl.ejb.entity.dict;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by er18837 on 07.08.2017.
 */
@Entity
@Table(name = "GL_LWDCUT")
public class LwdBalanceCut extends BaseEntity<Date> {

    @Id
    @Column(name = "RUNDATE")
    @Temporal(TemporalType.DATE)
    private Date runDate;

    @Column(name = "CUTOFFTIME")
    private String cutTime;

    @Column(name = "USER")
    private String userName;

    @Column(name = "OTS", insertable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createDateTime;

    @Column(name = "OTS_CLOSE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date closeDateTime;


    public LwdBalanceCut() {
    }

    public LwdBalanceCut(Date runDate, String cutTime, String userName) {
        this.runDate = runDate;
        this.cutTime = cutTime;
        this.userName = userName;
    }

    @Override
    public Date getId() {
        return runDate;
    }

    public Date getRunDate() {
        return runDate;
    }

    public void setRunDate(Date runDate) {
        this.runDate = runDate;
    }

    public String getCutTime() {
        return cutTime;
    }

    public void setCutTime(String cutTime) {
        this.cutTime = cutTime;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Date getCreateDateTime() {
        return createDateTime;
    }

    public void setCreateDateTime(Date createDateTime) {
        this.createDateTime = createDateTime;
    }

    public Date getCloseDateTime() {
        return closeDateTime;
    }

    public void setCloseDateTime(Date closeDateTime) {
        this.closeDateTime = closeDateTime;
    }
}