package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.controller.operday.task.EtlStructureMonitorTask;
import ru.rbt.barsgl.ejb.entity.acc.AccRlnId;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount.RelationType;
import ru.rbt.barsgl.ejb.entity.acc.GlAccRln;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.dict.CurrencyRate;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.entity.gl.GlPdTh;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.barsgl.ejb.integr.bg.EtlTechnicalPostingController;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.barsgl.shared.enums.EnumUtils;
import ru.rbt.barsgl.shared.enums.OperState;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.entity.etl.EtlPackage.PackageState.LOADED;

/**
 * Created by Ivan Sevastyanov
 * Обработка операций GL в формате простой проводки
 * @fsd 7.5
 */
public class EtlMessageTest extends AbstractTimerJobTest {

    public static final Logger log = Logger.getLogger(EtlMessageTest.class.getName());

    @BeforeClass
    public static void beforeClass() throws ParseException {
//        Date operday = DateUtils.parseDate("2015-02-26", "yyyy-MM-dd");
        Date operday = DateUtils.parseDate("2016-03-23", "yyyy-MM-dd");
        setOperday(operday, DateUtils.addDays(operday, -1), ONLINE, OPEN);
    }

    /**
     * Обработка операции в одном филиале, в одной валюте (1 проводка)
     * @fsd 7.5.2.1
     * @throws ParseException
     */
    @Test public void test() throws ParseException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountCredit("40817036200012959997");
        pst.setAccountDebit("40817036250010000018");
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());
        pst.setSourcePosting(GLOperation.srcKondorPlus);
        pst.setDealId("123");

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertEquals(getOperday().getCurrentDate(), operation.getCurrentDate());
        Assert.assertEquals(getOperday().getLastWorkdayStatus(), operation.getLastWorkdayStatus());

        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);                 // 1 проводка
        Assert.assertEquals(postList.size(), 1);

        List<Pd> pdList = getPostingPd(postList.get(0));
        Pd pdDr = pdList.get(0);
        Pd pdCr = pdList.get(1);
        Assert.assertTrue(pdDr.getCcy().equals(operation.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит
        Assert.assertTrue(pdCr.getCcy().equals(pdDr.getCcy()));                 // валюта одинаковый
        Assert.assertEquals(StringUtils.leftPad(pst.getDealId(), 6, "0"), pdDr.getPref());
        Assert.assertEquals(StringUtils.leftPad(pst.getDealId(), 6, "0"), pdCr.getPref());

        Assert.assertTrue(pdCr.getAmount() == operation.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCr.getAmount() == -pdDr.getAmount());       // сумма в валюте дебет - кредит

    }

    /**
     * Обработка операции из PaymentHub с пустым полем PMT_REF
     * @fsd 7.5.2.1
     * @throws ParseException
     */
    @Test public void testPH() throws ParseException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountCredit("40817036200012959997");
        pst.setAccountDebit("40817036250010000018");
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());
        pst.setSourcePosting(GLOperation.srcPaymentHub);
        pst.setDealId("123");
        pst.setPaymentRefernce(null);

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertEquals(getOperday().getCurrentDate(), operation.getCurrentDate());
        Assert.assertEquals(getOperday().getLastWorkdayStatus(), operation.getLastWorkdayStatus());

        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);                 // 1 проводка
        Assert.assertEquals(postList.size(), 1);

        List<Pd> pdList = getPostingPd(postList.get(0));
        Pd pdDr = pdList.get(0);
        Pd pdCr = pdList.get(1);
        Assert.assertTrue(pdDr.getCcy().equals(operation.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит
        Assert.assertTrue(pdCr.getCcy().equals(pdDr.getCcy()));                 // валюта одинаковый
        Assert.assertEquals("", pdDr.getPref());
        Assert.assertEquals("", pdCr.getPref());

        Assert.assertTrue(pdCr.getAmount() == operation.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCr.getAmount() == -pdDr.getAmount());       // сумма в валюте дебет - кредит

    }

    /**
     * Обработка межфилиальной операции в одной валюте (2 проводки)
     * @fsd 7.5.2.2
     * @throws ParseException
     */
    @Test public void testMfo() throws ParseException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "MFO");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

