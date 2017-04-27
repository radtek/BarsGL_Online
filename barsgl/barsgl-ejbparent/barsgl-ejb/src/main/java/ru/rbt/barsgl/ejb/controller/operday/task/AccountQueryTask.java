package ru.rbt.barsgl.ejb.controller.operday.task;

import com.ibm.jms.JMSBytesMessage;
import com.ibm.jms.JMSMessage;
import com.ibm.jms.JMSTextMessage;
import com.ibm.mq.jms.*;
import com.ibm.msg.client.wmq.WMQConstants;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.common.repository.od.OperdayRepository;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.AccountQueryBAProcessor;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.AccountQueryProcessor;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.MasterAccountProcessor;
import ru.rbt.barsgl.ejb.entity.acc.AclirqJournal;
import ru.rbt.barsgl.ejb.repository.AclirqJournalRepository;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.AccountQueryRepository;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.util.*;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.AccountQuery;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;


/**
 * Created by ER22228 on 14.03.2016.
 */
public class AccountQueryTask implements ParamsAwareRunnable {

    private static final Logger log = Logger.getLogger(AccountQueryTask.class);
    private static final String SCHEDULED_TASK_NAME = "AccountQuery";
    private static final int defaultQueueBachSize = 20;

    @Inject
    private OperdayRepository operdayRepository;

    @EJB
    private AuditController auditController;

    @EJB
    private CoreRepository coreRepository;

    @Inject
    private OperdayController operdayController;

    @Inject
    private AccountQueryProcessor queryProcessor;

    @Inject
    private AccountQueryBAProcessor queryProcessorBA;

    @Inject
    private MasterAccountProcessor queryProcessorMAPB;

    @Inject
    private ru.rbt.ejbcore.util.DateUtils dateUtils;

    @EJB
    private AccountQueryRepository queryRepository;

    @EJB
    private AclirqJournalRepository journalRepository;

    private static final String UU_DATE_KEY = "operday";
    private static final String UU_CHECK_RUN = "checkRun";

    @Override
    public void run(String jobName, Properties properties) throws Exception {
//        auditController.info(AccountQuery, "Старт задачи AccountQueryTask", null, "");
        coreRepository.executeInNewTransaction(persistence -> {
            try {
                log.info("AccountQueryTask started");
                if (checkRun(operdayController.getOperday(), properties)) {
                    loadCurrency();
                    executeWork(properties);
                    log.info("AccountQueryTask finished");
                }
            } catch (Exception e) {
                auditController.error(AccountQuery, "Ошибка при выполнении задачи AccountQueryTask", null, e);
            }
            return null;
        });
    }

    private static final Map<String, String> currencyMap = new HashMap<>();
    private static final Map<String, Integer> currencyNBDPMap = new HashMap<>();

    private void loadCurrency() throws Exception {
        queryRepository.loadCurrency(currencyMap, currencyNBDPMap);
    }

    private Date getExecuteDate(Properties properties) throws ParseException {
        String propday = Optional.ofNullable(properties.getProperty(UU_DATE_KEY)).orElse("");
        Date operday;
        if (isEmpty(propday)) {
            operday = operdayController.getOperday().getCurrentDate();
        } else {
            operday = DateUtils.parseDate(propday, "dd.MM.yyyy", "yyyy-MM-dd");
        }
        return operday;
    }

    public boolean checkRun(Operday operday, Properties properties) throws Exception {
        if (Boolean.parseBoolean(Optional.ofNullable(properties.getProperty(UU_CHECK_RUN)).orElse("true"))) {
            Date executeDate = getExecuteDate(properties);
            DataRecord rec = operdayRepository.selectFirst(
                "select count (1) from GL_ETLDWHS where PARDESC = ? and OPERDAY = ?"
                , SCHEDULED_TASK_NAME, executeDate);
            boolean already = 0 < rec.getInteger(0);
            if (already) {
                auditController.warning(AccountQuery, "Ошибка ответе на запрос AccountQuery", null
                    , format("Обработка AccountQuery невозможна: уже запущена или выполнена в текущем ОД (%s) <%s>"
                        , dateUtils.onlyDateString(executeDate), true));
                return false;
            }
            return true;
        } else {
            return true;
        }
    }

    private void executeWork(Properties properties) throws Exception {
        String mqHost = Optional.ofNullable(properties.getProperty("mq.host")).orElse("###");
        String mqPortStr = Optional.ofNullable(properties.getProperty("mq.port")).orElse("###");
        String mqQueueManager = Optional.ofNullable(properties.getProperty("mq.queueManager")).orElse("###");
        String mqChannel = Optional.ofNullable(properties.getProperty("mq.channel")).orElse("###");
        String mqTopicStr = Optional.ofNullable(properties.getProperty("mq.topics")).orElse("###");
        String mqBatchSize = Optional.ofNullable(properties.getProperty("mq.batchSize")).orElse("" + defaultQueueBachSize);

        String mqUser = Optional.ofNullable(properties.getProperty("mq.user")).orElse("###");
        String mqPassword = Optional.ofNullable(properties.getProperty("mq.password")).orElse("###");
        if ((mqHost + mqPortStr + mqQueueManager + mqChannel + mqTopicStr + mqUser + mqPassword).contains("###")) {
            log.error("Ошибка в параметрах подключения к серверу очередей (без user и password). " +
                          (mqHost + "/" + mqPortStr + "/" + mqQueueManager + "/" + mqChannel + "/" + mqTopicStr));
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
            log.error("Ошибка в параметрах подключения к серверу очередей. mqPort:" + mqPortStr, e);
            return;
        }

        queueProcessor(mqHost, mqPort, mqQueueManager, mqChannel, mqTopicStr, batchSize, mqUser, mqPassword);
    }

