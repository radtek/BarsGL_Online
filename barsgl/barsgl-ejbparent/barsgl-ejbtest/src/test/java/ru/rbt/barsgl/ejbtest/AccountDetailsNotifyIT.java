package ru.rbt.barsgl.ejbtest;

import com.ibm.jms.JMSTextMessage;
import com.ibm.mq.jms.*;
import com.ibm.msg.client.wmq.WMQConstants;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.operday.task.AccountDetailsNotifyTask;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.AccountDetailsNotifyProcessor;
import ru.rbt.barsgl.ejb.entity.acc.AcDNJournal;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.ejbcore.datarec.DataRecord;

import javax.jms.JMSException;
import javax.jms.Session;
import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Logger;

import static junit.framework.TestCase.assertTrue;
import static ru.rbt.ejbcore.util.DateUtils.getDatabaseDate;
import static ru.rbt.ejbcore.util.DateUtils.getFinalDateStr;

/**
 * Created by ER22228
 */
public class AccountDetailsNotifyIT extends AbstractTimerJobIT {

    public static final Logger logger = Logger.getLogger(AccountDetailsNotifyIT.class.getName());

    public static final String MBROKER = "QM_MBROKER10_TEST";

    public static final String HOST_NAME = "vs338";
    public static final String USERNAME = "srvwbl4mqtest";   //"er22228";
    public static final String PASSWORD = "UsATi8hU";   //"Vugluskr4";

//    public static final String HOST_NAME = "localhost";
//    public static final String USERNAME = "";
//    public static final String PASSWORD = "";

    @Test
    @Ignore
    public void testNotifyClose() throws Exception {
        // SYSTEM.DEF.SVRCONN/TCP/vs338(1414)
        // SYSTEM.ADMIN.SVRCONN/TCP/vs338(1414)
        // UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF

        baseEntityRepository.executeNativeUpdate("delete from accrln where bsaacid='40702810400154748352'");
        baseEntityRepository.executeNativeUpdate("delete from bsaacc where id='40702810400154748352'");
        baseEntityRepository.executeNativeUpdate("delete from acc where id='00695430RUR401102097'");


        putMessageInQueue("UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF", notifyFC12Close);

        SingleActionJob job =
                SingleActionJobBuilder.create()
                        .withClass(AccountDetailsNotifyTask.class)
                        .withProps(
//                     queue|topic
                                "mq.type = queue\n" +
                                        "mq.algo = simple\n" +
                                        "mq.host = " + HOST_NAME + "\n" +
                                        "mq.port = 1414\n" +
                                        "mq.queueManager = " + MBROKER + "\n" +
                                        "mq.channel = SYSTEM.DEF.SVRCONN\n" +
                                        "mq.batchSize = 7\n" +
                                        "mq.topics = FC12:UCBRU.ADP.BARSGL.V5.ACDENO.FCC12.NOTIF\n" +
                                        "mq.user=" + USERNAME + "\n" +
                                        "mq.password=" + PASSWORD + ""
                        )// MIDAS_OPEN:UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF;FCC:UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF
                        .build();
        jobService.executeJob(job);


    }

    @Test
    @Ignore
    public void testMidasDev() throws Exception {
        // SYSTEM.DEF.SVRCONN/TCP/vs338(1414)
        // SYSTEM.ADMIN.SVRCONN/TCP/vs338(1414)
        // UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF

        baseEntityRepository.executeNativeUpdate("delete from accrln where bsaacid='40702810400154748352'");
        baseEntityRepository.executeNativeUpdate("delete from bsaacc where id='40702810400154748352'");
        baseEntityRepository.executeNativeUpdate("delete from acc where id='00695430RUR401102097'");


        putMessageInQueue("UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF", fullTopicTestMidasOld);

        SingleActionJob job =
                SingleActionJobBuilder.create()
                        .withClass(AccountDetailsNotifyTask.class)
                        .withProps(
//                     queue|topic
                                "mq.type = queue\n" +
                                        "mq.algo = simple\n" +
                                        "mq.host = " + HOST_NAME + "\n" +
                                        "mq.port = 1414\n" +
                                        "mq.queueManager = " + MBROKER + "\n" +
                                        "mq.channel = SYSTEM.DEF.SVRCONN\n" +
                                        "mq.batchSize = 7\n" +
                                        "mq.topics = FCC:UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF;MIDAS_OPEN:UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF\n" +
                                        "mq.user=" + USERNAME + "\n" +
                                        "mq.password=" + PASSWORD + ""
                        )// MIDAS_OPEN:UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF;FCC:UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF
                        .build();
        jobService.executeJob(job);


    }


    @Test
    public void testFCCnoCustomer() throws Exception {
        baseEntityRepository.executeNativeUpdate("delete from accrln where bsaacid='40817810250300081806'");
        baseEntityRepository.executeNativeUpdate("delete from bsaacc where id='40817810250300081806'");
        baseEntityRepository.executeNativeUpdate("delete from acc where id='02263713RUR000099030'");

        remoteAccess.invoke(AccountDetailsNotifyTask.class, "processOneMessage", AcDNJournal.Sources.FCC, AccountDetailsNotifyProcessor.messageFCCNoCustomer, null);

        assertTrue(null != baseEntityRepository.selectFirst("select * from accrln where bsaacid=?", "40817810250300081806"));
        assertTrue(null != baseEntityRepository.selectFirst("select * from bsaacc where id=?", "40817810250300081806"));
        assertTrue(null != baseEntityRepository.selectFirst("select * from acc where id=?", "02263713RUR000099030"));
    }

    @Test
    public void testFCCShadow() throws Exception {

        baseEntityRepository.executeNativeUpdate("delete from accrln where bsaacid='40817840250010046747'");
        baseEntityRepository.executeNativeUpdate("delete from bsaacc where id='40817840250010046747'");
        baseEntityRepository.executeNativeUpdate("delete from acc where id='02263713RUR000099030'");

        remoteAccess.invoke(AccountDetailsNotifyTask.class, "processOneMessage", AcDNJournal.Sources.FCC, AccountDetailsNotifyProcessor.messageFCCShadow, null);

        assertTrue(null == baseEntityRepository.selectFirst("select * from accrln where bsaacid=?", "40817840250010046747"));

    }

    @Test
    @Ignore
    public void testMidas() throws Exception {

        baseEntityRepository.executeNativeUpdate("delete from accrln where bsaacid='40702810400154748352'");
        baseEntityRepository.executeNativeUpdate("delete from bsaacc where id='40702810400154748352'");
        baseEntityRepository.executeNativeUpdate("delete from acc where id='00695430RUR401102097'");

        remoteAccess.invoke(AccountDetailsNotifyTask.class, "processOneMessage", AcDNJournal.Sources.MIDAS_OPEN, AccountDetailsNotifyProcessor.messageMidas, null);

        // С проверкой заполнения ключевых полей
        assertTrue(null != baseEntityRepository.selectFirst(
                "select * from accrln where " +
                        "ACID='00695430RUR401102097' AND " +
                        "BSAACID='40702810400154748352' AND " +
                        "RLNTYPE='0' AND " +
                        "DRLNO=date '2014-12-22' AND " +
                        "DRLNC=date '2029-01-01' AND " +
                        "CTYPE=18 AND " +
                        "CNUM='00695430' AND " +
                        "CCODE='0015' AND " +
                        "ACC2='40702' AND " +
                        "PSAV='П' AND " +
                        "GLACOD='4011' AND " +
                        "CBCCY='810'"
        ));

        assertTrue(null != baseEntityRepository.selectFirst(
                "select * from bsaacc where " +
                        "ID='40702810400154748352' AND " +
                        "BSSAC= '40702' AND " +
                        "CCY='810' AND " +
                        "BSAKEY='4' AND " +
                        "BRCA='0015' AND " +
                        "BSACODE='4748352' AND " +
                        "BSAACO=date '2014-12-22' AND " +
                        "BSAACC=date '2029-01-01' AND " +
                        "BSATYPE='П' AND " +
                        "BSAGRP='0' AND " +
                        "BSAACNDAT=date '2014-12-22' AND " +
                        "BSAACNNUM='00695430    ' AND " +
                        "BSAACTAX=date '2014-12-25'"
        ));

        assertTrue(null != baseEntityRepository.selectFirst(
                "select * from acc where " +
                        "ID='00695430RUR401102097' AND " +
                        "BRCA='097' AND " +
                        "CNUM=695430 AND " +
                        "CCY='RUR' AND " +
                        "ACOD=4011 AND " +
                        "ACSQ=2 AND " +
                        "DACO=date '2014-12-22' AND " +
                        "DACC=date '2029-01-01' AND " +
                        "ANAM='ROSTENERGORESURS'"));
    }

