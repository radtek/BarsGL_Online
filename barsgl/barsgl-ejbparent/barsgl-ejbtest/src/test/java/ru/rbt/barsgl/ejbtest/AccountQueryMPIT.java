package ru.rbt.barsgl.ejbtest;

import com.ibm.mq.jms.*;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.controller.operday.task.AccountQueryTaskMT;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.AccountQueryProcessor;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.QueueInputMessage;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.QueueProperties;
import ru.rbt.barsgl.ejbcore.AsyncProcessor;
import ru.rbt.barsgl.ejbcore.mapping.job.IntervalJob;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbcore.mapping.job.TimerJob;
import ru.rbt.barsgl.ejbtest.mq.MqUtil;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.barsgl.shared.enums.JobSchedulingType;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.tasks.ejb.job.BackgroundJobsController;

import javax.persistence.SequenceGenerator;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.logging.Logger;

import static ru.rbt.audit.entity.AuditRecord.LogLevel.Error;
import static ru.rbt.audit.entity.AuditRecord.LogLevel.SysError;
import static ru.rbt.audit.entity.AuditRecord.LogLevel.Warning;
import static ru.rbt.barsgl.ejb.common.CommonConstants.ACLIRQ_TASK;
import static ru.rbt.barsgl.ejb.props.PropertyName.MQ_TIMEOUT;
import static ru.rbt.barsgl.ejb.props.PropertyName.MQ_TIME_UNIT;
import static ru.rbt.barsgl.ejbcore.mapping.job.TimerJob.JobState.STOPPED;
import static ru.rbt.barsgl.ejbtest.OperdayIT.shutdownJob;
import static ru.rbt.barsgl.shared.enums.JobStartupType.MANUAL;

/**
 * Created by ER22228
 */
public class AccountQueryMPIT extends AbstractQueueIT {

    public static final Logger logger = Logger.getLogger(AccountQueryMPIT.class.getName());

    private static final String qType = "LIRQ";
//    private final static String host = "vs569";   // int C
//    private final static String broker = "QM_MBROKER4_T4";
//    private final static String host = "vs529";   // int D
//    private final static String broker = "QM_MBROKER4_T5";
    public static final String host = "vs11205";    //"mbrk4-inta.testhpcsa.imb.ru";
    private final static String broker = "QM_MBROKER4";
    private final static String channel= "SYSTEM.DEF.SVRCONN";
    private final static String acliquIn = "UCBRU.ADP.BARSGL.ACLIQU.REQUEST";
    private final static String acliquOut = "UCBRU.ADP.BARSGL.ACLIQU.RESPONSE";
    private static final boolean writeOut = true;
    private static final Charset charset = StandardCharsets.UTF_8;

/*
    @After
    public void after() {
        setPropertyTimeout("MINUTES", 10);
    }
*/
    private String getJobProperty(String topic, String ahost, String abroker, String alogin, String apassw) {
        return getJobProperty (topic, acliquIn, null, ahost, "1414", abroker, channel, alogin, apassw, "30", writeOut);
    }

    private QueueProperties getQueueProperties(String topic, String ahost, String abroker, String alogin, String apassw) {
        return getQueueProperties(topic, acliquIn, acliquOut, ahost, 1414, abroker, channel, alogin, apassw, 30, writeOut, false);
    }

    public String getResourceText(String resource) throws IOException {
        File inFile = new File(this.getClass().getResource(resource).getFile());
        return FileUtils.readFileToString(inFile, AccountQueryProcessor.charsetName);
    }

    /**
     * Тест посылки одного запроса и получения ответа
     * (все кейзы по бизнесу: клиент, клиент + ACOD, клиент + AccountType, счет)
     * @throws Exception
     */
    @Test
    public void testA() throws Exception {
        printCommunicatorName();

        deletePropertyTimeout();
        deletePropertyExecutor();

        QueueProperties properties = getQueueProperties(qType, host, broker, mqTestLogin, mqTestPassw);
        clearQueue(properties, acliquIn, 1000);
        clearQueue(properties, acliquOut, 1000);

        startConnection(properties);
        sendToQueue(getResourceText("/AccountQueryProcessorTest_1.xml"), properties, null, null, acliquIn);

        Thread.sleep(30000L);
        SingleActionJob job =
                SingleActionJobBuilder.create()
                        .withClass(AccountQueryTaskMT.class)
                        .withName("AccountQueryTaskMT_A")
                        .withProps(getJobProperty(properties))
                        .build();
        jobService.executeJob(job);

        Thread.sleep(30000L);

        QueueInputMessage answer = receiveFromQueue(acliquOut, charset);
        Assert.assertFalse("Нет ответного сообщения", StringUtils.isEmpty(answer.getTextMessage()));
        Assert.assertFalse(answer.getTextMessage().contains("Error"));
        System.out.println("\nReceived message from " + acliquOut + ":\n" + answer.getTextMessage());
        System.out.println();

    }

