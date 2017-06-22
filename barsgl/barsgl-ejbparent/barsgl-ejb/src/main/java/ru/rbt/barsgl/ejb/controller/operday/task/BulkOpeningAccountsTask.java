/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2017
 */
package ru.rbt.barsgl.ejb.controller.operday.task;

import static java.lang.String.format;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.persistence.NonUniqueResultException;
import org.apache.log4j.Logger;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountController;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountService;
import ru.rbt.barsgl.ejb.repository.GLAccountRepository;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.shared.Assert;

import static ru.rbt.audit.entity.AuditRecord.LogCode.Account;
import static ru.rbt.audit.entity.AuditRecord.LogCode.BulkOpeningAccountsTask;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;

/**
 *
 * @author Andrew Samsonov
 */
/*
insert into gl_sched (TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR)
values('BulkOpeningAccountsTask', null, 'Массовое открытие счетов', 'STOPPED', 'MANUAL', 'SINGLE', 'ru.rbt.barsgl.ejb.controller.operday.task.BulkOpeningAccountsTask', 0, null, null);
 */
public class BulkOpeningAccountsTask implements ParamsAwareRunnable {

    private static final Logger log = Logger.getLogger(BulkOpeningAccountsTask.class);

    public enum GlOpenaccStatus {
        NEW, OK, ERROR
    };

    @EJB
    private AuditController auditController;

    @EJB
    private GLAccountRepository accountRepository;

    @EJB
    private GLAccountController accountController;

    @EJB
    private GLAccountService accountService;

    private List<DataRecord> listImbcbbrpAll;
    
    @Override
    public void run(String jobName, Properties properties) throws Exception {
        bulkOpeningAccounts(properties);
    }

    private void bulkOpeningAccounts(Properties properties) throws Exception {
        try {
            List<DataRecord> list = getAccount();

            /*
            int batchSize = 100;
            for (int i = 0; i < list.size(); i += batchSize) {
                List<DataRecord> subList = list.subList(i, Math.min(i + batchSize, list.size()));
            }
            */
            //Set<String> accounts = new HashSet<>();
            
            list.forEach(item -> {
                Long id = item.getLong("ID");
                String cNum = item.getString("CNUM");
                String branch = item.getString("BRANCH");

                try {
                    accountRepository.executeInNewTransaction(jac -> {
                        if (cNum != null && !cNum.isEmpty()) {
                            String cCode = getCCode(branch);
                            //accounts.add(loadAccount(item, cNum, branch, cCode));
                            loadAccount(item, cNum, branch, cCode);
                        } else {
                            List<DataRecord> listImbcbbrp = getImbcbbrpList(branch);

                            if (listImbcbbrp != null) {
                                listImbcbbrp.forEach(imbcbbrp -> {
                                    String imbCNum = imbcbbrp.getString("A8BICN");
                                    String imbBranch = imbcbbrp.getString("A8BRCD");
                                    String cCode = imbcbbrp.getString("BCBBR");
                                    try {
                                        //accounts.add(loadAccount(item, imbCNum, imbBranch, cCode));
                                        loadAccount(item, imbCNum, imbBranch, cCode);
                                    } catch (Exception ex) {
                                        throw new BreakException(ex);
                                    }
                                });
                            }
                        }
                        createBulkOpeningAccountResult(id, GlOpenaccStatus.OK);
                        return null;
                    });
                } catch (Throwable ex) {
                    createBulkOpeningAccountResult(id, GlOpenaccStatus.ERROR);
                    auditController.error(BulkOpeningAccountsTask, "Ошибка при массовом открытии счетов", null, ex);
                    log.error(null, ex);
                }
            });
        } catch (Exception ex) {
            auditController.error(BulkOpeningAccountsTask, "Ошибка при массовом открытии счетов", null, ex);
            throw ex;
        }
    }