    private void queueProcessor(String mqHost, int mqPort, String mqQueueManager, String mqChannel, String mqTopics, int batchSize, String mqUser, String mqPassword) throws Exception {
        MQQueueConnection connection = null;
        MQQueueSession session = null;
        try {
            MQQueueConnectionFactory cf = new MQQueueConnectionFactory();

            cf.setHostName(mqHost);
            cf.setPort(mqPort);
            cf.setTransportType(WMQConstants.WMQ_CM_CLIENT);
            cf.setQueueManager(mqQueueManager);
            cf.setChannel(mqChannel);

            connection = (MQQueueConnection) cf.createQueueConnection(mqUser, mqPassword);
            session = (MQQueueSession) connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);


            for (String item : mqTopics.split(";")) {
                processQueues(batchSize, session, connection, item);
            }

            session.close();
            connection.close();
            log.info("Сессия обработки очередей завершена");
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
        JMSMessage receivedMessage = (JMSMessage) receiver.receive(1);
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
                "\t\t\t<asbo:DateTime>" + dateTime.toString() + "</asbo:DateTime>\n" + //2016-06-24T16:10:03.010925+03:00
                "\t\t</asbo:Error>\n";
        return answerBody;
    }

    private void processQueues(int batchSize, MQQueueSession session, MQQueueConnection connection, String queueNames) throws Exception {
        String[] params = queueNames.split(":");
        MQQueue queueIn = (MQQueue) session.createQueue("queue:///" + params[1]);
//        MQQueue queueOut = (MQQueue) session.createQueue("queue:///" + params[2]);
        MQQueueReceiver receiver = (MQQueueReceiver) session.createReceiver(queueIn);
//        MQQueueSender sender = (MQQueueSender) session.createSender(queueOut);

        connection.start();

        for (int i = 0; i < batchSize; i++) {
            String[] incMessage = readFromJMS(receiver);
            if (incMessage == null || incMessage[0] == null) {
                break;
            }

            String textMessage = incMessage[0].trim();
//            if(textMessage.contains("Error")) continue;
            String outMessage = null;
            Long jId = 0L;
            try {
                jId = (Long) coreRepository.executeInNewTransaction(persistence -> {
                    return journalRepository.createJournalEntry(params[0], textMessage);
                });

                Long finalJId = jId;
                outMessage = (String) coreRepository.executeInNewTransaction(persistence -> {
                    try {
                        switch (params[0]) {
                            case "LIRQ":
                                return queryProcessor.process(textMessage, currencyMap, currencyNBDPMap, finalJId, false);
                            case "BALIRQ":
                                return queryProcessorBA.process(textMessage, currencyMap, currencyNBDPMap, finalJId);
                            case "MAPBRQ":
                                return queryProcessorMAPB.process(textMessage, finalJId);
                        }
                    } catch (Exception e) {
                        AclirqJournal aclirqJournal = journalRepository.findById(AclirqJournal.class, finalJId);
                        return getErrorMessage(aclirqJournal.getComment());
                    }
                    return null;
                });

                if (!isEmpty(outMessage)) {
                    JMSTextMessage message = (JMSTextMessage) session.createTextMessage(outMessage);
                    message.setJMSCorrelationID(incMessage[1]);
                    MQQueue queueOut = (MQQueue) session.createQueue(!isEmpty(incMessage[2]) ? incMessage[2] : "queue:///" + params[2]);
                    MQQueueSender sender = (MQQueueSender) session.createSender(queueOut);
                    sender.send(message);
                    sender.close();
                    journalRepository.updateLogStatus(jId, AclirqJournal.Status.PROCESSED, "");
                    jId = 0L;
                }
            } catch (JMSException e) {
                auditController.error(AccountQuery, "Ошибка при обработке сообщения из " + params[1] + " / Таблица DWH.GL_ACLIRQ / id=" + jId, null, e);
            }

            if (jId > 0) {
                // Была ошибка. Проверяем проставлен ли статус Error
                Long finalJId = jId;
                coreRepository.executeInNewTransaction(persistence -> {
                    if (journalRepository.finalizeOnException(finalJId)) {
                        auditController.error(AccountQuery, "Ошибка обработки сообщения. Запись " + finalJId + " в GL_ACLIRQ", null, "");
                    }
                    return 0;
                });
            }
        }

        receiver.close();
    }
}
