package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.*;
import ru.rbt.barsgl.ejb.controller.operday.task.PdSyncTaskOld;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeysBuilder;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.*;
import ru.rbt.barsgl.ejb.integr.bg.EtlPostingController;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.barsgl.shared.enums.OperState;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.CLOSED;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.BUFFER;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.DIRECT;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.RUB;
import static ru.rbt.barsgl.ejbcore.mapping.YesNo.Y;
import static ru.rbt.barsgl.ejbtest.utl.Utl4Tests.deleteGlAccountWithLinks;
import static ru.rbt.barsgl.shared.enums.OperState.ERCHK;
import static ru.rbt.barsgl.shared.enums.OperState.POST;

/**
 * Created by Ivan Sevastyanov on 11.02.2016.
 * обработка сторно в режиме BUFFER
 */
public class StornoBufferTest extends AbstractTimerJobTest {

    private static final Logger logger = Logger.getLogger(StornoTest.class.getName());

    @BeforeClass
    public static void beforeClass() throws ParseException {
        Date operday = DateUtils.parseDate("2015-02-26", "yyyy-MM-dd");
        setOperday(operday, DateUtils.addDays(operday, -1), ONLINE, OPEN, BUFFER);
    }

    @Before
    public void before() {
        updateOperday(ONLINE, OPEN, BUFFER);
    }

    @After
    public void after() {
        restoreOperday();
    }

    /**
     * Обработки операции сторно при отсутствии сторнируемой операции (ошибка операции)
     * @fsd 7.2.3
     */
    @Test
    public void testStornoNoRef() {
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
        Assert.assertTrue(postList.isEmpty());         // 0 реальных проводок, только буфер

        List<GLPd> glPds = baseEntityRepository.select(GLPd.class, "from GLPd p where p.glOperationId = ?1"
                , new Object[]{operation.getId()});

        Assert.assertTrue(glPds.stream().allMatch(p-> p.getInvisible().equals("1")));
    }

    /**
     * Обработка операции сторно в текущий день (отмена операции, подавление проводок)
     * в случае если в течение дня проведена СИНХРОНИЗАЦИЯ ПРОВОДОК
     * сторнируемую п/проводки ищем еще и в PD
     *
     * @fsd 7.7.2.1
     * @throws java.text.ParseException
     */
    @Test public void testStornoOnedayBufferDirect() throws Exception {

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

        baseEntityRepository.executeNativeUpdate("update gl_od set prc = 'STOPPED'");

        // сбрасываем буфер
        jobService.executeJob(SingleActionJobBuilder.create()
                .withClass(PdSyncTaskOld.class).withName("SyncAct1").build());

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
        Assert.assertTrue(!postList.isEmpty());         // 0 реальных проводок, только буфер
        List<Pd> pds = new ArrayList<>();
        for (GLPosting posting : postList) {
            pds.addAll(getPostingPd(posting));
        }
        Assert.assertTrue(pds.stream().allMatch(pd -> pd.getInvisible().equals("1")));
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
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());       // операция создана
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(operation.getState(), OperState.POST);
        List<GLPosting> postList1 = getPostings(operation);
        Assert.assertTrue(postList1.isEmpty());

        List<GLPd> glPdsPre = getGLPostings(operation);
        Map<Long,List<GLPd>> glPdsPreMap = glPdsPre.stream().collect(Collectors.groupingBy(GLPd::getPcId));
        Assert.assertEquals(glPdsPreMap.keySet().size(), 1);

        // Сторно операция - сдвигем день вперед на 1
        setOperday(DateUtils.addDays(operday, 1), operday, ONLINE, CLOSED, BUFFER);
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
        Assert.assertEquals(operationS.getState(), OperState.POST);
        Assert.assertEquals(operationS.getPstScheme(), GLOperation.OperType.S);
        Assert.assertEquals(operationS.getStornoRegistration(), GLOperation.StornoType.S);
        Assert.assertEquals(operationS.getStornoOperation().getId(), operation.getId());        // ссылка на сторно операцию

