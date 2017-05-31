package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.operday.task.EtlStructureMonitorTask;
import ru.rbt.barsgl.ejb.entity.acc.*;
import ru.rbt.barsgl.ejb.entity.dict.AccountingType;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.dict.GLRelationAccountingType;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountController;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountCounterType;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountExcludeInterval;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountFrontPartController;
import ru.rbt.barsgl.ejb.repository.GLAccountRepository;
import ru.rbt.barsgl.ejbtest.utl.GLOperationBuilder;
import ru.rbt.barsgl.ejbtesting.test.GLPLAccountTesting;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.ExceptionUtils;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.substring;
import static ru.rbt.barsgl.ejb.entity.acc.GLAccount.RelationType.ZERO;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.RUB;
import static ru.rbt.barsgl.ejb.entity.gl.GLOperation.OperSide.C;
import static ru.rbt.barsgl.ejbtest.utl.Utl4Tests.deleteAccountByAcid;
import static ru.rbt.barsgl.ejbtest.utl.Utl4Tests.deleteGlAccountWithLinks;
import static ru.rbt.ejbcore.mapping.YesNo.N;
import static ru.rbt.ejbcore.mapping.YesNo.Y;

/**
 * Created by Ivan Sevastyanov
 * @fsd – Автоматическое определение/открытие счетов при загрузке проводок из AE<br/> Расчет лицевой части счета
 */
public class AccountOpenAePostingsTest extends AbstractRemoteTest {

    private static final Logger logger = Logger.getLogger(AccountOpenAePostingsTest.class.getName());

    @Before
    public void  initClass() {
        initCorrectOperday();
    }


  /* @Before
     public void beforeClass() {
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
        baseEntityRepository.executeUpdate("update AccountingType a set a.barsAllowed = ?1", N);

    }*/


    /**
     * Автоматическое определение/открытие счетов при загрузке проводок из AE<br/> Расчет лицевой части счета
     * @fsd
     */
    @Test
    public void testFrontPart() throws Exception {
        executeAutonomic("create table GL_ACNOCNT_tmp as (select * from GL_ACNOCNT)");

        int cntDeleted = baseEntityRepository.executeNativeUpdate("delete from GL_ACNOCNT");
        ru.rb.ucb.util.GLAccountCounterType type1 = ru.rb.ucb.util.GLAccountCounterType.PROFIT_LOSS;
        AccountKeys keys1 = AccountKeysBuilder.create()
                .withAcc2("47023")
                .withCurrency("810")
                .withCompanyCode("8237")
                .withPlCode("12333")
                .build();
        logger.info("deleted GL_ACNOCNT: " + cntDeleted);

        String frontPart = remoteAccess.invoke(GLAccountFrontPartController.class, "getNextFrontPartNumber"
                , keys1.getAccount2()
                , keys1.getCurrency()
                , keys1.getCompanyCode()
                , keys1.getPlCode());
        Assert.assertEquals(type1.getDecimalFormat().format(type1.getStartNumber()), frontPart);

        frontPart = remoteAccess.invoke(GLAccountFrontPartController.class, "getNextFrontPartNumber"
                , keys1.getAccount2()
                , keys1.getCurrency()
                , keys1.getCompanyCode()
                , keys1.getPlCode());
        Assert.assertEquals(type1.getDecimalFormat().format(type1.getStartNumber()+ type1.getIncrementValue()), frontPart);

        frontPart = remoteAccess.invoke(GLAccountFrontPartController.class, "getNextFrontPartNumber"
                , keys1.getAccount2()
                , keys1.getCurrency()
                , keys1.getCompanyCode()
                , keys1.getPlCode());
        Assert.assertEquals(type1.getDecimalFormat().format(type1.getStartNumber()+ type1.getIncrementValue() + type1.getIncrementValue()), frontPart);

        // без plcode
        GLAccountCounterType type = GLAccountCounterType.ASSET_LIABILITY;
        AccountKeys keys2 = AccountKeysBuilder.create()
                .withAcc2("47023")
                .withCurrency("810")
                .withCompanyCode("8237")
                .build();
        logger.info("deleted GL_ACNOCNT: " + cntDeleted);
        frontPart = remoteAccess.invoke(GLAccountFrontPartController.class, "getNextFrontPartNumber"
                , keys2.getAccount2()
                , keys2.getCurrency()
                , keys2.getCompanyCode()
                , keys2.getPlCode());
        Assert.assertEquals(type.getDecimalFormat().format(type.getStartNumber()), frontPart);
        frontPart = remoteAccess.invoke(GLAccountFrontPartController.class, "getNextFrontPartNumber"
                , keys2.getAccount2()
                , keys2.getCurrency()
                , keys2.getCompanyCode()
                , keys2.getPlCode());
        Assert.assertEquals(type.getDecimalFormat().format(type.getStartNumber()+ 1), frontPart);

        // исключения
        type = GLAccountCounterType.ASSET_LIABILITY;
        List<GLAccountExcludeInterval> sorted = type.getExcludes();
        Collections.sort(sorted, (t1, t2) -> t1.getEndNumber() < t2.getEndNumber() ? 1 : t1.getEndNumber() == t2.getEndNumber() ? 0 : -1);
        baseEntityRepository.executeNativeUpdate(
                "update GL_ACNOCNT set COUNT = ? where ACC2 = ? and CCYN = ? and CBCCN = ? and PLCOD IS NULL"
                , sorted.get(0).getStartNumber() - 1, keys2.getAccount2(), keys2.getCurrency()
                , keys2.getCompanyCode());
        frontPart = remoteAccess.invoke(GLAccountFrontPartController.class, "getNextFrontPartNumber"
                , keys2.getAccount2()
                , keys2.getCurrency()
                , keys2.getCompanyCode()
                , keys2.getPlCode());
        Assert.assertEquals(type.getDecimalFormat().format(sorted.get(0).getEndNumber() + 1), frontPart);

        executeAutonomic("delete from GL_ACNOCNT");
        executeAutonomic("insert into GL_ACNOCNT select * from GL_ACNOCNT_tmp");
        executeAutonomic("drop table GL_ACNOCNT_tmp");
    }