    /**
     * тест генерации системной ошибки по таймауту в режиме JavaEE
     * @throws Exception
     */
    @Test
    public void testStressErrorEE() throws Exception {

        int cnt = 30;
        int cntmax = 5000;

        printCommunicatorName();

        QueueProperties properties = getQueueProperties(qType, host, broker, mqTestLogin, mqTestPassw);
        clearQueue(properties, acliquIn, 1000);
        clearQueue(properties, acliquOut, 1000);

        startConnection(properties);
        sendToQueue(getResourceText("/AccountQueryProcessorTest_1.xml"), properties, null, null, acliquIn, cnt);

        long idAudit = getAuditMaxId();

        setPropertyTimeout("SECONDS", 5);
//        setPropertyTimeout("MINUTES", 1);
        setPropertyExecutor(AsyncProcessor.ExecutorType.EE);

        Thread.sleep(1000L);
        SingleActionJob job =
                SingleActionJobBuilder.create()
                        .withClass(AccountQueryTaskMT.class)
                        .withName("AccountQueryTaskMT_A")
                        .withProps(getJobProperty (qType, acliquIn, acliquOut, host, "1414", broker, channel, mqTestLogin, mqTestPassw, "30", writeOut))
                        .build();
        jobService.executeJob(job);

        Thread.sleep(5000L);
        long n = clearQueue(properties, acliquOut, cntmax);
//        Assert.assertTrue("Нет сообщений в выходной очереди", n > 0);
//        Assert.assertTrue("Все сообщения обработаны", n < cnt);

        AuditRecord record = getAuditError(idAudit, SysError);
        Assert.assertNotNull("Нет записи об ошибке в аудит", record);
//        Assert.assertTrue("Нет записи об ошибке в аудит", record.getErrorMessage().contains("Код ошибки '3018'"));    // это был вариент с Error
    }

    /**
     * тест генерации системной ошибки по таймауту в режиме JavaSE
     * @throws Exception
     */
    @Test
    public void testStressErrorSE() throws Exception {

        int cnt = 30;
        int cntmax = 5000;

        printCommunicatorName();

        QueueProperties properties = getQueueProperties(qType, host, broker, mqTestLogin, mqTestPassw);
        clearQueue(properties, acliquIn, 1000);
        clearQueue(properties, acliquOut, 1000);

        startConnection(properties);
        sendToQueue(getResourceText("/AccountQueryProcessorTest_1.xml"), properties, null, null, acliquIn, cnt);

        long idAudit = getAuditMaxId();

        setPropertyTimeout("SECONDS", 5);
//        setPropertyTimeout("MINUTES", 1);
        setPropertyExecutor(AsyncProcessor.ExecutorType.SE);

        Thread.sleep(1000L);
        SingleActionJob job =
                SingleActionJobBuilder.create()
                        .withClass(AccountQueryTaskMT.class)
                        .withName("AccountQueryTaskMT_A")
                        .withProps(getJobProperty (qType, acliquIn, acliquOut, host, "1414", broker, channel, mqTestLogin, mqTestPassw, "30", writeOut))
                        .build();
        jobService.executeJob(job);

        Thread.sleep(5000L);
        long n = clearQueue(properties, acliquOut, cntmax);
//        Assert.assertTrue("Нет сообщений в выходной очереди", n > 0);
//        Assert.assertTrue("Все сообщения обработаны", n < cnt);

        AuditRecord record = getAuditError(idAudit, SysError);
        Assert.assertNotNull("Нет записи об ошибке в аудит", record);
//        Assert.assertTrue("Нет записи об ошибке в аудит", record.getErrorMessage().contains("Код ошибки '3018'"));
    }

