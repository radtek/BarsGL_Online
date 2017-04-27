package ru.rbt.barsgl.ejb.entity.gl;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by Ivan Sevastyanov on 11.02.2016.
 */
@Entity
@Table(name = "GL_EXBTACC")
public class GLBalanceExclude extends BaseEntity<GLBalanceExcludeId> {

    @EmbeddedId
    private GLBalanceExcludeId id;

    @Temporal(TemporalType.DATE)
    @Column(name = "DATTO")
    private Date dtTo;

    @Override
    public GLBalanceExcludeId getId() {
        return id;
    }

    @Override
    public void setId(GLBalanceExcludeId id) {
        this.id = id;
    }

    public Date getDtTo() {
        return dtTo;
    }

    public void setDtTo(Date dtTo) {
        this.dtTo = dtTo;
    }
}
