package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.controller.operday.task.EtlStructureMonitorTask;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount.RelationType;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.barsgl.ejb.integr.bg.EtlTechnicalPostingController;
import ru.rbt.barsgl.ejb.repository.dict.FwPostSourceCachedRepository;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.barsgl.shared.criteria.CriteriaBuilder;
import ru.rbt.barsgl.shared.criteria.CriteriaLogic;
import ru.rbt.barsgl.shared.enums.DealSource;
import ru.rbt.barsgl.shared.enums.EnumUtils;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.datarec.DataRecord;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.DIRECT;
import static ru.rbt.barsgl.ejb.entity.etl.EtlPackage.PackageState.LOADED;
import static ru.rbt.barsgl.shared.enums.DealSource.*;
import static ru.rbt.ejbcore.util.StringUtils.substr;

/**
 * Created by Ivan Sevastyanov
 * Обработка операций GL в формате простой проводки
 * @fsd 7.5
 */
public class EtlMessageIT extends AbstractTimerJobIT {

    public static final Logger log = Logger.getLogger(EtlMessageIT.class.getName());
    private static final Operday.PdMode pdMode = DIRECT;
//    private static final Operday.PdMode pdMode = BUFFER;

    @BeforeClass
    public static void beforeClass() throws ParseException {
//        Date operday = DateUtils.parseDate("2015-02-26", "yyyy-MM-dd");
        Date operday = DateUtils.parseDate("2016-03-23", "yyyy-MM-dd");
        setOperday(operday, DateUtils.addDays(operday, -1), ONLINE, OPEN, pdMode);

        baseEntityRepository.executeNativeUpdate("update gl_acc set dto = ? where bsaacid like '93307392%'", DateUtils.parseDate("2015-01-01", "yyyy-MM-dd"));

        baseEntityRepository.executeNativeUpdate("delete from CAL where DAT between '2015-01-23' and '2015-01-26' and CCY = 'RUR'");
        baseEntityRepository.executeNativeUpdate("insert into CAL values ('2015-01-23', ' ', 'RUR', ' ')");
        baseEntityRepository.executeNativeUpdate("insert into CAL values ('2015-01-24', 'X', 'RUR', 'X')");
        baseEntityRepository.executeNativeUpdate("insert into CAL values ('2015-01-25', 'X', 'RUR', 'X')");
        baseEntityRepository.executeNativeUpdate("insert into CAL values ('2015-01-26', ' ', 'RUR', ' ')");
        baseEntityRepository.executeNativeUpdate("delete from CAL where DAT = '2015-01-31' and CCY = 'RUR'");
        baseEntityRepository.executeNativeUpdate("insert into CAL values ('2015-01-31', 'X', 'RUR', 'T')");
    }

