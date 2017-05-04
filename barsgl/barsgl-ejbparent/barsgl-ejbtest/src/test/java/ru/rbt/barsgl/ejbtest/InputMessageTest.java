package ru.rbt.barsgl.ejbtest;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.excel.InputMessageProcessor;
import ru.rbt.barsgl.ejb.controller.operday.task.EtlStructureMonitorTask;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.security.AuthorizationServiceSupport;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbcore.mapping.job.TimerJob;
import ru.rbt.ejbcore.util.StringUtils;
//import ru.rbt.barsgl.gwt.server.upload.ExcelParser;
import ru.rbt.shared.LoginResult;
import ru.rbt.barsgl.shared.enums.OperState;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static ru.rbt.barsgl.ejbcore.mapping.job.TimerJob.JobState.STOPPED;
import static ru.rbt.barsgl.ejbtest.utl.Utl4Tests.shortTimestamp;
import static ru.rbt.shared.LoginResult.LoginResultStatus.FAILED;
import static ru.rbt.barsgl.shared.enums.JobStartupType.MANUAL;
import ru.rbt.gwt.security.ejb.AuthorizationServiceGwtSupport;

/**
 * Created by Ivan Sevastyanov
 * Обработка входных сообщений из файлы
 */
public class InputMessageTest extends AbstractTimerJobTest {

    private static final String ETL_SINGLE_ACTION_MONITOR = "ETL_SINGLE_ACTION_MONITOR";

    @BeforeClass
    public static void beforeClass() {
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
    }

    /**
     * Обработка сообщений из файла Excel
     * @throws Exception
     */
//    @Test
//    public void test() throws Exception {
//        try (InputStream is = InputMessageTest.class.getClassLoader().getResourceAsStream("example_old.xlsx")){
//            ExcelParser parser = new ExcelParser(is);
//            List<List<Object>> rows = parser.parse(2);
//
//            rows = Lists.newArrayList(Iterables.transform(rows, (List<Object> objects) -> {
//                objects.set(0, System.currentTimeMillis() + "");
//                objects.set(7, getOperday().getCurrentDate());
//                return objects;
//            }));
//
//            Assert.assertTrue(Iterables.all(rows, row ->
//                    null != row.get(17) && null != row.get(18) && null != row.get(19) && null != row.get(20)));
//
//            remoteAccess.invoke(InputMessageProcessor.class, "processMessage", rows);
//        }
//    }

    /**
     * Обработка сообщения с простой проводкой
     * @throws Exception
     */
    @Test
    @Ignore
    public void test2() throws Exception {
        //GLOperation operation1 = (GLOperation) baseEntityRepository.selectFirst(GLOperation.class, "from GLOperation o where o.id = (select max(o2.id) from GLOperation o2)");

        Random random = new Random();

        BigDecimal amnt = new BigDecimal(Math.abs(new Integer(random.nextInt()).longValue()));

        final String stamp = System.currentTimeMillis() + "";

        Object[] params = new Object[]{
                stamp
                ,"A"
                ,"1"
                ,"100"
                ,"200"
                ,"1200"
                ,"BR1"
                , getOperday().getCurrentDate()
                ,"Описание1"
                ,"R1"
                ,"R2"
                ,"40817036200012959997"
                , BankCurrency.AUD.getCurrencyCode()
                , amnt.doubleValue()
                ,"40817036250010000018"
                , BankCurrency.AUD.getCurrencyCode()
                , amnt.doubleValue()
                , "N"
                , shortTimestamp() + "_par_ref"
                , "N"
                , shortTimestamp() + "_str_ref"
        };
        List<List<Object>> rows = new ArrayList<>();
        rows.add(Lists.newArrayList(params));
        remoteAccess.invoke(InputMessageProcessor.class, "processMessage", rows);

        Thread.sleep(10000);

        GLOperation operation = (GLOperation) baseEntityRepository.selectFirst(GLOperation.class
                , "from GLOperation o where o.aePostingId = ?1", stamp);

        //Assert.assertTrue(operation1 == null || (!operation1.getId().equals(operation2.getId())));

        Assert.assertNotNull(operation);
        Assert.assertEquals(amnt.longValue(), operation.getAmountDebit().longValue());
        Assert.assertEquals(amnt.longValue(), operation.getAmountCredit().longValue());
        Assert.assertEquals(YesNo.N, operation.getFan());
        Assert.assertEquals(stamp + "_par_ref", operation.getParentReference());
        Assert.assertEquals(YesNo.N, operation.getStorno());
        Assert.assertEquals(stamp + "_str_ref", operation.getStornoReference());

        List<GLPosting> posts = (List<GLPosting>) baseEntityRepository.select(GLPosting.class
                , "from GLPosting p where p.operation = ?1", operation);
        Assert.assertEquals(1, posts.size());
        System.out.println("Posting PCID = " + posts.get(0).getId());
        List<Pd> pds = baseEntityRepository.select(GLPosting.class, "from Pd p where p.pcId = ?1 order by p.id", posts.get(0).getId());
        Assert.assertEquals(2, pds.size());
        Assert.assertEquals(amnt.longValue()*-100, pds.get(0).getAmount().longValue());
        Assert.assertEquals(amnt.longValue()*100, pds.get(1).getAmount().longValue());

    }

