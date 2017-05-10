package ru.rbt.barsgl.ejb.integr.pst;

import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;

import javax.inject.Inject;
import java.util.List;

import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.BUFFER;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.DIRECT;

/**
 * Created by Ivan Sevastyanov on 12.02.2016.
 */
public class StornoBufferMfoExchOperationProcessor extends GLOperationProcessor  {
    @Inject
    private MfoExchOperationProcessor mfoExchOperationProcessor;

    @Inject
    private StornoSimpleOperationProcessor directStornoSimpleOperationProcessor;

    @Inject
    private StornoBufferSimpleOperationProcessor bufferStornoSimpleOperationProcessor;

    @Override
    public boolean isSupported(GLOperation operation) {
        return     !operation.isFan()                                           // не веер
                && isStornoBackvalue(operation)                                 // сторно с прошедшей датой
                &&  operation.isInterFilial()                                   // филиалы разные
                &&  operation.isExchangeDifferenceA()                           // есть курсовая разница
                &&  BUFFER == operdayController.getOperday().getPdMode()
                && !operation.isTech();                                         //признак операции по техническому счёту
    }

    @Override
    public GLOperation.OperType getOperationType() {
        return GLOperation.OperType.ME;
    }

    @Override
    public void setSpecificParameters(GLOperation operation) throws Exception {
        mfoExchOperationProcessor.setSpecificParameters(operation);
    }

    @Override
    public List<GLPosting> createPosting(GLOperation operation) throws Exception {
        return mfoExchOperationProcessor.createPosting(operation);
    }

    @Override
    public void resolveOperationReference(GLOperation operation) throws Exception {
        directStornoSimpleOperationProcessor.resolveOperationReference(operation);
    }

    @Override
    public void resolvePostingReference(GLOperation operation, List<GLPosting> postList) {
        bufferStornoSimpleOperationProcessor.resolvePostingReference(operation, postList);
    }
}
