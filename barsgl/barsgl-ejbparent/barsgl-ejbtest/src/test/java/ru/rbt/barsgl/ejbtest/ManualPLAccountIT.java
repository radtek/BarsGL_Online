package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.*;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.dict.SourcesDeals;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountController;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountService;
import ru.rbt.barsgl.ejb.repository.GLAccountRepository;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.StringUtils;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.wrap;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.RUB;
import static ru.rbt.barsgl.ejbtest.AccountOpenAePostingsIT.checkDefinedAccount;
import static ru.rbt.barsgl.ejbtest.utl.Utl4Tests.deleteAccountByBsaAcid;
import static ru.rbt.barsgl.shared.enums.DealSource.Manual;
import static ru.rbt.ejbcore.util.StringUtils.rsubstr;

/**
 * Created by er18837 on 11.10.2018.
 */
public class ManualPLAccountIT extends AbstractRemoteIT {
    private static final Logger logger = Logger.getLogger(ManualPLAccountIT.class.getName());
    private static final Long USER_ID = 1L;

    private static List<String> bsaList = new ArrayList<String>();

    private static Operday oldOperday;

    @BeforeClass
    public static void beforeClass() throws ParseException {
        oldOperday = getOperday();
        Date curDate = DateUtils.parseDate("2018-07-02","yyyy-MM-dd");
        setOperday(curDate,curDate, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
        updateOperday(ONLINE, OPEN, Operday.PdMode.DIRECT);
    }

    @AfterClass
    public static void afterClass(){
        setOperday(oldOperday.getCurrentDate(),oldOperday.getLastWorkingDay(), oldOperday.getPhase(), oldOperday.getLastWorkdayStatus());
        updateOperday(ONLINE, OPEN, Operday.PdMode.DIRECT);
    }

    @Before
    public void before() {
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
    }

    @After
    public void After(){
        for (String bsaAcid: bsaList) {
            deleteAccountByBsaAcid(baseEntityRepository, bsaAcid);
        }
        bsaList.clear();
    }

    /**
     * Тест создания счета PL по ручному вводу RlnType = 2
     * @throws java.sql.SQLException
     */
    @Test
    public void testCreateManualPLAccount2() throws SQLException {
        String branch = "002";
        long accType = 602010300;
        short custType = 9;
        short term = 1;
        ManualAccountWrapper wr = newPLAccountWrapper(branch, accType, custType, term, USER_ID, null);

        ManualAccountWrapper wrapper = createManualPLAccount(wr);
        GLAccount account = (GLAccount) baseEntityRepository.findById(GLAccount.class, wrapper.getId());
        Assert.assertNotNull(account);
        bsaList.add(account.getBsaAcid());

        Assert.assertEquals("2", account.getRelationType());

        AccountKeys keys = remoteAccess.invoke(GLAccountController.class, "createWrapperAccountKeys", wrapper, getOperday().getCurrentDate());
        checkDefinedAccount(GLOperation.OperSide.N, wrapper.getBsaAcid(), keys.toString(), GLAccount.RelationType.TWO);
    }


    /**
     * Тест создания счета PL по ручному вводу RlnType = 5
     * @throws java.sql.SQLException
     */
    @Test
    public void testCreateManualPLAccount5() throws SQLException {
        String branch = "003";
        long accType = 712040100;
        short custType = 17;
        short term = 8;
        ManualAccountWrapper wr = newPLAccountWrapper(branch, accType, custType, term, USER_ID, null);

        ManualAccountWrapper wrapper = createManualPLAccount(wr);
        GLAccount account = (GLAccount) baseEntityRepository.findById(GLAccount.class, wrapper.getId());
        Assert.assertNotNull(account);

        Assert.assertEquals("5", account.getRelationType());

        AccountKeys keys = remoteAccess.invoke(GLAccountController.class, "createWrapperAccountKeys", wrapper, getOperday().getCurrentDate());
        checkDefinedAccount(GLOperation.OperSide.N, wrapper.getBsaAcid(), keys.toString(), GLAccount.RelationType.FIVE);
    }

    /**
     * Тест создания счета PL по ручному вводу RlnType = 4
     * @throws java.sql.SQLException
     */
    @Test
    public void testCreateManualPLAccount707() throws SQLException, ParseException {
        Date curDate = DateUtils.parseDate("2018-02-02","yyyy-MM-dd");
        setOperday(curDate,curDate, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
        String branch = "001";
        long accType = 706030101;
        short custType = 9;
        short term = 1;
        ManualAccountWrapper wr = newPLAccountWrapper(branch, accType, custType, term, USER_ID, null);
        wr.setBalanceAccount2("70706");
        wr.setPlCode("31415");

        ManualAccountWrapper wrapper = createManualPLAccount(wr);
        GLAccount account = (GLAccount) baseEntityRepository.findById(GLAccount.class, wrapper.getId());
        Assert.assertNotNull(account);

        Assert.assertEquals("4", account.getRelationType());

        AccountKeys keys = remoteAccess.invoke(GLAccountController.class, "createWrapperAccountKeys", wrapper, getOperday().getCurrentDate());
        checkDefinedAccount(GLOperation.OperSide.N, wrapper.getBsaAcid(), keys.toString(), GLAccount.RelationType.FOUR);
    }

    public ManualAccountWrapper createManualPLAccount(ManualAccountWrapper wrapper) throws SQLException {

        deletePLAccount(wrapper);
        RpcRes_Base<ManualAccountWrapper> res = remoteAccess.invoke(GLAccountService.class, "createManualPlAccount", wrapper);
        if (res.isError())
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        wrapper = res.getResult();
        Assert.assertTrue(0 < wrapper.getId());
        System.out.println("BSAACID = " + wrapper.getBsaAcid());
        bsaList.add(wrapper.getBsaAcid());
        return wrapper;
    }

    public ManualAccountWrapper newPLAccountWrapper(String branch,
                                                    long accountType, short custType, short term, Long userId, Date dateOpen) throws SQLException {
        DataRecord data = baseEntityRepository.selectFirst("select A8BICN as CNUM, BCBBR as CBCCN from IMBCBBRP where A8BRCD = ?", branch);
        String custNo = data.getString(0);
        String cbccn = data.getString(1);
        data = baseEntityRepository.selectFirst("select ACOD, AC_SQ from GL_ACTPARM where ACCTYPE = ? and CUSTYPE = ? and TERM = ?",
                accountType, custType, term);
        short acod = data.getShort(0);
        short sq = data.getShort(1);
        return newPLAccountWrapper(branch, custNo, cbccn, accountType, custType, term, acod, sq, userId, dateOpen);
    }

    public ManualAccountWrapper newPLAccountWrapper(String branch, String custNo, String cbccn,
                                                    long accountType, short custType, short term, short acod, short sq,
                                                    Long userId, Date dateOpen) {
        ManualAccountWrapper wrapper = new ManualAccountWrapper();
        wrapper.setBranch(branch);
        wrapper.setCurrency("RUR");
        wrapper.setCustomerNumber(custNo);  // Y (из IMBCBBRP)
        wrapper.setCompanyCode(cbccn);
        wrapper.setAccountType(accountType);
        wrapper.setCbCustomerType(custType);
        wrapper.setTerm(term);
        wrapper.setAccountCode(acod);
        wrapper.setAccountSequence(sq);
        wrapper.setDealSource(Manual.name());
        wrapper.setDateOpenStr(new SimpleDateFormat(wrapper.dateFormat).format(getOperday().getCurrentDate()));

        wrapper.setUserId(userId);
        if (null != dateOpen) {
            wrapper.setDateOpenStr(new SimpleDateFormat(wrapper.dateFormat).format(dateOpen));
        }

        return wrapper;
    }

    private boolean deletePLAccount(ManualAccountWrapper wrapper) {
        GLAccount account = remoteAccess.invoke(GLAccountRepository.class, "findGLPLAccountMnl",
                // currency, customerNumber, accountType, cbCustType, term, plcode, acc2, cbccn, dateCurrent);
                wrapper.getCurrency(), wrapper.getCustomerNumber(),
                wrapper.getAccountType().toString(), wrapper.getCbCustomerType().toString(), wrapper.getTerm().toString(),
                wrapper.getPlCode(), wrapper.getBalanceAccount2(), wrapper.getCompanyCode(), getOperday().getCurrentDate());

        if (null != account) {
            baseEntityRepository.executeNativeUpdate("delete from GL_ACC where ID = ? ", account.getId());
            return true;
        }
        return false;
    }
}