    /**
     * Автоматическое определение/открытие счетов при загрузке проводок из AE<br/>Расчет ключевого разряда
     * @fsd
     * Расчет ключевого разряда
     */
    @Test
    public void testCalculateKeyDigit() {
//        String account = "20202756K00010000053";
        String account = "40702C45K20010000005";
        logger.info(account);
        account = remoteAccess.invoke(GLAccountFrontPartController.class, "calculateKeyDigit", account, "0040");
        logger.info(account);
        Assert.assertTrue(account.matches("\\d{5}.{3}\\d{12}"));
    }

    /**
     * Тест масок ключей для открытия счета
     *
     */
    @Test
    public void testAccountKeyPattern() {
        // BRANCH.CCY.CUSTNO.ATYPE.CUSTTYPE.TERM.GL_SEQ.CBCCN.ACC2.PLCODE.ACOD.SQ.DEALSRC.DEALID.SUBDEALID
        Pattern patternAccountKey = AccountKeys.getPattern();

        Assert.assertTrue(patternAccountKey.matcher("001;RUR;00400044;911020101;19;03;0000000535;0001;96307;;6202;01;K+TP;955514;").matches());
        Assert.assertTrue(patternAccountKey.matcher("001;EUR;00012345;123456789;100;05;123456;0001;70613;16101;7301;01;src;deal;subdeal").matches());
        Assert.assertTrue(patternAccountKey.matcher("001;EUR;00012345;123456789;100;05;XX76543210;;;;;;;;").matches());
        Assert.assertTrue(patternAccountKey.matcher("001;EUR;00012345;123456789;;;XX;0001;70613;16101;7301;01;src;deal;subdeal").matches());
        Assert.assertTrue(patternAccountKey.matcher("001;RUR;00000018;643010201;;;PL00000003;0001;70613;16101;7302;01;K+TP;;").matches());
        Assert.assertTrue(patternAccountKey.matcher("001;EUR;00012345;123456789;;;123456;;;;;;;;").matches());
        Assert.assertTrue(patternAccountKey.matcher("001;EUR;00012345;123456789;;;123456;;;;;;;;;;").matches());

        Assert.assertFalse(patternAccountKey.matcher("001;EUR;012345;123456789;100;05;123456;0001;70613;16101;7301;01;src;deal;subdeal").matches());
        Assert.assertFalse(patternAccountKey.matcher("001;954;00012345;123456789;100;05;123456;0001;70613;16101;7301;01;src;deal;subdeal").matches());
        Assert.assertFalse(patternAccountKey.matcher("001;EUR;00012345;123456789;100;05;123456;;;;;").matches());
        Assert.assertFalse(patternAccountKey.matcher("001;EUR;;123456789;100;05;;;;;;;;;").matches());
        Assert.assertFalse(patternAccountKey.matcher("001;EUR;00012345;123456789;100;05;123456;0001;70613;16101;7301;01;").matches());
    }

    /**
     * Тест ошибок формата в строке ключей
     */
    @Test
    public void testAccountKeyFormat() {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        final String keyCredit = AccountKeysBuilder.create()
                .withBranch("001")
                .withCurrency("")
                .withCustomerNumber("00457533")
                .withAccountType("0123456789")
                .withGlSequence("123")
                .withAcc2("40817")
                .build().toString();
        pst.setAccountKeyCredit(keyCredit);
//        pst.setAccountKeyCredit("001..00457533.0123456789...123..40817.....");
        final String keyDebit = AccountKeysBuilder.create()
                .withBranch("001")
                .withCurrency("USD")
                .withCustomerNumber("01141299")
                .withAccountType("0123456789")
                .withGlSequence("124")
                .withAcc2("40817")
                .build().toString();
        pst.setAccountKeyDebit(keyDebit);
//        pst.setAccountKeyDebit("001.USD.01141299.0123456789...124..40817......");

        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNull(operation);                                               // операция не должна быть создана

        pst = (EtlPosting) baseEntityRepository.findById(pst.getClass(), pst.getId());
        Assert.assertTrue(pst.getErrorCode() == 1);
        String errorMessage = pst.getErrorMessage();
        Assert.assertNotEquals(errorMessage, "SUCCESS");

        Assert.assertTrue(errorMessage.contains("'16'"));
        Assert.assertTrue(errorMessage.contains("'15'"));
    }

    /**
     * Тест генерации счета ЦБ
     */
    @Test
    public void testGenerateAccountNumber() {
        final String keyString
                = AccountKeysBuilder.create()
                .withBranch("046")
                .withCurrency("AUD")
                .withCustomerNumber("00012345")
                .withAccountType("643010101")
                .withCustomerType("00")
                .withTerm("00") //05
                .withGlSequence("123457")
                .withAcc2("70613")
                .withAccountCode("7301")
                .withAccSequence("01")
              //  .withPlCode("25102")
                .build().toString();
//                "046.AUD.00012345.643010101.100.05.123457..70613..7301.01...";
        AccountKeys keys = new AccountKeys(keyString);

        String accountNumber = remoteAccess.invoke(GLAccountController.class, "getAccountNumber",
                C, getOperday().getCurrentDate(), keys);
        System.out.println(accountNumber);
        Assert.assertEquals(20, accountNumber.length());
        Assert.assertEquals(keys.getAccount2(), accountNumber.substring(0, 5));
        // TODO добавить проверки CCY, CBCCN
        // TODO добавить проверку дли счетов доходов / расходов
        // TODO добавить проверку при совпадении номера счета с существующим

    }

    /**
     * Тест создания счетов ЦБ по проводке
     * @throws ParseException
     */
    @Test public void testPostingCreateAccount() throws ParseException, SQLException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

//        pst.setAccountCredit("47407840700010060039");
        pst.setAccountCredit("");
        // BRANCH.CCY.CUSTNO.ATYPE.CUSTTYPE.TERM.GL_SEQ.CBCCN.ACC2.PLCODE.ACOD.SQ.DEALSRC.DEALID.SUBDEALID
        final String keyStringCredit = "001;USD;00674113;356030405;00;00;17;;47423;;1871;05;DEALSRC;123456;SUBDEALID";  //   00114240
        Assert.assertTrue(isEmpty(new AccountKeys(keyStringCredit).getPlCode()));
        deleteGlAccountWithLinks(baseEntityRepository, keyStringCredit);
        pst.setAccountKeyCredit(keyStringCredit);

//        pst.setAccountDebit("47408840700010262894");
        pst.setAccountDebit("");
        // BRANCH.CCY.CUSTNO.ATYPE.CUSTTYPE.TERM.GL_SEQ.CBCCN.ACC2.PLCODE.ACOD.SQ.DEALSRC.DEALID.SUBDEALID
        // 001;RUR;0000000018;912030101;;;XX00000007;0001;99997;;6280;01;K+TP;955304;
        final String keyStringDebit = "001;USD;00448806;356030405;00;00;17;;47423;;;;DEALSRC;123457;SUBDEALID";
        Assert.assertTrue(isEmpty(new AccountKeys(keyStringDebit).getPlCode()));
        deleteGlAccountWithLinks(baseEntityRepository, keyStringDebit);
        pst.setAccountKeyDebit(keyStringDebit);

