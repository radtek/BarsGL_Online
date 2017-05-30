package ru.rbt.barsgl.ejbtest;

import com.google.common.collect.Iterables;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.PreCobStepController;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.YesNo;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.google.common.collect.Iterables.find;

/**
 * Created by Ivan Sevastyanov
 *  Обработка операций в формате веерной проводки
 * @fsd 7.6
 */
public class FanIT extends AbstractTimerJobIT {

    public static final Logger log = Logger.getLogger(FanIT.class.getName());

    @BeforeClass
    public static void beforeClass() {
        initCorrectOperday();
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
    }

    /**
     * Регистрация частичной веерной операции (статус операции LOAD)
     * @fsd 7.6.1
     */
    @Test public void test() {
        final long st = System.currentTimeMillis();
        EtlPackage etlPackage = newPackage(st, "Checking fan base logic");
        EtlPosting pst = newPosting(st, etlPackage);
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());
        pst.setValueDate(getOperday().getCurrentDate());
        pst.setFan(YesNo.Y);
        pst.setParentReference("fanRef_" + st);
        pst.setAccountCredit("40817036200012959997");
        pst.setAccountDebit("40817036250010000018");
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        Assert.assertEquals(YesNo.Y, operation.getFan());
        Assert.assertEquals(pst.getParentReference(), operation.getParentReference());

