package ru.rbt.barsgl.ejbtesting.test;

import com.ibm.jms.JMSMessage;
import ru.rb.ucb.util.StringUtils;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.JMSQueueCommunicator;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.QueueCommunicator;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.QueueInputMessage;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.QueueProperties;
import ru.rbt.barsgl.ejb.integr.oper.MovementCommunicator;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import javax.jms.JMSConsumer;
import javax.jms.JMSException;
import javax.jms.Message;
import java.nio.charset.Charset;

/**
 * Created by er18837 on 25.04.2018.
 */
@Stateless
public class QueueTesting {

    @Inject
    @Any
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

    public JMSConsumer createConsumer(String queueName) {
        return queueCommunicator.createConsumer(queueName);
    }

    public void sendToQueue(String outMessage, QueueProperties queueProperties, String corrId, String replyTo, String queueName) throws JMSException {
        queueCommunicator.sendToQueue(outMessage, queueProperties, corrId, replyTo, queueName);
    }

    public QueueInputMessage receiveFromQueue(String queueName, String charsetName) throws JMSException {
        return queueCommunicator.receiveFromQueue(queueName, Charset.forName(charsetName));
    }

    public void sendToQueue(String outMessage, QueueProperties queueProperties, String corrId, String replyTo, String queueName, int cnt) throws JMSException {
        queueCommunicator.sendToQueue(outMessage, queueProperties, corrId, replyTo, queueName, cnt);
    }

    public long clearQueue(QueueProperties queueProperties, String queueName, long count) throws JMSException {
        startConnection(queueProperties);
        long n = queueCommunicator.clearQueue(queueName, count);

/*
        int i=0;
        for (; i<count; i++) {
            QueueInputMessage message = queueCommunicator.receiveFromQueue(queueName, Charset.forName("UTF-8"));
            if (null == message || StringUtils.isEmpty(message.getTextMessage()))
                break;
        }
*/
        System.out.println("Deleted from " + queueName + ": " + n);

        closeConnection();
        return n;
    }

/*
    public int clearQueue(QueueProperties queueProperties, String queueName, int count) throws JMSException {
        startConnection(queueProperties);
        JMSConsumer consumer = createConsumer(queueName);

        int i=0;
        for (; i<count; i++) {
            Message message = consumer.receiveNoWait();
            if (null == message)
                break;
        }
        System.out.println("Deleted from " + queueName + ": " + i);

        closeConnection();
        return i;
    }
*/

    public String getCommunicatorName() {
        return "QueueCommunicator class: " + queueCommunicator.toString();
    }
}