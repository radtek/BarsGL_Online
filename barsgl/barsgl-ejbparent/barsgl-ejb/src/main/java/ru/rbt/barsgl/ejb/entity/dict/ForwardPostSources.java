package ru.rbt.barsgl.ejb.entity.dict;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by er18837 on 14.02.2018.
 */
@Entity
@Table(name = "GL_FWPSTD")
public class ForwardPostSources extends BaseEntity<String> {

    @Id
    @Column(name = "ID_SRC")
    String sourceDeal;

    @Column(name = "DTB")
    @Temporal(TemporalType.DATE)
    private Date startDate;

    @Column(name = "DTE")
    @Temporal(TemporalType.DATE)
    private Date endDate;

    @Override
    public String getId() {
        return sourceDeal;
    }

    public String getSourceDeal() {
        return sourceDeal;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

}