    /**
     * Создание пакета с простой проводкой
     */
    @Test public void testBuildPackageFromParams() {
        Random random = new Random();

        BigDecimal amnt = new BigDecimal(new Integer(random.nextInt()).longValue());

        final String stamp = System.currentTimeMillis() + "";

        Object[] params = new Object[]{
                stamp
                ,"A"
                ,"1"
                ,"100"
                ,"200"
                ,"1200"
                ,"BR1"
                , getOperday().getCurrentDate()
                ,"Описание1"
                ,"R1"
                ,"R2"
                ,"40817036200012959997"
                , BankCurrency.AUD.getCurrencyCode()
                , amnt.doubleValue()
                ,"40817036250010000018"
                , BankCurrency.AUD.getCurrencyCode()
                , amnt.doubleValue()
                , "N"
                , stamp + "_par_ref"
                , "N"
                , stamp + "_str_ref"
        };
        List<List<Object>> rows = new ArrayList<>();
        rows.add(Lists.newArrayList(params));

        EtlPackage pkg = remoteAccess.invoke(InputMessageProcessor.class, "buildPackage", rows);

        Assert.assertNotNull(pkg);
    }

    /**
     * Обработка пакета проводок монитором входных сообщений
     * @throws InterruptedException
     */
    @Test
    @Ignore
    public void testProcessPackageByMonitor() throws InterruptedException {
        Random random = new Random();

        BigDecimal amnt = new BigDecimal(Math.abs(new Integer(random.nextInt()).longValue()));

        final String stamp = System.currentTimeMillis() + "";

        Object[] params1 = new Object[]{
                stamp + "_1"
                ,"A"
                ,"1"
                ,"100"
                ,"200"
                ,"1200"
                ,"BR1"
                , getOperday().getCurrentDate()
                ,"Описание1"
                ,"R1"
                ,"R2"
                ,"40817036200012959997"
                , BankCurrency.AUD.getCurrencyCode()
                , amnt.doubleValue()
                ,"40817036250010000018"
                , BankCurrency.AUD.getCurrencyCode()
                , amnt.doubleValue()
                , "N"
                , shortTimestamp() + "_1_par_ref"
                , "N"
                , shortTimestamp() + "_1_str_ref"
        };
        Object[] params2 = new Object[]{
                stamp + "_2"
                ,"A"
                ,"1"
                ,"100"
                ,"200"
                ,"1200"
                ,"BR1"
                , getOperday().getCurrentDate()
                ,"Описание1"
                ,"R1"
                ,"R2"
                ,"40817036200012959997"
                , BankCurrency.AUD.getCurrencyCode()
                , amnt.doubleValue()
                ,"40817036250010000018"
                , BankCurrency.AUD.getCurrencyCode()
                , amnt.doubleValue()
                , "N"
                , shortTimestamp() + "_2_par_ref"
                , "N"
                , shortTimestamp() + "_2_str_ref"
        };
        List<List<Object>> rows = new ArrayList<>();
        rows.add(Lists.newArrayList(params1));
        rows.add(Lists.newArrayList(params2));

        EtlPackage pkg = remoteAccess.invoke(InputMessageProcessor.class, "buildPackage", rows);

        Assert.assertNotNull(pkg);

        checkCreateEtlStructureMonitorSingle();

        TimerJob etlStructureMonitorJob = (TimerJob) baseEntityRepository.selectFirst(TimerJob.class
                , "from TimerJob j where j.name = ?1", ETL_SINGLE_ACTION_MONITOR);
        jobService.startupJob(etlStructureMonitorJob.getName());

        Thread.sleep(10000);

        pkg = (EtlPackage) baseEntityRepository.refresh(pkg, true);
        Assert.assertEquals(EtlPackage.PackageState.PROCESSED, pkg.getPackageState());

        List<EtlPosting> postings = baseEntityRepository.select(EtlPosting.class, "from EtlPosting p where p.etlPackage = ?1", pkg);
        Assert.assertEquals(2, postings.size());
        for (EtlPosting posting : postings) {
            Assert.assertNotNull(posting);
            Assert.assertTrue(posting.getErrorMessage(), StringUtils.isEmpty(posting.getErrorMessage()) || 0 == posting.getErrorCode());

        }
        GLOperation operation = (GLOperation) baseEntityRepository.selectFirst(GLOperation.class
                , "from GLOperation o where o.aePostingId = ?1", stamp + "_1");
        Assert.assertNotNull(operation);
        Assert.assertEquals(OperState.POST, operation.getState());

        operation = (GLOperation) baseEntityRepository.selectFirst(GLOperation.class
                , "from GLOperation o where o.aePostingId = ?1", stamp + "_2");
        Assert.assertNotNull(operation);
        Assert.assertEquals(OperState.POST, operation.getState());
    }

    /**
     * Проверка авторизации незарегистрированного пользователя (ошибка)
     */
    @Test
    public void testAuth() {
        LoginResult res = remoteAccess.invoke(AuthorizationServiceGwtSupport.class.getName(), "login", new Object[]{"FakeUser", "123"});
        Assert.assertEquals(FAILED, res.getLoginResultStatus());
    }

    private void checkCreateEtlStructureMonitorSingle() {
        TimerJob etlStructureMonitorJob = (TimerJob) baseEntityRepository.selectFirst(TimerJob.class
                , "from TimerJob j where j.name = ?1", ETL_SINGLE_ACTION_MONITOR);
        if (null == etlStructureMonitorJob) {
            SingleActionJob etlMonitor = new SingleActionJob();
            etlMonitor.setDelay(0L);
            etlMonitor.setDescription("SingleEtlMonitor");
            etlMonitor.setRunnableClass(EtlStructureMonitorTask.class.getName());
            etlMonitor.setStartupType(MANUAL);
            etlMonitor.setState(STOPPED);
            etlMonitor.setName(ETL_SINGLE_ACTION_MONITOR);
            etlMonitor = (SingleActionJob) baseEntityRepository.save(etlMonitor);
            registerJob(etlMonitor);
        }
    }
}
