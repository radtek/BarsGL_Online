package ru.rbt.barsgl.ejbtest;

import com.ibm.jms.JMSBytesMessage;
import com.ibm.jms.JMSMessage;
import com.ibm.jms.JMSTextMessage;
import com.ibm.mq.jms.*;
import com.ibm.msg.client.wmq.WMQConstants;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.controller.operday.task.AccountQueryTaskMT;
import ru.rbt.barsgl.ejbcore.mapping.job.IntervalJob;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbcore.mapping.job.TimerJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.barsgl.shared.enums.JobSchedulingType;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.tasks.ejb.job.BackgroundJobsController;

import javax.jms.JMSException;
import javax.jms.Session;
import java.io.*;
import java.sql.Array;
import java.sql.SQLException;
import java.util.logging.Logger;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static ru.rbt.audit.entity.AuditRecord.LogLevel.Error;
import static ru.rbt.audit.entity.AuditRecord.LogLevel.SysError;
import static ru.rbt.audit.entity.AuditRecord.LogLevel.Warning;
import static ru.rbt.barsgl.ejb.common.CommonConstants.ACLIRQ_TASK;
import static ru.rbt.barsgl.ejb.props.PropertyName.ACLIRQ_TIMEOUT;
import static ru.rbt.barsgl.ejb.props.PropertyName.ACLIRQ_TIME_UNIT;
import static ru.rbt.barsgl.ejbcore.mapping.job.TimerJob.JobState.STOPPED;
import static ru.rbt.barsgl.ejbtest.CustomerDetailsNotifyIT.getAuditMaxId;
import static ru.rbt.barsgl.ejbtest.OperdayIT.shutdownJob;
import static ru.rbt.barsgl.shared.enums.JobStartupType.MANUAL;

/**
 * Created by ER22228
 */
public class AccountQueryMPIT extends AbstractTimerJobIT {

    public static final Logger logger = Logger.getLogger(AccountQueryMPIT.class.getName());

    private static final String qType = "LIRQ";
    private final static String host = "vs338";
    private final static String broker = "QM_MBROKER10_TEST";
    private final static String channel= "SYSTEM.DEF.SVRCONN";
//    private final static String cudenoIn = "UCBRU.ADP.BARSGL.V3.CUDENO.NOTIF";
    private final static String acliquIn = "UCBRU.ADP.BARSGL.ACLIQU.REQUEST";
    private final static String acliquOut = "UCBRU.ADP.BARSGL.ACLIQU.RESPONSE";
    private final static String acdenoF = "UCBRU.ADP.BARSGL.V5.ACDENO.FCC.NOTIF";
    private final static String acdenoM = "UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF";
    private final static String cudenoIn = "UCBRU.ADP.BARSGL.V3.CUDENO.NOTIF";
    private static final String login = "srvwbl4mqtest";
    private static final String passw = "UsATi8hU";
    private static final boolean writeOut = true;

    @After
    public void after() {
        setPropertyTimeout("MINUTES", 10);
    }

    private String getQProperty (String topic, String ahost, String abroker, String alogin, String apassw) {
        return getQueueProperty (topic, acliquIn, acliquOut, ahost, "1414", abroker, "SYSTEM.DEF.SVRCONN", alogin, apassw, "30", writeOut);
    }

    public static MQQueueConnectionFactory getConnectionFactory(String ahost, String abroker, String achannel) throws JMSException {
        MQQueueConnectionFactory cf = new MQQueueConnectionFactory();

        cf.setHostName(ahost);
        cf.setPort(1414);
        cf.setTransportType(WMQConstants.WMQ_CM_CLIENT);
        cf.setQueueManager(abroker);
        cf.setChannel(achannel);
        return cf;
    }

    public static String getQueueProperty (String topic, String inQueue, String outQueue, String ahost, String aport, String abroker, String achannel, String alogin, String apassw, String batchSize, boolean writeOut) {
        return  "mq.type = queue\n"
                + "mq.host = " + ahost + "\n"
                + "mq.port = " + aport + "\n"
                + "mq.queueManager = " + abroker + "\n"
                + "mq.channel = " + achannel + "\n"
                + "mq.batchSize = " + batchSize + "\n"
                + "mq.topics = " + topic + ":" + inQueue + (StringUtils.isEmpty(outQueue) ? "" : ":" + outQueue) + "\n"
                + "mq.user=" + alogin + "\n"
                + "mq.password=" + apassw +"\n"
                + "unspents=show\n"
                + "writeOut=" + writeOut +"\n"
                + "writeSleepThreadTime=true\n"

        ;
    }

