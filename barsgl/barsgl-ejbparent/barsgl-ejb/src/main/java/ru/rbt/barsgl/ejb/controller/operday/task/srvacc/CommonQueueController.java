package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import org.apache.log4j.Logger;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.AsyncProcessor;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.JpaAccessCallback;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import javax.jms.*;
import javax.persistence.EntityManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static ru.rbt.audit.entity.AuditRecord.LogCode.AccountQuery;
import static ru.rbt.audit.entity.AuditRecord.LogCode.QueueProcessor;
import static ru.rbt.barsgl.ejb.props.PropertyName.PD_CONCURENCY;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;

/**
 * Created by er18837 on 12.12.2017.
 */
public abstract class CommonQueueController {
    private static final Logger log = Logger.getLogger(CommonQueueController.class);
    private static final int defaultQueueBachSize = 10;

    protected QueueProperties queueProperties;

    public void setQueueProperties(Properties properties) throws Exception {
        this.queueProperties = new QueueProperties(properties);
    }

    public QueueProperties getQueueProperties() {
        return queueProperties;
    }

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

    @Inject @Any
    protected QueueCommunicator queueCommunicator;

    protected abstract void afterConnect() throws Exception;
    protected abstract QueueProcessResult processQuery(String queueType, String textMessage, Long jId) throws Exception;

    protected abstract String getJournalName();
    protected abstract Long createJournalEntryInternal(String queueType, String textMessage) throws Exception;
    protected abstract void updateStatusSuccessOut(Long journalId, String comment, QueueProcessResult result) throws Exception;
    protected abstract void updateStatusErrorProc(Long journalId, Throwable e) throws Exception;
    protected abstract void updateStatusErrorOut(Long journalId, Throwable e) throws Exception;

    protected Charset getCharset() { return StandardCharsets.UTF_8;}
    protected int getConcurencySize() { return 10; }
    protected long getTimeout() { return 10L; }
    protected TimeUnit getTimeoutUnit() { return TimeUnit.MINUTES; };


    public Long createJournalEntry(String queueType, String textMessage) throws Exception{
        return (Long) coreRepository.executeInNewTransaction((persistence) -> {
            return createJournalEntryInternal(queueType, textMessage);
        });
    };

    public void process(Properties properties) throws Exception {
        try {
            setQueueProperties(properties);
            try{
                queueCommunicator.startConnection(getQueueProperties());
                afterConnect();
                processSources();
            }catch(JMSRuntimeException | JMSException ex){
                // reset session
                queueCommunicator.reConnect();
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

        try (JMSConsumer consumer = queueCommunicator.createConsumer(inQueue);) {
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
        asyncProcessor.awaitTermination(executor, timeout, unit, tillTo);
    }

/* // TODO пока оставляем SysError
    private void awaitTermination(ExecutorService executor, long timeout, TimeUnit unit) throws Exception {
        final long tillTo = System.currentTimeMillis() + unit.toMillis(timeout);
        try {
            asyncProcessor.awaitTermination(executor, timeout, unit, tillTo);
        } catch (TimeoutException e) {
            throw new ValidationError(ErrorCode.QUEUE_ERROR, e.getMessage());
        }
    }
*/

    private void processMessage(Message receivedMessage, String queueType, String fromQueue, String toQueue, long receiveTime, long startThreadTime){
        long waitingTime = System.currentTimeMillis() - startThreadTime;
        Long journalId = null;
        try {
            QueueInputMessage incMessage = queueCommunicator.readJMS(receivedMessage, getCharset());

            if (incMessage == null || incMessage.textMessage == null) {
                return;
            }

            journalId = createJournalEntry(queueType, incMessage.textMessage);
            processing(queueType, incMessage.textMessage, journalId, incMessage, toQueue, receiveTime, waitingTime);

        } catch (JMSException e) {
            queueCommunicator.reConnect();
            auditController.warning(QueueProcessor, getAuditMessage("при получении сообщения", fromQueue, journalId), getJournalName(), String.valueOf(journalId), e);
        } catch (Throwable e) {
            log.error("Ошибка при обработке сообщения из " + fromQueue, e);
            auditController.warning(QueueProcessor, getAuditMessage("при обработке сообщения", fromQueue, journalId), getJournalName(), String.valueOf(journalId), e);

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
    public Long processingWithLog(String queueType, QueueInputMessage incMessage, String toQueue, long receiveTime, long waitingTime) throws Exception {
        Long journalId = createJournalEntry(queueType, incMessage.textMessage);
        processing(queueType, incMessage.textMessage, journalId, incMessage, toQueue, receiveTime, waitingTime);
        return journalId;
    }

    private void processing(String queueType, String textMessage, Long journalId, QueueInputMessage incMessage, String toQueue, long receiveTime, long waitingTime) throws Exception {
        long startProcessing = System.currentTimeMillis();
        QueueProcessResult processResult = (QueueProcessResult) coreRepository.executeInNewTransaction(persistence1 -> {
            return processQuery(queueType, textMessage, journalId);
        });

        if (null != processResult && !isEmpty(processResult.getOutMessage())) {
            try {
                long createAnswerTime = System.currentTimeMillis();
                queueCommunicator.sendToQueue(processResult.getOutMessage(), queueProperties, incMessage.requestId, incMessage.replyTo, toQueue);
                long sendingAnswerTime = System.currentTimeMillis();
                processResult.setWriteOut("true".equals(queueProperties.writeOut));
                coreRepository.invokeAsynchronous(em -> {
                    updateStatusSuccessOut(journalId, getTimesComment(receiveTime, startProcessing, createAnswerTime, sendingAnswerTime, waitingTime), processResult);
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

    protected QueueInputMessage readFromJMS(JMSConsumer consumer) throws JMSException {
        Message receivedMessage = consumer.receiveNoWait();
        if (receivedMessage == null) {
            return null;
        }
        return queueCommunicator.readJMS(receivedMessage, getCharset());
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

    // =================== оставлено на всякий случай ===========================

    // старый вариант параллельной обработки (И. Севастьянов) - проверить здесь
    private void processSourcesOld() throws Exception {
        String[] params = queueProperties.mqTopics.split(":");

        try(JMSConsumer consumer =  queueCommunicator.createConsumer(params[1]);){

            List<JpaAccessCallback<Void>> callbacks = new ArrayList<>();

            for (int i = 0; i < queueProperties.mqBatchSize; i++) {
                long createReceiveTime = System.currentTimeMillis();

                QueueInputMessage incMessage = readFromJMS(consumer);
                if (incMessage == null || incMessage.textMessage == null) {
                    break;
                }
                long receiveTime = System.currentTimeMillis() - createReceiveTime;

                Long jId = 0L;
                try {
                    jId = createJournalEntry(params[0], incMessage.textMessage);
                    callbacks.add(new CommonRqCallback(params[0], incMessage.textMessage, jId, incMessage, params[2], receiveTime));
                } catch (JMSException e) {
                    queueCommunicator.reConnect();
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

    private class CommonRqCallback implements JpaAccessCallback<Void> {
        String textMessage;
        Long jId;
        QueueInputMessage incMessage;
        String queue;
        String queueType;
        long receiveTime;

        CommonRqCallback(String queueType, String textMessage, Long jId, QueueInputMessage incMessage, String queue, long receiveTime) {
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
