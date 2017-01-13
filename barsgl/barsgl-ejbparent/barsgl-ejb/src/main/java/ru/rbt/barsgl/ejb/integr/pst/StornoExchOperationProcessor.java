package ru.rbt.barsgl.ejb.integr.pst;

import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.repository.GLPostingRepository;

import javax.inject.Inject;
import java.util.List;

import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.DIRECT;

/**
 * Created by ER18837 on 07.04.15.
 */
public class StornoExchOperationProcessor extends GLOperationProcessor {

    @Inject
    private ExchOperationProcessor exchOperationProcessor;

    @Inject
    private StornoSimpleOperationProcessor stornoSimpleOperationProcessor;

    @Override
    public boolean isSupported(GLOperation operation) {
        return     !operation.isFan()                                           // не веер
                && isStornoBackvalue(operation)                                 // сторно с прошедшей датой
                && !operation.isInterFilial()                                   // филиал один
                &&  operation.isExchangeDifferenceA()                           // есть курсовая разница
                &&  DIRECT == operdayController.getOperday().getPdMode();
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
        stornoSimpleOperationProcessor.resolvePostingReference(operation, postList);
    }

}
