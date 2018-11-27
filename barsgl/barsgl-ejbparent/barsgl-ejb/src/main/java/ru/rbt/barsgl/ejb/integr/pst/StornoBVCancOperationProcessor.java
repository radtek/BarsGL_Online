package ru.rbt.barsgl.ejb.integr.pst;

import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.shared.enums.OperState;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.DIRECT;

/**
 * Created by er18837 on 26.11.2018.
 */
public class StornoBVCancOperationProcessor extends GLOperationProcessor{

    @Inject
    private StornoOnedayOperationProcessor stornoOnedayOperationProcessor;

    @Override
    public GLOperation.OperType getOperationType() {
        return GLOperation.OperType.ST;
    }

    @Override
    public boolean isSupported(GLOperation operation) {
        return     !operation.isFan()                                                                   // не веер
                && isStornoBVCanc(operation)                                                            // сторно в тот же день
                && DIRECT == operdayController.getOperday().getPdMode()
                && !operation.isTech();                                         // признак операции по техническим счетам
    }

    @Override
    public void setSpecificParameters(GLOperation operation) throws Exception {
        operation.setStornoRegistration(GLOperation.StornoType.C);
    }

    @Override
    public List<GLPosting> createPosting(GLOperation operation) throws Exception {
        stornoOnedayOperationProcessor.createPosting(operation);
        // TODO запись в журнал BV, AN_IND=’1’ ??
        return Collections.emptyList();
    }

    @Override
    public OperState getSuccessStatus() {
        return OperState.SOCANC;
    }
}
