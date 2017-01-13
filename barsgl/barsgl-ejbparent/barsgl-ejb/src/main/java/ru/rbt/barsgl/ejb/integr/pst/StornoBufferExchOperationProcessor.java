package ru.rbt.barsgl.ejb.integr.pst;

import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;

import javax.inject.Inject;
import java.util.List;

import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.BUFFER;

/**
 * Created by Ivan Sevastyanov on 11.02.2016.
 */
public class StornoBufferExchOperationProcessor extends GLOperationProcessor {
    @Inject
    private ExchOperationProcessor exchOperationProcessor;

    @Inject
    private StornoSimpleOperationProcessor stornoSimpleOperationProcessor;

    @Inject
    private StornoBufferSimpleOperationProcessor bufferSimpleOperationProcessor;

    @Override
    public boolean isSupported(GLOperation operation) {
        return     !operation.isFan()                                           // не веер
                && isStornoBackvalue(operation)                                 // сторно с прошедшей датой
                && !operation.isInterFilial()                                   // филиал один
                &&  operation.isExchangeDifferenceA()                           // есть курсовая разница
                &&  BUFFER == operdayController.getOperday().getPdMode();
    }

    @Override
    public GLOperation.OperType getOperationType() {
        return GLOperation.OperType.E;
    }

    @Override
    public void setSpecificParameters(GLOperation operation) throws Exception {
        exchOperationProcessor.setSpecificParameters(operation);
    }

    @Override
    public List<GLPosting> createPosting(GLOperation operation) throws Exception {
        return exchOperationProcessor.createPosting(operation);
    }

    @Override
    public void resolveOperationReference(GLOperation operation) throws Exception {
        stornoSimpleOperationProcessor.resolveOperationReference(operation);
    }

    @Override
    public void resolvePostingReference(GLOperation operation, List<GLPosting> postList) {
        bufferSimpleOperationProcessor.resolvePostingReference(operation, postList);
    }
}
