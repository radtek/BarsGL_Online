package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.*;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.common.mapping.od.BankCalendarDay;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.common.repository.od.OperdayRepository;
import ru.rbt.barsgl.ejb.controller.od.DatLCorrector;
import ru.rbt.barsgl.ejb.controller.operday.task.*;
import ru.rbt.barsgl.ejb.entity.acc.AccRlnId;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.dict.LwdBalanceCutView;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.repository.cob.CobStatRepository;
import ru.rbt.barsgl.ejb.repository.dict.LwdCutCachedRepository;
import ru.rbt.barsgl.ejbcore.mapping.job.CalendarJob;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbcore.mapping.job.TimerJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.barsgl.shared.enums.CobStepStatus;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.barsgl.shared.enums.ProcessingStatus;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.tasks.ejb.entity.task.JobHistory;
import ru.rbt.tasks.ejb.job.BackgroundJobsController;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static ru.rbt.barsgl.ejb.common.CommonConstants.ETL_MONITOR_TASK;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.CLOSED;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.COB;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.BUFFER;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.DIRECT;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.SystemAccessMode.FULL;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.SystemAccessMode.LIMIT;
import static ru.rbt.barsgl.ejb.controller.operday.task.OpenOperdayTask.*;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.USD;
import static ru.rbt.barsgl.ejbcore.mapping.job.TimerJob.JobState.STOPPED;
import static ru.rbt.barsgl.shared.enums.DealSource.KondorPlus;
import static ru.rbt.barsgl.shared.enums.JobStartupType.MANUAL;

/**
 * Created by Ivan Sevastyanov
 * Операционный день
 * @fsd 5.3.3
 */
public class OperdayIT extends AbstractTimerJobIT {

    public static final String FLEX_FINAL_STEP_NAME = "FLEX";

    private static final Logger logger = Logger.getLogger(OperdayIT.class.getName());

    @BeforeClass public static void beforeClass() {
        initCorrectOperday();
        baseEntityRepository.executeNativeUpdate("update gl_oper o set VDATE = VDATE - 20, POSTDATE = POSTDATE - 20 where o.procdate in (?,?)"
            , getOperday().getCurrentDate(), getOperday().getLastWorkingDay());
    }

    @AfterClass
    public static void afterClass() {
        stopProcessing();
    }

    @Before
    public void before() {
        initCorrectOperday();
        baseEntityRepository.executeNativeUpdate("update gl_etlpkg p set p.state = 'PROCESSED' where p.state not in ('PROCESSED', 'ERROR') ");
        baseEntityRepository.executeNativeUpdate("delete from gl_sched_h");
    }
    /**
     * Выполнение задачи открытия нового операционного дня
     * @fsd 5.3.5
     * @throws Exception
     */
    @Test
    public void testOpenOperday() throws Exception {

        Operday previosOperday = getOperday();
        updateOperday(COB, CLOSED);

        Date operdayToOpen = getOperDayToOpen(previosOperday.getCurrentDate());

        checkCreateBankCurrency(operdayToOpen);

        baseEntityRepository.executeNativeUpdate("delete from workproc where dat = ?", previosOperday.getCurrentDate());
        checkCreateFinalFlexStep(previosOperday.getCurrentDate());

        SingleActionJobBuilder openOperdayTaskBuilder = SingleActionJobBuilder.create()
                .withName(OpenOperdayTask.class.getSimpleName())
                .withClass(OpenOperdayTask.class).withProps(
                        FLEX_FINAL_STEP_KEY + "=" + FLEX_FINAL_STEP_NAME + "\n" +
                                OpenOperdayTask.PD_MODE_KEY + "=" +PD_MODE_DEFAULT);

        jobService.executeJob(openOperdayTaskBuilder.build());

        Operday newOperday = getOperday();
        Assert.assertFalse(newOperday.equals(previosOperday));
        Assert.assertEquals(newOperday.getCurrentDate(), operdayToOpen);
        Assert.assertEquals(newOperday.getLastWorkingDay(), previosOperday.getCurrentDate());
        Assert.assertEquals(newOperday.getLastWorkdayStatus(), Operday.LastWorkdayStatus.OPEN);
        Assert.assertEquals(newOperday.getPhase(), ONLINE);
        Assert.assertEquals(newOperday.getPdMode(), BUFFER);

        // переключнеие в режим DIRECT
        setOperday(previosOperday.getCurrentDate(), previosOperday.getLastWorkingDay(), COB, CLOSED, BUFFER);
        previosOperday = getOperday();
        Assert.assertEquals(COB, previosOperday.getPhase());
        Assert.assertEquals(BUFFER, previosOperday.getPdMode());

        openOperdayTaskBuilder = SingleActionJobBuilder.create()
                .withClass(OpenOperdayTask.class)
                .withName(OpenOperdayTask.class.getSimpleName())
                .withProps(FLEX_FINAL_STEP_KEY + "=" + FLEX_FINAL_STEP_NAME + "\n" +
                OpenOperdayTask.PD_MODE_KEY + "=" + DIRECT);

        baseEntityRepository.executeNativeUpdate("delete from gl_sched_h");
        jobService.executeJob(openOperdayTaskBuilder.build());

        newOperday = getOperday();
        Assert.assertEquals(ONLINE, newOperday.getPhase());
        Assert.assertEquals(DIRECT, newOperday.getPdMode());

        updateOperday(COB, CLOSED);

        checkCreateFinalFlexStep(getOperday().getCurrentDate());
        jobService.executeJob(openOperdayTaskBuilder.build());

        newOperday = getOperday();
        Assert.assertEquals(ONLINE, newOperday.getPhase());
        Assert.assertEquals(DIRECT, newOperday.getPdMode());
    }

