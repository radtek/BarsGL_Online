package ru.rbt.barsgl.ejb.controller.operday.task.srvacc.altqueue;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.JMSConsumer;
import javax.jms.JMSRuntimeException;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.io.Serializable;

/**
 * Created by er18837 on 24.04.2018.
 */
public class QueueConsumerAlt implements JMSConsumer {

    @Inject
    private QueueStorageAlt storageAlt;

    private String queueName;

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    @Override
    public String getMessageSelector() {
        return null;
    }

    @Override
    public MessageListener getMessageListener() throws JMSRuntimeException {
        return null;
    }

    @Override
    public void setMessageListener(MessageListener listener) throws JMSRuntimeException {

    }

    @Override
    public Message receive() {
        return storageAlt.readFromQueue(queueName);
    }

    @Override
    public Message receive(long timeout) {
        return storageAlt.readFromQueue(queueName);
    }

    @Override
    public Message receiveNoWait() {
        return storageAlt.readFromQueue(queueName);
    }

    @Override
    public void close() {

    }

    @Override
    public <T> T receiveBody(Class<T> c) {
        return null;
    }

    @Override
    public <T> T receiveBody(Class<T> c, long timeout) {
        return null;
    }

    @Override
    public <T> T receiveBodyNoWait(Class<T> c) {
        return null;
    }
}