    /**
     * Стресс-тест посылки и получения 500 сообщений
     * @throws Exception
     */
    @Test
    public void testStress500() throws Exception {

        int cnt = 500;
        int cntmax = 5000;

        printCommunicatorName();

        shutdownJob(ACLIRQ_TASK);

        QueueProperties properties = getQueueProperties(qType, host, broker, mqTestLogin, mqTestPassw);
        clearQueue(properties, acliquIn, cntmax);
        clearQueue(properties, acliquOut, cntmax);

        startConnection(properties);
        // длинный тест
//        sendToQueue(getResourceText("/AccountQueryProcessorTest.xml"), properties, null, null, acliquIn, cnt);
        // короткий тест
        sendToQueue(getResourceText("/AccountQueryProcessorTest_1.xml"), properties, null, null, acliquIn, cnt);

        long idAudit = getAuditMaxId();
        long idJ = getJournalMaxId();

        setPropertyExecutor(AsyncProcessor.ExecutorType.SE);
        setPropertyTimeout("MINUTES", 10);
        runAclirqJob(getJobProperty(qType, acliquIn, acliquOut, host, "1414", broker, channel, mqTestLogin, mqTestPassw, "30", writeOut));
        Thread.sleep(60 * 3000L);
        shutdownJob(ACLIRQ_TASK);

        long n = clearQueue(properties, acliquOut, cntmax);
        Assert.assertTrue(n >= cnt);    // ???
        Assert.assertNull("Есть записи об ошибке в аудит", getAuditError(idAudit, SysError, Error, Warning));

        Assert.assertTrue(getStatistics(idJ) >= cnt);
    }

    /**
     * Посылка 5000 сообщений для стресс-теста
     * @throws Exception
     */
    @Test
    @Ignore
    public void testSend() throws Exception {

        int cnt = 500;
        int cntmax = Math.max(cnt * 2, 5000);

        printCommunicatorName();

        shutdownJob(ACLIRQ_TASK);

        MQQueueConnectionFactory cf = MqUtil.getConnectionFactory(host, broker, channel);
        MqUtil.clearQueue(cf, acliquIn, mqTestLogin, mqTestPassw, cntmax);
        MqUtil.clearQueue(cf, acliquOut, mqTestLogin, mqTestPassw, cntmax);

        MqUtil.sendToQueue(cf, acliquIn, new File(this.getClass().getResource("/AccountQueryProcessorTest_1.xml").getFile()), acliquOut, mqTestLogin, mqTestPassw, cnt);
    }

    /**
     * Стресс-тест посылки и получения 5000 сообщений
     * @throws Exception
     */
    @Test
    @Ignore
    public void testStress5000() throws Exception {

        int cnt = 5000;
        int cntmax = cnt * 2;

        printCommunicatorName();

        shutdownJob(ACLIRQ_TASK);

        QueueProperties properties = getQueueProperties(qType, host, broker, mqTestLogin, mqTestPassw);
        clearQueue(properties, acliquIn, cntmax);
        clearQueue(properties, acliquOut, cntmax);

        startConnection(properties);
        // короткий тест
        sendToQueue(getResourceText("/AccountQueryProcessorTest_1.xml"), properties, null, null, acliquIn, cnt);

        long idAudit = getAuditMaxId();
        long idJ = getJournalMaxId();

        setPropertyTimeout("MINUTES", 10);
        runAclirqJob(getJobProperty (qType, acliquIn, acliquOut, host, "1414", broker, channel, mqTestLogin, mqTestPassw, "30", writeOut));

        long cntrec = 0;
        while(cntrec < cnt) {
            Thread.sleep(60 * 1000L);
            cntrec  = getStatistics(idJ);
        }
        Assert.assertNull("Есть записи об ошибке в аудит", getAuditError(idAudit, SysError, Error, Warning));

        shutdownJob(ACLIRQ_TASK);
        clearQueue(properties, acliquOut, cntmax);
    }

