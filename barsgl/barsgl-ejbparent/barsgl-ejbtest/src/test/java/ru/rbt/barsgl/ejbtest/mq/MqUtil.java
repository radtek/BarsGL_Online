package ru.rbt.barsgl.ejbtest.mq;

import com.ibm.jms.JMSBytesMessage;
import com.ibm.jms.JMSMessage;
import com.ibm.jms.JMSTextMessage;
import com.ibm.mq.jms.*;
import com.ibm.msg.client.wmq.WMQConstants;
import org.apache.commons.io.FileUtils;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.CommonQueueController;
import ru.rbt.ejbcore.util.StringUtils;

import javax.jms.JMSException;
import javax.jms.Session;
import java.io.*;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;

/**
 * Created by er18837 on 27.04.2018.
 */
public class MqUtil {

    public  static String getQueueProperty (String topic, String inQueue, String outQueue, String ahost, String aport, String abroker, String achannel, String alogin, String apassw,
                                            String batchSize, boolean writeOut) {
        return getQueueProperty (topic, inQueue, outQueue, ahost, aport, abroker, achannel, alogin, apassw, batchSize, writeOut, false);
    }

    public  static String getQueueProperty (String topic, String inQueue, String outQueue, String ahost, String aport, String abroker, String achannel, String alogin, String apassw,
                                            String batchSize, boolean writeOut, boolean remoteQueueOut) {
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
                + "remoteQueueOut=" + remoteQueueOut +"\n"

                ;
    }

    public  static MQQueueConnectionFactory getConnectionFactory(String ahost, String abroker, String achannel) throws JMSException {
        MQQueueConnectionFactory cf = new MQQueueConnectionFactory();

        cf.setHostName(ahost);
        cf.setPort(1414);
        cf.setTransportType(WMQConstants.WMQ_CM_CLIENT);
        cf.setQueueManager(abroker);
        cf.setChannel(achannel);
        return cf;
    }

    public static int clearQueue(MQQueueConnectionFactory cf, String queueName, String username, String password, int count) throws JMSException {
        MQQueueConnection connection = (MQQueueConnection) cf.createQueueConnection(username, password);
        MQQueueSession session = (MQQueueSession) connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        MQQueue queue = (MQQueue) session.createQueue("queue:///" + queueName);//UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF
//        MQQueueSender sender = (MQQueueSender) session.createSender(queue);
        MQQueueReceiver receiver = (MQQueueReceiver) session.createReceiver(queue);

        connection.start();

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

    public static CommonQueueController.QueueInputMessage receiveFromQueue(MQQueueConnectionFactory cf, String queueName, String username, String password) throws JMSException {
        MQQueueConnection connection = (MQQueueConnection) cf.createQueueConnection(username, password);
        MQQueueSession session = (MQQueueSession) connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        MQQueue queue = (MQQueue) session.createQueue("queue:///" + queueName);//UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF
//        MQQueueSender sender = (MQQueueSender) session.createSender(queue);
        MQQueueReceiver receiver = (MQQueueReceiver) session.createReceiver(queue);

        connection.start();
        CommonQueueController.QueueInputMessage answer = readFromJMS(receiver);

//        sender.close();
        receiver.close();
        session.close();
        connection.close();
        return answer;
    }

    public static CommonQueueController.QueueInputMessage readFromJMS(MQMessageConsumer receiver) throws JMSException {
        JMSMessage receivedMessage = (JMSMessage) receiver.receive(100);
        if (null == receivedMessage)
            return new CommonQueueController.QueueInputMessage(null);
        receivedMessage.acknowledge();
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
        return new CommonQueueController.QueueInputMessage(textMessage, receivedMessage.getJMSMessageID(),
                (receivedMessage.getJMSReplyTo() == null ? null : receivedMessage.getJMSReplyTo().toString()));
    }

    public static void answerToQueue(MQQueueConnectionFactory cf, String queueName, File file, String correlationId, String username, String password) throws JMSException {
        byte[] incomingMessage = null;
        try {
            incomingMessage = FileUtils.readFileToByteArray(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        answerToQueue(cf, queueName, incomingMessage, correlationId, username, password);
    }

    public static void answerToQueue(MQQueueConnectionFactory cf, String queueName, byte[] incomingMessage, String correlationId, String username, String password) throws JMSException {
        sendToQueue(cf, queueName, incomingMessage, null, correlationId, username, password, 1);
    }

    public static void sendToQueue(MQQueueConnectionFactory cf, String queueName, File file, String replyToQ, String username, String password) throws JMSException {
        sendToQueue (cf, queueName, file, replyToQ, username, password, 1);
    }

    public static  void sendToQueue(MQQueueConnectionFactory cf, String queueName, File file, String replyToQ, String username, String password, int cnt) throws JMSException {
        byte[] incomingMessage = null;
        try {
            incomingMessage = FileUtils.readFileToByteArray(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        sendToQueue(cf, queueName, incomingMessage, replyToQ, null, username, password, cnt);
    }

    public static void sendToQueue(MQQueueConnectionFactory cf, String queueName, byte[] incomingMessage, String correlationId, String username, String password) throws JMSException {
        sendToQueue(cf, queueName, incomingMessage, null, correlationId, username, password, 1);
    }

    public static  void sendToQueue(MQQueueConnectionFactory cf, String queueName, byte[] incomingMessage, String replyToQ, String correlationId, String username, String password, int cnt) throws JMSException {
        if (isEmpty(incomingMessage)) {
            System.exit(1);
        }

        MQQueueConnection connection = (MQQueueConnection) cf.createQueueConnection(username, password);
        MQQueueSession session = (MQQueueSession) connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        MQQueue queue = (MQQueue) session.createQueue("queue:///" + queueName);//UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF
        MQQueueSender sender = (MQQueueSender) session.createSender(queue);
//        MQQueueReceiver receiver = (MQQueueReceiver) session.createReceiver(queue);

        connection.start();

        JMSBytesMessage bytesMessage = (JMSBytesMessage) session.createBytesMessage();
        bytesMessage.writeBytes(incomingMessage);
        if (!StringUtils.isEmpty(replyToQ)) {
            MQQueue queueR2Q = (MQQueue) session.createQueue("queue:///" + replyToQ);
            bytesMessage.setJMSReplyTo(queueR2Q);
        }
        if (!StringUtils.isEmpty(correlationId))
            bytesMessage.setJMSCorrelationID(correlationId);

        for(int i=0; i<cnt; i++)
            sender.send(bytesMessage);
        System.out.println(String.format("Sent %d message to %s", cnt, queueName));

        sender.close();
//        receiver.close();
        session.close();
        connection.close();
    }

    private static void sendToQueue(MQQueueConnectionFactory cf, String queueName, String fullTopicTest) throws JMSException {
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

}
