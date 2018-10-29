package ru.rbt.barsgl.ejbtesting.test;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeysBuilder;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.acc.GLAccountRequest;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountController;
import ru.rbt.barsgl.ejb.repository.GLAccountRepository;
import ru.rbt.barsgl.ejbcore.AsyncProcessor;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.shared.ErrorList;
import ru.rbt.ejbcore.util.StringUtils;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Ivan Sevastyanov on 11.10.2018.
 */
public class AccountSrvConcurrentTesting {

    @EJB
    private CoreRepository repository;

    @Inject
    private AsyncProcessor asyncProcessor;

    @Inject
    private GLAccountController accountController;

    @Inject
    private OperdayController operdayController;

    @Inject
    private GLAccountRepository accountRepository;
//    GLAccountRepository.getMidasBranchByFlex()

    @Inject
    private AuditController auditController;

    public int testAll1(int poolSize, int rownum, int resentCount) throws Exception {
        return (int) repository.executeTransactionally((connection -> {
            int i = 0;
            ExecutorService executorService = asyncProcessor.getBlockingQueueThreadPoolExecutor(poolSize, poolSize, 2000*resentCount);
            try (PreparedStatement statement = connection.prepareStatement("select * from TMP_ACREQ where rownum <= ?")) {
                    statement.setInt(1, rownum);
                try (ResultSet resultSet = statement.executeQuery()){
                    while (resultSet.next()) {
                        AccountOpenRunnable1 runnable = new AccountOpenRunnable1(
                                resultSet.getString("REQUEST_ID")
                                , resultSet.getString("BRANCH")
                                , resultSet.getString("CCY")
                                , resultSet.getString("CUSTOMER_NO")
                                , resultSet.getString("ACCOUNTING_TYPE")
                                , resultSet.getString("CUSTOMER_CBTYPE")
                                , resultSet.getString("TERM")
                                , resultSet.getString("DEAL_ID")
                                , resultSet.getString("SUBDEAL_ID")
                        );
                        for (int j=0; j < resentCount; j++){
                            executorService.submit(runnable);
                        }
                        i++;
                    }
                }
            }
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.HOURS);
            return i;
        }));
    }

    private class AccountOpenRunnable1 implements Runnable {

        protected final String requestId;
        protected final String branch;
        protected final String ccy;
        protected final String customerNo;
        protected final String accountingType;
        protected final String customerType;
        protected final String term;
        protected final String dealId;
        protected final String subdealId;

        public AccountOpenRunnable1(String requestId, String branch, String ccy
                , String customerNo, String accountingType, String customerType
                , String term, String dealId, String subdealId) {
            this.requestId = requestId;
            this.branch = branch;
            this.ccy = ccy;
            this.customerNo = customerNo;
            this.accountingType = accountingType;
            this.customerType = customerType;
            this.term = term;
            this.dealId = dealId;
            this.subdealId = subdealId;
        }

        @Override
        public void run() {
            GLOperation operation = getOperation();
            try {
                GLAccount newAccount = (GLAccount) repository.executeInNewTransaction(persistence ->
                        accountController.createGLAccountAE(operation, GLOperation.OperSide.C
                       , operdayController.getOperday().getCurrentDate(),buildAccountKeys(), new ErrorList()));
                if (null != newAccount) {
                    repository.executeInNewTransaction(persistence -> {
                        repository.executeNativeUpdate("update TMP_ACREQ set new_bsaacid =? where request_id = ? and new_bsaacid is null"
                                , newAccount.getBsaAcid(), requestId);
                        return null;
                    });
                }
            } catch (Exception e) {
                auditController.error(AuditRecord.LogCode.Account, "Ошибка блин: " + e.getMessage(), null, e);
            }
        }

        protected final AccountKeys buildAccountKeys() throws SQLException {
            return AccountKeysBuilder.create()
                    .withBranch(getBranchNumber(branch))
                    .withCurrency(ccy)
                    .withCustomerNumber(customerNo)
                    .withAccountType(accountingType)
                    .withCustomerType(customerType)
                    .withTerm(term)
                    .withDealId(dealId)
                    .withSubDealId(subdealId)
                    .withGlSequence("0")
                    .withAccountType(accountingType).build();
        }

        protected final GLOperation getOperation() {
            return (GLOperation) repository.findById(GLOperation.class, 111769831L);
        }
    }

    private class AccountOpenRunnable2 extends AccountOpenRunnable1 {

        private final String dealRs;

        public AccountOpenRunnable2(String requestId, String branch, String ccy
                , String customerNo, String accountingType, String customerType
                , String term, String dealId, String subdealId, String dealRs) {
            super(requestId, branch, ccy, customerNo, accountingType, customerType, term, dealId, subdealId);
            this.dealRs = dealRs;
        }

        @Override
        public void run() {
            try {
                GLAccount newAccount = (GLAccount) repository.executeInNewTransaction(persistence ->
                {
                    GLAccountRequest request = new GLAccountRequest();
                    request.setBranchFlex(branch);
                    request.setCurrency(ccy);
                    request.setCustomerNumber(customerNo);
                    request.setAccountType(accountingType);
                    request.setCbCustomerType(customerType);
                    request.setTerm(term);
                    request.setDealSource(dealRs);
                    request.setDealId(dealId);
                    request.setSubDealId(subdealId);
                    request.setDateOpen(operdayController.getOperday().getCurrentDate());
                    request.setStatus(GLAccountRequest.RequestStatus.OK);
                    AccountKeys keys = accountController.createRequestAccountKeys(request, operdayController.getOperday().getCurrentDate());

                    return accountController.createGLAccountMnl(keys, operdayController.getOperday().getCurrentDate(), new ErrorList(), GLAccount.OpenType.MNL);
                });
                if (null != newAccount) {
                    repository.executeInNewTransaction(persistence -> {
                        repository.executeNativeUpdate("update TMP_ACREQ set new_bsaacid =? where request_id = ? and new_bsaacid is null"
                                , newAccount.getBsaAcid(), requestId);
                        return null;
                    });
                }
            } catch (Exception e) {
                auditController.error(AuditRecord.LogCode.Account, "Ошибка блин: " + e.getMessage(), null, e);
            }
        }
    }

    public int testAll2(int poolSize, int rownum, int resentCount) throws Exception {
        return (int) repository.executeTransactionally((connection -> {
            int i = 0;
            ExecutorService executorService = asyncProcessor.getBlockingQueueThreadPoolExecutor(poolSize, poolSize, 2000*resentCount);
            try (PreparedStatement statement = connection.prepareStatement("select * from TMP_ACREQ where rownum <= ?")) {
                    statement.setInt(1, rownum);
                try (ResultSet resultSet = statement.executeQuery()){
                    while (resultSet.next()) {
                        AccountOpenRunnable2 runnable = new AccountOpenRunnable2(
                                resultSet.getString("REQUEST_ID")
                                , resultSet.getString("BRANCH")
                                , resultSet.getString("CCY")
                                , resultSet.getString("CUSTOMER_NO")
                                , resultSet.getString("ACCOUNTING_TYPE")
                                , resultSet.getString("CUSTOMER_CBTYPE")
                                , resultSet.getString("TERM")
                                , resultSet.getString("DEAL_ID")
                                , resultSet.getString("SUBDEAL_ID")
                                , resultSet.getString("AC_DEALS_RS")
                        );
                        for (int j=0; j < resentCount; j++){
                            executorService.submit(runnable);
                        }
                        i++;
                    }
                }
            }
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.HOURS);
            return i;
        }));
    }

    private static final Map<String, String> branches = new ConcurrentHashMap<>();

    private String getBranchNumberInternal(String alfaCode) throws SQLException {
        String res = accountRepository.getMidasBranchByFlex(alfaCode);
        if (StringUtils.isEmpty(res)) {
            throw new RuntimeException("Branch '" + alfaCode + "' not found");
        } else {
            return res;
        }
    }

    private String getBranchNumber(String alfaCode) throws SQLException {
        if (null == branches.get(alfaCode)) {
            branches.putIfAbsent(alfaCode, getBranchNumberInternal(alfaCode));
        }
        return branches.get(alfaCode);
    }
}
