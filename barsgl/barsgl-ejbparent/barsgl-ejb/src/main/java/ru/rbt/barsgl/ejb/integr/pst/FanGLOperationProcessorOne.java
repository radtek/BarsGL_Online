package ru.rbt.barsgl.ejb.integr.pst;

import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.shared.enums.OperState;

import java.util.Collections;
import java.util.List;

import static ru.rbt.barsgl.ejbcore.mapping.YesNo.Y;

/**
 * Created by Ivan Sevastyanov
 * обработка одной веерной провождки ("первичная" обработка вееров)
 */
public class FanGLOperationProcessorOne extends GLOperationProcessor {


    @Override
    public boolean isSupported(GLOperation operation) {
        return     operation.isFan();                                          // веер
    }

    @Override
    public GLOperation.OperType getOperationType() {
        return GLOperation.OperType.F;
    }

    @Override
    public void setSpecificParameters(GLOperation operation) throws Exception {

    }

    @Override
    public List<GLPosting> createPosting(GLOperation operation) throws Exception {
        return Collections.emptyList();
    }

    @Override
    public OperState getSuccessStatus() {
        return OperState.LOAD;
    }

}
