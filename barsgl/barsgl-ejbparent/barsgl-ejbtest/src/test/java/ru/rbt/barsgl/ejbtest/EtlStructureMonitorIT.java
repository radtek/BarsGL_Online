package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.task.EtlStructureMonitorTask;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.repository.WorkdayRepository;
import ru.rbt.barsgl.ejbcore.mapping.job.CalendarJob;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.barsgl.shared.enums.ProcessingStatus;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import static ru.rbt.barsgl.ejb.common.CommonConstants.ETL_MONITOR_TASK;
import static ru.rbt.barsgl.ejb.entity.etl.EtlPackage.PackageState.LOADED;
import static ru.rbt.barsgl.ejb.entity.etl.EtlPackage.PackageState.PROCESSED;

/**
 * Created by Ivan Sevastyanov
 * Обработка пакетов входных сообщений из АЕ
 * @fsd 4
 */
public class EtlStructureMonitorIT extends AbstractTimerJobIT {

    private static final Logger log = Logger.getLogger(EtlStructureMonitorIT.class.getName());

    @Before
    public void beforeClass() {
        initCorrectOperday();
    }

    /**
     * Выполнение задачи обработки входного пакета, содержащего проводки
     * @fsd 4.2
     * @throws Exception
     */
    @Test
    public void etlStructureMonitorTaskTestNew() throws Exception {
        Long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackageNotSaved(stamp, "Тестовый пакет " + stamp);
        pkg.setDateLoad(getSystemDateTime());
        pkg.setPackageState(EtlPackage.PackageState.INPROGRESS);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.save(pkg);

        EtlPosting pst3 = newPosting(stamp, pkg);
        pst3.setAccountCredit("20202810900010237186");
        pst3.setAccountDebit("20202840000010609639");
        pst3.setAmountCredit(new BigDecimal("61.950"));
        pst3.setAmountDebit(new BigDecimal("1.000"));
        pst3.setCurrencyCredit(BankCurrency.RUB);
        pst3.setCurrencyDebit(BankCurrency.USD);
        pst3.setValueDate(getOperday().getCurrentDate());
        pst3.setDealId("OK1603250092");
        pst3.setSourcePosting("IMB-CA");
        pst3.setParentReference("0092");
        pst3 = (EtlPosting) baseEntityRepository.save(pst3);

        CalendarJob etlMonitor = (CalendarJob) baseEntityRepository.selectOne(CalendarJob.class
            , "from CalendarJob j where j.name = ?1", ETL_MONITOR_TASK);
        jobService.executeJob(etlMonitor);
        GLOperation oper3 = getOperation(pst3.getId());
        Assert.assertNull(oper3);

        pkg.setPackageState(LOADED);

        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        jobService.executeJob(etlMonitor);

        pkg = (EtlPackage) baseEntityRepository.findById(EtlPackage.class, pkg.getId());
        Assert.assertEquals(PROCESSED, pkg.getPackageState());

        oper3 = getOperation(pst3.getId());
        Assert.assertNotNull(oper3);
        Assert.assertEquals(OperState.POST, oper3.getState());
    }