        pst.setAmountCredit(new BigDecimal("13.99"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.USD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());

        // проверка счетов
        String acidDr = "", acidCr = "";
        if (isEmpty(pst.getAccountDebit()))
            acidDr = checkDefinedAccount(GLOperation.OperSide.D, operation.getAccountDebit(), operation.getAccountKeyDebit());
        if (isEmpty(pst.getAccountCredit()))
            acidCr = checkDefinedAccount(C, operation.getAccountCredit(), operation.getAccountKeyCredit());

        final String accountByOper = "from GLAccount a where a.operation = ?1 and a.operSide = ?2";
        GLAccount accountDt = (GLAccount) baseEntityRepository.selectOne(GLAccount.class, accountByOper,
                operation, GLOperation.OperSide.D);
        Assert.assertNotNull(accountDt);
        Assert.assertEquals(GLAccount.RelationType.FOUR.getValue(), accountDt.getRelationType());

        GLAccount accountCt = (GLAccount) baseEntityRepository.selectOne(GLAccount.class, accountByOper,
                operation, C);
        Assert.assertNotNull(accountCt);
        Assert.assertEquals(GLAccount.RelationType.FOUR.getValue(), accountCt.getRelationType());


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

    }

    /**
     * Тест создания счетов ЦБ (майдас-опц.) по проводке в случае если передены и ключи и счет
     * @throws ParseException
     */
    @Test public void testPostingCreateAccountWithKeys() throws ParseException, SQLException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        final String bsaacidCredit = "47407840020010000083";//"47407840700010060039";

        deleteAllReleations(bsaacidCredit);

        pst.setAccountCredit(bsaacidCredit);
        // BRANCH.CCY.CUSTNO.ATYPE.CUSTTYPE.TERM.GL_SEQ.CBCCN.ACC2.PLCODE.ACOD.SQ.DEALSRC.DEALID.SUBDEALID
        final String keyStringCredit = "001;USD;00114240;501020501;00;00;17;;;;;;DEALSRC;123456;SUBDEALID";
        deleteGlAccountWithLinks(baseEntityRepository, keyStringCredit);
        pst.setAccountKeyCredit(keyStringCredit);

        final String bsaacidDebit = "47408840020010000121";//"47408840700010262894";
        deleteAllReleations(bsaacidDebit);

        pst.setAccountDebit(bsaacidDebit);
        // BRANCH.CCY.CUSTNO.ATYPE.CUSTTYPE.TERM.GL_SEQ.CBCCN.ACC2.PLCODE.ACOD.SQ.DEALSRC.DEALID.SUBDEALID
        // 001;RUR;0000000018;912030101;;;XX00000007;0001;99997;;6280;01;K+TP;955304;
        final String keyStringDebit = "001;USD;00448806;351020301;00;00;17;;;;;;DEALSRC;123457;SUBDEALID";
        deleteGlAccountWithLinks(baseEntityRepository, keyStringDebit);
        pst.setAccountKeyDebit(keyStringDebit);

        pst.setAmountCredit(new BigDecimal("13.99"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.USD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
//        Assert.assertEquals(OperState.POST, operation.getState());

        Assert.assertEquals(bsaacidCredit, operation.getAccountCredit());
        Assert.assertEquals(bsaacidDebit, operation.getAccountDebit());

        GLAccount accountCt = findGlAccount(bsaacidCredit);
        Assert.assertNotNull(accountCt);
        Assert.assertEquals(operation, accountCt.getOperation());

        GLAccount accountDt = findGlAccount(bsaacidDebit);
        Assert.assertNotNull(accountDt);
        Assert.assertEquals(operation, accountDt.getOperation());

        // проверка счетов
        String acidDr = checkDefinedAccount(GLOperation.OperSide.D, operation.getAccountDebit(), operation.getAccountKeyDebit(), ZERO);
        String acidCr = checkDefinedAccount(C, operation.getAccountCredit(), operation.getAccountKeyCredit(), ZERO);

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

        final String accrlnSql = "select rlntype from accrln where bsaacid = ?";
        Assert.assertEquals(ZERO.getValue()
                , baseEntityRepository.selectOne(accrlnSql, bsaacidCredit).getString("rlntype"));
        Assert.assertEquals(ZERO.getValue()
                , baseEntityRepository.selectOne(accrlnSql, bsaacidDebit).getString("rlntype"));

        List<GLAccount> accountsCreated = baseEntityRepository.select(GLAccount.class, "from GLAccount a where a.operation = ?1", operation);
        Assert.assertTrue(accountsCreated.stream()
                .map(acc -> acc.getId() + ":" + acc.getRelationType()).collect(Collectors.joining(";"))
                ,accountsCreated.stream().allMatch(acc -> acc.getRelationTypeEnum() == ZERO));

        // теперь, может быть ситуация, когда у нас в GL_ACC счета нет, а в BSAACC и в ACCRLN счет есть
        // удаляем из GL_ACC
        Assert.assertEquals(1, baseEntityRepository.executeNativeUpdate("delete from gl_acc where bsaacid = ?", bsaacidDebit));
        Assert.assertEquals(1, baseEntityRepository.executeNativeUpdate("delete from gl_acc where bsaacid = ?", bsaacidCredit));

        operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());

        accountsCreated = baseEntityRepository.select(GLAccount.class, "from GLAccount a where a.operation = ?1", operation);
        Assert.assertTrue(accountsCreated.stream().allMatch(acc -> acc.getRelationTypeEnum() == ZERO));

    }

