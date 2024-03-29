package ru.rbt.barsgl.ejb.integr.bg;

import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.ejbcore.mapping.YesNo;

import java.util.List;

/**
 * Created by ER18837 on 09.04.15.
 */
public interface GLOperationController <From, To extends BaseEntity> {
    public List<To> processOperations(From reference);
}

