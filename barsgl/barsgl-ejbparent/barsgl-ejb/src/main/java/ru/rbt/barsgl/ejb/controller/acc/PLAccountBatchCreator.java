package ru.rbt.barsgl.ejb.controller.acc;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeysBuilder;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchRequest;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountController;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountProcessor;
import ru.rbt.barsgl.ejb.repository.GLAccountRepository;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.shared.ErrorList;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.util.StringUtils;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.SQLException;
import java.util.Date;
import java.util.Optional;

import static ru.rbt.barsgl.ejb.entity.acc.GLAccount.RelationType.FIVE;
import static ru.rbt.barsgl.ejb.entity.acc.GLAccount.RelationType.TWO;
import static ru.rbt.barsgl.ejb.entity.gl.GLOperation.OperSide.N;
import static ru.rbt.ejbcore.mapping.YesNo.Y;
import static ru.rbt.ejbcore.util.StringUtils.ifEmpty;

/**
 * Created by Ivan Sevastyanov on 31.10.2018.
 */
public class PLAccountBatchCreator implements AccountBatchCreator {

    @EJB
    private GLAccountController accountController;

    @EJB
    private OperdayController operdayController;

    @EJB
    private CoreRepository repository;

    @Inject
    private GLAccountProcessor accountProcessor;

    @EJB
    private GLAccountRepository accountRepository;

    @Override
    public Optional<GLAccount> find(AccountBatchRequest request) {
        AccountKeys keys = fromRequest(request);
        return Optional.ofNullable(accountController
                .findGLPLAccountMnl(keys, calcOpenDate(request)));
    }

    @Override
    public GLAccount createAccount(AccountBatchRequest request) throws Exception {
        return (GLAccount) repository.executeInNewTransaction(persistence -> {
            AccountKeys keys = fromRequest(request);
            accountProcessor.fillAccountKeys(null, null, keys);
            int cnum = (int) accountProcessor.stringToLong(N, "Customer number", keys.getCustomerNumber(), AccountKeys.getiCustomerNumber());
            short iacod = (short) accountProcessor.stringToLong(N, "ACOD Midas", keys.getAccountCode(), AccountKeys.getiAccountCode());
            short isq = (short) accountProcessor.stringToLong(N, "SQ Midas", keys.getAccSequence(), AccountKeys.getiAccSequence());
            String acid = accountRepository.makeMidasAccount(
                    cnum,
                    keys.getCurrency(),
                    keys.getBranch(),
                    iacod,
                    isq
            );
            keys.setAccountMidas(acid);
            return accountController.createGLPLAccountMnl(keys, calcRelationType(request), calcOpenDate(request), new ErrorList(), GLAccount.OpenType.XLS);
        });
    }

    private AccountKeys fromRequest(AccountBatchRequest request) {
        return AccountKeysBuilder.create()
                .withBranch(request.getInBranch())
                .withCurrency(request.getInCcy())
                .withCustomerNumber(request.getInCustno())
                .withAccountType(request.getInAcctype())
                .withCustomerType(request.getCalcCtypeAcc())
                .withTerm(ifEmpty(request.getInTerm(), "0"))
                .withAcc2(request.getCalcAcc2Parm())
                .withAccountCode(request.getCalcAcodParm())
                .withAccSequence(request.getCalcAcsqParm())
                .withFilial(request.getCalcCbcc())
                .withPlCode(request.getCalcPlcodeParm())
                .withDealSource(request.getInDealsrc())
                .build();
    }

    private GLAccount.RelationType calcRelationType(AccountBatchRequest request) {
        try {
            DataRecord actname = repository.selectFirst("Select * from gl_actname where acctype = ?", request.getInAcctype());
            return !StringUtils.isEmpty(actname.getString("pl_act")) && YesNo.valueOf(actname.getString("pl_act")) == Y ? FIVE : TWO;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private Date calcOpenDate(AccountBatchRequest request) {
        return null != request.getInOpendate() ? request.getInOpendate() : operdayController.getOperday().getCurrentDate();
    }

}