    /**
     * Последовательно устанавливается перевод опердня сначала PRE_COB затем COB
     * @fsd 5.3.4
     * @throws Exception
     */
    @Test public void testPreCOB() throws Exception {

        Operday previosOperday = getOperday();
        updateOperday(ONLINE, CLOSED);

        baseEntityRepository.executeNativeUpdate("update gl_od set prc = ?", ProcessingStatus.STOPPED.name());

        // задача мониторинга ETL
        checkCreateEtlStructureMonitor();

        Date systemDate = getSystemDateTime();
        Calendar systemCalendar = Calendar.getInstance();
        systemCalendar.setTime(systemDate);
        int hours = systemCalendar.get(Calendar.HOUR_OF_DAY);

        Date loadDate = getOperday().getCurrentDate();
        Calendar loadDateCalendar = Calendar.getInstance();
        loadDateCalendar.setTime(loadDate);
        loadDateCalendar.set(Calendar.HOUR_OF_DAY, hours);

        processOnePosting();

        baseEntityRepository.executeNativeUpdate("update gl_etlpkg p set p.state = ? where p.DT_LOAD <= ? and p.state not in ('PROCESSED', 'ERROR')"
                , EtlPackage.PackageState.PROCESSED.name(), loadDateCalendar.getTime());

        SingleActionJob job = SingleActionJobBuilder.create()
                .withClass(ExecutePreCOBTaskNew.class).withName(System.currentTimeMillis() + "")
                .withProps(ExecutePreCOBTaskNew.TIME_LOAD_BEFORE_KEY + "=" + twiceChar(hours) + ":00").build();
        baseEntityRepository.executeUpdate("delete from JobHistory h where h.jobName = ?1", job.getName());

        baseEntityRepository.executeNativeUpdate("update gl_od set prc = ?", ProcessingStatus.STOPPED.name());
        baseEntityRepository.executeNativeUpdate("update GL_COB_STAT set  status = ? where status <> ?", CobStepStatus.Success.name(), CobStepStatus.Success.name());

        jobService.executeJob(job);
        List<JobHistory> histories = baseEntityRepository.select(JobHistory.class, "from JobHistory h where h.jobName = ?1", job.getName());
        Assert.assertEquals(1, histories.size());
        JobHistory prehist = histories.get(0);

        Operday newOperday = getOperday();
        Assert.assertEquals(newOperday, previosOperday);
        Assert.assertEquals(Operday.LastWorkdayStatus.CLOSED, newOperday.getLastWorkdayStatus());
        Assert.assertEquals(Operday.OperdayPhase.COB, newOperday.getPhase());

        jobService.executeJob(job);
        histories = baseEntityRepository.select(JobHistory.class, "from JobHistory h where h.jobName = ?1", job.getName());
        Assert.assertEquals(1, histories.size());
        Assert.assertEquals(prehist, histories.get(0));
    }

