package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeysBuilder;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountController;
import ru.rbt.barsgl.ejb.integr.acc.OfrAccountService;
import ru.rbt.barsgl.ejb.repository.GLAccountRepository;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;
import ru.rbt.barsgl.shared.enums.OperState;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import static ru.rb.ucb.util.StringUtils.isEmpty;
import static ru.rbt.barsgl.ejbtest.utl.Utl4Tests.deleteGlAccountWithLinks;

/**
 * Created by ER18837 on 16.11.15.
 */
public class ManualAccountOfrTest extends AbstractRemoteTest {
    private static final Logger logger = Logger.getLogger(AccountOpenAePostingsTest.class.getName());

    @Before
    public void beforeClass() {
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
    }


    /**
     * Тест создания счета ЦБ через внешнюю функцию
     * @throws SQLException
     */
    @Test
    public void testCreateAccountCB() throws SQLException, ParseException {

        Date dateOpen = getOperday().getCurrentDate();

        // 010	UCB, NVS, OO Barnaulsky       	0050	00000109
        String branch = "010";
        Short acod = 7004;
        Short sq = 01;
        Short custType = 0;
        String acc2 = "70601";
        String rlnType = "0";

        String[] accRln = createAccountCB(branch, acod, sq, custType, acc2, rlnType, dateOpen);
        Assert.assertNotNull(accRln);
        Assert.assertNotNull(accRln[0]);
        Assert.assertNotNull(accRln[1]);

        DataRecord res = baseEntityRepository.selectFirst(
                "select * from ACCRLN where ACID = ? and BSAACID = ? and RLNTYPE = ? and ? between DRLNO and DRLNC",
                accRln[0], accRln[1], rlnType, dateOpen);
        Assert.assertNotNull(res);

        deleteAccountCB(accRln[0], accRln[1], true);

    }

    private String[] createAccountCB(String branch, Short acod, Short sq, Short custType, String acc2, String rlnType, Date dateOpen) throws SQLException, ParseException {

        DataRecord data = baseEntityRepository.selectOne("select BCBBR, A8BICN from IMBCBBRP where A8BRCD = ?", branch);
        Assert.assertNotNull(data);
        String filialCode = data.getString("BCBBR");
        String cnum = data.getString("A8BICN");

        String acid = remoteAccess.invoke(GLAccountRepository.class, "makeMidasAccount", Integer.parseInt(cnum), "RUR", branch, acod, sq);
        Assert.assertEquals(20, acid.length());
        DataRecord data1 = baseEntityRepository.selectFirst("select ID from ACC where ID = ?", acid);
        Assert.assertNull(data1);

        // String acod, short sq, short custType, Date curdate
        String plCode = remoteAccess.invoke(GLAccountRepository.class, "getPlCode", acod.toString(), sq, custType, dateOpen);
        Assert.assertFalse(isEmpty(plCode));

        DataRecord data2 = baseEntityRepository.selectOne("select PSAV from BSS where ACC2 = ?", acc2);
        Assert.assertNotNull(data2);
        String psav = data2.getString(0);

        AccountKeys keys0 = AccountKeysBuilder.create()
                .withAcc2(acc2)
                .withCurrencyDigital("810")
                .withCompanyCode(filialCode)
                .withPlCode(plCode)
                .withAccountMidas(acid)
                .withRelationType(rlnType)
                .withCustomerType(custType.toString())
                .withPassiveActive(psav)
                .build();

        Date dateClose = new SimpleDateFormat(ManualAccountWrapper.dateFormat).parse(ManualAccountWrapper.dateNull);
        try {   // пока транзакция на внешней стороне
            String bsaacid = remoteAccess.invoke(OfrAccountService.class, "createAccountCBnoTrans", keys0, dateOpen, dateClose, dateOpen);
            Assert.assertEquals(20, bsaacid.length());
            checkAccountCB(acid, bsaacid, rlnType);
            Assert.assertTrue(true);
            return new String[]{acid, bsaacid};
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Assert.assertTrue(false);
            return null;
        }
    }