    private List<DataRecord> getImbcbbrpListAll() throws SQLException{
        String sqlSelect = "SELECT BRP.A8BICN, BRP.A8BRCD, BRP.BCBBR, BRP.A8CMCD FROM IMBCBBRP BRP, IMBCBCMP CMP WHERE BRP.BCBBR = CMP.CCBBR AND CMP.CCPRI <> 'N'";
        return accountRepository.select(sqlSelect, new Object[]{});
    }
    
    private List<DataRecord> getImbcbbrpList(String branch) throws SQLException{
        if(listImbcbbrpAll == null)
            listImbcbbrpAll =  getImbcbbrpListAll();
        
        if ("ALL".equalsIgnoreCase(branch)) {
            return listImbcbbrpAll;
        }else{
            return listImbcbbrpAll.stream().filter(p -> branch.equals(p.getString("A8CMCD"))).collect(Collectors.toList());        
        }
    }
    
    private List<DataRecord> getListImbcbbrp(String branch) throws SQLException {
        String sqlSelect = "SELECT BRP.A8BICN, BRP.A8BRCD, BRP.BCBBR FROM IMBCBBRP BRP, IMBCBCMP CMP WHERE BRP.BCBBR = CMP.CCBBR AND CMP.CCPRI <> 'N' ";

        if ("ALL".equalsIgnoreCase(branch)) {
            return accountRepository.select(sqlSelect,
                    new Object[]{});
        } else {
            return accountRepository.select(sqlSelect
                    + " AND BRP.A8CMCD=?", new Object[]{branch});
        }
    }

    private String getCCode(String branch) throws Exception {
        List<DataRecord> list = accountRepository.select("SELECT BCBBR FROM IMBCBBRP WHERE A8BRCD=?", new Object[]{branch});
        if (list.isEmpty()) {
            throw new Exception("Код филиала для " + branch + " не найден");
        } else {
            return list.get(0).getString(0);
        }
    }

    public List<DataRecord> getAccount(Object... args) throws SQLException {
        List<DataRecord> dataRecords = accountRepository.selectMaxRows(
                "SELECT A.* FROM GL_OPENACC A WHERE A.STATUS = '" + GlOpenaccStatus.NEW.name() + "'"//+ "WHERE CLAUSE"
                ,
                 Integer.MAX_VALUE, null);
        return dataRecords;
    }

    private String createAcId(String cNum, String branch, DataRecord item) {
        // 8+3+4+2+3=20
        String acId = cNum
                + item.getString("CCY")
                + item.getString("ACOD")
                + item.getString("SQ")
                + branch;
        Assert.isTrue(20 == acId.length(), format("Неверная длина счета Майдас '%s' размер '%d'", acId, acId.length()));
        return acId;
    }

    private String loadAccount(DataRecord item, String cNum, String branch, String cCode) throws Exception {
        String acId = createAcId(cNum, branch, item);
        //@@@ ??? createTmp(item, vAcid, cCode); 

        GLAccount account = getGLAccount(acId);
        if (account != null) {
            try {
                updateAccount(account, item);
            } catch (Exception ex) {
                auditController.error(Account, format("Ошибка при изменении счета для acid = '%s'", acId), null, ex);
                throw ex;
            }
        } else {
            createBulkOpeningAccount(item, cNum, branch, cCode, acId);
        }

        return acId;
    }

