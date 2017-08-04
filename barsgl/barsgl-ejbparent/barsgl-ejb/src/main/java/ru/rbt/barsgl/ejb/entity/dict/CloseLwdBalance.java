package ru.rbt.barsgl.ejb.entity.dict;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by er18837 on 04.08.2017.
 */
@Entity
@Table(name = "GL_LWDCUT")
public class CloseLwdBalance extends BaseEntity<Date> {
    private final String timeFormat = "HH:mm";

    @Id
    @Column(name = "RUN_DATE")
    @Temporal(TemporalType.DATE)
    private Date runDate;

    @Column(name = "CUTOFFTIME")
    private String closeTime;

    @Column(name = "OTS")
    @Temporal(TemporalType.TIME)
    private Date createTimestamp;

    @Column(name = "USER")
    private String userName;

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

    public String getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(String closeTime) {
        this.closeTime = closeTime;
    }

    public Date getCreateTimestamp() {
        return createTimestamp;
    }

    public void setCreateTimestamp(Date createTimestamp) {
        this.createTimestamp = createTimestamp;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getTimeFormat() {
        return timeFormat;
    }
}
