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
    private YesNo stornoInvisible;

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

    public YesNo getStornoInvisible() {
        return stornoInvisible;
    }

    public void setStornoInvisible(YesNo stornoInvisible) {
        this.stornoInvisible = stornoInvisible;
    }

    public boolean isStornoInvisible() {
        return stornoInvisible == YesNo.Y;
    }
}
