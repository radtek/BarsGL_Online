package ru.rbt.barsgl.ejb.entity.gl;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by er18837 on 27.06.2017.
 */
@Entity
@DiscriminatorValue("BACK_VALUE")
public class GLBackValueOperation extends GLOperation {

    // GL_OPEREXT - Параметры авторизации BackValue ======

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, optional = false)    // fetch = FetchType.LAZY,
    @JoinColumn(name="GLOID", insertable = false, updatable = false)
    private GLOperationExt operExt;

    public GLOperationExt getOperExt() {
        return operExt;
    }

    public void setOperExt(GLOperationExt operExt) {
        this.operExt = operExt;
    }

    public boolean isAutomatic() {return false;}

    public boolean isManual() {return false;}

    public boolean isBackValue() {return true;}

}