    private static void setPropertyTimeout(String unit, int interval) {
        deletePropertyTimeout();
        baseEntityRepository.executeNativeUpdate("insert into gl_prprp values " +
                        "(?, 'root', 'Y', 'STRING_TYPE', 'Единицы времени обработки сообщений из очереди ACLIRQ', null, ?, null)",
                MQ_TIME_UNIT.getName(), unit);
        baseEntityRepository.executeNativeUpdate("insert into gl_prprp values " +
                        "(?, 'root', 'Y', 'NUMBER_TYPE', 'Макс. время обработки сообщений из очереди ACLIRQ', null, null, ?)",
                MQ_TIMEOUT.getName(), interval);
        remoteAccess.invoke(PropertiesRepository.class, "flushCache");
    }

    private static void setPropertyExecutor(AsyncProcessor.ExecutorType etype) {
        deletePropertyExecutor();
        baseEntityRepository.executeNativeUpdate("insert into gl_prprp values " +
                        "(?, 'root', 'Y', 'STRING_TYPE', 'Тип ThreadFactory', null, ?, null)",
                AsyncProcessor.MQ_EXECUTOR, etype.name());
        remoteAccess.invoke(PropertiesRepository.class, "flushCache");
    }

    private static void deletePropertyTimeout() {
        baseEntityRepository.executeNativeUpdate("delete from gl_prprp where ID_PRP in (?, ?)", MQ_TIMEOUT.getName(), MQ_TIME_UNIT.getName());
        remoteAccess.invoke(PropertiesRepository.class, "flushCache");
    }

    private static void deletePropertyExecutor() {
        baseEntityRepository.executeNativeUpdate("delete from gl_prprp where ID_PRP in (?)", AsyncProcessor.MQ_EXECUTOR);
        remoteAccess.invoke(PropertiesRepository.class, "flushCache");
    }

    private AuditRecord getAuditError(long idFrom, AuditRecord.LogLevel ... levels ) throws SQLException {
        String level = "";
        if (null != levels && levels.length > 0) {
            level = StringUtils.arrayToString(levels, ",", "'");
        } else
            level = "'Error'";

        return (AuditRecord) baseEntityRepository.selectFirst(AuditRecord.class,
                "from AuditRecord a where a.logCode in ('AccountQuery') and a.logLevel in (" + level + ") and a.id > ?1 ", idFrom);
    }

    public long getJournalMaxId() throws SQLException {
        DataRecord res = baseEntityRepository.selectFirst("select max(ID) from GL_ACLIRQ");
        return null == res ? 0 : res.getLong(0);
    }

    private long getStatistics() throws SQLException {
        DataRecord record = baseEntityRepository.selectOne("select min(id), max(id), count(*), max(status_date) - min(status_date) from gl_aclirq where status_date > " +
                "(select max(sys_time) from gl_audit where message like 'Запуск задачи ''AccountQueryTaskLIRQ%')");
        long cnt = record.getLong(2);
        DataRecord record1 = baseEntityRepository.selectOne("select count(*) from gl_aclirq where id between ? and ? and out like '%Error%'", record.getLong(0), record.getLong(1));
        long ers = record1.getLong(0);
        System.out.println(String.format("Всего записей: %d, ошибок: %d, время: %s", cnt, ers, record.getString(3)));
        Assert.assertEquals("Есть ошибки в ответе", 0, ers);
        return cnt;
    }

    private long getStatistics(long idJ) throws SQLException {
        DataRecord record = baseEntityRepository.selectOne("select min(id), max(id), count(*), max(status_date) - min(status_date) from gl_aclirq where id > ?", idJ);
        long cnt = record.getLong(2);
        System.out.println(String.format("Всего записей: %d, время: %s", cnt, record.getString(3)));
        return cnt;
    }

