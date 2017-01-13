package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
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
import ru.rbt.barsgl.ejb.repository.GLAccountRepository;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;
import ru.rbt.barsgl.ejbcore.util.StringUtils;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;
import ru.rbt.barsgl.shared.enums.OperState;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.rsubstr;
import static ru.rbt.barsgl.ejbtest.AccountOpenAePostingsTest.checkDefinedAccount;
import static ru.rbt.barsgl.ejbtest.AccountOpenAePostingsTest.delateSQvalue;
import static ru.rbt.barsgl.ejbtest.utl.Utl4Tests.deleteGlAccountWithLinks;

/**
 * Created by ER18837 on 29.10.15.
 */
public class ManualAccountTest extends AbstractRemoteTest {

    private static final Logger logger = Logger.getLogger(AccountOpenAePostingsTest.class.getName());

    @Before
    public void beforeClass() {
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
    }

    /**
     * Тест создания счета по ручному вводу
     * @throws java.sql.SQLException
     */
    @Test
    public void testCreateManualAccount() throws SQLException {
        final String subdealId = "SUB_" + StringUtils.rsubstr(System.currentTimeMillis() + "", 2);
        final String term = "00";
        ManualAccountWrapper wrapper = createManualAccount("001", "RUR", "00601715", 181010101, null, subdealId, term);
        GLAccount account = (GLAccount) baseEntityRepository.findById(GLAccount.class, wrapper.getId());
        Assert.assertNotNull(account);
        Assert.assertEquals(subdealId, account.getSubDealId());
        Assert.assertTrue(Short.parseShort(term) == account.getTerm());
        AccountKeys keys = remoteAccess.invoke(GLAccountController.class, "createWrapperAccountKeys", wrapper, getOperday().getCurrentDate());
        checkDefinedAccount(GLOperation.OperSide.N, wrapper.getBsaAcid(), keys.toString());
    }

    /**
     * Создание счета по ручному вводу для разных систем с подменой Midas SQ
     */
    @Test
    public void testCreateManualAccountMidasSQ(){
        final String dealId = "DEAL_" + StringUtils.rsubstr(System.currentTimeMillis() + "", 9);
        final String subdealId = "SUB_" + StringUtils.rsubstr(System.currentTimeMillis() + "", 2);
        final String ccy = "RUR";
        final String custNo = "00627339";
        final String custType = "6";
        final long accType = 410060308;
        final String term = "00";
        delateSQvalue(custNo, ccy);

        DataRecord data2 = remoteAccess.invoke(GLAccountRepository.class
                , "getAccountParams", Long.toString(accType), custType, term, getOperday().getCurrentDate());
        Assert.assertNotNull(data2);
        Short sq = data2.getShort("SQ");

        // KP - SQ из параметров
        ManualAccountWrapper wrapper1 = createManualAccountSQ("007", ccy, custNo, accType, null, "K+TP", dealId, subdealId, term, null);
        GLAccount account1 = (GLAccount) baseEntityRepository.findById(GLAccount.class, wrapper1.getId());
        Assert.assertNotNull(account1);
        Assert.assertEquals(sq, account1.getAccountSequence());

        // FLEX - SQ задан - не меняется
        final String dealId6 = "DEAL_" + StringUtils.rsubstr(System.currentTimeMillis() + "", 9);
        Short sq6 = 90;
        ManualAccountWrapper wrapper6 = createManualAccountSQ("007", ccy, custNo, accType, null, "FC12_CL", dealId6, subdealId, term, sq6);
        GLAccount account6 = (GLAccount) baseEntityRepository.findById(GLAccount.class, wrapper6.getId());
        Assert.assertNotNull(account6);
        Assert.assertEquals(sq6, account6.getAccountSequence());

        // FLEX - SQ не задан, та же сделка - SQ из предыдущей
        ManualAccountWrapper wrapper7 = createManualAccountSQ("008", ccy, custNo, accType, null, "FC12_CL", dealId6, subdealId, term, null);
        GLAccount account7 = (GLAccount) baseEntityRepository.findById(GLAccount.class, wrapper7.getId());
        Assert.assertNotNull(account7);
        Assert.assertEquals(sq6, account7.getAccountSequence());

        // ARMPRO - SQ задан - не меняется
        Short sq2 = 91;
        ManualAccountWrapper wrapper2 = createManualAccountSQ("007", ccy, custNo, accType, null, "ARMPRO", dealId, subdealId, term, sq2);
        GLAccount account2 = (GLAccount) baseEntityRepository.findById(GLAccount.class, wrapper2.getId());
        Assert.assertNotNull(account2);
        Assert.assertEquals(sq, account2.getAccountSequence());

        // ARMPRO - SQ не задан, та же сделка - SQ из параметров
        ManualAccountWrapper wrapper4 = createManualAccountSQ("008", ccy, custNo, accType, null, "ARMPRO", dealId, subdealId, term, null);
        GLAccount account4 = (GLAccount) baseEntityRepository.findById(GLAccount.class, wrapper4.getId());
        Assert.assertNotNull(account4);
        Assert.assertEquals(sq, account4.getAccountSequence());

        // MZO - другая сделка, SQ задан - не меняется
        Short sq3 = 92;
        final String dealId3 = "DEAL_" + StringUtils.rsubstr(System.currentTimeMillis() + "", 9);
        final String subdealId3 = "SUB_" + StringUtils.rsubstr(System.currentTimeMillis() + "", 2);
        ManualAccountWrapper wrapper3 = createManualAccountSQ("007", ccy, custNo, accType, null, "MZO", dealId3, subdealId3, term, sq3);
        GLAccount account3 = (GLAccount) baseEntityRepository.findById(GLAccount.class, wrapper3.getId());
        Assert.assertNotNull(account3);
        Assert.assertEquals(sq3, account3.getAccountSequence());

        // MZO - та же сделка, другой SQ - ошибка
        Short sq5 = 93;
        RpcRes_Base<ManualAccountWrapper> res = createManualAccountOnly("008", ccy, custNo, accType, null, "MZO", dealId3, subdealId3, term, sq5);
        Assert.assertTrue(res.isError());
    }
    /**
     * Тест создания счета из ручного ввода с ошибкой ввода (неверная валюта)
     * @throws SQLException
     */
    @Test
    public void testCreateManualAccountError() throws SQLException {
        ManualAccountWrapper wrapper = newAccountWrapper("257", "LOL", "00640994", 35102100);
        RpcRes_Base<ManualAccountWrapper> res = remoteAccess.invoke(GLAccountService.class, "createManualAccount", wrapper);
        Assert.assertTrue(res.isError());
        wrapper = res.getResult();
        Assert.assertNull(wrapper.getId());
        Assert.assertFalse(isEmpty(res.getMessage()));
        System.out.println("Message: " + res.getMessage());
    }

