package ru.rbt.barsgl.ejb.entity.dict;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by er18837 on 04.08.2017.
 */
@Entity
@Table(name = "V_GL_LWDCUT")
public class LwdBalanceCutView extends BaseEntity<Date> {
    public static final String timeFormat = "HH:mm";

    @Id
    @Column(name = "RUNDATE")
    @Temporal(TemporalType.DATE)
    private Date runDate;

    @Column(name = "CUTOFFTIME")
    private String cutTime;

    @Column(name = "CUT_DT")
    @Temporal(TemporalType.TIMESTAMP)
    private Date cutDateTime;

    @Column(name = "OTS_CLOSE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date closeDateTime;

    @Override
    public Date getId() {
        return runDate;
    }

    public Date getRunDate() {
        return runDate;
    }

    public String getCutTime() {
        return cutTime;
    }

    public Date getCutDateTime() {
        return cutDateTime;
    }

    public Date getCloseDateTime() {
        return closeDateTime;
    }

    public static String getTimeFormat() {
        return timeFormat;
    }
}
