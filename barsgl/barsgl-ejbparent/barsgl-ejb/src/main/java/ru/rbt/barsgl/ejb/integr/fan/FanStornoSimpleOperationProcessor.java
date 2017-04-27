package ru.rbt.barsgl.ejb.integr.fan;

import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.integr.pst.StornoSimpleOperationProcessor;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.barsgl.ejb.repository.GLPostingRepository;
import ru.rbt.ejbcore.validation.ValidationError;

import javax.inject.Inject;
import java.util.List;

import static ru.rbt.ejbcore.validation.ErrorCode.STORNO_POST_NOT_FOUND;

/**
 * Created by ER18837 on 18.05.15.
 */
public class FanStornoSimpleOperationProcessor extends FanOperationProcessor {

    @Inject
    private FanSimpleOperationProcessor fanSimpleOperationProcessor;

    @Inject
    private StornoSimpleOperationProcessor stornoSimpleOperationProcessor;

    @Inject
    private GLOperationRepository glOperationRepository;

    @Inject
    private GLPostingRepository glPostingRepository;

    @Override
    public boolean isSupported(GLOperation operation) {
        return     isStornoBackvalue(operation)                                 // сторно с прошедшей датой
                && operation.isFan()                                            // веер
                && !operation.isInterFilial()                                   // филиал один
                && !operation.isExchangeDifferenceA();                           // нет курсовой разницы
    }

    @Override
    public GLOperation.OperType getOperationType() {
        return GLOperation.OperType.S;
    }


    @Override
    public void setSpecificParameters(GLOperation operation) throws Exception {
        fanSimpleOperationProcessor.setSpecificParameters(operation);
    }

    @Override
    public void addPosting(GLOperation operation, List<GLPosting> postList) throws Exception {
        fanSimpleOperationProcessor.addPosting(operation, postList);
    }

    @Override
    public void resolveOperationReference(GLOperation operation) throws Exception {
        stornoSimpleOperationProcessor.resolveOperationReference(operation);
    }

    /**
     * Находит ссылку на сторнируемые проводки и добавляет ее в postList
     * для пера веерной проводки может не быть ссылки
     * @param operation     - операция
     * @param postList      - список проводок по операции
     */
    @Override
    public void resolvePostingReference(GLOperation operation, List<GLPosting> postList) {
        GLOperation stornoOperation = operation.getStornoOperation();
        List<GLPosting> stornoList = glOperationRepository.getPostings(stornoOperation);
        for (GLPosting posting : postList) {
            if ( null != posting.getStornoPcid())      // уже заполнено
                continue;
            Long pcidRef = glPostingRepository.getPcidByType(stornoList, posting.getStornoType());
            if (0 != pcidRef) {
                posting.setStornoPcid(pcidRef);
            } else if ( (null != operation.getFbSide())                                               // родительская проводка
                    || !GLPosting.PostingType.FanMain.getValue().equals(posting.getPostType()) ) {    // не основная проводка (!5)
                // для родительской проводки и для неосновной проводки пера - должна быть ссылка
                // для основной проводки пера  - нет
                throw new ValidationError(STORNO_POST_NOT_FOUND,
                        stornoOperation.getId().toString(), posting.getStornoType());
            }
        }
    }
}