    /**
     * Тест редактирования счета из ручного ввода
     * @throws SQLException
     */
    @Test
    public void testEditManualAccount() throws SQLException {
        ManualAccountWrapper wrapper = createManualAccount("001", "RUR", "00103796", 132010300, getOperday().getCurrentDate(), "subdeal", "01");
        GLAccount account = (GLAccount) baseEntityRepository.findById(GLAccount.class, wrapper.getId());

        Date dateOpen = getOperday().getLastWorkingDay();
        wrapper.setDateOpenStr(new SimpleDateFormat(wrapper.dateFormat).format(dateOpen));
        Date dateClose = getOperday().getCurrentDate();
        wrapper.setDateCloseStr(new SimpleDateFormat(wrapper.dateFormat).format(dateClose));
        Long stamp = System.currentTimeMillis();
        String tmp = rsubstr(stamp.toString(), 6);

        wrapper.setDealId("deal" + tmp);
        wrapper.setSubDealId("subdeal" + tmp);
        wrapper.setDescription("description" + tmp);
        RpcRes_Base<ManualAccountWrapper> res = remoteAccess.invoke(GLAccountService.class, "updateManualAccount", wrapper);
        Assert.assertFalse(res.isError());
        ManualAccountWrapper wrapper1 = res.getResult();
        Assert.assertEquals(account.getId(), wrapper1.getId());

        checkUpdateAccount(wrapper.getBsaAcid(), wrapper, dateOpen, dateClose);
    }

