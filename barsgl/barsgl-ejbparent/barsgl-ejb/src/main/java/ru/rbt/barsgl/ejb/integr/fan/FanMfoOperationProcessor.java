package ru.rbt.barsgl.ejb.integr.fan;

import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.repository.GLPostingRepository;
import ru.rbt.barsgl.ejb.repository.PdRepository;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import static com.google.common.collect.Iterables.find;
import static ru.rbt.barsgl.ejb.entity.gl.GLOperation.OperSide.C;
import static ru.rbt.barsgl.ejb.entity.gl.GLPosting.PostingType.*;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;
import static ru.rbt.ejbcore.validation.ErrorCode.MFO_ACCOUNT_NOT_FOUND;

/**
 * Created by Ivan Sevastyanov
 * Обработчик для частичных операций веера с признаками МФО
 */
public class FanMfoOperationProcessor extends FanOperationProcessor {

    @Inject
    private GLPostingRepository glPostingRepository;

    @EJB
    private PdRepository pdRepository;

    @Override
    public boolean isSupported(GLOperation operation) {
        return     !operation.isStorno()                                        // НЕ сторно
                &&  operation.isFan()                                           // веер
                &&  operation.isInterFilial()                                   // межфилиал
                && !operation.isExchangeDifferenceA();                           // НЕТ курсовой разницы
    }

    @Override
    public GLOperation.OperType getOperationType() {
        return GLOperation.OperType.M;
    }

    @Override
    public void setSpecificParameters(GLOperation operation) throws Exception {
        // для веерных МФО алгоритм определения параметров другой чем для простых проводок МФО
        BankCurrency ccyMfo;
        if (C == operation.getFpSide()) {
            ccyMfo = operation.getCurrencyCredit();
        } else {
            ccyMfo = operation.getCurrencyDebit();
        }

        boolean isClients = glPostingRepository.isMfoClientPosting(operation.getAccountDebit(),
                operation.getAccountCredit());
        String[] mfoAccounts = glPostingRepository.getMfoAccounts(ccyMfo
                , operation.getFilialDebit(), operation.getFilialCredit(), isClients);
        if (null == mfoAccounts) {      // нет нужных счетов
            throw new ValidationError( MFO_ACCOUNT_NOT_FOUND,
                    ccyMfo.getCurrencyCode(), operation.getFilialDebit(), operation.getFilialCredit());
        }

        operation.setCurrencyMfo(ccyMfo);
        operation.setAccountLiability(mfoAccounts[0]);
        operation.setAccountAsset(mfoAccounts[1]);
    }

    @Override
    public void addPosting(GLOperation operation, List<GLPosting> postList) throws Exception {
        addFeatherMfoPosting(operation, postList);
        postList.add(createMfoTargetPosting(operation));
    }

    /**
     * полупроводка на межфилиальный счет
    */
    public void addFeatherMfoPosting(GLOperation operation, List<GLPosting> postList) {
        // полупроводка привязана к основной проводке веера
        final GLPosting fanMainPosting = find(postList
                , input -> !isEmpty(input.getPostType())
                && input.getPostType().equalsIgnoreCase(FanMain.getValue()), null);
        Assert.notNull(fanMainPosting, "Не удалось найти основную проводку веера");
        // создаем проводку перо вместо целевого счета в другом филиале подставляем счета MFO
        String accDt; String accCt; BigDecimal amntDt; BigDecimal amntCt;
        BigDecimal equiv; GLPosting.PostingType postingType;
        if (operation.getFpSide() == C) {
            postingType = null != operation.getAccountLiability() ? MfoDebit : FanMain;
            accDt = operation.getAccountDebit();
            accCt = null != operation.getAccountLiability() ? operation.getAccountLiability() : operation.getAccountCredit();
            equiv = operation.getEquivalentCreditRu();
            amntDt = null; amntCt = operation.getAmountCredit();
        } else {
            postingType = null != operation.getAccountAsset() ? MfoDebit : FanMain;
            accDt = null != operation.getAccountAsset() ? operation.getAccountAsset() : operation.getAccountDebit();
            accCt = operation.getAccountCredit();
            equiv = operation.getEquivalentDebitRu();
            amntDt = operation.getAmountDebit(); amntCt = null;
        }
        glPostingRepository.addPostingPdWithSkip(
                operation, fanMainPosting, operation.getFpSide(), postingType,
                accDt, operation.getCurrencyDebit(), amntDt,
                accCt, operation.getCurrencyCredit(), amntCt, equiv);
    }

    /**
     нормальная проводка на целевой НЕ ОСНОВНОЙ счет веера (частичной операции)
    */
    public GLPosting createMfoTargetPosting(GLOperation operation) throws SQLException {
        String accDt; String accCt;
        BigDecimal amnDt; BigDecimal amnCt; BigDecimal equiv; BankCurrency ccy;
        if (operation.getFpSide() == C) {
            accDt = operation.getAccountAsset(); accCt = operation.getAccountCredit();
            amnDt = operation.getAmountCredit(); amnCt = operation.getAmountCredit(); equiv = operation.getEquivalentCreditRu();
            ccy = operation.getCurrencyCredit();
        } else {
            accDt = operation.getAccountDebit(); accCt = operation.getAccountLiability();
            amnDt = operation.getAmountDebit(); amnCt = operation.getAmountDebit(); equiv = operation.getEquivalentDebitRu();
            ccy = operation.getCurrencyDebit();
        }
        return glPostingRepository.createPosting(operation, OneFilial
                , accDt, ccy, amnDt, accCt, ccy, amnCt, equiv);
    }

}
