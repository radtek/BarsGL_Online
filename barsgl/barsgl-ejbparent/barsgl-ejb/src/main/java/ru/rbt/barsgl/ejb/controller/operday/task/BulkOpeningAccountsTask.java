/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2017
 */
package ru.rbt.barsgl.ejb.controller.operday.task;

import static java.lang.String.format;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.ejb.EJB;
import javax.persistence.NoResultException;
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

/**
 *
 * @author Andrew Samsonov
 */
public class BulkOpeningAccountsTask {

    private static final Logger log = Logger.getLogger(BulkOpeningAccountsTask.class);

    @EJB
    private AuditController auditController;

    @EJB
    private GLAccountRepository accountRepository;

    @EJB
    private GLAccountController accountController;

    @EJB
    private GLAccountService accountService;

    @EJB
    private CoreRepository repository;

    public void bulkOpeningAccounts(Object... args) throws Exception {
        List<DataRecord> list = getAccount(args);

        /*
        int batchSize = 100;
        for (int i = 0; i < list.size(); i += batchSize) {
            List<DataRecord> subList = list.subList(i, Math.min(i + batchSize, list.size()));
        }
         */
        //Set<String> accounts = new HashSet<>();
        list.forEach(item -> {
            String cNum = item.getString("CNUM");
            String branch = item.getString("BRANCH");
            try {
                if (cNum != null && !cNum.isEmpty()) {
                    String cCode = getCCode(branch);
                    //accounts.add(loadAccount(item, cNum, branch, cCode));
                    loadAccount(item, cNum, branch, cCode);
                } else {
                    List<DataRecord> listImbcbbrp = getListImbcbbrp(branch);

                    if (listImbcbbrp != null) {
                        listImbcbbrp.forEach(imbcbbrp -> {
                            String imbCNum = imbcbbrp.getString("A8BICN");
                            String imbBranch = imbcbbrp.getString("A8BRCD");
                            String cCode = imbcbbrp.getString("BCBBR");
                            //accounts.add(loadAccount(item, imbCNum, imbBranch, cCode));
                            loadAccount(item, imbCNum, imbBranch, cCode);
                        });
                    }
                }
            } catch (Exception ex) {
                auditController.error(BulkOpeningAccountsTask, "Ошибка при массовом открытии счетов", null, ex);
                log.error(null, ex);
            }
        });
    }

    private List<DataRecord> getListImbcbbrp(String branch) throws SQLException {
        String sqlSelect = "SELECT * FROM IMBCBBRP";

        if ("ALL".equalsIgnoreCase(branch)) {
            return repository.select(sqlSelect,
                     new Object[]{});
        } else {
            return repository.select(sqlSelect
                    + " WHERE A8CMCD=?", new Object[]{branch});
        }
    }

    private String getCCode(String branch) throws Exception {
        List<DataRecord> list = repository.select("SELECT CCODE FROM IMBCBBRP WHERE A8BRCD=?", new Object[]{branch});
        if (list.isEmpty()) {
            throw new Exception("Код филиала для " + branch + " не найден");
        } else {
            return list.get(0).getString(0);
        }
    }

    public List<DataRecord> getAccount(Object... args) throws SQLException {
        List<DataRecord> dataRecords = repository.selectMaxRows(
                "SELECT A.* FROM DWH.GL_OPENACC A " //+ "WHERE CLAUSE"
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

    private String loadAccount(DataRecord item, String cNum, String branch, String cCode) {
        String acId = createAcId(cNum, branch, item);
        //@@@ ??? createTmp(item, vAcid, cCode); 

        try {
            GLAccount account = getGLAccount(acId);
            try {
                updateAccount(account, item);
            } catch (Exception ex) {
                auditController.error(Account, format("Ошибка при изменении счета для acid = '%s'", acId), null, ex);
            }
        } catch (NoResultException ex) {
            createBalkOpeningAccount(item, cNum, branch, cCode, acId);
        }

        return acId;
    }

    private GLAccount getGLAccount(String acId) {
        return accountRepository.selectOne(GLAccount.class, "from GLAccount a where a.acid=?1 and a.dateClose is null", acId);
    }

    private List<DataRecord> getGLAccounts(Set<String> acids) throws Exception {
        try {
            String acidsStr = "'" + StringUtils.listToString(acids, "','") + "'";
            List<DataRecord> dataRecords = repository.selectMaxRows(
                    "SELECT * FROM DWH.GL_ACC A WHERE "
                    + "A.ACID IN (" + acidsStr + ") "
                    + "AND A.DTC IS NULL ",
                     Integer.MAX_VALUE, null);
            return dataRecords;
        } catch (SQLException e) {
            throw new Exception(e);
        }
    }

    private DataRecord getNativeGLAccount(String acId) throws Exception {
        return repository.selectFirst(
                "SELECT * FROM DWH.GL_ACC A WHERE "
                + "A.ACID = ? "
                + "AND A.DTC IS NULL ",
                 new Object[]{acId});
    }

    private void updateAccount(GLAccount account, DataRecord item) {
        account.setDealId(item.getString("DEALID"));
        account.setSubDealId(item.getString("SUBDEALID"));
        account.setDescription(item.getString("DESCRIPTION"));

        accountRepository.save(account);
    }

    private GLAccount createBalkOpeningAccount(DataRecord dataRecord, String cNum, String branch, String cCode, String acId) {
        Date dateOpen = dataRecord.getDate("DTO");
        AccountKeys keys = createBalkOpeningAccountKeys(dataRecord, cNum, branch, cCode, acId, dateOpen);

        return accountService.createBalkOpeningAccount(keys, dateOpen);
    }

    private AccountKeys createBalkOpeningAccountKeys(DataRecord dataRecord, String cNum, String branch, String cCode, String acId, Date dateOpen) {
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
}
