package ru.rbt.barsgl.ejbtest;

import com.google.common.collect.Iterables;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.*;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.PreCobStepController;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejbcore.mapping.YesNo;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by ER18837 on 20.05.15.
 * Обработка операций СТОРНО в формате веерной проводки
 * @fsd 7.7
 */
public class FanStornoIT extends AbstractTimerJobIT {

    @BeforeClass
    public static void beforeClass() {
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
    }

    @After
    public void afterProc() {
        restoreOperday();
    }


    /**
     * Обработка операции сторно веера в текущий день (отмена операции)
     * @fsd 7.7.2.2
     */
    @Test
    public void testFanStornoOneday() throws SQLException {

        final String paymentRef = "PM_" + ("" + System.currentTimeMillis()).substring(5);

        Date operday = getOperday().getCurrentDate();
        List<EtlPosting> etlList = createMfoExchPostings(paymentRef, operday);
        List<GLOperation> operList = processPostings(etlList);

        List<EtlPosting> etlListS = createStornoPostings(etlList, paymentRef, operday);
        processPostings(etlListS);

        // process fan storno
        List<GLOperation> operListS = fanStornoOnedayController.processOperations(paymentRef);
        Assert.assertEquals(2, operListS.size());

        int i = 0;
        for (GLOperation operS : operListS) {
            GLOperation oper = operList.get(i);
            operS = (GLOperation) baseEntityRepository.findById(operS.getClass(), operS.getId());
            Assert.assertEquals(operS.getState(), OperState.SOCANC);
            Assert.assertEquals(operS.getPstScheme(), GLOperation.OperType.ST);
            Assert.assertEquals(operS.getStornoRegistration(), GLOperation.StornoType.C);
            Assert.assertEquals(operS.getStornoOperation().getId(), oper.getId());        // ссылка на сторно операцию
            Assert.assertTrue(getPostings(operS).isEmpty());                    // нет своих проводки

            oper = (GLOperation) baseEntityRepository.findById(oper.getClass(), oper.getId());
            Assert.assertEquals(oper.getState(), OperState.CANC);
            Assert.assertEquals(oper.getPstScheme(), GLOperation.OperType.F);
            Assert.assertTrue(getPostings(oper).isEmpty());                    // нет своих проводки
            i++;
        }
    }

    /**
     * Обработка операции сторно веера backvalue в одном филиале, в одной валюте (1 проводка)
     * @fsd 7.7.3
     */
    @Test
    public void testFanStornoSimple() throws SQLException {

        final String paymentRef = "PM_" + ("" + System.currentTimeMillis()).substring(5);

        Date operday = getOperday().getCurrentDate();
        List<EtlPosting> etlList = createSimplePostings(paymentRef, operday);
        processPostings(etlList);

        // process fan
        List<GLOperation> operListF = fanPostingController.processOperations(paymentRef);
        Assert.assertEquals(2, operListF.size());

        // Сторно операция - сдвигем день вперед на 1
        setOperday(DateUtils.addDays(operday, 1), operday, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.CLOSED);
        List<EtlPosting> etlListS = createStornoPostings(etlList, paymentRef, operday);
        processPostings(etlListS);

        // process fan storno
        List<GLOperation> operListS = fanStornoController.processOperations(paymentRef);
        Assert.assertEquals(2, operListS.size());

        checkOperations(operListS, operListF);
    }

    /**
     * Обработка операции сторно веера backvalue межфилиальной в одной валюте (2 проводки)
     * @fsd 7.7.3
     */
    @Test
    public void testFanStornoMfo() throws SQLException {

        final String paymentRef = "PM_" + ("" + System.currentTimeMillis()).substring(5);

        Date operday = getOperday().getCurrentDate();
        List<EtlPosting> etlList = createMfoPostings(paymentRef, operday);
        processPostings(etlList);

        // process fan
        List<GLOperation> operListF = fanPostingController.processOperations(paymentRef);
        Assert.assertEquals(etlList.size(), operListF.size());

        // Сторно операция - сдвигем день вперед на 1
        setOperday(DateUtils.addDays(operday, 1), operday, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.CLOSED);
        List<EtlPosting> etlListS = createStornoPostings(etlList, paymentRef, operday);
        processPostings(etlListS);

        // process fan storno
        List<GLOperation> operListS = fanStornoController.processOperations(paymentRef);
        Assert.assertEquals(2, operListS.size());

        checkOperations(operListS, operListF);
    }

