package ru.rbt.barsgl.ejbtest;

import com.ibm.jms.JMSBytesMessage;
import com.ibm.jms.JMSMessage;
import com.ibm.jms.JMSTextMessage;
import com.ibm.mq.jms.*;
import com.ibm.msg.client.wmq.WMQConstants;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;

import javax.jms.JMSException;
import javax.jms.Session;
import java.io.*;
import java.util.logging.Logger;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;

/**
 * Created by ER22228
 */
public class AccountQueryTest extends AbstractTimerJobTest {

    public static final Logger logger = Logger.getLogger(AccountQueryTest.class.getName());

    @Test
    @Ignore
    public void testFull() throws Exception {
        // SYSTEM.DEF.SVRCONN/TCP/vs338(1414)
        // SYSTEM.ADMIN.SVRCONN/TCP/vs338(1414)
        // UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF

        MQQueueConnectionFactory cf = new MQQueueConnectionFactory();


        // Config
        cf.setHostName("172.17.77.144");
        cf.setPort(1414);
        cf.setTransportType(WMQConstants.WMQ_CM_CLIENT);
        cf.setQueueManager("QM_MBROKER4_T5");
        cf.setChannel("SYSTEM.DEV.SVRCONN");
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
//        sendToQueue(cf,"UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF", AccountQueryProcessor.fullTopicTestA);
//        sendToQueue(cf,"UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF", AccountQueryBAProcessor.fullTopicTestB);        
        sendToQueue(cf, "UCBRU.ADP.BARSGL.V2.ACLIQU.REQUEST",new File("c:\\develop\\Projects\\er21775\\task53\\AccountListQuery-Simple.xml"));
//        sendToQueue(cf, "UCBRU.ADP.BARSGL.V2.ACLIQU.REQUEST",new File("C:\\Projects\\task53\\AccountListQuery-Simple.xml"));
//        sendToQueue(cf, "UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF",new File("C:\\Projects\\task53\\AccountBalanceListQuery-B1"));
//        sendToQueue(cf, "UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF",new File("C:\\Projects\\task53\\AccountBalanceListQuery-B2"));
//        sendToQueue(cf, "UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF", new File("C:\\Projects\\task53\\AccountBalanceListQuery-B3.txt"));
//        sendToQueue(cf, "UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF",new File("C:\\Projects\\task53\\AccountListQuery-Over100000.xml"));

        /*
        SingleActionJob job =
            SingleActionJobBuilder.create()
                .withClass(AccountQueryTask.class)
                .withProps(
                    "mq.type = queue\n" +
                        "mq.host = vs338\n" +
                        "mq.port = 1414\n" +
                        "mq.queueManager = QM_MBROKER10_TEST\n" +
                        "mq.channel = SYSTEM.DEF.SVRCONN\n" +
                        "mq.batchSize = 30\n" + //todo
                        "mq.topics = LIRQ:UCBRU.ADP.BARSGL.V2.ACLIQU.REQUEST:UCBRU.ADP.BARSGL.V2.ACLIQU.RESPONSE"+
                        ";BALIRQ:UCBRU.ADP.BARSGL.V3.ACBALIQU.REQUEST:UCBRU.ADP.BARSGL.V3.ACBALIQU.RESPONSE\n"+
//                        "mq.topics = LIRQ:UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF:UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF" +
//                        ";BALIRQ:UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF:UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF\n" +
                        "mq.user=srvwbl4mqtest\n" +
                        "mq.password=UsATi8hU"

                )//;MIDAS_UPDATE:UCBRU.ADP.BARSGL.V4.ACDENO.MDSUPD.NOTIF
                .build();
        jobService.executeJob(job);
*/
//        receiveFromQueue(cf,"UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF");
        receiveFromQueue(cf,"UCBRU.ADP.BARSGL.V2.ACLIQU.RESPONSE");
        System.out.println();

    }

    private void receiveFromQueue(MQQueueConnectionFactory cf, String queueName) throws JMSException {
        MQQueueConnection connection = (MQQueueConnection) cf.createQueueConnection("srvwbl4mqtest","UsATi8hU");
        MQQueueSession session = (MQQueueSession) connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        MQQueue queue = (MQQueue) session.createQueue("queue:///" + queueName);//UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF
//        MQQueueSender sender = (MQQueueSender) session.createSender(queue);
        MQQueueReceiver receiver = (MQQueueReceiver) session.createReceiver(queue);

        connection.start();

//        JMSMessage receivedMessage = (JMSMessage) receiver.receive(10000);
//        System.out.println("\\nReceived message:\\n" + receivedMessage);

        System.out.println("\\nReceived message:\\n" + readFromJMS(receiver));

//        sender.close();
        receiver.close();
        session.close();
        connection.close();
    }

    private String readFromJMS(MQMessageConsumer receiver) throws JMSException {
        JMSMessage receivedMessage = (JMSMessage) receiver.receive(10000);
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

    private void sendToQueue(MQQueueConnectionFactory cf, String queueName, File file) throws JMSException {
        byte[] incomingMessage = null;
        try {
            incomingMessage = FileUtils.readFileToByteArray(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (isEmpty(incomingMessage)) {
            System.exit(1);
        }

        MQQueueConnection connection = (MQQueueConnection) cf.createQueueConnection("srvwbl4mqtest","UsATi8hU");
        MQQueueSession session = (MQQueueSession) connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        MQQueue queue = (MQQueue) session.createQueue("queue:///" + queueName);//UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF
        MQQueueSender sender = (MQQueueSender) session.createSender(queue);
        MQQueueReceiver receiver = (MQQueueReceiver) session.createReceiver(queue);

        connection.start();

        JMSBytesMessage bytesMessage = (JMSBytesMessage) session.createBytesMessage();
        bytesMessage.writeBytes(incomingMessage);
        sender.send(bytesMessage);
        System.out.println("Sent message");

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