        List<GLPosting> postListS1 = getPostings(operationS);
        Assert.assertTrue(postListS1.isEmpty());
        List<GLPd> glPdsStorno = getGLPostings(operationS);
        Map<Long,List<GLPd>> glPdsStornoMap = glPdsStorno.stream().collect(Collectors.groupingBy(GLPd::getPcId));
        Assert.assertEquals(glPdsStornoMap.keySet().size(), 1);

        checkBufferReferences(glPdsPre, glPdsStorno);

        GLPd pdDr = getPostingSide(glPdsStorno, true);
        GLPd pdCr = getPostingSide(glPdsStorno, false);

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
        List<GLPosting> postListDirect = getPostings(operation);
        Assert.assertTrue(postListDirect.isEmpty());
        List<GLPd> glPdsPre = getGLPostings(operation);
        Map<Long,List<GLPd>> glPdsPreMap = glPdsPre.stream().collect(Collectors.groupingBy(GLPd::getPcId));
        Assert.assertEquals(2, glPdsPreMap.keySet().size());

        // Сторно операция - сдвигем день вперед на 1
        setOperday(DateUtils.addDays(operday, 1), operday, ONLINE, CLOSED, BUFFER);
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
        Assert.assertTrue(postListS.isEmpty());
        List<GLPd> glPdsStorno = getGLPostings(operationS);
        Map<Long, List<GLPd>> glPdsStornoMap = glPdsStorno.stream().collect(Collectors.groupingBy(GLPd::getPcId));
        Assert.assertEquals(glPdsStornoMap.keySet().size(), 2);

        checkBufferReferences(glPdsPre, glPdsStorno);

