package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import com.ibm.jms.JMSBytesMessage;
import com.ibm.jms.JMSMessage;
import com.ibm.jms.JMSTextMessage;
import com.ibm.mq.jms.*;
import com.ibm.msg.client.wmq.WMQConstants;
import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.controller.operday.task.KeyValueStorage;
import ru.rbt.barsgl.ejb.entity.acc.AclirqJournal;
import ru.rbt.barsgl.ejb.repository.AclirqJournalRepository;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.AccountQueryRepository;
import ru.rbt.barsgl.ejbcore.AsyncProcessor;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejbcore.JpaAccessCallback;
import ru.rbt.ejb.repository.properties.PropertiesRepository;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.persistence.EntityManager;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static ru.rbt.audit.entity.AuditRecord.LogCode.AccountQuery;
import static ru.rbt.barsgl.ejb.props.PropertyName.PD_CONCURENCY;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;

/**
 * Created by ER22228 on 04.08.2016.
 */
@SuppressWarnings("ALL")
@Stateless
@LocalBean
public class CommonQueueProcessor implements javax.jms.MessageListener {
    private static final Logger log = Logger.getLogger(CommonQueueProcessor.class);
    private static final String SCHEDULED_TASK_NAME = "AccountQuery";

    //    private String queueNames;
    private QueueProperties queueProperties;
    private Map<String, String> currencyMap;
    private Map<String, Integer> currencyNBDPMap;

    @EJB
    private AsyncProcessor asyncProcessor;

    @EJB
    private AuditController auditController;

    @EJB
    private CoreRepository coreRepository;

    @Inject
    private AccountQueryProcessor queryProcessor;

    @Inject
    private AccountQueryBAProcessor queryProcessorBA;

    @Inject
    private MasterAccountProcessor queryProcessorMAPB;

    @EJB
    private AccountQueryRepository queryRepository;

    @EJB
    private AclirqJournalRepository journalRepository;

    @EJB
    private PropertiesRepository propertiesRepository;

    @EJB
    private KeyValueStorage keyValueStorage;

    private BlockingQueue<Message> blockingQueue = new LinkedBlockingDeque<>();

    @Override
    public void onMessage(Message msg) {
        blockingQueue.add(msg);
    }