    @Test
    public void testFccOpen() throws Exception {
        baseEntityRepository.executeNativeUpdate("delete from accrln where bsaacid='40817810000010696538'");
        baseEntityRepository.executeNativeUpdate("delete from bsaacc where id='40817810000010696538'");
        baseEntityRepository.executeNativeUpdate("delete from acc where id='00516770RUR000088001'");

        remoteAccess.invoke(AccountDetailsNotifyTask.class, "processOneMessage", AcDNJournal.Sources.FCC, AccountDetailsNotifyProcessor.messageFCC, null);

        assertTrue(null != baseEntityRepository.selectFirst("select * from accrln where bsaacid=?", "40817810000010696538"));
        assertTrue(null != baseEntityRepository.selectFirst("select * from bsaacc where id=?", "40817810000010696538"));
        assertTrue(null != baseEntityRepository.selectFirst("select * from acc where id=?", "00516770RUR000088001"));
    }

    @Test
    public void testErrorfromProd() throws Exception {
        baseEntityRepository.executeNativeUpdate("delete from accrln where bsaacid='40802810500014908835'");
        baseEntityRepository.executeNativeUpdate("delete from bsaacc where id='40802810500014908835'");
        baseEntityRepository.executeNativeUpdate("delete from acc where id='00800458RUR400902065'");

        remoteAccess.invoke(AccountDetailsNotifyTask.class, "processOneMessage", AcDNJournal.Sources.MIDAS_OPEN, error1, null);

        assertTrue(null != baseEntityRepository.selectFirst("select * from accrln where bsaacid=?", "40802810500014908835"));
        assertTrue(null != baseEntityRepository.selectFirst("select * from bsaacc where id=?", "40802810500014908835"));
        assertTrue(null != baseEntityRepository.selectFirst("select * from acc where id=?", "00800458RUR400902065"));
    }

    @Test
    public void testFC12Close() throws Exception {
        String bsaacid = "40702810400094449118";
        String closeDateStr = "2016-09-14";
        DataRecord rec = baseEntityRepository.selectFirst("SELECT * from ACCRLN where bsaacid = ?", bsaacid);
        Date dtc = rec.getDate("DRLNC");
        if (null == dtc || !getFinalDateStr().equals(dtc)) {
            reopenAccount(bsaacid);
        }

        remoteAccess.invoke(AccountDetailsNotifyTask.class, "processOneMessage", AcDNJournal.Sources.FCC_CLOSE, notifyFC12Close, null);

        checkCloseDate(bsaacid, getDatabaseDate().parse(closeDateStr), true);
    }

    @Test
    public void testFccClose() throws Exception {
        String bsaacid = "40817810050450101811";
        String closeDateStr = "2017-11-22";
        DataRecord rec = baseEntityRepository.selectFirst("SELECT * from ACCRLN where bsaacid = ?", bsaacid);
        Date dtc = rec.getDate("DRLNC");
        if (null == dtc || !getFinalDateStr().equals(dtc)) {
            reopenAccount(bsaacid);
        }

        remoteAccess.invoke(AccountDetailsNotifyTask.class, "processOneMessage", AcDNJournal.Sources.FCC, notifyFccClose, null);

        checkCloseDate(bsaacid, getDatabaseDate().parse(closeDateStr), false);
    }

    @Test
    public void testFC12EqualBranch() throws Exception {
        DataRecord rec = baseEntityRepository.selectFirst("SELECT branch from gl_acc where bsaacid='40802810900014820908'", new Object[]{});
        String oldBranch = rec.getString(0);

        remoteAccess.invoke(AccountDetailsNotifyTask.class, "processOneMessage", AcDNJournal.Sources.FC12, closeEqualBranch, null);

        if (null != baseEntityRepository.selectFirst("SELECT branch from gl_acc where bsaacid='40802810900014820908' and branch=?", new Object[]{oldBranch}))
            assertTrue(true);
        else{
            baseEntityRepository.executeNativeUpdate("UPDATE GL_ACC SET branch=? WHERE BSAACID='40802810900014820908'", new Object[]{oldBranch });
            assertTrue(false);
        }

    }

    @Test
    public void testFC12NoEqualBranch() throws Exception {
        DataRecord rec = baseEntityRepository.selectFirst("SELECT branch from gl_acc where bsaacid='40802810900014820908'", new Object[]{});
        String oldBranch = rec.getString(0);
        String errBranch = "054";
        baseEntityRepository.executeNativeUpdate("UPDATE GL_ACC SET branch=? WHERE BSAACID='40802810900014820908'", new Object[]{errBranch});

        remoteAccess.invoke(AccountDetailsNotifyTask.class, "processOneMessage", AcDNJournal.Sources.FC12, closeEqualBranch, null);

        if (null != baseEntityRepository.selectFirst("SELECT branch from gl_acc where bsaacid='40802810900014820908' and branch=?", new Object[]{oldBranch})) {
            assertTrue(true);
        }else{
            baseEntityRepository.executeNativeUpdate("UPDATE GL_ACC SET branch=? WHERE BSAACID='40802810900014820908'", new Object[]{oldBranch });
            assertTrue(false);
        }
    }


    private void putMessageInQueue(String queueName, String envelope) throws JMSException {
        MQQueueConnectionFactory cf = new MQQueueConnectionFactory();


        // Config
        cf.setHostName(HOST_NAME);
        cf.setPort(1414);
        cf.setTransportType(WMQConstants.WMQ_CM_CLIENT);
        cf.setQueueManager(MBROKER);
        cf.setChannel("SYSTEM.ADMIN.SVRCONN");

        MQQueueConnection connection = (MQQueueConnection) cf.createQueueConnection();
        MQQueueSession session = (MQQueueSession) connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        MQQueue queue = (MQQueue) session.createQueue("queue:///" + queueName);//UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF
        queue.setTargetClient(JMSC.MQJMS_MESSAGE_BODY_MQ);
        MQQueueSender sender = (MQQueueSender) session.createSender(queue);
        MQQueueReceiver receiver = (MQQueueReceiver) session.createReceiver(queue);

        connection.start();

        JMSTextMessage message = (JMSTextMessage) session.createTextMessage(envelope);
        sender.send(message);
        System.out.println("Sent message:\\n" + message);

//            JMSMessage receivedMessage = (JMSMessage) receiver.receive(10000);
//            System.out.println("\\nReceived message:\\n" + receivedMessage);

        sender.close();
        receiver.close();
        session.close();
        connection.close();
    }

    private void reopenAccount(String bsaacid) {
        baseEntityRepository.executeNativeUpdate("update GL_ACC set DTC = null where BSAACID = ?", bsaacid);
        baseEntityRepository.executeNativeUpdate("update ACCRLN set DRLNC = ? where BSAACID = ?", getFinalDateStr(),  bsaacid);
        baseEntityRepository.executeNativeUpdate("update BSAACC set BSAACC = ? where ID = ?", getFinalDateStr(), bsaacid);
    }

    private void checkCloseDate(String bsaacid, Date closeDate, boolean withGL) throws SQLException {
        if (withGL)
            Assert.assertNotNull(baseEntityRepository.selectFirst("SELECT * from GL_ACC where BSAACID=? and DTC = ?", bsaacid, closeDate));
        Assert.assertNotNull(baseEntityRepository.selectFirst("SELECT * from ACCRLN where BSAACID=? and DRLNC = ?", bsaacid, closeDate));
        Assert.assertNotNull(baseEntityRepository.selectFirst("SELECT * from BSAACC where ID=? and BSAACC = ?", bsaacid, closeDate));

    }

