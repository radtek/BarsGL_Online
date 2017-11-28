package ru.rbt.barsgl.ejb.controller.operday.task;

import com.ibm.jms.JMSMessage;
import com.ibm.mq.jms.*;
import com.ibm.msg.client.wmq.WMQConstants;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.common.repository.od.OperdayRepository;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.AccountDetailsNotifyProcessor;
import ru.rbt.barsgl.ejb.entity.acc.AcDNJournal;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountService;
import ru.rbt.barsgl.ejb.repository.AcDNJournalRepository;
import ru.rbt.barsgl.ejb.repository.GLAccountRequestRepository;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.jms.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.util.*;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Status.ERROR;
import static ru.rbt.audit.entity.AuditRecord.LogCode.AccountDetailsNotify;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;
import static ru.rbt.ejbcore.util.StringUtils.substr;


/**
 * Created by ER22228 on 14.03.2016.
 */
public class AccountDetailsNotifyTask implements ParamsAwareRunnable {

    private static final Logger log = Logger.getLogger(AccountDetailsNotifyTask.class);
    private static final String SCHEDULED_TASK_NAME = "AccountDetailsNotify";
    private static final int defaultQueueBachSize = 5;

    @Inject
    private AccountDetailsNotifyProcessor messageProcessor;

    @Inject
    private OperdayRepository operdayRepository;

    @EJB
    private AuditController auditController;

    @EJB
    private CoreRepository coreRepository;

    @Inject
    private OperdayController operdayController;

    @EJB
    private AcDNJournalRepository journalRepository;

    @Inject
    private ru.rbt.ejbcore.util.DateUtils dateUtils;

