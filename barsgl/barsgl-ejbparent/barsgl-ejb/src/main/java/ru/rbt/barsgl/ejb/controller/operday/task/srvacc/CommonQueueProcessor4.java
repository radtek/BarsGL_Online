package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import org.apache.log4j.Logger;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.entity.acc.AclirqJournal;
import ru.rbt.barsgl.ejb.repository.AclirqJournalRepository;
import ru.rbt.barsgl.ejbcore.AccountQueryRepository;
import ru.rbt.barsgl.ejbcore.AsyncProcessor;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.JpaAccessCallback;

import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.jms.*;
import javax.persistence.EntityManager;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import javax.ejb.EJBContext;

import static ru.rbt.audit.entity.AuditRecord.LogCode.AccountQuery;
import static ru.rbt.barsgl.ejb.props.PropertyName.PD_CONCURENCY;
import ru.rbt.ejbcore.DefaultApplicationException;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;

/**
 * Created by ER22228 on 04.08.2016.
 */
@SuppressWarnings("ALL")
@Stateless
@LocalBean
public class CommonQueueProcessor4 implements MessageListener {
    private static final Logger log = Logger.getLogger(CommonQueueProcessor4.class);
    //private static final String SCHEDULED_TASK_NAME = "AccountQuery";

    @Resource
    private EJBContext context;

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

    private static final Map<String, String> CURRENCY_MAP = new HashMap<>();
    private static final Map<String, Integer> CURRENCY_NBDP_MAP = new HashMap<>();

    private QueueProperties queueProperties;

    private JMSContext jmsContext = null;
    private final int defaultBatchSize = 50;

    public void startConnection() throws JMSException {
        if (jmsContext == null) {
            MQQueueConnectionFactory cf = new MQQueueConnectionFactory();
            cf.setHostName(queueProperties.mqHost);
            cf.setPort(queueProperties.mqPort);
            cf.setTransportType(WMQConstants.WMQ_CM_CLIENT);
            cf.setQueueManager(queueProperties.mqQueueManager);
            cf.setChannel(queueProperties.mqChannel);
            setJmsContext(cf.createContext(queueProperties.mqUser, queueProperties.mqPassword, JMSContext.CLIENT_ACKNOWLEDGE));
            jmsContext.setExceptionListener((JMSException e) -> {
                log.info("\n\nonException calling");
                reConnect();
            });
        }
    }

    private void reConnect() {
        log.info("\n\nreConnect calling");
        closeConnection();
        // На следующем старте задачи сработает startConnection()
    }

    @PreDestroy
    public void closeConnection() {
        log.info("\n\ncloseConnection calling");
        try {
            if (jmsContext != null) {
                jmsContext.close();
            }
        } catch (Exception e) {
            auditController.warning(AccountQuery, "Ошибка при закрытии соединения", null, e);
        }finally{
            jmsContext = null;
        }
    }

    private void loadCurrency() throws Exception {
        if (CURRENCY_MAP.isEmpty() || CURRENCY_NBDP_MAP.isEmpty()) {
            queryRepository.loadCurrency(CURRENCY_MAP, CURRENCY_NBDP_MAP);
        }
    }

    public void process(Properties properties) throws Exception {
        try {
            setQueueProperties(properties);
            try{
                startConnection();
                loadCurrency();
                processSources();
            }catch(JMSRuntimeException | JMSException ex){
                // reset session
                reConnect();
                auditController.warning(AccountQuery, "Ошибка при обработке сообщений", null, ex);
                throw ex;
            }
        } catch (Exception e) {
            log.error("Ошибка в методе process", e);
            throw e;
        }
    }

    public void setQueueProperties(Properties properties) throws Exception {
      this.queueProperties = new QueueProperties(properties);
    }

    public void setJmsContext(JMSContext jmsContext) {
        this.jmsContext = jmsContext;
    }
        
    private String[] readFromJMS(JMSConsumer consumer) throws JMSException {
        Message receivedMessage = consumer.receiveNoWait();
        if (receivedMessage == null) {
            return null;
        }
        return readJMS(receivedMessage);
    }
    