    private static String closeEqualBranch=
            "<NS1:Envelope xmlns:NS1=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                    "    <NS1:Header>\n" +
                    "        <NS2:UCBRUHeaders xmlns:NS2=\"urn:imb:gbo:v2\">\n" +
                    "            <NS2:Audit>\n" +
                    "                <NS2:MessagePath>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountDetailsNotify.NotificationHandler</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v4</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-06-22T18:07:32.740+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>START</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountListQuery</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-06-22T18:07:32.747+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>START</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountListQuery</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-06-22T18:07:32.942+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>SUCCESS</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountDetailsNotify.Publisher</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-06-22T18:07:32.964+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>SUCCESS</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment></NS2:Comment>\n" +
                    "                    </NS2:Step>\n" +
                    "                </NS2:MessagePath>\n" +
                    "            </NS2:Audit>\n" +
                    "        </NS2:UCBRUHeaders>\n" +
                    "    </NS1:Header>\n" +
                    "    <NS1:Body>\n" +
                    "        <gbo:AccountList xmlns:gbo=\"urn:imb:gbo:v2\">\n" +
                    "            <gbo:AccountDetails>\n" +
                    "                <gbo:AccountNo>01628209RURPRDC102</gbo:AccountNo>\n" +
                    "                <gbo:Branch>A29</gbo:Branch>\n" +
                    "                <gbo:CBAccountNo>40802810900014820908</gbo:CBAccountNo>\n" +
                    "                <gbo:Ccy>RUR</gbo:Ccy>\n" +
                    "                <gbo:CcyDigital>810</gbo:CcyDigital>\n" +
                    "                <gbo:Status>O</gbo:Status>\n" +
                    "                <gbo:CustomerNo>01628209</gbo:CustomerNo>\n" +
                    "                <gbo:Special>PRDC1</gbo:Special>\n" +
                    "                <gbo:OpenDate>2016-06-22</gbo:OpenDate>\n" +
                    "                <gbo:AltAccountNo>01628209RURPRDC102</gbo:AltAccountNo>\n" +
                    "                <gbo:Type>S</gbo:Type>\n" +
                    "                <gbo:ATMAvailable>Y</gbo:ATMAvailable>\n" +
                    "                <gbo:IsDormant>N</gbo:IsDormant>\n" +
                    "                <gbo:PostAllowed>N</gbo:PostAllowed>\n" +
                    "                <gbo:CreditTransAllowed>Y</gbo:CreditTransAllowed>\n" +
                    "                <gbo:DebitTransAllowed>Y</gbo:DebitTransAllowed>\n" +
                    "                <gbo:ClearingBank>044525545</gbo:ClearingBank>\n" +
                    "                <gbo:CorBank>044525545</gbo:CorBank>\n" +
                    "                <gbo:Positioning>\n" +
                    "                    <gbo:CBAccount>40817810950010736918</gbo:CBAccount>\n" +
                    "                    <gbo:IMBAccountNo>40817810950010736918</gbo:IMBAccountNo>\n" +
                    "                    <gbo:IMBBranch>A29</gbo:IMBBranch>\n" +
                    "                    <gbo:HostABSAccountNo>01628209RURPRDC102</gbo:HostABSAccountNo>\n" +
                    "                    <gbo:HostABSBranch>A29</gbo:HostABSBranch>\n" +
                    "                    <gbo:HostABS>FCC</gbo:HostABS>\n" +
                    "                </gbo:Positioning>\n" +
                    "                <gbo:MIS>\n" +
                    "                    <gbo:GroupComponent>R_P_MC</gbo:GroupComponent>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS1</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS2</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS3</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS4</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS5</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS6</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS7</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS8</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS9</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS10</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS1</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS2</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS3</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS4</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS5</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS6</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS7</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS8</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS9</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS10</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CostCode>\n" +
                    "                        <gbo:Name>COSTCOD1</gbo:Name>\n" +
                    "                    </gbo:CostCode>\n" +
                    "                    <gbo:CostCode>\n" +
                    "                        <gbo:Name>COSTCOD2</gbo:Name>\n" +
                    "                    </gbo:CostCode>\n" +
                    "                    <gbo:CostCode>\n" +
                    "                        <gbo:Name>COSTCOD3</gbo:Name>\n" +
                    "                    </gbo:CostCode>\n" +
                    "                    <gbo:CostCode>\n" +
                    "                        <gbo:Name>COSTCOD4</gbo:Name>\n" +
                    "                    </gbo:CostCode>\n" +
                    "                    <gbo:CostCode>\n" +
                    "                        <gbo:Name>COSTCOD5</gbo:Name>\n" +
                    "                    </gbo:CostCode>\n" +
                    "                </gbo:MIS>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>GWSAccType</gbo:Name>\n" +
                    "                    <gbo:Value>CURR</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>OperationTypeCodes</gbo:Name>\n" +
                    "                    <gbo:Value>VIEW=0,DOMPAY=1,CUPADE=0,CUPACO=0,</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>ParentBranchName</gbo:Name>\n" +
                    "                    <gbo:Value>UCB, Moscow</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>CusSegment</gbo:Name>\n" +
                    "                    <gbo:Value>TIER_I</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "            </gbo:AccountDetails>\n" +
                    "        </gbo:AccountList>\n" +
                    "    </NS1:Body>\n" +
                    "</NS1:Envelope>";

    public static String error1=
            "<NS1:Envelope xmlns:NS1=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                    "    <NS1:Header>\n" +
                    "        <NS2:UCBRUHeaders xmlns:NS2=\"urn:imb:gbo:v2\">\n" +
                    "            <NS2:Audit>\n" +
                    "                <NS2:MessagePath>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountDetailsNotify.NotificationHandler</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v4</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-09-07T09:15:55.966+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>START</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountListQuery.ESBDBRequest</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-09-07T09:15:55.988+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>START</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountListQuery</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-09-07T09:15:55.994+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>START</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountListQuery</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-09-07T09:15:56.171+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>SUCCESS</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountListQuery.ESBDBResponse</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-09-07T09:15:56.178+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>SUCCESS</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment>Finish of message processing</NS2:Comment>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountDetailsNotify.Publisher</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-09-07T09:15:56.198+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>SUCCESS</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment></NS2:Comment>\n" +
                    "                    </NS2:Step>\n" +
                    "                </NS2:MessagePath>\n" +
                    "            </NS2:Audit>\n" +
                    "        </NS2:UCBRUHeaders>\n" +
                    "    </NS1:Header>\n" +
                    "    <NS1:Body>\n" +
                    "        <gbo:AccountList xmlns:gbo=\"urn:imb:gbo:v2\">\n" +
                    "            <gbo:AccountDetails>\n" +
                    "                <gbo:AccountNo>800458RUR400902065</gbo:AccountNo>\n" +
                    "                <gbo:Branch>065</gbo:Branch>\n" +
                    "                <gbo:CBAccountNo>40802810500014908835</gbo:CBAccountNo>\n" +
                    "                <gbo:Ccy>RUR</gbo:Ccy>\n" +
                    "                <gbo:CcyDigital>810</gbo:CcyDigital>\n" +
                    "                <gbo:Description>VISHNYAKOV ALEKSEY</gbo:Description>\n" +
                    "                <gbo:Status>O</gbo:Status>\n" +
                    "                <gbo:CustomerNo>00800458</gbo:CustomerNo>\n" +
                    "                <gbo:Special>4009</gbo:Special>\n" +
                    "                <gbo:OpenDate>2016-09-07</gbo:OpenDate>\n" +
                    "                <gbo:CreditTransAllowed>Y</gbo:CreditTransAllowed>\n" +
                    "                <gbo:DebitTransAllowed>Y</gbo:DebitTransAllowed>\n" +
                    "                <gbo:CorBank>044525545</gbo:CorBank>\n" +
                    "                <gbo:CorINN>772457163509</gbo:CorINN>\n" +
                    "                <gbo:Positioning>\n" +
                    "                    <gbo:CBAccount>40802810500014908835</gbo:CBAccount>\n" +
                    "                    <gbo:IMBAccountNo>40802810500014908835</gbo:IMBAccountNo>\n" +
                    "                    <gbo:IMBBranch>065</gbo:IMBBranch>\n" +
                    "                    <gbo:HostABSAccountNo>800458RUR400902065</gbo:HostABSAccountNo>\n" +
                    "                    <gbo:HostABSBranch>065</gbo:HostABSBranch>\n" +
                    "                    <gbo:HostABS>MIDAS</gbo:HostABS>\n" +
                    "                </gbo:Positioning>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>GWSAccType</gbo:Name>\n" +
                    "                    <gbo:Value>CURR</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>OperationTypeCodes</gbo:Name>\n" +
                    "                    <gbo:Value>\n" +
                    "                        VIEW=1,DOMPAY=1,DOMTAX=1,DOMCUS=1,CUPADE=1,CUPACO=1,CUSEOD=1,CUSEOC=1,CURBUY=1,CUOPCE=1,\n" +
                    "                    </gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>ParentBranchName</gbo:Name>\n" +
                    "                    <gbo:Value>UCB, Moscow</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>CusSegment</gbo:Name>\n" +
                    "                    <gbo:Value>TIER_I</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "            </gbo:AccountDetails>\n" +
                    "        </gbo:AccountList>\n" +
                    "    </NS1:Body>\n" +
                    "</NS1:Envelope>\n";

