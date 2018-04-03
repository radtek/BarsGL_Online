package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.od.OperdaySynchronizationController;
import ru.rbt.barsgl.ejb.controller.operday.task.PdSyncTask;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.ejb.controller.operday.task.stamt.SyncStamtBackvalueTask;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.*;
import ru.rbt.barsgl.ejb.entity.lg.LongRunningTaskStep;
import ru.rbt.barsgl.ejb.repository.WorkprocRepository;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.barsgl.shared.enums.ProcessingStatus;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.tasks.ejb.entity.task.JobHistory;
import ru.rbt.tasks.ejb.repository.JobHistoryRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.CLOSED;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.COB;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.BUFFER;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.DIRECT;
import static ru.rbt.barsgl.ejb.controller.operday.task.OpenOperdayTask.BALANCE_MODE_KEY;
import static ru.rbt.barsgl.ejb.controller.operday.task.stamt.UnloadStamtParams.BALANCE_DELTA_INCR;
import static ru.rbt.barsgl.shared.enums.DealSource.KondorPlus;

/**
 * Created by Ivan Sevastyanov on 05.02.2016.
 * Тестирование обработки проводок в режиме "BUFFER"
 */
public class BufferModeIT extends AbstractRemoteIT {

    private static final Logger log = Logger.getLogger(BufferModeIT.class.getName());