    private String[] readJMS(Message receivedMessage) throws JMSException {
        if(jmsContext != null && jmsContext.getSessionMode() == JMSContext.CLIENT_ACKNOWLEDGE)
            receivedMessage.acknowledge();
        String textMessage = null;
        if (receivedMessage instanceof TextMessage) {
            textMessage = ((TextMessage) receivedMessage).getText();
        } else if (receivedMessage instanceof BytesMessage) {
            BytesMessage bytesMessage = (BytesMessage) receivedMessage;

            int length = (int) bytesMessage.getBodyLength();
            byte[] incomingBytes = new byte[length];
            bytesMessage.readBytes(incomingBytes);

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(incomingBytes);
            try (Reader r = new InputStreamReader(byteArrayInputStream, StandardCharsets.UTF_8)) {
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
        try (JMSConsumer consumer = jmsContext.createConsumer(jmsContext.createQueue("queue:///" + params[1]));) {
            int cuncurencySize = propertiesRepository.getNumber(PD_CONCURENCY.getName()).intValue();
            long timeout = 10L;
            TimeUnit unit = TimeUnit.MINUTES;
            ExecutorService executor = null;
            try {
                for (int i = 0; i < queueProperties.mqBatchSize; i++) {
                    long startReceiveTime = System.currentTimeMillis();
                    Message receivedMessage = consumer.receiveNoWait();
                    long endReceiveTime = System.currentTimeMillis();
                    if (receivedMessage != null) {
                        if (executor == null) {
                            executor = asyncProcessor.getBlockingQueueThreadPoolExecutor(cuncurencySize, cuncurencySize, queueProperties.mqBatchSize);
                        }
                        
                        executor.submit(() -> processMessage(receivedMessage, params[0], params[1], params[2], endReceiveTime - startReceiveTime, endReceiveTime));
                    } else
                        break;
                }
            } finally {
                if(executor != null)
                    awaitTermination(executor, timeout, unit);
            }
        }
    }

    private void processSourcesOld() throws Exception {
        String[] params = queueProperties.mqTopics.split(":");
        
        try(JMSConsumer consumer =  jmsContext.createConsumer(jmsContext.createQueue("queue:///" + params[1]));){
        
            List<JpaAccessCallback<Void>> callbacks = new ArrayList<>();

            for (int i = 0; i < queueProperties.mqBatchSize; i++) {
                long createReceiveTime = System.currentTimeMillis();

                String[] incMessage = readFromJMS(consumer);
                if (incMessage == null || incMessage[0] == null) {
                    break;
                }
                long receiveTime = System.currentTimeMillis() - createReceiveTime;

                String textMessage = incMessage[0].trim();
                Long jId = 0L;
                try {
                    jId = journalRepository.executeInNewTransaction(persistence -> {
                        return journalRepository.createJournalEntry(params[0], textMessage);
                    });
                    callbacks.add(new CommonRqCallback(params[0], textMessage, jId, incMessage, params[2], receiveTime));
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
        }
    }
    
    @Override
    public void onMessage(Message message) {
      Long jId = 0L;
      String[] params = queueProperties.mqTopics.split(":");
      try {
        String[] incMessage = readJMS(message);
        String textMessage = incMessage[0].trim();

        jId = journalRepository.executeInNewTransaction(persistence -> {
            return journalRepository.createJournalEntry(params[0], textMessage);
        });

        asyncProcessor.submitToDefaultExecutor(new CommonRqCallback(params[0], textMessage, jId, incMessage, params[2], -1L),
                propertiesRepository.getNumber(PD_CONCURENCY.getName()).intValue());
        
      } catch (JMSException e) {
        //reConnect();
        auditController.warning(AccountQuery, "Ошибка при обработке сообщения из " + params[1] + " / Таблица GL_ACLIRQ / id=" + jId, null, e);
      } catch (Exception ex) {
        log.error("Ошибка при обработке сообщения", ex);
      }
    }
    
    private void processMessage(Message receivedMessage, String queueType, String fromQueue, String queue, long receiveTime, long startThreadTime){
        long waitingTime = System.currentTimeMillis() - startThreadTime;
        Long jId = null;
        try {            
            String[] incMessage = readJMS(receivedMessage);
            
            if (incMessage == null || incMessage[0] == null) {
                return;
            }
            
            String textMessage = incMessage[0].trim();
            
            jId = journalRepository.executeInNewTransaction((persistence) -> {
                return journalRepository.createJournalEntry(queueType, textMessage);
            });

            processing(queueType, textMessage, jId, incMessage, queue, receiveTime, waitingTime);

        } catch (JMSException e) {
            reConnect();
            auditController.warning(AccountQuery, "Ошибка при обработке сообщения из " + fromQueue + " / Таблица GL_ACLIRQ / id="+jId, null, e);
        } catch (Exception e) {
            log.error("Ошибка при обработке сообщения из " + fromQueue, e);
            auditController.warning(AccountQuery, "Ошибка при обработке сообщения / Таблица GL_ACLIRQ / id=" + jId, null, e);
            if(jId != null)
                journalRepository.updateLogStatus(jId, AclirqJournal.Status.ERROR, "Ошибка при обработке сообщения. " + e.getMessage());
            context.setRollbackOnly();
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }
    
    private void processing(String queueType, String textMessage, Long jId, String[] incMessage, String queue, long receiveTime, long waitingTime) throws Exception {
        long startProcessing = System.currentTimeMillis();
        String outMessage = (String) coreRepository.executeInNewTransaction(persistence1 -> {
            try {
                switch (queueType) {
                    case "LIRQ":
                        return queryProcessor.process(textMessage, CURRENCY_MAP, CURRENCY_NBDP_MAP, jId, "show".equals(queueProperties.unspents));
                    case "BALIRQ":
                        return queryProcessorBA.process(textMessage, CURRENCY_MAP, CURRENCY_NBDP_MAP, jId);
                    case "MAPBRQ":
                        return queryProcessorMAPB.process(textMessage, CURRENCY_MAP, CURRENCY_NBDP_MAP, jId);
                }
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

        if (!isEmpty(outMessage)) {
            try {
                long createAnswerTime = System.currentTimeMillis();
                sendToQueue(outMessage, queueProperties, incMessage, queue);
                long sendingAnswerTime = System.currentTimeMillis();
                journalRepository.invokeAsynchronous(em -> {
                    return journalRepository.updateLogStatus(jId, AclirqJournal.Status.PROCESSED,
                            receiveTime
                            + "/"
                            + (createAnswerTime - startProcessing)
                            + "/"
                            + (sendingAnswerTime - createAnswerTime)
                            + "/"
                            + ("true".equals(queueProperties.writeSleepThreadTime) ? waitingTime : "")
                            + "/",
                             "true".equals(queueProperties.writeOut) ? outMessage : null);
                });
            } catch (JMSRuntimeException | JMSException e) {
                auditController.error(AccountQuery, "Ошибка при отправке сообщения / Таблица GL_ACLIRQ / id=" + jId, null, e);
                journalRepository.invokeAsynchronous(em -> {
                    return journalRepository.updateLogStatus(jId, AclirqJournal.Status.ERROR, "Ошибка отправки ответа. " + e.getMessage());
                });
            }
        }
    }
    
    public void sendToQueue(String outMessage, QueueProperties queueProperties, String[] incMessage, String queue) throws JMSException {
        TextMessage message = jmsContext.createTextMessage(outMessage);
        message.setJMSCorrelationID(incMessage[1]);
        JMSProducer producer = jmsContext.createProducer();
        producer.send(jmsContext.createQueue(!isEmpty(incMessage[2]) ? incMessage[2] : "queue:///" + queue), message);
    }

    private void awaitTermination(ExecutorService executor, long timeout, TimeUnit unit) throws Exception {
        final long tillTo = System.currentTimeMillis() + unit.toMillis(timeout);
        asyncProcessor.awaitTermination(executor, timeout, unit, tillTo);
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
    
    private class CommonRqCallback implements JpaAccessCallback<Void> {
        String textMessage;
        Long jId;
        String[] incMessage;
        String queue;
        String queueType;
        long receiveTime;

        CommonRqCallback(String queueType, String textMessage, Long jId, String[] incMessage, String queue, long receiveTime) {
            this.textMessage = textMessage;
            this.jId = jId;
            this.incMessage = incMessage;
            this.queue = queue;
            this.queueType = queueType;
            this.receiveTime = receiveTime;
        }

        @Override
        public Void call(EntityManager persistence) throws Exception {
            processing(queueType, textMessage, jId, incMessage, queue, receiveTime, -1L);
            return null;
        }
    }
}