    @Test
    @Ignore // странный тест, был заигнорен  раньше
    public void etlByPkgId()throws Exception {
        EtlPackage pkg = (EtlPackage) baseEntityRepository.findById(EtlPackage.class, 4647l);
        pkg.setPackageState(LOADED);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst = (EtlPosting) baseEntityRepository.findById(EtlPosting.class, 297797l);
        pst.setEtlPackage(pkg);
        pst.setErrorCode(null);
        pst.setErrorMessage(null);
        baseEntityRepository.update(pst);

        CalendarJob etlMonitor = (CalendarJob) baseEntityRepository.selectOne(CalendarJob.class
                , "from CalendarJob j where j.name = ?1", ETL_MONITOR_TASK);
        jobService.executeJob(etlMonitor);

        pkg = (EtlPackage) baseEntityRepository.findById(EtlPackage.class, pkg.getId());
        Assert.assertEquals(PROCESSED, pkg.getPackageState());
        GLOperation oper = getLastOperation(pst.getId());
        Assert.assertNotNull(oper);
        Assert.assertEquals(OperState.POST, oper.getState());
    }
    /**
     * Выполнение задачи обработки входного пакета, содержащего проводки и счета
     * @fsd 4.2
     * @throws Exception
     */
    @Test
    public void etlStructureMonitorTaskTestAll() throws Exception {
        Long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackageNotSaved(stamp, "Тестовый пакет " + stamp);
        pkg.setDateLoad(getSystemDateTime());
        pkg.setPackageState(EtlPackage.PackageState.INPROGRESS);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.save(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAccountCredit("40817036200012959997");
        pst1.setAccountDebit("40817036250010000018");
        pst1.setAmountCredit(new BigDecimal("12.0056"));
        pst1.setAmountDebit(pst1.getAmountCredit());
        pst1.setCurrencyCredit(BankCurrency.AUD);
        pst1.setCurrencyDebit(pst1.getCurrencyCredit());
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        EtlPosting pst2 = newPosting(stamp + 1, pkg);
        pst2.setAccountCredit("40817036200012959997");
        pst2.setAccountDebit("40817036250010000018");
        pst2.setAmountCredit(new BigDecimal("12.0056"));
        pst2.setAmountDebit(pst2.getAmountCredit());
        pst2.setCurrencyCredit(BankCurrency.AUD);
        pst2.setCurrencyDebit(pst2.getCurrencyCredit());
        pst2.setValueDate(getOperday().getCurrentDate());
        pst2 = (EtlPosting) baseEntityRepository.save(pst2);

        CalendarJob etlMonitor = (CalendarJob) baseEntityRepository.selectOne(CalendarJob.class
                , "from CalendarJob j where j.name = ?1", ETL_MONITOR_TASK);
        jobService.executeJob(etlMonitor);

        GLOperation oper1 = getOperation(pst1.getId());
        Assert.assertNull(oper1);
        GLOperation oper2 = getOperation(pst2.getId());
        Assert.assertNull(oper2);

        pkg.setPackageState(LOADED);

        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        jobService.executeJob(etlMonitor);

        pkg = (EtlPackage) baseEntityRepository.findById(EtlPackage.class, pkg.getId());
        Assert.assertEquals(PROCESSED, pkg.getPackageState());

        oper1 = getOperation(pst1.getId());
        Assert.assertNotNull(oper1);
        Assert.assertEquals(OperState.POST, oper1.getState());
        oper2 = getOperation(pst2.getId());
        Assert.assertEquals(OperState.POST, oper2.getState());
        Assert.assertNotNull(oper2);
    }

    /**
     * Выполнение задачи обработки входного пакета, содержащего проводки
     * @fsd 4.2
     * @throws Exception
     */
    @Test
    public void etlStructureMonitorTaskTestPosting() throws Exception {
        baseEntityRepository.executeUpdate("update EtlPackage p set p.packageState = ?1 where p.packageState = ?2"
                , EtlPackage.PackageState.ERROR, EtlPackage.PackageState.LOADED);
        Long stamp = System.currentTimeMillis();
        EtlPackage pkg = newPackageNotSaved(stamp, "Тестовый пакет " + stamp);
        pkg.setDateLoad(getSystemDateTime());
        pkg.setPackageState(EtlPackage.PackageState.INPROGRESS);
        pkg.setAccountCnt(0);
        pkg.setPostingCnt(4);
        pkg = (EtlPackage) baseEntityRepository.save(pkg);

        final String accCredit = "40817036200012959997";
        final String accDebit = "40817036250010000018";

        int cnt = baseEntityRepository.executeNativeUpdate("delete from GL_BSACCLK where BSAACID = ?", accCredit);
        log.info("cnt deleted cr: " + cnt);
        cnt = baseEntityRepository.executeNativeUpdate("delete from GL_BSACCLK where BSAACID = ?", accDebit);
        log.info("cnt deleted dt: " + cnt);

        EtlPosting pst1 = createSimple(stamp, pkg, accCredit, accDebit);
        EtlPosting pst2 = createSimple(stamp, pkg, accCredit, accDebit);;
        EtlPosting pst3 = createSimple(stamp, pkg, accCredit, accDebit);;
        EtlPosting pst4 = createSimple(stamp, pkg, accCredit, accDebit);;
        EtlPosting pst5 = createSimple(stamp, pkg, accCredit, accDebit);;
        EtlPosting pst6 = createSimple(stamp, pkg, accCredit, accDebit);;
        EtlPosting pst7 = createSimple(stamp, pkg, accCredit, accDebit);;
        EtlPosting pst8 = createSimple(stamp, pkg, accCredit, accDebit);;

        CalendarJob etlMonitor = (CalendarJob) baseEntityRepository.selectOne(CalendarJob.class
                , "from CalendarJob j where j.name = ?1", ETL_MONITOR_TASK);
        GLOperation oper1 = getOperation(pst1.getId());
        Assert.assertNull(oper1);
        GLOperation oper2 = getOperation(pst2.getId());
        Assert.assertNull(oper2);
        GLOperation oper3 = getOperation(pst3.getId());
        Assert.assertNull(oper3);
        GLOperation oper4 = getOperation(pst4.getId());
        Assert.assertNull(oper4);
        GLOperation oper5 = getOperation(pst5.getId());
        Assert.assertNull(oper5);
        GLOperation oper6 = getOperation(pst6.getId());
        Assert.assertNull(oper6);
        GLOperation oper7 = getOperation(pst7.getId());
        Assert.assertNull(oper7);
        GLOperation oper8 = getOperation(pst4.getId());
        Assert.assertNull(oper8);

        pkg.setPackageState(LOADED);

        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        jobService.executeJob(etlMonitor);

        pkg = (EtlPackage) baseEntityRepository.findById(EtlPackage.class, pkg.getId());
        Assert.assertEquals(PROCESSED, pkg.getPackageState());

        oper1 = getOperation(pst1.getId());
        Assert.assertNotNull(oper1);
        Assert.assertEquals(OperState.POST, oper1.getState());

        oper2 = getOperation(pst2.getId());
        Assert.assertEquals(oper2.getId() + "", OperState.POST, oper2.getState());
        Assert.assertNotNull(oper2);

        oper3 = getOperation(pst3.getId());
        Assert.assertNotNull(oper3);
        Assert.assertEquals(oper3.getId() + "", OperState.POST, oper3.getState());

        oper4 = getOperation(pst4.getId());
        Assert.assertNotNull(oper4);
        Assert.assertEquals(oper4.getId() + "", OperState.POST, oper4.getState());

        oper5 = getOperation(pst5.getId());
        Assert.assertNotNull(oper5);
        Assert.assertEquals(oper5.getId() +"", OperState.POST, oper5.getState());

        oper6 = getOperation(pst6.getId());
        Assert.assertNotNull(oper6);
        Assert.assertEquals(oper6.getId() +"", OperState.POST, oper6.getState());

        oper7 = getOperation(pst7.getId());
        Assert.assertNotNull(oper7);
        Assert.assertEquals(oper7.getId() +"", OperState.POST, oper7.getState());

        oper8 = getOperation(pst8.getId());
        Assert.assertNotNull(oper8);
        Assert.assertEquals(oper8.getId() +"", OperState.POST, oper8.getState());

    }

    @Test
    public void test100() throws Exception {

        final int postingsCount = 200;
        baseEntityRepository.executeUpdate("update EtlPackage p set p.packageState = ?1 where p.packageState = ?2"
            , EtlPackage.PackageState.ERROR, EtlPackage.PackageState.LOADED);

        Long stamp = System.currentTimeMillis();
        EtlPackage pkg = newPackageNotSaved(stamp, "Тестовый пакет 100" + stamp);
        pkg.setDateLoad(getSystemDateTime());
        pkg.setPackageState(EtlPackage.PackageState.INPROGRESS);
        pkg.setAccountCnt(0);
        pkg.setPostingCnt(4);
        pkg = (EtlPackage) baseEntityRepository.save(pkg);

        final String accCredit = "40817036200012959997";
        final String accDebit = "40817036250010000018";

        int cnt = baseEntityRepository.executeNativeUpdate("delete from GL_BSACCLK where BSAACID = ?", accCredit);
        log.info("cnt deleted cr: " + cnt);
        cnt = baseEntityRepository.executeNativeUpdate("delete from GL_BSACCLK where BSAACID = ?", accDebit);
        log.info("cnt deleted dt: " + cnt);

        for (int i = 0; i < postingsCount; i++) {
            createSimple(stamp, pkg, accCredit, accDebit);
        }
        pkg.setPackageState(LOADED);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        log.info("Package has built");

        CalendarJob etlMonitor = (CalendarJob) baseEntityRepository.selectOne(CalendarJob.class
                , "from CalendarJob j where j.name = ?1", ETL_MONITOR_TASK);
        log.info("Start processing");
        jobService.executeJob(etlMonitor);

        pkg = (EtlPackage) baseEntityRepository.findById(EtlPackage.class, pkg.getId());
        Assert.assertEquals(PROCESSED, pkg.getPackageState());

        List<GLOperation> opers = baseEntityRepository.findNative(GLOperation.class
                , "select * from gl_oper o where exists (select 1 from gl_etlpst p where o.id_pst = p.id_pst and p.id_pkg = ?)", postingsCount
                , pkg.getId());
        Assert.assertEquals(postingsCount, opers.size());
        Assert.assertTrue(opers.stream().allMatch(oper -> oper.getState() == OperState.POST));
        log.info("Processing has completed");
    }

    private EtlPosting createSimple(long stamp, EtlPackage pkg, String accCredit, String accDebit) {
        EtlPosting pst = newPosting(stamp + 1, pkg);
        pst.setAccountCredit(accCredit);
        pst.setAccountDebit(accDebit);
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());
        pst.setValueDate(getOperday().getCurrentDate());
        pst.setSourcePosting("K+TP");
        return (EtlPosting) baseEntityRepository.save(pst);
    }