    static String fullTopicTest =
            "<NS1:Envelope xmlns:NS1=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                    "    <NS1:Header>\n" +
                    "        <NS2:UCBRUHeaders xmlns:NS2=\"urn:imb:gbo:v2\">\n" +
                    "            <NS2:Audit>\n" +
                    "                <NS2:MessagePath>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountDetailsNotify.NotificationHandler</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v4</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-06-22T18:07:32.740+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>START</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountListQuery</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-06-22T18:07:32.747+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>START</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountListQuery</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-06-22T18:07:32.942+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>SUCCESS</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountDetailsNotify.Publisher</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-06-22T18:07:32.964+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>SUCCESS</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment></NS2:Comment>\n" +
                    "                    </NS2:Step>\n" +
                    "                </NS2:MessagePath>\n" +
                    "            </NS2:Audit>\n" +
                    "        </NS2:UCBRUHeaders>\n" +
                    "    </NS1:Header>\n" +
                    "    <NS1:Body>\n" +
                    "        <gbo:AccountList xmlns:gbo=\"urn:imb:gbo:v2\">\n" +
                    "            <gbo:AccountDetails>\n" +
                    "                <gbo:AccountNo>01628209RURPRDC102</gbo:AccountNo>\n" +
                    "                <gbo:Branch>A29</gbo:Branch>\n" +
                    "                <gbo:CBAccountNo>40817810950010736918</gbo:CBAccountNo>\n" +
                    "                <gbo:Ccy>RUR</gbo:Ccy>\n" +
                    "                <gbo:CcyDigital>810</gbo:CcyDigital>\n" +
                    "                <gbo:Status>O</gbo:Status>\n" +
                    "                <gbo:CustomerNo>01628209</gbo:CustomerNo>\n" +
                    "                <gbo:Special>PRDC1</gbo:Special>\n" +
                    "                <gbo:OpenDate>2016-06-22</gbo:OpenDate>\n" +
                    "                <gbo:AltAccountNo>01628209RURPRDC102</gbo:AltAccountNo>\n" +
                    "                <gbo:Type>S</gbo:Type>\n" +
                    "                <gbo:ATMAvailable>Y</gbo:ATMAvailable>\n" +
                    "                <gbo:IsDormant>N</gbo:IsDormant>\n" +
                    "                <gbo:PostAllowed>N</gbo:PostAllowed>\n" +
                    "                <gbo:CreditTransAllowed>Y</gbo:CreditTransAllowed>\n" +
                    "                <gbo:DebitTransAllowed>Y</gbo:DebitTransAllowed>\n" +
                    "                <gbo:ClearingBank>044525545</gbo:ClearingBank>\n" +
                    "                <gbo:CorBank>044525545</gbo:CorBank>\n" +
                    "                <gbo:Positioning>\n" +
                    "                    <gbo:CBAccount>40817810950010736918</gbo:CBAccount>\n" +
                    "                    <gbo:IMBAccountNo>40817810950010736918</gbo:IMBAccountNo>\n" +
                    "                    <gbo:IMBBranch>A29</gbo:IMBBranch>\n" +
                    "                    <gbo:HostABSAccountNo>01628209RURPRDC102</gbo:HostABSAccountNo>\n" +
                    "                    <gbo:HostABSBranch>A29</gbo:HostABSBranch>\n" +
                    "                    <gbo:HostABS>FCC</gbo:HostABS>\n" +
                    "                </gbo:Positioning>\n" +
                    "                <gbo:MIS>\n" +
                    "                    <gbo:GroupComponent>R_P_MC</gbo:GroupComponent>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS1</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS2</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS3</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS4</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS5</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS6</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS7</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS8</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS9</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS10</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS1</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS2</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS3</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS4</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS5</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS6</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS7</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS8</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS9</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS10</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CostCode>\n" +
                    "                        <gbo:Name>COSTCOD1</gbo:Name>\n" +
                    "                    </gbo:CostCode>\n" +
                    "                    <gbo:CostCode>\n" +
                    "                        <gbo:Name>COSTCOD2</gbo:Name>\n" +
                    "                    </gbo:CostCode>\n" +
                    "                    <gbo:CostCode>\n" +
                    "                        <gbo:Name>COSTCOD3</gbo:Name>\n" +
                    "                    </gbo:CostCode>\n" +
                    "                    <gbo:CostCode>\n" +
                    "                        <gbo:Name>COSTCOD4</gbo:Name>\n" +
                    "                    </gbo:CostCode>\n" +
                    "                    <gbo:CostCode>\n" +
                    "                        <gbo:Name>COSTCOD5</gbo:Name>\n" +
                    "                    </gbo:CostCode>\n" +
                    "                </gbo:MIS>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>GWSAccType</gbo:Name>\n" +
                    "                    <gbo:Value>CURR</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>OperationTypeCodes</gbo:Name>\n" +
                    "                    <gbo:Value>VIEW=0,DOMPAY=1,CUPADE=0,CUPACO=0,</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>ParentBranchName</gbo:Name>\n" +
                    "                    <gbo:Value>UCB, Moscow</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>CusSegment</gbo:Name>\n" +
                    "                    <gbo:Value>TIER_I</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "            </gbo:AccountDetails>\n" +
                    "        </gbo:AccountList>\n" +
                    "    </NS1:Body>\n" +
                    "</NS1:Envelope>";

    public static String fullTopicTestMidasOld =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "<NS1:Envelope xmlns:NS1=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                    "    <NS1:Header>\n" +
                    "        <NS2:UCBRUHeaders xmlns:NS2=\"urn:imb:gbo:v2\">\n" +
                    "            <NS2:Audit>\n" +
                    "                <NS2:MessagePath>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountDetailsNotify.NotificationHandler</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v4</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-03-29T17:50:54.289+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>START</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountListQuery</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-03-29T17:50:54.295+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>START</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountListQuery</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-03-29T17:50:54.405+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>SUCCESS</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountDetailsNotify.Publisher</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-03-29T17:50:54.413+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>SUCCESS</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment></NS2:Comment>\n" +
                    "                    </NS2:Step>\n" +
                    "                </NS2:MessagePath>\n" +
                    "            </NS2:Audit>\n" +
                    "        </NS2:UCBRUHeaders>\n" +
                    "    </NS1:Header>\n" +
                    "    <NS1:Body>\n" +
                    "        <gbo:AccountList xmlns:gbo=\"urn:imb:gbo:v2\">\n" +
                    "            <gbo:AccountDetails>\n" +
                    "                <gbo:AccountNo>695430RUR401102097</gbo:AccountNo>\n" +
                    "                <gbo:Branch>097</gbo:Branch>\n" +
                    "                <gbo:CBAccountNo>40702810400154748352</gbo:CBAccountNo>\n" +
                    "                <gbo:Ccy>RUR</gbo:Ccy>\n" +
                    "                <gbo:CcyDigital>810</gbo:CcyDigital>\n" +
                    "                <gbo:Description>БББББББББББББ</gbo:Description>\n" +//(портфель 2,  5 КК)
                    "                <gbo:Status>O</gbo:Status>\n" +
                    "                <gbo:CustomerNo>00695430</gbo:CustomerNo>\n" +
                    "                <gbo:Special>4011</gbo:Special>\n" +
                    "                <gbo:OpenDate>2014-12-22</gbo:OpenDate>\n" +
                    "                <gbo:CreditTransAllowed>N</gbo:CreditTransAllowed>\n" +
                    "                <gbo:DebitTransAllowed>N</gbo:DebitTransAllowed>\n" +
                    "                <gbo:CorBank>046027238</gbo:CorBank>\n" +
                    "                <gbo:CorINN>6166083860</gbo:CorINN>\n" +
                    "                <gbo:Positioning>\n" +
                    "                    <gbo:CBAccount>40702810400154748352</gbo:CBAccount>\n" +
                    "                    <gbo:IMBAccountNo>40702810400154748352</gbo:IMBAccountNo>\n" +
                    "                    <gbo:IMBBranch>097</gbo:IMBBranch>\n" +
                    "                    <gbo:HostABSAccountNo>695430RUR401102097</gbo:HostABSAccountNo>\n" +
                    "                    <gbo:HostABSBranch>097</gbo:HostABSBranch>\n" +
                    "                    <gbo:HostABS>MIDAS</gbo:HostABS>\n" +
                    "                </gbo:Positioning>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>GWSAccType</gbo:Name>\n" +
                    "                    <gbo:Value>CURR</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>OperationTypeCodes</gbo:Name>\n" +
                    "                    <gbo:Value>\n" +
                    "                        VIEW=1,DOMPAY=1,DOMTAX=1,DOMCUS=1,CUPADE=1,CUPACO=1,CUSEOD=1,CUSEOC=1,CURBUY=1,CUOPCE=1,\n" +
                    "                    </gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>ParentBranchName</gbo:Name>\n" +
                    "                    <gbo:Value>UCB, Rostov Branch</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>CusSegment</gbo:Name>\n" +
                    "                    <gbo:Value>TIER_I</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:ShadowAccounts>\n" +
                    "                    <gbo:HostABS>FCC</gbo:HostABS>\n" +
                    "                    <gbo:AccountNo>00695430RURCOSA101</gbo:AccountNo>\n" +
                    "                    <gbo:Branch>C04</gbo:Branch>\n" +
                    "                </gbo:ShadowAccounts>\n" +
                    "            </gbo:AccountDetails>\n" +
                    "        </gbo:AccountList>\n" +
                    "    </NS1:Body>\n" +
                    "</NS1:Envelope>";

