package ru.rbt.barsgl.ejb.integr.fan;

import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;

import javax.inject.Inject;
import java.util.List;

/**
 * Created by ER18837 on 22.05.15.
 */
public class FanStornoMfoOperationProcessor extends FanOperationProcessor {

    @Inject
    FanMfoOperationProcessor fanMfoOperationProcessor;

    @Inject
    FanStornoSimpleOperationProcessor fanStornoSimpleOperationProcessor;

    @Override
    public boolean isSupported(GLOperation operation) {
        return      isStornoBackvalue(operation)                                // сторно с прошедшей датой
                &&  operation.isFan()                                           // веер
                &&  operation.isInterFilial()                                   // межфилиал
                && !operation.isExchangeDifferenceA();                          // НЕТ курсовой разницы
    }

    @Override
    public GLOperation.OperType getOperationType() {
        return GLOperation.OperType.M;
    }

    @Override
    public void setSpecificParameters(GLOperation operation) throws Exception {
        fanMfoOperationProcessor.setSpecificParameters(operation);
    }

    @Override
    public void addPosting(GLOperation operation, List<GLPosting> postList) throws Exception {
        fanMfoOperationProcessor.addPosting(operation, postList);
    }

    @Override
    public void resolveOperationReference(GLOperation operation) throws Exception {
        fanStornoSimpleOperationProcessor.resolveOperationReference(operation);
    }

    @Override
    public void resolvePostingReference(GLOperation operation, List<GLPosting> postList) {
        fanStornoSimpleOperationProcessor.resolvePostingReference(operation, postList);
    }
}