    /**
     * Обработка операции сторно веера backvalue в одном филиале с курсовой разницей (2 проводки)
     * @fsd 7.7.3
     */
    @Test
    public void testFanStornoExch() throws SQLException {

        final String paymentRef = "PM_" + ("" + System.currentTimeMillis()).substring(5);

        Date operday = getOperday().getCurrentDate();
        List<EtlPosting> etlList = createExchPostings(paymentRef, operday);
        processPostings(etlList);

        // process fan
        List<GLOperation> operListF = fanPostingController.processOperations(paymentRef);
        Assert.assertEquals(etlList.size(), operListF.size());

        // Сторно операция - сдвигем день вперед на 1
        setOperday(DateUtils.addDays(operday, 1), operday, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.CLOSED);
        List<EtlPosting> etlListS = createStornoPostings(etlList, paymentRef, operday);
        processPostings(etlListS);

        // process fan storno
        List<GLOperation> operListS = fanStornoController.processOperations(paymentRef);
        Assert.assertEquals(2, operListS.size());

        checkOperations(operListS, operListF);
    }

    /**
     * Обработка операции сторно веера backvalue межфилиальной с курсовой разницей (3 проводки)
     * @fsd 7.7.3
     */
    @Test
    public void testFanStornoMfoExch() throws SQLException {

        final String paymentRef = "PM_" + ("" + System.currentTimeMillis()).substring(5);

        Date operday = getOperday().getCurrentDate();
        List<EtlPosting> etlList = createMfoExchPostings(paymentRef, operday);
        processPostings(etlList);

        // process fan
        List<GLOperation> operListF = fanPostingController.processOperations(paymentRef);
        Assert.assertEquals(etlList.size(), operListF.size());

        // Сторно операция - сдвигем день вперед на 1
        setOperday(DateUtils.addDays(operday, 1), operday, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.CLOSED);
        List<EtlPosting> etlListS = createStornoPostings(etlList, paymentRef, operday);
        processPostings(etlListS);

        // process fan storno
        List<GLOperation> operListS = fanStornoController.processOperations(paymentRef);
        Assert.assertEquals(2, operListS.size());

        checkOperations(operListS, operListF);
    }

    private List<EtlPosting> createSimplePostings(String paymentRef, Date operday) throws SQLException {
        long st = System.currentTimeMillis();
        EtlPackage etlPackage1 = newPackageNotSaved(st, "Checking fan posting logic simple");
        etlPackage1.setPostingCnt(2);                        // число перьев в веере
        etlPackage1 = (EtlPackage) baseEntityRepository.save(etlPackage1);

        List<EtlPosting> etlList = new ArrayList<EtlPosting>();
        String acc1 = findBsaAccount("4080681070001%");
        String acc2 = findBsaAccount("4070281090001%");
        String acc3 = findBsaAccount("4070281010001%");
        // неосновная проводка (счета открыты в одном филиале)
        etlList.add(createFanPosting(st, etlPackage1, acc1, acc2
                , new BigDecimal("101.13"), BankCurrency.RUB
                , new BigDecimal("101.13"), BankCurrency.RUB, paymentRef + "_child", paymentRef, YesNo.Y));

        // основная проводка
        etlList.add(createFanPosting(st, etlPackage1, acc1, acc3
                , new BigDecimal("100.12"), BankCurrency.RUB
                , new BigDecimal("100.12"), BankCurrency.RUB, paymentRef, paymentRef, YesNo.Y));

        return etlList;
    }

    private List<EtlPosting> createMfoPostings (String paymentRef, Date operday) throws SQLException {
        long st = System.currentTimeMillis();
        EtlPackage etlPackage = newPackageNotSaved(st, "Checking fan posting logic with MFO");
        etlPackage.setPostingCnt(2);                        // число перьев в веере
        etlPackage = (EtlPackage) baseEntityRepository.save(etlPackage);

        List<EtlPosting> etlList = new ArrayList<EtlPosting>();
        String acc1 = findBsaAccount("4080681070001%");
        String acc2 = findBsaAccount("4070281010001%");
        String acc3 = findBsaAccount("4742781055016%");
        // основная проводка
        etlList.add(createFanPosting(st, etlPackage, acc1, acc2
                , new BigDecimal("100.12"), BankCurrency.RUB
                , new BigDecimal("100.12"), BankCurrency.RUB, paymentRef, paymentRef, YesNo.Y));

        // неосновная проводка (счета открыты в разных филиалах)
        etlList.add(createFanPosting(st, etlPackage, acc1, acc3
                , new BigDecimal("100.12"), BankCurrency.RUB
                , new BigDecimal("100.12"), BankCurrency.RUB, paymentRef + "_child", paymentRef, YesNo.Y));

        return etlList;
    }

