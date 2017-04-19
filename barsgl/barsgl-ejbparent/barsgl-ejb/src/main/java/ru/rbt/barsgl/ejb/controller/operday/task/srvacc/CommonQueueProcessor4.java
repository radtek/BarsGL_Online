package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import javax.jms.BytesMessage;
import javax.jms.TextMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.entity.acc.AclirqJournal;
import ru.rbt.barsgl.ejb.repository.AclirqJournalRepository;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.AccountQueryRepository;
import ru.rbt.barsgl.ejbcore.AsyncProcessor;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejbcore.JpaAccessCallback;
import ru.rbt.barsgl.ejbcore.repository.PropertiesRepository;

import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
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
public class CommonQueueProcessor4 implements MessageListener {
    private static final Logger log = Logger.getLogger(CommonQueueProcessor4.class);
    private static final String SCHEDULED_TASK_NAME = "AccountQuery";

    @EJB
    private AsyncProcessor asyncProcessor;

    @EJB
    private AuditController auditController;

    @EJB
    private CoreRepository coreRepository;

    @EJB
    private AccountQueryProcessor queryProcessor;

    @EJB
    private AccountQueryBAProcessor queryProcessorBA;

    @EJB
    private MasterAccountProcessor queryProcessorMAPB;

    @EJB
    private AccountQueryRepository queryRepository;

    @EJB
    private AclirqJournalRepository journalRepository;

    @EJB
    private PropertiesRepository propertiesRepository;

    private static final Map<String, String> currencyMap = new HashMap<>();
    private static final Map<String, Integer> currencyNBDPMap = new HashMap<>();

    private QueueProperties queueProperties;

    QueueConnection connection = null;
    QueueSession session = null;

    private int defaultBatchSize = 50;
    private int batchSize;

