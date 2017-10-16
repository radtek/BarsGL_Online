package ru.rbt.barsgl.ejb.entity.gl;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Created by ER18837 on 13.08.15.
 * GL_OPER для ввода ручной операции
 */
@Entity
@DiscriminatorValue("MANUAL")
public class GLManualOperation extends GLOperation{
    public boolean isAutomatic() {return false;}

    public boolean isManual() {return true;}

    public boolean isBackValue() {return false;}

}
