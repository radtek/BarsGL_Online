package ru.rbt.barsgl.ejb.integr.pst;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPd;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.barsgl.ejb.repository.GLPdRepository;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.Assert;
import ru.rbt.barsgl.shared.enums.OperState;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.BUFFER;
import static ru.rbt.barsgl.ejbcore.validation.ErrorCode.STORNO_REF_NOT_VALID;

/**
 * Created by Ivan Sevastyanov on 11.02.2016.
 * обработка операции сторно в режиме BUFFER
 */
public class StornoBufferOnedayOperationProcessor extends GLOperationProcessor {

    private static final Logger log = Logger.getLogger(StornoBufferOnedayOperationProcessor.class.getName());

    @Inject
    private StornoOnedayOperationProcessor directStornoOnedayOperationProcessor;

    @EJB
    private OperdayController operdayController;

    @Inject
    private GLPdRepository glPdRepository;

    @Inject
    private GLOperationRepository glOperationRepository;

    @Override
    public boolean isSupported(GLOperation operation) {
        return     !operation.isFan()                                                                   // не веер
                && isStornoOneday(operation)                                                            // сторно в тот же день
                && BUFFER == operdayController.getOperday().getPdMode();                                // режим BUFFER
    }

    @Override
    public GLOperation.OperType getOperationType() {
        return directStornoOnedayOperationProcessor.getOperationType();
    }

    @Override
    public void setSpecificParameters(GLOperation operation) throws Exception {
        directStornoOnedayOperationProcessor.setSpecificParameters(operation);
    }

    @Override
    public void resolveOperationReference(GLOperation operation) throws Exception {
        directStornoOnedayOperationProcessor.resolveOperationReference(operation);
    }

    @Override
    public List<GLPosting> createPosting(GLOperation operation) throws Exception {
        GLOperation stornoOperation = operation.getStornoOperation();
        OperState operState = stornoOperation.getState();
        if ( operState.equals(OperState.POST) ) {
            int cancelled;
            List<GLPd> stornoGlPdList = glPdRepository.getGLPostings(stornoOperation);
            if (stornoGlPdList.isEmpty()) {
                List<GLPosting> stornoListPostings = glOperationRepository.getPostings(stornoOperation);
                Assert.isTrue(!stornoListPostings.isEmpty(),
                        () -> new ValidationError(STORNO_REF_NOT_VALID,
                                stornoOperation.getId().toString(), operState.name() + " (нет проводок, BUFFER MODE)", OperState.POST.name()));
                cancelled = pdRepository.updatePdInvisible(true, stornoListPostings);
            } else {
                cancelled = glPdRepository.updateGLPdInvisible(stornoGlPdList);
            }
            log.info(format("Подавлено проводок в режиме BUFFER: '%s'", cancelled));
        } else if (!operState.equals(OperState.WTAC) ) {
            // или не обработана из-за отсутствия счета
            // Операция не обработана - ошибка
            throw new ValidationError(STORNO_REF_NOT_VALID,
                    stornoOperation.getId().toString(), operState.name() + " (BUFFER MODE)", OperState.POST.name() + " или " + OperState.WTAC.name());
        }
        // статус сторнируемой операции - CANC
        glOperationRepository.updateOperationStatus(stornoOperation, OperState.CANC);

        return Collections.emptyList();
    }

    @Override
    public OperState getSuccessStatus() {
        return directStornoOnedayOperationProcessor.getSuccessStatus();
    }
}
