package ru.rbt.barsgl.ejb.entity.dict;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by er18837 on 28.06.2017.
 */
@Entity
@Table(name = "V_GL_CRPRD")
public class ClosedReportPeriodView extends BaseEntity<Date> {

    @Id
    @Column(name = "PRD_LDATE")
    @Temporal(TemporalType.DATE)
    private Date lastDate;

    @Column(name = "PRD_CUTDATE")
    @Temporal(TemporalType.DATE)
    private Date cutDate;

    @Column(name = "USER_NAME")
    String user;

    @Column(name = "OTS")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTimestamp;

    @Override
    public Date getId() {
        return lastDate;
    }

    public Date getLastDate() {
        return lastDate;
    }

    public Date getCutDate() {
        return cutDate;
    }
}