    /**
     *
     * @param bsaacid
     * @return
     */
    private void deleteAllReleations(String bsaacid) {
        logger.info("deleted gl_acc: " + baseEntityRepository.executeNativeUpdate("delete from gl_acc where bsaacid = ?", bsaacid));
        logger.info("deleted acc: " + baseEntityRepository.executeNativeUpdate("delete from acc a where a.id in (select r.acid from accrln r where r.bsaacid = ?)", bsaacid));
        logger.info("deleted accrln: " + baseEntityRepository.executeNativeUpdate("delete from accrln where bsaacid = ?", bsaacid));
        logger.info("deleted BSAACC: " + baseEntityRepository.executeNativeUpdate("delete from BSAACC where id = ?", bsaacid));
    }

    private static GLAccount findGlAccount(String bsaacid) {
        // ненавижу weblogic, left join fetch не работает
        GLAccount account = (GLAccount) baseEntityRepository.selectFirst(GLAccount.class,
                "from GLAccount a left join fetch a.operation o where a.bsaAcid = ?1", bsaacid);
        if (null != account) {
            GLOperation operation = (GLOperation) baseEntityRepository.selectFirst(GLOperation.class, "from GLOperation o where o.id = (select a.operation.id from GLAccount a where a.id = ?1)", account.getId());
            if (null != operation) {
                account.setOperation(operation);
            }
        }
        return account;
    }

    /**
     * Автоматическое определение/открытие счетов при загрузке проводок из AE
     * Обработка ситуации если счета Майдас не найден
     */
    @Test
    public void testProcessAccountCreateNotExistsXX() throws SQLException {
        long stamp = System.currentTimeMillis();

        // убираем все не наши WTAC
        baseEntityRepository.executeUpdate("update GLOperation o set o.state = ?1 where o.state = ?2 and o.valueDate IN (?3 , ?4)"
                , OperState.ERCHK, OperState.WTAC, getOperday().getCurrentDate(), getOperday().getLastWorkingDay());

        EtlPackage pkg = newPackage(stamp, "MIDAS");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        pst.setCurrencyCredit(RUB);
        pst.setCurrencyDebit(pst.getCurrencyCredit());

        pst.setAccountCredit("");
        pst.setAccountDebit("");

        final String acctype= "643010101";
        DataRecord record = Optional.ofNullable(baseEntityRepository.selectFirst("select * from gl_actparm where acctype = ?", acctype)).orElseThrow(() -> new RuntimeException("gl_actparm not found"));

        final AccountKeys acCt
                = AccountKeysBuilder.create()
                .withBranch("001").withCurrency(pst.getCurrencyCredit().getCurrencyCode()).withCustomerNumber("00000018")
                .withAccountType(acctype).withCustomerType(record.getString("CUSTYPE").trim()).withTerm("00").withPlCode(record.getString("PLCODE").trim())
                .withGlSequence("PL").withAcc2(record.getString("ACC2").trim()).withAccountCode(record.getString("acod").trim()).withAccSequence(record.getString("ac_sq").trim())
                .build();
        final AccountKeys acDt
                = AccountKeysBuilder.create()
                .withBranch("001").withCurrency(pst.getCurrencyDebit().getCurrencyCode()).withCustomerNumber("00000018")
                .withAccountType(acctype).withCustomerType(acCt.getCustomerType()).withTerm("00").withPlCode(acCt.getPlCode())
                .withGlSequence("XX").withAcc2(acCt.getAccount2()).withAccountCode(acCt.getAccountCode()).withAccSequence(acCt.getAccSequence())
                .build();
        String accountKeyCt = acCt.toString();
        String accountKeyDt = acDt.toString();
        deleteGlAccountWithLinks(baseEntityRepository, accountKeyCt);
        deleteGlAccountWithLinks(baseEntityRepository, accountKeyDt);

        final AccountKeys keys1 = remoteAccess.invoke(GLAccountController.class
                , "fillAccountKeysMidas", C, pst.getValueDate(), acCt);
        if (!isEmpty(keys1.getAccountMidas())) {
            deleteAccountByAcid(baseEntityRepository, keys1.getAccountMidas());
        }

        //todo сообщение 'Счет по дебету задан ключом ACCTYPE=643010101, CUSTNO=00000018, ACOD=7301, SQ=01
        // , DEALID=, PLCODE=16101, GL_SEQ=XX некорректно, PLCODE в таблице GL_ACTPARM д.б.пустым'
        // , источник ru.rbt.barsgl.ejb.integr.acc.GLAccountController:595
        Throwable exception = null;
        try {
            remoteAccess.invoke(GLAccountController.class
                    , "fillAccountKeysMidas", GLOperation.OperSide.D, pst.getValueDate(), acDt);
        }catch(Throwable e){
            exception = e;
        }
        Assert.assertNotNull(exception);
        ValidationError error = ExceptionUtils.findException(exception, ValidationError.class);
        Assert.assertNotNull(error);
        Assert.assertEquals(ErrorCode.GL_SEQ_XX_KEY_WITH_DB_PLCODE, error.getCode());

    }

    /**
     * Автоматическое ОПРЕДЕЛЕНИЕ счетов при загрузке проводок из AE (GLSEQ = "GL")
     * Обработка ситуации когда счета Майдас вообще нет физически
     */
    @Test
    public void testProcessAccountCreateMidasNotExistsGL() throws SQLException {
        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "NOMIDAS");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        pst.setCurrencyCredit(RUB);
        pst.setCurrencyDebit(pst.getCurrencyCredit());

        pst.setAccountCredit("40702810400010002676");
        pst.setAccountDebit("");

        final AccountKeys acDt
                = AccountKeysBuilder.create()
                .withBranch("001").withCurrency(pst.getCurrencyDebit().getCurrencyCode()).withCustomerNumber("00000018")
                .withAccountType("643010101").withCustomerType("23").withTerm("05").withPlCode("17101")
                .withGlSequence("GL").withAcc2("30102").withCompanyCode("0001")
                .build();
        String accountKeyDt = acDt.toString();
        deleteGlAccountWithLinks(baseEntityRepository, accountKeyDt);

        pst.setAccountKeyDebit(accountKeyDt);

