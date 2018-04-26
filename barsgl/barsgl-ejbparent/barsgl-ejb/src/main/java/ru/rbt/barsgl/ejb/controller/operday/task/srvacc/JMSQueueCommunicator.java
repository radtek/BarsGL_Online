package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import org.apache.log4j.Logger;
import ru.rbt.audit.controller.AuditController;

import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.jms.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static ru.rbt.audit.entity.AuditRecord.LogCode.QueueProcessor;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;

/**
 * Created by er18837 on 23.04.2018.
 */
@Default
public class JMSQueueCommunicator implements QueueCommunicator {
    private static final Logger log = Logger.getLogger(JMSQueueCommunicator.class);

    private JMSContext jmsContext = null;

    @EJB
    protected AuditController auditController;

    @Override
    public void startConnection(QueueProperties queueProperties) throws JMSException {
        if (jmsContext == null) {
            MQQueueConnectionFactory cf = new MQQueueConnectionFactory();
            cf.setHostName(queueProperties.mqHost);
            cf.setPort(queueProperties.mqPort);
            cf.setTransportType(WMQConstants.WMQ_CM_CLIENT);
            cf.setQueueManager(queueProperties.mqQueueManager);
            cf.setChannel(queueProperties.mqChannel);
            this.jmsContext = cf.createContext(queueProperties.mqUser, queueProperties.mqPassword, JMSContext.CLIENT_ACKNOWLEDGE);
            jmsContext.setExceptionListener((JMSException e) -> {
                log.info("\n\nonException calling");
                reConnect();
            });
        }
    }

    @Override
    public void reConnect() {
        log.info("\n\nreConnect calling");
        closeConnection();
        // На следующем старте задачи сработает startConnection()
    }

    @PreDestroy
    @Override
    public void closeConnection() {
        log.info("\n\ncloseConnection calling");
        try {
            if (jmsContext != null) {
                jmsContext.close();
            }
        } catch (Exception e) {
            auditController.warning(QueueProcessor, "Ошибка при закрытии соединения", null, e);
        }finally{
            jmsContext = null;
        }
    }

    @Override
    public JMSConsumer createConsumer(String inQueue){
        return jmsContext.createConsumer(jmsContext.createQueue("queue:///" + inQueue));
    }

    @Override
    public void sendToQueue(String outMessage, QueueProperties queueProperties, String corrId, String replyTo, String queue) throws JMSException {
        TextMessage message = jmsContext.createTextMessage(outMessage);
        message.setJMSCorrelationID(corrId);
        JMSProducer producer = jmsContext.createProducer();
        String queueName = (!isEmpty(replyTo) && !"true".equals(queueProperties.remoteQueueOut))
                ? replyTo : "queue:///" + queue;
        producer.send(jmsContext.createQueue(queueName), message);
    }

    @Override
    public QueueInputMessage receiveFromQueue(String inQueue, Charset cs) throws JMSException {
        JMSConsumer jmsConsumer = createConsumer(inQueue);
        Message receivedMessage = jmsConsumer.receiveNoWait();
        if (receivedMessage != null) {
            return readJMS(receivedMessage, cs);
        }
        return null;
    }

    @Override
    public QueueInputMessage readJMS(Message receivedMessage, Charset cs) throws JMSException {
        if(jmsContext != null && jmsContext.getSessionMode() == JMSContext.CLIENT_ACKNOWLEDGE)
            receivedMessage.acknowledge();
        String textMessage = null;
        if (receivedMessage instanceof TextMessage) {
            textMessage = ((TextMessage) receivedMessage).getText();
        } else if (receivedMessage instanceof BytesMessage) {
            BytesMessage bytesMessage = (BytesMessage) receivedMessage;

            int length = (int) bytesMessage.getBodyLength();
            byte[] incomingBytes = new byte[length];
            bytesMessage.readBytes(incomingBytes);

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(incomingBytes);
            try (Reader r = new InputStreamReader(byteArrayInputStream, cs)) { //} StandardCharsets.UTF_8)) {
                StringBuilder sb = new StringBuilder();
                char cb[] = new char[1024];
                int s = r.read(cb);
                while (s > -1) {
                    sb.append(cb, 0, s);
                    s = r.read(cb);
                }
                textMessage = sb.toString();
            } catch (IOException e) {
                log.error("Error during read message from QUEUE", e);
            }
        }
        if (textMessage == null) {
            return null;
        }
        return new QueueInputMessage(textMessage, receivedMessage.getJMSMessageID(),
                receivedMessage.getJMSReplyTo() == null ? null : receivedMessage.getJMSReplyTo().toString());
    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }
}
