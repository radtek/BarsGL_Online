package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import org.apache.log4j.Logger;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.AsyncProcessor;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.jms.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static ru.rbt.audit.entity.AuditRecord.LogCode.QueueProcessor;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;

/**
 * Created by er18837 on 12.12.2017.
 */
public abstract class CommonQueueController implements MessageListener {
    private static final Logger log = Logger.getLogger(CommonQueueController.class);
    private static final int defaultQueueBachSize = 10;

    @Resource
    protected EJBContext context;

    @EJB
    protected CoreRepository coreRepository;

    @EJB
    protected AsyncProcessor asyncProcessor;

    @EJB
    protected AuditController auditController;

    @EJB
    protected PropertiesRepository propertiesRepository;

    protected JMSContext jmsContext = null;
    protected QueueProperties queueProperties;

    protected abstract void afterConnect() throws Exception;
    protected abstract String processQuery(String queueType, String textMessage, Long jId) throws Exception;

    protected abstract String getJournalName();
    protected abstract Long createJournalEntry(String queueType, String textMessage) throws Exception;
    protected abstract void updateStatusSuccess(Long journalId, String comment, String outMessage) throws Exception;
    protected abstract void updateStatusErrorProc(Long journalId, Throwable e) throws Exception;
    protected abstract void updateStatusErrorOut(Long journalId, Throwable e) throws Exception;

    protected int getConcurencySize() { return 10; }
    protected long getTimeout() { return 10L; }
    protected TimeUnit getTimeoutUnit() { return TimeUnit.MINUTES; };

    public void setQueueProperties(Properties properties) throws Exception {
        this.queueProperties = new QueueProperties(properties);
    }

    public QueueProperties getQueueProperties() {
        return queueProperties;
    }

    public void setJmsContext(JMSContext jmsContext) {
        this.jmsContext = jmsContext;
    }

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