    /**
     * Тест создания счета ЦБ из ручного ввода
     * Проходит, если в таблице
     * @throws SQLException
     * @throws ParseException
     */
    @Test
    @Ignore
    // этим сервисом счета ОФР уже не создаются вручную
    public void testCreateManualOfrAccount() throws SQLException, ParseException {
        // создать счет Midas и псевдо-счет ЦБ
        Date dateOpen = getOperday().getCurrentDate();

        int rown = Math.abs((int) System.currentTimeMillis() % 100);   // случайное число
        // ACOD
        String sql = "select BSAACID, ACID, CTYPE, ACC2, PSAV, GLACOD  from ACCRLN ar" +
                " join IMBCBHBPN bpn on ar.GLACOD = bpn.HBMIAC and ar.CTYPE = bpn.HBCTYP and bpn.DAT <= ?" +
                " where RLNTYPE = '0' and CBCCY = '810'  and ? between DRLNO and DRLNC and GLACOD in " +
                " (select ac.A5ACCD from SDACODPD ac join IMBCBHBPN pl on pl.HBMIAC=ac.A5ACCD  where pl.DATTO >= ?)" +
                " fetch first " + rown + " rows only";

        // получить счет Майдас из ACCRLN с RLNTYPE = 0
        List<DataRecord> dataList = baseEntityRepository.select(sql, dateOpen, dateOpen, dateOpen);
        Assert.assertNotNull(dataList);
        DataRecord data0 = dataList.get(dataList.size()-1);
        String bsaAcid = data0.getString("BSAACID");
        String acid = data0.getString("ACID");
        Short custType = data0.getShort("CTYPE");
        String acc2 = data0.getString("ACC2");
        String psav = data0.getString("PSAV");
        Short acod = data0.getShort("GLACOD");
        Short sq = Short.parseShort(acid.substring(15, 17));
        String branch = acid.substring(17, 20);

        // создать по нему счет ОФР
        ManualAccountWrapper wrapper = new ManualAccountWrapper();
        wrapper.setBranch(branch);
        wrapper.setAccountCode(acod);
        wrapper.setAccountSequence(sq);
        wrapper.setCbCustomerType(custType);
        wrapper.setDateOpenStr(new SimpleDateFormat(wrapper.dateFormat).format(dateOpen));
        RpcRes_Base<ManualAccountWrapper> res0 = remoteAccess.invoke(OfrAccountService.class, "getOfrAccountParameters", wrapper);
        Assert.assertNotNull(res0);
        Assert.assertFalse(res0.isError());
        Assert.assertEquals(acid, res0.getResult().getAcid());

        RpcRes_Base<ManualAccountWrapper> res1 = remoteAccess.invoke(OfrAccountService.class, "createOfrManualAccount", wrapper);
        Assert.assertNotNull(res1);
        ManualAccountWrapper wrapper1 = res1.getResult();
        Assert.assertFalse(res1.isError());
        Assert.assertNotEquals(bsaAcid, wrapper1.getBsaAcid());

        // еще раз создать тот же счет - ошибка (уже существует (совпадает с предыдущим)
        RpcRes_Base<ManualAccountWrapper> res2 = remoteAccess.invoke(OfrAccountService.class, "createOfrManualAccount", wrapper);
        Assert.assertNotNull(res2);
        Assert.assertTrue(res2.isError());
        Assert.assertEquals(res1.getResult().getBsaAcid(), res2.getResult().getBsaAcid());

        // удалить все счета
        deleteAccountCB(wrapper1.getAcid(), wrapper1.getBsaAcid(), false);
    }