    /**
     * Последовательно устанавливается перевод опердня сначала PRE_COB затем COB
     * в режиме BUFFER
     * @fsd 5.3.4
     * @throws Exception
     */
    @Test public void testPreCOBBuffer() throws Exception {

        Operday previosOperday = getOperday();
        updateOperday(ONLINE, CLOSED, BUFFER);

        // задача мониторинга ETL
        checkCreateEtlStructureMonitor();

        Date systemDate = getSystemDateTime();
        Calendar systemCalendar = Calendar.getInstance();
        systemCalendar.setTime(systemDate);
        int hours = systemCalendar.get(Calendar.HOUR_OF_DAY);

        Date loadDate = getOperday().getCurrentDate();
        Calendar loadDateCalendar = Calendar.getInstance();
        loadDateCalendar.setTime(loadDate);
        loadDateCalendar.set(Calendar.HOUR_OF_DAY, hours);

        processOnePosting();

        baseEntityRepository.executeNativeUpdate("update gl_etlpkg p set p.state = ? where p.DT_LOAD <= ? and p.state not in ('PROCESSED', 'ERROR')"
                , EtlPackage.PackageState.PROCESSED.name(), loadDateCalendar.getTime());

        baseEntityRepository.executeNativeUpdate("update gl_od set prc = ?", ProcessingStatus.STOPPED.name());
        SingleActionJob calendarJob = SingleActionJobBuilder.create()
                .withClass(ExecutePreCOBTaskNew.class)
                .withProps(ExecutePreCOBTaskNew.TIME_LOAD_BEFORE_KEY + "=" + twiceChar(hours) + ":00").build();

        baseEntityRepository.executeNativeUpdate("update gl_od set prc = ?", ProcessingStatus.STOPPED.name());

        baseEntityRepository.executeNativeUpdate("update GL_COB_STAT set  status = ? where status <> ?", CobStepStatus.Success.name(), CobStepStatus.Success.name());

        jobService.executeJob(calendarJob);

        Operday newOperday = getOperday();
        Assert.assertEquals(newOperday, previosOperday);
        Assert.assertEquals(Operday.LastWorkdayStatus.CLOSED, newOperday.getLastWorkdayStatus());
        Assert.assertEquals(Operday.OperdayPhase.COB, newOperday.getPhase());
    }

    /**
     * перевод в PRE_COB
     * @fsd 5.3.4
     * @throws Exception
     */
    @Test public void testPreCOBOnOpenedLWD() throws Exception {

        Operday previosOperday = getOperday();
        updateOperday(ONLINE, OPEN);

        baseEntityRepository.executeNativeUpdate("update GL_COB_STAT set  status = ? where status <> ?", CobStepStatus.Success.name(), CobStepStatus.Success.name());

        // задача мониторинга ETL
        startupEtlStructureMonitor();

        Date systemDate = getSystemDateTime();
        Calendar systemCalendar = Calendar.getInstance();
        systemCalendar.setTime(systemDate);
        int hours = systemCalendar.get(Calendar.HOUR_OF_DAY);

        Date loadDate = getOperday().getCurrentDate();
        Calendar loadDateCalendar = Calendar.getInstance();
        loadDateCalendar.setTime(loadDate);
        loadDateCalendar.set(Calendar.HOUR_OF_DAY, hours);

        processOnePosting();

        baseEntityRepository.executeNativeUpdate("update gl_etlpkg p set p.state = ? where p.DT_LOAD <= ? and p.state not in ('PROCESSED', 'ERROR')"
                , EtlPackage.PackageState.PROCESSED.name(), loadDateCalendar.getTime());

        CalendarJob calendarJob = new CalendarJob();
        calendarJob.setDelay(0L);
        calendarJob.setDescription("test calendar job");
        calendarJob.setRunnableClass(ExecutePreCOBTaskNew.class.getName());
        calendarJob.setStartupType(MANUAL);
        calendarJob.setState(STOPPED);
        calendarJob.setName(System.currentTimeMillis() + "");
        calendarJob.setScheduleExpression("month=*;second=0;minute=0;hour=11");
        calendarJob.setProperties(ExecutePreCOBTaskNew.TIME_LOAD_BEFORE_KEY + "=" + twiceChar(hours) + ":00");

        //baseEntityRepository.executeUpdate("update Operday o set o.processingStatus = ?1", ProcessingStatus.STOPPED);

        jobService.executeJob(calendarJob);

        Operday newOperday = getOperday();
        Assert.assertEquals(newOperday, previosOperday);
        Assert.assertEquals(Operday.LastWorkdayStatus.CLOSED, newOperday.getLastWorkdayStatus());
        Assert.assertEquals(Operday.OperdayPhase.COB, newOperday.getPhase());
    }