    /**
     * Тест ошибки при редактирования счета из ручного ввода
     * @throws SQLException
     */
    @Test
    public void testEditManualAccountError() throws SQLException {
        ManualAccountWrapper wrapper = createManualAccount("001", "RUR", "00103796", 132010300, getOperday().getCurrentDate(), "subdeal", "01");
        GLAccount account = (GLAccount) baseEntityRepository.findById(GLAccount.class, wrapper.getId());

        wrapper.setCustomerNumber(null);
//        Date dateClose = getOperday().getLastWorkingDay();
//        wrapper.setDateCloseStr(new SimpleDateFormat(wrapper.dateFormat).format(dateClose));

        RpcRes_Base<ManualAccountWrapper> res = remoteAccess.invoke(GLAccountService.class, "updateManualAccount", wrapper);
        Assert.assertTrue(res.isError());
        ManualAccountWrapper wrapper1 = res.getResult();
        Assert.assertEquals(account.getId(), wrapper1.getId());
        Assert.assertFalse(isEmpty(res.getMessage()));
        System.out.println("Message: " + res.getMessage());
    }

    /**
     * Тест закрытия счета из ручного ввода
     * @throws SQLException
     */
    @Test
    public void testCloseManualAccount() throws SQLException {
        // TODO может быть несоотв между GL_ACNOCNT и максимальным номером с таким PLCODE в GL_ACC
        // TODO надо как-то этого избежать
        ManualAccountWrapper wrapper = createManualAccount("001", "RUR", "00100347", 191010201, getOperday().getLastWorkingDay());
        GLAccount account = (GLAccount) baseEntityRepository.findById(GLAccount.class, wrapper.getId());

        Date dateClose = getOperday().getCurrentDate();
        wrapper.setDateCloseStr(new SimpleDateFormat(wrapper.dateFormat).format(dateClose));
        wrapper.setUserId(2L);
        RpcRes_Base<ManualAccountWrapper> res = remoteAccess.invoke(GLAccountService.class, "closeManualAccount", wrapper);
        Assert.assertFalse(res.isError());
        wrapper = res.getResult();
        Assert.assertEquals(account.getId(), wrapper.getId());

        checkCloseAccount(wrapper.getBsaAcid(), dateClose);
    }

    /**
     * Тест закрытия счета из ручного ввода с ошибкой даты закрытия
     * @throws SQLException
     */
    @Test
    public void testCloseManualAccountDateError() throws SQLException {
        ManualAccountWrapper wrapper = createManualAccount("001", "RUR", "00699937", 351021001, getOperday().getCurrentDate());
        wrapper.setUserId(2L);
        GLAccount account = (GLAccount) baseEntityRepository.findById(GLAccount.class, wrapper.getId());

        Date curDate = getOperday().getCurrentDate();
        Date dateClose = DateUtils.addDays(curDate, 1);
        wrapper.setDateCloseStr(new SimpleDateFormat(wrapper.dateFormat).format(dateClose));
        RpcRes_Base<ManualAccountWrapper> res = remoteAccess.invoke(GLAccountService.class, "closeManualAccount", wrapper);
        Assert.assertTrue(res.isError());
        Assert.assertFalse(isEmpty(res.getMessage()));
        System.out.println("Message: " + res.getMessage());

        dateClose = DateUtils.addDays(curDate, -1);
        wrapper.setDateCloseStr(new SimpleDateFormat(wrapper.dateFormat).format(dateClose));
        res = remoteAccess.invoke(GLAccountService.class, "closeManualAccount", wrapper);
        Assert.assertTrue(res.isError());
        Assert.assertFalse(isEmpty(res.getMessage()));
        System.out.println("Message: " + res.getMessage());

        dateClose = curDate;
        wrapper.setDateCloseStr(new SimpleDateFormat(wrapper.dateFormat).format(dateClose));
        res = remoteAccess.invoke(GLAccountService.class, "closeManualAccount", wrapper);
        Assert.assertFalse(res.isError());
        dateClose = curDate;

    }

    /**
     * Тест закрытия счета из ручного ввода с ошибкой баланса
     * @throws SQLException
     */
    @Test
    public void testCloseManualAccountBalanceError() throws SQLException {
        ManualAccountWrapper wrapper = createManualAccount("001", "RUR", "00600609", 181010101, getOperday().getCurrentDate());
        GLAccount account = (GLAccount) baseEntityRepository.findById(GLAccount.class, wrapper.getId());

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "AccountBalance");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountCredit(account.getBsaAcid());
        pst.setAccountDebit("40702810400010002676");
        pst.setAmountCredit(new BigDecimal("698.35"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.RUB);
        pst.setCurrencyDebit(pst.getCurrencyCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());

        Date dateClose = getOperday().getCurrentDate();
        wrapper.setDateCloseStr(new SimpleDateFormat(wrapper.dateFormat).format(dateClose));
        RpcRes_Base<ManualAccountWrapper> res = remoteAccess.invoke(GLAccountService.class, "closeManualAccount", wrapper);
        Assert.assertTrue(res.isError());
        Assert.assertFalse(isEmpty(res.getMessage()));
        System.out.println("Message: " + res.getMessage());
    }


