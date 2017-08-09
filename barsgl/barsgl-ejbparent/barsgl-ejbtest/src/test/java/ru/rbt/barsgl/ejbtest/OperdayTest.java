package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.BankCalendarDay;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.common.repository.od.OperdayRepository;
import ru.rbt.barsgl.ejb.controller.operday.task.*;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.dict.LwdBalanceCutView;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.repository.dict.LwdCutCachedRepository;
import ru.rbt.tasks.ejb.entity.task.JobHistory;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.mapping.job.CalendarJob;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbcore.mapping.job.TimerJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.barsgl.shared.enums.EnumUtils;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.barsgl.shared.enums.ProcessingStatus;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static ru.rbt.barsgl.ejb.common.CommonConstants.ETL_MONITOR_TASK;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.CLOSED;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.COB;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.BUFFER;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.DIRECT;
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
public class OperdayTest extends AbstractTimerJobTest {

    public static final String FLEX_FINAL_STEP_NAME = "FLEX";

    @BeforeClass public static void beforeClass() {
        initCorrectOperday();
        baseEntityRepository.executeNativeUpdate("update gl_oper o set VDATE = VDATE - 20 DAY, POSTDATE = POSTDATE - 20 DAY");
    }


    @Before
    public void before() {
        initCorrectOperday();
        baseEntityRepository.executeUpdate("update EtlPackage p set p.packageState = ?1", EtlPackage.PackageState.PROCESSED);
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
     * Выполнение задачи закрытие баланса предыдущего рабочего дня
     * @fsd 5.3.4
     * @throws Exception
     */
    @Test public void closeLastWorkdayBalance() throws Exception {

        updateOperday(ONLINE, OPEN);

        Operday previosOperday = getOperday();

        CalendarJob calendarJob = new CalendarJob();
        calendarJob.setDelay(0L);
        calendarJob.setDescription("test calendar job");
        calendarJob.setRunnableClass(CloseLastWorkdayBalanceTask.class.getName());
        calendarJob.setStartupType(MANUAL);
        calendarJob.setState(STOPPED);
        calendarJob.setName(System.currentTimeMillis() + "");
        calendarJob.setScheduleExpression("month=*;second=0;minute=0;hour=11");

        registerJob(calendarJob);

        jobService.executeJob(calendarJob);

        Operday newOperday = getOperday();

        Assert.assertEquals(newOperday, previosOperday);
        Assert.assertEquals(newOperday.getLastWorkdayStatus(), Operday.LastWorkdayStatus.CLOSED);
        Assert.assertEquals(previosOperday.getLastWorkdayStatus(), Operday.LastWorkdayStatus.OPEN);
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

        baseEntityRepository.executeUpdate("update EtlPackage p set p.packageState = ?1 where p.dateLoad <= ?2"
                , EtlPackage.PackageState.PROCESSED, loadDateCalendar.getTime());

        SingleActionJob job = SingleActionJobBuilder.create()
                .withClass(ExecutePreCOBTaskNew.class).withName(System.currentTimeMillis() + "")
                .withProps(ExecutePreCOBTaskNew.TIME_LOAD_BEFORE_KEY + "=" + twiceChar(hours) + ":00").build();
        baseEntityRepository.executeUpdate("delete from JobHistory h where h.jobName = ?1", job.getName());

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

        baseEntityRepository.executeUpdate("update EtlPackage p set p.packageState = ?1 where p.dateLoad <= ?2"
                , EtlPackage.PackageState.PROCESSED, loadDateCalendar.getTime());

        baseEntityRepository.executeNativeUpdate("update gl_od set prc = ?", ProcessingStatus.STOPPED.name());
        SingleActionJob calendarJob = SingleActionJobBuilder.create()
                .withClass(ExecutePreCOBTaskNew.class)
                .withProps(ExecutePreCOBTaskNew.TIME_LOAD_BEFORE_KEY + "=" + twiceChar(hours) + ":00").build();

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
        // TODO чтобы тест прошел, надо запустить вручную Мониторинг АЕ (на тесте он не всегда сам запускается)
        Operday previosOperday = getOperday();
        updateOperday(ONLINE, OPEN);

        if (EnumUtils.contains(new ProcessingStatus[]{ProcessingStatus.ALLOWED, ProcessingStatus.STARTED}
                , previosOperday.getProcessingStatus())) {
            try {
                remoteAccess.invoke(OperdayController.class, "setProcessingStatus", ProcessingStatus.REQUIRED);
            } catch (Exception e) {
                remoteAccess.invoke(OperdayController.class, "setProcessingStatus", ProcessingStatus.STOPPED);
            }
        }

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

        baseEntityRepository.executeUpdate("update EtlPackage p set p.packageState = ?1 where p.dateLoad <= ?2"
                , EtlPackage.PackageState.PROCESSED, loadDateCalendar.getTime());

        CalendarJob calendarJob = new CalendarJob();
        calendarJob.setDelay(0L);
        calendarJob.setDescription("test calendar job");
        calendarJob.setRunnableClass(ExecutePreCOBTaskNew.class.getName());
        calendarJob.setStartupType(MANUAL);
        calendarJob.setState(STOPPED);
        calendarJob.setName(System.currentTimeMillis() + "");
        calendarJob.setScheduleExpression("month=*;second=0;minute=0;hour=11");
        calendarJob.setProperties(ExecutePreCOBTaskNew.TIME_LOAD_BEFORE_KEY + "=" + twiceChar(hours) + ":00");

        baseEntityRepository.executeUpdate("update Operday o set o.processingStatus = ?1", ProcessingStatus.STOPPED);

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
        baseEntityRepository.executeUpdate("update EtlPackage p set p.packageState = ?1", EtlPackage.PackageState.PROCESSED);
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
        baseEntityRepository.executeNativeUpdate("delete from cal where dat between ?1 and ?2 and ccy = 'RUR'", today, nextday);
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

    @Test
    public void testCloseLwdBalanceCut() {
        Operday operday = getOperday();
        try {
            setOperday(operday.getCurrentDate(), operday.getLastWorkingDay(), ONLINE, OPEN, BUFFER);
            updateOperdayMode(BUFFER, ProcessingStatus.STOPPED);
            Date currentDT = remoteAccess.invoke(OperdayController.class, "getSystemDateTime");
            Date currentDate = org.apache.commons.lang3.time.DateUtils.truncate(currentDT, Calendar.DATE);

            baseEntityRepository.executeNativeUpdate("delete from GL_LWDCUT");
            baseEntityRepository.executeNativeUpdate("insert into GL_LWDCUT (RUNDATE, CUTOFFTIME) values (?, ?)",
                    currentDate, new SimpleDateFormat(LwdBalanceCutView.getTimeFormat()).format(currentDT));

            remoteAccess.invoke(LwdCutCachedRepository.class, "flushCache");
            remoteAccess.invoke(CloseLwdBalanceCutTask.class, "execWork", null, null);
            Operday operday2 = getOperday();
            Assert.assertEquals(CLOSED, operday2.getLastWorkdayStatus());
            LwdBalanceCutView cutView = remoteAccess.invoke(LwdCutCachedRepository.class, "getRecord");
            Assert.assertNotNull(cutView.getCloseDateTime());

/*
            baseEntityRepository.executeNativeUpdate("update GL_LWDCUT set OTS_CLOSE = null");
            remoteAccess.invoke(CloseLwdBalanceCutTask.class, "execWork", null, null);
            cutView = (LwdBalanceCutView) baseEntityRepository.findById(LwdBalanceCutView.class, currentDate);
            Assert.assertNull(cutView.getCloseDateTime());

            remoteAccess.invoke(LwdCutCachedRepository.class, "flushCache");
            remoteAccess.invoke(CloseLwdBalanceCutTask.class, "execWork", null, null);
            cutView = remoteAccess.invoke(LwdCutCachedRepository.class, "getRecord");
            Assert.assertNotNull(cutView.getCloseDateTime());
*/
        } finally {
            setOperday(operday.getCurrentDate(), operday.getLastWorkingDay(), ONLINE, operday.getLastWorkdayStatus(), BUFFER);
            updateOperdayMode(BUFFER, ProcessingStatus.STARTED);
        }

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

    private void checkCreateEtlStructureMonitor() {
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

    private Operday.PdMode calculatePdMode(Properties properties) {
        return remoteAccess.invoke(OpenOperdayTask.class, "calculatePdMode", properties);
    }

    private void insertWorkday(Date date) {
        try {
            baseEntityRepository.executeNativeUpdate("insert into cal values (?1, '', 'RUR', '')", date);
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

}
