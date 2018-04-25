package ru.rbt.barsgl.ejb.controller.operday.task.srvacc.newproc;

import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import java.util.Properties;

/**
 * Created by er18837 on 23.04.2018.
 */
public interface JmsCommunicator {

    void init(Properties properties) throws JMSException;
    JMSProducer send(Message message)  throws JMSException;
    Message receive()  throws JMSException;
}
