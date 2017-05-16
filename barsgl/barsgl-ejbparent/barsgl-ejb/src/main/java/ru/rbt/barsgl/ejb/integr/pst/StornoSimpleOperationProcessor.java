package ru.rbt.barsgl.ejb.integr.pst;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.barsgl.ejb.repository.GLPostingRepository;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.enums.OperState;

import javax.inject.Inject;
import java.util.List;

import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.DIRECT;
import static ru.rbt.ejbcore.validation.ErrorCode.STORNO_POST_NOT_FOUND;

/**
 * Created by ER18837 on 07.04.15.
 */
public class StornoSimpleOperationProcessor extends GLOperationProcessor {

    @Inject
    private SimpleOperationProcessor simpleOperationProcessor;

    @Inject
    private GLOperationRepository glOperationRepository;

    @Inject
    private GLPostingRepository glPostingRepository;

    @Inject
    private OperdayController operdayController;

    @Override
    public boolean isSupported(GLOperation operation) {
        return     !operation.isFan()                                           // не веер
                && isStornoBackvalue(operation)                                 // сторно с прошедшей датой
                && !operation.isInterFilial()                                   // филиал один
                && !operation.isExchangeDifferenceA()                          // нет курсовой разницы
                && DIRECT == operdayController.getOperday().getPdMode()
                && !operation.isTech();                                         // признак операции по техническим счетам
    }

    @Override
    public GLOperation.OperType getOperationType() {
        return GLOperation.OperType.S;
    }

    @Override
    public void setSpecificParameters(GLOperation operation) throws Exception {
        simpleOperationProcessor.setSpecificParameters(operation);
    }

    @Override
    public List<GLPosting> createPosting(GLOperation operation) throws Exception {
        List<GLPosting> postList = simpleOperationProcessor.createPosting(operation);
        return postList;
    }

    @Override
    public void resolveOperationReference(GLOperation operation) throws Exception {
//        setStornoOperation(operation, GLOperation.StornoType.S);
        operation.setStornoRegistration(GLOperation.StornoType.S);
    }

    /**
     * Находит ссылку на сторнируемые проводки и добавляет ее в postList
     * @param operation     - операция
     * @param postList      - список проводок по операции
     */
    @Override
    public void resolvePostingReference(GLOperation operation, List<GLPosting> postList) {
        GLOperation stornoOperation = operation.getStornoOperation();
        if ( !stornoOperation.getState().equals(OperState.POST)) {
            // Операция не обработана - ошибка
            throw new ValidationError(ErrorCode.STORNO_REF_NOT_VALID,
                    stornoOperation.getId().toString(), stornoOperation.getState().name(), OperState.POST.name());
        }
        List<GLPosting> stornoList = glOperationRepository.getPostings(stornoOperation);
        for (GLPosting posting : postList) {
            Long pcidRef = glPostingRepository.getPcidByType(stornoList, posting.getStornoType());
            if (0 == pcidRef) {
                throw new ValidationError(STORNO_POST_NOT_FOUND,
                        stornoOperation.getId().toString(), posting.getStornoType());
            }
            posting.setStornoPcid(pcidRef);
        }
    }

}
