package ru.rbt.barsgl.ejb.integr.pst;

import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.repository.BankCurrencyRepository;
import ru.rbt.barsgl.ejb.repository.GLPostingRepository;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ER18837 on 13.03.15.
 * Обрабатывает межфилиальные операции с НЕнулевой курсовой разницей
 */
public class MfoExchOperationProcessor extends GLOperationProcessor {

    @Inject
    private GLPostingRepository glPostingRepository;

    @Inject
    private BankCurrencyRepository bankCurrencyRepository;

    @Override
    public boolean isSupported(GLOperation operation) {
        return     !operation.isFan()                                           // не веер
                && !operation.isStorno()                                        // не сторно
                &&  operation.isInterFilial()                                   // филиалы разные
                &&  operation.isExchangeDifferenceA();                           // есть курсовая разница
    }

    @Override
    public GLOperation.OperType getOperationType() {
        return GLOperation.OperType.ME;
    }

    @Override
    public void setSpecificParameters(GLOperation operation) throws Exception {
        setMfoParameters(operation);
        setMfoExchange(operation);
    }

    /**
     *     Bug_CCYExch+Interbanch v0.01.doc
     * Общая логика установки счета отвода курсовой разницы в случае МФР
     * @param operation
     * @throws Exception
     */
    public void setMfoExchange(GLOperation operation) throws Exception {
        // счет, в филиале которого будет списываться курсовая разница
        String bsaAcidExch = operation.getCurrencyDebit().equals(operation.getCurrencyMfo()) ?
                operation.getAccountCredit() :
                operation.getAccountDebit();
        setAccountExchange(operation, bsaAcidExch);
//        setMfoAccountExchange(operation, bsaAcidExch);
    }

    @Override
    public List<GLPosting> createPosting(GLOperation operation) throws Exception {
        String bsaAcidDebit = operation.getAccountDebit();
        BankCurrency ccyDebit = operation.getCurrencyDebit();
        BigDecimal amountDebit = operation.getAmountDebit();

        String bsaAcidCredit = operation.getAccountCredit();
        BankCurrency ccyCredit = operation.getCurrencyCredit();
        BigDecimal amountCredit = operation.getAmountCredit();

        GLPosting postingLiab, postingAsst, postingExch;
        if (operation.getCurrencyMfo().equals(ccyDebit)) {  // конвертация в филиале кредита
            // проводка в Филиале дебета
            postingLiab = glPostingRepository.createPosting(operation, GLPosting.PostingType.MfoDebit,
                    bsaAcidDebit, ccyDebit, operation.getAmountDebit(),
                    operation.getAccountLiability(), ccyDebit, amountDebit,
                    operation.getEquivalentDebit());

            // проводка в Филиале кредита основная
            postingAsst = glPostingRepository.createPosting(operation, GLPosting.PostingType.MfoCredit,
                    operation.getAccountAsset(), ccyDebit, amountDebit,
                    bsaAcidCredit, ccyCredit, amountCredit,
                    operation.getAmountPosting());

            // проводка в Филиале кредита по курсовой разнице
            postingExch = glPostingRepository.createExchPosting(operation, GLPosting.PostingType.ExchDiff,
                    operation.getAccountAsset(), ccyDebit,
                    bsaAcidCredit, ccyCredit);

        } else { // конвертация в филиале деьета
            // проводка в Филиале дебета основная
            postingLiab = glPostingRepository.createPosting(operation, GLPosting.PostingType.MfoDebit,
                    bsaAcidDebit, ccyDebit, amountDebit,
                    operation.getAccountLiability(), ccyCredit, amountCredit,
                    operation.getAmountPosting());

            // проводка в Филиале дебета по курсовой разнице
            postingExch = glPostingRepository.createExchPosting(operation, GLPosting.PostingType.ExchDiff,
                    bsaAcidDebit, ccyDebit,
                    operation.getAccountLiability(), ccyCredit);

            // проводка в Филиале кредита
            postingAsst = glPostingRepository.createPosting(operation, GLPosting.PostingType.MfoCredit,
                    operation.getAccountAsset(), ccyCredit, amountCredit,
                    bsaAcidCredit, ccyCredit, amountCredit,
                    operation.getAmountPosting());

        }

        List<GLPosting> pstList = new ArrayList<GLPosting>(3);
        pstList.add(postingLiab);
        pstList.add(postingAsst);
        pstList.add(postingExch);

        return pstList;
    }
}
