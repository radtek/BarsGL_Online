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
import javax.jms.JMSConsumer;
import javax.jms.JMSException;
import javax.jms.Message;
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
    public void startConnection(QueueProperties queueProperties) throws JMSException {
        this.queueProperties = queueProperties;
    }

    @Override
    public void reConnect() {

    }

    @Override
    public void closeConnection() {

    }

    @Override
    public JMSConsumer createConsumer(String inQueue) {
        consumerAlt.setQueueName(inQueue);
        return consumerAlt;
    }

    @Override
    public void acknowledge(Message receivedMessage) throws JMSException {

    }

    @Override
    public void sendToQueue(String outMessage, QueueProperties queueProperties, String corrId, String replyTo, String queue) throws JMSException {
        storageAlt.sendToQueue(queue, outMessage);
    }
}
