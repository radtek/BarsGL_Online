package ru.rbt.barsgl.ejb.entity.dict;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by er18837 on 28.06.2017.
 */
@Embeddable
@MappedSuperclass
public class BVSourceDealId implements Serializable {

    @Column(name = "DTB")
    @Temporal(TemporalType.DATE)
    private Date startDate;

    @Column(name = "ID_SRC")
    private String sourceDeal;

    public BVSourceDealId() {
    }

    public BVSourceDealId(String sourceDeal, Date startDate) {
        this.startDate = startDate;
        this.sourceDeal = sourceDeal;
    }


    public Date getStartDate() {
        return startDate;
    }

    public String getSourceDeal() {
        return sourceDeal;
    }
}