    private GLAccount getGLAccount(String acId) {
        List<GLAccount> list = accountRepository.select(GLAccount.class, "from GLAccount a where a.acid=?1 and a.dateClose is null", acId);
        if (1 < list.size()) {
            throw new NonUniqueResultException("Found more than one entity on query for acid " + acId);
        } else if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    private List<DataRecord> getGLAccounts(Set<String> acids) throws Exception {
        try {
            String acidsStr = "'" + StringUtils.listToString(acids, "','") + "'";
            List<DataRecord> dataRecords = accountRepository.selectMaxRows(
                    "SELECT * FROM GL_ACC A WHERE "
                    + "A.ACID IN (" + acidsStr + ") "
                    + "AND A.DTC IS NULL ",
                    Integer.MAX_VALUE, null);
            return dataRecords;
        } catch (SQLException e) {
            throw new Exception(e);
        }
    }

    private DataRecord getNativeGLAccount(String acId) throws Exception {
        return accountRepository.selectFirst(
                "SELECT * FROM GL_ACC A WHERE "
                + "A.ACID = ? "
                + "AND A.DTC IS NULL ",
                new Object[]{acId});
    }

    private void updateAccount(GLAccount account, DataRecord item) {
        account.setDealId(item.getString("DEALID"));
        account.setSubDealId(item.getString("SUBDEALID"));
        account.setDescription(item.getString("DESCRIPTION"));

        accountRepository.update(account);
    }

    private GLAccount createBulkOpeningAccount(DataRecord dataRecord, String cNum, String branch, String cCode, String acId) throws Exception {
        Date dateOpen = dataRecord.getDate("DTO");
        //AccountKeys keys = createBulkOpeningAccountKeys(dataRecord, cNum, branch, cCode, acId, dateOpen);

        //return accountService.createBulkOpeningAccount(keys, dateOpen);
        ManualAccountWrapper wrapper = new ManualAccountWrapper();

        wrapper.setAccountType(Long.parseLong(dataRecord.getString("ACCTYPE")));
        wrapper.setBalanceAccount2(dataRecord.getString("ACC2"));
        wrapper.setCurrency(dataRecord.getString("CCY"));
        wrapper.setAccountCode(Short.parseShort(dataRecord.getString("ACOD")));
        wrapper.setAccountSequence(Short.parseShort(dataRecord.getString("SQ")));

        wrapper.setBranch(branch);
        wrapper.setCompanyCode(cCode);
        wrapper.setCustomerNumber(cNum);
        wrapper.setAcid(acId);

        wrapper.setDealId(dataRecord.getString("DEALID"));
        wrapper.setSubDealId(dataRecord.getString("SUBDEALID"));
        wrapper.setDescription(dataRecord.getString("DESCRIPTION"));

        wrapper.setDateOpenStr(new SimpleDateFormat(wrapper.dateFormat).format(dateOpen));

        //UCBCA	-CA	Касса	N
        wrapper.setDealSource("UCBCA");
        return accountService.createBulkOpeningAccount(wrapper);
    }

    private AccountKeys createBulkOpeningAccountKeys(DataRecord dataRecord, String cNum, String branch, String cCode, String acId, Date dateOpen) {
        ManualAccountWrapper wrapper = new ManualAccountWrapper();

        wrapper.setAccountType(Long.parseLong(dataRecord.getString("ACCTYPE")));
        wrapper.setBalanceAccount2(dataRecord.getString("ACC2"));
        wrapper.setCurrency(dataRecord.getString("CCY"));
        wrapper.setAccountCode(Short.parseShort(dataRecord.getString("ACOD")));
        wrapper.setAccountSequence(Short.parseShort(dataRecord.getString("SQ")));

        wrapper.setBranch(branch);
        wrapper.setCompanyCode(cCode);
        wrapper.setCustomerNumber(cNum);
        wrapper.setAcid(acId);

        wrapper.setDealId(dataRecord.getString("DEALID"));
        wrapper.setSubDealId(dataRecord.getString("SUBDEALID"));
        wrapper.setDescription(dataRecord.getString("DESCRIPTION"));

        AccountKeys keys = accountController.createWrapperAccountKeys(wrapper, dateOpen);

        keys.setAccountCode(wrapper.getAccountCode().toString());
        keys.setAccountMidas(acId);

        return keys;
    }

    private void createBulkOpeningAccountResult(Long openAccId, GlOpenaccStatus status) {
        try {
            accountRepository.executeInNewTransaction(persistence -> {
                accountRepository.executeNativeUpdate("update gl_openacc set status = ? where id = ?", status.name(), BigDecimal.valueOf(openAccId) );
                return null;
            });
        } catch (Exception ex) {
            auditController.error(Account, format("Ошибка при изменении статуса записи gl_openacc для id = '%s'", openAccId), null, ex);
        }
    }

    private class BreakException extends RuntimeException {

        private BreakException(Exception ex) {
            super(ex);
        }

    }
}
