package ru.rbt.barsgl.ejb.integr.fan;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.integr.pst.StornoOnedayOperationProcessor;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.enums.OperState;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.List;


/**
 *
 * Created by ER18837 on 19.05.15.
 */
public class FanStornoOnedayOperationProcessor extends FanOperationProcessor {

    @Inject
    private OperdayController operdayController;

    @Inject
    private StornoOnedayOperationProcessor stornoOnedayOperationProcessor;

    @EJB
    private GLOperationRepository glOperationRepository;

    @Override
    public boolean isSupported(GLOperation operation) {
        return     operation.isFan()                        // веер
                && isStornoOneday(operation);               // сторно в тот же день
    }

    @Override
    public GLOperation.OperType getOperationType() {
        return GLOperation.OperType.ST;
    }


    @Override
    public void setSpecificParameters(GLOperation operation) throws Exception {

    }

    @Override
    public void resolveOperationReference(GLOperation operation) throws Exception {
        stornoOnedayOperationProcessor.resolveOperationReference(operation);
    }

    /**
     * Находит и отменяет сторноруемую операцию
     * @param operation
     * @param postList
     * @throws Exception
     */
    @Override
    public void addPosting(GLOperation operation, List<GLPosting> postList) throws Exception {
        GLOperation stornoOperation = operation.getStornoOperation();
        OperState operState = stornoOperation.getState();

        // Операция должна быть загружена или без ошибки или со статусом отсутствия счета
        if ( !(operState.equals(OperState.LOAD) || operState.equals(OperState.WTAC) )) {
            // Операция не обработана - ошибка
            throw new ValidationError(ErrorCode.STORNO_REF_NOT_VALID,
                stornoOperation.getId().toString(), operState.name(), OperState.LOAD.name() + " или " + OperState.WTAC.name());
        }

        // статус сторнируемой операции - CANC
        glOperationRepository.updateOperationStatus(stornoOperation, OperState.CANC);
    }

}