    @BeforeClass
    public static void init() throws ParseException {
        Date curDate = DateUtils.parseDate("2015-02-26", "yyyy-MM-dd");
        setOperday(curDate, DateUtils.addDays(curDate, -1), Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
        initCorrectOperday();
    }

    @Before
    public void before() {
        baseEntityRepository.executeNativeUpdate("delete from gl_baltur");
        baseEntityRepository.executeNativeUpdate("delete from gl_pd");
    }

    /**
     * проверка корректности работы в режиме BUFFER и правильности переноса в PD и BALTUR
     * @throws Exception
     */
    @Test
    public void test() throws Exception {
        Operday operday = getOperday();
        setOperday(operday.getCurrentDate(), operday.getLastWorkingDay()
                , ONLINE, OPEN, BUFFER);

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
        Assert.assertTrue(postList.isEmpty());                 //  нет проводок, только буфер

        List<GLPd> pdList = getGLPostingPd(operation);
        GLPd pdDr = Utl4Tests.findDebitPd(pdList);
        GLPd pdCr = Utl4Tests.findCreditPd(pdList);
        Assert.assertTrue(pdDr.getCcy().equals(operation.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит
        Assert.assertTrue(pdCr.getCcy().equals(pdDr.getCcy()));                 // валюта одинаковый
        Assert.assertEquals(StringUtils.leftPad(pst.getDealId(), 6, "0"), pdDr.getPref());
        Assert.assertEquals(StringUtils.leftPad(pst.getDealId(), 6, "0"), pdCr.getPref());

        Assert.assertTrue(pdCr.getAmount() == operation.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCr.getAmount() == -pdDr.getAmount());       // сумма в валюте дебет - кредит

        // тестируем устойчивость к пересечению MO_NO
        createForPcidMo();

        remoteAccess.invoke(OperdaySynchronizationController.class, "syncPostings", Operday.BalanceMode.NOCHANGE);

        List<GLPosting> postList1 = getPostings(operation);
        Assert.assertEquals(1, postList1.size());

        List<Pd> pdList1 = getPostingPd(postList1.get(0));
        Pd pdDr1 = pdList1.get(0);
        Pd pdCr1 = pdList1.get(1);
        Assert.assertTrue(pdDr1.getCcy().equals(operation.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr1.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит
        Assert.assertTrue(pdCr1.getCcy().equals(pdDr1.getCcy()));                 // валюта одинаковый
        Assert.assertEquals(StringUtils.leftPad(pst.getDealId(), 6, "0"), pdDr1.getPref());
        Assert.assertEquals(StringUtils.leftPad(pst.getDealId(), 6, "0"), pdCr1.getPref());

        Assert.assertTrue(pdCr1.getAmount() == operation.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCr1.getAmount() == -pdDr1.getAmount());       // сумма в валюте дебет - кредит

        Assert.assertNotNull(baseEntityRepository.select(Memorder.class, "from Memorder m where m.id = ?1", pdDr1.getPcId()));

        Assert.assertNotNull(baseEntityRepository.selectFirst("select 1 from gl_pd where pd_id = ?", pdDr1.getId()));
        Assert.assertNotNull(baseEntityRepository.selectFirst("select 1 from gl_pd where pd_id = ?", pdCr1.getId()));

        DataRecord record = baseEntityRepository.selectFirst("select * from gl_baltur where bsaacid = ?", pdDr1.getBsaAcid());
        Assert.assertNotNull(record);
        Assert.assertEquals("Y", record.getString("moved"));
        record = baseEntityRepository.selectFirst("select * from gl_baltur where bsaacid = ?", pdCr1.getBsaAcid());
        Assert.assertNotNull(record);
        Assert.assertEquals("Y", record.getString("moved"));

        Assert.assertTrue(2 == (Integer) remoteAccess.invoke(OperdaySynchronizationController.class, "moveGLPdsToHistory", getOperday().getCurrentDate()));

        Assert.assertEquals(0, baseEntityRepository.selectOne("select count(1) cnt from gl_pd").getInteger("cnt").intValue());
        Assert.assertEquals(0, baseEntityRepository.selectOne("select count(1) cnt from gl_baltur").getInteger("cnt").intValue());

    }

    /**
     * Проверка корректности выполнения синхронизации через запуск задачи
     * @throws Exception
     */
    @Test
    public void testJob() throws Exception {
        Operday operday = getOperday();
        setOperday(operday.getCurrentDate(), operday.getLastWorkingDay()
                , ONLINE, OPEN, BUFFER);

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
        Assert.assertTrue(postList.isEmpty());                 //  нет проводок, только буфер

        List<GLPd> pdList = getGLPostingPd(operation);
        GLPd pdDr = Utl4Tests.findDebitPd(pdList);
        GLPd pdCr = Utl4Tests.findCreditPd(pdList);
        Assert.assertTrue(pdDr.getCcy().equals(operation.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит
        Assert.assertTrue(pdCr.getCcy().equals(pdDr.getCcy()));                 // валюта одинаковый
        Assert.assertEquals(StringUtils.leftPad(pst.getDealId(), 6, "0"), pdDr.getPref());
        Assert.assertEquals(StringUtils.leftPad(pst.getDealId(), 6, "0"), pdCr.getPref());

        Assert.assertTrue(pdCr.getAmount() == operation.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCr.getAmount() == -pdDr.getAmount());       // сумма в валюте дебет - кредит

        baseEntityRepository.executeNativeUpdate("update gl_od set prc = 'STOPPED'");

        SingleActionJob syncJob = SingleActionJobBuilder.create().withClass(PdSyncTask.class).withName("SyncAct1").build();
        jobService.executeJob(syncJob);

        List<GLPosting> postList1 = getPostings(operation);
        Assert.assertEquals(1, postList1.size());

        List<Pd> pdList1 = getPostingPd(postList1.get(0));
        Pd pdDr1 = pdList1.get(0);
        Pd pdCr1 = pdList1.get(1);
        Assert.assertTrue(pdDr1.getCcy().equals(operation.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr1.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит
        Assert.assertTrue(pdCr1.getCcy().equals(pdDr1.getCcy()));                 // валюта одинаковый
        Assert.assertEquals(StringUtils.leftPad(pst.getDealId(), 6, "0"), pdDr1.getPref());
        Assert.assertEquals(StringUtils.leftPad(pst.getDealId(), 6, "0"), pdCr1.getPref());

        Assert.assertTrue(pdCr1.getAmount() == operation.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCr1.getAmount() == -pdDr1.getAmount());       // сумма в валюте дебет - кредит

        Assert.assertNotNull(baseEntityRepository.select(Memorder.class, "from Memorder m where m.id = ?1", pdDr1.getPcId()));

    }

    /**
     * Проверка корректности выполнения промежуточной синхронизации backvalue
     * и инкрементальной выгрузки в STAMT
     * @throws Exception
     */
    @Test
    public void testSyncIncrJob() throws Exception {
        log.info("starting test testSyncIncrJob" );
        baseEntityRepository.executeNativeUpdate("delete from gl_etlstms");
        baseEntityRepository.executeNativeUpdate("delete from gl_balstmd");
        baseEntityRepository.executeNativeUpdate("update gl_baltur set moved = 'N'");
        initCorrectOperday();
        Operday operday = getOperday();
        setOperday(operday.getCurrentDate(), operday.getLastWorkingDay()
                , ONLINE, OPEN, BUFFER);

        operday = getOperday();
        log.info("updated = " + baseEntityRepository.executeNativeUpdate("update gl_oper o set procdate = ? where o.procdate = ? and o.postdate < o.procdate"
                , DateUtils.addDays(operday.getCurrentDate(), -5), operday.getCurrentDate()));

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        // !!! проводки бэквалуе
        pst.setValueDate(getOperday().getLastWorkingDay());

        pst.setAccountCredit("40817036200012959997");
        pst.setAccountDebit("40817036250010000018");
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
        Assert.assertTrue(postList.isEmpty());                 //  нет проводок, только буфер

        List<GLPd> pdList = getGLPostingPd(operation);
        GLPd pdDr = Utl4Tests.findDebitPd(pdList);
        GLPd pdCr = Utl4Tests.findCreditPd(pdList);
        Assert.assertTrue(pdDr.getCcy().equals(operation.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит
        Assert.assertTrue(pdCr.getCcy().equals(pdDr.getCcy()));                 // валюта одинаковый
        Assert.assertEquals(StringUtils.leftPad(pst.getDealId(), 6, "0"), pdDr.getPref());
        Assert.assertEquals(StringUtils.leftPad(pst.getDealId(), 6, "0"), pdCr.getPref());

        Assert.assertTrue(pdCr.getAmount() == operation.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCr.getAmount() == -pdDr.getAmount());       // сумма в валюте дебет - кредит

        incldeBs2ByBufferOperation(operation);


        baseEntityRepository.executeNativeUpdate("delete from gl_sched_h");

        baseEntityRepository.executeNativeUpdate("update gl_od set prc = ?", ProcessingStatus.STOPPED.name());

        final String stepName = "WT_1";
        checkCreateStep(stepName, getOperday().getLastWorkingDay(), WorkprocRepository.WorkprocState.W.getValue());

        final String jobName = "IncSync1";

        execSyncStamtBackvalue(stepName, jobName);

        Assert.assertEquals(DwhUnloadStatus.SUCCEDED.getFlag()
                , getLastUnloadHeader(BALANCE_DELTA_INCR).getString("parvalue"));

        Assert.assertTrue(1 <= baseEntityRepository.selectFirst("select count(1) cnt from gl_balstmd").getInteger("cnt"));

        DataRecord lastHist = getLastHistRecord(jobName);
        Assert.assertNotNull(lastHist);
        Assert.assertEquals("1", lastHist.getString("SCHRSLT"));

        Assert.assertTrue(checkStepOk(stepName, operday.getLastWorkingDay()));

        List<GLPosting> postList1 = getPostings(operation);
        Assert.assertEquals(1, postList1.size());

        List<Pd> pdList1 = getPostingPd(postList1.get(0));
        Pd pdDr1 = pdList1.get(0);
        Pd pdCr1 = pdList1.get(1);
        Assert.assertTrue(pdDr1.getCcy().equals(operation.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr1.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит
        Assert.assertTrue(pdCr1.getCcy().equals(pdDr1.getCcy()));                 // валюта одинаковый
        Assert.assertEquals(StringUtils.leftPad(pst.getDealId(), 6, "0"), pdDr1.getPref());
        Assert.assertEquals(StringUtils.leftPad(pst.getDealId(), 6, "0"), pdCr1.getPref());

        Assert.assertTrue(pdCr1.getAmount() == operation.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCr1.getAmount() == -pdDr1.getAmount());       // сумма в валюте дебет - кредит

        Assert.assertNotNull(baseEntityRepository.select(Memorder.class, "from Memorder m where m.id = ?1", pdDr1.getPcId()));

        long pcid = getPcid(operation);
        List<DataRecord> bvrecs = baseEntityRepository.select("select * from gl_etlstmd");
        log.info(format("Finding PCID '%s' gl_etlstmd list size %s <%s>"
                , pcid, bvrecs.size(), bvrecs.stream().map(r -> "'" +r.getString("pcid") + "'")
                .collect(Collectors.joining(","))));
        List<DataRecord> filtered = bvrecs.stream().filter(r -> r.getLong("pcid") == pcid).collect(Collectors.toList());
        Assert.assertEquals(1, filtered.size());

        /*TODO final String accDt = operation.getAccountDebit();
        Assert.assertTrue(baseEntityRepository.select("select * from GL_BALSTMD").stream()
                .anyMatch(p -> ((DataRecord)p).getString("cbaccount").equals(accDt)));*/

        updateOperday(COB, CLOSED);
        checkCreateStep(stepName, getOperday().getCurrentDate(), WorkprocRepository.WorkprocState.W.getValue());
        Assert.assertFalse(checkStepOk(stepName, getOperday().getCurrentDate()));
        baseEntityRepository.executeNativeUpdate("update gl_etlstms set parvalue = '4'");
        execSyncStamtBackvalue(stepName, jobName);
        Assert.assertTrue(checkStepOk(stepName, getOperday().getCurrentDate()));
    }

    /**
     * В случае обработки проводок в процессе синхронизации запрещено обновлять перенесенные обороты
     */
    @Test public void testErrpost() {

        Operday operday = getOperday();
        setOperday(operday.getCurrentDate(), operday.getLastWorkingDay()
                , ONLINE, OPEN, BUFFER);

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
        Assert.assertTrue(postList.isEmpty());                 //  нет проводок, только буфер

        List<GLPd> pdList = getGLPostingPd(operation);
        Assert.assertTrue(!pdList.isEmpty());

        // метим как перенесенные
        Assert.assertTrue(0 < baseEntityRepository.executeNativeUpdate("update gl_baltur set moved = 'Y'"));
        // запускаем еще раз обработку
        operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.ERPOST, operation.getState());
        Assert.assertTrue(operation.getErrorMessage().contains("is after synchronization"));
    }

    /**
     * проверка синхронизации с помощью задачи
     * @throws Exception
     */
    @Test
    public void testPdSyncTaskTest() throws Exception {
        Operday operday = getOperday();
        setOperday(operday.getCurrentDate(), operday.getLastWorkingDay()
                , ONLINE, OPEN, BUFFER);

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
        Assert.assertTrue(postList.isEmpty());                 //  нет проводок, только буфер

        List<GLPd> pdList = getGLPostingPd(operation);
        GLPd pdDr = Utl4Tests.findDebitPd(pdList);
        GLPd pdCr = Utl4Tests.findCreditPd(pdList);
        Assert.assertTrue(pdDr.getCcy().equals(operation.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит
        Assert.assertTrue(pdCr.getCcy().equals(pdDr.getCcy()));                 // валюта одинаковый
        Assert.assertEquals(StringUtils.leftPad(pst.getDealId(), 6, "0"), pdDr.getPref());
        Assert.assertEquals(StringUtils.leftPad(pst.getDealId(), 6, "0"), pdCr.getPref());

        Assert.assertTrue(pdCr.getAmount() == operation.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCr.getAmount() == -pdDr.getAmount());       // сумма в валюте дебет - кредит

        baseEntityRepository.executeNativeUpdate("update gl_od set prc = 'STOPPED'");
        baseEntityRepository.executeNativeUpdate("delete from workproc where dat = ?", operday.getLastWorkingDay());
        baseEntityRepository.executeNativeUpdate("insert into workproc (dat,id,result,count,msg) values (?,?,?,?,?)"
                , operday.getLastWorkingDay(), "IFLEX","O", "0", "MI5GL");

        log.info("deleted " + baseEntityRepository.executeUpdate("delete from JobHistory h where h.jobName like ?1", PdSyncTask.class.getSimpleName() + "%"));
        JobHistory history = remoteAccess.invoke(JobHistoryRepository.class
                , "createHeader", PdSyncTask.class.getSimpleName(), getOperday().getCurrentDate());

        SingleActionJob syncJob = SingleActionJobBuilder.create()
                .withProps(AbstractJobHistoryAwareTask.JobHistoryContext.HISTORY_ID.name() + "=" + history.getId())
                .withClass(PdSyncTask.class)
                .withName("PdSyncTask001").build();
        jobService.executeJob(syncJob);

        List<GLPosting> postList1 = getPostings(operation);
        Assert.assertEquals(1, postList1.size());

        List<Pd> pdList1 = getPostingPd(postList1.get(0));
        Pd pdDr1 = pdList1.get(0);
        Pd pdCr1 = pdList1.get(1);
        Assert.assertTrue(pdDr1.getCcy().equals(operation.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr1.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит
        Assert.assertTrue(pdCr1.getCcy().equals(pdDr1.getCcy()));                 // валюта одинаковый
        Assert.assertEquals(StringUtils.leftPad(pst.getDealId(), 6, "0"), pdDr1.getPref());
        Assert.assertEquals(StringUtils.leftPad(pst.getDealId(), 6, "0"), pdCr1.getPref());

        Assert.assertTrue(pdCr1.getAmount() == operation.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCr1.getAmount() == -pdDr1.getAmount());       // сумма в валюте дебет - кредит

        Assert.assertNotNull(baseEntityRepository.select(Memorder.class, "from Memorder m where m.id = ?1", pdDr1.getPcId()));

        operday = getOperday();
        Assert.assertEquals(DIRECT, operday.getPdMode());

        List<LongRunningTaskStep> steps = baseEntityRepository
                .select(LongRunningTaskStep.class, "from LongRunningTaskStep s where s.id.idHistory = ?1", history.getId());
        Assert.assertEquals(7, steps.size());
        Assert.assertTrue(steps.stream().allMatch(s -> s.getEndDate() != null && s.isSuccess()));

        history = (JobHistory) baseEntityRepository.selectFirst(JobHistory.class, "from JobHistory h where h.id = ?1", history.getId());
        Assert.assertNotNull(history);
        Assert.assertEquals(DwhUnloadStatus.SUCCEDED, history.getResult());
    }

    @Test public void testPdSyncTaskBalanceMode() throws Exception {
        updateOperday(ONLINE, OPEN, BUFFER);
        setOnlineBalanceMode();
        baseEntityRepository.executeNativeUpdate("update gl_od set prc = 'STOPPED'");

        processOneOperation();
        SingleActionJob pdSyncTask = SingleActionJobBuilder.create().withClass(PdSyncTask.class)
                .withProps(BALANCE_MODE_KEY+"="+Operday.BalanceMode.GIBRID).build();
        jobService.executeJob(pdSyncTask);
        checkCurrentBalanceMode(Operday.BalanceMode.GIBRID);

        updateOperday(ONLINE, OPEN, BUFFER);
        processOneOperation();
        SingleActionJob pdSyncTask2 = SingleActionJobBuilder.create().withClass(PdSyncTask.class)
                .withProps(BALANCE_MODE_KEY+"="+Operday.BalanceMode.ONLINE).build();
        jobService.executeJob(pdSyncTask2);
        checkCurrentBalanceMode(Operday.BalanceMode.ONLINE);
    }

    private void incldeBs2ByBufferOperation(GLOperation operation) throws SQLException {
        baseEntityRepository.executeNativeUpdate("delete from GL_STMPARM");
        List<GLPd> pds = Utl4Tests.getGLPds(baseEntityRepository, operation);
        Assert.assertEquals(2, pds.size());
        for (GLPd pd : pds) {
            String acc = pd.getBsaAcid().substring(0,5);
            if (baseEntityRepository.selectFirst("select count(1) cnt from gl_stmparm where account = ?", acc).getLong("cnt") == 0) {
                baseEntityRepository.executeNativeUpdate("insert into gl_stmparm (account, INCLUDE,acctype, INCLUDEBLN) values (?, '1', 'B', '1')", acc);
            }
        }
    }

    private Long getPcid(GLOperation operation) throws SQLException {
        return baseEntityRepository.selectFirst("select pcid from gl_oper o, gl_posting p where o.gloid = p.glo_ref and o.gloid = ?"
                , operation.getId()).getLong("pcid");
    }

    private void execSyncStamtBackvalue(String stepName, String jobName) throws Exception {
        SingleActionJob incSync = SingleActionJobBuilder.create()
                .withClass(SyncStamtBackvalueTask.class).withProps("stepName=" + stepName).withName(jobName).build();
        jobService.executeJob(incSync);

    }

    private boolean checkStepOk(String stepName, Date operday) throws SQLException {
        DataRecord workproc = baseEntityRepository.selectFirst("select * from workproc where dat = ? and id = ?"
                , operday, stepName);
        Assert.assertNotNull(workproc);
        return  "O".equals(workproc.getString("RESULT"));
    }

    /**
     * генерируем пересечение номеров мемордеров с буфером
     */
    private void createForPcidMo() throws SQLException {

        DataRecord glPd = baseEntityRepository.selectFirst("select * from gl_pd where rownum < 2");
        Assert.assertNotNull(glPd);
        DataRecord pcidMo = baseEntityRepository.selectFirst("select * from pcid_mo m where pod = ? and mo_no = ?", glPd.getDate("pod"), glPd.getString("mo_no"));
        if (null == pcidMo) {
            pcidMo = baseEntityRepository.selectFirst("select * from pcid_mo m where rownum < 2");
            Assert.assertEquals(1, baseEntityRepository.executeNativeUpdate("update pcid_mo set pod = ?, mo_no = ? where pcid = ?", glPd.getDate("pod"), glPd.getString("mo_no"), pcidMo.getLong("pcid")));
        }

    }

    private static void processOneOperation() {
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
        pst.setSourcePosting(KondorPlus.getLabel());
        pst.setDealId("123");

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());

    }

}