    @Test
    public void testChronology() throws ParseException, IOException {
        Date operday = DateUtils.parseDate("01.01.2016", "dd.MM.yyyy");
        Date sysdate = DateUtils.parseDate("01.01.2016 02:00", "dd.MM.yyyy HH:mm");
        List<String> errList = new ArrayList<>();
        Properties properties = new Properties();
        properties.load(new StringReader(ExecutePreCOBTaskNew.CHECK_CHRONOLOGY_KEY+"=true"));
        Assert.assertFalse(remoteAccess.invoke(ExecutePreCOBTaskNew.class, "checkChronology", operday, sysdate, properties, errList));

        properties = new Properties();
        properties.load(new StringReader(ExecutePreCOBTaskNew.CHECK_CHRONOLOGY_KEY+"=false"));
        Assert.assertTrue(remoteAccess.invoke(ExecutePreCOBTaskNew.class, "checkChronology", operday, sysdate, properties, errList));

        properties = new Properties();
        properties.load(new StringReader("#" + ExecutePreCOBTaskNew.CHECK_CHRONOLOGY_KEY+"=true"));
        Assert.assertFalse(remoteAccess.invoke(ExecutePreCOBTaskNew.class, "checkChronology", operday, sysdate, properties, errList));

        Assert.assertFalse(remoteAccess.invoke(ExecutePreCOBTaskNew.class, "checkChronology", operday, sysdate, new Properties(), errList));

        properties = new Properties();
        properties.load(new StringReader(ExecutePreCOBTaskNew.CHECK_CHRONOLOGY_KEY+"=true"));
        sysdate = DateUtils.parseDate("02.01.2016 02:00", "dd.MM.yyyy HH:mm");
        Assert.assertTrue(remoteAccess.invoke(ExecutePreCOBTaskNew.class, "checkChronology", operday, sysdate, properties, errList));

        properties = new Properties();
        properties.load(new StringReader(ExecutePreCOBTaskNew.CHECK_CHRONOLOGY_KEY+"=true"));
        sysdate = DateUtils.parseDate("02.01.2016 00:00", "dd.MM.yyyy HH:mm");
        Assert.assertTrue(remoteAccess.invoke(ExecutePreCOBTaskNew.class, "checkChronology", operday, sysdate, properties, errList));
    }

    /**
     * проверка наличия необработанных пакетов при закрытии ОД
     */
    @Test public void testCheckPackages() {
        List<String> errList = new ArrayList<>();
        baseEntityRepository.executeNativeUpdate("update gl_etlpkg p set p.state = 'PROCESSED' where p.state not in ('PROCESSED', 'ERROR') ");
        Assert.assertTrue(remoteAccess.invoke(ExecutePreCOBTaskNew.class, "checkPackagesToloadExists", new Properties(), errList));

        Long stamp = System.currentTimeMillis();
        EtlPackage pkg1 = newPackageNotSaved(stamp, "Тестовый пакет " + stamp);
        pkg1.setDateLoad(getSystemDateTime());
        pkg1.setPackageState(EtlPackage.PackageState.LOADED);
        pkg1.setAccountCnt(1);
        pkg1.setPostingCnt(2);
        pkg1 = (EtlPackage) baseEntityRepository.save(pkg1);

        baseEntityRepository.executeUpdate("update EtlPackage p set p.packageState = ?1 where p.id = ?2"
                , EtlPackage.PackageState.LOADED, pkg1.getId());
        baseEntityRepository.executeUpdate("update EtlPosting p set p.valueDate = ?1 where p.etlPackage = ?2"
                , getOperday().getCurrentDate(), pkg1);
        Assert.assertFalse(remoteAccess.invoke(ExecutePreCOBTaskNew.class, "checkPackagesToloadExists", new Properties(), errList));

        Properties properties = new Properties();
        properties.setProperty(ExecutePreCOBTaskNew.CHECK_PACKAGES_KEY, "false");
        Assert.assertTrue(remoteAccess.invoke(ExecutePreCOBTaskNew.class, "checkPackagesToloadExists", properties, errList));

    }

    @Test public void testCalculateOperday() throws IOException {
        Date today = DateUtils.truncate(new Date(), Calendar.DATE);
        Date yesterday = DateUtils.addDays(today, -1);
        Date nextday = DateUtils.addDays(today, 3);
        baseEntityRepository.executeNativeUpdate("delete from cal where dat between ? and ? and ccy = 'RUR'", today, nextday);
        insertWorkday(nextday);
        setOperday(today, yesterday, COB, CLOSED);
        Assert.assertEquals(Operday.PdMode.DIRECT, calculatePdMode(new Properties()));

        insertWorkday(today);
        setOperday(yesterday, yesterday, COB, CLOSED);
        Assert.assertEquals(Operday.PdMode.BUFFER, calculatePdMode(new Properties()));

        Properties properties = new Properties();
        properties.load(new StringReader(OpenOperdayTask.PD_MODE_KEY + "=" + Operday.PdMode.DIRECT));
        Assert.assertEquals(Operday.PdMode.DIRECT, calculatePdMode(properties));

    }