    @Test
    public void testA() throws Exception {

        MQQueueConnectionFactory cf = getConnectionFactory(host, broker, channel);
        clearQueue(cf, acliquIn, login, passw, 1000);
        clearQueue(cf, acliquOut, login, passw, 1000);

//        sendToQueue(cf,"UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF", AccountQueryProcessor.fullTopicTestA);
//        sendToQueue(cf,"UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF", AccountQueryBAProcessor.fullTopicTestB);

//        sendToQueue(cf, acdenoF, new File(this.getClass().getResource("/AccountQueryProcessorTest.xml").getFile()), acdenoM, login, passw);
        sendToQueue(cf, acliquIn, new File(this.getClass().getResource("/AccountQueryProcessorTest.xml").getFile()), acliquOut, login, passw);

        Thread.sleep(1000L);
        SingleActionJob job =
                SingleActionJobBuilder.create()
                        .withClass(AccountQueryTaskMT.class)
                        .withName("AccountQueryTaskMT_A")
                        .withProps(getQueueProperty (qType, acliquIn, acliquOut, host, "1414", broker, channel, login, passw, "30", writeOut))
                        .build();
        jobService.executeJob(job);

        Thread.sleep(4000L);
        String answer = receiveFromQueue(cf, acliquOut, login, passw);
        Assert.assertFalse(StringUtils.isEmpty(answer));
        Assert.assertFalse(answer.contains("Error"));
        System.out.println("\nReceived message from " + acliquOut + ":\n" + answer);
        System.out.println();

    }

    @Test
    public void testStressError() throws Exception {

        int cnt = 30;
        int cntmax = 5000;

        MQQueueConnectionFactory cf = getConnectionFactory(host, broker, channel);
        clearQueue(cf, acliquIn, login, passw, cntmax);
        clearQueue(cf, acliquOut, login, passw, cntmax);

        sendToQueue(cf, acliquIn, new File(this.getClass().getResource("/AccountQueryProcessorTest.xml").getFile()), acliquOut, login, passw, cnt);

        long idAudit = getAuditMaxId();

        setPropertyTimeout("SECONDS", 5);
        Thread.sleep(5000L);
        SingleActionJob job =
                SingleActionJobBuilder.create()
                        .withClass(AccountQueryTaskMT.class)
                        .withName("AccountQueryTaskMT_A")
                        .withProps(getQueueProperty (qType, acliquIn, acliquOut, host, "1414", broker, channel, login, passw, "30", writeOut))
                        .build();
        jobService.executeJob(job);

        Thread.sleep(6000L);
        int n = clearQueue(cf, acliquOut, login, passw, cntmax);
        Assert.assertTrue(n < cnt);

        AuditRecord record = getAuditError(idAudit, Error);
        Assert.assertNotNull("Нет записи об ошибке в аудит", record);
        Assert.assertTrue("Нет записи об ошибке в аудит", record.getErrorMessage().contains("Код ошибки '3018'"));
    }

    @Test
    public void testStress500() throws Exception {

        int cnt = 500;
        int cntmax = 5000;

        shutdownJob(ACLIRQ_TASK);

        MQQueueConnectionFactory cf = getConnectionFactory(host, broker, channel);
        clearQueue(cf, acliquIn, login, passw, cntmax);
        clearQueue(cf, acliquOut, login, passw, cntmax);

        // длинный тест
//        sendToQueue(cf, acliquIn, new File(this.getClass().getResource("/AccountQueryProcessorTest.xml").getFile()), acliquOut, login, passw, cnt);
        // короткий тест
        sendToQueue(cf, acliquIn, new File(this.getClass().getResource("/AccountQueryProcessorTest_1.xml").getFile()), acliquOut, login, passw, cnt);

        long idAudit = getAuditMaxId();

        setPropertyTimeout("MINUTES", 10);
        runAclirqJob(getQueueProperty (qType, acliquIn, acliquOut, host, "1414", broker, channel, login, passw, "30", writeOut));
        Thread.sleep(60 * 1000L);
        shutdownJob(ACLIRQ_TASK);

        int n = clearQueue(cf, acliquOut, login, passw, cntmax);
        Assert.assertTrue(n >= cnt);
        Assert.assertNull("Есть записи об ошибке в аудит", getAuditError(idAudit, SysError, Error, Warning));

        Assert.assertTrue(getStatistics() == n);
        // static 31.046  old 37.191
    }