    private List<EtlPosting> createMfoExchPostings (String paymentRef, Date operday) throws SQLException {
        long st = System.currentTimeMillis();
        EtlPackage etlPackage = newPackageNotSaved(st, "Checking fan posting logic with MFO with Exchange");
        etlPackage.setPostingCnt(2);                        // число перьев в веере
        etlPackage = (EtlPackage) baseEntityRepository.save(etlPackage);

        List<EtlPosting> etlList = new ArrayList<EtlPosting>();
        // основная проводка
        String acc1 = findBsaAccount("4080681070001%");
        String acc2 = findBsaAccount("4070281010001%");
        String acc3 = findBsaAccount("4742797840040%");
        etlList.add(createFanPosting(st, etlPackage, acc1, acc2
                , new BigDecimal("100.12"), BankCurrency.RUB
                , new BigDecimal("100.12"), BankCurrency.RUB, paymentRef, paymentRef, YesNo.Y));

        // неосновная проводка (счета открыты в разных филиалах + курсовая разница)
        etlList.add(createFanPosting(st, etlPackage, acc1, acc3
                , new BigDecimal("100.12"), BankCurrency.RUB
                , new BigDecimal("5.23"), BankCurrency.EUR, paymentRef + "_child", paymentRef, YesNo.Y));

        return etlList;
    }

    private List<EtlPosting> createExchPostings (String paymentRef, Date operday) throws SQLException {
        long st = System.currentTimeMillis();
        EtlPackage etlPackage = newPackageNotSaved(st, "Checking fan posting logic with MFO with Exchange");
        etlPackage.setPostingCnt(2);                        // число перьев в веере
        etlPackage = (EtlPackage) baseEntityRepository.save(etlPackage);

        List<EtlPosting> etlList = new ArrayList<EtlPosting>();
        String acc1 = findBsaAccount("4080681070001%");
        String acc2 = findBsaAccount("4070281010001%");
        String acc3 = findBsaAccount("3030284070001%");
        // основная проводка
        etlList.add(createFanPosting(st, etlPackage, acc1, acc2
                , new BigDecimal("100.12"), BankCurrency.RUB
                , new BigDecimal("100.12"), BankCurrency.RUB, paymentRef, paymentRef, YesNo.Y));

        // неосновная проводка (счета открыты в одном филиале , но есть курсовая разница)
        etlList.add(createFanPosting(st, etlPackage, acc1, acc3
                , new BigDecimal("100.12"), BankCurrency.RUB
                , new BigDecimal("55.23"), BankCurrency.USD, paymentRef + "_child", paymentRef, YesNo.Y));

        return etlList;
    }

    private List<EtlPosting> createStornoPostings(List<EtlPosting> etlList, String paymentRef, Date valueDate) {
        long st=System.currentTimeMillis();
        EtlPackage etlPackage = newPackageNotSaved(st, "Checking fan storno posting logic");
        etlPackage.setPostingCnt(etlList.size());                        // число перьев в веере
        etlPackage=(EtlPackage)baseEntityRepository.save(etlPackage);

        List<EtlPosting> etlListS = new ArrayList<EtlPosting>();
        for( EtlPosting post :etlList) {
            EtlPosting postS = newStornoFanPosting(st, etlPackage, post, paymentRef, valueDate);
            postS.setValueDate(valueDate);
            postS = (EtlPosting) baseEntityRepository.save(postS);
            etlListS.add(postS);
        }
        return etlListS;
    }