    /**
     * Тест создания счетов ЦБ (в тч ОФР) по проводке
     * @throws ParseException
     */
    @Test public void testPostingCreateAccountOfr() throws ParseException, SQLException {

        long stamp = System.currentTimeMillis();

        // TODO если не работает, надо вставить очистку BSAACC от счетов ОФР, которых нет в ACCRLN
        // delete from BSAACC where ID like '70%' and BSAACO  >= (select DATE_VALUE from DB_CFG where PROP_NAME = 'START_446P_DATE')
        //      and BSAACC > CURRENT DATE and ID not in (select BSAACID from ACCRLN where RLNTYPE = '2');

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

//        pst.setAccountCredit("47407840700010060039");
        pst.setAccountCredit("");
        // BRANCH.CCY.CUSTNO.ATYPE.CUSTTYPE.TERM.GL_SEQ.CBCCN.ACC2.PLCODE.ACOD.SQ.DEALSRC.DEALID.SUBDEALID
        final String keyStringCredit = "001;RUR;00000018;643010201;18;00;PL00000003;0001;70613;16101;7302;01;K+TP;;";
//        final String keyStringCredit = "001;RUR;00000018;702040100;10;08;PL00000003;0001;70613;16101;7302;01;K+TP;;";
        deleteGlAccountWithLinks(baseEntityRepository, keyStringCredit);
        pst.setAccountKeyCredit(keyStringCredit);

//        pst.setAccountDebit("47408840700010262894");
        pst.setAccountDebit("");
        // BRANCH.CCY.CUSTNO.ATYPE.CUSTTYPE.TERM.GL_SEQ.CBCCN.ACC2.PLCODE.ACOD.SQ.DEALSRC.DEALID.SUBDEALID
        // 001;RUR;0000000018;912030101;;;XX00000007;0001;99997;;6280;01;K+TP;955304;
        final String keyStringDebit = "001;RUR;00696880;356030405;00;00;17;;47423;;;;DEALSRC;123457;SUBDEALID";
        deleteGlAccountWithLinks(baseEntityRepository, keyStringDebit);
        pst.setAccountKeyDebit(keyStringDebit);

        pst.setAmountCredit(new BigDecimal("15.99"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.RUB);
        pst.setCurrencyDebit(pst.getCurrencyCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());

        // проверка счетов
        String acidDr = "", acidCr = "";
        if (StringUtils.isEmpty(pst.getAccountDebit())) {
            acidDr = checkDefinedAccount(GLOperation.OperSide.D, operation.getAccountDebit(), operation.getAccountKeyDebit(), operation.getValueDate());
            checkAccountPl(acidDr, operation.getAccountDebit(), operation.getAccountKeyDebit(), operation.getValueDate());
        }
        if (StringUtils.isEmpty(pst.getAccountCredit())) {
            acidCr = checkDefinedAccount(GLOperation.OperSide.C, operation.getAccountCredit(), operation.getAccountKeyCredit(), operation.getValueDate());
            checkAccountPl(acidCr, operation.getAccountCredit(), operation.getAccountKeyCredit(), operation.getValueDate());
        }

        // проверка проводок
        Assert.assertEquals(getOperday().getCurrentDate(), operation.getCurrentDate());
        Assert.assertEquals(getOperday().getLastWorkdayStatus(), operation.getLastWorkdayStatus());

        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);                 // 1 проводка
        Assert.assertEquals(postList.size(), 1);

        List<Pd> pdList = getPostingPd(postList.get(0));
        Pd pdDr = pdList.get(0);
        Pd pdCr = pdList.get(1);
        Assert.assertEquals(acidDr, pdDr.getAcid());
        Assert.assertEquals(acidCr, pdCr.getAcid());

        Assert.assertTrue(pdDr.getCcy().equals(operation.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит
        Assert.assertTrue(pdCr.getCcy().equals(pdDr.getCcy()));                 // валюта одинаковый

        Assert.assertTrue(pdCr.getAmount() == operation.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCr.getAmount() == -pdDr.getAmount());       // сумма в валюте дебет - кредит

        // удалить все счета
        deleteAccountCB2(operation.getAccountCredit(), false);

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
    };

    private void deleteAccountCB(String acid, String bsaacid, boolean deleteAcid) {
        baseEntityRepository.executeNativeUpdate("delete from ACCRLN where ACID = ? and BSAACID = ?", acid, bsaacid);
        baseEntityRepository.executeNativeUpdate("delete from BSAACC where ID = ?", bsaacid);
        if (deleteAcid)
            baseEntityRepository.executeNativeUpdate("delete from ACC where ID = ?", acid);
    };

    private void checkAccountCB(String acid, String bsaacid, String rln) throws SQLException {
        DataRecord data = baseEntityRepository.selectOne("select * from ACCRLN where ACID = ? and BSAACID = ? and RLNTYPE = ?", acid, bsaacid, rln);
    };

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

    public void checkAccountPl(String acid, String bsaAcid, String keyString, Date dateOpen ) throws SQLException {
        AccountKeys keys = new AccountKeys(keyString);
        if (!keys.getGlSequence().startsWith("PL"))
            return;

        DataRecord data = baseEntityRepository.selectOne("select ACC2, PLCODE, CTYPE" +
                        " from ACCRLN where BSAACID = ? and ACID = ? and RLNTYPE = ?",
                bsaAcid, acid, '2');
        Assert.assertNotNull(data);                 // bsaAcid + acid есть в таблице ACCRLN

        DataRecord parm = remoteAccess.invoke(GLAccountRepository.class, "getAccountParams",
                keys.getAccountType(), keys.getCustomerType(), keys.getTerm(), dateOpen);
        Assert.assertNotNull(parm);                 // параметры счета в настройках

        Assert.assertEquals(parm.getString("ACC2"), data.getString("ACC2"));
        Assert.assertEquals(parm.getString("PLCODE"), data.getString("PLCODE"));
        int ctype = data.getInteger("CTYPE");
        int custype = Integer.parseInt(parm.getString("CUSTYPE").trim());
        Assert.assertEquals( ctype,  custype);
    }
}
