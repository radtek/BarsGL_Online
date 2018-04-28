package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;

import javax.annotation.PreDestroy;
import javax.jms.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static ru.rbt.audit.entity.AuditRecord.LogCode.QueueProcessor;

/**
 * Created by er18837 on 23.04.2018.
 */
public interface QueueCommunicator {
    void startConnection(QueueProperties queueProperties) throws JMSException;

    void reConnect();

    void closeConnection();

    void sendToQueue(String outMessage, QueueProperties queueProperties, String corrId, String replyTo, String queue) throws JMSException;

    void sendToQueue(String outMessage, QueueProperties queueProperties, String corrId, String replyTo, String queue, int cnt) throws JMSException;

    QueueInputMessage receiveFromQueue(String inQueue, Charset cs) throws JMSException;

    Long clearQueue(String inQueue, Long cntmax) throws JMSException;

    JMSConsumer createConsumer(String inQueue);

    QueueInputMessage readJMS(Message receivedMessage, Charset cs) throws JMSException;

}
