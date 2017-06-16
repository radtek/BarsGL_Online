/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2017
 */
package ru.rbt.barsgl.ejbtest;

import static java.lang.String.format;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.operday.task.BulkOpeningAccountsTask;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.shared.Assert;

/**
 *
 * @author Andrew Samsonov
 */
public class BulkOpeningAccountsTest extends AbstractRemoteTest {
    private static final Logger logger = Logger.getLogger(BulkOpeningAccountsTest.class.getName());

    @Test
    public void bulkOpeningAccount() {
        baseEntityRepository.executeNativeUpdate(
                "insert into dwh.gl_openacc values "
                //Cnum+CCY+Acod+SQ+Branch
                + "('008','00000083','RUR','3320','04','3320','04','91202','863020600','Бланки пластиковых карт, находящиеся в хранилище ценностей VISA Classic Unembossed (chip PayWave)','2017-05-26')");
        try {
            String acId = createAcId("00000083", "RUR", "3320", "04", "008");
            remoteAccess.invoke(BulkOpeningAccountsTask.class, "bulkOpeningAccounts");
            GLAccount account = (GLAccount) baseEntityRepository.selectOne(GLAccount.class,
                    "from GLAccount a where a.acid=?1 and a.dateClose is null", acId);
            Assert.notNull(account);
            deleteAccountByAcid(acId);
        } finally {
            baseEntityRepository.executeNativeUpdate("delete from GL_OPENACC "
                    + "where BRANCH = '008' and CNUM = '00000083' and CCY = 'RUR' and ACOD = '3320' and SQ = '04'");
        }
    }

    @Test
    public void bulkOpeningAccountWithErrorData() {
        baseEntityRepository.executeNativeUpdate(
                "insert into dwh.gl_openacc values "
                //Cnum+CCY+Acod+SQ+Branch
                + "('001','00000018','RUR','3213','01','3213','01','90702','851030300','Бланки собственных векселей Банка','2017-05-26')");
        try {
            String acId = createAcId("00000018", "RUR", "3213", "01", "001");
            remoteAccess.invoke(BulkOpeningAccountsTask.class, "bulkOpeningAccounts");
            GLAccount account = (GLAccount) baseEntityRepository.selectFirst(GLAccount.class,
                    "from GLAccount a where a.acid=?1 and a.dateClose is null", acId);
            org.junit.Assert.assertNull(account);
        } finally {
            baseEntityRepository.executeNativeUpdate("delete from GL_OPENACC "
                    + "where BRANCH = '001' and CNUM = '00000018' and CCY = 'RUR' and ACOD = '3213' and SQ = '01'");
        }
    }

    @Test
    public void bulkOpeningAccountWithCNumNull() throws SQLException {
        baseEntityRepository.executeNativeUpdate(
                "insert into dwh.gl_openacc values "
                //Cnum+CCY+Acod+SQ+Branch
                + "('MOS',null,'AUD','3201','01','3201','01','91104','861010101','Банкноты в иностранной валюте, принятые на экспертизу или выявленные сомнительные банкноты, по которым требуется экспертиза','2017-05-26')");

        try {
            List<String> acIdList = new ArrayList<>();

            List<DataRecord> listImbcbbrp = getListImbcbbrp("MOS");
            listImbcbbrp.forEach(imbcbbrp -> {
                String imbCNum = imbcbbrp.getString("A8BICN");
                String imbBranch = imbcbbrp.getString("A8BRCD");
                acIdList.add(createAcId(imbCNum, "AUD", "3201", "01", imbBranch));
            });

            remoteAccess.invoke(BulkOpeningAccountsTask.class, "bulkOpeningAccounts");

            acIdList.forEach(acId -> {
                GLAccount account = (GLAccount) baseEntityRepository.selectFirst(GLAccount.class,
                        "from GLAccount a where a.acid=?1 and a.dateClose is null", acId);
                Assert.notNull(account);
                deleteAccountByAcid(acId);
            });
        } finally {
            baseEntityRepository.executeNativeUpdate("delete from GL_OPENACC "
                    + "where BRANCH = 'MOS' and CNUM is null and CCY = 'AUD' and ACOD = '3201' and SQ = '01'");
        }
    }

    @Test
    public void bulkOpeningAccountWithCNumAll() throws SQLException {
        baseEntityRepository.executeNativeUpdate(
                "insert into dwh.gl_openacc values "
                //Cnum+CCY+Acod+SQ+Branch
                + "('ALL',null,'EUR','3201','01','3201','01','91104','861010101','Банкноты в иностранной валюте, принятые на экспертизу или выявленные сомнительные банкноты, по которым требуется экспертиза','2017-05-26')");

        try {
            List<String> acIdList = new ArrayList<>();

            List<DataRecord> listImbcbbrp = getListImbcbbrp("ALL");
            listImbcbbrp.forEach(imbcbbrp -> {
                String imbCNum = imbcbbrp.getString("A8BICN");
                String imbBranch = imbcbbrp.getString("A8BRCD");
                acIdList.add(createAcId(imbCNum, "EUR", "3201", "01", imbBranch));
            });

            remoteAccess.invoke(BulkOpeningAccountsTask.class, "bulkOpeningAccounts");

            acIdList.forEach(acId -> {
                GLAccount account = (GLAccount) baseEntityRepository.selectFirst(GLAccount.class,
                        "from GLAccount a where a.acid=?1 and a.dateClose is null", acId);
                Assert.notNull(account);
                deleteAccountByAcid(acId);
            });
        } finally {
            baseEntityRepository.executeNativeUpdate("delete from GL_OPENACC "
                    + "where BRANCH = 'ALL' and CNUM is null and CCY = 'EUR' and ACOD = '3201' and SQ = '01'");
        }
    }

    private List<DataRecord> getListImbcbbrp(String branch) throws SQLException {
        String sqlSelect = "SELECT * FROM IMBCBBRP";

        if ("ALL".equalsIgnoreCase(branch)) {
            return baseEntityRepository.select(sqlSelect,
                    new Object[]{});
        } else {
            return baseEntityRepository.select(sqlSelect
                    + " WHERE A8CMCD=?", new Object[]{branch});
        }
    }

    private String createAcId(String cNum, String ccy, String aCod, String sq, String branch) {
        // 8+3+4+2+3=20
        String acId = cNum
                + ccy
                + aCod
                + sq
                + branch;
        Assert.isTrue(20 == acId.length(), format("Неверная длина счета Майдас '%s' размер '%d'", acId, acId.length()));
        return acId;
    }
    
    private void deleteAccountByAcid(String acid) {
        try {
            int cntGlAcc = baseEntityRepository.executeNativeUpdate("delete from GL_ACC where BSAACID in " +
                    "(select BSAACID from accrln where acid = ?)", acid);
            int cntBsaAcc = baseEntityRepository.executeNativeUpdate("delete from BSAACC where ID in " +
                    "(select BSAACID from accrln where acid = ?)", acid);
            int cntAccRln = baseEntityRepository.executeNativeUpdate("delete from ACCRLN where acid = ?", acid);
            logger.info("deleted Midas from GL_ACC:" + cntGlAcc + "; ACCRLN:" + cntAccRln + "; BSAACC:" + cntBsaAcc);
        }finally {

        }
    }
    
}