        operation = (GLOperation) baseEntityRepository.findById(GLOperation.class, operation.getId());
        Assert.assertEquals(OperState.LOAD, operation.getState());
    }

    /**
     * Регистрация веерной операции с датой валютирования в прошедшем операционном дне
     * (в случае проводке в прошлом ОД дата проводки всегда равна текущей)
     * @fsd 7.3 ?
     */
    @Test public void testLoadBackValue() {
        final long st = System.currentTimeMillis();
        EtlPackage etlPackage = newPackage(st, "Checking fan base logic");
        EtlPosting pst = newPosting(st, etlPackage);
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());
        pst.setValueDate(getOperday().getLastWorkingDay());
        pst.setFan(YesNo.Y);
        pst.setParentReference("fanRef_" + st);
        pst.setAccountCredit("40817036200012959997");
        pst.setAccountDebit("40817036250010000018");
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        Assert.assertEquals(YesNo.Y, operation.getFan());
        Assert.assertEquals(pst.getParentReference(), operation.getParentReference());

        operation = (GLOperation) baseEntityRepository.findById(GLOperation.class, operation.getId());
        Assert.assertEquals(OperState.LOAD, operation.getState());
        Assert.assertEquals(pst.getValueDate(), operation.getValueDate());
        Assert.assertEquals(getOperday().getLastWorkingDay(), operation.getPostDate());
    }

    /**
     *  Регистрация веерной операции из трех частичных веерных операций
     *  @fsd 7.6.2
     */
    @Test public void testFanPostingSimple() {
        registerSimpleFans();
    }

    private String registerSimpleFans() {
        return registerSimpleFans("40806810700010000465", "40702810100013995679", "40702810900010002613");
    }

    private String registerSimpleFans(String accDebit, String accCredit, String accThree) {
        final long st = System.currentTimeMillis();
        EtlPackage etlPackage = newPackageNotSaved(st, "Checking fan posting logic");
        etlPackage.setAccountCnt(2);                        // число перьев в веере
        baseEntityRepository.save(etlPackage);

        EtlPosting pst = newPosting(st, etlPackage);
        pst.setValueDate(getOperday().getCurrentDate());
        pst.setFan(YesNo.Y);
        String parentRef = pst.getPaymentRefernce();
        pst.setParentReference(parentRef);

//        "40702810100013995679"        // MOS, RUB
//        "40806810700010000465"        // MOS, RUB
//        "40702810900010002613"        // MOS, RUB
//        "47427978400404502369"        // EKB, EUR
//        "47427810550160009330"        // CHL, RUB
//        "30302840700010000033"        // MOS, USD

//        pst.setAccountDebit("40806810700010000465");        // MOS, RUB
//        pst.setAccountCredit("40702810100013995679");       // MOS, RUB
        pst.setAccountDebit(accDebit);        // MOS, RUB
        pst.setAccountCredit(accCredit);       // MOS, RUB
        pst.setAmountCredit(new BigDecimal("1100.010"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.RUB);
        pst.setCurrencyDebit(pst.getCurrencyCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);
        String pmt_ref = pst.getPaymentRefernce();

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(null != operation && 0 < operation.getId());
        Assert.assertEquals(YesNo.Y, operation.getFan());
        Assert.assertEquals(pst.getParentReference(), operation.getParentReference());

        pst = newFanPosting(System.currentTimeMillis(), pst, "C", new BigDecimal("1200.020"),
                accThree, null, null);
        pst = (EtlPosting) baseEntityRepository.save(pst);

        operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());
        Assert.assertEquals(YesNo.Y, operation.getFan());
        Assert.assertEquals(pst.getParentReference(), operation.getParentReference());

        pst = newFanPosting(System.currentTimeMillis(), pst, "C", new BigDecimal("1300.030"),
                accThree, null, null);
        pst = (EtlPosting) baseEntityRepository.save(pst);

        operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());
        Assert.assertEquals(YesNo.Y, operation.getFan());
        Assert.assertEquals(pst.getParentReference(), operation.getParentReference());
        return operation.getParentReference();
    }

    /**
     * Обработка веерной операции в одном филиале, в одной валюте (1 проводка)
     * @fsd 7.6.3
     * @throws SQLException
     */
    @Test public void testFanControllerSimple() throws SQLException {
        testFanPostingSimple();

        DataRecord rec = baseEntityRepository.selectOne(
                "select PAR_RF from GL_OPER where GLOID = (select max(GLOID) from GL_OPER)");
        Assert.assertNotNull(rec);
        String parentRef = rec.getString(0);

        List<GLOperation> operList = fanPostingController.processOperations(parentRef);
        Assert.assertTrue(!operList.isEmpty());

        for (GLOperation operation : operList) {
            operation = (GLOperation) baseEntityRepository.selectOne(GLOperation.class,
                    "from GLOperation o left join fetch o.parentOperation where o.id = ?1", operation.getId());
            operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getParentOperation().getId());
            Assert.assertEquals(operation.getState(), OperState.POST);
            Assert.assertNotNull(operation.getFpSide());
            Assert.assertNotNull(operation.getParentOperation());
        }

        GLOperation oper = operList.get(0);
        oper = (GLOperation) baseEntityRepository.selectOne(GLOperation.class,
                "from GLOperation o left join fetch o.parentOperation where o.id = ?1", oper.getId());
        GLOperation operMain = (GLOperation) baseEntityRepository.findById(oper.getClass(), oper.getParentOperation().getId());
        Assert.assertNotNull(operMain);
        Assert.assertNotNull(operMain.getFbSide());
        Assert.assertNotNull(operMain.getAmountFan());
        Assert.assertNotNull(operMain.getAmountFanRu());

        List<GLPosting> postList = getPostings(operMain);
        Assert.assertNotNull(postList);                 // 1 проводка
        Assert.assertEquals(postList.size(), 1);

        List<Pd> pdList = getFanPostingPd(postList.get(0));
        Assert.assertNotNull(pdList);
        Assert.assertEquals(operList.size() + 1, pdList.size());
        long sum = 0;
        for( Pd pd: pdList) {
            Assert.assertTrue(pd.getAmountBC() != 0L);
            sum += pd.getAmountBC();
        }
        Assert.assertEquals(sum, 0);

        checkStatePost(parentRef);

    }

    /**
     * Обработка веерной операции межфилиальной, в одной валюте (2 проводка)
     * @fsd 7.6.3
     * @throws SQLException
     */
    @Test
    public void testFanMfo() throws SQLException {
        final long st = System.currentTimeMillis();
        EtlPackage etlPackage = newPackageNotSaved(st, "Checking fan posting logic with MFO");
        etlPackage.setAccountCnt(2);                        // число перьев в веере
        etlPackage = (EtlPackage) baseEntityRepository.save(etlPackage);

        String paymentRef = "PM_" + ("" + System.currentTimeMillis()).substring(5);

        // основная проводка
        EtlPosting pst1 = createFanPosting(st, etlPackage, "40806810700010000465"
                , "40702810100013995679", new BigDecimal("100.12"), BankCurrency.RUB
                , new BigDecimal("100.12"), BankCurrency.RUB, paymentRef, paymentRef, YesNo.Y);

        GLOperation oper1 = (GLOperation) postingController.processMessage(pst1);
        Assert.assertFalse(oper1.isInterFilial());

        // неосновная проводка (счета открыты в разных филиалах)
        EtlPosting pst2 = createFanPosting(st, etlPackage, "40806810700010000465"
                , "47427810550160009330", new BigDecimal("100.12"), BankCurrency.RUB
                , new BigDecimal("100.12"), BankCurrency.RUB, paymentRef + "_child", paymentRef, YesNo.Y);

        GLOperation oper2 = (GLOperation) postingController.processMessage(pst2);

        // process fan fully
        List<GLOperation> operList = fanPostingController.processOperations(paymentRef);
        Assert.assertEquals(2, operList.size());

        oper2 = (GLOperation) baseEntityRepository.findById(GLOperation.class, oper2.getId());
        Assert.assertTrue(oper2.isInterFilial());
        Assert.assertNotNull(oper2.getCurrencyMfo());
        Assert.assertNotNull(oper2.getAccountLiability());
        Assert.assertNotNull(oper2.getAccountAsset());

        // по основной проводке три записи в PD
        List<Pd> pds1 = baseEntityRepository.select(Pd.class,
                "select p from Pd p, GLPosting pst where pst.operation = ?1 and pst.id = p.pcId", oper1);
        Assert.assertEquals(3, pds1.size());
        // Основной счет веера
        Assert.assertNotNull(find(pds1, input -> input.getBsaAcid().equals("40806810700010000465"), null));
        // Не основной счет по кредиту гданой операции
        Assert.assertNotNull(find(pds1, input -> input.getBsaAcid().equals("40702810100013995679"), null));
        // счет заменен на счет МФО
        Assert.assertNull(find(pds1, input -> input.getBsaAcid().equals("47427810550160009330"), null));

        // для другой операции формируется только проводка на/с МФО
        List<Pd> pds2 = baseEntityRepository.select(Pd.class,
                "select p from Pd p, GLPosting pst where pst.operation = ?1 and pst.id = p.pcId", oper2);
        Assert.assertEquals(2, pds2.size());
        // счет заменен на счет МФО
        Assert.assertNotNull(find(pds2, input -> input.getBsaAcid().equals("47427810550160009330"), null));
        Assert.assertNull(find(pds2, input -> input.getBsaAcid().equals("40806810700010000465"), null));

        // проверка суммы
        checkFunSumma(pst1.getParentReference(), oper1.getId(), GLOperation.OperSide.D);

        // проверка статуса обработки
        checkStatePost(paymentRef);
    }

    /**
     * Обработка веерной операции в разных филиалах, с курсовой разницей (3 проводка) (веер по дебету)
     * @fsd 7.6.3
     * @throws SQLException
     */
    @Test
    public void testFanMfoExchD() throws SQLException {
        final long st = System.currentTimeMillis();
        EtlPackage etlPackage = newPackageNotSaved(st, "Checking fan posting logic with MFO with exchange");
        etlPackage.setAccountCnt(2);                        // число перьев в веере
        etlPackage = (EtlPackage) baseEntityRepository.save(etlPackage);

        String paymentRef = "PM_" + ("" + System.currentTimeMillis()).substring(5);

        // основная проводка
        EtlPosting pst1 = createFanPosting(st, etlPackage, "40806810700010000465"
                , "40702810100013995679", new BigDecimal("100.12"), BankCurrency.RUB
                , new BigDecimal("100.12"), BankCurrency.RUB, paymentRef, paymentRef, YesNo.Y);

        GLOperation oper1 = (GLOperation) postingController.processMessage(pst1);
        Assert.assertFalse(oper1.isInterFilial());

        // неосновная проводка (счета открыты в разных филиалах + курсовая разница)
        EtlPosting pst2 = createFanPosting(st, etlPackage, "40806810700010000465"
                , "47427978400404502369", new BigDecimal("100.12"), BankCurrency.RUB
                , new BigDecimal("5.23"), BankCurrency.EUR, paymentRef + "_child", paymentRef, YesNo.Y);

        GLOperation oper2 = (GLOperation) postingController.processMessage(pst2);

        // process fan fully
        List<GLOperation> operList = fanPostingController.processOperations(paymentRef);
        Assert.assertEquals(2, operList.size());

        oper2 = (GLOperation) baseEntityRepository.findById(GLOperation.class, oper2.getId());
        Assert.assertTrue(oper2.isInterFilial());
        Assert.assertNotNull(oper2.getCurrencyMfo());
        Assert.assertNotNull(oper2.getAccountLiability());
        Assert.assertNotNull(oper2.getAccountAsset());
        Assert.assertTrue(oper2.isExchangeDifferenceA());

        // по основной проводке три записи в PD
        List<Pd> pds1 = baseEntityRepository.select(Pd.class,
                "select p from Pd p, GLPosting pst where pst.operation = ?1 and pst.id = p.pcId", oper1);
        Assert.assertEquals(3, pds1.size());

        // Основной счет веера
        Assert.assertNotNull(find(pds1, input -> input.getBsaAcid().equals("40806810700010000465"), null));
        // Не основной счет по кредиту главной операции
        Assert.assertNotNull(find(pds1, input -> input.getBsaAcid().equals("40702810100013995679"), null));
        // счет заменен на счет МФО
        Assert.assertNull(find(pds1, input -> input.getBsaAcid().equals("47427978400404502369"), null));

        // для другой операции формируется проводка на/с МФО + проводка по курсовой разнице в
        List<Pd> pds2 = baseEntityRepository.select(Pd.class,
                "select p from Pd p, GLPosting pst where pst.operation = ?1 and pst.id = p.pcId", oper2);
        Assert.assertEquals(4, pds2.size());

        // проверки по проводке МФО: счет заменен на счет МФО
        Pd pdNotReplaced = find(pds2, input -> input.getBsaAcid().equals("47427978400404502369"), null);
        Assert.assertNotNull(pdNotReplaced);
        // replaced here
        Assert.assertNull(find(pds2, input -> input.getBsaAcid().equals("40806810700010000465"), null));

        // с межфилиального счета, который попал в веер по второй операции будет отведена курсовая разница
        // находим межфилиальный счет в проводке веера
        Pd mfoFeather = find(pds1
                , input -> !input.getBsaAcid().equals("40806810700010000465")
                    && !input.getBsaAcid().equals("40702810100013995679"), null);
        Assert.assertNotNull(mfoFeather);

        Pd mfoWithinExchDiff = find(pds2, input -> input.getBsaAcid().equals(mfoFeather.getBsaAcid()), null);
        Assert.assertNotNull(mfoWithinExchDiff);

        // проверка суммы
        checkFunSumma(pst1.getParentReference(), oper1.getId(), GLOperation.OperSide.D);

        // проверка статуса обработки
        checkStatePost(paymentRef);
    }

    /**
     * Обработка веерной операции в разных филиалах, с курсовой разницей (3 проводка) (веер по кредиту)
     * @fsd 7.6.3
     * @throws SQLException
     */
    @Test
    public void testFanMfoExchC() throws SQLException {
        final long st = System.currentTimeMillis();
        EtlPackage etlPackage = newPackageNotSaved(st, "Checking fan posting logic with MFO with exchange");
        etlPackage.setAccountCnt(2);                        // число перьев в веере
        etlPackage = (EtlPackage) baseEntityRepository.save(etlPackage);

        String paymentRef = "PM_" + ("" + System.currentTimeMillis()).substring(5);

        // основная проводка
        EtlPosting pst1 = createFanPosting(st, etlPackage
                , "40702810100013995679", "40806810700010000465"
                , new BigDecimal("100.12"), BankCurrency.RUB
                , new BigDecimal("100.12"), BankCurrency.RUB
                , paymentRef, paymentRef, YesNo.Y);

        GLOperation oper1 = (GLOperation) postingController.processMessage(pst1);
        Assert.assertFalse(oper1.isInterFilial());

        // неосновная проводка (счета открыты в разных филиалах + курсовая разница)
        EtlPosting pst2 = createFanPosting(st, etlPackage
                , "47427978400404502369", "40806810700010000465"
                , new BigDecimal("5.23"), BankCurrency.EUR
                , new BigDecimal("100.12"), BankCurrency.RUB
                , paymentRef + "_child", paymentRef, YesNo.Y);

        GLOperation oper2 = (GLOperation) postingController.processMessage(pst2);

        // process fan fully
        List<GLOperation> operList = fanPostingController.processOperations(paymentRef);
        Assert.assertEquals(2, operList.size());

        oper2 = (GLOperation) baseEntityRepository.findById(GLOperation.class, oper2.getId());
        Assert.assertTrue(oper2.isInterFilial());
        Assert.assertNotNull(oper2.getCurrencyMfo());
        Assert.assertNotNull(oper2.getAccountLiability());
        Assert.assertNotNull(oper2.getAccountAsset());
        Assert.assertTrue(oper2.isExchangeDifferenceA());

        // по основной проводке три записи в PD
        List<Pd> pds1 = baseEntityRepository.select(Pd.class,
                "select p from Pd p, GLPosting pst where pst.operation = ?1 and pst.id = p.pcId", oper1);
        Assert.assertEquals(3, pds1.size());

        // Основной счет веера
        Assert.assertNotNull(find(pds1, input -> input.getBsaAcid().equals("40806810700010000465"), null));
        // Не основной счет по кредиту главной операции
        Assert.assertNotNull(find(pds1, input -> input.getBsaAcid().equals("40702810100013995679"), null));
        // счет заменен на счет МФО
        Assert.assertNull(find(pds1, input -> input.getBsaAcid().equals("47427978400404502369"), null));

        // для другой операции формируется проводка на/с МФО + проводка по курсовой разнице в
        List<Pd> pds2 = baseEntityRepository.select(Pd.class,
                "select p from Pd p, GLPosting pst where pst.operation = ?1 and pst.id = p.pcId", oper2);
        Assert.assertEquals(4, pds2.size());

        // проверки по проводке МФО: счет заменен на счет МФО
        Pd pdNotReplaced = find(pds2, input -> input.getBsaAcid().equals("47427978400404502369"), null);
        Assert.assertNotNull(pdNotReplaced);
        // replaced here
        Assert.assertNull(find(pds2, input -> input.getBsaAcid().equals("40806810700010000465"), null));

        // с межфилиального счета, который попал в веер по второй операции будет отведена курсовая разница
        // находим межфилиальный счет в проводке веера
        Pd mfoFeather = find(pds1
                , input -> !input.getBsaAcid().equals("40806810700010000465")
                        && !input.getBsaAcid().equals("40702810100013995679"), null);
        Assert.assertNotNull(mfoFeather);

        Pd mfoWithinExchDiff = find(pds2, input -> input.getBsaAcid().equals(mfoFeather.getBsaAcid()), null);
        Assert.assertNotNull(mfoWithinExchDiff);

        // проверка суммы
        checkFunSumma(pst1.getParentReference(), oper1.getId(), GLOperation.OperSide.C);

        // проверка статуса обработки
        checkStatePost(paymentRef);
    }

    /**
     * Обработка веерной операции в разных филиалах, при отсутствии межфилиальных счетов
     * (автоматическая генерация счетов межфилиальных переводов)
     * @fsd 7.6.3
     * @throws SQLException
     */
    @Test
    public void testMfo() throws SQLException {
        baseEntityRepository.executeNativeUpdate("delete from ibcb where (ibbrnm='MOS') AND (ibcbrn='CHL') AND (ibccy ='RUR')");

        checkIbcb(true);

        final long st = System.currentTimeMillis();
        EtlPackage etlPackage = newPackageNotSaved(st, "Checking posting logic with MFO");
        etlPackage = (EtlPackage) baseEntityRepository.save(etlPackage);

        String paymentRef = "PM_" + ("" + System.currentTimeMillis()).substring(5);

        // основная проводка
        EtlPosting pst1 = createFanPosting(st, etlPackage, "40806810700010000465"
                , "47427810550160009330", new BigDecimal("100.12"), BankCurrency.RUB
                , new BigDecimal("100.12"), BankCurrency.RUB, paymentRef, paymentRef, YesNo.N);

        GLOperation oper1 = (GLOperation) postingController.processMessage(pst1);
        Assert.assertTrue(oper1.isInterFilial());

        List<Pd> pds1 = baseEntityRepository.select(Pd.class,
                "select p from Pd p, GLPosting pst where pst.operation = ?1 and pst.id = p.pcId", oper1);
        Assert.assertEquals(4, pds1.size());

        // проверка статуса обработки
        checkStatePost(paymentRef);

        // проверка создания мф счета
        checkIbcb(false);
    }

    /**
     * Обработка веерной операции в одном филиале, с курсовой разницей (2 проводка)
     * @fsd 7.6.3
     * @throws SQLException
     */
    @Test
    public void testFanExch() throws SQLException {
        final long st = System.currentTimeMillis();
        EtlPackage etlPackage = newPackageNotSaved(st, "Checking fan posting logic with MFO with exchange");
        etlPackage.setAccountCnt(2);                        // число перьев в веере
        etlPackage = (EtlPackage) baseEntityRepository.save(etlPackage);

        String paymentRef = "PM_" + ("" + System.currentTimeMillis()).substring(5);

        // основная проводка
        EtlPosting pst1 = createFanPosting(st, etlPackage, "40806810700010000465"
                , "40702810100013995679", new BigDecimal("100.12"), BankCurrency.RUB
                , new BigDecimal("100.12"), BankCurrency.RUB, paymentRef, paymentRef, YesNo.Y);

        GLOperation oper1 = (GLOperation) postingController.processMessage(pst1);
        Assert.assertFalse(oper1.isInterFilial());

        // неосновная проводка (счета открыты в одном филиале , но есть курсовая разница)
        EtlPosting pst2 = createFanPosting(st, etlPackage, "40806810700010000465"
                , "30302840700010000033", new BigDecimal("100.12"), BankCurrency.RUB
                , new BigDecimal("55.23"), BankCurrency.USD, paymentRef + "_child", paymentRef, YesNo.Y);

        GLOperation oper2 = (GLOperation) postingController.processMessage(pst2);

        // process fan fully
        List<GLOperation> operList = fanPostingController.processOperations(paymentRef);
        Assert.assertEquals(2, operList.size());

        oper2 = (GLOperation) baseEntityRepository.findById(GLOperation.class, oper2.getId());
        Assert.assertFalse(oper2.isInterFilial());
        Assert.assertTrue(oper2.isExchangeDifferenceA());

        // по основной проводке три записи в PD
        List<Pd> pds1 = baseEntityRepository.select(Pd.class,
                "select p from Pd p, GLPosting pst where pst.operation = ?1 and pst.id = p.pcId", oper1);
        Assert.assertEquals(3, pds1.size());
        // Основной счет веера
        Assert.assertNotNull(find(pds1, input -> input.getBsaAcid().equals("40806810700010000465"), null));
        // Не основной счет по кредиту главной операции
        Assert.assertNotNull(find(pds1, input -> input.getBsaAcid().equals("40702810100013995679"), null));
        // Не основной счет по кредиту главной операции
        Assert.assertNotNull(find(pds1, input -> input.getBsaAcid().equals("30302840700010000033"), null));

        // для другой операции формируется проводка по курсовой разнице в
        List<Pd> pds2 = baseEntityRepository.select(Pd.class,
                "select p from Pd p, GLPosting pst where pst.operation = ?1 and pst.id = p.pcId", oper2);
        Assert.assertEquals(2, pds2.size());
        // курсовую разницу отводим с кредита
        Assert.assertNotNull(find(pds1, input -> input.getBsaAcid().equals("30302840700010000033"), null));

        // проверка суммы
        checkFunSumma(pst1.getParentReference(), oper1.getId(), GLOperation.OperSide.D);

        // проверка статуса обработки
        checkStatePost(paymentRef);
    }

    /**
     * Обработка веерной операции в одном филиале, в разной валюте, без курсовой разницы (2 проводка)
     * @fsd 7.6.3
     * @throws SQLException
     */
    @Test
    public void testFanExchRu() throws SQLException {
        final long st = System.currentTimeMillis();
        EtlPackage etlPackage = newPackageNotSaved(st, "Checking fan posting logic with MFO with exchange");
        etlPackage.setAccountCnt(2);                        // число перьев в веере
        etlPackage = (EtlPackage) baseEntityRepository.save(etlPackage);

        String paymentRef = "PM_" + ("" + System.currentTimeMillis()).substring(5);

        // основная проводка
        // неосновная проводка (счета открыты в одном филиале , но есть курсовая разница)
        EtlPosting pst1 = createFanPosting(st, etlPackage, "30114978400010115618"
                , "61304810400014521019", new BigDecimal("775.820"), BankCurrency.EUR
                , new BigDecimal("56704.070"), BankCurrency.RUB, paymentRef, paymentRef, YesNo.Y);
        pst1.setAmountDebitRu(new BigDecimal("56704.070"));
        pst1.setAmountCreditRu(pst1.getAmountDebitRu());

        GLOperation oper1 = (GLOperation) postingController.processMessage(pst1);
        Assert.assertFalse(oper1.isInterFilial());

        // неосновная проводка (счета открыты в одном филиале , но есть курсовая разница)
        EtlPosting pst2 = createFanPosting(st, etlPackage, "30114978400010115618"
                , "47423978500014506894", new BigDecimal("120.200"), BankCurrency.EUR
                , new BigDecimal("120.200"), BankCurrency.EUR, paymentRef + "_child", paymentRef, YesNo.Y);
        pst2.setAmountDebitRu(new BigDecimal("8800.260"));
        pst2.setAmountCreditRu(pst2.getAmountDebitRu());

        GLOperation oper2 = (GLOperation) postingController.processMessage(pst2);

        // process fan fully
        List<GLOperation> operList = fanPostingController.processOperations(paymentRef);
        Assert.assertEquals(2, operList.size());

        oper2 = (GLOperation) baseEntityRepository.findById(GLOperation.class, oper2.getId());
        Assert.assertFalse(oper2.isInterFilial());
        Assert.assertFalse(oper2.isExchangeDifferenceA());

        // по основной проводке три записи в PD
        List<Pd> pds1 = baseEntityRepository.select(Pd.class,
                "select p from Pd p, GLPosting pst where pst.operation = ?1 and pst.id = p.pcId", oper1);
        Assert.assertEquals(3, pds1.size());
        // Основной счет веера
        Assert.assertNotNull(find(pds1, input -> input.getBsaAcid().equals("30114978400010115618"), null));
        // Не основной счет по кредиту главной операции
        Assert.assertNotNull(find(pds1, input -> input.getBsaAcid().equals("61304810400014521019"), null));
        // Не основной счет по кредиту главной операции
        Assert.assertNotNull(find(pds1, input -> input.getBsaAcid().equals("61304810400014521019"), null));

        // проверка суммы
        checkFunSumma(pst1.getParentReference(), oper1.getId(), GLOperation.OperSide.D);

        // проверка статуса обработки
        checkStatePost(paymentRef);
    }

    /**
     * Обработка веерной операций на фаза PreCob в конце операционного дня:
     * @fsd 7.6.2
     */
    @Test
    public void testFanAsPreCobStep() {

        baseEntityRepository.executeNativeUpdate("update gl_acc set dtc = null where bsaacid = ?","40702810100013995679");
        baseEntityRepository.executeNativeUpdate("update accrln set drlnc = null where bsaacid = ?", "40702810100013995679");
        baseEntityRepository.executeNativeUpdate("update bsaacc set bsaacc = ? where id = ?",
                Utl4Tests.parseDate("2029-01-01", "yyyy-MM-dd"), "40702810100013995679");


        final long st = System.currentTimeMillis();
        EtlPackage etlPackage = newPackageNotSaved(st, "Checking fan posting logic with MFO");
        etlPackage.setAccountCnt(2);                        // число перьев в веере
        etlPackage = (EtlPackage) baseEntityRepository.save(etlPackage);

        String paymentRef = "PM_" + ("" + System.currentTimeMillis()).substring(5);

        // основная проводка
        EtlPosting pst1 = createFanPosting(st, etlPackage, "40806810700010000465"
                , "40702810100013995679", new BigDecimal("100.12"), BankCurrency.RUB
                , new BigDecimal("100.12"), BankCurrency.RUB, paymentRef, paymentRef, YesNo.Y);

        GLOperation oper1 = (GLOperation) postingController.processMessage(pst1);
        Assert.assertFalse(oper1.isInterFilial());

        // неосновная проводка (счета открыты в разных филиалах)
        EtlPosting pst2 = createFanPosting(st, etlPackage, "40806810700010000465"
                , "47427810550160009330", new BigDecimal("100.12"), BankCurrency.RUB
                , new BigDecimal("100.12"), BankCurrency.RUB, paymentRef + "_child", paymentRef, YesNo.Y);

        GLOperation oper2 = (GLOperation) postingController.processMessage(pst2);

        remoteAccess.invoke(PreCobStepController.class, "processFan");

        checkStatePost(paymentRef);
    }

    @Ignore
    @Test public void testPerfomance() throws SQLException {
        final int count = 30;
        List<String> res = createFanOperation(count);
        Assert.assertEquals(count, res.size());

        for (String ref : res) {
            List<GLOperation> opers = baseEntityRepository.select(GLOperation.class, "from GLOperation o where o.parentReference = ?1", ref);
            Assert.assertTrue(opers.stream().anyMatch(p -> p.getState() == OperState.LOAD));
        }

        log.info("Starting processing");
        remoteAccess.invoke(PreCobStepController.class, "processFan");
        log.info("Processing has completed");

        for (String ref : res) {
            List<GLOperation> opers = baseEntityRepository.select(GLOperation.class,"from GLOperation o where o.parentReference = ?1", ref);
            Assert.assertTrue(opers.stream().map(o -> o.toString() + " >> " + o.getState() + " >> " + o.getParentReference())
                    .collect(Collectors.joining("} {", "{", "}"))
                    , opers.stream().anyMatch(p -> p.getState() == OperState.POST));
        }
    }

    private List<String> createFanOperation(int count) {
        List<String> refs = new ArrayList<>();
        String acc1;String acc2;String acc3;
        for (int i = 0; i < count; i++){
            if (i%3 == 0) {
                acc1 = "40502810600010186385"; acc2 = "40602810500934509312"; acc3 = "40701810200010102366";
            } else
            if (i%2 == 0) {
                acc1 = "40806810700010000465"; acc2 = "40702810100013995679"; acc3 = "40702810900010002613";
            } else {
                acc1 = "40701810600010528744"; acc2 = "40701810600010684387"; acc3 = "40701810700010005361";
            }
            String ref = registerSimpleFans(acc1, acc2, acc3);
            refs.add(ref);
        }
        return refs;
    }

    public void checkFunSumma(String par_rf, Long oper_id, GLOperation.OperSide fbSide) throws SQLException {
        // проверка суммы веера
        DataRecord rec = baseEntityRepository.selectFirst(
                "select sum(case FP_SIDE when 'C' then AMT_DR else AMT_CR end), sum(AMTR_POST) from GL_OPER where PAR_RF = ?",
                par_rf);

        GLOperation oper = (GLOperation) baseEntityRepository.findById(GLOperation.class, oper_id);

        Assert.assertTrue(oper.getFbSide().equals(fbSide));
        Assert.assertFalse(oper.getFpSide().equals(fbSide));
        BigDecimal amt = rec.getBigDecimal(0);
        BigDecimal amtru = rec.getBigDecimal(1);

        Assert.assertEquals(amt, oper.getAmountFan());
        Assert.assertEquals(amtru, oper.getAmountFanRu());
    }

    private void checkStatePost(String parentRef) {
        List<GLOperation> oprs = baseEntityRepository.select(GLOperation.class, "from GLOperation o where o.parentReference = ?1", parentRef);
        Assert.assertFalse(oprs.isEmpty());
        Assert.assertTrue(Iterables.all(oprs, input -> OperState.POST == input.getState()));
    }

    private void checkIbcb(boolean isEmpty) throws SQLException {
        List<DataRecord> ibcbs = baseEntityRepository
                .select("select * from ibcb where (ibbrnm='MOS') AND (ibcbrn='CHL') AND (ibccy ='RUR')");
        if (isEmpty) {
            Assert.assertTrue(ibcbs.size()+"", ibcbs.isEmpty());
        } else {
            Assert.assertFalse(ibcbs.size() + "", ibcbs.isEmpty());
        }
    }

}

