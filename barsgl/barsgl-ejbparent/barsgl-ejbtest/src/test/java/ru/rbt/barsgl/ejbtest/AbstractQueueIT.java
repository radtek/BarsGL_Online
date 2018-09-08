package ru.rbt.barsgl.ejbtest;

import com.ibm.jms.JMSBytesMessage;
import com.ibm.jms.JMSMessage;
import com.ibm.jms.JMSTextMessage;
import com.ibm.mq.jms.*;
import com.ibm.msg.client.wmq.WMQConstants;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.CommonQueueController;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.QueueInputMessage;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.QueueProperties;
import ru.rbt.barsgl.ejbtest.mq.MqUtil;
import ru.rbt.barsgl.ejbtesting.test.QueueTesting;
import ru.rbt.ejbcore.util.StringUtils;

import javax.jms.JMSConsumer;
import javax.jms.JMSException;
import javax.jms.Session;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Properties;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;

/**
 * Created by er18837 on 23.03.2018.
 */
public class AbstractQueueIT extends AbstractTimerJobIT {
    public static final String mqTestLogin = "srvwbl4mqtest";
    public static final String mqTestPassw = "JxQGk7nJ";

    public QueueProperties getQueueProperties (String topic, String inQueue, String outQueue, String ahost, int aport, String abroker, String achannel,
                                                String alogin, String apassw, int batchSize, boolean writeOut, boolean remoteQueueOut) {
        return MqUtil.getQueueProperties (topic, inQueue, outQueue, ahost, aport, abroker, achannel, alogin, apassw, batchSize, writeOut, remoteQueueOut);
    }

    public String getJobProperty(QueueProperties queueProperties) {
        return MqUtil.getJobProperty(queueProperties);
    }

    public  String getJobProperty (String topic, String inQueue, String outQueue, String ahost, String aport, String abroker, String achannel,
                                   String alogin, String apassw, String batchSize, boolean writeOut, boolean remoteQueueOut) {
        return MqUtil.getJobProperty(topic, inQueue, outQueue, ahost, aport, abroker, achannel, alogin, apassw, batchSize, writeOut, remoteQueueOut);
    }

    public String getJobProperty (String topic, String inQueue, String outQueue, String ahost, String aport, String abroker, String achannel,
                                  String alogin, String apassw, String batchSize, boolean writeOut) {
        return getJobProperty (topic, inQueue, outQueue, ahost, aport, abroker, achannel, alogin, apassw, batchSize, writeOut, false);
    }

    public void testProperties(String propStr, boolean isError) throws Exception {
        System.out.print(propStr);
        Properties properties = new Properties();
        properties.load(new StringReader(propStr));
        try {
            QueueProperties queueProperties = new QueueProperties(properties);
            System.out.println(queueProperties.toString());
            Assert.assertFalse(isError);
        } catch (Throwable e) {
            System.out.println("Error!");
            Assert.assertTrue(isError);
        }
        System.out.println();
    }

    // ========================================================
    public static void printCommunicatorName() {
        System.out.println((String) remoteAccess.invoke(QueueTesting.class, "getCommunicatorName"));
    }

    public static void startConnection(QueueProperties queueProperties) throws JMSException {
        remoteAccess.invoke(QueueTesting.class, "startConnection", queueProperties);
    }

    public static void closeConnection() throws JMSException {
        remoteAccess.invoke(QueueTesting.class, "closeConnection");
    }

    public static void sendToQueue(String outMessage, QueueProperties queueProperties, String corrId, String replyTo, String queue) throws JMSException {
        remoteAccess.invoke(QueueTesting.class, "sendToQueue", outMessage, queueProperties, corrId, replyTo, queue);
    }

//    public static void answerToQueue(String incomingMessage, QueueProperties queueProperties, String corrId, String username, String password) throws JMSException {
//        sendToQueue(incomingMessage, null, corrId, username, password, 1);
//    }

    public static void sendToQueue(File file, Charset charset, QueueProperties queueProperties, String corrId, String replyTo, String queue) throws JMSException, IOException {
        sendToQueue(FileUtils.readFileToString(file, charset), queueProperties, corrId, replyTo, queue);
    }

    public static void sendToQueue(String outMessage, QueueProperties queueProperties, String corrId, String replyTo, String queueName, int cnt) throws JMSException {
        remoteAccess.invoke(QueueTesting.class, "sendToQueue", outMessage, queueProperties, corrId, replyTo, queueName, cnt);
        System.out.println("Sent to " + queueName + ": " + cnt);
    }

    public static QueueInputMessage receiveFromQueue(String queueName, Charset cs) throws JMSException {
        return remoteAccess.invoke(QueueTesting.class, "receiveFromQueue", queueName, cs.name());
    }

    public static long clearQueue(QueueProperties queueProperties, String queueName, long count) throws JMSException {
        long n =  remoteAccess.invoke(QueueTesting.class, "clearQueue", queueProperties, queueName, count);
        System.out.println("Deleted from " + queueName + ": " + n);
        return n;
    }
}