    public static void runAclirqJob(String props) throws SQLException {
        DataRecord tsk = baseEntityRepository.selectFirst("select * from GL_SCHED_S where TSKNM = ?", ACLIRQ_TASK);
        TimerJob aclirqTaskJob = (TimerJob) baseEntityRepository.selectFirst(TimerJob.class
                , "from TimerJob j where j.name = ?1", ACLIRQ_TASK);
        if(null != aclirqTaskJob &&
                (!aclirqTaskJob.getSchedulingType().equals(JobSchedulingType.INTERVAL)
                        || aclirqTaskJob.getInterval() != 50L
                        || !props.equals(aclirqTaskJob.getProperties())
                )
                || null != tsk) {
            baseEntityRepository.executeUpdate("delete from TimerJob j where j.name = ?1", ACLIRQ_TASK);
            aclirqTaskJob = null;
        }
        if (null == aclirqTaskJob) {
            IntervalJob aclirqJob = new IntervalJob();
            aclirqJob.setDelay(0L);
            aclirqJob.setDescription("AccountQueryTaskLIRQ1");
            aclirqJob.setRunnableClass(AccountQueryTaskMT.class.getName());
            aclirqJob.setProperties(props);
            aclirqJob.setStartupType(MANUAL);
            aclirqJob.setState(STOPPED);
            aclirqJob.setName(ACLIRQ_TASK);
            aclirqJob.setInterval(50L);
            try {
                aclirqTaskJob = (IntervalJob) baseEntityRepository.save(aclirqJob);
            } catch (Exception e) {
                restartSequenceWithTable(IntervalJob.class);
                aclirqTaskJob = (IntervalJob) baseEntityRepository.save(aclirqJob);
            }
        }
        remoteAccess.invoke(BackgroundJobsController.class, "startupJob", aclirqTaskJob );
//            jobService.startupJob(aclirqJob);
//            registerJob(aclirqJob);
    }

    protected static void restartSequenceWithTable(Class<? extends BaseEntity<Long>> entityClass) throws SQLException {
        SequenceGenerator seq = entityClass.getDeclaredAnnotation(SequenceGenerator.class);
        if(null == seq)
            seq = entityClass.getAnnotation(SequenceGenerator.class);
        String seqName = seq.sequenceName();
        Long idSeq = baseEntityRepository.nextId(seqName);
        Long idTable = (Long) baseEntityRepository.selectFirst(entityClass, "select max(t.id) from " + entityClass.getName() + " t");
        if (idSeq < idTable) {

        }
    }

