package ru.rbt.barsgl.ejb.controller.operday.task;

import com.ibm.jms.JMSBytesMessage;
import com.ibm.jms.JMSMessage;
import com.ibm.jms.JMSTextMessage;
import com.ibm.mq.jms.*;
import com.ibm.msg.client.wmq.WMQConstants;
import ru.rbt.barsgl.ejb.security.AuditController;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;

import javax.ejb.EJB;
import javax.jms.JMSException;
import javax.jms.Session;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;

import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.TestRunner;

/**
 * Created by ER22228 on 28.06.2016.
 * Класс для тестирования приема/отправки сообщений на тестовой среде
 * запускается вручную через приложение barsgl
 *
 * Скрипт для GL_SCHED
 * INSERT INTO DWH.GL_SCHED (TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('TestRunnerTask', '', 'Test Run Task', 'STOPPED', 'MANUAL', 'SINGLE', 'ru.rbt.barsgl.ejb.controller.operday.task.TestRunnerTask', 0, 0, '');
 *
 * В Система/Задания выбрать задачу и в Свойства задать режим
 * mode=send - отправка сообщения
 * mode=read - чтение сообщения
 * по задаче №53 - AccountQueryTask.java
 */
public class TestRunnerTask implements ParamsAwareRunnable {

    // INSERT INTO TMB07.DWH.GL_SCHED (TSKNM, PROPS, DESCR, STATE, STR_TYPE, SCH_TYPE, RUN_CLS, DELAY, INTERVAL, SCH_EXPR) VALUES ('TestRunnerTask', '', 'Test Run Task', 'STOPPED', 'MANUAL', 'SINGLE', 'ru.rbt.barsgl.ejb.controller.operday.task.TestRunnerTask', 0, 0, '');

    @EJB
    private AuditController auditController;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        auditController.info(TestRunner, "Старт задачи TestRunnerTask", null, "");

        MQQueueConnectionFactory cf = new MQQueueConnectionFactory();
        cf.setHostName("172.17.77.144");
        cf.setPort(1414);
        cf.setTransportType(WMQConstants.WMQ_CM_CLIENT);
        cf.setQueueManager("QM_MBROKER4_T5");
        cf.setChannel("SYSTEM.ADMIN.SVRCONN");

