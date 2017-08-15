package ru.rbt.barsgl.ejb.entity.dict;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by er18837 on 28.06.2017.
 */
@Entity
@Table(name = "GL_BVPARM")
public class BVSourceDeal extends BaseEntity<BVSourceDealId> {
    @EmbeddedId
    BVSourceDealId id;

    @Column(name = "DTE")
    @Temporal(TemporalType.DATE)
    private Date endDate;

    @Column(name = "BV_SHIFT")
    Integer shift;

    @Column(name = "USER")
    String user;

    @Column(name = "OTS")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTimestamp;

    public BVSourceDeal() {
    }

    public BVSourceDeal(BVSourceDealId id, Date endDate, Integer shift, String user, Date createTimestamp) {
        this.id = id;
        this.endDate = endDate;
        this.shift = shift;
        this.user = user;
        this.createTimestamp = createTimestamp;
    }

    public BVSourceDealId getId() {
        return id;
    }

    public void setId(BVSourceDealId id) {
        this.id = id;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Integer getShift() {
        return shift;
    }

    public void setShift(Integer shift) {
        this.shift = shift;
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
