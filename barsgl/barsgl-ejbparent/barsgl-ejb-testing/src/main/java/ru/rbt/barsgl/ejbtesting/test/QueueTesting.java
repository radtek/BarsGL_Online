package ru.rbt.barsgl.ejbtesting.test;

import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.JMSQueueCommunicator;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.QueueCommunicator;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.QueueProperties;
import ru.rbt.barsgl.ejb.integr.oper.MovementCommunicator;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import javax.jms.JMSConsumer;
import javax.jms.JMSException;
import javax.jms.Message;

/**
 * Created by er18837 on 25.04.2018.
 */
@Stateless
public class QueueTesting {

    @Inject @Any
    private QueueCommunicator queueCommunicator;

    public void startConnection(QueueProperties queueProperties) throws JMSException {
        queueCommunicator.startConnection(queueProperties);
    }

    public void reConnect() {
        queueCommunicator.reConnect();
    }

    public void closeConnection() {
        queueCommunicator.closeConnection();
    }

    public JMSConsumer createConsumer(String inQueue) {
        return queueCommunicator.createConsumer(inQueue);
    }

    public void acknowledge(Message receivedMessage) throws JMSException {
        queueCommunicator.acknowledge(receivedMessage);
    }

    public void sendToQueue(String outMessage, QueueProperties queueProperties, String corrId, String replyTo, String queue) throws JMSException {
        queueCommunicator.sendToQueue(outMessage, queueProperties, corrId, replyTo, queue);
    }

}