    static String acliquFullTopicTest =
            "<NS1:Envelope xmlns:NS1=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                    "<NS1:Header>\n" +
                    "    <NS2:UCBRUHeaders xmlns:NS2=\"urn:imb:gbo:v2\">\n" +
                    "        <NS2:Audit>\n" +
                    "            <NS2:MessagePath>\n" +
                    "                <NS2:Step>\n" +
                    "                    <NS2:Application.Module>SRVACC.AccountDetailsNotify.NotificationHandler</NS2:Application.Module>\n" +
                    "                    <NS2:VersionId>v4</NS2:VersionId>\n" +
                    "                    <NS2:TimeStamp>2016-03-30T08:15:15.175+03:00</NS2:TimeStamp>\n" +
                    "                    <NS2:RoutingRole>START</NS2:RoutingRole>\n" +
                    "                    <NS2:Comment/>\n" +
                    "                </NS2:Step>\n" +
                    "                <NS2:Step>\n" +
                    "                    <NS2:Application.Module>SRVACC.AccountListQuery</NS2:Application.Module>\n" +
                    "                    <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                    <NS2:TimeStamp>2016-03-30T08:15:15.216+03:00</NS2:TimeStamp>\n" +
                    "                    <NS2:RoutingRole>START</NS2:RoutingRole>\n" +
                    "                    <NS2:Comment/>\n" +
                    "                </NS2:Step>\n" +
                    "                <NS2:Step>\n" +
                    "                    <NS2:Application.Module>SRVACC.AccountListQuery</NS2:Application.Module>\n" +
                    "                    <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                    <NS2:TimeStamp>2016-03-30T08:15:15.422+03:00</NS2:TimeStamp>\n" +
                    "                    <NS2:RoutingRole>SUCCESS</NS2:RoutingRole>\n" +
                    "                    <NS2:Comment/>\n" +
                    "                </NS2:Step>\n" +
                    "                <NS2:Step>\n" +
                    "                    <NS2:Application.Module>SRVACC.AccountDetailsNotify.Publisher</NS2:Application.Module>\n" +
                    "                    <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                    <NS2:TimeStamp>2016-03-30T08:15:15.440+03:00</NS2:TimeStamp>\n" +
                    "                    <NS2:RoutingRole>SUCCESS</NS2:RoutingRole>\n" +
                    "                    <NS2:Comment></NS2:Comment>\n" +
                    "                </NS2:Step>\n" +
                    "            </NS2:MessagePath>\n" +
                    "        </NS2:Audit>\n" +
                    "    </NS2:UCBRUHeaders>\n" +
                    "</NS1:Header>\n" +
                    "<NS1:Body>\n" +
                    "<!-- Midas -->\n" +
                    "    <gbo:AccountList xmlns:gbo=\"urn:imb:gbo:v2\">\n" +
                    "        <gbo:AccountDetails>\n" +
                    "            <gbo:AccountNo>057438RUR401102040</gbo:AccountNo>\n" +
                    "            <gbo:Branch>040</gbo:Branch>\n" +
                    "            <gbo:CBAccountNo>40702810800404496871</gbo:CBAccountNo>\n" +
                    "            <gbo:Ccy>RUR</gbo:Ccy>\n" +
                    "            <gbo:CcyDigital>810</gbo:CcyDigital>\n" +
                    "            <gbo:Description>GENERATSIYA NGO</gbo:Description>\n" +
                    "            <gbo:Status>O</gbo:Status>\n" +
                    "            <gbo:CustomerNo>00057438</gbo:CustomerNo>\n" +
                    "            <gbo:Special>4011</gbo:Special>\n" +
                    "            <gbo:OpenDate>2012-02-03</gbo:OpenDate>\n" +
                    "            <gbo:CreditTransAllowed>Y</gbo:CreditTransAllowed>\n" +
                    "            <gbo:DebitTransAllowed>N</gbo:DebitTransAllowed>\n" +
                    "            <gbo:CorBank>046577971</gbo:CorBank>\n" +
                    "            <gbo:CorINN>6670216662</gbo:CorINN>\n" +
                    "            <gbo:Positioning>\n" +
                    "                <gbo:CBAccount>40702810800404496871</gbo:CBAccount>\n" +
                    "                <gbo:IMBAccountNo>40702810800404496871</gbo:IMBAccountNo>\n" +
                    "                <gbo:IMBBranch>040</gbo:IMBBranch>\n" +
                    "                <gbo:HostABSAccountNo>057438RUR401102040</gbo:HostABSAccountNo>\n" +
                    "                <gbo:HostABSBranch>040</gbo:HostABSBranch>\n" +
                    "                <gbo:HostABS>MIDAS</gbo:HostABS>\n" +
                    "            </gbo:Positioning>\n" +
                    "            <gbo:UDF>\n" +
                    "                <gbo:Name>GWSAccType</gbo:Name>\n" +
                    "                <gbo:Value>CURR</gbo:Value>\n" +
                    "            </gbo:UDF>\n" +
                    "            <gbo:UDF>\n" +
                    "                <gbo:Name>OperationTypeCodes</gbo:Name>\n" +
                    "                <gbo:Value>VIEW=1,DOMPAY=1,DOMTAX=1,DOMCUS=1,CUPADE=1,CUPACO=1,CUSEOD=1,CUSEOC=1,CURBUY=1,CUOPCE=1,\n" +
                    "                </gbo:Value>\n" +
                    "            </gbo:UDF>\n" +
                    "            <gbo:UDF>\n" +
                    "                <gbo:Name>ParentBranchName</gbo:Name>\n" +
                    "                <gbo:Value>UCB, Ekaterinburg Branch</gbo:Value>\n" +
                    "            </gbo:UDF>\n" +
                    "            <gbo:UDF>\n" +
                    "                <gbo:Name>CusSegment</gbo:Name>\n" +
                    "                <gbo:Value>TIER_I</gbo:Value>\n" +
                    "            </gbo:UDF>\n" +
                    "            <gbo:ShadowAccounts>\n" +
                    "                <gbo:HostABS>FCC</gbo:HostABS>\n" +
                    "                <gbo:AccountNo>00057438RURCOSA101</gbo:AccountNo>\n" +
                    "                <gbo:Branch>K01</gbo:Branch>\n" +
                    "            </gbo:ShadowAccounts>\n" +
                    "        </gbo:AccountDetails>\n" +
                    "    </gbo:AccountList>\n" +
                    "</NS1:Body>\n" +
                    "</NS1:Envelope>";
}
