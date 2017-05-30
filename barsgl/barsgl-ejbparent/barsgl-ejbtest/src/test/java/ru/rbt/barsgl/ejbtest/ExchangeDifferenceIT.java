package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountController;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountService;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejbcore.datarec.DataRecord;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static ru.rb.ucb.util.StringUtils.isEmpty;
import static ru.rbt.barsgl.ejbtest.utl.Utl4Tests.deleteGlAccountWithLinks;

/**
 * Created by ER22228 on 27.11.2015.
 */
public class ExchangeDifferenceIT extends AbstractRemoteIT {


//    @BeforeClass
//    public static void beforeClass() throws SQLException {
//        Date midasOperday = baseEntityRepository.selectOne("select workday from workday").getDate("workday");
//        setOperday(getWorkdayAfter(midasOperday), midasOperday, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
//    }


    @Before
    public void beforeClass() {
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
    }

    /**
     * Курсовая разница
     *
     * @throws ParseException
     */

//    @Test
    public void shouldReturnBsaacid() throws Exception {
        GLOperation glOperation = new GLOperation();

        glOperation.setValueDate(new SimpleDateFormat("yyyy-MM-dd").parse("2012-01-09"));

        String bsaacid = remoteAccess.invoke(GLAccountService.class, "getAccount", glOperation, GLOperation.OperSide.D, new AccountKeys(
//                "001;RUR;00400044;911020101;19;03;PL00000535;0001;96307;;6202;01;K+TP;955514;"
                "001;RUR;00000018;643010201;;;PL00000003;0001;70613;16101;7302;01;K+TP;;" // рабочий набор ключей для vtestmgr
//                "001;RUR;00000018;643010201;;;PL00000003;0001;70613;16101;7302;01;K+TP;;"
        ));

        Assert.assertNotNull(bsaacid);
    }

    @Test
    public void shouldCreateBsaacidIfAcidExists() throws Exception {
        GLOperation glOperation = new GLOperation();
        glOperation.setCurrencyDebit(BankCurrency.EUR);

        glOperation.setValueDate(getOperday().getCurrentDate());

        AccountKeys accountKeys = new AccountKeys("001;RUR;00000018;771030103;;;PL00000068;0001;70606;22101;7903;01;K+TP;;");
        String res = remoteAccess.invoke(GLAccountService.class, "getAccount", glOperation, GLOperation.OperSide.C, accountKeys);
        Assert.assertNotNull(res);

    }

    @Test
    public void shouldThrowException() throws Exception {
        GLOperation glOperation = (GLOperation) baseEntityRepository.selectFirst(GLOperation.class, "from GLOperation o");
        glOperation.setValueDate(getOperday().getCurrentDate());

        AccountKeys accountKeys = new AccountKeys(
//                "001;USD;00401794;351020301;9;;0000000242;0001;47408;;1876;01;K+TP;981162;"
                "001;RUR;00000018;643010201;;;0000000242;0001;70613;16101;7302;01;K+TP;;"
        );
        remoteAccess.invoke(GLAccountService.class, "getAccount", glOperation, GLOperation.OperSide.C, accountKeys);
    }


    private void deleteAccountCB22(String acid, String bsaacid, boolean wasAcid) {
//        remoteAccess.invoke(AccRlnRepository.class,"" +
        baseEntityRepository.executeNativeUpdate("delete from ACCRLN where BSAACID = ? and RLNTYPE='2'", bsaacid);
        baseEntityRepository.executeNativeUpdate("delete from BSAACC where ID = ?", bsaacid);
        if (!wasAcid)
            baseEntityRepository.executeNativeUpdate("delete from ACC where ID = ? ", acid);
    }

    @Test
    public void testPostingCreateAccountOfr() throws ParseException, SQLException {

//        baseEntityRepository.executeNativeUpdate("update gl_acc set dtc = ?", DateUtils.addDays(getOperday().getCurrentDate(), -10));

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());


        pst.setAccountCredit("");
        final String keyStringCredit = "001;RUR;00000018;643010201;0;0;PL00000003;0001;70613;16101;7904;01;K+TP;;";
        deleteGlAccountWithLinks(baseEntityRepository, keyStringCredit);
        pst.setAccountKeyCredit(keyStringCredit);//keysCredit.toString());

//        String accDt = Optional.ofNullable(baseEntityRepository.selectFirst("select id from bsaacc where id like ?", "47408810%"))
//                .orElseThrow(()->new RuntimeException("Null operation")).getString("id");
//        pst.setAccountDebit(accDt);

        pst.setAccountDebit("");
//        final String keyStringDebit = "001;EUR;00640839;501020501;18;;0000001601;0001;47407;;4498;01;K+TP;968007;";
        final String keyStringDebit = "001;BGN;00640839;501020501;18;;0000001601;0001;47407;;4498;01;K+TP;968007;";
        deleteGlAccountWithLinks(baseEntityRepository, keyStringDebit);
        pst.setAccountKeyDebit(keyStringDebit);

