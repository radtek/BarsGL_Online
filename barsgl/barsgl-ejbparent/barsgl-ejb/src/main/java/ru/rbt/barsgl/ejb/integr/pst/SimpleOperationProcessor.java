package ru.rbt.barsgl.ejb.integr.pst;

import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.repository.GLPostingRepository;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ivan Sevastyanov
 * Обрабатывает проводки внутри одного филиала при отсутствии курсовой разницы
 */
public class SimpleOperationProcessor extends GLOperationProcessor {

    @Inject
    private GLPostingRepository glPostingRepository;

    @Override
    public boolean isSupported(GLOperation operation) {
        return     !operation.isFan()                                           // не веер
                && !operation.isStorno()                                        // не сторно
                && !operation.isInterFilial()                                   // филиал один
                && !operation.isExchangeDifferenceA();                          // нет курсовой разницы или не глава А
    }

    @Override
    public GLOperation.OperType getOperationType() {
        return GLOperation.OperType.S;
    }

    @Override
    public void setSpecificParameters(GLOperation operation) {

    }

    /**
     * Создает 1 проводку (2 полупроводки) в одном филиале с одной валютой
     * @param operation
     * @return
     * @throws Exception
     */
    @Override
    public List<GLPosting> createPosting(GLOperation operation) throws Exception {

        // создаем проводку
        GLPosting posting = glPostingRepository.createPosting(operation, GLPosting.PostingType.OneFilial,
                operation.getAccountDebit(), operation.getCurrencyDebit(), operation.getAmountDebit(),
                operation.getAccountCredit(), operation.getCurrencyCredit(), operation.getAmountCredit(),
                operation.getAmountPosting());

        List<GLPosting> pstList = new ArrayList<GLPosting>();
        pstList.add(posting);

        return pstList;

    }

}