        if ("send".equals(properties.getProperty("mode"))) {
            auditController.info(TestRunner, "Старт отправки", null, "");
            sendToQueue(cf, "UCBRU.ADP.BARSGL.V2.ACLIQU.REQUEST", testALQ.getBytes(), "UCBRU.ADP.BARSGL.V2.ACLIQU.RESPONSE", "srvwbl4mqtest", "UsATi8hU");
            auditController.info(TestRunner, "Окончание отправки", null, "");
        } else {
            auditController.info(TestRunner, "Старт получения", null, "");
            receiveFromQueue(cf, "UCBRU.ADP.BARSGL.V2.ACLIQU.RESPONSE", "srvwbl4mqtest", "UsATi8hU");
            auditController.info(TestRunner, "Окончание получения", null, "");
        }
    }

    private void receiveFromQueue(MQQueueConnectionFactory cf, String queueName, String username, String password) throws JMSException {
        MQQueueConnection connection = (MQQueueConnection) cf.createQueueConnection(username, password);
        MQQueueSession session = (MQQueueSession) connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        MQQueue queue = (MQQueue) session.createQueue("queue:///" + queueName);//UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF
        MQQueueReceiver receiver = (MQQueueReceiver) session.createReceiver(queue);

        connection.start();

        String result = readFromJMS(receiver);
        auditController.error(TestRunner, "Старт задачи TestRunnerTask", null, result);

//        sender.close();
        receiver.close();
        session.close();
        connection.close();
    }

    private String readFromJMS(MQMessageConsumer receiver) throws JMSException {
        JMSMessage receivedMessage = (JMSMessage) receiver.receive(100);
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

    private void sendToQueue(MQQueueConnectionFactory cf, String queueName, byte[] incomingMessage, String replyToQ, String username, String password) {
        try {
            auditController.info(TestRunner, "Отправка. Коннекшн", null, "");
            MQQueueConnection connection = (MQQueueConnection) cf.createQueueConnection(username, password);
            auditController.info(TestRunner, "Отправка. Коннекшн получен", null, "");
            MQQueueSession session = (MQQueueSession) connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            MQQueue queue = (MQQueue) session.createQueue("queue:///" + queueName);//UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF
            MQQueueSender sender = (MQQueueSender) session.createSender(queue);
            MQQueueReceiver receiver = (MQQueueReceiver) session.createReceiver(queue);

            connection.start();

            JMSBytesMessage bytesMessage = (JMSBytesMessage) session.createBytesMessage();
            bytesMessage.writeBytes(incomingMessage);
            MQQueue queueR2Q = (MQQueue) session.createQueue("queue:///" + replyToQ);
            bytesMessage.setJMSReplyTo(queueR2Q);
            sender.send(bytesMessage);
            System.out.println("Sent message");

            sender.close();
            receiver.close();
            session.close();
            connection.close();
        }catch (Exception e){
            auditController.error(TestRunner, "Отправка. Коннекшн", null, e);
        }
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

    String testALQ =
        "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "\t<SOAP-ENV:Header>\n" +
            "\t\t<gbo:UCBRUHeaders xmlns:gbo=\"urn:asbo:barsgl\">\n" +
            "\t\t\t<gbo:Correlation>\t\t\t\t\n" +
            "\t\t\t\t\t<gbo:XRef>1234567847567488</gbo:XRef>\n" +
            "\t\t\t\t<gbo:Segmentation>\n" +
            "\t\t\t\t\t<gbo:CanSegmentResponse>true</gbo:CanSegmentResponse>\n" +
            "\t\t\t\t</gbo:Segmentation>\n" +
            "\t\t\t</gbo:Correlation>\n" +
            "\t\t\t<gbo:Security/>\n" +
            "\t\t\t<gbo:Audit>\n" +
            "\t\t\t\t<gbo:MessagePath>\n" +
            "\t\t\t\t\t<gbo:Step>\n" +
            "\t\t\t\t\t\t<gbo:Application.Module>String</gbo:Application.Module>\n" +
            "\t\t\t\t\t\t<gbo:VersionId>String</gbo:VersionId>\n" +
            "\t\t\t\t\t\t<gbo:TimeStamp>2001-12-17T09:30:47Z</gbo:TimeStamp>\n" +
            "\t\t\t\t\t\t<gbo:RoutingRole>String</gbo:RoutingRole>\n" +
            "\t\t\t\t\t\t<gbo:Comment>String</gbo:Comment>\n" +
            "\t\t\t\t\t</gbo:Step>\n" +
            "\t\t\t\t</gbo:MessagePath>\n" +
            "\t\t\t\t<gbo:ProcessInfo>\n" +
            "\t\t\t\t\t<gbo:Name>String</gbo:Name>\n" +
            "\t\t\t\t\t<gbo:InstanceId>String</gbo:InstanceId>\n" +
            "\t\t\t\t</gbo:ProcessInfo>\n" +
            "\t\t\t</gbo:Audit>\n" +
            "\t\t\t<gbo:Usability>\n" +
            "\t\t\t\t<gbo:Internationalization>\n" +
            "\t\t\t\t\t<gbo:Language>aa</gbo:Language>\n" +
            "\t\t\t\t</gbo:Internationalization>\n" +
            "\t\t\t\t<gbo:Fetch>\n" +
            "\t\t\t\t\t<gbo:MaxRecords>0</gbo:MaxRecords>\n" +
            "\t\t\t\t\t<gbo:MoreRecordsAvailable>true</gbo:MoreRecordsAvailable>\n" +
            "\t\t\t\t\t<gbo:RecordCount>0</gbo:RecordCount>\n" +
            "\t\t\t\t</gbo:Fetch>\n" +
            "\t\t\t</gbo:Usability>\n" +
            "\t\t\t<gbo:Tools>\n" +
            "\t\t\t\t<gbo:Environment/>\n" +
            "\t\t\t</gbo:Tools>\n" +
            "\t\t</gbo:UCBRUHeaders>\n" +
            "\t</SOAP-ENV:Header>\n" +
            "\t<SOAP-ENV:Body>\n" +
            "\t\t<asbo:AccountListQuery xmlns:gbo=\"urn:asbo:barsgl\">\n" +
            "\t\t\t<asbo:AccountQuery>\n" +
            "\t\t\t\t<asbo:CustomerNo>911641</asbo:CustomerNo>\n" +
            "\t\t\t\t<asbo:AccountSpecials>PRCA1</asbo:AccountSpecials>\n" +
            "\t\t\t\t<asbo:AccountSpecials>PRCD2</asbo:AccountSpecials>\n" +
            "\t\t\t\t<asbo:AccountSpecials>4371</asbo:AccountSpecials>\n" +
            "\t\t\t</asbo:AccountQuery>\n" +
            "\t\t\t<asbo:AccountQuery>\n" +
            "\t\t\t\t<asbo:CustomerNo>00597197</asbo:CustomerNo>\n" +
            "\t\t\t</asbo:AccountQuery>\n" +
            "\t\t\t<asbo:AccountQuery>\n" +
            "\t\t\t\t<asbo:CustomerNo>00500633</asbo:CustomerNo>\n" +
            "\t\t\t</asbo:AccountQuery>\t\t\n" +
            "            <asbo:AccountQuery>\n" +
            "\t\t\t\t<asbo:CustomerNo>00000018</asbo:CustomerNo>\n" +
            "\t\t\t\t<asbo:AccountingType>702010300</asbo:AccountingType>\n" +
            "\t\t\t\t<asbo:AccountingType>758030400</asbo:AccountingType>\n" +
            "            </asbo:AccountQuery>\n" +
            "\t\t\t<asbo:AccountNumber>42309810800504735550</asbo:AccountNumber>\t\t\t\n" +
            "\t\t</asbo:AccountListQuery>\n" +
            "\t</SOAP-ENV:Body>\n" +
            "</SOAP-ENV:Envelope>";
}
