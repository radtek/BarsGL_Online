package ru.rbt.barsgl.ejb.integr.fan;

import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.repository.GLPostingRepository;
import ru.rbt.shared.Assert;

import javax.inject.Inject;
import java.util.List;

import static com.google.common.collect.Iterables.find;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;

/**
 * Created by ER18837 on 10.04.15.
 */
public class FanSimpleOperationProcessor extends FanOperationProcessor {

    @Inject
    private GLPostingRepository glPostingRepository;

    @Override
    public boolean isSupported(GLOperation operation) {
        return     !operation.isStorno()                                        // не сторно
                &&  operation.isFan()                                           // не веер
                && !operation.isInterFilial()                                   // филиал один
                && !operation.isExchangeDifferenceA();                           // нет курсовой разницы
    }

    @Override
    public GLOperation.OperType getOperationType() {
        return GLOperation.OperType.S;
    }

    @Override
    public void setSpecificParameters(GLOperation operation) {

    }

    @Override
    public void addPosting(GLOperation operation, List<GLPosting> postList) throws Exception {
        addSimpleFeatherPosting(operation, postList);

    }

    /**
     * Создает 1 полупроводку ("перо") в одном филиале с одной валютой
     * @param operation
     * @return
     * @throws Exception
     */
    public void addSimpleFeatherPosting(GLOperation operation, List<GLPosting> postList) {
        GLPosting fanPosting = find(postList
                , input -> !isEmpty(input.getPostType())
                && input.getPostType().equalsIgnoreCase(GLPosting.PostingType.FanMain.getValue()), null);
        Assert.notNull(fanPosting, "Не удалось найти основную проводку веера");
        // создаем проводку
        glPostingRepository.addPostingPdWithSkip(
                operation, fanPosting, operation.getFpSide(),
                GLPosting.PostingType.OneFilial,
                operation.getAccountDebit(), operation.getCurrencyDebit(), operation.getAmountDebit(),
                operation.getAccountCredit(), operation.getCurrencyCredit(), operation.getAmountCredit(),
                operation.getAmountPosting());
    }
}