        for (Map.Entry<Long, List<GLPd>> e : glPdsStornoMap.entrySet()) {
            GLPd pdDr = e.getValue().stream().filter(a -> a.getAmount() < 0).findFirst().orElseThrow(() -> new RuntimeException("Debit not found"));
            GLPd pdCr = e.getValue().stream().filter(a -> a.getAmount() > 0).findFirst().orElseThrow(() -> new RuntimeException("Credit not found"));

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
        List<GLPosting> postList1 = getPostings(operation);
        Assert.assertTrue(postList1.isEmpty());

        List<GLPd> glPdsPre = getGLPostings(operation);
        Map<Long,List<GLPd>> glPdsPreMap = glPdsPre.stream().collect(Collectors.groupingBy(GLPd::getPcId));
        Assert.assertEquals(2, glPdsPreMap.keySet().size());

        // Сторно операция - сдвигем день вперед на 1
        setOperday(DateUtils.addDays(operday, 1), operday, ONLINE, CLOSED, BUFFER);
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

        List<GLPosting> postListS1 = getPostings(operationS);
        Assert.assertTrue(postListS1.isEmpty());
        List<GLPd> glPdsStorno = getGLPostings(operationS);
        Map<Long,List<GLPd>> glPdsStornoMap = glPdsStorno.stream().collect(Collectors.groupingBy(GLPd::getPcId));
        Assert.assertEquals(glPdsStornoMap.keySet().size(), 2);

        Long pcIdS = glPdsStorno.stream().filter(
                        pdst -> !StringUtils.isEmpty(pdst.getPostType())
                        && pdst.getPostType().equals(GLPosting.PostingType.OneFilial.getValue())
                    )
                .findFirst().orElseThrow(() -> new RuntimeException("simple storno posting not found")).getPcId();
        List<GLPd> pdListS = glPdsStornoMap.get(pcIdS);
        GLPd pdDr = pdListS.stream().filter(a -> a.getAmount() < 0).findFirst().orElseThrow(() -> new RuntimeException("Debit not found"));
        GLPd pdCr = pdListS.stream().filter(a -> a.getAmount() > 0).findFirst().orElseThrow(() -> new RuntimeException("Credit not found"));
        // основная проводка
        Assert.assertTrue(pdDr.getCcy().equals(operationS.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr.getCcy().equals(operationS.getCurrencyCredit())); // валюта кредит

        Assert.assertTrue(-pdDr.getAmount() == operationS.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCr.getAmount() == operationS.getAmountCredit().movePointRight(2).longValue());  // сумма в валюте

        // курсовая разница
        Long pcIdSE = glPdsStorno.stream().filter(
                        pdst -> !StringUtils.isEmpty(pdst.getPostType())
                        && pdst.getPostType().equals(GLPosting.PostingType.ExchDiff.getValue())
                    )
                .findFirst().orElseThrow(() -> new RuntimeException("Exch storno posting not found")).getPcId();
        List<GLPd> pdListSE = glPdsStornoMap.get(pcIdSE);
        GLPd pdDrE = pdListSE.stream().filter(a -> a.getAmountBC() < 0).findFirst().orElseThrow(() -> new RuntimeException("Debit not found"));
        GLPd pdCrE = pdListSE.stream().filter(a -> a.getAmountBC() > 0).findFirst().orElseThrow(() -> new RuntimeException("Credit not found"));
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
        List<GLPosting> postList1 = getPostings(operation);
        Assert.assertTrue(postList1.isEmpty());
        List<GLPd> glPdsPre = getGLPostings(operation);
        Map<Long,List<GLPd>> glPdsPreMap = glPdsPre.stream().collect(Collectors.groupingBy(GLPd::getPcId));
        Assert.assertEquals(3, glPdsPreMap.keySet().size());

        // Сторно операция - сдвигем день вперед на 1
        setOperday(DateUtils.addDays(operday, 1), operday, ONLINE, CLOSED, BUFFER);
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
        Assert.assertEquals(operationS.getPstScheme(), GLOperation.OperType.ME);
        Assert.assertEquals(operationS.getStornoRegistration(), GLOperation.StornoType.S);
        Assert.assertEquals(operationS.getStornoOperation().getId(), operation.getId());        // ссылка на сторно операцию

        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operationS.getId());
        Assert.assertEquals(operation.getState(), OperState.POST);

        List<GLPosting> postListS1 = getPostings(operationS);
        Assert.assertTrue(postListS1.isEmpty());

        List<GLPd> glPdsStorno = getGLPostings(operationS);
        Map<Long, List<GLPd>> glPdsStornoMap = glPdsStorno.stream().collect(Collectors.groupingBy(GLPd::getPcId));
        Assert.assertEquals(glPdsStornoMap.keySet().size(), 3);

        // дебет
        checkBufferReferences(glPdsPre, glPdsStorno);
        List<GLPd> postSDs = getGlPdsStornoByPostType("3", glPdsStorno, glPdsStornoMap);
        GLPd pdDrD = getPostingSide(postSDs, true);
        GLPd pdCrD = getPostingSide(postSDs, false);

        // кредит
        List<GLPd> pdListSCs = getGlPdsStornoByPostType("4", glPdsStorno, glPdsStornoMap);
        GLPd pdDrC = getPostingSide(pdListSCs, true);
        GLPd pdCrC = getPostingSide(pdListSCs, false);

        // курсовая разница
        List<GLPd> pdListSEs = getGlPdsStornoByPostType("2", glPdsStorno, glPdsStornoMap);
        GLPd pdDrE = getPostingSide(pdListSEs, true);
        GLPd pdCrE = getPostingSide(pdListSEs, false);

        // основные проводки
        Assert.assertTrue(pdDrD.getCcy().equals(operationS.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCrC.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит

        Assert.assertTrue(-pdDrD.getAmount() == operation.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCrC.getAmount() == operation.getAmountCredit().movePointRight(2).longValue());  // сумма в валюте

        // курсовая разница
        Assert.assertTrue(pdDrE.getCcy().equals(BankCurrency.RUB));  // валюта дебет
        Assert.assertTrue(pdCrE.getCcy().equals(BankCurrency.RUB)); // валюта кредит

        Assert.assertTrue(pdCrE.getAmountBC() +":"+ operation.getExchangeDifference().movePointRight(2).abs().longValue()
                , pdCrE.getAmountBC() == operation.getExchangeDifference().movePointRight(2).abs().longValue());  // в рублях

    }

    private void checkBufferReferences(List<GLPd> glPdsPre, List<GLPd> glPdsStorno) {
        glPdsStorno.stream().filter(pdst -> null != pdst.getStornoPcid()).forEach(pdst -> {
            GLPd prePd = glPdsPre.stream().filter(t -> GLPosting.getStornoTypeStatic(pdst.getPostType()).equals(t.getPostType())).findFirst()
                    .orElseThrow(()->new RuntimeException("Not found: " + GLPosting.getStornoTypeStatic(pdst.getPostType())));
            Long pcidRef = prePd.getId();
            Assert.assertEquals(pdst.getStornoPcid(), pcidRef);
        });

    }

    private <T extends AbstractPd> T getPostingSide(List<T> glPds, boolean isDebit) {
        List<AbstractPd> pds = glPds.stream().filter(p->isDebit ? p.getAmountBC() <0 : p.getAmountBC() > 0).collect(Collectors.toList());
        if (pds.size() == 1) return (T) pds.get(0);
        else if (pds.size() > 1) throw new RuntimeException(String.format("Too many '%s'", isDebit ? "debits" : "credits"));
        else throw new RuntimeException("pd is not found");
    }

    private List<GLPd> getGlPdsStornoByPostType(String type, List<GLPd> fullStornoPds, Map<Long,List<GLPd>> fullStornoPdsMap) {
        Long pcid = fullStornoPds.stream().filter(p->type.equals(p.getPostType())).findFirst()
                .orElseThrow(() -> new RuntimeException("Not found: " +type)).getPcId();
        return fullStornoPdsMap.get(pcid);
    }

    /**
     * Повторная обработка сторно с статусом ERCHK
     */
    @Test public void testReprocessStorno() {

        Date toDate = Date.from(getOperday().getCurrentDate().toInstant().minus(-10, ChronoUnit.DAYS));
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
        pst.setAmountCredit(new BigDecimal("12.0056"));
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
        setOperday(DateUtils.addDays(operday, 1), operday, ONLINE, CLOSED, BUFFER);
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
        pst.setAmountCredit(new BigDecimal("12.0056"));
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
        List<GLPosting> postList1 = getPostings(operation);
        Assert.assertTrue(postList1.isEmpty());
        List<GLPd> glPdsPre = getGLPostings(operation);
        Map<Long,List<GLPd>> glPdsPreMap = glPdsPre.stream().collect(Collectors.groupingBy(GLPd::getPcId));
        Assert.assertEquals(glPdsPreMap.keySet().size(), 1);

        // Сторно операция - сдвигем день вперед на 1
        setOperday(DateUtils.addDays(operday, 1), operday, ONLINE, CLOSED, BUFFER);
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
        Assert.assertEquals(operationS.getState(), OperState.POST);
        Assert.assertEquals(operationS.getPstScheme(), GLOperation.OperType.S);
        Assert.assertEquals(operationS.getStornoRegistration(), GLOperation.StornoType.S);
        Assert.assertEquals(operationS.getStornoOperation().getId(), operation.getId());        // ссылка на сторно операцию

        List<GLPosting> postListS1 = getPostings(operationS);
        Assert.assertTrue(postListS1.isEmpty());
        List<GLPd> glPdsStorno = getGLPostings(operationS);
        Map<Long,List<GLPd>> glPdsStornoMap = glPdsStorno.stream().collect(Collectors.groupingBy(GLPd::getPcId));
        Assert.assertEquals(glPdsStornoMap.keySet().size(), 1);

        checkBufferReferences(glPdsPre, glPdsStorno);

//        List<Pd> pdListS = getPostingPd(postListS.get(0));
        GLPd pdDr = getPostingSide(glPdsStorno, true);
        GLPd pdCr = getPostingSide(glPdsStorno, false);

        Assert.assertTrue(pdDr.getCcy().equals(operationS.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr.getCcy().equals(operationS.getCurrencyCredit())); // валюта кредит
        Assert.assertTrue(pdCr.getCcy().equals(pdDr.getCcy()));                 // валюта одинаковый

        Assert.assertTrue(pdCr.getAmount() == operationS.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCr.getAmount() == -pdDr.getAmount());       // сумма в валюте дебет - кредит

    }

    /**
     * Обработка операции сторно backvalue межфилиальной с курсовой разницей (3 проводки)
     * при обработки сторно backvalue возможно сторнируемые проводки будут в прошлом дне и соотв не в буфере а в PD
     * тогда ссылки нужно ставить на реальную PD
     * @fsd 7.7.3, 7.5.2.2
     * @throws ParseException
     */
    @Test public void testStornoMfoExchSmart() throws ParseException {

        // "вгоняем" операцию в режиме DIRECT
        Date operday = getOperday().getCurrentDate();
        setOperday(operday, getOperday().getLastWorkingDay(), ONLINE, CLOSED, DIRECT);
        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "MfoExchange");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
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
        List<GLPosting> postList1 = getPostings(operation);
        Assert.assertTrue(!postList1.isEmpty());
        List<Pd> glPdsPre = getAllPds(operation);
        Map<Long,List<Pd>> glPdsPreMap = glPdsPre.stream().collect(Collectors.groupingBy(Pd::getPcId));
        Assert.assertEquals(3, glPdsPreMap.keySet().size());

        // Сторно операция - сдвигем день вперед на 1
        setOperday(DateUtils.addDays(operday, 1), operday, ONLINE, CLOSED, BUFFER);
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

        List<GLPosting> postListS1 = getPostings(operationS);
        Assert.assertTrue(postListS1.isEmpty());

        List<GLPd> glPdsStorno = getGLPostings(operationS);
        Map<Long, List<GLPd>> glPdsStornoMap = glPdsStorno.stream().collect(Collectors.groupingBy(GLPd::getPcId));
        Assert.assertEquals(glPdsStornoMap.keySet().size(), 3);

        for (Long pcId : glPdsStornoMap.keySet()) {
            GLPd debitS = getPostingSide(glPdsStornoMap.get(pcId), true);
            GLPosting post = findGLPosting(GLPosting.getStornoTypeStatic(debitS.getPostType()), postList1);
            Long pcidRef = post.getId();
            Assert.assertEquals(debitS.getStornoPcid(), pcidRef);
        }

        List<GLPd> postSDs = getGlPdsStornoByPostType("3", glPdsStorno, glPdsStornoMap);
        GLPd pdDrD = getPostingSide(postSDs, true);
        GLPd pdCrD = getPostingSide(postSDs, false);

        // кредит
        List<GLPd> pdListSCs = getGlPdsStornoByPostType("4", glPdsStorno, glPdsStornoMap);
        GLPd pdDrC = getPostingSide(pdListSCs, true);
        GLPd pdCrC = getPostingSide(pdListSCs, false);

        // курсовая разница
        List<GLPd> pdListSEs = getGlPdsStornoByPostType("2", glPdsStorno, glPdsStornoMap);
        GLPd pdDrE = getPostingSide(pdListSEs, true);
        GLPd pdCrE = getPostingSide(pdListSEs, false);

        // основные проводки
        Assert.assertTrue(pdDrD.getCcy().equals(operationS.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCrC.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит

        Assert.assertTrue(-pdDrD.getAmount() == operation.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCrC.getAmount() == operation.getAmountCredit().movePointRight(2).longValue());  // сумма в валюте

        // курсовая разница
        Assert.assertTrue(pdDrE.getCcy().equals(BankCurrency.RUB));  // валюта дебет
        Assert.assertTrue(pdCrE.getCcy().equals(BankCurrency.RUB)); // валюта кредит

        Assert.assertTrue(pdCrE.getAmountBC() +":"+ operation.getExchangeDifference().movePointRight(2).abs().longValue()
                , pdCrE.getAmountBC() == operation.getExchangeDifference().movePointRight(2).abs().longValue());  // в рублях

    }

    private List<Pd> getAllPds(GLOperation operation) {
        return baseEntityRepository.select(Pd.class, "select p1 from GLPosting p, Pd p1 where p.operation = ?1 and p1.pcId = p.id", operation);
    }

}