        pst.setAmountCredit(new BigDecimal("15.99"));
        pst.setAmountDebit(new BigDecimal("200"));
        pst.setCurrencyCredit(BankCurrency.RUB);
        pst.setCurrencyDebit(BankCurrency.BGN);

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(null != operation && 0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(GLOperation.class, operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());

        // проверка счетов
        String acidDr = "", acidCr = "";
        if (StringUtils.isEmpty(pst.getAccountDebit()))
            acidDr = checkDefinedAccount(GLOperation.OperSide.D, operation.getAccountDebit(), operation.getAccountKeyDebit(), operation.getValueDate());

        Assert.assertNotNull(acidDr);
//        deleteAccountExDiff(operation.getAccountCredit());

    }

    private void deleteAccountExDiff(String bsaacid) throws SQLException {
        DataRecord data = baseEntityRepository.selectFirst("select ACID from ACCRLN where BSAACID = ? and RLNTYPE = ?", bsaacid, "2");
        String acid = "";
        if (data != null) {
            acid =  data.getString(0);
        }
        baseEntityRepository.executeNativeUpdate("delete from excacrln where ACID = ? and BSAACID = ?", acid, bsaacid);
        baseEntityRepository.executeNativeUpdate("delete from BSAACC where ID = ?", bsaacid);
        baseEntityRepository.executeNativeUpdate("delete from ACCrln where ACID = ? and BSAACID = ?", acid, bsaacid);
    }

    private void deleteAccountCB2(String bsaacid, boolean deleteAcid) throws SQLException {
        DataRecord data = baseEntityRepository.selectFirst("select ACID from ACCRLN where BSAACID = ? and RLNTYPE = ?", bsaacid, "2");
        String acid = "";
        if (data != null) {
            acid =  data.getString(0);
        }
        baseEntityRepository.executeNativeUpdate("delete from ACCRLN where ACID = ? and BSAACID = ?", acid, bsaacid);
        baseEntityRepository.executeNativeUpdate("delete from BSAACC where ID = ?", bsaacid);
        if (!isEmpty(acid) && deleteAcid)
            baseEntityRepository.executeNativeUpdate("delete from ACC where ID = ?", acid);
    }

    private void deleteAccountCB(String acid, String bsaacid, boolean deleteAcid) {
        baseEntityRepository.executeNativeUpdate("delete from ACCRLN where ACID = ? and BSAACID = ?", acid, bsaacid);
        baseEntityRepository.executeNativeUpdate("delete from BSAACC where ID = ?", bsaacid);
        if (deleteAcid)
            baseEntityRepository.executeNativeUpdate("delete from ACC where ID = ?", acid);
    }



    /**
     * Проверка правильности создания / определения счета по кдючам счета
     * @param operSide  - сторона операции (D / C)
     * @param bsaAcid   - счет ЦБ
     * @param keyString - ключи счета
     * @return          - счет Майдас
     * @throws SQLException
     */
    public String checkDefinedAccount(GLOperation.OperSide operSide, String bsaAcid, String keyString, Date dateOpen) throws SQLException {
        AccountKeys keys = new AccountKeys(keyString);
        Assert.assertFalse(StringUtils.isEmpty(bsaAcid));
        DataRecord data = baseEntityRepository.selectOne("select 1 from BSAACC where ID = ?", bsaAcid);
        Assert.assertNotNull(data);                     // bsaAcid есть в таблице BSAACC
        String acid, rlnType;
        String glSequence = keys.getGlSequence();
        boolean accMidas = !StringUtils.isEmpty(glSequence)
                && ( glSequence.startsWith("XX") || glSequence.startsWith("GL") );
        boolean accPl = !StringUtils.isEmpty(glSequence)
                && ( glSequence.startsWith("PL"));
        if (accMidas) {                                        // счет существует
            AccountKeys keys1 = remoteAccess.invoke(GLAccountController.class, "fillAccountKeysMidas", GLOperation.OperSide.N, dateOpen, keys);
            acid = keys1.getAccountMidas();
            Assert.assertFalse(StringUtils.isEmpty(acid));
            rlnType = StringUtils.isEmpty(keys1.getPlCode()) ? "0" : "2";
        } else if (accPl) {
            AccountKeys keys1 = remoteAccess.invoke(GLAccountController.class, "fillAccountOfrKeysMidas", GLOperation.OperSide.N, dateOpen, keys);
            acid = keys1.getAccountMidas();
            Assert.assertFalse(StringUtils.isEmpty(acid));
            rlnType = StringUtils.isEmpty(keys1.getPlCode()) ? "0" : "2";
        } else {    // был создан новый счет
            GLAccount account = (GLAccount) baseEntityRepository.selectOne(GLAccount.class,
                    "from GLAccount a where a.bsaAcid = ?1", bsaAcid);
            Assert.assertNotNull(account);
            Assert.assertEquals(operSide, account.getOperSide());
            acid = account.getAcid();
            Assert.assertFalse(StringUtils.isEmpty(acid));
            rlnType = StringUtils.isEmpty(account.getPlCode()) ? "4" : "2";
        }
        data = baseEntityRepository.selectOne("select 1 from ACC where ID = ?", acid);
        Assert.assertNotNull(data);                 // acid есть в таблице ACC
        data = baseEntityRepository.selectOne("select 1 from ACCRLN where BSAACID = ? and ACID = ? and RLNTYPE = ?",
                bsaAcid, acid, rlnType);
        Assert.assertNotNull(data);                 // bsaAcid + acid есть в таблице ACCRLN
        return acid;
    }

}