    @Test
    public void testStress5000() throws Exception {

        int cnt = 5000;
        int cntmax = cnt * 2;

        shutdownJob(ACLIRQ_TASK);

        MQQueueConnectionFactory cf = getConnectionFactory(host, broker, channel);
        clearQueue(cf, acliquIn, login, passw, cntmax);
        clearQueue(cf, acliquOut, login, passw, cntmax);

        long idAudit = getAuditMaxId();
        sendToQueue(cf, acliquIn, new File(this.getClass().getResource("/AccountQueryProcessorTest_1.xml").getFile()), acliquOut, login, passw, cnt);

        setPropertyTimeout("MINUTES", 10);
        runAclirqJob(getQueueProperty (qType, acliquIn, acliquOut, host, "1414", broker, channel, login, passw, "30", writeOut));

        long cntrec = 0;
        while(cntrec < cnt) {
            Thread.sleep(60 * 1000L);
            cntrec  = getStatistics();
        }
        Assert.assertNull("Есть записи об ошибке в аудит", getAuditError(idAudit, SysError, Error, Warning));

        shutdownJob(ACLIRQ_TASK);
        int n = clearQueue(cf, acliquOut, login, passw, cntmax);
        // new   6:12.409
        // old   6:47.807
    }

    @Test
    @Ignore
    public void testLocal() throws Exception {
        // SYSTEM.DEF.SVRCONN/TCP/vs338(1414)
        // SYSTEM.ADMIN.SVRCONN/TCP/vs338(1414)
        // UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF
//        final String inQueue = "UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF";
//        final String outQueue = "UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF";
        final String inQueue = "UCBRU.ADP.BARSGL.ACLIQU.REQUEST";
        final String outQueue = "UCBRU.ADP.BARSGL.ACLIQU.RESPONSE";

        MQQueueConnectionFactory cf = new MQQueueConnectionFactory();


        // Config

        cf.setHostName("vs338");
        cf.setPort(1414);
        cf.setTransportType(WMQConstants.WMQ_CM_CLIENT);
        cf.setQueueManager("QM_MBROKER10_TEST");
        cf.setChannel("SYSTEM.DEF.SVRCONN");
/*
mq.type = queue
mq.host = vs529
mq.port = 1414
mq.queueManager = QM_MBROKER4_T5
mq.channel = SYSTEM.ADMIN.SVRCONN
mq.batchSize = 30
mq.topics = LIRQ:UCBRU.ADP.BARSGL.V2.ACLIQU.REQUEST:UCBRU.ADP.BARSGL.V2.ACLIQU.RESPONSE;BALIRQ:UCBRU.ADP.BARSGL.V3.ACBALIQU.REQUEST:UCBRU.ADP.BARSGL.V3.ACBALIQU.RESPONSE
mq.user=srvwbl4mqtest
mq.password=UsATi8hU
 */
        String vugluskr9 = "Vugluskr4";


        sendToQueue(cf, inQueue,
                new File(this.getClass().getResource("/AccountQueryProcessorTest.xml").getFile()),
                outQueue,"er22228",vugluskr9);


//        sendToQueue(cf,"UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF", AccountQueryProcessor.fullTopicTestA);
//        sendToQueue(cf,"UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF", AccountQueryBAProcessor.fullTopicTestB);

        // 00375106 //00200428

//        sendToQueue(cf, "UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF", new File("C:\\Projects\\task53\\AccountListQuerySh2.xml"),"UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF","er22228",vugluskr9);
//                sendToQueue(cf, "UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF", new File("C:\\Projects\\task53\\AccountListQuery-NoBody.xml"),"UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF","er22228",vugluskr9);
//        sendToQueue(cf, "UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF", new File("C:\\Projects\\task53\\AccountBalanceListQuery-B4.xml"), "UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF", "er22228", vugluskr9);

//        sendToQueue(cf, "UCBRU.ADP.BARSGL.V2.ACLIQU.FCC.NOTIF",new File("C:\\Projects\\task53\\AccountListQuery-Simple.xml"),"UCBRU.ADP.BARSGL.V2.ACLIQU.RESPONSE","er22228",vugluskr9);

//        sendToQueue(cf, "UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF",new File("C:\\Projects\\task53\\AccountBalanceListQuery-B1"),"UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF","er22228", vugluskr9);
//        sendToQueue(cf, "UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF",new File("C:\\Projects\\task53\\AccountBalanceListQuery-B2"),"UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF","er22228",vugluskr9);
//        sendToQueue(cf, "UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF", new File("C:\\Projects\\task53\\AccountBalanceListQuery-B3.txt"),"UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF","er22228",vugluskr9);
//        sendToQueue(cf, "UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF",new File("C:\\Projects\\task53\\AccountListQuery-Over100000.xml"));

        SingleActionJob job =
                SingleActionJobBuilder.create()
                        .withClass(AccountQueryTaskMT.class)
                        .withName("AccountQuery5")
                        .withProps(
                                "mq.batchSize = 30\n" + //todo
                                        "mq.host = vs338\n" +
                                        "mq.port = 1414\n" +
                                        "mq.queueManager = QM_MBROKER10_TEST\n" +
                                        "mq.channel = SYSTEM.DEF.SVRCONN\n" +
                                        "mq.topics = " +
                                        "LIRQ:" + inQueue + ":" + outQueue + "\n" +     // LIRQ:UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF:UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF
//                        "BALIRQ:UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF:UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF\n" +
//                        "MAPBRQ:UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF:UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF\n" +
//                        "LIRQ:UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF:UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF" +
//                        ";BALIRQ:UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF:UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF\n" +
//                        ";MAPBRQ:UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF:UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF\n" +
//                        ";BALIRQ:UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF:UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF\n" +
                                        "mq.user=er22228\n" +
                                        "mq.password="+vugluskr9+"\n"+
                                        "unspents=show\n"+
                                        "writeOut=true"
                        )
                        .build();
        jobService.executeJob(job);

        receiveFromQueue(cf, outQueue, "er22228", vugluskr9);
//        receiveFromQueue(cf,"UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF","er22228","Vugluskr7");
        System.out.println();

    }

