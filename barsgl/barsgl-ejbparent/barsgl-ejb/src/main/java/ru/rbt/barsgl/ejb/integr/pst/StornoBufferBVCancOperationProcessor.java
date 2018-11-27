package ru.rbt.barsgl.ejb.integr.pst;

import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.shared.enums.OperState;

import java.util.Collections;
import java.util.List;

import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.BUFFER;

/**
 * Created by er18837 on 26.11.2018.
 */
public class StornoBufferBVCancOperationProcessor extends GLOperationProcessor {

    @Override
    public GLOperation.OperType getOperationType() {
        return GLOperation.OperType.ST;
    }

    @Override
    public boolean isSupported(GLOperation operation) {
        return     !operation.isFan()                                                                   // не веер
                && isStornoBVCanc(operation)                                                            // сторно в тот же день
                && BUFFER == operdayController.getOperday().getPdMode()
                && !operation.isTech();                                         // признак операции по техническим счетам
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
        return OperState.STRN_WAIT;
    }
}
