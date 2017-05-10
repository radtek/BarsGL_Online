package ru.rbt.barsgl.ejb.integr.pst;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.barsgl.ejb.repository.PdRepository;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.enums.OperState;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.DIRECT;
import static ru.rbt.barsgl.ejbcore.validation.ErrorCode.STORNO_REF_NOT_VALID;
/**
 * Created by Ivan Sevastyanov
 */
public class StornoOnedayOperationProcessor extends GLOperationProcessor {

    @Inject
    private PdRepository pdRepository;

    @Inject
    private GLOperationRepository glOperationRepository;

    @EJB
    private OperdayController operdayController;

    @Override
    public GLOperation.OperType getOperationType() {
        return GLOperation.OperType.ST;
    }

    @Override
    public boolean isSupported(GLOperation operation) {
        return     !operation.isFan()                                                                   // не веер
                && isStornoOneday(operation)                                                            // сторно в тот же день
                && DIRECT == operdayController.getOperday().getPdMode()
                && !operation.isTech();                                         // признак операции по техническим счетам
    }

    @Override
    public void setSpecificParameters(GLOperation operation) throws Exception {
    }

    @Override
    public void resolveOperationReference(GLOperation operation) throws Exception {
//        setStornoOperation(operation, GLOperation.StornoType.C);
        operation.setStornoRegistration(GLOperation.StornoType.C);
    }

    @Override
    public List<GLPosting> createPosting(GLOperation operation) throws Exception {
        GLOperation stornoOperation = operation.getStornoOperation();
        OperState operState = stornoOperation.getState();

        // Операция должна быть обработана
        if ( operState.equals(OperState.POST) ) {
            List<GLPosting> stornoList = glOperationRepository.getPostings(stornoOperation);
            if (null == stornoList || stornoList.isEmpty()) {
                throw new ValidationError(STORNO_REF_NOT_VALID,
                        stornoOperation.getId().toString(), operState.name() + " (нет проводок)", OperState.POST.name());
            }
            pdRepository.updatePdInvisible(true, stornoList);
        }
        // или не обработана из-за отсутствия счета
        else if ( !operState.equals(OperState.WTAC) ) {
            // Операция не обработана - ошибка
            throw new ValidationError(STORNO_REF_NOT_VALID,
                        stornoOperation.getId().toString(), operState.name(), OperState.POST.name() + " или " + OperState.WTAC.name());
        }

        // статус сторнируемой операции - CANC
        glOperationRepository.updateOperationStatus(stornoOperation, OperState.CANC);

        return Collections.emptyList();
    }

    @Override
    public OperState getSuccessStatus() {
        return OperState.SOCANC;
    }

}