    @Test
    @Ignore
    public void testLocalStress() throws Exception {
        //System.setProperty("com.ibm.msg.client.commonservices.trace.status", "ON");
        
        MQQueueConnectionFactory cf = new MQQueueConnectionFactory();

        // Config
        cf.setHostName("localhost");
        cf.setPort(1414);
        cf.setTransportType(WMQConstants.WMQ_CM_CLIENT);
        cf.setQueueManager("QM_MBROKER4_T4");
        cf.setChannel("SYSTEM.DEF.SVRCONN");
        
        int count = 1000;
        //*
        for (int i = 0; i < count; i++) {        
        sendToQueue(cf, 
//                "UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF", 
//                new File(this.getClass().getResource("/MasterAccountPositioningBatchQuery_01_req.xml").getFile()),
//                "UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF","","");
                
//                "UCBRU.ADP.BARSGL.MAACPOBAQU.REQUEST",
//                new File(this.getClass().getResource("/MasterAccountPositioningBatchQuery_01_req.xml").getFile()),
//                "UCBRU.ADP.BARSGL.MAACPOBAQU.RESPONSE","","");

                "UCBRU.ADP.BARSGL.ACLIQU.REQUEST",
                new File(this.getClass().getResource("/AccountQueryProcessorTest.xml").getFile()),
                "UCBRU.ADP.BARSGL.ACLIQU.RESPONSE","","");
        }
        
/*
     SingleActionJob job =
            SingleActionJobBuilder.create()
                .withClass(AccountQueryTaskMT.class)
                .withName("AccountQuery5")
                .withProps(                        
                    "mq.batchSize = 100\n" + //todo
                        "mq.host = localhost\n" +
                        "mq.port = 1414\n" +
                        "mq.queueManager = QM_MBROKER4_T4\n" +
                        "mq.channel = SYSTEM.DEF.SVRCONN\n" +
                        "mq.topics = " +
//                        "LIRQ:UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF:UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF\n" +
                        "LIRQ:UCBRU.ADP.BARSGL.ACLIQU.REQUEST:UCBRU.ADP.BARSGL.ACLIQU.RESPONSE\n" +
//                        "MAPBRQ:UCBRU.ADP.BARSGL.MAACPOBAQU.REQUEST:UCBRU.ADP.BARSGL.MAACPOBAQU.RESPONSE\n" +
//                        "BALIRQ:UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF:UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF\n" +
                        "mq.user=\n" +
                        "mq.password=\n"+ 
//                        "writeOut=true\n"+
                        "unspents=show"
                )
                .build();
        jobService.executeJob(job);
        for (int i = 0; i < count; i++) {        
          receiveFromQueue(cf, "UCBRU.ADP.BARSGL.ACLIQU.RESPONSE", "", "");
//        receiveFromQueue(cf, "UCBRU.ADP.BARSGL.MAACPOBAQU.RESPONSE", "", "");
        }
//*/
        System.out.println();

    }