    /**
     * Выполнение задачи обработки входного пакета, содержащего проводки с ошибками
     * @fsd 4.2
     * @throws Exception
     */
    @Test
    public void etlStructureMonitorTaskTestErrorPosting() throws Exception {
        Long stamp = System.currentTimeMillis();
        EtlPackage pkg = newPackageNotSaved(stamp, "Тестовый пакет " + stamp);
        pkg.setDateLoad(getSystemDateTime());
        pkg.setPackageState(EtlPackage.PackageState.INPROGRESS);
        pkg = (EtlPackage) baseEntityRepository.save(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAccountCredit("00000036000000000000");
        pst1.setAccountDebit("40817036250010000018");
        pst1.setAmountCredit(new BigDecimal("12.0056"));
        pst1.setAmountDebit(pst1.getAmountCredit());
        pst1.setCurrencyCredit(BankCurrency.AUD);
        pst1.setCurrencyDebit(pst1.getCurrencyCredit());
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        EtlPosting pst2 = newPosting(stamp + 1, pkg);
        pst2.setAccountCredit("40817036200012959997");
        pst2.setAccountDebit("40817036250010000018");
        pst2.setAmountCredit(new BigDecimal("12.0056"));
        pst2.setAmountDebit(pst2.getAmountCredit());
        pst2.setCurrencyCredit(BankCurrency.AUD);
        pst2.setCurrencyDebit(pst2.getCurrencyCredit());
        pst2.setValueDate(getOperday().getCurrentDate());
        pst2 = (EtlPosting) baseEntityRepository.save(pst2);

        CalendarJob etlMonitor = (CalendarJob) baseEntityRepository.selectOne(CalendarJob.class
                , "from CalendarJob j where j.name = ?1", ETL_MONITOR_TASK);
        jobService.executeJob(etlMonitor);
        GLOperation oper1 = getOperation(pst1.getId());
        Assert.assertNull(oper1);
        GLOperation oper2 = getOperation(pst2.getId());
        Assert.assertNull(oper2);

        pkg.setPackageState(LOADED);

        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        jobService.executeJob(etlMonitor);

        pkg = (EtlPackage) baseEntityRepository.findById(EtlPackage.class, pkg.getId());
        Assert.assertEquals(PROCESSED, pkg.getPackageState());


        oper1 = getOperation(pst1.getId());
        Assert.assertNotNull(oper1);
        Assert.assertEquals(OperState.WTAC, oper1.getState());
        oper2 = getOperation(pst2.getId());
        Assert.assertEquals(OperState.POST, oper2.getState());
        Assert.assertNotNull(oper2);
    }

    @Test
    public void testCheckAvailablePrcessing() {
        Date operdayMidas = remoteAccess.invoke(WorkdayRepository.class, "getWorkday");
        setOperday(operdayMidas
                , org.apache.commons.lang3.time.DateUtils.addDays(operdayMidas, -1), Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
        Assert.assertFalse(remoteAccess.invoke(EtlStructureMonitorTask.class, "checkOperdayState"));

        setOperday(org.apache.commons.lang3.time.DateUtils.addDays(operdayMidas, 1)
                , operdayMidas, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
        Assert.assertTrue(remoteAccess.invoke(EtlStructureMonitorTask.class, "checkOperdayState"));

        setOperday(operdayMidas
                , org.apache.commons.lang3.time.DateUtils.addDays(operdayMidas, -1)
                , Operday.OperdayPhase.COB, Operday.LastWorkdayStatus.CLOSED);
        Assert.assertFalse(remoteAccess.invoke(EtlStructureMonitorTask.class, "checkOperdayState"));
    }

    private GLOperation getOperation(Long idpst) {
        return (GLOperation) baseEntityRepository.selectFirst(GLOperation.class, "from GLOperation o where o.etlPostingRef = ?1", idpst);
    }

    private GLAccount getAccount(String bsaAcid) {
        return (GLAccount) baseEntityRepository.selectFirst(GLAccount.class, "from GLAccount a where a.bsaAcid = ?1", bsaAcid);
    }

    @Test
    public void testIsProcessingAllowed() throws Exception {
        Long stamp = System.currentTimeMillis();
        EtlPackage pkg = newPackageNotSaved(stamp, "Тестовый пакет " + stamp);
        pkg.setDateLoad(getSystemDateTime());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.save(pkg);

        remoteAccess.invoke(OperdayController.class, "setProcessingStatus", ProcessingStatus.REQUIRED);
        SingleActionJob etlProcess = SingleActionJobBuilder.create()
                .withClass(EtlStructureMonitorTask.class).build();
        jobService.executeJob(etlProcess);

        pkg = (EtlPackage) baseEntityRepository.findById(EtlPackage.class, pkg.getId());
        Assert.assertEquals(pkg.getPackageState(), LOADED);
        Assert.assertEquals(ProcessingStatus.STOPPED, getOperday().getProcessingStatus());

        remoteAccess.invoke(OperdayController.class, "setProcessingStatus", ProcessingStatus.ALLOWED);
        jobService.executeJob(etlProcess);
        pkg = (EtlPackage) baseEntityRepository.findById(EtlPackage.class, pkg.getId());
        Assert.assertEquals(pkg.getPackageState(), PROCESSED);
        Assert.assertEquals(ProcessingStatus.STARTED, getOperday().getProcessingStatus());
    }
}
