package ru.rbt.barsgl.ejb.integr.pst;

import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;

import javax.inject.Inject;
import java.util.List;

import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.BUFFER;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.DIRECT;

/**
 * Created by Ivan Sevastyanov on 11.02.2016.
 */
public class StornoBufferMfoOperationProcessor extends GLOperationProcessor {

    @Inject
    private StornoMfoOperationProcessor directStornoMfoOperationProcessor;

    @Inject
    private StornoBufferSimpleOperationProcessor stornoBufferSimpleOperationProcessor;

    @Override
    public boolean isSupported(GLOperation operation) {
        return     !operation.isFan()                                           // не веер
                && isStornoBackvalue(operation)                                 // сторно с прошедшей датой
                &&  operation.isInterFilial()                                   // филиалы разные
                && !operation.isExchangeDifferenceA()                           // нет курсовой разницы
                && BUFFER == operdayController.getOperday().getPdMode();
    }

    @Override
    public GLOperation.OperType getOperationType() {
        return directStornoMfoOperationProcessor.getOperationType();
    }

    @Override
    public void setSpecificParameters(GLOperation operation) throws Exception {
        directStornoMfoOperationProcessor.setSpecificParameters(operation);
    }

    @Override
    public List<GLPosting> createPosting(GLOperation operation) throws Exception {
        return directStornoMfoOperationProcessor.createPosting(operation);
    }

    @Override
    public void resolveOperationReference(GLOperation operation) throws Exception {
        directStornoMfoOperationProcessor.resolveOperationReference(operation);
    }

    @Override
    public void resolvePostingReference(GLOperation operation, List<GLPosting> postList) {
        stornoBufferSimpleOperationProcessor.resolvePostingReference(operation, postList);
    }
}