    private static void setPropertyTimeout(String unit, int interval) {
        deletePropertyTimeout();
        baseEntityRepository.executeNativeUpdate("insert into gl_prprp values " +
                        "(?, 'root', 'Y', 'STRING_TYPE', 'Единицы времени обработки сообщений из очереди ACLIRQ', null, ?, null)",
                ACLIRQ_TIME_UNIT.getName(), unit);
        baseEntityRepository.executeNativeUpdate("insert into gl_prprp values " +
                        "(?, 'root', 'Y', 'NUMBER_TYPE', 'Макс. время обработки сообщений из очереди ACLIRQ', null, null, ?)",
                ACLIRQ_TIMEOUT.getName(), interval);
        remoteAccess.invoke(PropertiesRepository.class, "flushCache");
    }

    private static void deletePropertyTimeout() {
        baseEntityRepository.executeNativeUpdate("delete from gl_prprp where ID_PRP in (?, ?)", ACLIRQ_TIMEOUT.getName(), ACLIRQ_TIME_UNIT.getName());
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

    private int clearQueue(MQQueueConnectionFactory cf, String queueName, String username, String password, int count) throws JMSException {
        MQQueueConnection connection = (MQQueueConnection) cf.createQueueConnection(username, password);
        MQQueueSession session = (MQQueueSession) connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        MQQueue queue = (MQQueue) session.createQueue("queue:///" + queueName);//UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF
//        MQQueueSender sender = (MQQueueSender) session.createSender(queue);
        MQQueueReceiver receiver = (MQQueueReceiver) session.createReceiver(queue);

        connection.start();

//        JMSMessage receivedMessage = (JMSMessage) receiver.receive(100);
//        System.out.println("\\nReceived message:\\n" + receivedMessage);
        int i=0;
        for (; i<count; i++) {
            JMSMessage message = (JMSMessage) receiver.receiveNoWait();
            if (null == message)
                break;
//            System.out.println("DeliveryTime=" + message.getJMSTimestamp() + " MessageID=" + message.getJMSMessageID());
        }
        System.out.println("Deleted from " + queueName + ": " + i);

//        sender.close();
        receiver.close();
        session.close();
        connection.close();

        return i;
    }

    private String receiveFromQueue(MQQueueConnectionFactory cf, String queueName, String username, String password) throws JMSException {
        MQQueueConnection connection = (MQQueueConnection) cf.createQueueConnection(username, password);
        MQQueueSession session = (MQQueueSession) connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        MQQueue queue = (MQQueue) session.createQueue("queue:///" + queueName);//UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF
//        MQQueueSender sender = (MQQueueSender) session.createSender(queue);
        MQQueueReceiver receiver = (MQQueueReceiver) session.createReceiver(queue);

        connection.start();

//        JMSMessage receivedMessage = (JMSMessage) receiver.receive(100);
//        System.out.println("\\nReceived message:\\n" + receivedMessage);

        String answer = readFromJMS(receiver);
//        System.out.println("\nReceived message from " + queueName + ":\n" + answer);

//        sender.close();
        receiver.close();
        session.close();
        connection.close();
        return answer;
    }

    private String readFromJMS(MQMessageConsumer receiver) throws JMSException {
        JMSMessage receivedMessage = (JMSMessage) receiver.receive(100);
//        receivedMessage.acknowledge();
        String textMessage = null;
        if (receivedMessage instanceof JMSTextMessage) {
            textMessage = ((JMSTextMessage) receivedMessage).getText();
        } else if (receivedMessage instanceof JMSBytesMessage) {
            JMSBytesMessage bytesMessage = (JMSBytesMessage) receivedMessage;

            int length = (int) bytesMessage.getBodyLength();
            byte[] incomingBytes = new byte[length];
            bytesMessage.readBytes(incomingBytes);

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(incomingBytes);
            try (Reader r = new InputStreamReader(byteArrayInputStream, "UTF-8")) {
                StringBuilder sb = new StringBuilder();
                char cb[] = new char[1024];
                int s = r.read(cb);
                while (s > -1) {
                    sb.append(cb, 0, s);
                    s = r.read(cb);
                }
                textMessage = sb.toString();
            } catch (IOException e) {
                System.out.println("Error during read message from QUEUE");
            }
        }
        return textMessage;
    }

    public static void sendToQueue(MQQueueConnectionFactory cf, String queueName, File file, String replyToQ, String username, String password) throws JMSException {
        sendToQueue (cf, queueName, file, replyToQ, username, password, 1);
    }

    public static void sendToQueue(MQQueueConnectionFactory cf, String queueName, File file, String replyToQ, String username, String password, int cnt) throws JMSException {
        byte[] incomingMessage = null;
        try {
            incomingMessage = FileUtils.readFileToByteArray(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (isEmpty(incomingMessage)) {
            System.exit(1);
        }

        MQQueueConnection connection = (MQQueueConnection) cf.createQueueConnection(username, password);
        MQQueueSession session = (MQQueueSession) connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        MQQueue queue = (MQQueue) session.createQueue("queue:///" + queueName);//UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF
        MQQueueSender sender = (MQQueueSender) session.createSender(queue);
        MQQueueReceiver receiver = (MQQueueReceiver) session.createReceiver(queue);

        connection.start();

        JMSBytesMessage bytesMessage = (JMSBytesMessage) session.createBytesMessage();
        bytesMessage.writeBytes(incomingMessage);
        if (!StringUtils.isEmpty(replyToQ)) {
            MQQueue queueR2Q = (MQQueue) session.createQueue("queue:///" + replyToQ);
            bytesMessage.setJMSReplyTo(queueR2Q);
        }
        for(int i=0; i<cnt; i++)
            sender.send(bytesMessage);
        System.out.println(String.format("Sent %d message to %s", cnt, queueName));

        sender.close();
        receiver.close();
        session.close();
        connection.close();
    }

    private void sendToQueue(MQQueueConnectionFactory cf, String queueName, String fullTopicTest) throws JMSException {
        MQQueueConnection connection = (MQQueueConnection) cf.createQueueConnection();
        MQQueueSession session = (MQQueueSession) connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        MQQueue queue = (MQQueue) session.createQueue("queue:///" + queueName);//UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF
        MQQueueSender sender = (MQQueueSender) session.createSender(queue);
        MQQueueReceiver receiver = (MQQueueReceiver) session.createReceiver(queue);

        connection.start();

        JMSTextMessage message = (JMSTextMessage) session.createTextMessage(fullTopicTest);
        sender.send(message);
        System.out.println("Sent message:\\n" + message);

        sender.close();
        receiver.close();
        session.close();
        connection.close();
    }

    public static void runAclirqJob(String props) {
        TimerJob aclirqTaskJob = (TimerJob) baseEntityRepository.selectFirst(TimerJob.class
                , "from TimerJob j where j.name = ?1", ACLIRQ_TASK);
        if(null != aclirqTaskJob &&
                (!aclirqTaskJob.getSchedulingType().equals(JobSchedulingType.INTERVAL)
                        || aclirqTaskJob.getInterval() != 50L
                        || !props.equals(aclirqTaskJob.getProperties())
                )) {
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
            aclirqTaskJob = (IntervalJob) baseEntityRepository.save(aclirqJob);

        }
        remoteAccess.invoke(BackgroundJobsController.class, "startupJob", aclirqTaskJob );
//            jobService.startupJob(aclirqJob);
//            registerJob(aclirqJob);
    }

    static String fullTopicTest =
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
