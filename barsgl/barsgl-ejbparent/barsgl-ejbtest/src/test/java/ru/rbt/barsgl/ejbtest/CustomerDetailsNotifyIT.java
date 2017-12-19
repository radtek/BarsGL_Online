package ru.rbt.barsgl.ejbtest;

import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.controller.operday.task.CustomerDetailsNotifyTask;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.CustomerNotifyQueueController;
import ru.rbt.barsgl.ejb.entity.cust.CustDNInput;
import ru.rbt.barsgl.ejb.entity.cust.CustDNJournal;
import ru.rbt.barsgl.ejb.entity.cust.CustDNMapped;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.ejbcore.datarec.DataRecord;
import sun.net.www.protocol.file.FileURLConnection;

import javax.jms.JMSException;
import java.io.File;
import java.sql.SQLException;

import static ru.rbt.barsgl.ejb.entity.cust.CustDNJournal.Status.*;
import static ru.rbt.barsgl.ejbtest.AccountQueryMPIT.sendToQueue;

/**
 * Created by er18837 on 14.12.2017.
 */
public class CustomerDetailsNotifyIT extends AbstractTimerJobIT {

//    private final String host = "vs569";
//    private final String broker = "QM_MBROKER4_T4";

    private final static String host = "vs338";
    private final static String broker = "QM_MBROKER10_TEST";
    private final static String channel= "SYSTEM.DEF.SVRCONN";
    private final static String inQueue = "UCBRU.ADP.BARSGL.V3.CUDENO.NOTIF";
//    private final static String inQueue = "UCBRU.ADP.BARSGL.ACLIQU.REQUEST";
//    private final static String outQueue = "UCBRU.ADP.BARSGL.ACLIQU.RESPONSE";
    public static final String login = "srvwbl4mqtest";
    public static final String passw = "UsATi8hU";

    public static final String qType = "CUST";

