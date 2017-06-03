package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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
import ru.rbt.ejbcore.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;

/**
 * Created by Ivan Sevastyanov on 03.04.2017.
 */
public class TachAccountIT extends AbstractRemoteIT {

    @Before public void init() {

        baseEntityRepository.executeNativeUpdate("delete from gl_btth");
    }

    /**
     * Тестовый метод проверки работы с техническими счетами
     */
    @Test
    public void testTechAccountPostingProcessor() throws ParseException, SQLException {

        Operday oldOperday = getOperday();
        Date curDate = DateUtils.parseDate("2017-03-13","yyy-MM-dd");
        setOperday(curDate,curDate, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
        updateOperday(ONLINE, OPEN, Operday.PdMode.DIRECT);

        //Добавление нового курса
        CurrencyRate currencyRate = new CurrencyRate(new BankCurrency("USD"),new Date(), BigDecimal.valueOf(58.95),BigDecimal.valueOf(1.0));
        baseEntityRepository.save(currencyRate);

        List<CurrencyRate> curRate = baseEntityRepository.select(CurrencyRate.class,"from CurrencyRate cr where cr.id.rateDt = ?1",new Date());
        Assert.assertFalse("Не найден курс на текущую дату. Раскоментируйте код добавления курса.",curRate.isEmpty());


        //Удаление записей по техничесим с счетам.
        //this.clearTechRecords();

        EtlPosting pst_2 = this.getPosting_RUR_RUR();
        pst_2 = (EtlPosting) baseEntityRepository.save(pst_2);
        GLOperation operation_2 = (GLOperation) postingController.processMessage(pst_2);
        Assert.assertNotNull(operation_2);
        Assert.assertTrue(0 < operation_2.getId());
        operation_2 = (GLOperation) baseEntityRepository.findById(operation_2.getClass(), operation_2.getId());
        Assert.assertEquals(OperState.POST, operation_2.getState());
        //Assert.assertEquals(getOperday().getCurrentDate(), operation_2.getCurrentDate());
        //Assert.assertEquals(getOperday().getLastWorkdayStatus(), operation_2.getLastWorkdayStatus());

        //Проверяем наличие счёта по дебету
        List<GLAccount> accListDebit = baseEntityRepository.select(GLAccount.class,"from GLAccount a where a.bsaAcid = ?1",operation_2.getAccountDebit());
        Assert.assertFalse("Отсутствует и не создан счёт по дебету.",accListDebit.isEmpty());

        //Проверяем наличе счёта по кредиту
        List<GLAccount> accListCredit = baseEntityRepository.select(GLAccount.class,"from GLAccount a where a.bsaAcid = ?1",operation_2.getAccountCredit());
        Assert.assertFalse("Отсутствует и не создан счёт по кредиту.",accListCredit.isEmpty());

        List<GlPdTh> pdList = baseEntityRepository.select(GlPdTh.class,"from GlPdTh pd where pd.glOperationId = ?1",operation_2.getId());
        Assert.assertEquals("Неверное количество проводок созданных по операции",pdList.size(), 2);

        Assert.assertTrue(!baseEntityRepository.select("select * from gl_btth where bsaacid = ?", operation_2.getAccountCredit()).isEmpty());
        Assert.assertTrue(!baseEntityRepository.select("select * from gl_btth where bsaacid = ?", operation_2.getAccountDebit()).isEmpty());

    }


    @Test public void testNotTechAccountPostingProcessor() throws ParseException {

        Date curDate = DateUtils.parseDate("2017-03-13","yyy-MM-dd");
        setOperday(curDate,curDate, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN, Operday.PdMode.DIRECT);

        checkCreateBankCurrency(curDate, BankCurrency.USD, new BigDecimal("100.01"));

        EtlPackage pkg = (EtlPackage)baseEntityRepository.findById(EtlPackage.class,147252L);
        List<EtlPosting> listPst = baseEntityRepository.select(EtlPosting.class,"from EtlPosting p where p.etlPackage = ?1",pkg);
        EtlPosting pst_2 = listPst.get(0);
        pst_2.setErrorCode(null);
        //pst_2 = (EtlPosting) baseEntityRepository.save(pst_2);
        GLOperation operation_2 = (GLOperation) postingController.processMessage(pst_2);
        Assert.assertNotNull(operation_2);
        Assert.assertTrue(0 < operation_2.getId());
        operation_2 = (GLOperation) baseEntityRepository.findById(operation_2.getClass(), operation_2.getId());
        Assert.assertEquals(OperState.POST, operation_2.getState());
        //Assert.assertEquals(getOperday().getCurrentDate(), operation_2.getCurrentDate());
        //Assert.assertEquals(getOperday().getLastWorkdayStatus(), operation_2.getLastWorkdayStatus());

        //Проверяем наличие счёта по дебету
        List<GLAccount> accListDebit = baseEntityRepository.select(GLAccount.class,"from GLAccount a where a.bsaAcid = ?1",operation_2.getAccountDebit());
        Assert.assertFalse("Отсутствует и не создан счёт по дебету.",accListDebit.isEmpty());

        //Проверяем наличе счёта по кредиту
        List<GLAccount> accListCredit = baseEntityRepository.select(GLAccount.class,"from GLAccount a where a.bsaAcid = ?1",operation_2.getAccountCredit());
        Assert.assertFalse("Отсутствует и не создан счёт по кредиту.",accListCredit.isEmpty());

        //List<GlPdTh> pdList = baseEntityRepository.select(GlPdTh.class,"from GlPdTh pd where pd.glOperationId = ?1",operation_2.getId());
        //Assert.assertEquals("Неверное количество проводок созданных по операции",pdList.size(), 2);

        //setOperday(oldOperday.getCurrentDate(),oldOperday.getLastWorkingDay(), oldOperday.getPhase(), oldOperday.getLastWorkdayStatus());
        //updateOperday(ONLINE, OPEN, Operday.PdMode.DIRECT);
    }

    /**
     * проверка корректности поиска/создания техн счетов
     * @throws ParseException
     */
    @Test public void testNotTechAccountCheck() throws ParseException {

        Date curDate = DateUtils.parseDate("2017-03-13","yyy-MM-dd");
        setOperday(curDate,curDate, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN, Operday.PdMode.DIRECT);

        checkCreateBankCurrency(curDate, BankCurrency.USD, new BigDecimal("100.01"));

        //Удаление записей по техничесим с счетам.
        baseEntityRepository.executeNativeUpdate("delete from gl_acc where rlntype = 9");

        clearTechRecords();


        EtlPackage pkg = (EtlPackage)baseEntityRepository.findById(EtlPackage.class,147252L);
        List<EtlPosting> listPst = baseEntityRepository.select(EtlPosting.class,"from EtlPosting p where p.etlPackage = ?1",pkg);
        EtlPosting pst_2 = listPst.get(0);
        pst_2.setErrorCode(null);
        //pst_2 = (EtlPosting) baseEntityRepository.save(pst_2);
        GLOperation operation_2 = (GLOperation) postingController.processMessage(pst_2);
        Assert.assertNotNull(operation_2);
        Assert.assertTrue(0 < operation_2.getId());
        operation_2 = (GLOperation) baseEntityRepository.findById(operation_2.getClass(), operation_2.getId());
        Assert.assertEquals(OperState.POST, operation_2.getState());
        //Assert.assertEquals(getOperday().getCurrentDate(), operation_2.getCurrentDate());
        //Assert.assertEquals(getOperday().getLastWorkdayStatus(), operation_2.getLastWorkdayStatus());

        //Проверяем наличие счёта по дебету
        List<GLAccount> accListDebit = baseEntityRepository.select(GLAccount.class,"from GLAccount a where a.bsaAcid = ?1",operation_2.getAccountDebit());
        Assert.assertFalse("Отсутствует и не создан счёт по дебету.",accListDebit.isEmpty());

        //Проверяем наличе счёта по кредиту
        List<GLAccount> accListCredit = baseEntityRepository.select(GLAccount.class,"from GLAccount a where a.bsaAcid = ?1",operation_2.getAccountCredit());
        Assert.assertFalse("Отсутствует и не создан счёт по кредиту.",accListCredit.isEmpty());

        pst_2.setAePostingId(StringUtils.rsubstr(System.currentTimeMillis()+"", 6));
        GLOperation operation_3 = (GLOperation) postingController.processMessage(pst_2);

        operation_3 = (GLOperation) baseEntityRepository.findById(GLOperation.class, operation_3.getId());
        Assert.assertEquals(OperState.POST, operation_3.getState());

        Assert.assertEquals(operation_2.getAccountDebit(), operation_3.getAccountDebit());
        Assert.assertEquals(operation_2.getAccountCredit(), operation_3.getAccountCredit());


        //List<GlPdTh> pdList = baseEntityRepository.select(GlPdTh.class,"from GlPdTh pd where pd.glOperationId = ?1",operation_2.getId());
        //Assert.assertEquals("Неверное количество проводок созданных по операции",pdList.size(), 2);

        //setOperday(oldOperday.getCurrentDate(),oldOperday.getLastWorkingDay(), oldOperday.getPhase(), oldOperday.getLastWorkdayStatus());
        //updateOperday(ONLINE, OPEN, Operday.PdMode.DIRECT);
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
        //pst.setValueDate(getOperday().getCurrentDate());
        pst.setValueDate(DateUtils.parseDate("2017-02-16","yyyy-MM-dd"));

        //pst.setAccountCredit("40817036200012959997");
        //pst.setAccountDebit("40817036250010000018");
        pst.setAccountKeyDebit(";RUR;;008010103;;;TH01096372;0001;;;;;K+TP;;");
        pst.setAccountKeyCredit(";RUR;;007010103;;;TH01096364;0001;;;;;K+TP;;");
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
        pst.setOperationTimestamp(DateUtils.parseDate("2016-07-21 15:37:57.930000","yyyy-MM-dd HH:mm:ss.SSS"));
        pst.setNarrative("FS;2400412;533792;777033");
        pst.setRusNarrativeLong("Реализованный финансовый результат по сделке SPOT № KTP533792");
        pst.setRusNarrativeShort("Реализованный финансовый результат по сделке SPOT № KTP533792");
        pst.setStorno(YesNo.N);
        pst.setFan(YesNo.N);
        pst.setEventType("Conversion");

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


    @Test @Ignore public void LoadEtlFromFile() throws IOException, InvalidFormatException, ParseException {

        File f = new File("c:\\Projects\\GL_ETLPST_20170320_01.xlsx");
        Assert.assertTrue("Файл с даными для загрузки не существует", f.exists());

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
            Date curDate = DateUtils.parseDate("2017-03-13","yyy-MM-dd");
            setOperday(curDate,curDate, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
            updateOperday(ONLINE, OPEN, Operday.PdMode.DIRECT);

            List<Object> header = it.next();

            while (it.hasNext())
            {
                List<Object> row = it.next();
                EtlPosting pst = newPosting(stamp, pkg);
                pst = this.fillEtlPst(pst,row);
                pst = (EtlPosting) baseEntityRepository.save(pst);
                GLOperation operation = (GLOperation) postingController.processMessage(pst);
                Assert.assertNotNull("Ошибка создания операции.",operation);
                operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
                Assert.assertEquals("Ошибка при обработке операции: "+operation.getId(),OperState.POST, operation.getState());
            }

            setOperday(oldOperday.getCurrentDate(),oldOperday.getLastWorkingDay(), oldOperday.getPhase(), oldOperday.getLastWorkdayStatus());
            updateOperday(ONLINE, OPEN, Operday.PdMode.DIRECT);
        }
    }

    @Test public void LoadEtlFromFileByNumber() throws IOException, InvalidFormatException, ParseException {

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
            pst = (EtlPosting) baseEntityRepository.save(pst);
            GLOperation operation = (GLOperation) postingController.processMessage(pst);
            Assert.assertNotNull("Ошибка создания операции.",operation);
            operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
            Assert.assertEquals("Ошибка при обработке операции: "+operation.getId(),OperState.POST, operation.getState());

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
        pst.setOperationTimestamp(DateUtils.parseDate(row.get(10).toString().trim(),"yyyy-MM-dd HH:mm:ss.SSS"));
        //pst.setOperationTimestamp(DateUtils.parseDate("2016-07-21 15:37:57.930000","yyyy-MM-dd HH:mm:ss.SSS"));
        pst.setNarrative(row.get(11).toString());
        pst.setRusNarrativeLong(row.get(12).toString());
        pst.setRusNarrativeShort(row.get(13).toString());
        pst.setStorno(YesNo.valueOf(row.get(14).toString()));
        pst.setCurrencyDebit(row.get(17).toString().equals("RUR")?BankCurrency.RUB:BankCurrency.USD);
        pst.setAmountDebit(new BigDecimal(row.get(18).toString()));
        pst.setCurrencyCredit(row.get(21).toString().equals("RUR")?BankCurrency.RUB:BankCurrency.USD);
        pst.setAmountCredit(new BigDecimal(row.get(22).toString()));
        pst.setFan(YesNo.valueOf(row.get(24).toString()));

        pst.setAccountKeyDebit(row.get(28).toString());
        pst.setAccountKeyCredit(row.get(29).toString());
        pst.setEventType(row.get(30).toString());

        return pst;
    }

}