    String notifyFC12Close ="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:gbo=\"urn:ucbru:gbo:v5\"\n" +
            "                  xmlns:acc=\"urn:ucbru:gbo:v5:acc\">\n" +
            "    <soapenv:Header>\n" +
            "        <gbo:UCBRUHeaders>\n" +
            "            <gbo:Audit>\n" +
            "                <gbo:MessagePath>\n" +
            "                    <gbo:Step>\n" +
            "                        <gbo:Application.Module>ATSFC12AC.AccountDetailsNotify.NotificationHandler\n" +
            "                        </gbo:Application.Module>\n" +
            "                        <gbo:VersionId>v5</gbo:VersionId>\n" +
            "                        <gbo:TimeStamp>2016-11-24T12:43:11.055+03:00</gbo:TimeStamp>\n" +
            "                        <gbo:RoutingRole>ROUTE</gbo:RoutingRole>\n" +
            "                        <gbo:Comment>Routing in SACC.v5.AcDeNo</gbo:Comment>\n" +
            "                    </gbo:Step>\n" +
            "                    <gbo:Step>\n" +
            "                        <gbo:Application.Module>SACC.v5.AcDeNo.NotificationHandler</gbo:Application.Module>\n" +
            "                        <gbo:VersionId>v5</gbo:VersionId>\n" +
            "                        <gbo:TimeStamp>2016-11-24T12:43:11.059+03:00</gbo:TimeStamp>\n" +
            "                        <gbo:RoutingRole>START</gbo:RoutingRole>\n" +
            "                        <gbo:Comment>Start of message processing</gbo:Comment>\n" +
            "                    </gbo:Step>\n" +
            "                    <gbo:Step>\n" +
            "                        <gbo:Application.Module>SACC.AccountListQuery.RequestHandler</gbo:Application.Module>\n" +
            "                        <gbo:VersionId>v5</gbo:VersionId>\n" +
            "                        <gbo:TimeStamp>2016-11-24T12:43:11.063+03:00</gbo:TimeStamp>\n" +
            "                        <gbo:RoutingRole>START</gbo:RoutingRole>\n" +
            "                        <gbo:Comment>Start of message processing</gbo:Comment>\n" +
            "                    </gbo:Step>\n" +
            "                    <gbo:Step>\n" +
            "                        <gbo:Application.Module>SACC.MasterAccountPositioningBatchQuery.RequestHandler\n" +
            "                        </gbo:Application.Module>\n" +
            "                        <gbo:VersionId>v5</gbo:VersionId>\n" +
            "                        <gbo:TimeStamp>2016-11-24T12:43:11.069+03:00</gbo:TimeStamp>\n" +
            "                        <gbo:RoutingRole>START</gbo:RoutingRole>\n" +
            "                        <gbo:Comment>Start of message processing</gbo:Comment>\n" +
            "                    </gbo:Step>\n" +
            "                    <gbo:Step>\n" +
            "                        <gbo:Application.Module>SACC.MasterAccountPositioningBatchQuery.RequestHandler\n" +
            "                        </gbo:Application.Module>\n" +
            "                        <gbo:VersionId>v5</gbo:VersionId>\n" +
            "                        <gbo:TimeStamp>2016-11-24T12:43:11.075+03:00</gbo:TimeStamp>\n" +
            "                        <gbo:RoutingRole>ROUTE</gbo:RoutingRole>\n" +
            "                        <gbo:Comment>In the cache of the accounts not detected, will be requested ABS</gbo:Comment>\n" +
            "                    </gbo:Step>\n" +
            "                    <gbo:Step>\n" +
            "                        <gbo:Application.Module>SACC.MasterAccountPositioningBatchQuery.ABSRequestHandler\n" +
            "                        </gbo:Application.Module>\n" +
            "                        <gbo:VersionId>v5</gbo:VersionId>\n" +
            "                        <gbo:TimeStamp>2016-11-24T12:43:11.078+03:00</gbo:TimeStamp>\n" +
            "                        <gbo:RoutingRole>START</gbo:RoutingRole>\n" +
            "                        <gbo:Comment>Start of message processing</gbo:Comment>\n" +
            "                    </gbo:Step>\n" +
            "                    <gbo:Step>\n" +
            "                        <gbo:Application.Module>SACC.MasterAccountPositioningBatchQuery.ABSRequestHandler\n" +
            "                        </gbo:Application.Module>\n" +
            "                        <gbo:VersionId>v5</gbo:VersionId>\n" +
            "                        <gbo:TimeStamp>2016-11-24T12:43:11.080+03:00</gbo:TimeStamp>\n" +
            "                        <gbo:RoutingRole>ROUTE</gbo:RoutingRole>\n" +
            "                        <gbo:Comment>Routing by cache settings</gbo:Comment>\n" +
            "                    </gbo:Step>\n" +
            "                    <gbo:Step>\n" +
            "                        <gbo:Application.Module>SACC.MasterAccountPositioningBatchQuery.ABSResponseHandler\n" +
            "                        </gbo:Application.Module>\n" +
            "                        <gbo:VersionId>v5</gbo:VersionId>\n" +
            "                        <gbo:TimeStamp>2016-11-24T12:43:11.316+03:00</gbo:TimeStamp>\n" +
            "                        <gbo:RoutingRole>ROUTE SUCCESS</gbo:RoutingRole>\n" +
            "                        <gbo:Comment>The responses received from ECHO,FC12</gbo:Comment>\n" +
            "                    </gbo:Step>\n" +
            "                    <gbo:Step>\n" +
            "                        <gbo:Application.Module>SACC.MasterAccountPositioningBatchQuery.ABSResponseHandler\n" +
            "                        </gbo:Application.Module>\n" +
            "                        <gbo:VersionId>v5</gbo:VersionId>\n" +
            "                        <gbo:TimeStamp>2016-11-24T12:43:11.318+03:00</gbo:TimeStamp>\n" +
            "                        <gbo:RoutingRole>SUCCESS</gbo:RoutingRole>\n" +
            "                        <gbo:Comment>Finish of message processing</gbo:Comment>\n" +
            "                    </gbo:Step>\n" +
            "                    <gbo:Step>\n" +
            "                        <gbo:Application.Module>SACC.MasterAccountPositioningBatchQuery.ResponseHandler\n" +
            "                        </gbo:Application.Module>\n" +
            "                        <gbo:VersionId>v5</gbo:VersionId>\n" +
            "                        <gbo:TimeStamp>2016-11-24T12:43:11.321+03:00</gbo:TimeStamp>\n" +
            "                        <gbo:RoutingRole>SUCCESS</gbo:RoutingRole>\n" +
            "                        <gbo:Comment>Finish of message processing</gbo:Comment>\n" +
            "                    </gbo:Step>\n" +
            "                    <gbo:Step>\n" +
            "                        <gbo:Application.Module>SACC.AccountListQuery.ABSRouter</gbo:Application.Module>\n" +
            "                        <gbo:VersionId>v5</gbo:VersionId>\n" +
            "                        <gbo:TimeStamp>2016-11-24T12:43:11.325+03:00</gbo:TimeStamp>\n" +
            "                        <gbo:RoutingRole>ROUTE</gbo:RoutingRole>\n" +
            "                        <gbo:Comment>Routing by cache settings</gbo:Comment>\n" +
            "                    </gbo:Step>\n" +
            "                    <gbo:Step>\n" +
            "                        <gbo:Application.Module>SACC.AccountListQuery.ResponseHandler</gbo:Application.Module>\n" +
            "                        <gbo:VersionId>5</gbo:VersionId>\n" +
            "                        <gbo:TimeStamp>2016-11-24T12:43:11.496+03:00</gbo:TimeStamp>\n" +
            "                        <gbo:RoutingRole>ROUTE SUCCESS</gbo:RoutingRole>\n" +
            "                        <gbo:Comment>The responses received from ECHO,FC12</gbo:Comment>\n" +
            "                    </gbo:Step>\n" +
            "                    <gbo:Step>\n" +
            "                        <gbo:Application.Module>SACC.AccountListQuery.ResponseHandler</gbo:Application.Module>\n" +
            "                        <gbo:VersionId>v5</gbo:VersionId>\n" +
            "                        <gbo:TimeStamp>2016-11-24T12:43:11.498+03:00</gbo:TimeStamp>\n" +
            "                        <gbo:RoutingRole>SUCCESS</gbo:RoutingRole>\n" +
            "                        <gbo:Comment>Finish of message processing</gbo:Comment>\n" +
            "                    </gbo:Step>\n" +
            "                    <gbo:Step>\n" +
            "                        <gbo:Application.Module>SACC.v5.AcDeNo.PublishHandler</gbo:Application.Module>\n" +
            "                        <gbo:VersionId>v5</gbo:VersionId>\n" +
            "                        <gbo:TimeStamp>2016-11-24T12:43:11.504+03:00</gbo:TimeStamp>\n" +
            "                        <gbo:RoutingRole>SUCCESS</gbo:RoutingRole>\n" +
            "                        <gbo:Comment>Finish of message processing</gbo:Comment>\n" +
            "                    </gbo:Step>\n" +
            "                </gbo:MessagePath>\n" +
            "            </gbo:Audit>\n" +
            "        </gbo:UCBRUHeaders>\n" +
            "    </soapenv:Header>\n" +
            "    <soapenv:Body>\n" +
            "        <acc:AccountList>\n" +
            "            <acc:AccountDetails>\n" +
            "                <acc:AccountNo>00145005RURCOSA101</acc:AccountNo>\n" +
            "                <acc:Branch>E01</acc:Branch>\n" +
            "                <acc:CBAccountNo>40702810400094449118</acc:CBAccountNo>\n" +
            "                <acc:Ccy>RUR</acc:Ccy>\n" +
            "                <acc:Description>OOO GruzAvtoImport</acc:Description>\n" +
            "                <acc:Status>C</acc:Status>\n" +
            "                <acc:CustomerNo>00145005</acc:CustomerNo>\n" +
            "                <acc:Special>COSA1</acc:Special>\n" +
            "                <acc:OpenDate>2011-07-22</acc:OpenDate>\n" +
            "                <acc:AltAccountNo>145005RUR401102009</acc:AltAccountNo>\n" +
            "                <acc:ATMAvailable>N</acc:ATMAvailable>\n" +
            "                <acc:CreditTransAllowed>Y</acc:CreditTransAllowed>\n" +
            "                <acc:DebitTransAllowed>Y</acc:DebitTransAllowed>\n" +
            "                <acc:BackPeriodEntryAllowed>Y</acc:BackPeriodEntryAllowed>\n" +
            "                <acc:AutoProvisionRequired>N</acc:AutoProvisionRequired>\n" +
            "                <acc:ClearingAccountNo>00145005RURCOSA101</acc:ClearingAccountNo>\n" +
            "                <acc:DormantParameter>B</acc:DormantParameter>\n" +
            "                <acc:ExcludeReversalTrans>N</acc:ExcludeReversalTrans>\n" +
            "                <acc:MT210Required>N</acc:MT210Required>\n" +
            "                <acc:SweepType>1</acc:SweepType>\n" +
            "                <acc:CorBank>045773873</acc:CorBank>\n" +
            "                <acc:CorINN>7710030411</acc:CorINN>\n" +
            "                <acc:Positioning>\n" +
            "                    <acc:CBAccount>40702810400094449118</acc:CBAccount>\n" +
            "                    <acc:IMBAccountNo>00145005RURCOSA101</acc:IMBAccountNo>\n" +
            "                    <acc:IMBBranch>E01</acc:IMBBranch>\n" +
            "                    <acc:HostABSAccountNo>00145005RURCOSA101</acc:HostABSAccountNo>\n" +
            "                    <acc:HostABSBranch>E01</acc:HostABSBranch>\n" +
            "                    <acc:HostABS>FC12</acc:HostABS>\n" +
            "                </acc:Positioning>\n" +
            "                <acc:GLCredit>\n" +
            "                    <acc:CBLine>LI400</acc:CBLine>\n" +
            "                    <acc:GLValue>202019102</acc:GLValue>\n" +
            "                    <acc:HOLine>202000</acc:HOLine>\n" +
            "                </acc:GLCredit>\n" +
            "                <acc:GLDebit>\n" +
            "                    <acc:CBLine>AS400</acc:CBLine>\n" +
            "                    <acc:GLValue>105049022</acc:GLValue>\n" +
            "                    <acc:HOLine>105000</acc:HOLine>\n" +
            "                </acc:GLDebit>\n" +
            "                <acc:StmtSettings>\n" +
            "                    <acc:Cycle>D</acc:Cycle>\n" +
            "                    <acc:Type>D</acc:Type>\n" +
            "                    <acc:GenOnMovement>Y</acc:GenOnMovement>\n" +
            "                </acc:StmtSettings>\n" +
            "                <acc:Stmt2Settings>\n" +
            "                    <acc:Type>N</acc:Type>\n" +
            "                </acc:Stmt2Settings>\n" +
            "                <acc:Stmt3Settings>\n" +
            "                    <acc:Type>N</acc:Type>\n" +
            "                </acc:Stmt3Settings>\n" +
            "                <acc:PrevStmt>\n" +
            "                    <acc:Date>2016-09-14</acc:Date>\n" +
            "                </acc:PrevStmt>\n" +
            "                <acc:PrevStmt2/>\n" +
            "                <acc:PrevStmt3/>\n" +
            "                <acc:MIS>\n" +
            "                    <acc:Pool>POOL2</acc:Pool>\n" +
            "                </acc:MIS>\n" +
            "                <acc:ControlledAccount>Y</acc:ControlledAccount>\n" +
            "                <acc:CloseDate>2016-09-14</acc:CloseDate>\n" +
            "                <acc:OpenBalance>0</acc:OpenBalance>\n" +
            "                <acc:CurrentBalance>0</acc:CurrentBalance>\n" +
            "                <acc:AvailableBalance>0</acc:AvailableBalance>\n" +
            "                <acc:UncollectedBalance>0</acc:UncollectedBalance>\n" +
            "                <acc:CreditTurnoverToday>0</acc:CreditTurnoverToday>\n" +
            "                <acc:DebitTurnoverToday>0</acc:DebitTurnoverToday>\n" +
            "                <acc:BlockedAmount>0</acc:BlockedAmount>\n" +
            "                <acc:DebitAccrued>0</acc:DebitAccrued>\n" +
            "                <acc:CreditAccrued>0</acc:CreditAccrued>\n" +
            "                <acc:GWSAccType>CURR</acc:GWSAccType>\n" +
            "                <acc:OperationTypeCodes>\n" +
            "                    VIEW=1,DOMPAY=1,DOMTAX=1,DOMCUS=1,CUPADE=1,CUPACO=1,CUSEOD=1,CUSEOC=1,CURBUY=1,CUOPCE=1,\n" +
            "                </acc:OperationTypeCodes>\n" +
            "            </acc:AccountDetails>\n" +
            "        </acc:AccountList>\n" +
            "    </soapenv:Body>\n" +
            "</soapenv:Envelope>";

