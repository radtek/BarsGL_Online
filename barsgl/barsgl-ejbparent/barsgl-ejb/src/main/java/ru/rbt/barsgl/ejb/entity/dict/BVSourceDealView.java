package ru.rbt.barsgl.ejb.entity.dict;

import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.ejbcore.mapping.YesNo;

import javax.persistence.*;

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

    @Column(name = "BVSTRN_INVISIBLE", insertable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private YesNo bvStornoInvisible;

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

    public YesNo getBvStornoInvisible() {
        return bvStornoInvisible;
    }

    public void setBvStornoInvisible(YesNo bvStornoInvisible) {
        this.bvStornoInvisible = bvStornoInvisible;
    }

    public boolean isBvStornoInvisible() {
        return bvStornoInvisible == YesNo.Y;
    }
}