    @Test public void testCheckRunOpenday() throws IOException {

        Operday operday = getOperday();
        BankCalendarDay nexday = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkdayAfter", operday.getCurrentDate());
        checkCreateBankCurrency(nexday.getId().getCalendarDate(), USD, new BigDecimal("61.222"));
        updateOperday(COB, CLOSED);
        operday = getOperday();

        Properties properties = new Properties();
        properties.put(OpenOperdayContextKey.OPERDAY_TO_OPEN, nexday.getId().getCalendarDate());
        properties.put(OpenOperdayContextKey.CURRENT_OD, operday);
        properties.put(OpenOperdayContextKey.TARGET_PD_MODE, Operday.PdMode.BUFFER);
        Assert.assertTrue(remoteAccess.invoke(OpenOperdayTask.class, "checkRun", "Open1", properties));

        updateOperday(ONLINE, OPEN);
        operday = getOperday();
        properties.put(OpenOperdayContextKey.OPERDAY_TO_OPEN, nexday.getId().getCalendarDate());
        properties.put(OpenOperdayContextKey.CURRENT_OD, operday);
        properties.put(OpenOperdayContextKey.TARGET_PD_MODE, Operday.PdMode.BUFFER);
        Assert.assertFalse(remoteAccess.invoke(OpenOperdayTask.class, "checkRun", "Open1", properties));

        baseEntityRepository.executeNativeUpdate("delete from currates where dat = ? and ccy = ?"
            , nexday.getId().getCalendarDate(), USD.getId());
        Assert.assertFalse(remoteAccess.invoke(OpenOperdayTask.class, "checkRun", "Open1", properties));

        updateOperday(COB, CLOSED);
        operday = getOperday();
        properties = new Properties();
        properties.put(OpenOperdayContextKey.OPERDAY_TO_OPEN, nexday.getId().getCalendarDate());
        properties.put(OpenOperdayContextKey.CURRENT_OD, operday);
        properties.put(OpenOperdayContextKey.TARGET_PD_MODE, Operday.PdMode.BUFFER);
        Assert.assertFalse(remoteAccess.invoke(OpenOperdayTask.class, "checkRun", "Open1", properties));

        checkCreateBankCurrency(nexday.getId().getCalendarDate(), USD, new BigDecimal("61.222"));
        updateOperday(COB, CLOSED);
        operday = getOperday();
        properties = new Properties();
        properties.put(OpenOperdayContextKey.OPERDAY_TO_OPEN, nexday.getId().getCalendarDate());
        properties.put(OpenOperdayContextKey.CURRENT_OD, operday);
        properties.put(OpenOperdayContextKey.TARGET_PD_MODE, Operday.PdMode.BUFFER);
        Assert.assertTrue(remoteAccess.invoke(OpenOperdayTask.class, "checkRun", "Open1", properties));
    }