    public void startConnection() throws JMSException {
        if (session == null) {
            MQQueueConnectionFactory cf = new MQQueueConnectionFactory();
            cf.setHostName(queueProperties.mqHost);
            cf.setPort(queueProperties.mqPort);
            cf.setTransportType(WMQConstants.WMQ_CM_CLIENT);
            cf.setQueueManager(queueProperties.mqQueueManager);
            cf.setChannel(queueProperties.mqChannel);
            connection = cf.createQueueConnection(queueProperties.mqUser, queueProperties.mqPassword);
            setSession(connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE));

            connection.setExceptionListener(new ExceptionListener() {
                @Override
                public void onException(JMSException e) {
                    log.info("\n\nonException calling");
                    reConnect();
                }
            });
        }
    }

    private void reConnect() {
        log.info("\n\nreConnect calling");
        closeConnection();
        connection = null;
        session = null;
        // На следующем старте задачи сработает startConnection()
    }

    @PreDestroy
    public void closeConnection() {
        log.info("\n\ncloseConnection calling");
        try {
            if (session != null) {
                session.close();
            }
        } catch (JMSException e1) {
            auditController.warning(AccountQuery, "Ошибка при закрытии сессии", null, e1);
        }

        try {
            if (connection != null) {
                connection.close();
            }

        } catch (JMSException e1) {
            auditController.warning(AccountQuery, "Ошибка при закрытии соединения", null, e1);
        }
    }

    private void loadCurrency() throws Exception {
        if (currencyMap == null || currencyNBDPMap == null || currencyMap.size() == 0 || currencyNBDPMap.size() == 0) {
            queryRepository.loadCurrency(currencyMap, currencyNBDPMap);
        }
    }

    public void process(Properties properties) throws Exception {
        try {
            setQueueProperties(properties);
            startConnection();
            batchSize = queueProperties.mqBatchSize;
            loadCurrency();
            processSources();
//        log.info("Сессия обработки одной очереди завершена");
        } catch (Exception e) {
            log.error("Ошибка в методе process", e);
            throw e;
        }
    }

    public void setQueueProperties(Properties properties) throws Exception {
      this.queueProperties = new QueueProperties(properties);
    }

    public void setSession(QueueSession session) {
      this.session = session;
    }
    
    private String[] readFromJMS(MessageConsumer receiver) throws JMSException {
        Message receivedMessage = receiver.receiveNoWait();
        if (receivedMessage == null) {
            return null;
        }
        return readJMS(receivedMessage);
    }
    
    private String[] readJMS(Message receivedMessage) throws JMSException {
//        log.info(receivedMessage.toString());

        String textMessage = null;
        if (receivedMessage instanceof TextMessage) {
            textMessage = ((TextMessage) receivedMessage).getText();
        } else if (receivedMessage instanceof BytesMessage) {
            BytesMessage bytesMessage = (BytesMessage) receivedMessage;

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

    private void processSources() throws Exception {
        String[] params = queueProperties.mqTopics.split(":");
        Queue queueIn = session.createQueue("queue:///" + params[1]);
        QueueReceiver receiver = session.createReceiver(queueIn);

        connection.start();

        List<JpaAccessCallback<Void>> callbacks = new ArrayList<>();

        for (int i = 0; i < batchSize; i++) {
            String[] incMessage = readFromJMS(receiver);
            if (incMessage == null || incMessage[0] == null) {
                break;
            }

            String textMessage = incMessage[0].trim();
            Long jId = 0L;
            try {
                jId = (Long) coreRepository.executeInNewTransaction(persistence -> {
                    return journalRepository.createJournalEntry(params[0], textMessage);
                });
                callbacks.add(new CommonRqCallback(params[0], textMessage, jId, incMessage, params[2]));
            } catch (JMSException e) {
                reConnect();
                auditController.warning(AccountQuery, "Ошибка при обработке сообщения из " + params[1] + " / Таблица GL_ACLIRQ / id=" + jId, null, e);
            }
        }

        if (callbacks.size() > 0) {
//            log.info("Из очереди " + params[1] + " принято на обработку " + callbacks.size() + " запросов");
            //asyncProcessor.asyncProcessPooled(callbacks, propertiesRepository.getNumber(PD_CONCURENCY.getName()).intValue(), 10, TimeUnit.MINUTES);
            asyncProcessor.asyncProcessPooledByExecutor(callbacks, propertiesRepository.getNumber(PD_CONCURENCY.getName()).intValue(), 10, TimeUnit.MINUTES);
//            log.info("Из очереди " + params[1] + " принято на обработку " + callbacks.size() + " запросов. Обработка завершена");
        }

        receiver.close();
    }

    @Override
    public void onMessage(Message message) {
      Long jId = 0L;
      String[] params = queueProperties.mqTopics.split(":");
      try {
        String[] incMessage = readJMS(message);
        String textMessage = incMessage[0].trim();

        jId = (Long) coreRepository.executeInNewTransaction(persistence -> {
          return journalRepository.createJournalEntry(params[0], textMessage);
        });

        asyncProcessor.submitToDefaultExecutor(new CommonRqCallback(params[0], textMessage, jId, incMessage, params[2]),
                propertiesRepository.getNumber(PD_CONCURENCY.getName()).intValue());
        
      } catch (JMSException e) {
        //reConnect();
        auditController.warning(AccountQuery, "Ошибка при обработке сообщения из " + params[1] + " / Таблица GL_ACLIRQ / id=" + jId, null, e);
      } catch (Exception ex) {
        log.error("Ошибка при обработке сообщения", ex);
      }
    }

    private class CommonRqCallback implements JpaAccessCallback<Void> {

        String textMessage;
        Long jId;
        String[] incMessage;
        String queue;
        String queueType;

        CommonRqCallback(String queueType, String textMessage, Long jId, String[] incMessage, String queue) {
            this.textMessage = textMessage;
            this.jId = jId;
            this.incMessage = incMessage;
            this.queue = queue;
            this.queueType = queueType;
        }

        @Override
        public Void call(EntityManager persistence) throws Exception {
            long startProcessing = System.currentTimeMillis();
            String outMessage = (String) coreRepository.executeInNewTransaction(persistence1 -> {
                try {
                    switch (queueType) {
                        case "LIRQ":
                            return queryProcessor.process(textMessage, currencyMap, currencyNBDPMap, jId, "show".equals(queueProperties.unspents));
                        case "BALIRQ":
                            return queryProcessorBA.process(textMessage, currencyMap, currencyNBDPMap, jId);
                        case "MAPBRQ":
                            return queryProcessorMAPB.process(textMessage, currencyMap, currencyNBDPMap, jId);
                    }
                } catch (JMSException e) {
                    reConnect();
                    auditController.warning(AccountQuery, "Ошибка при отправке сообщения / Таблица GL_ACLIRQ / id=" + jId, null, e);
                } catch (Exception e) {
                    log.error("Ошибка при подготовке ответа. ", e);
                    auditController.warning(AccountQuery, "Ошибка при подготовке ответа / Таблица GL_ACLIRQ / id=" + jId, null, e);
                    return journalRepository.executeInNewTransaction(persistence2 -> {
                        AclirqJournal aclirqJournal = journalRepository.findById(AclirqJournal.class, jId);
                        return getErrorMessage(aclirqJournal.getComment());
                    });
                }
                return "";
            });

            long createAnswerTime = System.currentTimeMillis();
            if (!isEmpty(outMessage)) {
                try {
                    sendToQueue(outMessage, queueProperties, incMessage, queue);
                    long sendingAnswerTime = System.currentTimeMillis();
                    //journalRepository.updateLogStatus(jId, AclirqJournal.Status.PROCESSED, "" + (createAnswerTime - startProcessing) + "/" + (sendingAnswerTime - createAnswerTime));                    
                    journalRepository.invokeAsynchronous(em -> {
                      return journalRepository.updateLogStatus(jId, AclirqJournal.Status.PROCESSED, "" 
                              + (createAnswerTime - startProcessing) 
                              + "/" 
                              + (sendingAnswerTime - createAnswerTime) 
                              + "/",
                              "true".equals(queueProperties.writeOut) ? outMessage : null);
                    });                    
                } catch (Exception e) {
                    log.error("Ошибка отправки ответа. ", e);
                    journalRepository.invokeAsynchronous(em -> {
                      return journalRepository.updateLogStatus(jId, AclirqJournal.Status.ERROR, "Ошибка отправки ответа. " + e.getMessage());
                    });
                }
            }
            return null;
        }

        public void sendToQueue(String outMessage, QueueProperties queueProperties, String[] incMessage, String queue) throws JMSException {
            TextMessage message = session.createTextMessage(outMessage);
            message.setJMSCorrelationID(incMessage[1]);
            Queue queueOut = session.createQueue(!isEmpty(incMessage[2]) ? incMessage[2] : "queue:///" + queue);
//            QueueSender sender = session.createSender(queueOut);
            MessageProducer sender = session.createProducer(queueOut);            
            sender.send(message);
            sender.close();
//            log.info("Отправка сообщения завершена");
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
