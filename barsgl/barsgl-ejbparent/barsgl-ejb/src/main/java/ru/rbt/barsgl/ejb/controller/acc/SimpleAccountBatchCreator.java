package ru.rbt.barsgl.ejb.controller.acc;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeysBuilder;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchRequest;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountController;
import ru.rbt.barsgl.shared.ErrorList;

import javax.ejb.EJB;
import java.util.Optional;

import static ru.rbt.ejbcore.util.StringUtils.ifEmpty;

/**
 * Created by Ivan Sevastyanov on 31.10.2018.
 */
public class SimpleAccountBatchCreator implements AccountBatchCreator {

    @EJB
    private GLAccountController accountController;

    @EJB
    private OperdayController operdayController;

    @Override
    public Optional<GLAccount> find(AccountBatchRequest request) {
        AccountKeys keys = fromRequest(request);
        return Optional.ofNullable(accountController.findGLAccountMnl(keys, request.getDtOpen()));
    }

    @Override
    public GLAccount createAccount(AccountBatchRequest request) throws Exception {
        AccountKeys keys = fromRequest(request);
        return accountController.createGLAccountMnl(keys, Optional.ofNullable(request.getInOpendate())
                .orElseGet(() -> operdayController.getOperday().getCurrentDate()), new ErrorList(), GLAccount.OpenType.XLS);
    }

    private AccountKeys fromRequest(AccountBatchRequest request) {
        return AccountKeysBuilder.create()
                .withBranch(request.getInBranch())
                .withCurrency(request.getInCcy())
                .withCustomerNumber(request.getInCustno())
                .withAccountType(request.getInAcctype())
                .withCustomerType(request.getCalcCtypeAcc())
                .withTerm(ifEmpty(request.getInTerm(), "0"))
                .withDealSource(request.getInDealsrc())
                .withDealId(request.getInDealid())
                .withSubDealId(request.getInSubdealid())
                .withAcc2(request.getCalcAcc2Parm())
                .withFilial(request.getCalcCbcc())
                .build();
    }
}