    /**
     * выравнивание даты последней операции
     * @throws Exception
     */
    @Test public void correctBalturDATL() throws Exception {

        updateOperday(ONLINE, OPEN);

        AccRlnId rlnId = findAccRln("47422810%");

        logger.info("deleted BALTUR entries: " + baseEntityRepository.executeNativeUpdate("delete from baltur where bsaacid = ? and acid = ?"
                , rlnId.getBsaAcid(), rlnId.getAcid()));

        logger.info("deleted GL_PDJCHG entries: " + baseEntityRepository.executeNativeUpdate("delete from GL_PDJCHG where bsaacid = ? and acid = ?"
                , rlnId.getBsaAcid(), rlnId.getAcid()));

        Date day1 = DateUtils.parseDate("2017-07-01", "yyyy-MM-dd");
        Date day2 = DateUtils.parseDate("2017-07-04", "yyyy-MM-dd");
        Date day3 = DateUtils.parseDate("2017-07-10", "yyyy-MM-dd");

        Long id1 = createPd(day1, rlnId.getAcid(), rlnId.getBsaAcid(), "RUR", "@@GL123");
        Long id2 = createPd(day2, rlnId.getAcid(), rlnId.getBsaAcid(), "RUR", "@@GL123");
        Long id3 = createPd(day3, rlnId.getAcid(), rlnId.getBsaAcid(), "RUR", "@@GL123");

        logger.info(id1 + ":" + id2 + ":" + id3);

        TreeMap<Date, Long> ids = new TreeMap<>();
        ids.put(day1, id1);
        ids.put(day3, id3);
        ids.put(day2, id2);

        Date[] dateArr = ids.keySet().toArray(new Date[]{});
        Assert.assertEquals(dateArr[0], day1);
        Assert.assertEquals(dateArr[1], day2);
        Assert.assertEquals(dateArr[2], day3);

        List<DataRecord> balturs = getBalturList(rlnId);

        Assert.assertEquals(3, balturs.size());
        Assert.assertTrue("Все записи baltur DAT = DATL перед удаление проводок", balturs.stream().allMatch(r -> r.getDate("DATL").equals(r.getDate("DAT"))));

        // удалить среднюю проводку DATL будет равна из предыдущей записи
        baseEntityRepository.executeNativeUpdate("delete from pd where id = ?", id2);

        List<DataRecord> pdjrns = baseEntityRepository.select("select * from GL_PDJCHG where bsaacid = ? and acid = ? "
                , rlnId.getBsaAcid(), rlnId.getAcid());
        Assert.assertEquals(1, pdjrns.size());

        checkCorrectCount(rlnId, 0);
        Assert.assertTrue((Long)remoteAccess.invoke(DatLCorrector.class, "correctDatL") >= 1L);
        checkCorrectCount(rlnId, 1);

        balturs = getBalturList(rlnId);
        Assert.assertEquals(day1, balturs.get(0).getDate("DATL"));
        Assert.assertEquals(day1, balturs.get(1).getDate("DATL"));
        Assert.assertEquals(day3, balturs.get(2).getDate("DATL"));

        // давим первую проводку
        baseEntityRepository.executeNativeUpdate("update pd set invisible = '1' where id = ?", id1);
        Assert.assertTrue((Long)remoteAccess.invoke(DatLCorrector.class, "correctDatL") >= 2L);
        checkCorrectCount(rlnId, 2);

        balturs = getBalturList(rlnId);
        Assert.assertNull(balturs.get(0).getDate("DATL"));
        Assert.assertNull(balturs.get(1).getDate("DATL"));
        Assert.assertEquals(day3, balturs.get(2).getDate("DATL"));

        stopProcessing();

        Long idCobOld = remoteAccess.invoke(CobStatRepository.class, "getMaxRunCobId", getOperday().getCurrentDate());
        if (null != idCobOld) {
            logger.info("updated statistics to success: " + baseEntityRepository.executeNativeUpdate("update GL_COB_STAT set status = 'Success' where ID_COB = ?", idCobOld));
        }

        baseEntityRepository.executeNativeUpdate("update pd set invisible = '1' where id = ?", id3);

        SingleActionJob job = SingleActionJobBuilder.create()
                .withClass(ExecutePreCOBTaskNew.class).withName(System.currentTimeMillis() + "").build();
        baseEntityRepository.executeUpdate("delete from JobHistory h where h.jobName = ?1", job.getName());

        jobService.executeJob(job);

        balturs = getBalturList(rlnId);
        Assert.assertNull(balturs.get(0).getDate("DATL"));
        Assert.assertNull(balturs.get(1).getDate("DATL"));
        Assert.assertNull(balturs.get(2).getDate("DATL"));

    }

    private void checkCorrectCount(AccRlnId rlnId, int corr) throws SQLException {
        List<DataRecord> balt2 = baseEntityRepository.select("select DAT, DATL from BALTUR where bsaacid = ? and acid = ? "
                , rlnId.getBsaAcid(), rlnId.getAcid());
        final int[] cntDiff = {0};
        balt2.forEach((DataRecord rec) -> {if (!rec.getDate(0).equals(rec.getDate(1)))  cntDiff[0]++;});
        Assert.assertEquals(corr, cntDiff[0]);

    }

    /**
     * выполнение задачи закрытия баланса пред дня по расписанию
     */
    @Test
    public void testCloseLwdBalanceCut() throws InterruptedException {
        try {
            updateOperday(ONLINE, OPEN);
            Operday operday = getOperday();
            startupEtlStructureMonitor();                               // запустить обработку проводок
            shutdownJob(CloseLwdBalanceCutTask.class.getSimpleName());  // остановить задачу закрытия баланса

            // установить текущее время как время отсечки
            setLwdCut();

            boolean chk = remoteAccess.invoke(CloseLwdBalanceCutTask.class, "checkRun", null, null);
            Assert.assertTrue(chk);

            // запустить однократно задачу закрытия баланса
            remoteAccess.invoke(CloseLwdBalanceCutTask.class, "execWork", null, null);
            Operday operday2 = getOperday();

            Assert.assertEquals(operday, operday2);
            Assert.assertEquals(CLOSED, operday2.getLastWorkdayStatus());
            LwdBalanceCutView cutView = remoteAccess.invoke(LwdCutCachedRepository.class, "getRecord");
            Assert.assertNotNull(cutView.getCloseDateTime());
            TimeUnit.SECONDS.sleep(20);
            Operday operday3 = getOperday();
            Assert.assertEquals(ProcessingStatus.STARTED, operday3.getProcessingStatus());

        } finally {
            updateOperday(ONLINE, OPEN);
            updateOperdayMode(BUFFER, ProcessingStatus.STARTED);
        }
    }

