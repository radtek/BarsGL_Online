package ru.rbt.barsgl.ejb.controller.operday.task.srvacc.newproc;

import javax.jms.JMSException;

/**
 * Created by er18837 on 23.04.2018.
 */
public interface JmsProcessor {

    void init(JmsCommunicator communicator) throws JMSException;
    void process() throws JMSException;
}
