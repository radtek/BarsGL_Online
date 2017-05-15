package ru.rbt.barsgl.ejb.integr.pst;

import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.repository.BankCurrencyRepository;
import ru.rbt.barsgl.ejb.repository.GLPostingRepository;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ER18837 on 05.03.15.
 */
public class ExchOperationProcessor extends GLOperationProcessor {

    @Inject
    private GLPostingRepository glPostingRepository;

    @Inject
    private BankCurrencyRepository bankCurrencyRepository;

    @Override
    public boolean isSupported(GLOperation operation) {
        return     !operation.isFan()                                           // не веер
                && !operation.isStorno()                                        // не сторно
                && !operation.isInterFilial()                                   // филиал один
                &&  operation.isExchangeDifferenceA()                          // есть курсовая разница (глава А)
                && !operation.isTech();                                        //прзнак операции по техническим счетам
    }

    @Override
    public GLOperation.OperType getOperationType() {
        return GLOperation.OperType.E;
    }

    @Override
    public void setSpecificParameters(GLOperation operation) throws Exception {
        setAccountExchange(operation, operation.getAccountDebit());
    }

    /**
     * Создает 2 проводки (4 полупроводки) в одном филиале с разной валютой
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

        // основная проводка
        GLPosting postingMain = glPostingRepository.createPosting(operation, GLPosting.PostingType.OneFilial,
                bsaAcidDebit, ccyDebit, operation.getAmountDebit(),
                bsaAcidCredit, ccyCredit, operation.getAmountCredit(),
                operation.getAmountPosting());

        // курсовая разница
        GLPosting postingExch = glPostingRepository.createExchPosting(operation, GLPosting.PostingType.ExchDiff,
                bsaAcidDebit, ccyDebit,
                bsaAcidCredit, ccyCredit);

        List<GLPosting> pstList = new ArrayList<GLPosting>(2);
        pstList.add(postingMain);
        pstList.add(postingExch);

        return pstList;
    }
}