    /**
     * Выполнение задачи закрытие баланса предыдущего рабочего как задачи
     */
    @Test public void testCloseLwdBalanceCutJob() throws Exception {

        try {
            startupEtlStructureMonitor();                               // запустить обработку проводок
            shutdownJob(CloseLwdBalanceCutTask.class.getSimpleName());  // остановить задачу закрытия баланса
            setLwdCut();                                                // установить текущее время как время отсечки

            updateOperday(ONLINE, OPEN);
            Operday operday = getOperday();
            Assert.assertEquals(OPEN, operday.getLastWorkdayStatus());

            CalendarJob closeBalanceJob = createCloseLwdBalanceCutJob();

            // симулируем запуск PdSyncTask
            JobHistory pdSyncJob = createJobHistory(PdSyncTask.class.getSimpleName(), operday.getCurrentDate());
            jobService.executeJob(closeBalanceJob);
            // закрытие баланса НЕ должно выполниться
            Operday operday2 = getOperday();
            Assert.assertEquals(operday2, operday);
            Assert.assertEquals(OPEN, operday2.getLastWorkdayStatus());
            List<JobHistory> histories = baseEntityRepository.select(JobHistory.class, "from JobHistory h where h.jobName = ?1", closeBalanceJob.getName());
            Assert.assertEquals(0, histories.size());

            // симулируем завершение PdSyncTask
            updateJobHistory(pdSyncJob, DwhUnloadStatus.SUCCEDED);
            jobService.executeJob(closeBalanceJob);
            // закрытие баланса ДОЛЖНО выполниться
            Operday operday3 = getOperday();
            Assert.assertEquals(operday3, operday);
            Assert.assertEquals(CLOSED, operday3.getLastWorkdayStatus());
            histories = baseEntityRepository.select(JobHistory.class, "from JobHistory h where h.jobName = ?1", closeBalanceJob.getName());
            Assert.assertEquals(1, histories.size());

            updateOperday(ONLINE, OPEN);
            jobService.executeJob(closeBalanceJob);
            // повторное закрытие баланса НЕ должно выполниться
            Operday operday4 = getOperday();
            Assert.assertEquals(operday4, operday);
            Assert.assertEquals(OPEN, operday4.getLastWorkdayStatus());

        } finally {
            updateOperday(ONLINE, OPEN);
            updateOperdayMode(BUFFER, ProcessingStatus.STARTED);
        }
    }

    @Test public void testAccessMode() {
        Operday operday = getOperday();
        Operday.SystemAccessMode before = (FULL == operday.getAccessMode()) ? LIMIT : FULL;
        Operday.SystemAccessMode mode = operday.getAccessMode();
        Assert.assertNotNull(mode);

        remoteAccess.invoke(OperdayController.class, "setAccessMode", before);
        operday = getOperday();
        Assert.assertEquals(before, operday.getAccessMode());
        boolean isexc = false;
        try {
            remoteAccess.invoke(OperdayController.class, "setAccessMode", before);
        } catch (Exception e) {
            isexc = true;
        }
        Assert.assertTrue(isexc);
    }

    private void setLwdCut() {
        Date currentDT = remoteAccess.invoke(OperdayController.class, "getSystemDateTime");
        Date currentDate = org.apache.commons.lang3.time.DateUtils.truncate(currentDT, Calendar.DATE);
        baseEntityRepository.executeNativeUpdate("delete from GL_LWDCUT");
        baseEntityRepository.executeNativeUpdate("insert into GL_LWDCUT (RUNDATE, CUTOFFTIME) values (?, ?)",
                currentDate, new SimpleDateFormat(LwdBalanceCutView.getTimeFormat()).format(currentDT));
        remoteAccess.invoke(LwdCutCachedRepository.class, "flushCache");
    }