    private void checkOperations(List<GLOperation> operListS, List<GLOperation> operListF) {
        int i = 0;
        for (GLOperation operS : operListS) {
            GLOperation operF = operListF.get(i);
            operF = (GLOperation) baseEntityRepository.findById(operF.getClass(), operF.getId());
            Assert.assertEquals(OperState.POST, operF.getState());

            operS = (GLOperation) baseEntityRepository.findById(operS.getClass(), operS.getId());
            Assert.assertEquals(OperState.POST, operS.getState());
            Assert.assertEquals(operF.getPstScheme(), operS.getPstScheme());
            Assert.assertEquals(operS.getStornoRegistration(), GLOperation.StornoType.S);
            Assert.assertEquals(operS.getStornoOperation().getId(), operF.getId());        // ссылка на сторно операцию

            List<GLPosting> postList = getPostings(operF);
            Assert.assertNotNull(postList);

            List<GLPosting> postListS = getPostings(operS);
            Assert.assertNotNull(postListS);
            Assert.assertEquals(postList.size(), postListS.size());

            for (GLPosting post : postListS) {
                checkStornoRef(post, postList);
            }
            i++;
        }
    }

    /**
     * Обработка веерных операций на фаза PreCob в течение двух операционных дней:
     * прямые операции в 3-х вариантах, сторно в тот же день и backValue
     * @fsd 7.7.2, 7.7.3
     */
    @Test
    public void testFanWithStornoAsPreCobStep() throws SQLException {

        Date operday1 = getOperday().getCurrentDate();

        // создание операция MfoExch в первый день (операция 1)
        final String paymentRef1 = "PM_" + ("" + System.currentTimeMillis()).substring(5);
        List<EtlPosting> etlList1 = createMfoExchPostings(paymentRef1, operday1);
        processPostings(etlList1);

        // обработка операций в первый день
        remoteAccess.invoke(PreCobStepController.class, "processFan");
        checkStatePost(paymentRef1, YesNo.N, OperState.POST);       // операция 1

        // сдвигем опердень вперед на 1 день
        Date operday2 = DateUtils.addDays(operday1, 1);
        setOperday(operday2, operday1, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.CLOSED);

        // создание операция Exch во второй день (операция 2)
        final String paymentRef2 = "PM_" + ("" + System.currentTimeMillis()).substring(5);
        List<EtlPosting> etlList2 = createExchPostings(paymentRef2, operday2);
        processPostings(etlList2);

        // создание операция Mfo во второй день (операция 3)
        final String paymentRef3 = "PM_" + ("" + System.currentTimeMillis()).substring(5);
        List<EtlPosting> etlList3 = createMfoPostings(paymentRef3, operday2);
        processPostings(etlList3);

        // создание сторно операция MfoExch во второй день (сторно операции 1 Backvalue)
        List<EtlPosting> etlList1S = createStornoPostings(etlList1, paymentRef1, operday2);
        processPostings(etlList1S);

        // создание сторно операция Exch во второй день (сторно операции 2 Oneday)
        List<EtlPosting> etlList2S = createStornoPostings(etlList2, paymentRef2, operday2);
        processPostings(etlList2S);

        // обработка операций во сторой день
        remoteAccess.invoke(PreCobStepController.class, "processFan");
        checkStatePost(paymentRef1, YesNo.Y, OperState.POST);       // сторно операции 1
        checkStatePost(paymentRef3, YesNo.N, OperState.POST);       // операция 3
        checkStatePost(paymentRef2, YesNo.N, OperState.CANC);       // операция 2
        checkStatePost(paymentRef2, YesNo.Y, OperState.SOCANC);     // сторно операции 2

    }

    private void checkStatePost(String parentRef, YesNo storno, OperState operState) {
        List<GLOperation> oprs = baseEntityRepository.select(GLOperation.class,
                "from GLOperation o where o.parentReference = ?1 and o.storno = ?2", parentRef, storno);
        Assert.assertFalse(oprs.isEmpty());
        Assert.assertTrue(parentRef + ":" + operState, Iterables.all(oprs, input -> operState == input.getState()));
    }

    /**
     * Обработка операции сторно веера в текущий день межфилиальной с курсовой разницей
     * @fsd 7.7.3
     */
    @Test
    @Ignore
    // на tmb01 тест проходит, потому что там есть такие операции. Но он фактически ничего не делает. Тест аналогичен testFanStornoOneday
    public void testOnedayStornoME() {

        Date operday1 = getOperday().getCurrentDate();

        final String paymentRef1 = "PMT_REF-N001";
        checkStatePost(paymentRef1, YesNo.N, OperState.POST);       // операция 1

        // обработка операций в первый день
        remoteAccess.invoke(PreCobStepController.class, "processFan");
        checkStatePost(paymentRef1, YesNo.N, OperState.POST);       // операция 1

    }


}
