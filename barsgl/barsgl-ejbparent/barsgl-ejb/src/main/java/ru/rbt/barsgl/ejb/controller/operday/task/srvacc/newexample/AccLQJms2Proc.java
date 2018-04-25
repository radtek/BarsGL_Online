package ru.rbt.barsgl.ejb.controller.operday.task.srvacc.newexample;

import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.newproc.JmsCommunicator;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.newproc.JmsProcessor;

import javax.jms.Message;

/**
 * Created by er18837 on 23.04.2018.
 */
public class AccLQJms2Proc implements JmsProcessor {

    private JmsCommunicator communicator;

    @Override
    public void init(JmsCommunicator communicator) {
        if (null != communicator) {
            this.communicator = communicator;
        }
    }

    @Override
    public void process() {
/*
        for (int i =0 ; i <= 99; i++) {
            Message message = communicator.receive();
            processOneMessage(message);
        }
*/
    }

    private void processOneMessage(Message message) {
        /// todo
    }
}
