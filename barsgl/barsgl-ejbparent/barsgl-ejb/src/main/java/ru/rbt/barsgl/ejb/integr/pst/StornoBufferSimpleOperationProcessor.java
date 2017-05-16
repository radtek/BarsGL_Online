package ru.rbt.barsgl.ejb.integr.pst;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPd;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.repository.GLPdRepository;
import ru.rbt.barsgl.ejb.repository.GLPostingRepository;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.barsgl.shared.enums.OperState;

import javax.inject.Inject;
import java.util.List;

import static java.lang.String.format;
import static ru.rbt.ejbcore.validation.ErrorCode.STORNO_POST_NOT_FOUND;
import static ru.rbt.ejbcore.validation.ErrorCode.STORNO_POST_NOT_FOUND_BUFFER;
import static ru.rbt.ejbcore.validation.ErrorCode.STORNO_REF_NOT_VALID;
import static ru.rbt.barsgl.shared.enums.OperState.POST;

/**
 * Created by Ivan Sevastyanov on 11.02.2016.
 */
public class StornoBufferSimpleOperationProcessor extends GLOperationProcessor {

    @Inject
    private StornoSimpleOperationProcessor directStornoSimpleOperationProcessor;

    @Inject
    private OperdayController operdayController;

    @Inject
    private GLPdRepository glPdRepository;

    @Inject
    private GLPostingRepository glPostingRepository;

    @Override
    public boolean isSupported(GLOperation operation) {
        return     !operation.isFan()                                           // не веер
                && isStornoBackvalue(operation)                                 // сторно с прошедшей датой
                && !operation.isInterFilial()                                   // филиал один
                && !operation.isExchangeDifferenceA()                          // нет курсовой разницы
                && Operday.PdMode.BUFFER == operdayController.getOperday().getPdMode()
                && !operation.isTech();                                         //признак операции по техническим счетам
    }

    @Override
    public GLOperation.OperType getOperationType() {
        return directStornoSimpleOperationProcessor.getOperationType();
    }

    @Override
    public void setSpecificParameters(GLOperation operation) throws Exception {
        directStornoSimpleOperationProcessor.setSpecificParameters(operation);
    }

    @Override
    public List<GLPosting> createPosting(GLOperation operation) throws Exception {
        return directStornoSimpleOperationProcessor.createPosting(operation);
    }

    @Override
    public void resolveOperationReference(GLOperation operation) throws Exception {
        directStornoSimpleOperationProcessor.resolveOperationReference(operation);
    }

    @Override
    public void resolvePostingReference(GLOperation operation, List<GLPosting> postList) {
        GLOperation stornoOperation = operation.getStornoOperation();
        if ( !stornoOperation.getState().equals(OperState.POST)) {
            // Операция не обработана - ошибка
            throw new ValidationError(ErrorCode.STORNO_REF_NOT_VALID,
                    stornoOperation.getId().toString(), stornoOperation.getState().name(), OperState.POST.name());
        }
        List<GLPd> stornoglPds = glPdRepository.getGLPostings(stornoOperation);
        if (!stornoglPds.isEmpty()) {
            postList.forEach(post -> {
                Long pcidRef = stornoglPds.stream().filter(pd -> post.getStornoType()
                        .equals(pd.getPostType())).findFirst().orElse(stornoglPds.get(0)).getPcId();
                post.setStornoPcid(pcidRef);
            });
        } else {
            // попытка установки ссылок по таблице GLPosting
            directStornoSimpleOperationProcessor.resolvePostingReference(operation, postList);
        }
    }
}
