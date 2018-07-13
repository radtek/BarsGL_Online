package ru.rbt.barsgl.ejb.controller.operday.task.srvacc.altqueue;

import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.QueueCommunicator;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.QueueInputMessage;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.QueueProperties;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.jms.*;
import java.nio.charset.Charset;
import java.util.PriorityQueue;

/**
 * Created by er18837 on 23.04.2018.
 */
@Alternative
public class QueueCommunicatorAlt implements QueueCommunicator {

    @Inject
    private QueueStorageAlt storageAlt;

    @Inject
    private QueueConsumerAlt consumerAlt;

    private QueueProperties queueProperties = null;

    @Override
    public void startConnection(QueueProperties queueProperties, boolean restart) throws JMSException {
        this.queueProperties = queueProperties;
    }

    @Override
    public void reConnect() {

    }

    @Override
    public void closeConnection() {

    }

    @Override
    public void sendToQueue(String outMessage, QueueProperties queueProperties, String corrId, String replyTo, String queue) throws JMSException {
        storageAlt.sendToQueue(queue, outMessage);
    }

    @Override
    public void sendToQueue(String outMessage, QueueProperties queueProperties, String corrId, String replyTo, String queue, int cnt) throws JMSException {
        storageAlt.sendToQueue(queue, outMessage, cnt);
    }

    @Override
    public QueueInputMessage receiveFromQueue(String inQueue, Charset cs) throws JMSException {
        Message receivedMessage = storageAlt.readFromQueue(inQueue);
        if (receivedMessage != null) {
            return readJMS(receivedMessage, cs);
        }
        return new QueueInputMessage(null, null, null);
    }

    @Override
    public Long clearQueue(String inQueue, Long cntmax) throws JMSException {
        return storageAlt.clearQueue(inQueue);
    }

    @Override
    public JMSConsumer createConsumer(String inQueue) {
        consumerAlt.setQueueName(inQueue);
        return consumerAlt;
    }

    @Override
    public QueueInputMessage readJMS(Message receivedMessage, Charset cs) throws JMSException {
        return new QueueInputMessage(((TextMessage) receivedMessage).getText(), receivedMessage.getJMSMessageID(),
                receivedMessage.getJMSReplyTo() == null ? null : receivedMessage.getJMSReplyTo().toString());
    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }

}