    /**
     * Тест закрытия счета из ручного ввода с необработанными операциями
     * @throws SQLException
     */
    @Test
    public void testCloseManualAccountOperError() throws SQLException {
        ManualAccountWrapper wrapper = createManualAccount("001", "RUR", "00640539", 351021001, getOperday().getCurrentDate(), "subdeal", "01");
        GLAccount account = (GLAccount) baseEntityRepository.findById(GLAccount.class, wrapper.getId());

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "AccountBalance");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountCredit(account.getBsaAcid());
        pst.setAccountDebit("40702810400010002676");
        pst.setAmountCredit(new BigDecimal("699.35"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.RUB);
        pst.setCurrencyDebit(pst.getCurrencyCredit());
        pst.setParentReference("fanRef_" + stamp);
        pst.setFan(YesNo.Y);

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.LOAD, operation.getState());

        Date dateClose = getOperday().getCurrentDate();
        wrapper.setDateCloseStr(new SimpleDateFormat(wrapper.dateFormat).format(dateClose));
        RpcRes_Base<ManualAccountWrapper> res = remoteAccess.invoke(GLAccountService.class, "closeManualAccount", wrapper);
        Assert.assertTrue(res.isError());
        Assert.assertFalse(isEmpty(res.getMessage()));
        System.out.println("Message: " + res.getMessage());
    }

    public static ManualAccountWrapper createManualAccount(String branch, String currency, String customerNumber,
                                                           long accountType, Date dateOpen, String subdealId, String term) {
        ManualAccountWrapper wrapper = newAccountWrapper(branch, currency, customerNumber, accountType);
        wrapper.setSubDealId(subdealId);
        wrapper.setTerm(isEmpty(term) ? null : Short.parseShort(term));
        if (null != dateOpen) {
            wrapper.setDateOpenStr(new SimpleDateFormat(wrapper.dateFormat).format(dateOpen));
        }
        RpcRes_Base<ManualAccountWrapper> res = remoteAccess.invoke(GLAccountService.class, "createManualAccount", wrapper);
        Assert.assertFalse(res.isError());
        wrapper = res.getResult();
        Assert.assertTrue(0 < wrapper.getId());
        System.out.println("BSAACID = " + wrapper.getBsaAcid());
        return wrapper;
    }

    public static ManualAccountWrapper createManualAccountSQ(String branch, String currency, String customerNumber,
                                                             long accountType, Date dateOpen, String src, String dealId, String subdealId, String term, Short sq) {
/*
        ManualAccountWrapper wrapper = newAccountWrapper(branch, currency, customerNumber, accountType, src, dealId);
        wrapper.setSubDealId(subdealId);
        wrapper.setTerm(isEmpty(term) ? null : Short.parseShort(term));
        wrapper.setAccountSequence(sq);
        if (null != dateOpen) {
            wrapper.setDateOpenStr(new SimpleDateFormat(wrapper.dateFormat).format(dateOpen));
        }
        RpcRes_Base<ManualAccountWrapper> res = remoteAccess.invoke(GLAccountService.class, "createManualAccount", wrapper);
*/
        RpcRes_Base<ManualAccountWrapper> res = createManualAccountOnly(branch, currency, customerNumber,
                accountType, dateOpen, src, dealId, subdealId, term, sq);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        ManualAccountWrapper wrapper = res.getResult();
        Assert.assertTrue(0 < wrapper.getId());
        System.out.println("BSAACID = " + wrapper.getBsaAcid());
        return wrapper;
    }

    public static RpcRes_Base<ManualAccountWrapper>  createManualAccountOnly(String branch, String currency, String customerNumber,
                                                                             long accountType, Date dateOpen, String src, String dealId, String subdealId, String term, Short sq) {
        ManualAccountWrapper wrapper = newAccountWrapper(branch, currency, customerNumber, accountType, src, dealId);
        wrapper.setSubDealId(subdealId);
        wrapper.setTerm(isEmpty(term) ? null : Short.parseShort(term));
        wrapper.setAccountSequence(sq);
        if (null != dateOpen) {
            wrapper.setDateOpenStr(new SimpleDateFormat(wrapper.dateFormat).format(dateOpen));
        }
        AccountKeys keys = remoteAccess.invoke(GLAccountController.class, "createWrapperAccountKeys", wrapper,
                null != dateOpen ? dateOpen : getOperday().getCurrentDate());
        deleteGlAccountWithLinks(baseEntityRepository, keys.toString());
        RpcRes_Base<ManualAccountWrapper> res = remoteAccess.invoke(GLAccountService.class, "createManualAccount", wrapper);
        return res;
    }

    private ManualAccountWrapper createManualAccount(String branch, String currency, String customerNumber,
                                                     long accountType, Date dateOpen) {
        return createManualAccount(branch, currency, customerNumber, accountType, dateOpen, null, null);
    }

    public static ManualAccountWrapper newAccountWrapper(String branch, String currency, String customerNumber,
                                                         long accountType) {
        Long stamp = System.currentTimeMillis();
        String dealId = rsubstr(stamp.toString(), 6);

        return newAccountWrapper(branch, currency, customerNumber, accountType, "K+TP", dealId);
    }

    public static ManualAccountWrapper newAccountWrapper(String branch, String currency, String customerNumber,
                                                         long accountType, String src, String dealId) {
        ManualAccountWrapper wrapper = new ManualAccountWrapper();
        wrapper.setBranch(branch);
        wrapper.setCurrency(currency);
        wrapper.setCustomerNumber(customerNumber);
        wrapper.setAccountType(accountType);
        wrapper.setDealId(dealId);
        wrapper.setDealSource(src); // TODO что это за символ, гнать его из кода!
        wrapper.setDateOpenStr(new SimpleDateFormat(wrapper.dateFormat).format(getOperday().getCurrentDate()));

        return wrapper;
    }

    private void checkCloseAccount(String bsaAcid, Date dateClose) throws SQLException {
        Assert.assertFalse(isEmpty(bsaAcid));
        GLAccount account = (GLAccount) baseEntityRepository.selectOne(GLAccount.class,
                "from GLAccount a where a.bsaAcid = ?1 and a.dateClose = ?2", bsaAcid, dateClose);
        Assert.assertNotNull(account);
        Assert.assertEquals(account.getDateClose(), dateClose);

        DataRecord data = baseEntityRepository.selectOne("select BSAACC from BsaAcc a where a.ID = ?", bsaAcid);
        Assert.assertNotNull(data);                     // bsaAcid есть в таблице BSAACC
        Assert.assertEquals(data.getDate(0), dateClose);

        data = baseEntityRepository.selectOne("select DRLNC from AccRln a where BSAACID = ? and ACID = ? and RLNTYPE = ?",
                bsaAcid, account.getAcid(), account.getRelationType());
        Assert.assertNotNull(data);                 // bsaAcid + acid есть в таблице ACCRLN
        Assert.assertEquals(data.getDate(0), dateClose);
    }

    private void checkUpdateAccount(String bsaAcid, ManualAccountWrapper wrapper, Date dateOpen, Date dateClose) throws SQLException {
        Assert.assertFalse(isEmpty(bsaAcid));
        GLAccount account = (GLAccount) baseEntityRepository.selectOne(GLAccount.class,
                "from GLAccount a where a.bsaAcid = ?1", bsaAcid);
        Assert.assertNotNull(account);
        Assert.assertEquals(dateOpen, account.getDateOpen());
        Assert.assertEquals(dateClose, account.getDateClose());
        Assert.assertEquals(wrapper.getDescription(), account.getDescription());
        Assert.assertEquals(wrapper.getDealId(), account.getDealId());
        Assert.assertEquals(wrapper.getSubDealId(), account.getSubDealId());

        DataRecord data = baseEntityRepository.selectOne("select BSAACO, BSAACC from BsaAcc a where a.ID = ?", bsaAcid);
        Assert.assertNotNull(data);                     // bsaAcid есть в таблице BSAACC
        Assert.assertEquals(data.getDate(0), dateOpen);
        Assert.assertEquals(data.getDate(1), dateClose);

        data = baseEntityRepository.selectOne("select DRLNO, DRLNC from AccRln a where BSAACID = ? and ACID = ? and RLNTYPE = ?",
                bsaAcid, account.getAcid(), account.getRelationType());
        Assert.assertNotNull(data);                 // bsaAcid + acid есть в таблице ACCRLN
        Assert.assertEquals(data.getDate(0), dateOpen);
        Assert.assertEquals(data.getDate(1), dateClose);
    }

}
