package ru.rbt.barsgl.ejb.entity.dict;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by er18837 on 28.06.2017.
 */
@Entity
@Table(name = "V_GL_BVPARM")
public class BVSourceDealView extends BaseEntity<String> {

    @Id
    @Column(name = "ID_SRC")
    String sourceDeal;

    @Column(name = "BV_SHIFT", insertable = false, updatable = false)
    Integer shift;

    @Override
    public String getId() {
        return sourceDeal;
    }

    public String getSourceDeal() {
        return sourceDeal;
    }

    public Integer getShift() {
        return shift;
    }
}