    private Date getOperDayToOpen(Date current) {
        return ((BankCalendarDay)remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkdayAfter", current)).getId().getCalendarDate();
    }

    private void checkCreateFinalFlexStep(Date ondate) {
        DataRecord record = remoteAccess.invoke(OperdayRepository.class, "findWorkprocStep", FLEX_FINAL_STEP_NAME, ondate);

        if (null == record) {
            baseEntityRepository.executeNativeUpdate(
                    "insert into workproc (dat, id, starttime, endtime, result, count, msg) values (?,?,?,?,?,?,?)"
                    , ondate, FLEX_FINAL_STEP_NAME, null, null, FLEX_FINAL_STEP_RESULT_OK, 0, FLEX_FINAL_MSG_OK);
        } else if (!FLEX_FINAL_STEP_RESULT_OK.equalsIgnoreCase(record.getString("result"))
                || !FLEX_FINAL_MSG_OK.equalsIgnoreCase(record.getString("msg"))) {
            baseEntityRepository.executeNativeUpdate("update workproc set result = ?, msg = ? where id = ? and dat = ?",
                    FLEX_FINAL_STEP_RESULT_OK, FLEX_FINAL_MSG_OK, FLEX_FINAL_STEP_NAME, ondate);
        }
    }

    private String twiceChar(int hourOrMinute) {
        return StringUtils.leftPad(Integer.toString(hourOrMinute) + "", 2);
    }

    private CalendarJob createCloseLwdBalanceCutJob() {
        CalendarJob calendarJob = new CalendarJob();
        calendarJob.setDelay(0L);
        calendarJob.setDescription("test closeBalance job");
        calendarJob.setRunnableClass(CloseLwdBalanceCutTask.class.getName());
        calendarJob.setStartupType(MANUAL);
        calendarJob.setState(STOPPED);
        calendarJob.setName(CloseLwdBalanceCutTask.class.getSimpleName());  // System.currentTimeMillis() + "");
        calendarJob.setScheduleExpression("month=*;second=0;minute=0;hour=11");

        registerJob(calendarJob);
        return calendarJob;
    }

    private JobHistory createJobHistory(String jobName, Date operdate) {
        JobHistory job = new JobHistory();
        job.setJobName(jobName);
        job.setOperday(operdate);
        job.setResult(DwhUnloadStatus.STARTED);
        job.setStarttime(new Date());
        return (JobHistory) baseEntityRepository.save(job);
    }

    private JobHistory updateJobHistory(JobHistory job, DwhUnloadStatus result) {
        job.setResult(result);
        job.setEndtime(new Date());
        return (JobHistory) baseEntityRepository.update(job);
    }

    public static void checkCreateEtlStructureMonitor() {
        TimerJob etlStructureMonitorJob = (TimerJob) baseEntityRepository.selectFirst(TimerJob.class
                , "from TimerJob j where j.name = ?1", ETL_MONITOR_TASK);
        if (null == etlStructureMonitorJob) {
            CalendarJob etlMonitor = new CalendarJob();
            etlMonitor.setDelay(0L);
            etlMonitor.setDescription("TCJob");
            etlMonitor.setRunnableClass(EtlStructureMonitorTask.class.getName());
            etlMonitor.setStartupType(MANUAL);
            etlMonitor.setState(STOPPED);
            etlMonitor.setName(ETL_MONITOR_TASK);
            etlMonitor.setScheduleExpression("month=*;second=0;minute=*/1;hour=*");
            etlMonitor = (CalendarJob) baseEntityRepository.save(etlMonitor);
            jobService.startupJob(etlMonitor);
            registerJob(etlMonitor);
        }
    }

    public static void startupEtlStructureMonitor() {
        checkCreateEtlStructureMonitor();
        startupJob(ETL_MONITOR_TASK);
    }

    public static void startupJob(String jobName) {
        TimerJob job = remoteAccess.invoke(BackgroundJobsController.class, "getJob", jobName);
        if (null != job)
            remoteAccess.invoke(BackgroundJobsController.class, "startupJob", job);
    }

    public static void shutdownJob(String jobName) {
        TimerJob job = (TimerJob) baseEntityRepository.selectFirst(TimerJob.class, "from TimerJob j where j.name = ?1", jobName);
        if (null != job)
            remoteAccess.invoke(BackgroundJobsController.class, "shutdownJob", job);
    }

    private Operday.PdMode calculatePdMode(Properties properties) {
        return remoteAccess.invoke(OpenOperdayTask.class, "calculatePdMode", properties);
    }

    private void insertWorkday(Date date) {
        try {
            baseEntityRepository.executeNativeUpdate("insert into cal values (?, ' ', 'RUR', 'N')", date);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void processOnePosting() {
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

    private static void stopProcessing() {
        setProcessingFlag(ProcessingStatus.STOPPED);
    }

    private static void startProcessing() {
        setProcessingFlag(ProcessingStatus.STARTED);
    }

    private static void setProcessingFlag(ProcessingStatus status) {
        baseEntityRepository.executeNativeUpdate("update gl_od set prc = ?", status.name());
        refreshOperdayState();
    }

/*
    private long createPd(Date pod, String acid, String bsaacid, String glccy, String pbr) throws SQLException {
        long id = baseEntityRepository.selectFirst("select next value for PD_SEQ id from sysibm.sysdummy1").getLong(0);
        baseEntityRepository.executeNativeUpdate("insert into pd (id,pod,vald,acid,bsaacid,ccy,amnt,amntbc,pbr,pnar) " +
                "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", id, pod, pod, acid, bsaacid, glccy, 100,100, pbr, "1234");
        return id;
    }
*/

    private List<DataRecord> getBalturList(AccRlnId rlnId) throws SQLException {
        return baseEntityRepository.select("select * from baltur where bsaacid = ? and acid = ? order by dat"
                , rlnId.getBsaAcid(), rlnId.getAcid());
    }

}
