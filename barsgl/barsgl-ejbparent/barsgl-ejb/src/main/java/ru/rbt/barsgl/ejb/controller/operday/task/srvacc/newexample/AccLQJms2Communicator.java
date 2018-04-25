package ru.rbt.barsgl.ejb.controller.operday.task.srvacc.newexample;

import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.QueueProperties;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.newproc.JmsCommunicator;

import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import java.util.Properties;

import static ru.rbt.audit.entity.AuditRecord.LogCode.QueueProcessor;

/**
 * Created by er18837 on 23.04.2018.
 */
public class AccLQJms2Communicator implements JmsCommunicator {

    private JMSContext jmsContext = null;


    @Override
    public void init(Properties properties) throws JMSException {

    }

    @Override
    public JMSProducer send(Message message) throws JMSException {
        return null;
    }

    @Override
    public Message receive() throws JMSException {
        return null;
    }

    private void startConnection(QueueProperties queueProperties) throws JMSException {
        if (jmsContext == null) {
            MQQueueConnectionFactory cf = new MQQueueConnectionFactory();
            cf.setHostName(queueProperties.mqHost);
            cf.setPort(queueProperties.mqPort);
            cf.setTransportType(WMQConstants.WMQ_CM_CLIENT);
            cf.setQueueManager(queueProperties.mqQueueManager);
            cf.setChannel(queueProperties.mqChannel);
            jmsContext = cf.createContext(queueProperties.mqUser, queueProperties.mqPassword, JMSContext.CLIENT_ACKNOWLEDGE);
//            jmsContext.setExceptionListener((JMSException e) -> {
//                log.info("\n\nonException calling");
//                reConnect();
//            });
        }
    }

    private void closeConnection() {
//        todo log.info("\n\ncloseConnection calling");
        try {
            if (jmsContext != null) {
                jmsContext.close();
            }
        } catch (Exception e) {
//            todo auditController.warning(QueueProcessor, "Ошибка при закрытии соединения", null, e);
        }finally{
            jmsContext = null;
        }
    }

//    private void setJmsContext(JMSContext jmsContext) {
//        this.jmsContext = jmsContext;
//    }
}
