package ru.rbt.barsgl.ejb.controller.operday.task;

import com.ibm.jms.JMSTextMessage;
import com.ibm.mq.jms.*;
import com.ibm.msg.client.wmq.WMQConstants;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.QueueProperties;
import ru.rbt.barsgl.audit.entity.AuditRecord;
import ru.rbt.barsgl.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;

import javax.ejb.EJB;
import javax.jms.JMSException;
import javax.jms.Session;
import java.util.Properties;

/**
 * ER22228
 * Для теста MovementCreateProcessor
 */
public class SRVACCTester implements ParamsAwareRunnable {
    @EJB
    private AuditController auditController;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        QueueProperties queueProperties = new QueueProperties(properties);
        try {
            MQQueueConnectionFactory cf = new MQQueueConnectionFactory();

            cf.setHostName(queueProperties.mqHost);
            cf.setPort(queueProperties.mqPort);
            cf.setTransportType(WMQConstants.WMQ_CM_CLIENT);
            cf.setQueueManager(queueProperties.mqQueueManager);
            cf.setChannel(queueProperties.mqChannel);

            for (String item : queueProperties.mqTopics.split(";")) {
                if(item.startsWith("LIRQ")) {
                    String[] params = item.split(":");
                    auditController.info(AuditRecord.LogCode.AccountListTester,item);
                    sendToQueue(cf, params[1], aqTest, queueProperties);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendToQueue(MQQueueConnectionFactory cf, String queueName, String fullTopicTest, QueueProperties queueProperties) throws JMSException {
        MQQueueConnection connection = (MQQueueConnection) cf.createQueueConnection(queueProperties.mqUser, queueProperties.mqPassword);
        MQQueueSession session = (MQQueueSession) connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        MQQueue queue = (MQQueue) session.createQueue("queue:///" + queueName);//UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF
        MQQueueSender sender = (MQQueueSender) session.createSender(queue);

        connection.start();

        JMSTextMessage message = (JMSTextMessage) session.createTextMessage(fullTopicTest);
        sender.send(message);
        System.out.println("Sent message:\\n" + message);

        sender.close();
        session.close();
        connection.close();
    }

    String aqTest="<?xml version='1.0' encoding='UTF-8'?>\n" +
                      "\t\t<asbo:AccountListQuery xmlns:gbo=\"urn:asbo:barsgl\">\n" +
                      "\t\t\t<asbo:AccountQuery>\n" +
                      "\t\t\t\t<asbo:CustomerNo>911641</asbo:CustomerNo>\n" +
                      "\t\t\t\t<asbo:AccountSpecials>PRCA1</asbo:AccountSpecials>\n" +
                      "\t\t\t\t<asbo:AccountSpecials>PRCD2</asbo:AccountSpecials>\n" +
                      "\t\t\t\t<asbo:AccountSpecials>4371</asbo:AccountSpecials>\n" +
                      "\t\t\t</asbo:AccountQuery>\n" +
                      "\t\t\t<asbo:AccountQuery>\n" +
                      "\t\t\t\t<asbo:CustomerNo>00597197</asbo:CustomerNo>\n" +
                      "\t\t\t</asbo:AccountQuery>\n" +
                      "\t\t\t<asbo:AccountQuery>\n" +
                      "\t\t\t\t<asbo:CustomerNo>00500633</asbo:CustomerNo>\n" +
                      "\t\t\t</asbo:AccountQuery>\t\t\n" +
                      "            <asbo:AccountQuery>\n" +
                      "\t\t\t\t<asbo:CustomerNo>00000018</asbo:CustomerNo>\n" +
                      "\t\t\t\t<asbo:AccountingType>702010300</asbo:AccountingType>\n" +
                      "\t\t\t\t<asbo:AccountingType>758030400</asbo:AccountingType>\n" +
                      "            </asbo:AccountQuery>\n" +
                      "\t\t\t<asbo:AccountQuery>\n" +
                      "\t\t\t<asbo:AccountNumber>42309810800504735550</asbo:AccountNumber>\t\t\t\n" +
                      "\t\t\t</asbo:AccountQuery>\n" +
                      "\t\t</asbo:AccountListQuery>\n";
}