//        pst.setAccountCredit("40817978550160000066");     // "CHL" клиент
        pst.setAccountCredit("47411978750020010096");       // "SPB" не клиент
        pst.setAccountDebit("47427978400404502369");        // "EKB" клиент

        pst.setAmountCredit(new BigDecimal("321.56"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.EUR);
        pst.setCurrencyDebit(pst.getCurrencyCredit());
        pst.setSourcePosting(GLOperation.srcKondorPlus);

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());       // операция создана
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(operation.getState(), OperState.POST);

        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);                 // 2 проводки
        Assert.assertEquals(postList.size(), 2);

        for (GLPosting post: postList) {                // в каждой проводке:
            List<Pd> pdList = getPostingPd(post);
            Pd pdDr = pdList.get(0);
            Pd pdCr = pdList.get(1);
            Assert.assertTrue(pdDr.getCcy().equals(operation.getCurrencyDebit()));  // валюта дебет
            Assert.assertTrue(pdCr.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит
            Assert.assertTrue(pdCr.getCcy().equals(pdDr.getCcy()));                 // валюта одинаковый

            Assert.assertTrue(pdCr.getAmount() == operation.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
            Assert.assertTrue(pdCr.getAmount() == -pdDr.getAmount());       // сумма в валюте дебет - кредит

            Assert.assertTrue(pdCr.getAmountBC() == operation.getEquivalentDebit().movePointRight(2).longValue());  // в рублях
            Assert.assertTrue(-pdDr.getAmount() == pdCr.getAmount());       // сумма в валюте дебет - кредит
        }
    }

    /**
     * Обработка операции в одном филиале, в разной валюте (2 проводки)
     * @fsd 7.5.2.1, 10.5
     * @throws ParseException
     */
    @Test public void testExch() throws ParseException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "Exchange");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountDebit("30302840700010000033");    // "MOS"
        pst.setCurrencyDebit(BankCurrency.USD);
        pst.setAmountDebit(new BigDecimal("100.00"));

        pst.setAccountCredit("40702810100013995679");   // "MOS" клиент
        pst.setCurrencyCredit(BankCurrency.RUB);
        pst.setAmountCredit(new BigDecimal("3500.00"));
        pst.setSourcePosting("FC12_CL");
        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());       // операция создана
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());

        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);                 // 2 проводки
        Assert.assertEquals(postList.size(), 2);

        List<Pd> pdList1 = getPostingPd(postList.get(0));
        Pd pdDr = pdList1.get(0);
        Pd pdCr = pdList1.get(1);

        List<Pd> pdList2 = getPostingPd(postList.get(1));
        Pd pdDrE = pdList2.get(0);
        Pd pdCrE = pdList2.get(1);

        // основная проводка
        Assert.assertTrue(pdDr.getCcy().equals(operation.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит

        Assert.assertTrue(-pdDr.getAmount() == operation.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCr.getAmount() == operation.getAmountCredit().movePointRight(2).longValue());  // сумма в валюте

        // курсовая разница
        Assert.assertTrue(pdDrE.getCcy().equals(BankCurrency.RUB));  // валюта дебет
        Assert.assertTrue(pdCrE.getCcy().equals(BankCurrency.RUB)); // валюта кредит

        Assert.assertTrue(pdCrE.getAmountBC() == operation.getExchangeDifference().movePointRight(2).abs().longValue());  // в рублях

        // сумма эквивалентов по дебету и
        Assert.assertTrue(
                    (pdDr.getAmountBC() + pdDrE.getAmountBC() == -operation.getEquivalentDebit().movePointRight(2).longValue())
                ||  (pdCr.getAmountBC() + pdCrE.getAmountBC() == operation.getEquivalentCredit().movePointRight(2).longValue())
                    );

    }

    /**
     * Обработка межфилиальной операции в разной валюте с курсовой разницей (3 проводки)
     * @fsd 7.5.2.2, 10.5
     * @throws ParseException
     */
    @Test public void testMfoExch() throws ParseException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "MfoExchange");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setDealId("1234");
        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountDebit("30302840700010000033");    // "MOS"
        pst.setCurrencyDebit(BankCurrency.USD);
        pst.setAmountDebit(new BigDecimal("100.00"));

        pst.setAccountCredit("47427810550160009330");     // "CHL"
        pst.setCurrencyCredit(BankCurrency.RUB);
        pst.setAmountCredit(new BigDecimal("3500.00"));
        pst.setSourcePosting("FC12_LD");

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());       // операция создана
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(operation.getState(), OperState.POST);

        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);                 // 2 проводки
        Assert.assertEquals(postList.size(), 3);

        // дебет
        List<Pd> pdList1 = getPostingPd( findGLPosting("3", postList) );
        Pd pdDr1 = pdList1.get(0);
        Pd pdCr1 = pdList1.get(1);

        // кредит
        List<Pd> pdList2 = getPostingPd( findGLPosting("4", postList) );
        Pd pdDr2 = pdList2.get(0);
        Pd pdCr2 = pdList2.get(1);

        // курсовая разница
        List<Pd> pdList3 = getPostingPd( findGLPosting("2", postList) );
        Pd pdDrE = pdList3.get(0);
        Pd pdCrE = pdList3.get(1);

        // основные проводки
        Assert.assertTrue(pdDr1.getCcy().equals(operation.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr2.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит

        Assert.assertTrue(-pdDr1.getAmount() == operation.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCr2.getAmount() == operation.getAmountCredit().movePointRight(2).longValue());  // сумма в валюте

        // курсовая разница
        Assert.assertTrue(pdDrE.getCcy().equals(BankCurrency.RUB));  // валюта дебет
        Assert.assertTrue(pdCrE.getCcy().equals(BankCurrency.RUB)); // валюта кредит

        Assert.assertTrue(pdCrE.getAmountBC() == operation.getExchangeDifference().movePointRight(2).abs().longValue());  // в рублях

        // сумма эквивалентов по дебету и
//        Assert.assertTrue(
//                (pdDr1.getAmountBC() + pdDr2.getAmountBC() == -operation.getEquivalentDebit().movePointRight(2).longValue())
//                        ||  (pdCr1.getAmountBC() + pdCr2.getAmountBC() == operation.getEquivalentCredit().movePointRight(2).longValue())
//        );

    }

    /**
     * Обработка межфилиальной операции в одной валюте с пустым значением DEAL_ID (2 проводки)
     * @fsd 7.5.2.2, 10.2.2
     * @throws ParseException
     */
    @Test public void testMfoDealNull() throws ParseException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "MFO");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        Date operday = DateUtils.parseDate("2014-12-29", "yyyy-MM-dd");
        Date lastday = DateUtils.parseDate("2014-12-26", "yyyy-MM-dd");
        setOperday(operday, lastday, ONLINE, OPEN);
        pst.setValueDate(getOperday().getCurrentDate());
        pst.setDealId(null);
        pst.setPaymentRefernce("00001");
        pst.setDeptId(null);

        pst.setAccountCredit("40817810900020709007");   // SPB
        pst.setAccountDebit("40702810300011001792");    // MOS

        pst.setAmountCredit(new BigDecimal("155.000"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.RUB);
        pst.setCurrencyDebit(pst.getCurrencyCredit());
        pst.setSourcePosting("FC12_IC");

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());       // операция создана
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());

        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);                 // 2 проводки
        Assert.assertEquals(postList.size(), 2);

        for (GLPosting post: postList) {                // в каждой проводке:
            List<Pd> pdList = getPostingPd(post);
            Pd pdDr = pdList.get(0);
            Pd pdCr = pdList.get(1);
            Assert.assertTrue(pdDr.getCcy().equals(operation.getCurrencyDebit()));  // валюта дебет
            Assert.assertTrue(pdCr.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит
            Assert.assertTrue(pdCr.getCcy().equals(pdDr.getCcy()));                 // валюта одинаковый

            Assert.assertTrue(pdCr.getAmount() == operation.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
            Assert.assertTrue(pdCr.getAmount() == -pdDr.getAmount());       // сумма в валюте дебет - кредит

            Assert.assertTrue(pdCr.getAmountBC() == operation.getEquivalentDebit().movePointRight(2).longValue());  // в рублях
            Assert.assertTrue(-pdDr.getAmount() == pdCr.getAmount());       // сумма в валюте дебет - кредит
        }
    }

    /**
     * Обработка операции в одном филиале, в разной валюте валюте,
     * если глава не А или нет курсовой разницы (1 проводка)
     * @fsd 7.5.2.1, 10.5
     * @throws ParseException
     */
    @Test public void testNoExch() throws ParseException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setDealId("1234;");
        pst.setValueDate(getOperday().getCurrentDate());

        // глава Г
        pst.setAccountCredit("93902036000014669813");
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setAmountCredit(new BigDecimal("100.00"));

        pst.setAccountDebit("93902810000014668674");
        pst.setCurrencyDebit(BankCurrency.RUB);
        pst.setAmountDebit(new BigDecimal("3500.00"));

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(operation.getState(), OperState.POST);

        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);                 // 1 проводка
        Assert.assertEquals(postList.size(), 1);

        List<Pd> pdList = getPostingPd(postList.get(0));
        Pd pdDr = pdList.get(0);
        Pd pdCr = pdList.get(1);
        Assert.assertTrue(pdDr.getCcy().equals(operation.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит

        Assert.assertTrue(pdCr.getAmountBC() == operation.getAmountPosting().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCr.getAmountBC() == -pdDr.getAmountBC());       // сумма в валюте дебет - кредит

    }

    /**
     * Обработка операции в одном филиале, в разной валюте с заданной суммой в рублях (2 проводки)
     * @fsd 7.5.2.1, 10.5
     * @throws ParseException
     */
    @Test public void testExchRu() throws ParseException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "Exchange");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());
        pst.setDealId("1234;5678");

        pst.setAccountDebit("30302840700010000033");    // "MOS"
        pst.setCurrencyDebit(BankCurrency.USD);
        pst.setAmountDebit(new BigDecimal("100.00"));
        pst.setAmountDebitRu(new BigDecimal("6000.00"));

        pst.setAccountCredit("40702810100013995679");   // "MOS" клиент
        pst.setCurrencyCredit(BankCurrency.RUB);
        pst.setAmountCredit(new BigDecimal("6500.00"));

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());       // операция создана
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(operation.getState(), OperState.POST);

        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);                 // 2 проводки
        Assert.assertEquals(postList.size(), 2);

        List<Pd> pdList1 = getPostingPd(postList.get(0));
        Pd pdDr = pdList1.get(0);
        Pd pdCr = pdList1.get(1);

        List<Pd> pdList2 = getPostingPd(postList.get(1));
        Pd pdDrE = pdList2.get(0);
        Pd pdCrE = pdList2.get(1);

        // основная проводка
        Assert.assertTrue(pdDr.getCcy().equals(operation.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит

        Assert.assertTrue(-pdDr.getAmount() == operation.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCr.getAmount() == operation.getAmountCredit().movePointRight(2).longValue());  // сумма в валюте

        // курсовая разница
        Assert.assertTrue(pdDrE.getCcy().equals(BankCurrency.RUB));  // валюта дебет
        Assert.assertTrue(pdCrE.getCcy().equals(BankCurrency.RUB)); // валюта кредит

        Assert.assertEquals(operation.getAmountPosting(), operation.getAmountCreditRu());
        Assert.assertEquals(operation.getExchangeDifference(),
                operation.getAmountDebitRu().subtract(operation.getAmountCreditRu()));
        Assert.assertTrue(pdCrE.getAmountBC() == operation.getExchangeDifference().movePointRight(2).abs().longValue());  // в рублях

        // сумма эквивалентов по дебету и
        long amountE;
        if (pdDrE.getBsaAcid().equals(operation.getAccountDebit()) || pdDrE.getBsaAcid().equals(operation.getAccountCredit()))
            amountE = pdDrE.getAmountBC();
        else
            amountE = pdCrE.getAmountBC();

        Assert.assertTrue(
                (pdDr.getAmountBC() + amountE == -operation.getEquivalentDebitRu().movePointRight(2).longValue())
            ||  (pdCr.getAmountBC() + amountE == operation.getEquivalentCreditRu().movePointRight(2).longValue())
        );

    }

    /**
     * Обработка операции в одном филиале, в разной валюте,
     * с заданной суммой в рублях (1 проводка)
     * @fsd 7.5.2.1, 10.5
     * @throws ParseException
     */
    @Test public void testNoExchRu() throws ParseException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountDebit("30302840700010000033");    // "MOS"
        pst.setCurrencyDebit(BankCurrency.USD);
        pst.setAmountDebit(new BigDecimal("100.00"));
        pst.setAmountDebitRu(new BigDecimal("6000.00"));

        pst.setAccountCredit("40817036200012959997");
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setAmountCredit(new BigDecimal("200"));
        pst.setAmountCreditRu(new BigDecimal("6000.00"));

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(operation.getState(), OperState.POST);
        Assert.assertEquals(operation.getAmountPosting(), operation.getAmountDebitRu());

        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);                 // 1 проводка
        Assert.assertEquals(postList.size(), 1);

        List<Pd> pdList = getPostingPd(postList.get(0));
        Pd pdDr = pdList.get(0);
        Pd pdCr = pdList.get(1);
        Assert.assertTrue(pdDr.getCcy().equals(operation.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит

        Assert.assertTrue(pdCr.getAmountBC() == operation.getAmountPosting().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCr.getAmountBC() == -pdDr.getAmountBC());       // сумма в валюте дебет - кредит

    }

    @Test public void testJPY() throws ParseException {

        setOperday(DateUtils.parseDate("2015-02-28", "yyyy-MM-dd")
                , DateUtils.parseDate("2015-02-29", "yyyy-MM-dd"), ONLINE, OPEN);

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());


        pst.setAccountDebit("");
        pst.setAccountKeyDebit("001;JPY;00200147;911020201;10;03;0000001326;0001;93307;;3002;01;K+TP;992030;");
        pst.setAmountDebit(new BigDecimal("600000000.000"));
        pst.setCurrencyDebit(new BankCurrency("JPY"));

        pst.setAccountCredit("99997810600010000001");
        pst.setAmountCredit(new BigDecimal("313273800.000"));
        pst.setCurrencyCredit(BankCurrency.RUB);

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertEquals(getOperday().getCurrentDate(), operation.getCurrentDate());
        Assert.assertEquals(getOperday().getLastWorkdayStatus(), operation.getLastWorkdayStatus());

        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);                 // 1 проводка
        Assert.assertEquals(postList.size(), 1);

        List<Pd> pdList = getPostingPd(postList.get(0));
        Pd pdDr = pdList.get(0);
        Pd pdCr = pdList.get(1);
        Assert.assertTrue(pdDr.getCcy().equals(operation.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит
        Assert.assertEquals(pdCr.getCcy(), BankCurrency.RUB);
        Assert.assertEquals(pdDr.getCcy(), new BankCurrency("JPY"));

        Assert.assertTrue(pdCr.getAmount() != -pdDr.getAmount());       // сумма в валюте дебет - кредит
        Assert.assertEquals(operation.getAmountCredit().movePointRight(2).longValue()
                , pdCr.getAmount().longValue());  // сумма в валюте
        Assert.assertEquals(operation.getAmountDebit().longValue()
                , -pdDr.getAmount().longValue());  // сумма в валюте

    }

    /**
     * в случае даты валютирования меньше текущей и больше предыдущей
     * дата проводки в текущем дне если текущий день не в следующем месяце относительно предыдущего рабочего дня
     */
    @Test public void testPostingHolidays() throws Exception {

        long stamp = System.currentTimeMillis();

        final Date longPrev = DateUtils.parseDate("18.01.2015", "dd.MM.yyyy");
        Assert.assertFalse(remoteAccess.invoke(BankCalendarDayRepository.class, "isWorkday", longPrev));
        Date prev = DateUtils.parseDate("24.01.2015", "dd.MM.yyyy");
        Date hold = DateUtils.parseDate("25.01.2015", "dd.MM.yyyy");
        Date curr = DateUtils.parseDate("27.01.2015", "dd.MM.yyyy");
        List<DataRecord> days = baseEntityRepository.select("select * from cal where dat between ? and ? and ccy = 'RUR' and thol <> 'X'"
                , prev, curr);
        Assert.assertEquals(2, days.size());
        final Date finalCurr = curr;
        Assert.assertEquals(2, days.stream().filter(rec
                -> rec.getDate("dat").equals(prev) || rec.getDate("dat").equals(finalCurr)).collect(Collectors.toList()).size());

        setOperday(curr, prev, ONLINE,OPEN);

        EtlPackage pkg = newPackage(stamp, "HILPST");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(hold);

        pst.setAccountCredit("40817036200012959997");
        pst.setAccountDebit("40817036250010000018");
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());
        pst.setSourcePosting(GLOperation.srcKondorPlus);
        pst.setDealId("123");

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertEquals(getOperday().getCurrentDate(), operation.getCurrentDate());
        Assert.assertEquals(getOperday().getLastWorkdayStatus(), operation.getLastWorkdayStatus());
        Assert.assertEquals(operation.getPostDate()+"",curr, operation.getPostDate());

        // далеко назад выходной
        pst.setValueDate(longPrev);
        operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertEquals(getOperday().getCurrentDate(), operation.getCurrentDate());
        Assert.assertEquals(getOperday().getLastWorkdayStatus(), operation.getLastWorkdayStatus());
        Assert.assertEquals(operation.getPostDate()+"", curr, operation.getPostDate());

        // переход через месяц
        curr = DateUtils.parseDate("16.02.2015", "dd.MM.yyyy");
        setOperday(curr, prev, ONLINE,OPEN);

        pst.setValueDate(prev);
        operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertEquals(getOperday().getCurrentDate(), operation.getCurrentDate());
        Assert.assertEquals(getOperday().getLastWorkdayStatus(), operation.getLastWorkdayStatus());
        Assert.assertEquals(operation.getPostDate()+"", prev, operation.getPostDate());

        // дата в будущем !!
        setOperday(curr, prev, ONLINE,OPEN);

        pst.setValueDate(DateUtils.addDays(curr, 1));
        operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.ERCHK, operation.getState());
        Assert.assertNotNull(operation.getProcDate());
        Assert.assertEquals(curr, operation.getProcDate());

    }

    /**
     * Обработка ошибочных проводок с клиентским счетом в корреспонденции
     */
    @Test public void testClientErrors() throws SQLException {
        updateOperday(ONLINE,OPEN);

        log.info("deleted technicals: " + baseEntityRepository.executeNativeUpdate("delete from gl_acc where acctype in (?,?)", "999010201", "999010101"));

        // создаем операцию, которая должна упасть с ERCHK или WTAC
        String clientAccount = findBsaAccount("40701840%");
        Assert.assertTrue(!StringUtils.isEmpty(clientAccount));

        incldeBs2ByBsaacid(clientAccount);

        long stamp = System.currentTimeMillis();
        EtlPackage pkg = newPackage(stamp, "HILPST");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountCredit(clientAccount);
        final String failedAcDt = "01909840742938470934";
        pst.setAccountDebit(failedAcDt);
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.USD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());
        pst.setSourcePosting(GLOperation.srcKondorPlus);
        pst.setDealId("123");

        pst = (EtlPosting) baseEntityRepository.save(pst);

        pst = (EtlPosting) baseEntityRepository.selectFirst(EtlPosting.class
                , "from EtlPosting p join fetch p.etlPackage k where p = ?1", pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        operation = (GLOperation) baseEntityRepository.selectFirst(GLOperation.class
                , "from GLOperation o where o.id = ?1", operation.getId());

        Assert.assertTrue(EnumUtils.contains(new OperState[]{OperState.ERCHK, OperState.WTAC}, operation.getState()));

        // подаем ошибочную проводку на вход обработчика ошибок
        GLOperation operationTrans = remoteAccess.invoke(EtlTechnicalPostingController.class, "processMessage", pst);
        operationTrans = (GLOperation) baseEntityRepository.findById(GLOperation.class, operationTrans.getId());
        Assert.assertNotNull(operationTrans);
        Assert.assertEquals(OperState.POST, operationTrans.getState());

        final String idpst1 = pst.getAePostingId();
        EtlPosting transitPosting = (EtlPosting) baseEntityRepository.selectFirst(EtlPosting.class
                , "from EtlPosting t where t.aePostingId = ?1", idpst1 + "*");
        Assert.assertNotNull(transitPosting);
        Assert.assertTrue(transitPosting.getAccountDebit(), transitPosting.getAccountDebit().matches("47423\\d{15}"));
        Assert.assertEquals(clientAccount, transitPosting.getAccountCredit());

        GlAccRln rln = (GlAccRln) baseEntityRepository.findById(GlAccRln.class, new AccRlnId("", transitPosting.getAccountDebit()));
        Assert.assertNotNull(rln);
        Assert.assertEquals(RelationType.E, RelationType.parse(rln.getRelationType()));
        Assert.assertEquals(new Integer("0"), transitPosting.getErrorCode());
        Assert.assertEquals("SUCCESS", transitPosting.getErrorMessage());

        pst = (EtlPosting) baseEntityRepository.findById(EtlPosting.class, pst.getId());
        Assert.assertEquals(pst.getAccountCredit(), transitPosting.getAccountDebit());

        // повторная обработка - тот же счет

        final String idpst2 = ru.rbt.ejbcore.util.StringUtils.rsubstr(System.currentTimeMillis() + "", 8);
        baseEntityRepository.executeUpdate("update EtlPosting p set p.aePostingId = ?1, p.accountDebit = ?2, p.accountCredit = ?3 where p.id = ?4"
                , idpst2, failedAcDt, clientAccount, pst.getId());
        pst = (EtlPosting) baseEntityRepository.findById(EtlPosting.class, pst.getId());

        GLOperation operationTrans2 = remoteAccess.invoke(EtlTechnicalPostingController.class, "processMessage", pst);
        EtlPosting transitPosting2 = (EtlPosting) baseEntityRepository.selectFirst(EtlPosting.class
                , "from EtlPosting t where t.aePostingId = ?1", idpst2 + "*");
        Assert.assertNotNull(transitPosting2);

        Assert.assertNotEquals(transitPosting.getId(), transitPosting2.getId());
        Assert.assertEquals(transitPosting.getAccountDebit(), transitPosting2.getAccountDebit());
        Assert.assertEquals(operationTrans.getAccountDebit(), operationTrans2.getAccountDebit());


    }

    @Test public void testClientErrorsAsync() throws SQLException {
        updateOperday(ONLINE,OPEN);

        log.info("deleted technicals: " + baseEntityRepository.executeNativeUpdate("delete from gl_acc where acctype in (?,?)", "999010201", "999010101"));

        // создаем операцию, которая должна упасть с ERCHK или WTAC
        String clientAccount = findBsaAccount("40701840%");
        Assert.assertTrue(!StringUtils.isEmpty(clientAccount));

        incldeBs2ByBsaacid(clientAccount);

        long stamp = System.currentTimeMillis();
        EtlPackage pkg = newPackage(stamp, "HILPST");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountCredit(clientAccount);
        final String failedAcDt = "01909840742938470934";
        pst.setAccountDebit(failedAcDt);
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.USD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());
        pst.setSourcePosting(GLOperation.srcKondorPlus);
        pst.setDealId("123");

        pst = (EtlPosting) baseEntityRepository.save(pst);

        pst = (EtlPosting) baseEntityRepository.selectFirst(EtlPosting.class
                , "from EtlPosting p join fetch p.etlPackage k where p = ?1", pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        operation = (GLOperation) baseEntityRepository.selectFirst(GLOperation.class
                , "from GLOperation o where o.id = ?1", operation.getId());

        Assert.assertTrue(EnumUtils.contains(new OperState[]{OperState.ERCHK, OperState.WTAC}, operation.getState()));

        baseEntityRepository.executeUpdate("update GLOperation o set o.state = ?1 where o.id = ?2", OperState.ERCHK, operation.getId());

        // подаем ошибочную проводку на вход обработчика ошибок
        remoteAccess.invoke(EtlTechnicalPostingController.class, "reprocessPostingByPackage", pkg);
        GLOperation operationTrans = (GLOperation) baseEntityRepository
                .selectFirst(GLOperation.class, "from GLOperation o where o.aePostingId = ?1", operation.getAePostingId()+"*");
        Assert.assertNotNull(operationTrans);
        Assert.assertEquals(OperState.POST, operationTrans.getState());

    }

    /**
     * обработка при выполнении задачи мониторинга сообщ АЕ
     * @throws SQLException
     */
    @Ignore // ошибка при backvalue
    @Test public void testClientErrorsMonitor() throws Exception {
        updateOperday(ONLINE,OPEN);

        log.info("updated = " + baseEntityRepository.executeNativeUpdate("update gl_etlpkg set dt_load = ? where dt_load > ?"
            , DateUtils.addDays(new Date(), -10), DateUtils.addDays(new Date(), -10)));

        log.info("deleted technicals: " + baseEntityRepository.executeNativeUpdate("delete from gl_acc where acctype in (?,?)", "999010201", "999010101"));

        // создаем операцию, которая должна упасть с ERCHK или WTAC
        String clientAccount = findBsaAccount("40701840%");
        Assert.assertTrue(!StringUtils.isEmpty(clientAccount));

        incldeBs2ByBsaacid(clientAccount);

        long stamp = System.currentTimeMillis();
        EtlPackage pkg = newPackage(stamp, "HILPST");
        baseEntityRepository.executeUpdate("update EtlPackage p set p.packageState = ?1 where p.id = ?2"
                , LOADED, pkg.getId());
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountCredit(clientAccount);
        final String failedAcDt = "01909840742938470934";
        pst.setAccountDebit(failedAcDt);
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.USD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());
        pst.setSourcePosting(GLOperation.srcKondorPlus);
        pst.setDealId("123");

        pst = (EtlPosting) baseEntityRepository.save(pst);

        pst = (EtlPosting) baseEntityRepository.selectFirst(EtlPosting.class
                , "from EtlPosting p join fetch p.etlPackage k where p = ?1", pst);

        jobService.executeJob(SingleActionJobBuilder.create().withClass(EtlStructureMonitorTask.class).build());

        GLOperation operation = (GLOperation) baseEntityRepository
                .selectFirst(GLOperation.class, "from GLOperation o where o.aePostingId = ?1", pst.getAePostingId());
        Assert.assertNotNull(operation);
        operation = (GLOperation) baseEntityRepository.selectFirst(GLOperation.class
                , "from GLOperation o where o.id = ?1", operation.getId());

        Assert.assertTrue(EnumUtils.contains(new OperState[]{OperState.ERCHK, OperState.WTAC}, operation.getState()));

        baseEntityRepository.executeUpdate("update GLOperation o set o.state = ?1 where o.id = ?2", OperState.ERCHK, operation.getId());
        // меняем статус пакеиа на LOAD чтоб прошла переобработка ошибок
        baseEntityRepository.executeUpdate("update EtlPackage p set p.packageState = ?1 where p.id = ?2"
                , LOADED, pkg.getId());

        jobService.executeJob(SingleActionJobBuilder.create().withClass(EtlStructureMonitorTask.class).build());

        GLOperation operationTrans = (GLOperation) baseEntityRepository
                .selectFirst(GLOperation.class, "from GLOperation o where o.aePostingId = ?1", operation.getAePostingId()+"*");
        Assert.assertNotNull(operationTrans);
        Assert.assertEquals(OperState.POST, operationTrans.getState());

    }

    private void incldeBs2ByBsaacid(String bsaacid) throws SQLException {
        baseEntityRepository.executeNativeUpdate("delete from GL_STMPARM");
        String acc = bsaacid.substring(0,5);
        baseEntityRepository
                .executeNativeUpdate("insert into gl_stmparm (account, INCLUDE,acctype, INCLUDEBLN) values (?, '1', 'B', '1')", acc);
    }

    /**
     * Тестовый метод проверки работы с техническими счетами
     */
    @Test public void testTechAccountPostingProcessor() throws ParseException {

        Operday oldOperday = getOperday();
        Date curDate = DateUtils.parseDate("2017-03-13","yyy-MM-dd");
        setOperday(curDate,curDate, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
        updateOperday(ONLINE, OPEN, Operday.PdMode.DIRECT);

        //Добавление нового курса
        CurrencyRate currencyRate = new CurrencyRate(new BankCurrency("USD"),new Date(),BigDecimal.valueOf(58.95),BigDecimal.valueOf(1.0));
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

        setOperday(oldOperday.getCurrentDate(),oldOperday.getLastWorkingDay(), oldOperday.getPhase(), oldOperday.getLastWorkdayStatus());
        updateOperday(ONLINE, OPEN, Operday.PdMode.DIRECT);
    }


    @Test public void testNotTechAccountPostingProcessor() throws ParseException {

        //Operday oldOperday = getOperday();
        //Date curDate = DateUtils.parseDate("2017-03-13","yyy-MM-dd");
        //setOperday(curDate,curDate, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
        //updateOperday(ONLINE, OPEN, Operday.PdMode.DIRECT);

        //Добавление нового курса
         //CurrencyRate currencyRate = new CurrencyRate(new BankCurrency("USD"),new Date(),BigDecimal.valueOf(58.95),BigDecimal.valueOf(1.0));
         //baseEntityRepository.save(currencyRate);

        //List<CurrencyRate> curRate = baseEntityRepository.select(CurrencyRate.class,"from CurrencyRate cr where cr.id.rateDt = ?1",new Date());
        //Assert.assertFalse("Не найден курс на текущую дату. Раскоментируйте код добавления курса.",curRate.isEmpty());


        //Удаление записей по техничесим с счетам.
        //this.clearTechRecords();


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


    @Test public void LoadEtlFromFile() throws IOException, InvalidFormatException, ParseException {

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
;