    public void process(String queueNames, QueueProperties queueProperties, Map<String, String> currencyMap, Map<String, Integer> currencyNBDPMap) throws Exception {
//        this.queueNames = queueNames;
        this.queueProperties = queueProperties;
        this.currencyMap = currencyMap;
        this.currencyNBDPMap = currencyNBDPMap;

        MQQueueConnection connection = null;
        MQQueueSession session = null;
        try {
            MQQueueConnectionFactory cf = new MQQueueConnectionFactory();

            cf.setHostName(queueProperties.mqHost);
            cf.setPort(queueProperties.mqPort);
            cf.setTransportType(WMQConstants.WMQ_CM_CLIENT);
            cf.setQueueManager(queueProperties.mqQueueManager);
            cf.setChannel(queueProperties.mqChannel);

            connection = (MQQueueConnection) cf.createQueueConnection(queueProperties.mqUser, queueProperties.mqPassword);
            session = (MQQueueSession) connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

            processOneSource(queueProperties.mqBatchSize, session, connection, queueNames);

            session.close();
            connection.close();
            log.info("Сессия обработки одной очереди завершена");
        }catch (Exception e){
            log.error("Process error",e);
            throw new Exception(e);
        } finally {
            if (session != null) {
                session.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }

    private String[] readFromJMS(MQMessageConsumer receiver) throws JMSException {
        JMSMessage receivedMessage = (JMSMessage) receiver.receiveNoWait();
        String textMessage = null;
        if (receivedMessage instanceof JMSTextMessage) {
            textMessage = ((JMSTextMessage) receivedMessage).getText();
        } else if (receivedMessage instanceof JMSBytesMessage) {
            JMSBytesMessage bytesMessage = (JMSBytesMessage) receivedMessage;

            int length = (int) bytesMessage.getBodyLength();
            byte[] incomingBytes = new byte[length];
            bytesMessage.readBytes(incomingBytes);

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(incomingBytes);
            try (Reader r = new InputStreamReader(byteArrayInputStream, "UTF-8")) {
                StringBuilder sb = new StringBuilder();
                char cb[] = new char[1024];
                int s = r.read(cb);
                while (s > -1) {
                    sb.append(cb, 0, s);
                    s = r.read(cb);
                }
                textMessage = sb.toString();
            } catch (IOException e) {
                log.error("Error during read message from QUEUE", e);
            }
        }
        if (textMessage == null) {
            return null;
        }
        return new String[]{textMessage, receivedMessage.getJMSMessageID(),
            receivedMessage.getJMSReplyTo() == null ? null : receivedMessage.getJMSReplyTo().toString()};
    }

    private String[] parseMessage(Message receivedMessage) throws JMSException {
//        JMSMessage receivedMessage = (JMSMessage) receiver.receiveNoWait();
        String textMessage = null;
        if (receivedMessage instanceof JMSTextMessage) {
            textMessage = ((JMSTextMessage) receivedMessage).getText();
        } else if (receivedMessage instanceof JMSBytesMessage) {
            JMSBytesMessage bytesMessage = (JMSBytesMessage) receivedMessage;

            int length = (int) bytesMessage.getBodyLength();
            byte[] incomingBytes = new byte[length];
            bytesMessage.readBytes(incomingBytes);

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(incomingBytes);
            try (Reader r = new InputStreamReader(byteArrayInputStream, "UTF-8")) {
                StringBuilder sb = new StringBuilder();
                char cb[] = new char[1024];
                int s = r.read(cb);
                while (s > -1) {
                    sb.append(cb, 0, s);
                    s = r.read(cb);
                }
                textMessage = sb.toString();
            } catch (IOException e) {
                log.error("Error during read message from QUEUE", e);
            }
        }
        if (textMessage == null) {
            return null;
        }
        return new String[]{textMessage, receivedMessage.getJMSMessageID(),
            receivedMessage.getJMSReplyTo() == null ? null : receivedMessage.getJMSReplyTo().toString()};
    }


    private void processOneSource(int batchSize, MQQueueSession session, MQQueueConnection connection, String queueNames) throws Exception {
        String[] params = queueNames.split(":");
        MQQueue queueIn = (MQQueue) session.createQueue("queue:///" + params[1]);
        MQQueueReceiver receiver = (MQQueueReceiver) session.createReceiver(queueIn);

        receiver.setMessageListener(this);
        connection.start();

        while (keyValueStorage.getTaskContinue(KeyValueStorage.TASKS.AccountListTask)) {
            if (blockingQueue.size() == 0) {
                Thread.sleep(30);
                continue;
            }

            List<JpaAccessCallback<Void>> callbacks = new ArrayList<>();

            int len = blockingQueue.size();
            len = len>batchSize?batchSize:len;
            for (int i = 0; i < len; i++) {
                String[] incMessage = parseMessage(blockingQueue.take());
                if (incMessage == null || incMessage[0] == null) {
                    break;
                }

                String textMessage = incMessage[0].trim();
//            if(textMessage.contains("Error")) continue;
                Long jId = 0L;
                try {
                    jId = (Long) coreRepository.executeInNewTransaction(persistence -> {
                        return journalRepository.createJournalEntry(params[0], textMessage);
                    });
                    callbacks.add(new CommonRqCallback(params[0], textMessage, currencyMap, currencyNBDPMap, jId, incMessage, params[2], queueProperties));
                } catch (JMSException e) {
                    auditController.error(AccountQuery, "Ошибка при обработке сообщения из " + params[1] + " / Таблица DWH.GL_ACLIRQ / id=" + jId, null, e);
                }
            }

            asyncProcessor.asyncProcessPooled(callbacks, propertiesRepository.getNumber(PD_CONCURENCY.getName()).intValue(), 10, TimeUnit.MINUTES);
        }
        receiver.close();
    }

    private class CommonRqCallback implements JpaAccessCallback<Void> {

        String textMessage;
        Map<String, String> currencyMap;
        Map<String, Integer> currencyNBDPMap;
        Long jId;
        String[] incMessage;
        String queue;
        String queueType;

        CommonRqCallback(String queueType, String textMessage, Map<String, String> currencyMap, Map<String, Integer> currencyNBDPMap, Long jId, String[] incMessage, String queue, QueueProperties queueProperties) {
            this.textMessage = textMessage;
            this.currencyMap = currencyMap;
            this.currencyNBDPMap = currencyNBDPMap;
            this.jId = jId;
            this.incMessage = incMessage;
            this.queue = queue;
            this.queueType = queueType;
        }

        @Override
        public Void call(EntityManager persistence) throws Exception {
            String outMessage = (String) coreRepository.executeInNewTransaction(persistence1 -> {
                try {
                    switch (queueType) {
                        case "LIRQ":
                            return queryProcessor.process(textMessage, currencyMap, currencyNBDPMap, jId,false);
                        case "BALIRQ":
                            return queryProcessorBA.process(textMessage, currencyMap, currencyNBDPMap, jId);
                        case "MAPBRQ":
                            return queryProcessorMAPB.process(textMessage, currencyMap, currencyNBDPMap, jId);
                    }
                } catch (Exception e) {
                    log.error("Ошибка при подготовке ответа. ", e);
                    AclirqJournal aclirqJournal = journalRepository.findById(AclirqJournal.class, jId);
                    return getErrorMessage(aclirqJournal.getComment());
                }
                return "";
            });

            if (!isEmpty(outMessage)) {
                try {
                    sendToQueue(outMessage, queueProperties, incMessage, queue);
                    journalRepository.updateLogStatus(jId, AclirqJournal.Status.PROCESSED, "");
                } catch (Exception e) {
                    log.error("Ошибка отправки ответа. ", e);
                    journalRepository.updateLogStatus(jId, AclirqJournal.Status.ERROR, "Ошибка отправки ответа. " + e.getMessage());
                }
            }
            return null;
        }

        public void sendToQueue(String outMessage, QueueProperties queueProperties, String[] incMessage, String queue) throws JMSException {
            MQQueueConnection connection = null;
            MQQueueSession session = null;
            try {
                MQQueueConnectionFactory cf = new MQQueueConnectionFactory();

                cf.setHostName(queueProperties.mqHost);
                cf.setPort(queueProperties.mqPort);
                cf.setTransportType(WMQConstants.WMQ_CM_CLIENT);
                cf.setQueueManager(queueProperties.mqQueueManager);
                cf.setChannel(queueProperties.mqChannel);

                connection = (MQQueueConnection) cf.createQueueConnection(queueProperties.mqUser, queueProperties.mqPassword);
                session = (MQQueueSession) connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

                JMSTextMessage message = (JMSTextMessage) session.createTextMessage(outMessage);
                message.setJMSCorrelationID(incMessage[1]);
                MQQueue queueOut = (MQQueue) session.createQueue(!isEmpty(incMessage[2]) ? incMessage[2] : "queue:///" + queue);
                MQQueueSender sender = (MQQueueSender) session.createSender(queueOut);
                sender.send(message);
                sender.close();

                session.close();
                connection.close();
//                log.info("Отправка сообщения завершена");
            } finally {
                if (session != null) {
                    try {
                        session.close();
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public String getErrorMessage(String message) throws DatatypeConfigurationException {
            String answerBody;
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime(new Date());
            DatatypeFactory df = DatatypeFactory.newInstance();
            XMLGregorianCalendar dateTime = df.newXMLGregorianCalendar(calendar);

            answerBody =
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "\t\t<asbo:Error xmlns:asbo=\"urn:asbo:barsgl\">\n" +
                    "\t\t\t<asbo:Code>0100</asbo:Code>\n" +
                    "\t\t\t<asbo:Description>" + message + "</asbo:Description>\n" +
                    "\t\t\t<asbo:Source>BarsGL</asbo:Source>\n" +
                    "\t\t\t<asbo:Kind>SYSERR</asbo:Kind>\n" +
                    "\t\t\t<asbo:DateTime>" + dateTime.toString() + "</asbo:DateTime>\n" +
                    "\t\t</asbo:Error>\n";
            return answerBody;
        }
    }
}