    /**
     * Обработка операции в одном филиале, в одной валюте (1 проводка)
     * @fsd 7.5.2.1
     * @throws ParseException
     */
    @Test public void test() throws ParseException, SQLException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        String bsaCt = findBsaAccount("40817036%", getOperday().getCurrentDate());
        String bsaDt = findBsaAccount("40817036%", getOperday().getCurrentDate(), CriteriaBuilder.create(CriteriaLogic.AND).appendNOT("bsaacid", bsaCt).build());
        pst.setAccountCredit(bsaCt);
        pst.setAccountDebit(bsaDt);
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());
        pst.setSourcePosting(KondorPlus.getLabel());
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
        Assert.assertEquals(StringUtils.leftPad(pst.getDealId(), 6, "0"), pdDr.getPref().trim());
        Assert.assertEquals(StringUtils.leftPad(pst.getDealId(), 6, "0"), pdCr.getPref().trim());

        Assert.assertTrue(pdCr.getAmount() == operation.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCr.getAmount() == -pdDr.getAmount());       // сумма в валюте дебет - кредит

    }

    /**
     * Обработка операции из PaymentHub с пустым полем PMT_REF
     * @fsd 7.5.2.1
     * @throws ParseException
     */
    @Test public void testPH() throws ParseException, SQLException {

//        updateOperdayMode(BUFFER, ProcessingStatus.STARTED);
        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        String bsaCt = findBsaAccount("40817036%", getOperday().getCurrentDate());
        String bsaDt = findBsaAccount("40817036%", getOperday().getCurrentDate(), CriteriaBuilder.create(CriteriaLogic.AND).appendNOT("bsaacid", bsaCt).build());
        pst.setAccountCredit(bsaCt);
        pst.setAccountDebit(bsaDt);
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());
        pst.setSourcePosting(PaymentHub.getLabel());
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
        Assert.assertEquals(ru.rbt.ejbcore.util.StringUtils.leftSpace(" ", 20), pdDr.getPref());
        Assert.assertEquals(ru.rbt.ejbcore.util.StringUtils.leftSpace(" ", 20), pdCr.getPref());

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
        pst.setSourcePosting(KondorPlus.getLabel());

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
    @Test public void testMfoExchD() throws ParseException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "MfoExchange");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setDealId("1234");
        pst.setValueDate(getOperday().getCurrentDate());

        String accVal = "40702840000010002486";
        pst.setAccountDebit(accVal);    // "MOS"
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

        Pd pdProfitLoss = pdList3.stream().filter(p -> p.getBsaAcid().startsWith("706")).findFirst().orElseThrow(() -> new RuntimeException("Not found 706% posting"));

        Assert.assertEquals(accVal + ":" + pdProfitLoss.getBsaAcid()
                , substr(accVal, 10, 13), substr(pdProfitLoss.getBsaAcid(), 10, 13));

    }

    /**
     * Обработка межфилиальной операции в разной валюте с курсовой разницей (3 проводки)
     * оба счета валютные
     * @fsd 7.5.2.2, 10.5
     * @throws ParseException
     */
    @Test public void testMfoExchD_val() throws ParseException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "MfoExchange");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setDealId("1234");
        pst.setValueDate(getOperday().getCurrentDate());

        String accountDebit = "40702840000010002486";
        pst.setAccountDebit(accountDebit);    // "MOS"
        pst.setCurrencyDebit(BankCurrency.USD);
        pst.setAmountDebit(new BigDecimal("100.00"));

        pst.setAccountCredit("40702978000164522105");     // "CHL"
        pst.setCurrencyCredit(BankCurrency.EUR);
        pst.setAmountCredit(new BigDecimal("90.00"));
        pst.setSourcePosting("FC12_LD");

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());       // операция создана
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(operation.getState(), OperState.POST);

        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);
        Assert.assertEquals(3,postList.size());

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


        Pd pdProfitLoss = pdList3.stream().filter(p -> p.getBsaAcid().startsWith("706")).findFirst().orElseThrow(() -> new RuntimeException("Not found 706% posting"));

        Assert.assertEquals(accountDebit + ":" + pdProfitLoss.getBsaAcid()
                , substr(accountDebit, 10, 13), substr(pdProfitLoss.getBsaAcid(), 10, 13));

    }

    /**
     * Обработка межфилиальной операции в разной валюте с курсовой разницей (3 проводки)
     * валютный счет по кредиту
     * @fsd 7.5.2.2, 10.5
     * @throws ParseException
     */
    @Test public void testMfoExchC() throws ParseException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "MfoExchange");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setDealId("1234");
        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountDebit("47427810550160009330");     // "CHL"
        pst.setCurrencyDebit(BankCurrency.RUB);
        pst.setAmountDebit(new BigDecimal("3500.00"));

        final String accValMos = "40702840000010002486";
        pst.setAccountCredit(accValMos);   // "MOS"
        pst.setCurrencyCredit(BankCurrency.USD);
        pst.setAmountCredit(new BigDecimal("100.00"));
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

        Pd pdProfitLoss = pdList3.stream().filter(p -> p.getBsaAcid().startsWith("706")).findFirst().orElseThrow(() -> new RuntimeException("Not found 706% posting"));

        Assert.assertEquals(accValMos + ":" + pdProfitLoss.getBsaAcid()
                , substr(accValMos, 10, 13), substr(pdProfitLoss.getBsaAcid(), 10, 13));

    }

    /**
     * Обработка межфилиальной операции в одной валюте с пустым значением DEAL_ID (2 проводки)
     * @fsd 7.5.2.2, 10.2.2
     * @throws ParseException
     */
    @Test public void testMfoDealNull() throws ParseException, SQLException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "MFO");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        Date operday = DateUtils.parseDate("2014-12-29", "yyyy-MM-dd");
        Date lastday = DateUtils.parseDate("2014-12-26", "yyyy-MM-dd");
        setOperday(operday, lastday, ONLINE, OPEN, pdMode);
        pst.setValueDate(getOperday().getCurrentDate());
        pst.setDealId(null);
        pst.setPaymentRefernce("00001");
        pst.setDeptId(null);

        String acCt = Utl4Tests.findBsaacid(baseEntityRepository, operday, "40817810_0002%");      // SPB
        String acDt = Utl4Tests.findBsaacid(baseEntityRepository, operday, "40702810_0001%");       // MOS
        pst.setAccountCredit(acCt);
        pst.setAccountDebit(acDt);

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
    @Test public void testNoExchRu() throws Exception {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountDebit(findBsaAccount("40701840%", getOperday().getCurrentDate(), filialCriteria("MOS")));    // "MOS"
        pst.setCurrencyDebit(BankCurrency.USD);
        pst.setAmountDebit(new BigDecimal("100.00"));
        pst.setAmountDebitRu(new BigDecimal("6000.00"));

        pst.setAccountCredit(findBsaAccount("408__036%", getOperday().getCurrentDate(), filialCriteria("MOS")));
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setAmountCredit(new BigDecimal("200"));
        pst.setAmountCreditRu(new BigDecimal("6000.00"));

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertEquals(operation.getAmountPosting(), operation.getAmountDebitRu());

        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);                 // 1 проводка
        Assert.assertEquals(1, postList.size());

        List<Pd> pdList = getPostingPd(postList.get(0));
        Pd pdDr = pdList.get(0);
        Pd pdCr = pdList.get(1);
        Assert.assertTrue(pdDr.getCcy().equals(operation.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит

        Assert.assertTrue(pdCr.getAmountBC() == operation.getAmountPosting().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCr.getAmountBC() == -pdDr.getAmountBC());       // сумма в валюте дебет - кредит


    }

    @Test public void testJPY() throws ParseException, SQLException {

        setOperday(DateUtils.parseDate("2015-02-28", "yyyy-MM-dd")
                , DateUtils.parseDate("2015-02-27", "yyyy-MM-dd"), ONLINE, OPEN, pdMode);

        long stamp = System.currentTimeMillis();
        final BankCurrency JPY = new BankCurrency("JPY");

        baseEntityRepository.executeNativeUpdate("delete from currates where dat = ? and ccy = ?"
            , getOperday().getCurrentDate(), JPY.getCurrencyCode());
        checkCreateBankCurrency(getOperday().getCurrentDate(), JPY, new BigDecimal("0.555"));

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());


        pst.setAccountDebit("");

        String accdt = findBsaAccount("47427392%");

        pst.setAccountDebit(accdt);
        pst.setAmountDebit(new BigDecimal("60.000"));
        pst.setCurrencyDebit(JPY);

        String creditAccount = findBsaAccount("40702392%", getOperday().getCurrentDate());

        pst.setAccountCredit(creditAccount);
        pst.setAmountCredit(pst.getAmountDebit());
        pst.setCurrencyCredit(JPY);

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
        Assert.assertEquals(1, postList.size());

        List<Pd> pdList = getPostingPd(postList.get(0));
        Pd pdDr = pdList.get(0);
        Pd pdCr = pdList.get(1);
        Assert.assertTrue(pdDr.getCcy().equals(operation.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит
        Assert.assertEquals(JPY, pdCr.getCcy());
        Assert.assertEquals(pdDr.getCcy(), JPY);

        Assert.assertTrue(pdCr.getAmount() == -pdDr.getAmount());       // сумма в валюте дебет - кредит
        Assert.assertEquals(operation.getAmountCredit().longValue()
                , pdCr.getAmount().longValue());  // сумма в валюте
        Assert.assertEquals(operation.getAmountDebit().longValue()
                , -pdDr.getAmount().longValue());  // сумма в валюте

        Assert.assertEquals(new BigDecimal("33.3"), operation.getEquivalentCredit());
        Assert.assertEquals(new BigDecimal("33.3"), operation.getEquivalentDebit());

    }

    /**
     * в случае даты валютирования меньше текущей и больше предыдущей
     * дата проводки в текущем дне если текущий день не в следующем месяце относительно предыдущего рабочего дня
     */
    @Test public void testPostingHolidays() throws Exception {

        long stamp = System.currentTimeMillis();

        final Date longPrev = DateUtils.parseDate("18.01.2015", "dd.MM.yyyy");
        Assert.assertFalse(remoteAccess.invoke(BankCalendarDayRepository.class, "isWorkday", longPrev));
        Date prev = DateUtils.parseDate("23.01.2015", "dd.MM.yyyy");
        Date hold = DateUtils.parseDate("25.01.2015", "dd.MM.yyyy");
        Date curr = DateUtils.parseDate("26.01.2015", "dd.MM.yyyy");

        List<DataRecord> days = baseEntityRepository.select("select * from cal where dat between ? and ? and ccy = 'RUR' and thol <> 'X'"
                , prev, curr);
        Assert.assertEquals(2, days.size());
        final Date finalCurr = curr;
        final Date finalPrev = prev;
        Assert.assertEquals(2, days.stream().filter(rec
                -> rec.getDate("dat").equals(finalPrev) || rec.getDate("dat").equals(finalCurr)).collect(Collectors.toList()).size());

        setOperday(curr, prev, ONLINE,OPEN, pdMode);

        EtlPackage pkg = newPackage(stamp, "HILPST");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        String bsaCt = findBsaAccount("40817036%", getOperday().getCurrentDate());
        String bsaDt = findBsaAccount("40817036%", getOperday().getCurrentDate(), CriteriaBuilder.create(CriteriaLogic.AND).appendNOT("bsaacid", bsaCt).build());
        pst.setAccountCredit(bsaCt);
        pst.setAccountDebit(bsaDt);
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());
        pst.setDealId("123");

        pst.setSourcePosting(DealSource.PaymentHub.getLabel());
        pst.setValueDate(hold);
        pst = (EtlPosting) baseEntityRepository.save(pst);

        // K+TP выходной
        processPst(pst, curr);

        // далеко назад выходной
        pst.setValueDate(longPrev);
        Date wday = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateAfter", longPrev, false);
        processPst(pst, wday);

        // переход через месяц
        curr = DateUtils.parseDate("16.02.2015", "dd.MM.yyyy");
        setOperday(curr, prev, ONLINE,OPEN, pdMode);
        pst.setValueDate(prev);
        processPst(pst, prev);

        // переход через месяц - технический опердень
        curr = DateUtils.parseDate("10.02.2015", "dd.MM.yyyy");
        prev = DateUtils.parseDate("09.02.2015", "dd.MM.yyyy");
        setOperday(curr, prev, ONLINE, OPEN, pdMode);

        // ARMPRO
        Date holdMonth = DateUtils.parseDate("31.01.2015", "dd.MM.yyyy");
        pst.setValueDate(holdMonth);
        pst.setSourcePosting(ARMPRO.getLabel());
        processPst(pst, holdMonth);

        // AOS
        setFwPostingSource(AOS.getLabel(), curr, null);
        pst.setSourcePosting(AOS.getLabel());
        wday = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateAfter", holdMonth, false);
        processPst(pst, wday);

        // дата в будущем !!
        setOperday(curr, prev, ONLINE, OPEN, pdMode);
        pst.setValueDate(DateUtils.addDays(curr, 1));
        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.ERCHK, operation.getState());
        Assert.assertNotNull(operation.getProcDate());
        Assert.assertEquals(curr, operation.getProcDate());

    }

    @Test public void testFwPostSources() throws Exception {
        long stamp = System.currentTimeMillis();

        Date holdMonth = DateUtils.parseDate("31.01.2015", "dd.MM.yyyy");

        Date prev = DateUtils.parseDate("06.02.2015", "dd.MM.yyyy");
        Date curr = DateUtils.parseDate("09.02.2015", "dd.MM.yyyy");
        Date next = DateUtils.parseDate("10.02.2015", "dd.MM.yyyy");
        EtlPackage pkg = newPackage(stamp, "AOSPST");
        Assert.assertTrue(pkg.getId() > 0);

        setOperday(curr, prev, ONLINE, OPEN, pdMode);

        EtlPosting pst = newPosting(stamp, pkg);
        String bsaCt = findBsaAccount("40817036%", getOperday().getCurrentDate());
        String bsaDt = findBsaAccount("40817036%", getOperday().getCurrentDate(), CriteriaBuilder.create(CriteriaLogic.AND).appendNOT("bsaacid", bsaCt).build());
        pst.setAccountCredit(bsaCt);
        pst.setAccountDebit(bsaDt);
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());
        pst.setDealId("123");

        pst.setSourcePosting(AOS.getLabel());
        pst.setValueDate(holdMonth);
        pst = (EtlPosting) baseEntityRepository.save(pst);
        Date nextday = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateAfter", holdMonth, false);
        Date prevday = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateBefore", holdMonth, false);

        setFwPostingSource(AOS.getLabel(), curr, null);
        processPst(pst, nextday);

        setFwPostingSource(AOS.getLabel(), prev, prev);
        processPst(pst, prevday);

        setFwPostingSource(AOS.getLabel(), next, null);
        processPst(pst, prevday);

        setFwPostingSource(AOS.getLabel(), null, null);
        processPst(pst, prevday);

        setFwPostingSource(AOS.getLabel(), curr, null);
        processPst(pst, nextday);
    }

    private void processPst(EtlPosting pst, Date wday) {
        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertTrue(OperState.POST == operation.getState() || OperState.BLOAD == operation.getState());
        Assert.assertEquals(getOperday().getCurrentDate(), operation.getCurrentDate());
        Assert.assertEquals(getOperday().getLastWorkdayStatus(), operation.getLastWorkdayStatus());
        Assert.assertEquals(operation.getPostDate()+"", wday, operation.getPostDate());
    }

    private void setFwPostingSource(String src, Date startdate, Date enddate) {
        baseEntityRepository.executeNativeUpdate("delete from GL_FWPSTD where ID_SRC = ?", src);
        if (null != startdate)
            baseEntityRepository.executeNativeUpdate("insert into GL_FWPSTD (ID_SRC, DTB, DTE) values (?, ?, ?)", src, startdate, enddate);
        remoteAccess.invoke(FwPostSourceCachedRepository.class, "flushCache");
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
        pst.setSourcePosting(KondorPlus.getLabel());
        pst.setDealId("123");

        pst = (EtlPosting) baseEntityRepository.save(pst);

        pst = (EtlPosting) baseEntityRepository.selectFirst(EtlPosting.class
                , "from EtlPosting p join fetch p.etlPackage k where p = ?1", pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        operation = (GLOperation) baseEntityRepository.selectFirst(GLOperation.class
                , "from GLOperation o where o.id = ?1", operation.getId());

        Assert.assertTrue(operation.getState().name(), EnumUtils.contains(new OperState[]{OperState.ERCHK, OperState.WTAC}, operation.getState()));

        // подаем ошибочную проводку на вход обработчика ошибок
        GLOperation operationTrans = (GLOperation) Optional.ofNullable(remoteAccess.invoke(EtlTechnicalPostingController.class, "processMessage", pst))
                .orElseThrow(() -> new RuntimeException("Operation is not created"));
        operationTrans = (GLOperation) baseEntityRepository.findById(GLOperation.class, operationTrans.getId());
        Assert.assertNotNull(operationTrans);
        Assert.assertEquals(OperState.POST, operationTrans.getState());

        final String idpst1 = pst.getAePostingId();
        EtlPosting transitPosting = (EtlPosting) baseEntityRepository.selectFirst(EtlPosting.class
                , "from EtlPosting t where t.aePostingId = ?1", idpst1 + "*");
        Assert.assertNotNull(transitPosting);
        Assert.assertTrue(transitPosting.getAccountDebit(), transitPosting.getAccountDebit().matches("47423\\d{15}"));
        Assert.assertEquals(clientAccount, transitPosting.getAccountCredit());


        GLAccount acc = findAccount(transitPosting.getAccountDebit());
                //baseEntityRepository.findById(GLAccount.class, new AccRlnId("", transitPosting.getAccountDebit()));
        Assert.assertNotNull(transitPosting.getAccountDebit(), acc);

        Assert.assertEquals(RelationType.E, RelationType.parse(acc.getRelationType()));
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
        pst.setSourcePosting(KondorPlus.getLabel());
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
        pst.setSourcePosting(KondorPlus.getLabel());
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

        baseEntityRepository.executeNativeUpdate("update gl_prprp set string_value = '1' where id_prp = 'client.transit.reprocess'");
        remoteAccess.invoke(PropertiesRepository.class, "flushCache");

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


}
;
