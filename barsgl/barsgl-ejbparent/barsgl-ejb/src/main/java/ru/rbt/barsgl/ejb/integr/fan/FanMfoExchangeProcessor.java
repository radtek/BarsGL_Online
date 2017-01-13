package ru.rbt.barsgl.ejb.integr.fan;

import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.integr.pst.MfoExchOperationProcessor;
import ru.rbt.barsgl.ejb.repository.GLPostingRepository;
import ru.rbt.barsgl.ejbcore.util.StringUtils;

import javax.inject.Inject;
import java.util.List;

import static ru.rbt.barsgl.ejb.entity.gl.GLOperation.OperSide.C;

/**
 * Created by Ivan Sevastyanov
 */
public class FanMfoExchangeProcessor extends FanOperationProcessor {

    @Inject
    private FanMfoOperationProcessor fanMfoOperationProcessor;

    @Inject
    private GLPostingRepository postingRepository;

    @Inject
    private MfoExchOperationProcessor mfoExchOperationProcessor;

    @Override
    public boolean isSupported(GLOperation operation) {
        return     !operation.isStorno()                                        // НЕ сторно
                &&  operation.isFan()                                           // веер
                &&  operation.isInterFilial()                                   // межфилиал
                &&  operation.isExchangeDifferenceA();                           // с курсовой разницей
    }

    @Override
    public GLOperation.OperType getOperationType() {
        return GLOperation.OperType.ME;
    }

    @Override
    public void setSpecificParameters(GLOperation operation) throws Exception {
        fanMfoOperationProcessor.setSpecificParameters(operation);
        mfoExchOperationProcessor.setMfoExchange(operation);
    }

    @Override
    public void addPosting(GLOperation operation, List<GLPosting> postList) throws Exception {
        fanMfoOperationProcessor.addFeatherMfoPosting(operation, postList);
        postList.add(fanMfoOperationProcessor.createMfoTargetPosting(operation));
        postList.add(createExchangeMfoPosting(operation));
    }

    /**
     * проводка по курсовой разнице в основном филиале
     */
    private GLPosting createExchangeMfoPosting(GLOperation operation) {
        String accDt, accCt;
        if (C == operation.getFpSide()) {
            accDt = operation.getAccountDebit(); accCt = ifEmpty(operation.getAccountLiability(), operation.getAccountCredit());
        } else {
            accDt = ifEmpty(operation.getAccountAsset(), operation.getAccountDebit()); accCt = operation.getAccountCredit();
        }
        return postingRepository.createExchPosting(operation
                , GLPosting.PostingType.ExchDiff
                , accDt, operation.getCurrencyDebit()
                , accCt, operation.getCurrencyCredit());
    }

    private String ifEmpty(String target, String defaultString) {
        return StringUtils.isEmpty(target) ? defaultString : target;
    }
}
