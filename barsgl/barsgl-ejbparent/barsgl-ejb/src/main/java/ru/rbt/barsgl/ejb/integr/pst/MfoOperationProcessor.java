package ru.rbt.barsgl.ejb.integr.pst;

import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.repository.GLPostingRepository;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ER18837 on 26.02.15.
 * Обрабатывает межфилиальные операции с нулевой курсовой разницей
 */
public class MfoOperationProcessor extends GLOperationProcessor {

    @Inject
    private GLPostingRepository glPostingRepository;

    @Override
    public boolean isSupported(GLOperation operation) {
        return     !operation.isFan()                                           // не веер
                && !operation.isStorno()                                        // не сторно
                &&  operation.isInterFilial()                                   // филиалы разные
                && !operation.isExchangeDifferenceA();                           // нет курсовой разницы
    }

    @Override
    public GLOperation.OperType getOperationType() {
        return GLOperation.OperType.M;
    }

    @Override
    public void setSpecificParameters(GLOperation operation) throws Exception {
        setMfoParameters(operation);
    }

    /**
     * Создает 2 проводки (4 полупроводки) в разных филиале с одной валютой
     * @param operation
     * @return
     * @throws Exception
     */
    @Override
    public List<GLPosting> createPosting(GLOperation operation) throws Exception {
        String bsaAcidDebit = operation.getAccountDebit();
        BankCurrency ccyDebit = operation.getCurrencyDebit();
        String bsaAcidCredit = operation.getAccountCredit();
        BankCurrency ccyCredit = operation.getCurrencyCredit();

        // определить межфилиальные параметры
        BankCurrency ccyMfo = operation.getCurrencyMfo();
        boolean isMfoDebit = ccyMfo.equals(ccyDebit);
        BigDecimal amountMfo = isMfoDebit ? operation.getAmountDebit() : operation.getAmountCredit();
        BigDecimal equivalentMfo = isMfoDebit ? operation.getEquivalentDebitRu() : operation.getEquivalentCreditRu();

        // проводка по Филиалу дебета
        GLPosting postingLiab = glPostingRepository.createPosting(operation, GLPosting.PostingType.MfoDebit,
                bsaAcidDebit, ccyDebit, operation.getAmountDebit(),
                operation.getAccountLiability(), ccyMfo, amountMfo, equivalentMfo);

        // проводка по Филиалу кредита
        GLPosting postingAsst = glPostingRepository.createPosting(operation, GLPosting.PostingType.MfoCredit,
                operation.getAccountAsset(), ccyMfo, amountMfo,
                bsaAcidCredit, ccyCredit, operation.getAmountCredit(), equivalentMfo);

        List<GLPosting> pstList = new ArrayList<GLPosting>(2);
        pstList.add(postingLiab);
        pstList.add(postingAsst);

        return pstList;
    }
}