    private static final String UU_DATE_KEY = "operday";
    private static final String UU_CHECK_RUN = "checkRun";

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        try {
            if (checkRun(operdayController.getOperday(), properties)) {
                executeWork(properties);
            }
        } catch (Exception e) {
            log.error("AccountDetailsNotify #run ", e);
            auditController.error(AccountDetailsNotify, "Ошибка при выполнении задачи AccountDetailsNotifyTask", null, e);
        }
    }

    private java.util.Date getExecuteDate(Properties properties) throws ParseException {
        String propday = Optional.ofNullable(properties.getProperty(UU_DATE_KEY)).orElse("");
        java.util.Date operday;
        if (isEmpty(propday)) {
            operday = operdayController.getOperday().getCurrentDate();
        } else {
            operday = DateUtils.parseDate(propday, "dd.MM.yyyy", "yyyy-MM-dd");
        }
//        auditController.info(AccountDetailsNotify, "operday: " + operday);
        return operday;
    }

    public boolean checkRun(Operday operday, Properties properties) throws Exception {
        if (Boolean.parseBoolean(Optional.ofNullable(properties.getProperty(UU_CHECK_RUN)).orElse("true"))) {
            java.util.Date executeDate = getExecuteDate(properties);
            DataRecord rec = operdayRepository.selectFirst(
                "select count (1) from GL_ETLDWHS where PARDESC = ? and OPERDAY = ?"
                , SCHEDULED_TASK_NAME, executeDate);
            boolean already = 0 < rec.getInteger(0);
            if (already) {
                auditController.warning(AccountDetailsNotify, "Ошибка при открытии счетов из SRVACC", null
                    , format("Открытие счетов из SRVACC невозможна: уже запущена или выполнена в текущем ОД (%s) <%s>"
                        , dateUtils.onlyDateString(executeDate), true));
                return false;
            }
            return true;
        } else {
            return true;
        }
    }

    private void executeWork(Properties properties) throws Exception {
        String mqType = Optional.ofNullable(properties.getProperty("mq.type")).orElse("###");
        String mqHost = Optional.ofNullable(properties.getProperty("mq.host")).orElse("###");
        String mqPortStr = Optional.ofNullable(properties.getProperty("mq.port")).orElse("###");
        String mqQueueManager = Optional.ofNullable(properties.getProperty("mq.queueManager")).orElse("###");
        String mqChannel = Optional.ofNullable(properties.getProperty("mq.channel")).orElse("###");
        String mqTopicStr = Optional.ofNullable(properties.getProperty("mq.topics")).orElse("###");
        String mqBatchSize = Optional.ofNullable(properties.getProperty("mq.batchSize")).orElse("" + defaultQueueBachSize);
        String mqAlgo = Optional.ofNullable(properties.getProperty("mq.algo")).orElse("simple"); // hard - with browsing, simple - default

        String mqUser = Optional.ofNullable(properties.getProperty("mq.user")).orElse("###");
        String mqPassword = Optional.ofNullable(properties.getProperty("mq.password")).orElse("###");
        if ((mqType + mqHost + mqPortStr + mqQueueManager + mqChannel + mqTopicStr + mqUser + mqPassword).contains("###")) {
            log.error("AccountDetailsNotify " + "Ошибка в параметрах подключения к серверу очередей (без user и password). " +
                          (mqType + "/" + mqHost + "/" + mqPortStr + "/" + mqQueueManager + "/" + mqChannel + "/" + mqTopicStr));
            return;
        }

        int batchSize;
        try {
            batchSize = Integer.parseInt(mqBatchSize);
        } catch (Exception e) {
            batchSize = defaultQueueBachSize;
        }

        int mqPort;
        try {
            mqPort = Integer.parseInt(mqPortStr);
        } catch (Exception e) {
            log.error("AccountDetailsNotify " + "Ошибка в параметрах подключения к серверу очередей. mqPort:" + mqPortStr, e);
            return;
        }

        String[] mqTopics = mqTopicStr.split(";");

        if("hard".equals(mqAlgo)) {
            queueProcessorTransact(mqHost, mqPort, mqQueueManager, mqChannel, mqTopics, batchSize, mqUser, mqPassword);
        }else{
            queueProcessor(mqHost, mqPort, mqQueueManager, mqChannel, mqTopics, batchSize, mqUser, mqPassword);
        }
    }

    private Object[] readFromJMS(MQMessageConsumer receiver) throws Exception {
        Message receivedMessage = (Message) receiver.receiveNoWait();
        if (receivedMessage == null) {
            return null;
        }

        log.info("AccountDetailsNotify " + "JMSMessageID:" + receivedMessage.getJMSMessageID() + " Получено сообщение. " + receivedMessage.toString());
        return new Object[]{receivedMessage, getMessageText(receivedMessage)};
    }

    private String getMessageText(Message receivedMessage) throws Exception {
        String textMessage = null;
        if (receivedMessage instanceof TextMessage) {
            textMessage = ((TextMessage) receivedMessage).getText();
            log.info("AccountDetailsNotify " + "JMSMessageID:" + receivedMessage.getJMSMessageID() + " Текстовое сообщение");
        } else if (receivedMessage instanceof BytesMessage) {
//            textMessage = ((BytesMessage) receivedMessage).readUTF();
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
                String message = "JMSMessageID:" + receivedMessage.getJMSMessageID() + " Ошибка при чтении байтового сообщения из очереди";
                log.error("AccountDetailsNotify " + message, e);
                auditController.error(AccountDetailsNotify, message, null, e);
                throw new Exception(message);
            }
            log.info("AccountDetailsNotify " + "JMSMessageID:" + receivedMessage.getJMSMessageID() + " Байтовое сообщение");
        } else {
            String message = "JMSMessageID:" + receivedMessage.getJMSMessageID() + " Сообщение в необрабатываемом формате.";
            log.error("AccountDetailsNotify " + message);
            auditController.error(AccountDetailsNotify, message, null, "");
            throw new Exception(message);
        }

        if (isEmpty(textMessage)) {
            String message = "JMSMessageID:" + receivedMessage.getJMSMessageID() + " Пустое сообщение.";
            log.error("AccountDetailsNotify " + message);
            auditController.error(AccountDetailsNotify, message, null, "");
            throw new Exception(message);
        }
        return textMessage;
    }

    private void queueProcessor(String mqHost, int mqPort, String mqQueueManager, String mqChannel,
                                String[] mqTopics, int batchSize, String mqUser, String mqPassword) throws Exception {
        MQQueueConnection connection = null;
        MQQueueSession session = null;

        Long jId = 0L;

        try {
            MQQueueConnectionFactory cf = getConnectionFactory(mqHost, mqPort, mqQueueManager, mqChannel);

            connection = (MQQueueConnection) cf.createQueueConnection(mqUser, mqPassword);
            session = (MQQueueSession) connection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);//Session.AUTO_ACKNOWLEDGE);

            for (String mqTopic : mqTopics) {
                processOneQueue(batchSize, session, connection, mqTopic);
            }

            session.close();
            connection.close();

        } catch (Exception e) {
            String message = "Ошибка при обработке сообщения (см. GL_ACDENO id=" + jId + ")";
            log.error("AccountDetailsNotify " + message, e);
            auditController.error(AccountDetailsNotify, message, null, e);
        } finally {
            if (session != null) {
                session.close();
            }
            if (connection != null) {
                connection.close();
            }
            log.info("AccountDetailsNotify " + "Сессия обработки очередей завершена. ");
        }
    }

    private MQQueueConnectionFactory getConnectionFactory(String mqHost, int mqPort, String mqQueueManager, String mqChannel) throws javax.jms.JMSException {
        MQQueueConnectionFactory cf = new MQQueueConnectionFactory();
        cf.setHostName(mqHost);
        cf.setPort(mqPort);
        cf.setTransportType(WMQConstants.WMQ_CM_CLIENT);
        cf.setQueueManager(mqQueueManager);
        cf.setChannel(mqChannel);
        return cf;
    }

    private void processOneQueue(int batchSize, MQQueueSession session, MQQueueConnection connection, String mqTopic) throws Exception {
        MQQueueReceiver receiver = null;

        int processedMessage;
        String[] parts = mqTopic.split(":");

        AcDNJournal.Sources source = AcDNJournal.Sources.valueOf(parts[0]);// "FCC".equals(parts[0]) ? AcDNJournal.Sources.FCC : AcDNJournal.Sources.MIDAS_OPEN;
        if (null == source) {
            auditController.error(AccountDetailsNotify, "Ошибка при подключении сервиса AccountDetailsNotify", null, String.format("Неверный параметр topik: '%s'", parts[0]));
            return;
        }
        MQQueue queue = (MQQueue) session.createQueue("queue:///" + parts[1]);
        receiver = (MQQueueReceiver) session.createReceiver(queue);

        connection.start();

        processedMessage = readAndProcess(receiver, source, batchSize, session);

        receiver.close();
        log.info("AccountDetailsNotify " + "Обрабработано сообщений из " + parts[0] + ":" + processedMessage);
    }

    private void processOneQueueTransact(int batchSize, MQQueueSession session, MQQueueConnection connection, String mqTopic) throws Exception {
        MQQueueReceiver receiver = null;

        int processedMessage;
        String[] parts = mqTopic.split(":");

        AcDNJournal.Sources source = AcDNJournal.Sources.valueOf(parts[0]);// "FCC".equals(parts[0]) ? AcDNJournal.Sources.FCC : AcDNJournal.Sources.MIDAS_OPEN;
        if (null == source) {
            auditController.error(AccountDetailsNotify, "Ошибка при подключении сервиса AccountDetailsNotify", null, String.format("Неверный параметр topik: '%s'", parts[0]));
            return;
        }
        MQQueue queue = (MQQueue) session.createQueue("queue:///" + parts[1]);
        receiver = (MQQueueReceiver) session.createReceiver(queue);
        MQQueueBrowser browser = (MQQueueBrowser) session.createBrowser(queue);

        connection.start();

        Map<String, String> incMessagesMap = getMessageList(browser, batchSize);
        processedMessage = readAndProcessTransact(receiver, source, incMessagesMap, batchSize, session);

        receiver.close();
        log.info("AccountDetailsNotify " + "Обрабработано сообщений из " + parts[0] + ":" + processedMessage);
    }

    private Map<String, String> getMessageList(MQQueueBrowser browser, int batchSize) throws Exception {
        Map<String, String> incMessagesMap = new HashMap<>();
        Enumeration messageEnumeration = browser.getEnumeration();
        int batchNum = 0;
        StringBuilder queueContent = new StringBuilder();
        while (messageEnumeration.hasMoreElements()) {
            if (batchNum == 0) {
                log.info("\n\rОчередь содержит для обработки следующие сообщения:");
            }

            Message message = (Message) messageEnumeration.nextElement();
            log.info("AccountDetailsNotify " + "JMSMessage" + message.getJMSMessageID());
            String textMessage = getMessageText(message);
            String bsaacid = getBsaacidFromMessage(textMessage);
            log.info("AccountDetailsNotify " + "JMSMessage" + message.getJMSMessageID() + " / cbAccountNo=" + bsaacid);
            queueContent.append("JMSMessage").append(message.getJMSMessageID()).append(" / cbAccountNo=").append(bsaacid).append(" \n");
            incMessagesMap.put(message.getJMSMessageID(), textMessage);
            batchNum++;
        }

        browser.close();
        if (queueContent.length() > 0) {
            log.info("AccountDetailsNotify Просмотр очереди завершён. Размер списка: " + batchNum);
            auditController.stat(AccountDetailsNotify, "Список сообщений в очереди. длина:" + batchNum, substr(queueContent.toString(), 3900));
        }
        return incMessagesMap;
    }

    private int readAndProcess(MQQueueReceiver receiver, AcDNJournal.Sources source, int batchSize, MQQueueSession session) throws Exception {
        int processedMessage = 0;
        Message msg = null;
        batchSize = batchSize == -1 ? Integer.MAX_VALUE : batchSize;
        for (int i = 0; i < batchSize; i++) {
            try {
                msg = receiver.receiveNoWait();
                if (msg == null) {
                    break;
                }

                String textMessage = getMessageText(msg);
                log.info("AccountDetailsNotify " + " JMSMessageID:" + msg.getJMSMessageID() + " / cbAccountNo=" + getBsaacidFromMessage(textMessage) + " readed");

                if (isEmpty(textMessage)) {
                    log.error("AccountDetailsNotify " + "JMSMessageID:" + msg.getJMSMessageID() + " не удалось получить текст сообщения");
                    auditController.error(AccountDetailsNotify, "JMSMessageID:" + msg.getJMSMessageID() + " не удалось получить текст сообщения", null, "");
                } else {
                    processedMessage++;
                    processOneMessage(source, textMessage, msg);
                }

                log.info("AccountDetailsNotify " + " JMSMessageID:" + msg.getJMSMessageID() + " / cbAccountNo=" + getBsaacidFromMessage(textMessage) + " processed");
            } catch (JMSException e) {
                log.error("AccountDetailsNotify " + "JMSMessageID:" + (msg != null ? msg.getJMSMessageID() : "без номера") + " " + e.getMessage() + e.getLinkedException().getMessage() + msg.toString());
                auditController.error(AccountDetailsNotify, "JMSMessageID:" + (msg != null ? msg.getJMSMessageID() : "без номера"), null, e);
                throw new Exception(e);
            }
        }
        return processedMessage;
    }

    private int readAndProcessTransact(MQQueueReceiver receiver, AcDNJournal.Sources source, Map<String, String> incMessagesMap, int batchSize, MQQueueSession session) throws Exception {
        int processedMessage = 0;
        Message msg = null;
        batchSize = batchSize == -1 ? Integer.MAX_VALUE : batchSize;
        for (int i = 0; i < batchSize; i++) {
            try {
                msg = receiver.receiveNoWait();
                if (msg == null) {
                    break;
                }

                String textMessage;
                if (incMessagesMap.containsKey(msg.getJMSMessageID())) {
                    textMessage = incMessagesMap.get(msg.getJMSMessageID());
                } else {
                    log.error("AccountDetailsNotify " + "JMSMessageID:" + msg.getJMSMessageID() + " не содержится в списке, полученном при предварительном просмотре очереди");
                    textMessage = getMessageText(msg);
                }
                log.info("AccountDetailsNotify " + " JMSMessageID:" + msg.getJMSMessageID() + " / cbAccountNo=" + getBsaacidFromMessage(textMessage) + " readed");

                if (isEmpty(textMessage)) {
                    log.error("AccountDetailsNotify " + "JMSMessageID:" + msg.getJMSMessageID() + " не удалось получить текст сообщения");
                    auditController.error(AccountDetailsNotify, "JMSMessageID:" + msg.getJMSMessageID() + " не удалось получить текст сообщения", null, "");
                } else {
                    processedMessage++;
                    processOneMessage(source, textMessage, msg);
                }
                session.commit();
                log.info("AccountDetailsNotify " + " JMSMessageID:" + msg.getJMSMessageID() + " / cbAccountNo=" + getBsaacidFromMessage(textMessage) + " processed");
            } catch (JMSException e) {
                log.error("AccountDetailsNotify " + "JMSMessageID:" + (msg != null ? msg.getJMSMessageID() : "без номера") + " " + e.getMessage() + e.getLinkedException().getMessage() + msg.toString());
                session.rollback();
                auditController.error(AccountDetailsNotify, "JMSMessageID:" + (msg != null ? msg.getJMSMessageID() : "без номера"), null, e);
            }
        }
        return processedMessage;
    }

    private String getBsaacidFromMessage(String textMessage) {
        String bsaacid = null;
        if (!isEmpty(textMessage)) {
            int index = textMessage.toLowerCase().indexOf("cbaccountno>");
            if (index > -1) {
                bsaacid = textMessage.substring(index + 12, index + 32);
            }
        }
        return bsaacid;
    }

    //
    // метод сделан для тестирования обработки сообщений
    //
    public void processOneMessage(AcDNJournal.Sources source, String textMessage, Message jmsm) throws Exception {
        Long jId = (Long) coreRepository.executeInNewTransaction(persistence -> {
            return journalRepository.createJournalEntry(source, textMessage);
        });

        // Приняли и записали сообщение - подтверждаем получение
        if (jmsm != null) {
            jmsm.acknowledge();
        }

        try {
            coreRepository.executeInNewTransaction(persistence -> {
                messageProcessor.process(source, textMessage, jId);
                return 0;
            });
        } catch (Exception e) {
            String message = "Ошибка при обработке сообщения (см. GL_ACDENO id=" + jId + ")";
            log.error("AccountDetailsNotify " + message, e);
            auditController.error(AccountDetailsNotify, message, null, e);
            coreRepository.executeInNewTransaction(persistence -> {
                journalRepository.updateLogStatus(jId, ERROR, e.getMessage().length() > 127 ? e.getMessage().substring(0, 127) : e.getMessage());
                return 0;
            });
        }
    }

    private void queueProcessorTransact(String mqHost, int mqPort, String mqQueueManager, String mqChannel,
                                String[] mqTopics, int batchSize, String mqUser, String mqPassword) throws Exception {
        MQQueueConnection connection = null;
        MQQueueSession session = null;

        Long jId = 0L;
        boolean transacted = true;

        try {
            MQQueueConnectionFactory cf = getConnectionFactory(mqHost, mqPort, mqQueueManager, mqChannel);

            connection = (MQQueueConnection) cf.createQueueConnection(mqUser, mqPassword);
            session = (MQQueueSession) connection.createQueueSession(transacted, Session.CLIENT_ACKNOWLEDGE);//Session.AUTO_ACKNOWLEDGE);

            for (String mqTopic : mqTopics) {
                processOneQueueTransact(batchSize, session, connection, mqTopic);
            }

            session.close();
            connection.close();

        } catch (Exception e) {
            String message = "Ошибка при обработке сообщения (см. GL_ACDENO id=" + jId + ")";
            log.error("AccountDetailsNotify " + message, e);
            auditController.error(AccountDetailsNotify, message, null, e);
        } finally {
            if (session != null) {
                session.close();
            }
            if (connection != null) {
                connection.close();
            }
            log.info("AccountDetailsNotify " + "Сессия обработки очередей завершена. ");
        }
    }

}