    String notifyFccClose ="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:gbo=\"urn:ucbru:gbo:v5\"\n" +
            "                  xmlns:acc=\"urn:ucbru:gbo:v5:acc\">\n" +
            "    <soapenv:Header>\n" +
            "        <NS1:UCBRUHeaders xmlns:NS1=\"urn:ucbru:gbo:v5\">\n" +
            "            <NS1:Audit>\n" +
            "                <NS1:MessagePath>\n" +
            "                    <NS1:Step>\n" +
            "                        <NS1:Application.Module>SACC.v5.AcDeNo.NotificationHandler</NS1:Application.Module>\n" +
            "                        <NS1:VersionId>v5</NS1:VersionId>\n" +
            "                        <NS1:TimeStamp>2017-11-22T14:13:42.032+03:00</NS1:TimeStamp>\n" +
            "                        <NS1:RoutingRole>START</NS1:RoutingRole>\n" +
            "                        <NS1:Comment>Start of message processing</NS1:Comment>\n" +
            "                    </NS1:Step>\n" +
            "                    <NS1:Step>\n" +
            "                        <NS1:Application.Module>SACC.AccountListQuery.RequestHandler</NS1:Application.Module>\n" +
            "                        <NS1:VersionId>v5</NS1:VersionId>\n" +
            "                        <NS1:TimeStamp>2017-11-22T14:13:42.044+03:00</NS1:TimeStamp>\n" +
            "                        <NS1:RoutingRole>START</NS1:RoutingRole>\n" +
            "                        <NS1:Comment>Start of message processing</NS1:Comment>\n" +
            "                    </NS1:Step>\n" +
            "                    <NS1:Step>\n" +
            "                        <NS1:Application.Module>SACC.MasterAccountPositioningBatchQuery.RequestHandler\n" +
            "                        </NS1:Application.Module>\n" +
            "                        <NS1:VersionId>v5</NS1:VersionId>\n" +
            "                        <NS1:TimeStamp>2017-11-22T14:13:42.076+03:00</NS1:TimeStamp>\n" +
            "                        <NS1:RoutingRole>START</NS1:RoutingRole>\n" +
            "                        <NS1:Comment>Start of message processing</NS1:Comment>\n" +
            "                    </NS1:Step>\n" +
            "                    <NS1:Step>\n" +
            "                        <NS1:Application.Module>SACC.MasterAccountPositioningBatchQuery.RequestHandler\n" +
            "                        </NS1:Application.Module>\n" +
            "                        <NS1:VersionId>v5</NS1:VersionId>\n" +
            "                        <NS1:TimeStamp>2017-11-22T14:13:42.105+03:00</NS1:TimeStamp>\n" +
            "                        <NS1:RoutingRole>ROUTE</NS1:RoutingRole>\n" +
            "                        <NS1:Comment>In the cache of the accounts not detected, will be requested ABS</NS1:Comment>\n" +
            "                    </NS1:Step>\n" +
            "                    <NS1:Step>\n" +
            "                        <NS1:Application.Module>SACC.MasterAccountPositioningBatchQuery.ABSRequestHandler\n" +
            "                        </NS1:Application.Module>\n" +
            "                        <NS1:VersionId>v5</NS1:VersionId>\n" +
            "                        <NS1:TimeStamp>2017-11-22T14:13:42.119+03:00</NS1:TimeStamp>\n" +
            "                        <NS1:RoutingRole>START</NS1:RoutingRole>\n" +
            "                        <NS1:Comment>Start of message processing</NS1:Comment>\n" +
            "                    </NS1:Step>\n" +
            "                    <NS1:Step>\n" +
            "                        <NS1:Application.Module>SACC.MasterAccountPositioningBatchQuery.ABSRequestHandler\n" +
            "                        </NS1:Application.Module>\n" +
            "                        <NS1:VersionId>v5</NS1:VersionId>\n" +
            "                        <NS1:TimeStamp>2017-11-22T14:13:42.126+03:00</NS1:TimeStamp>\n" +
            "                        <NS1:RoutingRole>ROUTE</NS1:RoutingRole>\n" +
            "                        <NS1:Comment>Routing by cache settings</NS1:Comment>\n" +
            "                    </NS1:Step>\n" +
            "                    <NS1:Step>\n" +
            "                        <NS1:Application.Module>SACC.MasterAccountPositioningBatchQuery.ABSResponseHandler\n" +
            "                        </NS1:Application.Module>\n" +
            "                        <NS1:VersionId>v5</NS1:VersionId>\n" +
            "                        <NS1:TimeStamp>2017-11-22T14:13:42.190+03:00</NS1:TimeStamp>\n" +
            "                        <NS1:RoutingRole>ROUTE SUCCESS</NS1:RoutingRole>\n" +
            "                        <NS1:Comment>The responses received from ECHO,FCC</NS1:Comment>\n" +
            "                    </NS1:Step>\n" +
            "                    <NS1:Step>\n" +
            "                        <NS1:Application.Module>SACC.MasterAccountPositioningBatchQuery.ABSResponseHandler\n" +
            "                        </NS1:Application.Module>\n" +
            "                        <NS1:VersionId>v5</NS1:VersionId>\n" +
            "                        <NS1:TimeStamp>2017-11-22T14:13:42.194+03:00</NS1:TimeStamp>\n" +
            "                        <NS1:RoutingRole>SUCCESS</NS1:RoutingRole>\n" +
            "                        <NS1:Comment>Finish of message processing</NS1:Comment>\n" +
            "                    </NS1:Step>\n" +
            "                    <NS1:Step>\n" +
            "                        <NS1:Application.Module>SACC.MasterAccountPositioningBatchQuery.ResponseHandler\n" +
            "                        </NS1:Application.Module>\n" +
            "                        <NS1:VersionId>v5</NS1:VersionId>\n" +
            "                        <NS1:TimeStamp>2017-11-22T14:13:42.204+03:00</NS1:TimeStamp>\n" +
            "                        <NS1:RoutingRole>SUCCESS</NS1:RoutingRole>\n" +
            "                        <NS1:Comment>Finish of message processing</NS1:Comment>\n" +
            "                    </NS1:Step>\n" +
            "                    <NS1:Step>\n" +
            "                        <NS1:Application.Module>SACC.AccountListQuery.ABSRouter</NS1:Application.Module>\n" +
            "                        <NS1:VersionId>v5</NS1:VersionId>\n" +
            "                        <NS1:TimeStamp>2017-11-22T14:13:42.214+03:00</NS1:TimeStamp>\n" +
            "                        <NS1:RoutingRole>ROUTE</NS1:RoutingRole>\n" +
            "                        <NS1:Comment>Routing by cache settings</NS1:Comment>\n" +
            "                    </NS1:Step>\n" +
            "                    <NS1:Step>\n" +
            "                        <NS1:Application.Module>SACC.AccountListQuery.ResponseHandler</NS1:Application.Module>\n" +
            "                        <NS1:VersionId>5</NS1:VersionId>\n" +
            "                        <NS1:TimeStamp>2017-11-22T14:13:42.341+03:00</NS1:TimeStamp>\n" +
            "                        <NS1:RoutingRole>ROUTE SUCCESS</NS1:RoutingRole>\n" +
            "                        <NS1:Comment>The responses received from ECHO,FCC</NS1:Comment>\n" +
            "                    </NS1:Step>\n" +
            "                    <NS1:Step>\n" +
            "                        <NS1:Application.Module>SACC.AccountListQuery.ResponseHandler</NS1:Application.Module>\n" +
            "                        <NS1:VersionId>v5</NS1:VersionId>\n" +
            "                        <NS1:TimeStamp>2017-11-22T14:13:42.346+03:00</NS1:TimeStamp>\n" +
            "                        <NS1:RoutingRole>SUCCESS</NS1:RoutingRole>\n" +
            "                        <NS1:Comment>Finish of message processing</NS1:Comment>\n" +
            "                    </NS1:Step>\n" +
            "                    <NS1:Step>\n" +
            "                        <NS1:Application.Module>SACC.v5.AcDeNo.PublishHandler</NS1:Application.Module>\n" +
            "                        <NS1:VersionId>v5</NS1:VersionId>\n" +
            "                        <NS1:TimeStamp>2017-11-22T14:13:42.359+03:00</NS1:TimeStamp>\n" +
            "                        <NS1:RoutingRole>SUCCESS</NS1:RoutingRole>\n" +
            "                        <NS1:Comment>Finish of message processing</NS1:Comment>\n" +
            "                    </NS1:Step>\n" +
            "                </NS1:MessagePath>\n" +
            "            </NS1:Audit>\n" +
            "        </NS1:UCBRUHeaders>\n" +
            "    </soapenv:Header>\n" +
            "    <soapenv:Body>\n" +
            "        <acc:AccountList>\n" +
            "            <acc:AccountDetails>\n" +
            "                <acc:AccountNo>02372591RURPRDC101</acc:AccountNo>\n" +
            "                <acc:Branch>M03</acc:Branch>\n" +
            "                <acc:CBAccountNo>40817810050450101811</acc:CBAccountNo>\n" +
            "                <acc:Ccy>RUR</acc:Ccy>\n" +
            "                <acc:CcyDigital>810</acc:CcyDigital>\n" +
            "                <acc:Status>C</acc:Status>\n" +
            "                <acc:CustomerNo>02372591</acc:CustomerNo>\n" +
            "                <acc:Special>PRDC1</acc:Special>\n" +
            "                <acc:OpenDate>2017-02-15</acc:OpenDate>\n" +
            "                <acc:AltAccountNo>02372591RURPRDC101</acc:AltAccountNo>\n" +
            "                <acc:Type>S</acc:Type>\n" +
            "                <acc:ATMAvailable>Y</acc:ATMAvailable>\n" +
            "                <acc:IsDormant>N</acc:IsDormant>\n" +
            "                <acc:PostAllowed>N</acc:PostAllowed>\n" +
            "                <acc:CreditTransAllowed>Y</acc:CreditTransAllowed>\n" +
            "                <acc:DebitTransAllowed>Y</acc:DebitTransAllowed>\n" +
            "                <acc:ClearingBank>042202799</acc:ClearingBank>\n" +
            "                <acc:CorBank>042202799</acc:CorBank>\n" +
            "                <acc:Positioning>\n" +
            "                    <acc:CBAccount>40817810050450101811</acc:CBAccount>\n" +
            "                    <acc:IMBAccountNo>40817810050450101811</acc:IMBAccountNo>\n" +
            "                    <acc:IMBBranch>M03</acc:IMBBranch>\n" +
            "                    <acc:HostABSAccountNo>02372591RURPRDC101</acc:HostABSAccountNo>\n" +
            "                    <acc:HostABSBranch>M03</acc:HostABSBranch>\n" +
            "                    <acc:HostABS>FCC</acc:HostABS>\n" +
            "                </acc:Positioning>\n" +
            "                <acc:MIS>\n" +
            "                    <acc:GroupComponent>R_P_MC</acc:GroupComponent>\n" +
            "                    <acc:TransactionClass>\n" +
            "                        <acc:Name>TXNMIS1</acc:Name>\n" +
            "                    </acc:TransactionClass>\n" +
            "                    <acc:TransactionClass>\n" +
            "                        <acc:Name>TXNMIS2</acc:Name>\n" +
            "                    </acc:TransactionClass>\n" +
            "                    <acc:TransactionClass>\n" +
            "                        <acc:Name>TXNMIS3</acc:Name>\n" +
            "                    </acc:TransactionClass>\n" +
            "                    <acc:TransactionClass>\n" +
            "                        <acc:Name>TXNMIS4</acc:Name>\n" +
            "                    </acc:TransactionClass>\n" +
            "                    <acc:TransactionClass>\n" +
            "                        <acc:Name>TXNMIS5</acc:Name>\n" +
            "                    </acc:TransactionClass>\n" +
            "                    <acc:TransactionClass>\n" +
            "                        <acc:Name>TXNMIS6</acc:Name>\n" +
            "                    </acc:TransactionClass>\n" +
            "                    <acc:TransactionClass>\n" +
            "                        <acc:Name>TXNMIS7</acc:Name>\n" +
            "                    </acc:TransactionClass>\n" +
            "                    <acc:TransactionClass>\n" +
            "                        <acc:Name>TXNMIS8</acc:Name>\n" +
            "                    </acc:TransactionClass>\n" +
            "                    <acc:TransactionClass>\n" +
            "                        <acc:Name>TXNMIS9</acc:Name>\n" +
            "                    </acc:TransactionClass>\n" +
            "                    <acc:TransactionClass>\n" +
            "                        <acc:Name>TXNMIS10</acc:Name>\n" +
            "                    </acc:TransactionClass>\n" +
            "                    <acc:CompositeClass>\n" +
            "                        <acc:Name>COMPMIS1</acc:Name>\n" +
            "                    </acc:CompositeClass>\n" +
            "                    <acc:CompositeClass>\n" +
            "                        <acc:Name>COMPMIS2</acc:Name>\n" +
            "                    </acc:CompositeClass>\n" +
            "                    <acc:CompositeClass>\n" +
            "                        <acc:Name>COMPMIS3</acc:Name>\n" +
            "                    </acc:CompositeClass>\n" +
            "                    <acc:CompositeClass>\n" +
            "                        <acc:Name>COMPMIS4</acc:Name>\n" +
            "                    </acc:CompositeClass>\n" +
            "                    <acc:CompositeClass>\n" +
            "                        <acc:Name>COMPMIS5</acc:Name>\n" +
            "                    </acc:CompositeClass>\n" +
            "                    <acc:CompositeClass>\n" +
            "                        <acc:Name>COMPMIS6</acc:Name>\n" +
            "                    </acc:CompositeClass>\n" +
            "                    <acc:CompositeClass>\n" +
            "                        <acc:Name>COMPMIS7</acc:Name>\n" +
            "                    </acc:CompositeClass>\n" +
            "                    <acc:CompositeClass>\n" +
            "                        <acc:Name>COMPMIS8</acc:Name>\n" +
            "                    </acc:CompositeClass>\n" +
            "                    <acc:CompositeClass>\n" +
            "                        <acc:Name>COMPMIS9</acc:Name>\n" +
            "                    </acc:CompositeClass>\n" +
            "                    <acc:CompositeClass>\n" +
            "                        <acc:Name>COMPMIS10</acc:Name>\n" +
            "                    </acc:CompositeClass>\n" +
            "                    <acc:CostCode>\n" +
            "                        <acc:Name>COSTCOD1</acc:Name>\n" +
            "                    </acc:CostCode>\n" +
            "                    <acc:CostCode>\n" +
            "                        <acc:Name>COSTCOD2</acc:Name>\n" +
            "                    </acc:CostCode>\n" +
            "                    <acc:CostCode>\n" +
            "                        <acc:Name>COSTCOD3</acc:Name>\n" +
            "                    </acc:CostCode>\n" +
            "                    <acc:CostCode>\n" +
            "                        <acc:Name>COSTCOD4</acc:Name>\n" +
            "                    </acc:CostCode>\n" +
            "                    <acc:CostCode>\n" +
            "                        <acc:Name>COSTCOD5</acc:Name>\n" +
            "                    </acc:CostCode>\n" +
            "                </acc:MIS>\n" +
            "                <acc:CloseDate>2017-11-22</acc:CloseDate>\n" +
            "                <acc:OpenBalance>0.00</acc:OpenBalance>\n" +
            "                <acc:CurrentBalance>0.00</acc:CurrentBalance>\n" +
            "                <acc:GWSAccType>CURR</acc:GWSAccType>\n" +
            "                <acc:OperationTypeCodes>VIEW=0,DOMPAY=1,CUPADE=0,CUPACO=0,</acc:OperationTypeCodes>\n" +
            "            </acc:AccountDetails>\n" +
            "        </acc:AccountList>\n" +
            "    </soapenv:Body>\n" +
            "</soapenv:Envelope>";

}