    @Test
    public void testReadQueue() throws Exception {
        long idAudit = getAuditMaxId();
        remoteAccess.invoke(CustomerNotifyQueueController.class, "closeConnection");

        SingleActionJob job =
                SingleActionJobBuilder.create()
                        .withClass(CustomerDetailsNotifyTask.class)
                        .withName("CustomerNotify1")
                        .withProps(getProperty (qType, host, broker, login, passw))
                        .build();
        jobService.executeJob(job);

        Thread.sleep(2000L);
        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));
    }

    @Test
    public void testLoadParams() throws Exception {

        long idAudit = getAuditMaxId();
        long idCudeno = getCudenoMaxId();
        remoteAccess.invoke(CustomerNotifyQueueController.class, "closeConnection");

        MQQueueConnectionFactory cf = getConnectionFactory(host, broker, channel);

        sendToQueue(cf, inQueue,
                new File(this.getClass().getResource("/CustomerDetailsTest_1.xml").getFile()),
                null, login, passw);

        SingleActionJob job =
                SingleActionJobBuilder.create()
                        .withClass(CustomerDetailsNotifyTask.class)
                        .withName("CustomerNotify2")
                        .withProps(getProperty (qType, host, broker, login, passw))
                        .build();
        jobService.executeJob(job);

        Thread.sleep(2000L);
        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));

        CustDNJournal journal = getCudenoNewRecord(idCudeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_CUDENO1", journal);
        Assert.assertEquals(MAPPED, journal.getStatus());

        Assert.assertNotNull("Нет записи в таблице GL_CUDENO2", getCudenoInput(journal.getId()));
        Assert.assertNotNull("Нет записи в таблице GL_CUDENO3", getCudenoMapped(journal.getId()));
    }
    @Test

    public void testNoChangeCustomer() throws Exception {

        long idAudit = getAuditMaxId();
        long idCudeno = getCudenoMaxId();

        String textMessage = FileUtils.readFileToString(new File(this.getClass().getResource("/CustomerDetailsTest_1.xml").getFile()), "UTF-8");

        // Long processing(String queueType, String[] incMessage, String toQueue, long receiveTime, long waitingTime) throws Exception {
        remoteAccess.invoke(CustomerNotifyQueueController.class, "processing", qType, new String[] {textMessage}, null, -1, -1);

        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));

        CustDNJournal journal = getCudenoNewRecord(idCudeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_CUDENO1", journal);
        Assert.assertEquals(PROCESSED, journal.getStatus());

        Assert.assertNotNull("Нет записи в таблице GL_CUDENO2", getCudenoInput(journal.getId()));
        Assert.assertNotNull("Нет записи в таблице GL_CUDENO3", getCudenoMapped(journal.getId()));
    }

        @Test
    public void testValidationError() throws Exception {

        long idAudit = getAuditMaxId();
        long idCudeno = getCudenoMaxId();

        String textMessage = FileUtils.readFileToString(new File(this.getClass().getResource("/CustomerDetailsTest_err1.xml").getFile()), "UTF-8");

        // Long processing(String queueType, String[] incMessage, String toQueue, long receiveTime, long waitingTime) throws Exception {
        remoteAccess.invoke(CustomerNotifyQueueController.class, "processing", qType, new String[] {textMessage}, null, -1, -1);

        Thread.sleep(2000L);
        Assert.assertNotNull("Нет записи об ошибке в аудит", getAuditError(idAudit));

        CustDNJournal journal = getCudenoNewRecord(idCudeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_CUDENO1", journal);
        Assert.assertEquals(ERR_VAL, journal.getStatus());
        System.out.println(journal.getComment());
    }

    @Test
    public void testMappedError() throws Exception {

        long idAudit = getAuditMaxId();
        long idCudeno = getCudenoMaxId();

        String textMessage = FileUtils.readFileToString(new File(this.getClass().getResource("/CustomerDetailsTest_err2.xml").getFile()), "UTF-8");

        // Long processing(String queueType, String[] incMessage, String toQueue, long receiveTime, long waitingTime) throws Exception {
        remoteAccess.invoke(CustomerNotifyQueueController.class, "processing", qType, new String[] {textMessage}, null, -1, -1);

        Thread.sleep(2000L);
        AuditRecord auditRecord = getAuditError(idAudit);
        Assert.assertNotNull("Нет записи об ошибке в аудит", auditRecord);

        CustDNJournal journal = getCudenoNewRecord(idCudeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_CUDENO1", journal);
        Assert.assertEquals(ERR_MAP, journal.getStatus());
        System.out.println(journal.getComment());

        Assert.assertNotNull("Нет записи в таблице GL_CUDENO2", getCudenoInput(journal.getId()));
    }

    private MQQueueConnectionFactory getConnectionFactory(String ahost, String abroker, String achannel) throws JMSException {
        MQQueueConnectionFactory cf = new MQQueueConnectionFactory();

        cf.setHostName(ahost);
        cf.setPort(1414);
        cf.setTransportType(WMQConstants.WMQ_CM_CLIENT);
        cf.setQueueManager(abroker);
        cf.setChannel(achannel);
        return cf;
    }

    private String getProperty (String topic, String ahost, String abroker, String alogin, String apassw) {
        return  "mq.batchSize = 30\n"  //todo
                + "mq.host = " + ahost + "\n"
                + "mq.port = 1414\n"
                + "mq.queueManager = " + abroker + "\n"
                + "mq.channel = SYSTEM.DEF.SVRCONN\n"
                + "mq.topics = " + topic + ":" + inQueue + "\n"   // + ":" + outQueue
                + "mq.user=" + alogin + "\n"
                + "mq.password=" + apassw +"\n"
//                                        + "unspents=show\n"
//                                        + "writeOut=true"
            ;
    }

    private long getCudenoMaxId() throws SQLException {
        DataRecord res = baseEntityRepository.selectFirst("select max(MESSAGE_ID) from GL_CUDENO1");
        return null == res ? 0 : res.getLong(0);
    }

    private CustDNJournal getCudenoNewRecord(long idFrom) throws SQLException {
        return (CustDNJournal) baseEntityRepository.selectFirst(CustDNJournal.class, "from CustDNJournal j where j.id > ?1", idFrom);
    }

    private CustDNInput getCudenoInput(long id) throws SQLException {
        return (CustDNInput) baseEntityRepository.selectFirst(CustDNInput.class, "from CustDNInput j where j.id = ?1", id);
    }

    private CustDNMapped getCudenoMapped(long id) throws SQLException {
        return (CustDNMapped) baseEntityRepository.selectFirst(CustDNMapped.class, "from CustDNMapped j where j.id = ?1", id);
    }

    private long getAuditMaxId() throws SQLException {
        DataRecord res = baseEntityRepository.selectFirst("select max(ID_RECORD) from GL_AUDIT");
        return null == res ? 0 : res.getLong(0);
    }

    private AuditRecord getAuditError(long idFrom ) throws SQLException {
        return (AuditRecord) baseEntityRepository.selectFirst(AuditRecord.class,
                "from AuditRecord a where a.logCode in ('CustomerDetailsNotify', 'QueueProcessor') and a.logLevel <> 'Info' and a.id > ?1 ", idFrom);
    }
}
