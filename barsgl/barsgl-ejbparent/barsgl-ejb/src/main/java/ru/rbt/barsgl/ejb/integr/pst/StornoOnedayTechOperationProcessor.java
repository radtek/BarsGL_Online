package ru.rbt.barsgl.ejb.integr.pst;

import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.entity.gl.GlPdTh;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.barsgl.ejb.repository.GLTechOperationRepository;
import ru.rbt.barsgl.ejb.repository.GlPdThRepository;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejbcore.validation.ValidationError;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

import static ru.rbt.ejbcore.validation.ErrorCode.STORNO_REF_NOT_VALID;

/**
 * Created by er23851 on 01.08.2017.
 */
public class StornoOnedayTechOperationProcessor extends GLOperationProcessor
{
    @Inject
    private GLOperationRepository glOperationRepository;

    @Inject
    private GLTechOperationRepository glTechOperationRepository;

    @Inject
    private GlPdThRepository glPdThRepository;

    @Override
    public GLOperation.OperType getOperationType() {
        return GLOperation.OperType.ST;
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
    public boolean isSupported(GLOperation operation) {
        return     !operation.isFan()                                                                   // не веер
                && isStornoOneday(operation)                                                            // сторно в тот же день
                && operation.isTech();                                         // признак операции по техническим счетам
    }

    @Override
    public OperState getSuccessStatus() {
        return OperState.SOCANC;
    }

    @Override
    public List<GLPosting> createPosting(GLOperation operation) throws Exception {
        GLOperation stornoOperation = operation.getStornoOperation();
        OperState operState = stornoOperation.getState();

        // Операция должна быть обработана
        if ( operState.equals(OperState.POST) ) {
            List<GlPdTh> stornoList = glPdThRepository.getPostings(stornoOperation);
            if (null == stornoList || stornoList.isEmpty()) {
                throw new ValidationError(STORNO_REF_NOT_VALID,
                        stornoOperation.getId().toString(), operState.name() + " (нет проводок)", OperState.POST.name());
            }
            int i = glTechOperationRepository.updatePdInvisible(true, stornoList);
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


}