        pst.setAmountCredit(new BigDecimal("13.99"));
        pst.setAmountDebit(pst.getAmountCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation.getId());
        Assert.assertTrue(0 < operation.getId());

        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertTrue(!isEmpty(operation.getAccountKeyDebit()));

        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());

        Assert.assertTrue(!StringUtils.isEmpty(operation.getAccountDebit()));

    }

    /**
     * Тест генерации сиквенса Midas для счетов с источником FLEX
     */
    @Test
    public void testChangeMidasSQFlex() {
        //на проде все записи таблицы закрыты датой 07.12.2016, что вызывает непрохождение теста
        baseEntityRepository.executeNativeUpdate("update gl_sqparam set dte = null");

        String dealSrc = "FC12_CL";

        final AccountKeys keys0 = new AccountKeys(
                // BRANCH.CCY.CUSTNO.ATYPE.CUSTTYPE.TERM.GL_SEQ.CBCCN.ACC2.PLCODE.ACOD.SQ.DEALSRC.DEALID.SUBDEALID
                "001;RUR;00107294;891060100;18;;0000001899;0001;91316;;;;;;");
        keys0.setDealSource(dealSrc);
        keys0.setDealId("FL_" + System.currentTimeMillis());

        DataRecord data = remoteAccess.invoke(GLAccountRepository.class
                , "getDealSQParams", dealSrc, getOperday().getCurrentDate());
        Assert.assertNotNull(data);
        deleteSQvalue(keys0);

        // первая сделка - SQ задан, не должен измениться
        String sq0 = "78";
        keys0.setAccSequence(sq0);
        final AccountKeys keys1 = remoteAccess.invoke(GLAccountController.class
                , "fillAccountKeysMidas", C, getOperday().getCurrentDate(), keys0);
        Assert.assertEquals(sq0, keys1.getAccSequence());
        Assert.assertEquals(sq0, substring(keys1.getAccountMidas(), 15, 17));

        // снова первая сделка - SQ не задан, получить предыдущий SQ
        keys0.setAccSequence("");
        final AccountKeys keys2 = remoteAccess.invoke(GLAccountController.class
                , "fillAccountKeysMidas", C, getOperday().getCurrentDate(), keys0);
        Assert.assertEquals(sq0, keys2.getAccSequence());

        // и снова первая сделка - SQ другой, ошибка
        keys0.setAccSequence("79");
        try {
            final AccountKeys keys6 = remoteAccess.invoke(GLAccountController.class
                    , "fillAccountKeysMidas", C, getOperday().getCurrentDate(), keys0);
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(true);
        }

        // вторая сделка, SQ не задан - взять из параметров
        keys0.setDealId("FL_" + System.currentTimeMillis());
        keys0.setAccSequence("");
        final AccountKeys keys5 = remoteAccess.invoke(GLAccountController.class
                , "fillAccountKeysMidas", C, getOperday().getCurrentDate(), keys0);
        Assert.assertEquals("00", keys5.getAccSequence());

        // пустой номер сделки, SQ задан - не должен измениться
        keys0.setDealId("");
        String sq3 = "72";
        keys0.setAccSequence(sq3);
        final AccountKeys keys3 = remoteAccess.invoke(GLAccountController.class
                , "fillAccountKeysMidas", C, getOperday().getCurrentDate(), keys0);
        Assert.assertEquals(sq3, keys3.getAccSequence());

        // пустой номер сделки, SQ не задан - взять из параметров
        keys0.setAccSequence("");
        final AccountKeys keys4 = remoteAccess.invoke(GLAccountController.class
                , "fillAccountKeysMidas", C, getOperday().getCurrentDate(), keys0);
        Assert.assertEquals("00", keys4.getAccSequence());

        // новая сделка, ACOD и SQ заданы - не меняются, норма
        keys0.setDealId("FL_" + System.currentTimeMillis());
        keys0.setAccountCode("6603");
        keys0.setAccSequence("00");
        final AccountKeys keys7 = remoteAccess.invoke(GLAccountController.class
                , "fillAccountKeysMidas", C, getOperday().getCurrentDate(), keys0);
        Assert.assertEquals("00", keys7.getAccSequence());

        // новая сделка, ACOD и SQ заданы неверно - ошибка
        keys0.setDealId("FL_" + System.currentTimeMillis());
        keys0.setAccountCode("91317");
        try {
            final AccountKeys keys8 = remoteAccess.invoke(GLAccountController.class
                    , "fillAccountKeysMidas", C, getOperday().getCurrentDate(), keys0);
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(true);
        }

        // новая сделка, ACOD задан, SQ - не задан - ошибка
        keys0.setDealId("FL_" + System.currentTimeMillis());
        keys0.setAccSequence("01");
        try {
            final AccountKeys keys9 = remoteAccess.invoke(GLAccountController.class
                    , "fillAccountKeysMidas", C, getOperday().getCurrentDate(), keys0);
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * Тест генерации сиквенса Midas для счетов с источником не FLEX
     */
    @Test
    public void testChangeMidasSQNotFlex() {
        //на проде все записи таблицы закрыты датой 07.12.2016, что вызывает непрохождение теста
        baseEntityRepository.executeNativeUpdate("update gl_sqparam set dte = null");

        final AccountKeys keys0 = new AccountKeys(
                // BRANCH.CCY.CUSTNO.ATYPE.CUSTTYPE.TERM.GL_SEQ.CBCCN.ACC2.PLCODE.ACOD.SQ.DEALSRC.DEALID.SUBDEALID
                "001;RUR;00696880;;;;0000034599;0001;52602;;;;;;");
        String accTtype = "461010101";
        String custType = "18";
        String term = "";
        keys0.setAccountType(accTtype);
        keys0.setCustomerType(custType);
        keys0.setTerm(term);
        deleteSQvalue(keys0);

        DataRecord data2 = remoteAccess.invoke(GLAccountRepository.class
                , "getAccountParams", accTtype, custType, term, getOperday().getCurrentDate());
        Assert.assertNotNull(data2);
        String sq = format("%02d", data2.getShort("SQ"));

        String acod = "5731";
        // первая слелка, источник MZO -  SQ задан
        keys0.setDealSource("MZO");
        keys0.setDealId("MZO_" + System.currentTimeMillis());
        keys0.setSubDealId("SUB_" + System.currentTimeMillis());
        String sq0 = "81";
        keys0.setAccSequence(sq0);
        final AccountKeys keys1 = remoteAccess.invoke(GLAccountController.class
                , "fillAccountKeysMidas", C, getOperday().getCurrentDate(), keys0);
        Assert.assertEquals(sq0, keys1.getAccSequence());

        // первая слелка, источник MZO - SQ не задан, вернуть предыдущий
        keys0.setDealSource("MZO");
        keys0.setDealId("MZO_" + System.currentTimeMillis());
        keys0.setAccSequence("");
        final AccountKeys keys2 = remoteAccess.invoke(GLAccountController.class
                , "fillAccountKeysMidas", C, getOperday().getCurrentDate(), keys0);
        Assert.assertEquals(sq0, keys2.getAccSequence());

        // вторая слелка, источник ARMPRO, SQ задан - не меняется
        keys0.setDealSource("ARMPRO");
        keys0.setDealId("ARM_" + System.currentTimeMillis());
        String sq3 = "82";
        keys0.setAccSequence(sq3);
        final AccountKeys keys3 = remoteAccess.invoke(GLAccountController.class
                , "fillAccountKeysMidas", C, getOperday().getCurrentDate(), keys0);
        Assert.assertEquals(sq, keys3.getAccSequence());

        // вторая слелка, источник ARMPRO, SQ не задан - вернуть из параметров
        keys0.setDealSource("ARMPRO");
        keys0.setAccSequence("");
        final AccountKeys keys4 = remoteAccess.invoke(GLAccountController.class
                , "fillAccountKeysMidas", C, getOperday().getCurrentDate(), keys0);
        Assert.assertEquals(sq, keys4.getAccSequence());

        // вторая слелка, K+TP, SQ задан - остается без изменения
        keys0.setDealSource("K+TP");
        String sq5 = "83";
        keys0.setAccSequence(sq5);
        final AccountKeys keys5 = remoteAccess.invoke(GLAccountController.class
                , "fillAccountKeysMidas", C, getOperday().getCurrentDate(), keys0);
        Assert.assertEquals(sq5, keys5.getAccSequence());

        // вторая слелка, K+TP, SQ не задан - вернуть из параметров
        keys0.setDealSource("K+TP");
        keys0.setAccSequence("");
        final AccountKeys keys6 = remoteAccess.invoke(GLAccountController.class
                , "fillAccountKeysMidas", C, getOperday().getCurrentDate(), keys0);
        Assert.assertEquals(sq, keys6.getAccSequence());

    }

        /**
         * Тест создания счетов ЦБ по проводке из FLEX с подменой сиквенса Midas
         * @throws ParseException
         */
    @Test public void testPostingCreateAccountFlex() throws ParseException, SQLException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        String dealId = "FL_" + System.currentTimeMillis();
        pst.setAccountCredit("");
        // BRANCH.CCY.CUSTNO.ATYPE.CUSTTYPE.TERM.GL_SEQ.CBCCN.ACC2.PLCODE.ACOD.SQ.DEALSRC.DEALID.SUBDEALID
        final String keyStringCredit = "001;RUR;00208934;161020100;18;06;0000001645;0001;45205;;;;FC12_CL;" + dealId + ";00204487RURSN0100001"; // 00100198
        deleteGlAccountWithLinks(baseEntityRepository, keyStringCredit);
        deleteSQvalue(new AccountKeys(keyStringCredit));
        pst.setAccountKeyCredit(keyStringCredit);

        pst.setAccountDebit("");
        // BRANCH.CCY.CUSTNO.ATYPE.CUSTTYPE.TERM.GL_SEQ.CBCCN.ACC2.PLCODE.ACOD.SQ.DEALSRC.DEALID.SUBDEALID
        final String keyStringDebit = "001;RUR;00208934;161020200;18;10;0000001898;0001;45812;;;;FC12_CL;" + dealId + ";00204487RURSN0100001"; // 00100198
        deleteGlAccountWithLinks(baseEntityRepository, keyStringDebit);
        deleteSQvalue(new AccountKeys(keyStringDebit));
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
        if (isEmpty(pst.getAccountDebit()))
            acidDr = checkDefinedAccount(GLOperation.OperSide.D, operation.getAccountDebit(), operation.getAccountKeyDebit());
        if (isEmpty(pst.getAccountCredit()))
            acidCr = checkDefinedAccount(C, operation.getAccountCredit(), operation.getAccountKeyCredit());

        GLAccount accountDr = getGLAccount(operation.getAccountDebit());
        GLAccount accountCr = getGLAccount(operation.getAccountCredit());
        Assert.assertNotEquals("00", accountCr.getAccountSequence());
        Assert.assertEquals(accountCr.getAccountSequence(), accountDr.getAccountSequence());

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

    }

    /**
     * Тест создания счетов ЦБ для пакета из FLEX с подменой сиквенса Midas
     * @throws ParseException
     */
    @Test public void testPackageCreateAccountFlex() throws ParseException, SQLException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setValueDate(getOperday().getCurrentDate());

        String dealId1 = "FL_" + System.currentTimeMillis();
        pst1.setAccountCredit("");
        // BRANCH.CCY.CUSTNO.ATYPE.CUSTTYPE.TERM.GL_SEQ.CBCCN.ACC2.PLCODE.ACOD.SQ.DEALSRC.DEALID.SUBDEALID
        final String keyStringCredit = "001;RUR;00204487;161020100;18;06;0000001645;0001;45205;;;;FC12_CL;" + dealId1 + ";00204487RURSN0100001";
        deleteGlAccountWithLinks(baseEntityRepository, keyStringCredit);
        deleteSQvalue(new AccountKeys(keyStringCredit));
        pst1.setAccountKeyCredit(keyStringCredit);

        String dealId2 = "FL_" + System.currentTimeMillis();
        pst1.setAccountDebit("");
        // BRANCH.CCY.CUSTNO.ATYPE.CUSTTYPE.TERM.GL_SEQ.CBCCN.ACC2.PLCODE.ACOD.SQ.DEALSRC.DEALID.SUBDEALID
        final String keyStringDebit = "001;RUR;00204487;161020200;8;10;0000001898;0001;45804;;;;FC12_CL;" + dealId2 + ";00204487RURSN0100001";
        deleteGlAccountWithLinks(baseEntityRepository, keyStringDebit);
        deleteSQvalue(new AccountKeys(keyStringDebit));
        pst1.setAccountKeyDebit(keyStringDebit);

        pst1.setAmountCredit(new BigDecimal("15.99"));
        pst1.setAmountDebit(pst1.getAmountCredit());
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(pst1.getCurrencyCredit());
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        // во второй операции меняем меятами номер сделки по дебету и кредиту
        EtlPosting pst2 = newPosting(stamp, pkg);
        pst2.setValueDate(pst1.getValueDate());
        pst2.setAccountKeyCredit(pst1.getAccountKeyDebit());
        pst2.setAccountKeyDebit(pst1.getAccountKeyCredit());
        pst2.setAmountCredit(new BigDecimal("19.99"));
        pst2.setAmountDebit(pst2.getAmountCredit());
        pst2.setCurrencyCredit(pst1.getCurrencyCredit());
        pst2.setCurrencyDebit(pst2.getCurrencyCredit());
        pst2 = (EtlPosting) baseEntityRepository.save(pst2);

        remoteAccess.invoke(EtlStructureMonitorTask.class, "processEtlPackage", pkg);

        GLOperation oper1 = getOperation(pst1.getId());
        Assert.assertEquals(OperState.POST, oper1.getState());
        Assert.assertTrue(0 < oper1.getId());
        GLOperation oper2 = getOperation(pst2.getId());
        Assert.assertEquals(OperState.POST, oper2.getState());
        Assert.assertTrue(0 < oper2.getId());

        GLAccount accountDr1 = getGLAccount(oper1.getAccountDebit());
        GLAccount accountCr1 = getGLAccount(oper1.getAccountCredit());
        GLAccount accountDr2 = getGLAccount(oper2.getAccountDebit());
        GLAccount accountCr2 = getGLAccount(oper2.getAccountCredit());

        Assert.assertNotEquals("00", accountDr1.getAccountSequence());
        Assert.assertEquals(accountDr1.getAccountSequence(), accountCr2.getAccountSequence());
        Assert.assertNotEquals("00", accountCr1.getAccountSequence());
        Assert.assertEquals(accountCr1.getAccountSequence(), accountDr2.getAccountSequence());
    }

    /**
     * При попытке открыть PL счет и отсутствуещем счете Майдас - создаем счет Майдас
     * @throws SQLException
     */
    @Test public void testPLAccountOpenByAccountTypeError() throws SQLException {

        GLOperation operation = (GLOperation) baseEntityRepository.findById(GLOperation.class
                , baseEntityRepository.selectFirst("select gloid from gl_oper order by 1 desc").getLong("gloid"));

        AccountingType accountingType = Optional
                .ofNullable((AccountingType) baseEntityRepository.findById(AccountingType.class, "643010101"))
                .orElseThrow(() -> new RuntimeException("GL_ACTNAME is not found"));
        baseEntityRepository.executeUpdate("update AccountingType a set a.barsAllowed = ?1 where a.id = ?2"
                , N, accountingType.getId());

        final String acctype = "643010101";
        DataRecord record = Optional.ofNullable(baseEntityRepository.selectFirst("select * from gl_actparm where acctype = ?", acctype))
                .orElseThrow(() -> new RuntimeException("ACCTYPE = '" + acctype + "' not found"));

        AccountKeys acCt
                = AccountKeysBuilder.create()
                .withBranch("001").withCurrency("RUR").withCustomerNumber("00000018")
                .withAccountType(acctype).withCustomerType(record.getString("CUSTYPE").trim()).withTerm("00").withPlCode(record.getString("PLCODE").trim())
                .withGlSequence("PL").withAcc2(record.getString("ACC2").trim()).withAccountCode(record.getString("acod").trim()).withAccSequence(record.getString("ac_sq").trim())
                .build();
        acCt = remoteAccess.invoke(GLAccountController.class, "fillAccountOfrKeysMidas"
                , C, getOperday().getCurrentDate(), acCt);
        acCt = remoteAccess.invoke(GLAccountController.class, "fillAccountOfrKeys"
                , C, getOperday().getCurrentDate(), acCt);

        Assert.assertFalse(StringUtils.isEmpty(acCt.getAccountMidas()));
        cleanAccountsByMidas(acCt.getAccountMidas());

        operation = GLOperationBuilder.create(operation).withValueDate(getOperday().getCurrentDate()).build();
        // майдас счета нет
        Assert.assertFalse(remoteAccess.invoke(GLAccountRepository.class, "checkMidasAccountExists"
                , acCt.getAccountMidas(), operation.getValueDate()));

        String bsaacid = remoteAccess.invoke(GLPLAccountTesting.class, "getAccount"
                    , operation, C, acCt);
        Assert.assertTrue(remoteAccess.invoke(GLAccountRepository.class, "checkMidasAccountExists"
                , acCt.getAccountMidas(), operation.getValueDate()));
    }

    /**
     * создание PL счета на основе наличия флага "Y" в таблице GL_ACTNAME.PL_ACT
     * в этом случае создается счет в т.ч в GL_ACC
     * @throws SQLException
     */
    @Test public void testPLAccountOpenByAccountTypeOwnGLAccount() throws SQLException {

        GLOperation operation = (GLOperation) baseEntityRepository.findById(GLOperation.class
                , baseEntityRepository.selectFirst("select gloid from gl_oper order by 1 desc").getLong("gloid"));

        final String acctype= "643010101";
        DataRecord record = Optional.ofNullable(baseEntityRepository
                .selectFirst("select * from gl_actparm where acctype = ?", acctype))
                .orElseThrow(() -> new RuntimeException("gl_actparm not found"));

        AccountingType accountingType = Optional
                .ofNullable((AccountingType) baseEntityRepository.findById(AccountingType.class, "643010101"))
                .orElseThrow(() -> new RuntimeException("GL_ACTNAME is not found"));
        if (N == accountingType.getBarsAllowed()) {
            baseEntityRepository.executeUpdate("update AccountingType a set a.barsAllowed = ?1 where a.id = ?2"
                    , Y, accountingType.getId());
        }

        AccountKeys acCt
                = AccountKeysBuilder.create()
                .withBranch("001").withCurrency("RUR").withCustomerNumber("00000018")
                .withAccountType(acctype).withCustomerType(record.getString("CUSTYPE").trim()).withTerm("00")
                .withPlCode(record.getString("PLCODE").trim()).withGlSequence("PL")
                .withAcc2(record.getString("ACC2").trim()).withAccountCode(record.getString("acod").trim())
                .withAccSequence(record.getString("ac_sq").trim())
                .build();
        acCt = remoteAccess.invoke(GLAccountController.class, "fillAccountOfrKeysMidas"
                , C, getOperday().getCurrentDate(), acCt);
        acCt = remoteAccess.invoke(GLAccountController.class, "fillAccountOfrKeys"
                , C, getOperday().getCurrentDate(), acCt);

        cleanAccountsByMidas(acCt.getAccountMidas());

        String bsaacid = remoteAccess.invoke(GLPLAccountTesting.class, "getAccount"
                , GLOperationBuilder.create(operation).withValueDate(getOperday().getCurrentDate()).build(), C, acCt);
        Assert.assertTrue(bsaacid, !StringUtils.isEmpty(bsaacid));
        logger.info("Account has created: " + bsaacid);

        GLAccount account = (GLAccount) baseEntityRepository.selectOne(GLAccount.class, "from GLAccount a where a.bsaAcid = ?1", bsaacid);
        Assert.assertEquals(account.getRelationTypeEnum(), GLAccount.RelationType.FIVE);

        // проверяем содержимое GL_RLNACT
        GLRelationAccountingType relationAccountingType
                = (GLRelationAccountingType) baseEntityRepository.selectFirst(GLRelationAccountingType.class
                , "from GLRelationAccountingType r where r.id.bsaacid = ?1", account.getBsaAcid());
        Assert.assertNotNull(relationAccountingType);

        GlAccRln rln = (GlAccRln) baseEntityRepository.findById(GlAccRln.class, new AccRlnId(account.getAcid(), account.getBsaAcid()));
        Assert.assertNotNull(rln);
        Assert.assertEquals(GLAccount.RelationType.FIVE.getValue(), rln.getRelationType());

        // поиск возвращает уже созданный счет
        String bsaacid2 = remoteAccess.invoke(GLPLAccountTesting.class, "getAccount"
                , GLOperationBuilder.create(operation).withValueDate(getOperday().getCurrentDate()).build(), C, acCt);
        Assert.assertEquals(bsaacid, bsaacid2);
    }

    /**
     * Проверка правильности создания / определения счета по кдючам счета
     * @param operSide  - сторона операции (D / C)
     * @param bsaAcid   - счет ЦБ
     * @param keyString - ключи счета
     * @return          - счет Майдас
     * @throws SQLException
     */
    public static String checkDefinedAccount(GLOperation.OperSide operSide
            , String bsaAcid, String keyString, GLAccount.RelationType relationType) throws SQLException {
        AccountKeys keys = new AccountKeys(keyString);
        Assert.assertFalse(isEmpty(bsaAcid));
        DataRecord data = baseEntityRepository.selectOne("select 1 from BSAACC where ID = ?", bsaAcid);
        Assert.assertNotNull(data);                     // bsaAcid есть в таблице BSAACC
        String acid;
        GLAccount account = (GLAccount) baseEntityRepository.selectOne(GLAccount.class,
                "from GLAccount a where a.bsaAcid = ?1", bsaAcid);
        boolean accMidas = !isEmpty(keys.getGlSequence())
                && ( keys.getGlSequence().startsWith("XX") || keys.getGlSequence().startsWith("PL"));
        final String rlnType = Optional.ofNullable(relationType).orElseGet(() ->
        {
            AccountKeys keys1 = remoteAccess.invoke(GLAccountController.class,
                    "fillAccountKeysMidas",  GLOperation.OperSide.N, account.getDateOpen(), keys);
            String acidInt = keys1.getAccountMidas();
            Assert.assertFalse(isEmpty(acidInt));
            return GLAccount.RelationType.parse(!accMidas
                    ? (isEmpty(account.getPlCode()) ? "4" : "2")
                    : (isEmpty(keys1.getPlCode()) ? "0" : "2"));
        }).getValue();
        if (!accMidas) {    // был создан новый счет
            Assert.assertNotNull(account);
            Assert.assertEquals(operSide, account.getOperSide());
            acid = account.getAcid();
            Assert.assertFalse(isEmpty(acid));
        } else {                                        // счет существует
            AccountKeys keys1 = remoteAccess.invoke(GLAccountController.class, "fillAccountKeysMidas", GLOperation.OperSide.N, keys);
            acid = keys1.getAccountMidas();
            Assert.assertFalse(isEmpty(acid));
        }
        data = baseEntityRepository.selectOne("select 1 from ACC where ID = ?", acid);
        Assert.assertNotNull(data);                 // acid есть в таблице ACC
        data = baseEntityRepository.selectOne("select 1 from ACCRLN where BSAACID = ? and ACID = ? and RLNTYPE = ?",
                bsaAcid, acid, rlnType);
        Assert.assertNotNull(data);                 // bsaAcid + acid есть в таблице ACCRLN
        return acid;
    }

    public static String checkDefinedAccount(GLOperation.OperSide operSide, String bsaAcid, String keyString) throws SQLException {
        return checkDefinedAccount(operSide, bsaAcid, keyString, null);
    }

    private void deleteSQvalue(AccountKeys keys) {
        delateSQvalue(keys.getCustomerNumber(), keys.getCurrency());
    }

    public static void delateSQvalue(String custNo, String ccy) {
        baseEntityRepository.executeNativeUpdate("delete from GL_SQVALUE where CUSTNO = ? and CCY = ?",
                custNo, ccy);
    }

    private GLOperation getOperation(Long idpst) {
        return (GLOperation) baseEntityRepository.selectFirst(GLOperation.class, "from GLOperation o where o.etlPostingRef = ?1", idpst);
    }

    private GLAccount getGLAccount(String account) {
        return Optional.<GLAccount>ofNullable(remoteAccess.invoke(GLAccountRepository.class, "findGLAccount", account))
                .orElseThrow(() -> new RuntimeException("GLAccount is not found"));
    }

    private void cleanAccountsByMidas(String acid) {
        logger.info("deleted accrln: " + baseEntityRepository.executeNativeUpdate("delete from accrln where acid = ?", acid));
        logger.info("deleted acc: " + baseEntityRepository.executeNativeUpdate("delete from acc where id = ?", acid));
        logger.info("deleted acc: " + baseEntityRepository.executeNativeUpdate("delete from gl_acc where acid = ?", acid));

    }

}
