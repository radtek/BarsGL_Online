package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.*;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeysBuilder;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.barsgl.ejb.integr.bg.EtlPostingController;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejbcore.mapping.YesNo;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.RUB;
import static ru.rbt.barsgl.ejbtest.utl.Utl4Tests.deleteGlAccountWithLinks;
import static ru.rbt.barsgl.shared.enums.OperState.ERCHK;
import static ru.rbt.barsgl.shared.enums.OperState.POST;
import static ru.rbt.ejbcore.mapping.YesNo.Y;

/**
 * Created by Ivan Sevastyanov
 * Обработка операций СТОРНО в формате простой проводки
 * @fsd 7.2.3
 */
public class StornoIT extends AbstractTimerJobIT {

    private static final Logger logger = Logger.getLogger(StornoIT.class.getName());

    @BeforeClass
    public static void beforeClass() throws ParseException {
        Date operday = DateUtils.parseDate("2015-02-26", "yyyy-MM-dd");
        setOperday(operday, DateUtils.addDays(operday, -1), Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
    }

    @Before
    public void before() {
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
    }

    @After
    public void after() {
        restoreOperday();
    }

    /**
     * Обработки операции сторно при отсутствии сторнируемой операции (ошибка операции)
     * @fsd 7.2.3
     */
    @Test public void testStornoNoRef() {
        final long st = System.currentTimeMillis();
        EtlPackage etlPackage = newPackage(st, "Checking storno");
        EtlPosting pst = newPosting(st, etlPackage);
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());
        pst.setValueDate(getOperday().getCurrentDate());
        pst.setStorno(Y);
        pst.setStornoReference("storno_" + st);
        pst.setAccountCredit("40817036200012959997");
        pst.setAccountDebit("40817036250010000018");
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());

        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(Y, operation.getStorno());
        Assert.assertEquals(pst.getStornoReference(), operation.getStornoReference());
        Assert.assertEquals(ERCHK, operation.getState());
        Assert.assertNull(operation.getStornoOperation());        // ссылка на сторно операцию
        String errorMessage = operation.getErrorMessage();
        Assert.assertTrue(errorMessage.contains("1007"));
    }

    /**
     * Обработка операции сторно в текущий день (отмена операции, подавление проводок)
     * @fsd 7.7.2.1
     * @throws java.text.ParseException
     */
    @Test public void testStornoOneday() throws ParseException {

        long stamp = System.currentTimeMillis();

        // прямая операция
        EtlPackage pkg = newPackage(stamp, "MfoExchange");
        Assert.assertTrue(pkg.getId() > 0);
        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());
        pst.setAccountDebit("30302840700010000033");    // "MOS"
        pst.setCurrencyDebit(BankCurrency.USD);
        pst.setAmountDebit(new BigDecimal("100.00"));
        pst.setAccountCredit("47427810550160009330");     // "CHL"
        pst.setCurrencyCredit(BankCurrency.RUB);
        pst.setAmountCredit(new BigDecimal("3500.00"));
        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());       // операция создана

        // Сторно операция
        stamp = System.currentTimeMillis();
        pkg = newPackage(stamp, "MfoExchangeStorno");
        Assert.assertTrue(pkg.getId() > 0);
        EtlPosting pstS = newStornoPosting(stamp, pkg, pst);
        pstS.setValueDate(pst.getValueDate());
        pstS = (EtlPosting) baseEntityRepository.save(pstS);

        GLOperation operationS = (GLOperation) postingController.processMessage(pstS);
        Assert.assertTrue(0 < operationS.getId());       // операция создана

        operationS = (GLOperation) baseEntityRepository.findById(operationS.getClass(), operationS.getId());
        Assert.assertEquals(operationS.getState(), OperState.SOCANC);
        Assert.assertEquals(operationS.getPstScheme(), GLOperation.OperType.ST);
        Assert.assertEquals(operationS.getStornoRegistration(), GLOperation.StornoType.C);
        Assert.assertEquals(operationS.getStornoOperation().getId(), operation.getId());        // ссылка на сторно операцию
        List<GLPosting> postList = getPostings(operationS);
        Assert.assertTrue(postList.isEmpty());                    // нет своих проводки

        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(operation.getState(), OperState.CANC);
        postList = getPostings(operation);

        Assert.assertNotNull(postList);
        Assert.assertFalse(postList.isEmpty());         // 2 проводки

        for (GLPosting posting: postList) {
            List<Pd> pdList = getPostingPd( posting );
            for (Pd pd: pdList) {
//                pd = (Pd)baseEntityRepository.findById(Pd.class, pd.getId());
                Assert.assertEquals(pd.getInvisible(), "1");
            }
        }

    }

    /**
     * Обработка операции сторно в текущий день при отсутствии счета (отмена операции)
     * @fsd 7.7.2
     * @throws java.text.ParseException
     */
    @Test public void testStornoOnedayWtac() throws ParseException {

        long stamp = System.currentTimeMillis();

        // прямая операция
        EtlPackage pkg = newPackage(stamp, "MfoExchange");
        Assert.assertTrue(pkg.getId() > 0);
        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());
        pst.setAccountDebit("30302840700010000034");    // "MOS" такого счета нет
        pst.setCurrencyDebit(BankCurrency.USD);
        pst.setAmountDebit(new BigDecimal("100.00"));
        pst.setAccountCredit("47427810550160009330");     // "CHL"
        pst.setCurrencyCredit(BankCurrency.RUB);
        pst.setAmountCredit(new BigDecimal("3500.00"));
        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());       // операция создана

        // Сторно операция
        stamp = System.currentTimeMillis();
        pkg = newPackage(stamp, "MfoExchangeStorno");
        Assert.assertTrue(pkg.getId() > 0);
        EtlPosting pstS = newStornoPosting(stamp, pkg, pst);
        pstS.setValueDate(pst.getValueDate());
        pstS = (EtlPosting) baseEntityRepository.save(pstS);

        GLOperation operationS = (GLOperation) postingController.processMessage(pstS);
        Assert.assertTrue(0 < operationS.getId());       // операция создана

        operationS = (GLOperation) baseEntityRepository.findById(operationS.getClass(), operationS.getId());
        Assert.assertEquals(operationS.getState(), OperState.SOCANC);
        Assert.assertEquals(operationS.getPstScheme(), GLOperation.OperType.ST);
        Assert.assertEquals(operationS.getStornoRegistration(), GLOperation.StornoType.C);
        Assert.assertEquals(operationS.getStornoOperation().getId(), operation.getId());        // ссылка на сторно операцию
        List<GLPosting> postList = getPostings(operationS);
        Assert.assertTrue(postList.isEmpty());                    // нет своих проводки

        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(operation.getState(), OperState.CANC);
        postList = getPostings(operation);

        Assert.assertNotNull(postList);
        Assert.assertTrue(postList.isEmpty());          // нет проводок

    }

    /**
     * Обработка операции сторно backvalue в одном филиале, в одной валюте (1 проводка)
     * @fsd 7.7.3, 7.5.2.1
     * @throws ParseException
     */
    @Test public void testStornoSimple() throws ParseException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        Date operday = getOperday().getCurrentDate();
        pst.setValueDate(operday);

        pst.setAccountCredit("40817036200012959997");
        pst.setAccountDebit("40817036250010000018");
        pst.setAmountCredit(new BigDecimal("12.006"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());       // операция создана
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(operation.getState(), OperState.POST);
        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);                 // 1 проводка
        Assert.assertEquals(postList.size(), 1);

        // Сторно операция - сдвигем день вперед на 1
        setOperday(DateUtils.addDays(operday, 1), operday, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.CLOSED);
        stamp = System.currentTimeMillis();
        pkg = newPackage(stamp, "SimpleStornoBack");
        Assert.assertTrue(pkg.getId() > 0);
        EtlPosting pstS = newStornoPosting(stamp, pkg, pst);
        pstS.setValueDate(getOperday().getCurrentDate());
        pstS.setStorno(Y);
        pstS = (EtlPosting) baseEntityRepository.save(pstS);

        GLOperation operationS = (GLOperation) postingController.processMessage(pstS);
        Assert.assertTrue(0 < operationS.getId());       // операция создана

        operationS = (GLOperation) baseEntityRepository.findById(operationS.getClass(), operationS.getId());
        Assert.assertEquals(OperState.POST, operationS.getState());
        Assert.assertEquals(operationS.getPstScheme(), GLOperation.OperType.S);
        Assert.assertEquals(operationS.getStornoRegistration(), GLOperation.StornoType.S);
        Assert.assertEquals(operationS.getStornoOperation().getId(), operation.getId());        // ссылка на сторно операцию

        List<GLPosting> postListS = getPostings(operationS);
        Assert.assertNotNull(postListS);                 // 1 проводка
        Assert.assertEquals(postListS.size(), 1);

        checkStornoRef(postListS.get(0), postList);

        List<Pd> pdListS = getPostingPd(postListS.get(0));
        Pd pdDr = pdListS.get(0);
        Pd pdCr = pdListS.get(1);

        Assert.assertTrue(pdDr.getCcy().equals(operationS.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr.getCcy().equals(operationS.getCurrencyCredit())); // валюта кредит
        Assert.assertTrue(pdCr.getCcy().equals(pdDr.getCcy()));                 // валюта одинаковый

        Assert.assertTrue(pdCr.getAmount() == operationS.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCr.getAmount() == -pdDr.getAmount());       // сумма в валюте дебет - кредит

    }

    /**
     * Обработка операции сторно backvalue межфилиальной в одной валюте (2 проводки)
     * @fsd 7.7.3, 7.5.2.2
     * @throws ParseException
     */
    @Test public void testStornoMfo() throws ParseException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "MFO");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        Date operday = getOperday().getCurrentDate();
        pst.setValueDate(operday);

        pst.setAccountCredit("47427978000164566575");     // "CHL"
        pst.setAccountDebit("47427978400404502369");        // "EKB"

        pst.setAmountCredit(new BigDecimal("321.56"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.EUR);
        pst.setCurrencyDebit(pst.getCurrencyCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());       // операция создана
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(operation.getState(), OperState.POST);
        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);                 // 1 проводка
        Assert.assertEquals(postList.size(), 2);

        // Сторно операция - сдвигем день вперед на 1
        setOperday(DateUtils.addDays(operday, 1), operday, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.CLOSED);
        stamp = System.currentTimeMillis();
        pkg = newPackage(stamp, "MfoStornoBack");
        Assert.assertTrue(pkg.getId() > 0);
        EtlPosting pstS = newStornoPosting(stamp, pkg, pst);
        pstS.setValueDate(getOperday().getCurrentDate());
        pstS.setStorno(Y);
        pstS = (EtlPosting) baseEntityRepository.save(pstS);

        GLOperation operationS = (GLOperation) postingController.processMessage(pstS);
        Assert.assertTrue(0 < operationS.getId());       // операция создана

        operationS = (GLOperation) baseEntityRepository.findById(operationS.getClass(), operationS.getId());
        Assert.assertEquals(operationS.getState(), OperState.POST);
        Assert.assertEquals(operationS.getPstScheme(), GLOperation.OperType.M);
        Assert.assertEquals(operationS.getStornoRegistration(), GLOperation.StornoType.S);
        Assert.assertEquals(operationS.getStornoOperation().getId(), operation.getId());        // ссылка на сторно операцию

        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operationS.getId());
        Assert.assertEquals(operation.getState(), OperState.POST);

        List<GLPosting> postListS = getPostings(operationS);
        Assert.assertNotNull(postListS);                 // 2 проводки
        Assert.assertEquals(postListS.size(), 2);

        for (GLPosting postS: postListS) {                // в каждой проводке:
            checkStornoRef(postS, postList);

            List<Pd> pdListS = getPostingPd(postS);
            Pd pdDr = pdListS.get(0);
            Pd pdCr = pdListS.get(1);

            Assert.assertTrue(pdDr.getCcy().equals(operationS.getCurrencyDebit()));  // валюта дебет
            Assert.assertTrue(pdCr.getCcy().equals(operationS.getCurrencyCredit())); // валюта кредит
            Assert.assertTrue(pdCr.getCcy().equals(pdDr.getCcy()));                 // валюта одинаковый

            Assert.assertTrue(pdCr.getAmount() == operationS.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
            Assert.assertTrue(pdCr.getAmount() == -pdDr.getAmount());       // сумма в валюте дебет - кредит

            Assert.assertTrue(pdCr.getAmountBC() == operationS.getEquivalentDebit().movePointRight(2).longValue());  // в рублях
            Assert.assertTrue(-pdDr.getAmount() == pdCr.getAmount());       // сумма в валюте дебет - кредит
        }
    }

    /**
     * Обработка операции сторно backvalue в одном филиале с курсовой разницей (2 проводки)
     * @fsd 7.7.3, 7.5.2.1
     * @throws ParseException
     */
    @Test public void testStornoExch() throws ParseException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "Exchange");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        Date operday = getOperday().getCurrentDate();
        pst.setValueDate(operday);

        pst.setAccountDebit("30302840700010000033");    // "MOS"
        pst.setCurrencyDebit(BankCurrency.USD);
        pst.setAmountDebit(new BigDecimal("100.00"));

        pst.setAccountCredit("40702810100013995679");   // "MOS" клиент
        pst.setCurrencyCredit(BankCurrency.RUB);
        pst.setAmountCredit(new BigDecimal("3500.00"));

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());       // операция создана
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(operation.getState(), OperState.POST);
        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);                 // 1 проводка
        Assert.assertEquals(postList.size(), 2);

        // Сторно операция - сдвигем день вперед на 1
        setOperday(DateUtils.addDays(operday, 1), operday, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.CLOSED);
        stamp = System.currentTimeMillis();
        pkg = newPackage(stamp, "ExchStornoBack");
        Assert.assertTrue(pkg.getId() > 0);
        EtlPosting pstS = newStornoPosting(stamp, pkg, pst);
        pstS.setValueDate(getOperday().getCurrentDate());
        pstS.setStorno(Y);
        pstS = (EtlPosting) baseEntityRepository.save(pstS);

        GLOperation operationS = (GLOperation) postingController.processMessage(pstS);
        Assert.assertTrue(0 < operationS.getId());       // операция создана

        operationS = (GLOperation) baseEntityRepository.findById(operationS.getClass(), operationS.getId());
        Assert.assertEquals(operationS.getState(), OperState.POST);
        Assert.assertEquals(operationS.getPstScheme(), GLOperation.OperType.E);
        Assert.assertEquals(operationS.getStornoRegistration(), GLOperation.StornoType.S);
        Assert.assertEquals(operationS.getStornoOperation().getId(), operation.getId());        // ссылка на сторно операцию

        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operationS.getId());
        Assert.assertEquals(operation.getState(), OperState.POST);

        List<GLPosting> postListS = getPostings(operationS);
        Assert.assertNotNull(postListS);                 // 2 проводки
        Assert.assertEquals(postListS.size(), 2);

        GLPosting postS = postListS.get(0);
        checkStornoRef(postS, postList);
        List<Pd> pdListS = getPostingPd(postS);
        Pd pdDr = pdListS.get(0);
        Pd pdCr = pdListS.get(1);

        GLPosting postSE = postListS.get(1);
        checkStornoRef(postSE, postList);
        List<Pd> pdListSE = getPostingPd(postSE);
        Pd pdDrE = pdListSE.get(0);
        Pd pdCrE = pdListSE.get(1);

        // основная проводка
        Assert.assertTrue(pdDr.getCcy().equals(operationS.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr.getCcy().equals(operationS.getCurrencyCredit())); // валюта кредит

        Assert.assertTrue(-pdDr.getAmount() == operationS.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCr.getAmount() == operationS.getAmountCredit().movePointRight(2).longValue());  // сумма в валюте

        // курсовая разница
        Assert.assertTrue(pdDrE.getCcy().equals(BankCurrency.RUB));  // валюта дебет
        Assert.assertTrue(pdCrE.getCcy().equals(BankCurrency.RUB)); // валюта кредит

        Assert.assertTrue(pdCrE.getAmountBC() == operationS.getExchangeDifference().movePointRight(2).abs().longValue());  // в рублях

        // сумма эквивалентов по дебету и
        Assert.assertTrue(
                (pdDr.getAmountBC() + pdDrE.getAmountBC() == -operationS.getEquivalentDebit().movePointRight(2).longValue())
                        ||  (pdCr.getAmountBC() + pdCrE.getAmountBC() == operationS.getEquivalentCredit().movePointRight(2).longValue())
        );

    }

    /**
     * Обработка операции сторно backvalue межфилиальной с курсовой разницей (3 проводки)
     * @fsd 7.7.3, 7.5.2.2
     * @throws ParseException
     */
    @Test public void testStornoMfoExch() throws ParseException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "MfoExchange");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        Date operday = getOperday().getCurrentDate();
        pst.setValueDate(operday);

        pst.setAccountDebit("30302840700010000033");    // "MOS"
        pst.setCurrencyDebit(BankCurrency.USD);
        pst.setAmountDebit(new BigDecimal("100.00"));

        pst.setAccountCredit("47427810550160009330");     // "CHL"
        pst.setCurrencyCredit(BankCurrency.RUB);
        pst.setAmountCredit(new BigDecimal("3500.00"));

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());       // операция создана
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(operation.getState(), OperState.POST);
        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);                 // 1 проводка
        Assert.assertEquals(postList.size(), 3);

        // Сторно операция - сдвигем день вперед на 1
        setOperday(DateUtils.addDays(operday, 1), operday, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.CLOSED);
        stamp = System.currentTimeMillis();
        pkg = newPackage(stamp, "ExchStornoBack");
        Assert.assertTrue(pkg.getId() > 0);
        EtlPosting pstS = newStornoPosting(stamp, pkg, pst);
        pstS.setValueDate(operday);
        pstS.setStorno(Y);
        pstS = (EtlPosting) baseEntityRepository.save(pstS);

        GLOperation operationS = (GLOperation) postingController.processMessage(pstS);
        Assert.assertTrue(0 < operationS.getId());       // операция создана

        operationS = (GLOperation) baseEntityRepository.findById(operationS.getClass(), operationS.getId());
        Assert.assertEquals(operationS.getState(), OperState.POST);
        Assert.assertEquals(operationS.getPstScheme(), GLOperation.OperType.ME);
        Assert.assertEquals(operationS.getStornoRegistration(), GLOperation.StornoType.S);
        Assert.assertEquals(operationS.getStornoOperation().getId(), operation.getId());        // ссылка на сторно операцию

        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operationS.getId());
        Assert.assertEquals(operation.getState(), OperState.POST);

        List<GLPosting> postListS = getPostings(operationS);        Assert.assertNotNull(postListS);                 // 2 проводки
        Assert.assertEquals(postListS.size(), 3);

        // дебет
        GLPosting postSD = findGLPosting("3", postListS);
        checkStornoRef(postSD, postList);
        List<Pd> pdListSD = getPostingPd(postSD );
        Pd pdDrD = pdListSD.get(0);
        Pd pdCrD = pdListSD.get(1);

        // кредит
        GLPosting postSC = findGLPosting("4", postListS);
        checkStornoRef(postSC, postList);
        List<Pd> pdListSC = getPostingPd( postSC );
        Pd pdDrC = pdListSC.get(0);
        Pd pdCrC = pdListSC.get(1);

        // курсовая разница
        GLPosting postSE = findGLPosting("2", postListS);
        checkStornoRef(postSE, postList);
        List<Pd> pdListSE = getPostingPd( postSE );
        Pd pdDrE = pdListSE.get(0);
        Pd pdCrE = pdListSE.get(1);

        // основные проводки
        Assert.assertTrue(pdDrD.getCcy().equals(operationS.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCrC.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит

        Assert.assertTrue(-pdDrD.getAmount() == operation.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCrC.getAmount() == operation.getAmountCredit().movePointRight(2).longValue());  // сумма в валюте

        // курсовая разница
        Assert.assertTrue(pdDrE.getCcy().equals(BankCurrency.RUB));  // валюта дебет
        Assert.assertTrue(pdCrE.getCcy().equals(BankCurrency.RUB)); // валюта кредит

        Assert.assertTrue(pdCrE.getAmountBC() == operation.getExchangeDifference().movePointRight(2).abs().longValue());  // в рублях

    }

    /**
     * Повторная обработка сторно с статусом ERCHK
     */
    @Test public void testReprocessStorno() {
        Date currentDate = new Date(getOperday().getCurrentDate().getTime());
        Date toDate = Date.from(currentDate.toInstant().minus(-10, ChronoUnit.DAYS));
        baseEntityRepository.executeUpdate("update GLOperation o set o.valueDate = ?1 where o.storno = ?2 and o.state = ?3 and o.valueDate = ?4"
                , toDate, YesNo.Y, OperState.ERCHK, getOperday().getCurrentDate());

        long stamp = System.currentTimeMillis();
        EtlPackage pkg = newPackage(stamp, "StornoErchkReprocess");

        final String ref = "sref_" + System.currentTimeMillis();

        EtlPosting pstStrn = newPosting(stamp, pkg);
        pstStrn.setValueDate(getOperday().getCurrentDate());

        pstStrn.setCurrencyCredit(RUB);
        pstStrn.setCurrencyDebit(pstStrn.getCurrencyCredit());

        pstStrn.setAccountCredit("");
        pstStrn.setAccountDebit("");
        pstStrn.setStorno(Y);
        pstStrn.setStornoReference(ref);
        pstStrn.setDealId((""+System.currentTimeMillis()).substring(7));

        final AccountKeys acCt
                = AccountKeysBuilder.create()
                .withBranch("001").withCurrency(pstStrn.getCurrencyCredit().getCurrencyCode()).withCustomerNumber("00000018")
                .withAccountType("643010101").withCustomerType("23").withTerm("05").withPlCode("17101")
                .withGlSequence("123").withAcc2("70613").withAccountCode("7301").withAccSequence("01")
                .build();
        final AccountKeys acDt
                = AccountKeysBuilder.create()
                .withBranch("001").withCurrency(pstStrn.getCurrencyDebit().getCurrencyCode()).withCustomerNumber("00000018")
                .withAccountType("643010101").withCustomerType(acCt.getCustomerType()).withTerm("05").withPlCode("17101")
                .withGlSequence("123").withAcc2("70613").withAccountCode("7301").withAccSequence("01")
                .build();
        String accountKeyCt = acCt.toString();
        String accountKeyDt = acDt.toString();
        deleteGlAccountWithLinks(baseEntityRepository, accountKeyCt);
        deleteGlAccountWithLinks(baseEntityRepository, accountKeyDt);

        pstStrn.setAccountKeyCredit(accountKeyCt);
        pstStrn.setAccountKeyDebit(accountKeyDt);

        pstStrn.setAmountCredit(new BigDecimal("13.99"));
        pstStrn.setAmountDebit(pstStrn.getAmountCredit());

        pstStrn = (EtlPosting) baseEntityRepository.save(pstStrn);

        logger.info("storno posting id = " + pstStrn.getId());

        GLOperation operStrn = (GLOperation) postingController.processMessage(pstStrn);
        Assert.assertNotNull(operStrn.getId());
        Assert.assertTrue(0 < operStrn.getId());

        operStrn = (GLOperation) baseEntityRepository.findById(operStrn.getClass(), operStrn.getId());
        Assert.assertEquals(ERCHK, operStrn.getState());

        // прямая проводка
        EtlPosting pst = newPosting(stamp, pkg);
        pst.setStornoReference(pstStrn.getEventId());
        pst.setDealId(pstStrn.getDealId());
        pst.setStorno(YesNo.N);
        pst.setAccountDebit(pstStrn.getAccountCredit());
        pst.setCurrencyDebit(pstStrn.getCurrencyCredit());
        pst.setAmountDebit(pstStrn.getAmountCredit());
        pst.setAccountCredit(pstStrn.getAccountDebit());
        pst.setCurrencyCredit(pstStrn.getCurrencyDebit());
        pst.setAmountCredit(pstStrn.getAmountDebit());
        pst.setValueDate(pstStrn.getValueDate());
        pst.setAccountKeyCredit(accountKeyCt);
        pst.setAccountKeyDebit(accountKeyDt);
        pst.setEventId(ref);

        pst = (EtlPosting) baseEntityRepository.save(pst);
        logger.info("posting id = " + pst.getId());
        GLOperation oper = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(oper);

        logger.info("oper id=" + oper.getId());

        oper = (GLOperation) baseEntityRepository.findById(GLOperation.class, oper.getId());
        Assert.assertEquals(POST, oper.getState());

        remoteAccess.invoke(EtlPostingController.class, "reprocessErckStorno"
                , getOperday().getLastWorkingDay(), getOperday().getCurrentDate());

        operStrn = (GLOperation) baseEntityRepository.findById(GLOperation.class, operStrn.getId());
        Assert.assertEquals(OperState.SOCANC, operStrn.getState());

    }

    /**
     * Обработка операции сторно backvalue на операцию в статусе WTAC
     * @fsd 7.7.3, 7.5.2.1
     * @throws ParseException
     */
    @Test public void testStornoWtacOperation() throws ParseException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        Date operday = getOperday().getCurrentDate();
        pst.setValueDate(operday);

        pst.setAccountCredit("40817036200012959997");
        pst.setAccountDebit("40817036250010000018");
        pst.setAmountCredit(new BigDecimal("12.006"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());       // операция создана
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(operation.getState(), OperState.POST);
        // переведем в WTAC, чтоб проверить ошибку статуса операции
        baseEntityRepository.executeNativeUpdate("update GL_OPER set STATE = 'WTAC' where GLOID = ?", operation.getId());

        // Сторно операция - сдвигем день вперед на 1
        setOperday(DateUtils.addDays(operday, 1), operday, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.CLOSED);
        stamp = System.currentTimeMillis();
        pkg = newPackage(stamp, "SimpleStornoBack");
        Assert.assertTrue(pkg.getId() > 0);
        EtlPosting pstS = newStornoPosting(stamp, pkg, pst);
        pstS.setValueDate(operday);
        pstS.setStorno(Y);
        pstS = (EtlPosting) baseEntityRepository.save(pstS);

        GLOperation operationS = (GLOperation) postingController.processMessage(pstS);
        Assert.assertTrue(0 < operationS.getId());       // операция создана

        operationS = (GLOperation) baseEntityRepository.findById(operationS.getClass(), operationS.getId());
        Assert.assertEquals(operationS.getState(), OperState.ERPOST);

    }

    /**
     * Обработка операции сторно backvalue на операцию, обработанную повторно (первый раз с ошибкой)
     * @fsd 7.7.3, 7.5.2.1
     * @throws ParseException
     */
    @Test public void testStornoTwoOperation() throws ParseException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        Date operday = getOperday().getCurrentDate();
        pst.setValueDate(operday);

        pst.setAccountCredit("40817036200012959997");
        pst.setAccountDebit("40817036250010000018");
        pst.setAmountCredit(new BigDecimal("12.006"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operationBad = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operationBad.getId());       // операция создана
        operationBad = (GLOperation) baseEntityRepository.findById(operationBad.getClass(), operationBad.getId());
        Assert.assertEquals(operationBad.getState(), OperState.POST);
        // переведем в ERPOST, чтоб проверить ошибку статуса операции
        baseEntityRepository.executeNativeUpdate("update GL_OPER set STATE = 'ERPOST' where GLOID = ?", operationBad.getId());

        // запускаем повторную обработку
        pst.setErrorCode(null);
        pst.setErrorMessage(null);
        pst = (EtlPosting)baseEntityRepository.update(pst);
        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());       // операция создана
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(operation.getState(), OperState.POST);
        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);                 // 1 проводка
        Assert.assertEquals(postList.size(), 1);

        // Сторно операция - сдвигем день вперед на 1
        setOperday(DateUtils.addDays(operday, 1), operday, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.CLOSED);
        stamp = System.currentTimeMillis();
        pkg = newPackage(stamp, "SimpleStornoBack");
        Assert.assertTrue(pkg.getId() > 0);
        EtlPosting pstS = newStornoPosting(stamp, pkg, pst);
        pstS.setValueDate(operday);
        pstS.setStorno(Y);
        pstS = (EtlPosting) baseEntityRepository.save(pstS);

        GLOperation operationS = (GLOperation) postingController.processMessage(pstS);
        Assert.assertTrue(0 < operationS.getId());       // операция создана

        operationS = (GLOperation) baseEntityRepository.findById(operationS.getClass(), operationS.getId());
        Assert.assertEquals(OperState.POST, operationS.getState());
        Assert.assertEquals(operationS.getPstScheme(), GLOperation.OperType.S);
        Assert.assertEquals(operationS.getStornoRegistration(), GLOperation.StornoType.S);
        Assert.assertEquals(operationS.getStornoOperation().getId(), operation.getId());        // ссылка на сторно операцию

        List<GLPosting> postListS = getPostings(operationS);
        Assert.assertNotNull(postListS);                 // 1 проводка
        Assert.assertEquals(postListS.size(), 1);

        checkStornoRef(postListS.get(0), postList);

        List<Pd> pdListS = getPostingPd(postListS.get(0));
        Pd pdDr = pdListS.get(0);
        Pd pdCr = pdListS.get(1);

        Assert.assertTrue(pdDr.getCcy().equals(operationS.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr.getCcy().equals(operationS.getCurrencyCredit())); // валюта кредит
        Assert.assertTrue(pdCr.getCcy().equals(pdDr.getCcy()));                 // валюта одинаковый

        Assert.assertTrue(pdCr.getAmount() == operationS.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCr.getAmount() == -pdDr.getAmount());       // сумма в валюте дебет - кредит

    }
}