    protected void reConnect() {
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
            auditController.warning(QueueProcessor, "Ошибка при закрытии соединения", null, e);
        }finally{
            jmsContext = null;
        }
    }

    public void process(Properties properties) throws Exception {
        try {
            setQueueProperties(properties);
            try{
                startConnection();
                afterConnect();
                processSources();
            }catch(JMSRuntimeException | JMSException ex){
                // reset session
                reConnect();
                auditController.warning(QueueProcessor, "Ошибка при обработке сообщений", null, ex);
                throw ex;
            }
        } catch (Exception e) {
            log.error("Ошибка в методе process", e);
            throw e;
        }
    }

    private void processSources() throws Exception {
        String[] params = queueProperties.mqTopics.split(":");
        if (params.length < 2 || isEmpty(params[1])) {
            auditController.warning(QueueProcessor, "Неверный формат topic: " + queueProperties.mqTopics);
            return;
        }
        String queueType = params[0];
        String inQueue = params[1];
        String outQueue = params.length > 2 ? params[2] : null;

        try (JMSConsumer consumer = jmsContext.createConsumer(jmsContext.createQueue("queue:///" + inQueue));) {
            int cuncurencySize = getConcurencySize();
            long timeout = getTimeout(); // 10
            TimeUnit unit = getTimeoutUnit();   // TimeUnit.MINUTES;
            ExecutorService executor = null;
            int batchSize = queueProperties.mqBatchSize;
            batchSize = batchSize == -1 ? defaultQueueBachSize : batchSize;
            try {
                for (int i = 0; i < batchSize; i++) {
                    long startReceiveTime = System.currentTimeMillis();
                    Message receivedMessage = consumer.receiveNoWait();
                    long endReceiveTime = System.currentTimeMillis();
                    if (receivedMessage != null) {
                        if (executor == null) {
                            executor = asyncProcessor.getBlockingQueueThreadPoolExecutor(cuncurencySize, cuncurencySize, batchSize);
                        }

                        executor.submit(() -> processMessage(receivedMessage, queueType, inQueue, outQueue , endReceiveTime - startReceiveTime, endReceiveTime));
                    } else
                        break;
                }
            } finally {
                if(executor != null)
                    awaitTermination(executor, timeout, unit);
            }
        }
    }

    private void awaitTermination(ExecutorService executor, long timeout, TimeUnit unit) throws Exception {
        final long tillTo = System.currentTimeMillis() + unit.toMillis(timeout);
        try {
            asyncProcessor.awaitTermination(executor, timeout, unit, tillTo);
        } catch (TimeoutException e) {
            throw new ValidationError(ErrorCode.QUEUE_ERROR, e.getMessage());
        }
    }

    private void processMessage(Message receivedMessage, String queueType, String fromQueue, String toQueue, long receiveTime, long startThreadTime){
        long waitingTime = System.currentTimeMillis() - startThreadTime;
        Long journalId = null;
        try {
            String[] incMessage = readJMS(receivedMessage);

            if (incMessage == null || incMessage[0] == null) {
                return;
            }

            String textMessage = incMessage[0].trim();
            journalId = (Long) coreRepository.executeInNewTransaction((persistence) -> {
                return createJournalEntry(queueType, textMessage);
            });
            processing(queueType, textMessage, journalId, incMessage, toQueue, receiveTime, waitingTime);

        } catch (JMSException e) {
            reConnect();
            auditController.warning(QueueProcessor, getAuditMessage("при получении сообщения", fromQueue, journalId), getJournalName(), journalId.toString(), e);
        } catch (Throwable e) {
            log.error("Ошибка при обработке сообщения из " + fromQueue, e);
            auditController.warning(QueueProcessor, getAuditMessage("при обработке сообщения", fromQueue, journalId), getJournalName(), journalId.toString(), e);

            try {
                if(journalId != null) {
                    Long finalJournalId = journalId;
                    coreRepository.executeInNewTransaction((persistence) -> {
                        updateStatusErrorProc(finalJournalId, e);
                        return null;
                    });
                }
            } catch (Throwable e1) {
                auditController.warning(QueueProcessor, getAuditMessage("при изменении статуса сообщения", fromQueue, journalId), getJournalName(), journalId.toString(), e1);
            }
            context.setRollbackOnly();
            // TODO ???
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    // Удобно для тестирования
    public Long processingWithLog(String queueType, String[] incMessage, String toQueue, long receiveTime, long waitingTime) throws Exception {
        String textMessage = incMessage[0].trim();
        Long journalId = (Long) coreRepository.executeInNewTransaction((persistence) -> {
            return createJournalEntry(queueType, textMessage);
        });
        processing(queueType, textMessage, journalId, incMessage, toQueue, receiveTime, waitingTime);
        return journalId;
    }

    private void processing(String queueType, String textMessage, Long journalId, String[] incMessage, String toQueue, long receiveTime, long waitingTime) throws Exception {
//    public Long processing(String queueType, String[] incMessage, String toQueue, long receiveTime, long waitingTime) throws Exception {
//        String textMessage = incMessage[0].trim();
//        Long journalId = (Long) coreRepository.executeInNewTransaction((persistence) -> {
//            return createJournalEntry(queueType, textMessage);
//        });
        long startProcessing = System.currentTimeMillis();
        String outMessage = (String) coreRepository.executeInNewTransaction(persistence1 -> {
            return processQuery(queueType, textMessage, journalId);
        });

        if (!isEmpty(outMessage)) {
            try {
                long createAnswerTime = System.currentTimeMillis();
                sendToQueue(outMessage, queueProperties, incMessage, toQueue);
                long sendingAnswerTime = System.currentTimeMillis();
                coreRepository.invokeAsynchronous(em -> {
                    updateStatusSuccess(journalId, getTimesComment(receiveTime, startProcessing, createAnswerTime, sendingAnswerTime, waitingTime),
                            "true".equals(queueProperties.writeOut) ? outMessage : null);
                    return null;
                });
            } catch (JMSRuntimeException | JMSException e) {
                auditController.warning(QueueProcessor, getAuditMessage("при отправке сообщения", toQueue, journalId), getJournalName(), journalId.toString(), e);
                coreRepository.invokeAsynchronous(em -> {
                    updateStatusErrorOut(journalId, e);
                    return null;
                });
            }
        }
    }

    protected String[] readFromJMS(JMSConsumer consumer) throws JMSException {
        Message receivedMessage = consumer.receiveNoWait();
        if (receivedMessage == null) {
            return null;
        }
        return readJMS(receivedMessage);
    }

    protected String[] readJMS(Message receivedMessage) throws JMSException {
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

    public void sendToQueue(String outMessage, QueueProperties queueProperties, String[] incMessage, String queue) throws JMSException {
        TextMessage message = jmsContext.createTextMessage(outMessage);
        message.setJMSCorrelationID(incMessage[1]);
        JMSProducer producer = jmsContext.createProducer();
        producer.send(jmsContext.createQueue(!isEmpty(incMessage[2]) ? incMessage[2] : "queue:///" + queue), message);
    }

    protected String getAuditMessage(String msg, String fromQueue, Long jId) {
        return String.format("Ошибка %s из %s / Таблица %s / id=%d", msg, fromQueue, getJournalName(), jId);
    }

    protected String getTimesComment(long receiveTime, long startProcessing, long createAnswerTime, long sendingAnswerTime, long waitingTime) {
        return receiveTime
                + "/"
                + (createAnswerTime - startProcessing)
                + "/"
                + (sendingAnswerTime - createAnswerTime)
                + "/"
                + ("true".equals(queueProperties.writeSleepThreadTime) ? waitingTime : "")
                + "/";
    }

    // TODO асинхронная обработка - не проверялась и сейчас не используется
    @Override
    public void onMessage(Message message) {
        String[] params = queueProperties.mqTopics.split(":");
        // TODO вместо всего блока можно использовать
        // processMessage(message, params[0], params[1], params.length > 2 ? params[2] : null, -1, -1);
        Long journalId = null;
        try {
            String[] incMessage = readJMS(message);
            String queueType = params[0];
            String textMessage = incMessage[0].trim();
            journalId = (Long) coreRepository.executeInNewTransaction((persistence) -> {
                return createJournalEntry(queueType, textMessage);
            });

            processing(queueType, textMessage, journalId, incMessage, params[2], -1L, -1L);

        } catch (JMSException e) {
            //reConnect();
            String fromQueue = params[1];
            auditController.warning(QueueProcessor, getAuditMessage("при обработке сообщения", fromQueue, journalId), getJournalName(), journalId.toString(), e);
        } catch (Exception ex) {
            log.error("Ошибка при обработке сообщения", ex);
        }
    }
}
