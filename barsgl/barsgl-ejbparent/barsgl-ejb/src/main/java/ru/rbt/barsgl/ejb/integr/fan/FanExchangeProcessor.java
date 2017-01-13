package ru.rbt.barsgl.ejb.integr.fan;

import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.integr.pst.ExchOperationProcessor;
import ru.rbt.barsgl.ejb.repository.GLPostingRepository;

import javax.inject.Inject;
import java.util.List;

/**
 * Created by Ivan Sevastyanov
 */
public class FanExchangeProcessor extends FanOperationProcessor {

    @Inject
    private ExchOperationProcessor exchOperationProcessor;

    @Inject
    private FanSimpleOperationProcessor fanSimpleOperationProcessor;

    @Inject
    private GLPostingRepository glPostingRepository;

    @Override
    public boolean isSupported(GLOperation operation) {
        return     !operation.isStorno()                                        // НЕ сторно
                && !operation.isInterFilial()                                   // НЕ межфилиал
                &&  operation.isFan()                                           // веер
                &&  operation.isExchangeDifferenceA();                           // с курсовой разницей
    }

    @Override
    public GLOperation.OperType getOperationType() {
        return GLOperation.OperType.E;
    }

    @Override
    public void setSpecificParameters(GLOperation operation) throws Exception {
        exchOperationProcessor.setAccountExchange(operation, operation.getAccountDebit());
    }

    @Override
    public void addPosting(GLOperation operation, List<GLPosting> postList) throws Exception {
        fanSimpleOperationProcessor.addSimpleFeatherPosting(operation, postList);
        postList.add(createExchangeDiffPosting(operation));
    }

    private GLPosting createExchangeDiffPosting(GLOperation operation) {
        return glPostingRepository.createExchPosting(operation, GLPosting.PostingType.ExchDiff,
                operation.getAccountDebit(), operation.getCurrencyDebit(),
                operation.getAccountCredit(), operation.getCurrencyCredit());
    }
}
