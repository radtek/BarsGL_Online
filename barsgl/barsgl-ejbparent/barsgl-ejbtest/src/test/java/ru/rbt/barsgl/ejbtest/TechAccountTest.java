package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.dict.CurrencyRate;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GlPdTh;
import ru.rbt.barsgl.ejbcore.util.ExcelParser;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejbcore.mapping.YesNo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;

/**
 * Created by Ivan Sevastyanov on 03.04.2017.
 */
public class TechAccountTest extends AbstractRemoteTest {

    @Before public void init() {

//        baseEntityRepository.executeNativeUpdate("delete from gl_btth");
//        baseEntityRepository.executeNativeUpdate("delete from gl_pdth");
//        baseEntityRepository.executeNativeUpdate("delete from gl_oper where bs_chapter = 'T'");
//        baseEntityRepository.executeNativeUpdate("delete from gl_acc where rlntype = '9'");
    }

    /**
     * Тестовый метод проверки работы с техническими счетами
     */
    @Test public void testTHCreateNewAccount() throws ParseException {

        Operday oldOperday = getOperday();
        Date curDate = DateUtils.parseDate("2017-06-26","yyy-MM-dd");
        setOperday(curDate,curDate, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
        updateOperday(ONLINE, OPEN, Operday.PdMode.DIRECT);

        closeAllTHAccount();

        //Добавление нового курса
        List<CurrencyRate> curRate = baseEntityRepository.select(CurrencyRate.class,"from CurrencyRate cr where cr.id.rateDt = ?1",curDate);
        if (null==curDate)
        {
            CurrencyRate currencyRate = new CurrencyRate(new BankCurrency("USD"),curDate,BigDecimal.valueOf(58.95),BigDecimal.valueOf(1.0));
            baseEntityRepository.save(currencyRate);
            curRate = baseEntityRepository.select(CurrencyRate.class,"from CurrencyRate cr where cr.id.rateDt = ?1",curDate);
        }
        Assert.assertFalse("Не найден курс на текущую дату. Раскоментируйте код добавления курса.",curRate.isEmpty());

        EtlPosting pst_2 = this.getPosting_RUR_RUR();
        pst_2 = (EtlPosting) baseEntityRepository.save(pst_2);
        GLOperation operation_2 = (GLOperation) postingController.processMessage(pst_2);
        Assert.assertNotNull(operation_2);
        Assert.assertTrue(0 < operation_2.getId());
        operation_2 = (GLOperation) baseEntityRepository.findById(operation_2.getClass(), operation_2.getId());
        Assert.assertEquals(OperState.POST, operation_2.getState());
        //Проверяем наличие счёта по дебету
        List<GLAccount> accListDebit = baseEntityRepository.select(GLAccount.class,"from GLAccount a where a.bsaAcid = ?1",operation_2.getAccountDebit());
        Assert.assertFalse("Отсутствует и не создан счёт по дебету.",accListDebit.isEmpty());

        //Проверяем наличе счёта по кредиту
        List<GLAccount> accListCredit = baseEntityRepository.select(GLAccount.class,"from GLAccount a where a.bsaAcid = ?1",operation_2.getAccountCredit());
        Assert.assertFalse("Отсутствует и не создан счёт по кредиту.",accListCredit.isEmpty());

        List<GlPdTh> pdList = baseEntityRepository.select(GlPdTh.class,"from GlPdTh pd where pd.glOperationId = ?1",operation_2.getId());
        Assert.assertEquals("Неверное количество проводок созданных по операции",pdList.size(), 2);

        setOperday(oldOperday.getCurrentDate(),oldOperday.getLastWorkingDay(), oldOperday.getPhase(), oldOperday.getLastWorkdayStatus());
        updateOperday(ONLINE, OPEN, Operday.PdMode.DIRECT);
    }


    @Test public void testTHFindExistingAccount() throws ParseException {

        Operday oldOperday = getOperday();
        Date curDate = new Date();///DateUtils.parseDate("2017-05-13","yyy-MM-dd");
        setOperday(curDate,curDate, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
        updateOperday(ONLINE, OPEN, Operday.PdMode.DIRECT);

        EtlPosting pst_2 = this.getPosting_RUR_RUR();
        pst_2 = (EtlPosting) baseEntityRepository.save(pst_2);
        GLOperation operation_2 = (GLOperation) postingController.processMessage(pst_2);
        Assert.assertNotNull(operation_2);
        Assert.assertTrue(0 < operation_2.getId());
        operation_2 = (GLOperation) baseEntityRepository.findById(operation_2.getClass(), operation_2.getId());
        Assert.assertEquals(OperState.POST, operation_2.getState());
        //Проверяем наличие счёта по дебету
        List<GLAccount> accListDebit = baseEntityRepository.select(GLAccount.class,"from GLAccount a where a.bsaAcid = ?1",operation_2.getAccountDebit());
        Assert.assertFalse("Отсутствует и не создан счёт по дебету.",accListDebit.isEmpty());

        //Проверяем наличе счёта по кредиту
        List<GLAccount> accListCredit = baseEntityRepository.select(GLAccount.class,"from GLAccount a where a.bsaAcid = ?1",operation_2.getAccountCredit());
        Assert.assertFalse("Отсутствует и не создан счёт по кредиту.",accListCredit.isEmpty());

        List<GlPdTh> pdList = baseEntityRepository.select(GlPdTh.class,"from GlPdTh pd where pd.glOperationId = ?1",operation_2.getId());
        Assert.assertEquals("Неверное количество проводок созданных по операции",pdList.size(), 2);

        setOperday(oldOperday.getCurrentDate(),oldOperday.getLastWorkingDay(), oldOperday.getPhase(), oldOperday.getLastWorkdayStatus());
        updateOperday(ONLINE, OPEN, Operday.PdMode.DIRECT);
    }

    @Test public void testTHStorno() throws ParseException {

        Operday oldOperday = getOperday();
        Date curDate = new Date();///DateUtils.parseDate("2017-05-13","yyy-MM-dd");
        setOperday(curDate,curDate, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
        updateOperday(ONLINE, OPEN, Operday.PdMode.DIRECT);

        //Обрабатываем прямую операцию
        EtlPosting pst_1 = getPostingForStorno(DateUtils.parseDate("2017-05-13","yyy-MM-dd"));
        pst_1 = (EtlPosting) baseEntityRepository.save(pst_1);
        GLOperation operation_1 = (GLOperation) postingController.processMessage(pst_1);
        Assert.assertNotNull(operation_1);
        Assert.assertTrue(0 < operation_1.getId());
        operation_1 = (GLOperation) baseEntityRepository.findById(operation_1.getClass(), operation_1.getId());
        Assert.assertEquals(OperState.POST, operation_1.getState());
        //Проверяем наличие счёта по дебету
        List<GLAccount> accListDebit = baseEntityRepository.select(GLAccount.class,"from GLAccount a where a.bsaAcid = ?1",operation_1.getAccountDebit());
        Assert.assertFalse("Отсутствует и не создан счёт по дебету.",accListDebit.isEmpty());

        //Проверяем наличе счёта по кредиту
        List<GLAccount> accListCredit = baseEntityRepository.select(GLAccount.class,"from GLAccount a where a.bsaAcid = ?1",operation_1.getAccountCredit());
        Assert.assertFalse("Отсутствует и не создан счёт по кредиту.",accListCredit.isEmpty());

        List<GlPdTh> pdList = baseEntityRepository.select(GlPdTh.class,"from GlPdTh pd where pd.glOperationId = ?1",operation_1.getId());
        Assert.assertEquals("Неверное количество проводок созданных по операции",pdList.size(), 2);

        //Обрабатываем сторнирующую операцию
        EtlPosting pst_2 = getPostingStorno(DateUtils.parseDate("2017-05-13","yyy-MM-dd"));
        pst_2 = (EtlPosting) baseEntityRepository.save(pst_2);
        GLOperation operation_2 = (GLOperation) postingController.processMessage(pst_2);
        Assert.assertNotNull(operation_2);
        Assert.assertTrue(0 < operation_2.getId());
        operation_2 = (GLOperation) baseEntityRepository.findById(operation_2.getClass(), operation_2.getId());
        Assert.assertEquals(OperState.POST, operation_2.getState());
        //Проверяем наличие счёта по дебету
        accListDebit = baseEntityRepository.select(GLAccount.class,"from GLAccount a where a.bsaAcid = ?1",operation_2.getAccountDebit());
        Assert.assertFalse("Отсутствует и не создан счёт по дебету.",accListDebit.isEmpty());

        //Проверяем наличе счёта по кредиту
        accListCredit = baseEntityRepository.select(GLAccount.class,"from GLAccount a where a.bsaAcid = ?1",operation_2.getAccountCredit());
        Assert.assertFalse("Отсутствует и не создан счёт по кредиту.",accListCredit.isEmpty());

        pdList = baseEntityRepository.select(GlPdTh.class,"from GlPdTh pd where pd.glOperationId = ?1",operation_2.getId());
        Assert.assertEquals("Неверное количество проводок созданных по операции",pdList.size(), 2);

        setOperday(oldOperday.getCurrentDate(),oldOperday.getLastWorkingDay(), oldOperday.getPhase(), oldOperday.getLastWorkdayStatus());
        updateOperday(ONLINE, OPEN, Operday.PdMode.DIRECT);
    }

    @Test public void testTHStornoEmptyStornoRef() throws ParseException {

        Operday oldOperday = getOperday();
        Date curDate = new Date();///DateUtils.parseDate("2017-05-13","yyy-MM-dd");
        setOperday(curDate,curDate, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
        updateOperday(ONLINE, OPEN, Operday.PdMode.DIRECT);

        //Обрабатываем сторнирующую операцию
        EtlPosting pst_2 = getPostingStorno(DateUtils.parseDate("2017-05-13","yyy-MM-dd"));
        pst_2.setStornoReference("");
        pst_2 = (EtlPosting) baseEntityRepository.save(pst_2);
        GLOperation operation_2 = (GLOperation) postingController.processMessage(pst_2);
        Assert.assertNull(operation_2);

        setOperday(oldOperday.getCurrentDate(),oldOperday.getLastWorkingDay(), oldOperday.getPhase(), oldOperday.getLastWorkdayStatus());
        updateOperday(ONLINE, OPEN, Operday.PdMode.DIRECT);
    }

    @Test public void testTHStornoDirectNotFound() throws ParseException {

        Operday oldOperday = getOperday();
        Date curDate = new Date();///DateUtils.parseDate("2017-05-13","yyy-MM-dd");
        setOperday(curDate,curDate, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
        updateOperday(ONLINE, OPEN, Operday.PdMode.DIRECT);

        //Обрабатываем сторнирующую операцию
        EtlPosting pst_2 = getPostingStorno(DateUtils.parseDate("2017-05-13","yyy-MM-dd"));
        pst_2.setStornoReference("12312121");
        pst_2 = (EtlPosting) baseEntityRepository.save(pst_2);
        GLOperation operation_2 = (GLOperation) postingController.processMessage(pst_2);
        Assert.assertNotNull(operation_2);
        Assert.assertTrue(0 < operation_2.getId());
        operation_2 = (GLOperation) baseEntityRepository.findById(operation_2.getClass(), operation_2.getId());
        Assert.assertNotEquals(OperState.POST, operation_2.getState());

        setOperday(oldOperday.getCurrentDate(),oldOperday.getLastWorkingDay(), oldOperday.getPhase(), oldOperday.getLastWorkdayStatus());
        updateOperday(ONLINE, OPEN, Operday.PdMode.DIRECT);
    }

    @Test public void testTHStornoOneDay() throws ParseException, InterruptedException {

        Operday oldOperday = getOperday();
        Date curDate = oldOperday.getCurrentDate();//new Date();///DateUtils.parseDate("2017-05-13","yyy-MM-dd");
        setOperday(curDate, DateUtils.addDays(curDate,-1), Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
        updateOperday(ONLINE, OPEN, Operday.PdMode.DIRECT);

        //Обрабатываем прямую операцию
        EtlPosting pst_1 = getPostingForStorno(curDate);
        pst_1 = (EtlPosting) baseEntityRepository.save(pst_1);
        GLOperation operation_1 = (GLOperation) postingController.processMessage(pst_1);
        Assert.assertNotNull(operation_1);
        Assert.assertTrue(0 < operation_1.getId());
        operation_1 = (GLOperation) baseEntityRepository.findById(operation_1.getClass(), operation_1.getId());
        Assert.assertEquals(OperState.POST, operation_1.getState());
        //Проверяем наличие счёта по дебету
        List<GLAccount> accListDebit = baseEntityRepository.select(GLAccount.class,"from GLAccount a where a.bsaAcid = ?1",operation_1.getAccountDebit());
        Assert.assertFalse("Отсутствует и не создан счёт по дебету.",accListDebit.isEmpty());

        //Проверяем наличе счёта по кредиту
        List<GLAccount> accListCredit = baseEntityRepository.select(GLAccount.class,"from GLAccount a where a.bsaAcid = ?1",operation_1.getAccountCredit());
        Assert.assertFalse("Отсутствует и не создан счёт по кредиту.",accListCredit.isEmpty());

        List<GlPdTh> pdList = baseEntityRepository.select(GlPdTh.class,"from GlPdTh pd where pd.glOperationId = ?1",operation_1.getId());
        Assert.assertEquals("Неверное количество проводок созданных по операции",pdList.size(), 2);

        //Обрабатываем сторнирующую операцию
        EtlPosting pst_2 = getPostingStorno(curDate);
        pst_2 = (EtlPosting) baseEntityRepository.save(pst_2);
        GLOperation operation_2 = (GLOperation) postingController.processMessage(pst_2);

        Thread.sleep(100);

        Assert.assertNotNull(operation_2);
        Assert.assertTrue(0 < operation_2.getId());
        operation_2 = (GLOperation) baseEntityRepository.findById(operation_2.getClass(), operation_2.getId());
        Assert.assertEquals(OperState.SOCANC, operation_2.getState());

        GLOperation stornoOper =  (GLOperation) baseEntityRepository.findById(GLOperation.class, operation_2.getStornoOperation().getId());
        Assert.assertEquals(OperState.CANC, stornoOper.getState());
        //Проверяем наличие счёта по дебету
        accListDebit = baseEntityRepository.select(GLAccount.class,"from GLAccount a where a.bsaAcid = ?1",operation_2.getAccountDebit());
        Assert.assertFalse("Отсутствует и не создан счёт по дебету.",accListDebit.isEmpty());

        //Проверяем наличе счёта по кредиту
        accListCredit = baseEntityRepository.select(GLAccount.class,"from GLAccount a where a.bsaAcid = ?1",operation_2.getAccountCredit());
        Assert.assertFalse("Отсутствует и не создан счёт по кредиту.",accListCredit.isEmpty());

        pdList = baseEntityRepository.select(GlPdTh.class,"from GlPdTh pd where pd.glOperationId = ?1 and pd.invisible='1'",operation_2.getStornoOperation().getId());
        Assert.assertEquals("Неверное количество проводок созданных по операции",pdList.size(), 2);

        setOperday(oldOperday.getCurrentDate(),oldOperday.getLastWorkingDay(), oldOperday.getPhase(), oldOperday.getLastWorkdayStatus());
        updateOperday(ONLINE, OPEN, Operday.PdMode.DIRECT);
    }

    @Test
    public void testLoadEtlPstFromFile() throws ParseException, InvalidFormatException, IOException {

        LoadEtlFromFile();
    }


    private void closeAllTHAccount()
    {
        List<GLAccount> accList = baseEntityRepository.select(GLAccount.class,"from GLAccount a where a.relationType = '9' and a.dateClose is null");


        int days = (int)(Math.random()*30-50);

        for(GLAccount acc:accList) {
            acc.setDateClose(DateUtils.addDays(new Date(),days));
            baseEntityRepository.update(acc);
            baseEntityRepository.executeNativeUpdate("delete from accrlnext x where x.bsaacid = ?",acc.getBsaAcid());
        }
    }


    private EtlPosting getPosting_USD_RUR() throws ParseException {
        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "TECHACC");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());
        //pst.setValueDate(DateUtils.parseDate("2017-02-17","yyyy-MM-dd"));

        //pst.setAccountCredit("40817036200012959997");
        //pst.setAccountDebit("40817036250010000018");
        pst.setAccountKeyDebit(";USD;;057010103;;;TH01096378;0001;;;;;K+TP;;");
        pst.setAccountKeyCredit(";RUR;;007010201;;;TH01096366;0001;;;;;K+TP;;");
        pst.setAmountCredit(new BigDecimal("60000.000"));
        pst.setAmountDebit(new BigDecimal("1000.000"));
        //pst.setAmountCreditRu(pst.getAmountCredit());
        //pst.setAmountDebitRu(pst.getAmountDebit());

        pst.setCurrencyCredit(BankCurrency.RUB);
        pst.setCurrencyDebit(BankCurrency.USD);
        pst.setSourcePosting(GLOperation.srcKondorPlus);
        pst.setEventId("2316768");
        pst.setDeptId("TBM");
        pst.setDealId("921458");
        //pst.setValueDate(DateUtils.parseDate("2017-02-18","yyyy-MM-dd"));
        pst.setOperationTimestamp(getOperday().getCurrentDate());
        pst.setOperationTimestamp(DateUtils.parseDate("2017-01-12 10:02:12.040000","yyyy-MM-dd HH:mm:ss.SSS"));
        pst.setNarrative("FS;2604950;921458;400038");
        pst.setRusNarrativeLong("Реализованный финансовый результат по сделке SPOT № KTP921458");
        pst.setRusNarrativeShort("Реализованный финансовый результат по сделке SPOT № KTP921458");
        pst.setStorno(YesNo.N);
        pst.setFan(YesNo.N);
        pst.setEventType("Conversion");

        return pst;
    }

    private EtlPosting getPosting_RUR_RUR() throws ParseException {
        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "TECHACC");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(DateUtils.parseDate("2017-05-26","yyyy-MM-dd"));

        pst.setAccountKeyDebit(";RUR;;001020204;;;TH00000001;0002;;;;;K+TP;;");
        pst.setAccountKeyCredit(";RUR;;007010101;;;TH00000003;0002;;;;;K+TP;;");
        pst.setAmountCredit(new BigDecimal("20539.180"));
        pst.setAmountDebit(new BigDecimal("20539.180"));
        pst.setAmountCreditRu(pst.getAmountCredit());
        pst.setAmountDebitRu(pst.getAmountDebit());
        pst.setCurrencyCredit(BankCurrency.RUB);
        pst.setCurrencyDebit(BankCurrency.RUB);
        pst.setSourcePosting(GLOperation.srcKondorPlus);
        pst.setEventId("2316768");
        pst.setDeptId("TBM");
        pst.setDealId("875859");
        //pst.setOperationTimestamp(getOperday().getCurrentDate());
        pst.setOperationTimestamp(DateUtils.parseDate("2015-05-29 15:37:57.930000","yyyy-MM-dd HH:mm:ss.SSS"));
        pst.setNarrative("FS;2400412;533792;777033");
        pst.setRusNarrativeLong("Реализованный финансовый результат по сделке SPOT № KTP533792");
        pst.setRusNarrativeShort("Реализованный финансовый результат по сделке SPOT № KTP533792");
        pst.setStorno(YesNo.N);
        pst.setFan(YesNo.N);
        pst.setEventType("Conversion");

        return pst;
    }

    private EtlPosting getPostingForStorno(Date valueDate) throws ParseException {
        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "TECHACC_TEST_STORNO");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(valueDate);
        pst.setOperationTimestamp(valueDate);

        pst.setAccountKeyDebit(";RUR;;008010403;;;TH00000018;0001;;;;;K+TP;;");
        pst.setAccountKeyCredit(";RUR;;007010403;;;TH00000017;0001;;;;;K+TP;;");
        pst.setAmountCredit(new BigDecimal("530936.610"));
        pst.setAmountDebit(new BigDecimal("530936.610"));
        pst.setAmountCreditRu(pst.getAmountCredit());
        pst.setAmountDebitRu(pst.getAmountDebit());
        pst.setCurrencyCredit(BankCurrency.RUB);
        pst.setCurrencyDebit(BankCurrency.RUB);
        pst.setSourcePosting("TBO");
        pst.setEventId("3370547");
        pst.setDeptId("TBM");
        pst.setDealId("1287387");
        pst.setChnlName("KONDOR+TP");
        pst.setNarrative("04;5731;1287387;200428");
        pst.setRusNarrativeLong("04;5731;1287387;200428;Реализованный финансовый результат по опциону");
        pst.setRusNarrativeShort("04;5731;1287387;200428");
        pst.setStorno(YesNo.N);
        pst.setFan(YesNo.N);
        pst.setEventType("Exercise");
        pst.setPaymentRefernce("");

        return pst;
    }

    private EtlPosting getPostingStorno(Date valueDate) throws ParseException {
        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "TECHACC_TEST_STORNO");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(valueDate);
        pst.setOperationTimestamp(valueDate);

        pst.setAccountKeyDebit(";RUR;;007010403;;;TH00000017;0001;;;;;K+TP;;");
        pst.setAccountKeyCredit(";RUR;;008010403;;;TH00000018;0001;;;;;K+TP;;");
        pst.setAmountCredit(new BigDecimal("530936.610"));
        pst.setAmountDebit(new BigDecimal("530936.610"));
        pst.setAmountCreditRu(pst.getAmountCredit());
        pst.setAmountDebitRu(pst.getAmountDebit());
        pst.setCurrencyCredit(BankCurrency.RUB);
        pst.setCurrencyDebit(BankCurrency.RUB);
        pst.setSourcePosting("TBO");
        pst.setEventId("3370547");
        pst.setStornoReference("3370547");
        pst.setDeptId("TBM");
        pst.setDealId("1287387");
        pst.setChnlName("KONDOR+TP");
        pst.setNarrative("*04;5731;1287387;200428");
        pst.setRusNarrativeLong("*04;5731;1287387;200428;Реализованный финансовый результат по опциону");
        pst.setRusNarrativeShort("*04;5731;1287387;200428");
        pst.setStorno(YesNo.Y);
        pst.setFan(YesNo.N);
        pst.setEventType("Exercise");
        pst.setPaymentRefernce("");

        return pst;
    }



    private void clearTechRecords()
    {
        List<EtlPackage> pkgList = baseEntityRepository.select(EtlPackage.class,"FROM EtlPackage p WHERE p.description=?1","TECHACC");

        pkgList.forEach((a)->{
            List<EtlPosting> pstList = baseEntityRepository.select(EtlPosting.class,"from EtlPosting p where p.etlPackage=?1",a);
            pstList.forEach((b)->{
                List<GLOperation> operList = baseEntityRepository.select(GLOperation.class,"from GLOperation o where o.aePostingId=?1",b.getAePostingId());
                operList.forEach((c)->{
                    List<GLAccount> accList = baseEntityRepository.select(GLAccount.class,"from GLAccount a where a.operation = ?1",c);
                    accList.forEach((acc)->{
                        baseEntityRepository.executeNativeUpdate("delete from gl_btth where glacid = ?1",acc.getId());
                    });
                    baseEntityRepository.executeUpdate("delete from GlPdTh pd WHERE pd.glOperationId = ?1",c.getId());
                    baseEntityRepository.executeUpdate("delete from GLAccount acc where acc.operation = ?1",c);
                });
                baseEntityRepository.executeUpdate("delete from GLOperation o where o.aePostingId = ?1",b.getAePostingId());
            });
            baseEntityRepository.executeUpdate("delete from EtlPosting p where p.etlPackage=?1",a);
        });
        baseEntityRepository.executeUpdate("delete from EtlPackage p WHERE p.description=?1","TECHACC");
    }


    public void LoadEtlFromFile() throws IOException, InvalidFormatException, ParseException {

        File f = new File("c:\\Projects\\ETLPST_storno2.xlsx");
        Assert.assertTrue("Файл с даными для загрузки не существует", f.exists());

        if (f.exists()) {

            InputStream fileStream = new FileInputStream(f);
            ExcelParser parser = new ExcelParser(fileStream);
            Iterator<List<Object>> it = parser.parseSafe(0);

            Assert.assertTrue("Нет строк для загрузки",parser.hasNext());

            long stamp = System.currentTimeMillis();
            EtlPackage pkg = newPackage(stamp, "TECHACC_storno");
            Assert.assertTrue(pkg.getId() > 0);

            //Сохраняем дату опердня и меняем на свою
            /*Operday oldOperday = getOperday();
            Date curDate = DateUtils.parseDate("2017-03-13","yyy-MM-dd");
            setOperday(curDate,curDate, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
            updateOperday(ONLINE, OPEN, Operday.PdMode.DIRECT);*/

            List<Object> header = it.next();

            while (it.hasNext())
            {
                List<Object> row = it.next();
                EtlPosting pst = newPosting(stamp, pkg);
                pst = this.fillEtlPst(pst,row);
                pst = (EtlPosting) baseEntityRepository.save(pst);

                EtlPosting pst2 = (EtlPosting) baseEntityRepository.findById(EtlPosting.class,pst.getId());
                //GLOperation operation = (GLOperation) postingController.processMessage(pst);
                //Assert.assertNotNull("Ошибка создания операции.",operation);
                //operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
                //Assert.assertEquals("Ошибка при обработке операции: "+operation.getId(),OperState.POST, operation.getState());
            }

            /*setOperday(oldOperday.getCurrentDate(),oldOperday.getLastWorkingDay(), oldOperday.getPhase(), oldOperday.getLastWorkdayStatus());
            updateOperday(ONLINE, OPEN, Operday.PdMode.DIRECT);*/
        }
    }

    public void LoadEtlFromFileByNumber() throws IOException, InvalidFormatException, ParseException {

        File f = new File("c:\\Projects\\GL_ETLPST_20170320_02.xlsx");
        Assert.assertTrue("Файл с даными для загрузки не существует", f.exists());
        final int ROW_NUMBER = 1;  //номер строки начиная с данных. Первая строка считается нулевой (заголовок).

        if (f.exists()) {

            InputStream fileStream = new FileInputStream(f);
            ExcelParser parser = new ExcelParser(fileStream);
            Iterator<List<Object>> it = parser.parseSafe(0);


            Assert.assertTrue("Нет строк для загрузки",parser.hasNext());

            long stamp = System.currentTimeMillis();
            EtlPackage pkg = newPackage(stamp, "TECHACC_1");
            Assert.assertTrue(pkg.getId() > 0);

            //Сохраняем дату опердня и меняем на свою
            Operday oldOperday = getOperday();
            Date curDate = DateUtils.parseDate("2017-02-20","yyy-MM-dd");
            setOperday(curDate,curDate, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
            updateOperday(ONLINE, OPEN, Operday.PdMode.DIRECT);

            //List<Object> header = it.next();

            for(int i=0;i<ROW_NUMBER;i++)
            {
                it.next();
            }

            List<Object> row = it.next();
            EtlPosting pst = newPosting(stamp, pkg);
            pst = this.fillEtlPst(pst,row);
            pst.setErrorCode(0);
            pst = (EtlPosting) baseEntityRepository.save(pst);
            //GLOperation operation = (GLOperation) postingController.processMessage(pst);
            //Assert.assertNotNull("Ошибка создания операции.",operation);
            //operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
            //Assert.assertEquals("Ошибка при обработке операции: "+operation.getId(),OperState.POST, operation.getState());

           /* while (it.hasNext())
            {
                List<Object> row = it.next();
                EtlPosting pst = newPosting(stamp, pkg);
                pst = this.fillEtlPst(pst,row);
                pst = (EtlPosting) baseEntityRepository.save(pst);
                GLOperation operation = (GLOperation) postingController.processMessage(pst);
                Assert.assertNotNull("Ошибка создания операции.",operation);
                operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
                Assert.assertEquals("Ошибка при обработке операции: "+operation.getId(),OperState.POST, operation.getState());
            }*/

            setOperday(oldOperday.getCurrentDate(),oldOperday.getLastWorkingDay(), oldOperday.getPhase(), oldOperday.getLastWorkdayStatus());
            updateOperday(ONLINE, OPEN, Operday.PdMode.DIRECT);
        }
    }

    private EtlPosting fillEtlPst(EtlPosting pst, List<Object> row) throws ParseException {

        pst.setSourcePosting(row.get(3).toString());
        pst.setEventId(row.get(4).toString());
        pst.setDealId(row.get(5).toString());
        pst.setDeptId(row.get(8).toString());
        pst.setValueDate(DateUtils.parseDate(row.get(9).toString(),"yyyy-MM-dd"));
        //pst.setOperationTimestamp(DateUtils.parseDate(row.get(10).toString().trim(),"yyyy-MM-dd HH:mm:ss.SSS"));
        pst.setOperationTimestamp(Timestamp.from(Instant.now()));
        //pst.setOperationTimestamp(DateUtils.parseDate("2016-07-21 15:37:57.930000","yyyy-MM-dd HH:mm:ss.SSS"));
        pst.setNarrative(row.get(11).toString());
        pst.setRusNarrativeLong(row.get(12).toString());
        pst.setRusNarrativeShort(row.get(13).toString());
        pst.setStorno(YesNo.valueOf(row.get(14).toString()));
        pst.setStornoReference(row.get(15)!=null?row.get(15).toString():null);
        pst.setCurrencyDebit(row.get(17).toString().equals("RUR")?BankCurrency.RUB:BankCurrency.USD);
        pst.setAmountDebit(new BigDecimal(row.get(18).toString()));
        pst.setCurrencyCredit(row.get(21).toString().equals("RUR")?BankCurrency.RUB:BankCurrency.USD);
        pst.setAmountCredit(new BigDecimal(row.get(22).toString()));
        pst.setFan(YesNo.valueOf(row.get(24).toString()));
        pst.setPaymentRefernce(null);

        pst.setAccountKeyDebit(row.get(28).toString());
        pst.setAccountKeyCredit(row.get(29).toString());
        pst.setEventType(row.get(30).toString());

        return pst;
    }

}
