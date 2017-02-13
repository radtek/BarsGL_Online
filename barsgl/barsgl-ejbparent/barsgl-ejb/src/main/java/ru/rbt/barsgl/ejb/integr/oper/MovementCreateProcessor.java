package ru.rbt.barsgl.ejb.integr.oper;

import com.ibm.jms.JMSBytesMessage;
import com.ibm.jms.JMSMessage;
import com.ibm.jms.JMSTextMessage;
import com.ibm.mq.jms.*;
import com.ibm.msg.client.wmq.WMQConstants;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ru.rbt.barsgl.ejb.controller.operday.task.SCASAMCResponseStorage;
import ru.rbt.barsgl.ejb.integr.struct.MovementCreateData;
import ru.rbt.barsgl.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.repository.PropertiesRepository;
import ru.rbt.barsgl.shared.enums.MovementErrorTypes;

import javax.ejb.EJB;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static ru.rbt.barsgl.ejb.controller.operday.task.srvacc.QueueUtil.dateToXML;
import static ru.rbt.barsgl.audit.entity.AuditRecord.LogCode.MovementCreate;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.ifEmpty;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.isEmpty;

/**
 * Created by ER22228 on 02.06.2016.
 */
public class MovementCreateProcessor {
    private static final Logger log = Logger.getLogger(MovementCreateProcessor.class.getName());
    private static final String PROP_ATTEMPTS = "mvmt.attempts";
    private static final String MC_QUEUES_PARAM = "mc.queues.param";

    @EJB
    private AuditController auditController;

    @EJB
    private SCASAMCResponseStorage responseStorage;

    @EJB
    private PropertiesRepository propertiesRepository;

    public static final String SYSTEM_CODE = "BARSGL";
    public static final String EXT_OPERATION_CODE = "PAY";
    public static final String PRIORITY = "5";
    public static final String DIRECTION = "D";

    private static final long ATTEMPTS = 3600;
    private static final int PERIOD = 1000;

    public static final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");

    private static final String[] envelopTemplates = new String[]{
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
            "xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" " +
            "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "    <soapenv:Header>\n"
        ,
        "    </soapenv:Header>\n" +
            "    <soapenv:Body>\n",
        "    </soapenv:Body>\n" +
            "</soapenv:Envelope>"
    };

    /*
INSERT INTO DWH.GL_PRPRP (ID_PRP, ID_PRN, REQUIRED, PRPTP, DESCRP, STRING_VALUE) VALUES (
  'mc.queues.param',
  'root',
  'N',
  'STRING_TYPE',
  'Параметры очередей для сервиса SCASAMovementCreate',
  'mq.host = vs338' || CHR(10) ||
  'mq.port = 1414' || CHR(10) ||
  'mq.queueManager = QM_MBROKER10_TEST' || CHR(10) ||
  'mq.channel = SYSTEM.DEF.SVRCONN' || CHR(10) ||
  'mq.queue.inc = UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF' || CHR(10) ||
  'mq.queue.out = UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF' || CHR(10) ||
  'mq.user=er22228' || CHR(10) ||
  'mq.password=Vugluskr6'
);
     */

    public void process(List<MovementCreateData> mcdList) {

        try {
            loadProperties();
            if (queueProperties == null) {
                auditController.error(MovementCreate, "Ошибка при выполнении задачи MovementCreate. Неполные данные для подключения к очередям", null, "");
                throw new IllegalArgumentException();
            }

            // Формируем конверты запросов
            for (MovementCreateData item : mcdList) {
                createOneEnvelope(item);
            }
            // Debug if local
            sendEnvelops(mcdList);
            if ("yes".equals(queueProperties.local)) {
                putTestAnswer();
            }
            receiveEnvelops(mcdList);
        } catch (Exception e) {
            log.info("MovementCreate ", e);
            auditController.error(MovementCreate, "Ошибка при выполнении задачи MovementCreate.", null, e);
            for (MovementCreateData item : mcdList) {
                item.setErrType(MovementErrorTypes.ERR_REQUEST);
                item.setState(MovementCreateData.StateEnum.ERROR);
            }
        }
    }

    private class QueueProperties {
        String mqHost;
        String mqPortStr;
        String mqQueueManager;
        String mqChannel;

        String mqUser;
        String mqPassword;

        String mqQueueInc;
        String mqQueueOut;

        String local;

        @Override
        public String toString() {
            return "QueueProperties{" +
                       "mqHost='" + mqHost + '\'' +
                       ", mqPortStr='" + mqPortStr + '\'' +
//                       ", mqQueueManager='" + mqQueueManager + '\'' +
                       ", mqChannel='" + mqChannel + '\'' +
                       ", mqQueueInc='" + mqQueueInc + '\'' +
                       ", mqQueueOut='" + mqQueueOut + '\'' +
                       '}';
        }
    }

    private QueueProperties processQueueProperties(Properties properties) {
        QueueProperties queueProperties = new QueueProperties();
        queueProperties.mqHost = Optional.ofNullable(properties.getProperty("mq.host")).orElse("###");
        queueProperties.mqPortStr = Optional.ofNullable(properties.getProperty("mq.port")).orElse("###");
        queueProperties.mqQueueManager = Optional.ofNullable(properties.getProperty("mq.queueManager")).orElse("###");
        queueProperties.mqChannel = Optional.ofNullable(properties.getProperty("mq.channel")).orElse("###");
        queueProperties.mqQueueInc = Optional.ofNullable(properties.getProperty("mq.queue.inc")).orElse("###");
        queueProperties.mqQueueOut = Optional.ofNullable(properties.getProperty("mq.queue.out")).orElse("###");
        queueProperties.local = Optional.ofNullable(properties.getProperty("local")).orElse("no");

        queueProperties.mqUser = Optional.ofNullable(properties.getProperty("mq.user")).orElse("###");
        queueProperties.mqPassword = Optional.ofNullable(properties.getProperty("mq.password")).orElse("###");
        if ((queueProperties.toString()).contains("###")) {
            log.error("Ошибка в параметрах подключения к серверу очередей (без user и password). " + queueProperties.toString());
            return null;
        }
        return queueProperties;
    }

    private QueueProperties queueProperties = null;

    private void loadProperties() throws SQLException, IOException, ExecutionException {
        String strProp = propertiesRepository.getString(MC_QUEUES_PARAM);
        if(isEmpty(strProp)){
          throw new IllegalArgumentException("No data in table GL_PRPRP for ID_PRP='mc.queues.param'");
        }

        Properties properties = new Properties();
        properties.load(new ByteArrayInputStream(strProp.getBytes("UTF-8")));

        queueProperties = processQueueProperties(properties);
    }

    // Debug "server"
    private void putTestAnswer() throws JMSException {
        MQQueueConnectionFactory cf = configMQCF();

        MQQueueConnection connection = (MQQueueConnection) cf.createQueueConnection(queueProperties.mqUser, queueProperties.mqPassword);
        MQQueueSession session = (MQQueueSession) connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        MQQueue queue = (MQQueue) session.createQueue("queue:///" + queueProperties.mqQueueInc);
        MQQueueSender sender = (MQQueueSender) session.createSender(queue);

        MQQueue queueErr = (MQQueue) session.createQueue("queue:///" + queueProperties.mqQueueInc);
        MQQueueSender senderErr = (MQQueueSender) session.createSender(queueErr);

        MQQueue queueOut = (MQQueue) session.createQueue("queue:///" + queueProperties.mqQueueOut);
        MQQueueReceiver receiver = (MQQueueReceiver) session.createReceiver(queueOut);

        connection.start();

        String[] fromOperator = readFromJMS(receiver);

        if (false && Math.random() > 0.2) {
            JMSTextMessage message = (JMSTextMessage) session.createTextMessage();
            message.setText(incomingExample);
            message.setJMSReplyTo(queueOut);

            if (fromOperator != null && fromOperator.length == 3) {
                queue = (MQQueue) session.createQueue(fromOperator[2]);
                sender = (MQQueueSender) session.createSender(queue);
                message.setJMSCorrelationID(fromOperator[0]);
            }

            sender.send(message);
            System.out.println("Test answer sent");

        } else {
            JMSTextMessage message = (JMSTextMessage) session.createTextMessage();
            message.setText(doubleError/*errorExample2*/);
            if (fromOperator != null) {
                message.setJMSCorrelationID(fromOperator[0]);
            }
            senderErr.send(message);
            System.out.println("Test error sent");
        }

        sender.close();
        senderErr.close();
        session.close();
        connection.close();
    }

    private String[] readFromJMS(MQMessageConsumer receiver) throws JMSException {
        JMSMessage receivedMessage = (JMSMessage) receiver.receiveNoWait();//receive(10000);
        if (receivedMessage == null) {
            return new String[]{null, null, null};
        }

        String textMessage = null;
        String correlationID = receivedMessage.getJMSCorrelationID();
        Destination destination = receivedMessage.getJMSReplyTo();
        String destinationStr = null;
        if (destination != null) {
            if (destination instanceof Queue) {
                destinationStr = ((Queue) destination).getQueueName();
            }
        }

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
        return new String[]{correlationID, textMessage, destinationStr};
    }

    private void receiveEnvelops(List<MovementCreateData> mcdList) throws JMSException, JAXBException {
//        auditController.info(MovementCreate, "Messages receiving");
        MQQueueConnectionFactory cf = configMQCF();

        MQQueueConnection connection = (MQQueueConnection) cf.createQueueConnection(queueProperties.mqUser, queueProperties.mqPassword);
        MQQueueSession session = (MQQueueSession) connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        MQQueue queue = (MQQueue) session.createQueue("queue:///" + queueProperties.mqQueueInc);
        MQQueueReceiver receiver = (MQQueueReceiver) session.createReceiver(queue);

        connection.start();

        int completed = 0;
        Long att = propertiesRepository.getNumberDef(PROP_ATTEMPTS, ATTEMPTS);
        for (int i = 0; i < att && completed < mcdList.size(); i++) {
            while (true) {
                int ret = receiveMessage(receiver, mcdList);
                if (ret == 2) {
                    completed++;
                    if (completed >= mcdList.size()) {
                        break;
                    }
                } else if (ret == 3) {
                    break;
                }
            }

            try {
                Thread.sleep(PERIOD);
            } catch (InterruptedException e) {
                log.info("", e);
            }
        }

//        auditController.info(MovementCreate, "Parsed. completed:" + completed);
        if (responseStorage.size() > 0) {
            auditController.info(MovementCreate, "Storage (on exit):" + Arrays.toString(responseStorage.keySet().toArray()));
        }

        for (MovementCreateData item : mcdList) {
            if (item.getState() == null) {
//                auditController.info(MovementCreate, "Timeout. set state. id:" + item.getMessageUUID());
                item.setState(MovementCreateData.StateEnum.ERROR);
                item.setErrType(MovementErrorTypes.ERR_TIMEOUT);
                item.setErrDescr("");
            }
        }

        receiver.close();
        session.close();
        connection.close();
    }

    private Map<String, String> readFromXML(String bodyXML/*, Long jId*/) throws Exception {
        org.w3c.dom.Document doc = null;
        try {
            DocumentBuilder b = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc = b.parse(new ByteArrayInputStream(bodyXML.getBytes("UTF8")));
            if (doc == null) {
                //Ошибка XML
//                journalRepository.updateLogStatus(jId, ERROR, "Ошибка при преобразовании входящего XML");
                return null;
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
//            journalRepository.updateLogStatus(jId, ERROR, "Ошибка при преобразовании входящего XML\n" + e.getMessage());
            throw e;
        }

        Map<String, String> answerMap = new HashMap<>();

        NodeList nodes = null;
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            nodes = (NodeList) xPath.evaluate("/ExtendedStatus/StatusDetails", doc.getDocumentElement(), XPathConstants.NODESET);
            if (nodes == null || nodes.getLength() != 1) {
                //Ошибка XML
//                journalRepository.updateLogStatus(jId, ERROR, "Отсутствуют неоходимые данные /AccountList/AccountDetails");
                return null;
            }
        } catch (XPathExpressionException e) {
//            journalRepository.updateLogStatus(jId, ERROR, "Ошибка при чтении входящего XML\n" + e.getMessage());
            throw e;
        }

        NodeList nodes2 = null;
        try {
            nodes2 = (NodeList) xPath.evaluate("/ExtendedStatus/StatusExtension/SCASAMovementExtension/Movement/BlockReference/BlockID", doc.getDocumentElement(), XPathConstants.NODESET);
            if (nodes2 == null || nodes2.getLength() == 0 || nodes2.item(0) == null) {
                //Ошибка XML
//                journalRepository.updateLogStatus(jId, ERROR, "Отсутствуют неоходимые данные /AccountList/AccountDetails");
//                return null;
            } else {
                int i = 0;
                String name = nodes2.item(i).getNodeName();
                String value = nodes2.item(i).getTextContent();
                if (name.contains(":")) {
                    name = name.substring(name.indexOf(":") + 1);
                }
                answerMap.put(name, value);
            }
        } catch (XPathExpressionException e) {
//            journalRepository.updateLogStatus(jId, ERROR, "Ошибка при чтении входящего XML\n" + e.getMessage());
            throw e;
        }

        for (int i = 0; i < nodes.getLength(); i++) {
            for (int j = 0; j < nodes.item(i).getChildNodes().getLength(); j++) {
                String name = nodes.item(i).getChildNodes().item(j).getNodeName();
                String value = nodes.item(i).getChildNodes().item(j).getTextContent();
                if (name.contains(":")) {
                    name = name.substring(name.indexOf(":") + 1);
                }
                answerMap.put(name, value);
            }
        }

        return answerMap;
    }

    // Message queue
    private int receiveMessage(MQQueueReceiver receiver, List<MovementCreateData> mcdList) throws JMSException {
        String[] messageParts = readFromJMS(receiver);
        if (!isEmpty(messageParts[1])) {
            //Парсим
            // по id ищем item
            // проставляем результат
            // Body, XRef, Error

            String[] envelopeParts = getInfoFromSOAPEnvelope(messageParts[1]);

            String messageID = isEmpty(messageParts[0]) ? envelopeParts[1] : messageParts[0];

            auditController.stat(MovementCreate, "Service answer", messageParts[1], messageID);

            // Есть ID по которому можно найти запрос?
            if (isEmpty(messageID)) {
                // не можем найти запрос для данного ответа
                auditController.stat(MovementCreate, "messageID is null", messageParts[1], messageID);
                return 1;//continue;
            }

            // Ждём ли мы ответ для полученного messageID
            MovementCreateData item = null;
            for (MovementCreateData mcd : mcdList) {
                if (messageID.equals(mcd.getMessageUUID())) {
                    item = mcd;
                    break;
                }
            }


            // item == null -> не можем найти запрос для данного ответа
            if (item != null) {
                //
//                auditController.stat(MovementCreate, "Parse directly", messageParts[1], messageID);
                parseResponse(envelopeParts[0], item);
                return 2;
            } else {
                // Записать в storage
                if (!messageID.startsWith("ID:")) {
                    responseStorage.put(messageID, messageParts);
//                    auditController.stat(MovementCreate, "Save to storage", Arrays.toString(responseStorage.entrySet().toArray()));
                } else {
                    auditController.stat(MovementCreate, "Nodata message received", messageParts[1]);
                }
            }
        } else {
            // Проверить storage
            MovementCreateData item = null;
            for (MovementCreateData mcd : mcdList) {
                messageParts = responseStorage.get(mcd.getMessageUUID());
                if (messageParts != null) {
                    item = mcd;
                    String[] envelopeParts = getInfoFromSOAPEnvelope(messageParts[1]);
                    parseResponse(envelopeParts[0], item);
                    responseStorage.remove(mcd.getMessageUUID());
                    auditController.stat(MovementCreate, "Service answer from Storage", messageParts[1], mcd.getMessageUUID());
                    return 2;
                }
            }
        }
        return 3;//break;
    }

    private void parseResponse(String envelopePart0, MovementCreateData item) {
        try {
//                    String fault = envelopeParts[0].replaceAll(".*<([^/]*Fault)>(.*?)</\\1>.*", "$2");
            if (envelopePart0.matches(".*<([^/]*Fault)>(.*?)</\\1>.*")) {
//                auditController.stat(MovementCreate, "fault detected", envelopePart0, item.getMessageUUID());
                fillErrorDataFromXML(envelopePart0, item);
            } else {
                //envelopeParts[0] = envelopeParts[0].replaceAll("(<[^/]*ExtendedStatus)>", "$1 xmlns:gbo=\"urn:ucbru:gbo:v4\">");

                Map<String, String> data = readFromXML(envelopePart0);
//                auditController.stat(MovementCreate, "nonfault detected. date:" + data, envelopePart0, item.getMessageUUID());
                if (data != null && data.keySet().size() > 0) {
                    if (data.get("Status") != null) {
//                        auditController.stat(MovementCreate, "setState:" + data.get("Status"), envelopePart0, item.getMessageUUID());
                        item.setState(MovementCreateData.StateEnum.valueOf(data.get("Status")));
                    }
                    if (data.get("BlockID") != null) {
                        item.setBlockId(data.get("BlockID"));
                    }
                    if (data.get("Description") != null) {
                        item.setErrDescr(data.get("Description"));
                    }
                }
            }
        } catch (Exception e) {
            log.info("", e);
            item.setState(MovementCreateData.StateEnum.ERROR);
            item.setErrType(MovementErrorTypes.ERR_REQUEST);
            // Ошибка парсинга ответа
            throw new DefaultApplicationException(e);
        }
    }

    private void fillErrorDataFromXML(String bodyXML, MovementCreateData item) throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        org.w3c.dom.Document doc = null;

        item.setState(MovementCreateData.StateEnum.ERROR);
        item.setErrType(MovementErrorTypes.ERR_BUSINESS);

        DocumentBuilder b = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        doc = b.parse(new ByteArrayInputStream(bodyXML.getBytes("UTF8")));
        if (doc == null) {
            //Ошибка XML
//                journalRepository.updateLogStatus(jId, ERROR, "Ошибка при преобразовании входящего XML");
            return;
        }


        XPath xPath = XPathFactory.newInstance().newXPath();

        String description = (String) xPath.evaluate("/Fault/detail/ExtendedError/ErrorDetails/Description", doc.getDocumentElement(), XPathConstants.STRING);
        String source = (String) xPath.evaluate("/Fault/detail/ExtendedError/ErrorDetails/Source", doc.getDocumentElement(), XPathConstants.STRING);
        String kind = (String) xPath.evaluate("/Fault/detail/ExtendedError/ErrorDetails/Kind", doc.getDocumentElement(), XPathConstants.STRING);

        if (!isEmpty(description) && !isEmpty(source)) {
            item.setErrDescr(source + ": " + description);
            if("SYSERR".equals(kind)){
                item.setErrType(MovementErrorTypes.ERR_SERVICE);
            }
        }

        NodeList movementList = (NodeList) xPath.evaluate("/Fault/detail/ExtendedError/ErrorExtension/SCASAMovementExtension/Movement", doc.getDocumentElement(), XPathConstants.NODESET);
        for (int i = 0; i < movementList.getLength(); i++) {
            Node mvment = movementList.item(i);
            String abs = (String) xPath.evaluate("./ABS", mvment, XPathConstants.STRING);
            source = (String) xPath.evaluate("./ErrorCode", mvment, XPathConstants.STRING);
            description = (String) xPath.evaluate("./ErrorDescription", mvment, XPathConstants.STRING);
            String message = ((!isEmpty(abs) ? abs + ": " : "") + (!isEmpty(source) ? source + ": " : "") + (!isEmpty(description) ? description : "")).replaceAll("\\s+"," ");
            item.setErrDescr(isEmpty(item.getErrDescr())?message:item.getErrDescr()+" \n"+message);
        }

    }

    private String[] getInfoFromSOAPEnvelope(String envelope) {
        String errorMessage = "";
        envelope = envelope.replace("\n", "").replace("\r", "").replace("\t", "");
        String body = ifEmpty(envelope.replaceAll(".*<(.*[B|b]ody)>(.*)</\\1>.*", "$2"), "").trim();
        if (isEmpty(body)) {
            // Записать ошибку
            errorMessage += "Неправильный формат сообщения. Раздел <body> отсутствует.";
        }

        String xref = ifEmpty(envelope.replaceAll(".*<(.*XRef)>(.*)</\\1>.*", "$2"), "").trim();
        if (isEmpty(xref) || xref.contains("<")) {
            // Записать ошибку
            xref = "";
            errorMessage += "Неправильный формат сообщения. Раздел <body> отсутствует.";

        }

        return new String[]{body, xref, errorMessage};
    }

    private void createOneEnvelope(MovementCreateData item) {
        try {
            String body = fillMovementData(item);
            item.setMessageUUID(ifEmpty(item.getOperIdD(), "") + "." + ifEmpty(item.getOperIdC(), "")); //+"."+UUID.randomUUID().toString().substring(0,6));
            item.setEnvelopOutcoming(createSOAPEnvelop(body, item.getMessageUUID()));
        } catch (JAXBException e) {
            log.info("", e);
            auditController.error(MovementCreate, "Ошибка при создании конверта", null, "");
        }
    }

    private void sendEnvelops(List<MovementCreateData> envelopes) throws JMSException, UnsupportedEncodingException {
        MQQueueConnectionFactory cf = configMQCF();

        MQQueueConnection connection = (MQQueueConnection) cf.createQueueConnection(queueProperties.mqUser, queueProperties.mqPassword);
        MQQueueSession session = (MQQueueSession) connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        MQQueue queue = (MQQueue) session.createQueue("queue:///" + queueProperties.mqQueueOut);//UCBRU.ADP.BARSGL.V4.ACDENO.FCC.NOTIF
        MQQueue queueReplyTo = (MQQueue) session.createQueue("queue:///" + queueProperties.mqQueueInc);
        MQQueueSender sender = (MQQueueSender) session.createSender(queue);

        connection.start();

        for (MovementCreateData oneEnvelope : envelopes) {
            JMSTextMessage message = (JMSTextMessage) session.createTextMessage();
            message.setText(oneEnvelope.getEnvelopOutcoming());
            message.setJMSCorrelationID(oneEnvelope.getMessageUUID());
            message.setJMSReplyTo(queueReplyTo);
            sender.send(message);
            auditController.stat(MovementCreate, "Message sent", oneEnvelope.getEnvelopOutcoming(), oneEnvelope.getMessageUUID());
        }

        System.out.println("Messages sent");
        sender.close();
        session.close();
        connection.close();
    }

    private MQQueueConnectionFactory configMQCF() throws JMSException {
        MQQueueConnectionFactory cf = new MQQueueConnectionFactory();
        cf.setHostName(queueProperties.mqHost);
        cf.setPort(Integer.parseInt(queueProperties.mqPortStr));
        cf.setTransportType(WMQConstants.WMQ_CM_CLIENT);
        cf.setQueueManager(queueProperties.mqQueueManager);
        cf.setChannel(queueProperties.mqChannel);
        return cf;
    }

    private String createSOAPEnvelop(String answerBody, String xRef) throws JAXBException {
        String headerStr =
            "\t\t<m:UCBRUHeaders xmlns:m=\"urn:ucbru:gbo:v4\">\n" +
                "\t\t\t<m:Correlation>\n" +
                "\t\t\t\t<m:XRef>" + xRef + "</m:XRef>\n" +
                "\t\t\t</m:Correlation>\n" +
                "\t\t\t<m:Audit>\n" +
                "\t\t\t\t<m:ProcessInfo>\n" +
                "\t\t\t\t\t<m:Name>BARSGL</m:Name>\n" +
                "\t\t\t\t\t<m:InstanceId>" + xRef + "</m:InstanceId>\n" +
                "\t\t\t\t</m:ProcessInfo>\n" +
                "\t\t\t</m:Audit>\n" +
                "\t\t\t\t<m:Composite>\n" +
                "\t\t\t\t\t<m:Orchestration>SERVICE_DR_FIRST_OPTIMISTIC</m:Orchestration>\n" +
                "\t\t\t\t</m:Composite>\n" +
                "\t\t</m:UCBRUHeaders>\n";

        String xmlHead = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
//        headerStr = headerStr.length() > xmlHead.length() ? headerStr.substring(xmlHead.length()) : headerStr;
        answerBody = !isEmpty(answerBody) && answerBody.length() > xmlHead.length() ? answerBody.substring(xmlHead.length()) : answerBody;
        String envelopStr = envelopTemplates[0] + headerStr + envelopTemplates[1] + answerBody + envelopTemplates[2];
        envelopStr = envelopStr.replaceAll(">null<", "><");
        return envelopStr;
    }

    private String fillMovementData(MovementCreateData item) {
        StringBuilder sb = new StringBuilder();
        int movementNum = 0;

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<ns2:SCASAMovementCreate xmlns:ns2=\"urn:ucbru:gbo:v4:casa\" xmlns=\"urn:ucbru:gbo:v4\">");

        if (!isEmpty(item.getOperIdD())) {
            sb.append("<ns2:Movement>");
            sb.append("<ns2:RequestNumber>" + (++movementNum) + "</ns2:RequestNumber>");//todo
            sb.append("<ns2:MovementReference>");
            sb.append("<ns2:SystemCode>" + SYSTEM_CODE + "</ns2:SystemCode>");
            sb.append("<ns2:MovementID>").append(item.getOperIdD()).append("</ns2:MovementID>");
            sb.append("</ns2:MovementReference>");
            sb.append("<ns2:CBAccount>").append(item.getAccountCBD()).append("</ns2:CBAccount>");
            sb.append("<ns2:MovementAmount>").append(item.getOperAmountD()).append("</ns2:MovementAmount>");
            createMovementCommonPart(item, sb, "D");
            sb.append("</ns2:Movement>");
        }

        if (!isEmpty(item.getOperIdC())) {
            sb.append("<ns2:Movement>");
            sb.append("<ns2:RequestNumber>" + (++movementNum) + "</ns2:RequestNumber>");//todo
            sb.append("<ns2:MovementReference>");
            sb.append("<ns2:SystemCode>" + SYSTEM_CODE + "</ns2:SystemCode>");
            sb.append("<ns2:MovementID>").append(item.getOperIdC()).append("</ns2:MovementID>");
            sb.append("</ns2:MovementReference>");
            sb.append("<ns2:CBAccount>").append(item.getAccountCBC()).append("</ns2:CBAccount>");
            sb.append("<ns2:MovementAmount>").append(item.getOperAmountC()).append("</ns2:MovementAmount>");
            createMovementCommonPart(item, sb, "C");
            sb.append("</ns2:Movement>");
        }

        sb.append("</ns2:SCASAMovementCreate>");
        return sb.toString().replaceAll("&","&amp;");
    }

    private void createMovementCommonPart(MovementCreateData item, StringBuilder sb, String drcr) {
        sb.append("<ns2:ObjectReference>").append(ifEmpty(item.getDealId(), "")).append("</ns2:ObjectReference>");
        sb.append("<ns2:ExtModule>").append(ifEmpty(item.getPstSource(), "")).append("</ns2:ExtModule>");
        sb.append("<ns2:ExtOperationCode>" + EXT_OPERATION_CODE + "</ns2:ExtOperationCode>");
        sb.append("<ns2:Priority>" + PRIORITY + "</ns2:Priority>");
        sb.append("<ns2:OperationTimestamp>").append(dateToXML(item.getOperCreate())).append("</ns2:OperationTimestamp>");
        sb.append("<ns2:DrCr>" + drcr + "</ns2:DrCr>");
        sb.append("<ns2:Narrative>").append(ifEmpty(item.getDestinationR(), "")).append("</ns2:Narrative>");
        sb.append("<ns2:UseOverdraft>true</ns2:UseOverdraft>");
        sb.append("<ns2:IgnoreBalance>false</ns2:IgnoreBalance>");
        sb.append("<ns2:IgnoreBlockFlag>false</ns2:IgnoreBlockFlag>");
        sb.append("<ns2:ValueDate>").append(sdfDate.format(item.getPstDate())).append("</ns2:ValueDate>");
        sb.append("<ns2:Storno>").append(item.getCorrectionPst() == null ? false : item.getCorrectionPst()).append("</ns2:Storno>");

        sb.append("<ns2:ABSSpecificParameters>");
        sb.append("<ns2:MIDASSpecificParameters>");
        sb.append("<ns2:SPOS>").append(ifEmpty(item.getPstSource(), "")).append("</ns2:SPOS>");
        sb.append("<ns2:OTRF>").append(ifEmpty(item.getDealId(), "")).append("</ns2:OTRF>");
        sb.append("<ns2:Department>").append(ifEmpty(item.getDeptId(), "")).append("</ns2:Department>");
        sb.append("<ns2:ProfitCenter>").append(ifEmpty(item.getProfitCenter(), "")).append("</ns2:ProfitCenter>");
        sb.append("<ns2:BookCode/>");
        sb.append("<ns2:PostingTrnTyp/>");
        sb.append("<ns2:PostingNarrative>").append(ifEmpty(item.getPnar(), "")).append("</ns2:PostingNarrative>");
//        sb.append("<ns2:ProjectTrnTyp/>");
        sb.append("<ns2:ProjectTrnTyp>GL</ns2:ProjectTrnTyp>");
        sb.append("<ns2:ProjectTrnNbr/>");
        sb.append("<ns2:ProjectNarrative>").append(ifEmpty(item.getPnar(), "")).append("</ns2:ProjectNarrative>");
        sb.append("</ns2:MIDASSpecificParameters>");
        sb.append("</ns2:ABSSpecificParameters>");
    }

    public static void main(String[] args) {
        MovementCreateProcessor processor = new MovementCreateProcessor();
        MovementCreateData data = processor.fillTestData();
        List<MovementCreateData> datas = new ArrayList<>();
        datas.add(data);
        processor.process(datas);
    }

    public MovementCreateData fillTestData() {
        MovementCreateData data = new MovementCreateData();
        try {
            data.setOperIdD("789003");
            data.setAccountCBD("40817036000013911516");
            data.setOperAmountD(new BigDecimal("777.77"));
//            data.setOperIdC("789004");
//            data.setAccountCBC("40702810700010003897");
//            data.setOperAmountC(new BigDecimal("-777.77"));
            data.setDealId("88000");
            data.setPstSource("PHub1");
            data.setOperCreate(sdfDate.parse("2016-03-02"));
            data.setDirection("D");
            data.setDestinationR("test");
            data.setPstDate(sdfDate.parse("2016-03-02"));
            data.setPnar("PHUB");
            data.setDeptId("ITD");
            data.setProfitCenter("Test");

        } catch (ParseException e) {
            log.info("", e);
        }

        return data;
    }

    private String errorExampleW =
        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:gbo4=\"urn:ucbru:gbo:v4\">\n" +
            "    <soapenv:Header>\n" +
            "        <m:UCBRUHeaders xmlns:m=\"urn:ucbru:gbo:v4\">\n" +
            "            <m:Correlation>\n" +
            "                <m:XRef>789003</m:XRef>\n" +
            "            </m:Correlation>\n" +
            "            <m:Audit>\n" +
            "                <m:MessagePath>\n" +
            "                    <m:Step>\n" +
            "                        <m:Application.Module>SCASAMovementCreate.RequestHandler</m:Application.Module>\n" +
            "                        <m:VersionId>v4</m:VersionId>\n" +
            "                        <m:TimeStamp>2016-09-24T12:47:22.902+03:00</m:TimeStamp>\n" +
            "                        <m:RoutingRole>START</m:RoutingRole>\n" +
            "                        <m:Comment/>\n" +
            "                    </m:Step>\n" +
            "                    <m:Step>\n" +
            "                        <m:Application.Module>SRVACC.MaAcPoBaQu.ESBDB_RequestHandler</m:Application.Module>\n" +
            "                        <m:VersionId>v4</m:VersionId>\n" +
            "                        <m:TimeStamp>2016-09-24T12:47:22.907+03:00</m:TimeStamp>\n" +
            "                        <m:RoutingRole>START</m:RoutingRole>\n" +
            "                        <m:Comment>Start of message processing</m:Comment>\n" +
            "                    </m:Step>\n" +
            "                    <m:Step>\n" +
            "                        <m:Application.Module>SRVACC.MaAcPoBaQu.RequestHandler</m:Application.Module>\n" +
            "                        <m:VersionId>v4</m:VersionId>\n" +
            "                        <m:TimeStamp>2016-09-24T12:47:22.912+03:00</m:TimeStamp>\n" +
            "                        <m:RoutingRole>START</m:RoutingRole>\n" +
            "                        <m:Comment>Start of message processing</m:Comment>\n" +
            "                    </m:Step>\n" +
            "                    <m:Step>\n" +
            "                        <m:Application.Module>SRVACC.MaAcPoBaQu.RequestHandler</m:Application.Module>\n" +
            "                        <m:VersionId>v4</m:VersionId>\n" +
            "                        <m:TimeStamp>2016-09-24T12:47:22.913+03:00</m:TimeStamp>\n" +
            "                        <m:RoutingRole>ROUTE</m:RoutingRole>\n" +
            "                        <m:Comment>Routing by format accounts</m:Comment>\n" +
            "                    </m:Step>\n" +
            "                    <m:Step>\n" +
            "                        <m:Application.Module>SRVACC.MaAcPoBaQu.ABS1_ResponseHandler</m:Application.Module>\n" +
            "                        <m:VersionId>v4</m:VersionId>\n" +
            "                        <m:TimeStamp>2016-09-24T12:47:23.037+03:00</m:TimeStamp>\n" +
            "                        <m:RoutingRole>ROUTE</m:RoutingRole>\n" +
            "                        <m:Comment>Routing by cb accounts</m:Comment>\n" +
            "                    </m:Step>\n" +
            "                    <m:Step>\n" +
            "                        <m:Application.Module>SrvName.QueryName</m:Application.Module>\n" +
            "                        <m:VersionId>v4</m:VersionId>\n" +
            "                        <m:TimeStamp>2016-09-24T12:47:26.735+03:00</m:TimeStamp>\n" +
            "                        <m:RoutingRole>ROUTE</m:RoutingRole>\n" +
            "                        <m:Comment>Routing barsgl accounts</m:Comment>\n" +
            "                    </m:Step>\n" +
            "                    <m:Step>\n" +
            "                        <m:Application.Module>SRVACC.MaAcPoBaQu.ABS3_ResponseHandler</m:Application.Module>\n" +
            "                        <m:VersionId>v4</m:VersionId>\n" +
            "                        <m:TimeStamp>2016-09-24T12:47:27.186+03:00</m:TimeStamp>\n" +
            "                        <m:RoutingRole>SUCCESS</m:RoutingRole>\n" +
            "                        <m:Comment>Finish of message processing</m:Comment>\n" +
            "                    </m:Step>\n" +
            "                    <m:Step>\n" +
            "                        <m:Application.Module>SRVACC.MaAcPoBaQu.ESBDB_ResponseHandler</m:Application.Module>\n" +
            "                        <m:VersionId>v4</m:VersionId>\n" +
            "                        <m:TimeStamp>2016-09-24T12:47:27.206+03:00</m:TimeStamp>\n" +
            "                        <m:RoutingRole>SUCCESS</m:RoutingRole>\n" +
            "                        <m:Comment>Finish of message processing</m:Comment>\n" +
            "                    </m:Step>\n" +
            "                    <m:Step>\n" +
            "                        <m:Application.Module>SCASAMovementCreate.MaAcPoResponseHandler</m:Application.Module>\n" +
            "                        <m:VersionId>v4</m:VersionId>\n" +
            "                        <m:TimeStamp>2016-09-24T12:47:27.327+03:00</m:TimeStamp>\n" +
            "                        <m:RoutingRole>ROUTE</m:RoutingRole>\n" +
            "                        <m:Comment>Route to CoordinatorHandler</m:Comment>\n" +
            "                    </m:Step>\n" +
            "                    <m:Step>\n" +
            "                        <m:Application.Module>SCASAMovementCreate.CoordinatorHandler</m:Application.Module>\n" +
            "                        <m:VersionId>v4</m:VersionId>\n" +
            "                        <m:TimeStamp>2016-09-24T12:47:27.335+03:00</m:TimeStamp>\n" +
            "                        <m:RoutingRole>ROUTE</m:RoutingRole>\n" +
            "                        <m:Comment>Route to ResponseHandler</m:Comment>\n" +
            "                    </m:Step>\n" +
            "                    <m:Step>\n" +
            "                        <m:Application.Module>SCASAMovementCreate.ResponseHandler</m:Application.Module>\n" +
            "                        <m:VersionId>v4</m:VersionId>\n" +
            "                        <m:TimeStamp>2016-09-24T12:47:27.341+03:00</m:TimeStamp>\n" +
            "                        <m:RoutingRole>SUCCESS</m:RoutingRole>\n" +
            "                        <m:Comment></m:Comment>\n" +
            "                    </m:Step>\n" +
            "                </m:MessagePath>\n" +
            "                <m:ProcessInfo>\n" +
            "                    <m:Name>BARSGL</m:Name>\n" +
            "                    <m:InstanceId>693201</m:InstanceId>\n" +
            "                </m:ProcessInfo>\n" +
            "            </m:Audit>\n" +
            "            <m:Tools>\n" +
            "                <m:Environment/>\n" +
            "            </m:Tools>\n" +
            "            <m:Composite>\n" +
            "                <m:Orchestration>SERVICE_DR_FIRST_OPTIMISTIC</m:Orchestration>\n" +
            "            </m:Composite>\n" +
            "        </m:UCBRUHeaders>\n" +
            "    </soapenv:Header>\n" +
            "    <soapenv:Body>\n" +
            "        <gbo4:ExtendedStatus>\n" +
            "            <gbo4:StatusDetails>\n" +
            "                <gbo4:Status>WARNING</gbo4:Status>\n" +
            "                <gbo4:Description>Обработка запроса прошла успешно</gbo4:Description>\n" +
            "                <gbo4:Source>SCASA</gbo4:Source>\n" +
            "                <gbo4:Kind>WARNING</gbo4:Kind>\n" +
            "                <gbo4:DateTime>2016-09-24T12:47:27.340666</gbo4:DateTime>\n" +
            "            </gbo4:StatusDetails>\n" +
            "            <gbo4:StatusExtension>\n" +
            "                <NS1:SCASAMovementExtension xmlns:NS1=\"urn:ucbru:gbo:v4:casa\">\n" +
            "                    <NS1:Movement>\n" +
            "                        <NS1:MovementReference>\n" +
            "                            <NS1:SystemCode>BARSGL</NS1:SystemCode>\n" +
            "                            <NS1:MovementID>693201</NS1:MovementID>\n" +
            "                        </NS1:MovementReference>\n" +
            "                        <NS1:MovementAmount>4562.000</NS1:MovementAmount>\n" +
            "                    </NS1:Movement>\n" +
            "                    <NS1:Movement>\n" +
            "                        <NS1:MovementReference>\n" +
            "                            <NS1:SystemCode>BARSGL</NS1:SystemCode>\n" +
            "                            <NS1:MovementID>693202</NS1:MovementID>\n" +
            "                        </NS1:MovementReference>\n" +
            "                        <NS1:MovementAmount>4562.000</NS1:MovementAmount>\n" +
            "                    </NS1:Movement>\n" +
            "                </NS1:SCASAMovementExtension>\n" +
            "            </gbo4:StatusExtension>\n" +
            "        </gbo4:ExtendedStatus>\n" +
            "    </soapenv:Body>\n" +
            "</soapenv:Envelope>";

    private String errorExample2 =
        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:gbo4=\"urn:ucbru:gbo:v4\">\n" +
            "    <soapenv:Body>\n" +
            "        <soapenv:Fault>\n" +
            "            <faultcode>soapenv:Server</faultcode>\n" +
            "            <faultstring>[FCC]:Ошибка при обработке в FCC;</faultstring>\n" +
            "            <faultactor>FCC;</faultactor>\n" +
            "            <detail>\n" +
            "                <gbo4:ExtendedError>\n" +
            "                    <gbo4:ErrorDetails>\n" +
            "                        <gbo4:Code>ERROR</gbo4:Code>\n" +
            "                        <gbo4:Description>[FCC]:Ошибка при обработке в FCC;</gbo4:Description>\n" +
            "                        <gbo4:Source>FCC;</gbo4:Source>\n" +
            "                        <gbo4:Kind>ERROR</gbo4:Kind>\n" +
            "                        <gbo4:DateTime>2016-09-20T16:42:37.226148</gbo4:DateTime>\n" +
            "                    </gbo4:ErrorDetails>\n" +
            "                    <gbo4:ErrorExtension>\n" +
            "                        <m:UCBRUHeaders xmlns:m=\"urn:ucbru:gbo:v4\">\n" +
            "                            <m:Correlation>\n" +
            "                                <m:XRef>789003</m:XRef>\n" +
            "                            </m:Correlation>\n" +
            "                            <m:Audit>\n" +
            "                                <m:MessagePath>\n" +
            "                                    <m:Step>\n" +
            "                                        <m:Application.Module>SCASAMovementCreate.RequestHandler</m:Application.Module>\n" +
            "                                        <m:VersionId>v4</m:VersionId>\n" +
            "                                        <m:TimeStamp>2016-09-20T16:42:36.475+03:00</m:TimeStamp>\n" +
            "                                        <m:RoutingRole>START</m:RoutingRole>\n" +
            "                                        <m:Comment/>\n" +
            "                                    </m:Step>\n" +
            "                                    <m:Step>\n" +
            "                                        <m:Application.Module>SRVACC.MaAcPoBaQu.ESBDB_RequestHandler\n" +
            "                                        </m:Application.Module>\n" +
            "                                        <m:VersionId>v4</m:VersionId>\n" +
            "                                        <m:TimeStamp>2016-09-20T16:42:36.480+03:00</m:TimeStamp>\n" +
            "                                        <m:RoutingRole>START</m:RoutingRole>\n" +
            "                                        <m:Comment>Start of message processing</m:Comment>\n" +
            "                                    </m:Step>\n" +
            "                                    <m:Step>\n" +
            "                                        <m:Application.Module>SRVACC.MaAcPoBaQu.RequestHandler</m:Application.Module>\n" +
            "                                        <m:VersionId>v4</m:VersionId>\n" +
            "                                        <m:TimeStamp>2016-09-20T16:42:36.484+03:00</m:TimeStamp>\n" +
            "                                        <m:RoutingRole>START</m:RoutingRole>\n" +
            "                                        <m:Comment>Start of message processing</m:Comment>\n" +
            "                                    </m:Step>\n" +
            "                                    <m:Step>\n" +
            "                                        <m:Application.Module>SRVACC.MaAcPoBaQu.RequestHandler</m:Application.Module>\n" +
            "                                        <m:VersionId>v4</m:VersionId>\n" +
            "                                        <m:TimeStamp>2016-09-20T16:42:36.486+03:00</m:TimeStamp>\n" +
            "                                        <m:RoutingRole>ROUTE</m:RoutingRole>\n" +
            "                                        <m:Comment>Routing by format accounts</m:Comment>\n" +
            "                                    </m:Step>\n" +
            "                                    <m:Step>\n" +
            "                                        <m:Application.Module>SRVACC.MaAcPoBaQu.ABS1_ResponseHandler\n" +
            "                                        </m:Application.Module>\n" +
            "                                        <m:VersionId>v4</m:VersionId>\n" +
            "                                        <m:TimeStamp>2016-09-20T16:42:36.491+03:00</m:TimeStamp>\n" +
            "                                        <m:RoutingRole>ROUTE</m:RoutingRole>\n" +
            "                                        <m:Comment>Routing by cb accounts</m:Comment>\n" +
            "                                    </m:Step>\n" +
            "                                    <m:Step>\n" +
            "                                        <m:Application.Module>SrvName.QueryName</m:Application.Module>\n" +
            "                                        <m:VersionId>v4</m:VersionId>\n" +
            "                                        <m:TimeStamp>2016-09-20T16:42:36.579+03:00</m:TimeStamp>\n" +
            "                                        <m:RoutingRole>ROUTE</m:RoutingRole>\n" +
            "                                        <m:Comment>Routing barsgl accounts</m:Comment>\n" +
            "                                    </m:Step>\n" +
            "                                    <m:Step>\n" +
            "                                        <m:Application.Module>SRVACC.MaAcPoBaQu.ABS3_ResponseHandler\n" +
            "                                        </m:Application.Module>\n" +
            "                                        <m:VersionId>v4</m:VersionId>\n" +
            "                                        <m:TimeStamp>2016-09-20T16:42:36.594+03:00</m:TimeStamp>\n" +
            "                                        <m:RoutingRole>SUCCESS</m:RoutingRole>\n" +
            "                                        <m:Comment>Finish of message processing</m:Comment>\n" +
            "                                    </m:Step>\n" +
            "                                    <m:Step>\n" +
            "                                        <m:Application.Module>SRVACC.MaAcPoBaQu.ESBDB_ResponseHandler\n" +
            "                                        </m:Application.Module>\n" +
            "                                        <m:VersionId>v4</m:VersionId>\n" +
            "                                        <m:TimeStamp>2016-09-20T16:42:36.599+03:00</m:TimeStamp>\n" +
            "                                        <m:RoutingRole>SUCCESS</m:RoutingRole>\n" +
            "                                        <m:Comment>Finish of message processing</m:Comment>\n" +
            "                                    </m:Step>\n" +
            "                                    <m:Step>\n" +
            "                                        <m:Application.Module>SCASAMovementCreate.MaAcPoResponseHandler\n" +
            "                                        </m:Application.Module>\n" +
            "                                        <m:VersionId>v4</m:VersionId>\n" +
            "                                        <m:TimeStamp>2016-09-20T16:42:36.730+03:00</m:TimeStamp>\n" +
            "                                        <m:RoutingRole>ROUTE</m:RoutingRole>\n" +
            "                                        <m:Comment>Route to CoordinatorHandler</m:Comment>\n" +
            "                                    </m:Step>\n" +
            "                                    <m:Step>\n" +
            "                                        <m:Application.Module>SCASAMovementCreate.CoordinatorHandler\n" +
            "                                        </m:Application.Module>\n" +
            "                                        <m:VersionId>v4</m:VersionId>\n" +
            "                                        <m:TimeStamp>2016-09-20T16:42:36.736+03:00</m:TimeStamp>\n" +
            "                                        <m:RoutingRole>ROUTE</m:RoutingRole>\n" +
            "                                        <m:Comment>Route to Send2ABS</m:Comment>\n" +
            "                                    </m:Step>\n" +
            "                                    <m:Step>\n" +
            "                                        <m:Application.Module>SCASAMovementCreate.Send2ABS_Request\n" +
            "                                        </m:Application.Module>\n" +
            "                                        <m:VersionId>v4</m:VersionId>\n" +
            "                                        <m:TimeStamp>2016-09-20T16:42:36.740+03:00</m:TimeStamp>\n" +
            "                                        <m:RoutingRole>ROUTE</m:RoutingRole>\n" +
            "                                        <m:Comment/>\n" +
            "                                    </m:Step>\n" +
            "                                    <m:Step>\n" +
            "                                        <m:Application.Module>AFCCMovementCreate.RequestHandler</m:Application.Module>\n" +
            "                                        <m:VersionId>v4</m:VersionId>\n" +
            "                                        <m:TimeStamp>2016-09-20T16:42:36.746+03:00</m:TimeStamp>\n" +
            "                                        <m:RoutingRole>START</m:RoutingRole>\n" +
            "                                        <m:Comment/>\n" +
            "                                    </m:Step>\n" +
            "                                    <m:Step>\n" +
            "                                        <m:Application.Module>AFCCMovementCreate.ResponseHandler</m:Application.Module>\n" +
            "                                        <m:VersionId>v4</m:VersionId>\n" +
            "                                        <m:TimeStamp>2016-09-20T16:42:37.194+03:00</m:TimeStamp>\n" +
            "                                        <m:RoutingRole>ERROR</m:RoutingRole>\n" +
            "                                        <m:Comment>Error: Code=2951; Description=Ошибка при обработке в FCC</m:Comment>\n" +
            "                                    </m:Step>\n" +
            "                                    <m:Step>\n" +
            "                                        <m:Application.Module>SCASAMovementCreate.Send2ABS_Response\n" +
            "                                        </m:Application.Module>\n" +
            "                                        <m:VersionId>v4</m:VersionId>\n" +
            "                                        <m:TimeStamp>2016-09-20T16:42:37.214+03:00</m:TimeStamp>\n" +
            "                                        <m:RoutingRole>ROUTE</m:RoutingRole>\n" +
            "                                        <m:Comment>Route to CoordinatorHandler</m:Comment>\n" +
            "                                    </m:Step>\n" +
            "                                    <m:Step>\n" +
            "                                        <m:Application.Module>SCASAMovementCreate.CoordinatorHandler\n" +
            "                                        </m:Application.Module>\n" +
            "                                        <m:VersionId>v4</m:VersionId>\n" +
            "                                        <m:TimeStamp>2016-09-20T16:42:37.221+03:00</m:TimeStamp>\n" +
            "                                        <m:RoutingRole>ROUTE</m:RoutingRole>\n" +
            "                                        <m:Comment>Route to ResponseHandler</m:Comment>\n" +
            "                                    </m:Step>\n" +
            "                                    <m:Step>\n" +
            "                                        <m:Application.Module>SCASAMovementCreate.ResponseHandler</m:Application.Module>\n" +
            "                                        <m:VersionId>v4</m:VersionId>\n" +
            "                                        <m:TimeStamp>2016-09-20T16:42:37.227+03:00</m:TimeStamp>\n" +
            "                                        <m:RoutingRole>ERROR</m:RoutingRole>\n" +
            "                                        <m:Comment>Error: Code=ERROR; Description=[FCC]:Ошибка при обработке в FCC;\n" +
            "                                        </m:Comment>\n" +
            "                                    </m:Step>\n" +
            "                                </m:MessagePath>\n" +
            "                                <m:ProcessInfo>\n" +
            "                                    <m:Name>BARSGL</m:Name>\n" +
            "                                    <m:InstanceId>687701</m:InstanceId>\n" +
            "                                </m:ProcessInfo>\n" +
            "                            </m:Audit>\n" +
            "                            <m:Tools>\n" +
            "                                <m:Environment/>\n" +
            "                            </m:Tools>\n" +
            "                            <m:Composite>\n" +
            "                                <m:Orchestration>SERVICE_DR_FIRST_OPTIMISTIC</m:Orchestration>\n" +
            "                            </m:Composite>\n" +
            "                        </m:UCBRUHeaders>\n" +
            "                        <NS1:SCASAMovementExtension xmlns:NS1=\"urn:ucbru:gbo:v4:casa\">\n" +
            "                            <NS1:Movement>\n" +
            "                                <NS1:MovementReference>\n" +
            "                                    <NS1:SystemCode>BARSGL</NS1:SystemCode>\n" +
            "                                    <NS1:MovementID>687702</NS1:MovementID>\n" +
            "                                </NS1:MovementReference>\n" +
            "                                <NS1:MovementAmount>9800.000</NS1:MovementAmount>\n" +
            "                                <NS1:CBAccount/>\n" +
            "                                <NS1:Status>R</NS1:Status>\n" +
            "                            </NS1:Movement>\n" +
            "                            <NS1:Movement>\n" +
            "                                <NS1:MovementReference>\n" +
            "                                    <NS1:SystemCode>BARSGL</NS1:SystemCode>\n" +
            "                                    <NS1:MovementID>687701</NS1:MovementID>\n" +
            "                                </NS1:MovementReference>\n" +
            "                                <NS1:ABS>FCC</NS1:ABS>\n" +
            "                                <NS1:CBAccount>40817810450330010549</NS1:CBAccount>\n" +
            "                                <NS1:Status>F</NS1:Status>\n" +
            "                                <NS1:ErrorCode>IF-PH014</NS1:ErrorCode>\n" +
            "                                <NS1:ErrorDescription>Account marked for No Debits/Amount block number is not\n" +
            "                                    entered40817810450330010549\n" +
            "                                </NS1:ErrorDescription>\n" +
            "                            </NS1:Movement>\n" +
            "                        </NS1:SCASAMovementExtension>\n" +
            "                    </gbo4:ErrorExtension>\n" +
            "                </gbo4:ExtendedError>\n" +
            "            </detail>\n" +
            "        </soapenv:Fault>\n" +
            "    </soapenv:Body>\n" +
            "</soapenv:Envelope>";

    private String errorExample =
        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:gbo=\"urn:ucbru:gbo:v4\">\n" +
            "\t<soapenv:Body>\n" +
            "\t\t<soapenv:Fault>\n" +
            "\t\t\t<faultcode>soapenv:Server</faultcode>\n" +
            "\t\t\t<faultstring>A schema validation error has occurred while parsing the XML document (5025: 5006; 2; 42; 62; cvc-complex-type.2.4.c: The matching wildcard is strict, but no declaration can be found for elem...; /Root/XMLNSC/http://schemas.xmlsoap.org/soap/envelope/:Envelope/http://schemas.xmlsoap.org/soap/e...)</faultstring>\n" +
            "\t\t\t<faultactor>AMDSMovementCreate</faultactor>\n" +
            "\t\t\t<detail>\n" +
            "\t\t\t\t<gbo:ExtendedError>\n" +
            "\t\t\t\t\t<gbo:ErrorDetails>\n" +
            "\t\t\t\t\t\t<gbo:Code>5025</gbo:Code>\n" +
            "\t\t\t\t\t\t<gbo:Description>A schema validation error has occurred while parsing the XML document (5025: 5006; 2; 42; 62; cvc-complex-type.2.4.c: The matching wildcard is strict, but no declaration can be found for elem...; /Root/XMLNSC/http://schemas.xmlsoap.org/soap/envelope/:Envelope/http://schemas.xmlsoap.org/soap/e...)</gbo:Description>\n" +
            "\t\t\t\t\t\t<gbo:Source>AMDSMovementCreate</gbo:Source>\n" +
            "\t\t\t\t\t\t<gbo:Kind>SYSERR</gbo:Kind>\n" +
            "\t\t\t\t\t\t<gbo:DateTime>2015-01-23T20:05:14.417255</gbo:DateTime>\n" +
            "\t\t\t\t\t</gbo:ErrorDetails>\n" +
            "\t\t\t\t\t<gbo:ErrorExtension>\n" +
            "\t\t\t\t\t\t<m:UCBRUHeaders xmlns:m=\"urn:ucbru:gbo:v4\">\n" +
            "\t\t\t\t\t\t\t<m:Correlation>\n" +
            "\t\t\t\t\t\t\t\t<m:XRef>String</m:XRef>\n" +
            "\t\t\t\t\t\t\t\t<m:Segmentation>\n" +
            "\t\t\t\t\t\t\t\t\t<m:CanSegmentResponse>true</m:CanSegmentResponse>\n" +
            "\t\t\t\t\t\t\t\t</m:Segmentation>\n" +
            "\t\t\t\t\t\t\t</m:Correlation>\n" +
            "\t\t\t\t\t\t\t<m:Security/>\n" +
            "\t\t\t\t\t\t\t<m:Audit>\n" +
            "\t\t\t\t\t\t\t\t<m:MessagePath>\n" +
            "\t\t\t\t\t\t\t\t\t<m:Step>\n" +
            "\t\t\t\t\t\t\t\t\t\t<m:Application.Module>String</m:Application.Module>\n" +
            "\t\t\t\t\t\t\t\t\t\t<m:VersionId>String</m:VersionId>\n" +
            "\t\t\t\t\t\t\t\t\t\t<m:TimeStamp>2001-12-17T09:30:47Z</m:TimeStamp>\n" +
            "\t\t\t\t\t\t\t\t\t\t<m:RoutingRole>String</m:RoutingRole>\n" +
            "\t\t\t\t\t\t\t\t\t\t<m:Comment>String</m:Comment>\n" +
            "\t\t\t\t\t\t\t\t\t</m:Step>\n" +
            "\t\t\t\t\t\t\t\t</m:MessagePath>\n" +
            "\t\t\t\t\t\t\t\t<m:ProcessInfo>\n" +
            "\t\t\t\t\t\t\t\t\t<m:Name>String</m:Name>\n" +
            "\t\t\t\t\t\t\t\t\t<m:InstanceId>String</m:InstanceId>\n" +
            "\t\t\t\t\t\t\t\t</m:ProcessInfo>\n" +
            "\t\t\t\t\t\t\t</m:Audit>\n" +
            "\t\t\t\t\t\t\t<m:Usability>\n" +
            "\t\t\t\t\t\t\t\t<m:Internationalization>\n" +
            "\t\t\t\t\t\t\t\t\t<m:Language>aa</m:Language>\n" +
            "\t\t\t\t\t\t\t\t</m:Internationalization>\n" +
            "\t\t\t\t\t\t\t\t<m:Fetch>\n" +
            "\t\t\t\t\t\t\t\t\t<m:MaxRecords>0</m:MaxRecords>\n" +
            "\t\t\t\t\t\t\t\t\t<m:MoreRecordsAvailable>true</m:MoreRecordsAvailable>\n" +
            "\t\t\t\t\t\t\t\t\t<m:RecordCount>0</m:RecordCount>\n" +
            "\t\t\t\t\t\t\t\t</m:Fetch>\n" +
            "\t\t\t\t\t\t\t</m:Usability>\n" +
            "\t\t\t\t\t\t\t<m:Tools>\n" +
            "\t\t\t\t\t\t\t\t<m:Environment/>\n" +
            "\t\t\t\t\t\t\t</m:Tools>\n" +
            "\t\t\t\t\t\t</m:UCBRUHeaders>\n" +
            "\t\t\t\t\t\t<ExceptionList>\n" +
            "\t\t\t\t\t\t\t<ParserException>\n" +
            "\t\t\t\t\t\t\t\t<File>F:\\build\\S700_P\\src\\DataFlowEngine\\ImbRootParser.cpp</File>\n" +
            "\t\t\t\t\t\t\t\t<Line>816</Line>\n" +
            "\t\t\t\t\t\t\t\t<Function>ImbRootParser::parseNextItem</Function>\n" +
            "\t\t\t\t\t\t\t\t<Type>ComIbmMQInputNode</Type>\n" +
            "\t\t\t\t\t\t\t\t<Name>ucbru/ats/amds/v4/mocr/RequestHandler#FCMComposite_1_2</Name>\n" +
            "\t\t\t\t\t\t\t\t<Label>ucbru.ats.amds.v4.mocr.RequestHandler.UCBRU.AMDS.V4.MOCR.REQUEST</Label>\n" +
            "\t\t\t\t\t\t\t\t<Catalog>BIPmsgs</Catalog>\n" +
            "\t\t\t\t\t\t\t\t<Severity>2</Severity>\n" +
            "\t\t\t\t\t\t\t\t<Number>5902</Number>\n" +
            "\t\t\t\t\t\t\t\t<Text>Exception whilst parsing</Text>\n" +
            "\t\t\t\t\t\t\t\t<Insert>\n" +
            "\t\t\t\t\t\t\t\t\t<Type>5</Type>\n" +
            "\t\t\t\t\t\t\t\t\t<Text>Root</Text>\n" +
            "\t\t\t\t\t\t\t\t</Insert>\n" +
            "\t\t\t\t\t\t\t\t<Insert>\n" +
            "\t\t\t\t\t\t\t\t\t<Type>5</Type>\n" +
            "\t\t\t\t\t\t\t\t\t<Text>XMLNSC</Text>\n" +
            "\t\t\t\t\t\t\t\t</Insert>\n" +
            "\t\t\t\t\t\t\t\t<Insert>\n" +
            "\t\t\t\t\t\t\t\t\t<Type>5</Type>\n" +
            "\t\t\t\t\t\t\t\t\t<Text>3c534f41502d454e563a456e76656c6f706520786d6c6e733a534f41502d454e563d22687474703a2f2f736368656d61732e786d6c736f61702e6f72672f736f61702f656e76656c6f70652f2220786d6c6e733a534f41502d454e433d22687474703a2f2f736368656d61732e786d6c736f61702e6f72672f736f61702f656e636f64696e672f2220786d6c6e733a7873693d22687474703a2f2f7777772e77332e6f72672f323030312f584d4c536368656d612d696e7374616e63652220786d6c6e733a7873643d22687474703a2f2f7777772e77332e6f72672f323030312f584d4c536368656d61223e0d0a093c534f41502d454e563a4865616465723e0d0a09093c6d3a55434252554865616465727320786d6c6e733a6d3d2275726e3a75636272753a67626f3a7634223e0d0a0909093c6d3a436f7272656c6174696f6e3e0d0a090909093c6d3a585265663e537472696e673c2f6d3a585265663e0d0a090909093c6d3a5365676d656e746174696f6e3e0d0a09090909093c6d3a43616e5365676d656e74526573706f6e73653e747275653c2f6d3a43616e5365676d656e74526573706f6e73653e0d0a090909093c2f6d3a5365676d656e746174696f6e3e0d0a0909093c2f6d3a436f7272656c6174696f6e3e0d0a0909093c6d3a53656375726974792f3e0d0a0909093c6d3a41756469743e0d0a090909093c6d3a4d657373616765506174683e0d0a09090909093c6d3a537465703e0d0a0909090909093c6d3a4170706c69636174696f6e2e4d6f64756c653e537472696e673c2f6d3a4170706c69636174696f6e2e4d6f64756c653e0d0a0909090909093c6d3a56657273696f6e49643e537472696e673c2f6d3a56657273696f6e49643e0d0a0909090909093c6d3a54696d655374616d703e323030312d31322d31375430393a33303a34375a3c2f6d3a54696d655374616d703e0d0a0909090909093c6d3a526f7574696e67526f6c653e537472696e673c2f6d3a526f7574696e67526f6c653e0d0a0909090909093c6d3a436f6d6d656e743e537472696e673c2f6d3a436f6d6d656e743e0d0a09090909093c2f6d3a537465703e0d0a090909093c2f6d3a4d657373616765506174683e0d0a090909093c6d3a50726f63657373496e666f3e0d0a09090909093c6d3a4e616d653e537472696e673c2f6d3a4e616d653e0d0a09090909093c6d3a496e7374616e636549643e537472696e673c2f6d3a496e7374616e636549643e0d0a090909093c2f6d3a50726f63657373496e666f3e0d0a0909093c2f6d3a41756469743e0d0a0909093c6d3a55736162696c6974793e0d0a090909093c6d3a496e7465726e6174696f6e616c697a6174696f6e3e0d0a09090909093c6d3a4c616e67756167653e61613c2f6d3a4c616e67756167653e0d0a090909093c2f6d3a496e7465726e6174696f6e616c697a6174696f6e3e0d0a090909093c6d3a46657463683e0d0a09090909093c6d3a4d61785265636f7264733e303c2f6d3a4d61785265636f7264733e0d0a09090909093c6d3a4d6f72655265636f726473417661696c61626c653e747275653c2f6d3a4d6f72655265636f726473417661696c61626c653e0d0a09090909093c6d3a5265636f7264436f756e743e303c2f6d3a5265636f7264436f756e743e0d0a090909093c2f6d3a46657463683e0d0a0909093c2f6d3a55736162696c6974793e0d0a0909093c6d3a546f6f6c733e0d0a090909093c6d3a456e7669726f6e6d656e742f3e0d0a0909093c2f6d3a546f6f6c733e0d0a09093c2f6d3a5543425255486561646572733e0d0a093c2f534f41502d454e563a4865616465723e0d0a093c534f41502d454e563a426f64793e0d0a09093c6d3a53434153414d6f76656d656e7443726561746546726f6d426c6f636b20786d6c6e733a6d3d2275726e3a75636272753a67626f3a7634223e0d0a0909093c6d3a4d6f76656d656e743e0d0a090909093c6d3a526571756573744e756d6265723e303c2f6d3a526571756573744e756d6265723e0d0a090909093c6d3a426c6f636b5265666572656e63653e0d0a09090909093c6d3a53797374656d436f64653e61616161616161616161616161616161616161616161616161616161616161613c2f6d3a53797374656d436f64653e0d0a09090909093c6d3a426c6f636b49443e61616161616161616161616161616161616161616161616161616161616161613c2f6d3a426c6f636b49443e0d0a090909093c2f6d3a426c6f636b5265666572656e63653e0d0a090909093c6d3a4d6f76656d656e745265666572656e63653e0d0a09090909093c6d3a53797374656d436f64653e61616161616161616161616161616161616161616161616161616161616161613c2f6d3a53797374656d436f64653e0d0a09090909093c6d3a4d6f76656d656e7449443e61616161616161616161616161616161616161616161616161616161616161613c2f6d3a4d6f76656d656e7449443e0d0a090909093c2f6d3a4d6f76656d656e745265666572656e63653e0d0a090909093c6d3a43424163636f756e743e39363330323937383230303031303133303838363c2f6d3a43424163636f756e743e0d0a090909093c6d3a4d6f76656d656e74416d6f756e743e31323334353637383931323334353637383931322e3132333c2f6d3a4d6f76656d656e74416d6f756e743e0d0a090909093c6d3a4f626a6563745265666572656e63653e61616161616161616161616161616161616161616161616161616161616161613c2f6d3a4f626a6563745265666572656e63653e0d0a090909093c6d3a4578744d6f64756c653e61616161616161616161616161616161616161616161616161616161616161613c2f6d3a4578744d6f64756c653e0d0a090909093c6d3a4578744f7065726174696f6e436f64653e61616161616161616161616161616161616161616161616161616161616161613c2f6d3a4578744f7065726174696f6e436f64653e0d0a090909093c6d3a5072696f726974793e353c2f6d3a5072696f726974793e0d0a090909093c6d3a4f7065726174696f6e54696d657374616d703e323030312d31322d31375430393a33303a34375a3c2f6d3a4f7065726174696f6e54696d657374616d703e0d0a090909093c6d3a447243723e443c2f6d3a447243723e0d0a090909093c6d3a4e61727261746976653e537472696e673c2f6d3a4e61727261746976653e0d0a090909093c6d3a5573654f76657264726166743e747275653c2f6d3a5573654f76657264726166743e0d0a090909093c6d3a49676e6f726542616c616e63653e747275653c2f6d3a49676e6f726542616c616e63653e0d0a090909093c6d3a49676e6f7265426c6f636b466c61673e747275653c2f6d3a49676e6f7265426c6f636b466c61673e0d0a090909093c6d3a5573654641464f3e537472696e673c2f6d3a5573654641464f3e0d0a090909093c6d3a56616c7565446174653e313936372d30382d31333c2f6d3a56616c7565446174653e0d0a090909093c6d3a5072696d65426c6f636b49443e537472696e673c2f6d3a5072696d65426c6f636b49443e0d0a090909093c6d3a53746f726e6f3e747275653c2f6d3a53746f726e6f3e0d0a090909093c6d3a4142535370656369666963506172616d65746572733e0d0a09090909093c6d3a4d696461735370656369666963506172616d65746572733e0d0a0909090909093c6d3a53504f533e616161616161613c2f6d3a53504f533e0d0a0909090909093c6d3a4f5452463e6161616161616161616161616161613c2f6d3a4f5452463e0d0a0909090909093c6d3a4465706172746d656e743e6161613c2f6d3a4465706172746d656e743e0d0a0909090909093c6d3a50726f66697443656e7465723e616161613c2f6d3a50726f66697443656e7465723e0d0a0909090909093c6d3a426f6f6b436f64653e61613c2f6d3a426f6f6b436f64653e0d0a0909090909093c6d3a506f7374696e6754726e5479703e61616161613c2f6d3a506f7374696e6754726e5479703e0d0a0909090909093c6d3a506f7374696e674e61727261746976653e6161616161616161616161616161616161616161616161616161616161613c2f6d3a506f7374696e674e61727261746976653e0d0a0909090909093c6d3a50726f6a65637454726e5479703e61613c2f6d3a50726f6a65637454726e5479703e0d0a0909090909093c6d3a50726f6a65637454726e4e62723e6161616161613c2f6d3a50726f6a65637454726e4e62723e0d0a0909090909093c6d3a50726f6a6563744e61727261746976653e6161616161616161616161616161616161616161616161616161616161613c2f6d3a50726f6a6563744e61727261746976653e0d0a09090909093c2f6d3a4d696461735370656369666963506172616d65746572733e0d0a090909093c2f6d3a4142535370656369666963506172616d65746572733e0d0a0909093c2f6d3a4d6f76656d656e743e0d0a0909093c6d3a4d6f76656d656e743e0d0a090909093c6d3a526571756573744e756d6265723e303c2f6d3a526571756573744e756d6265723e0d0a090909093c6d3a426c6f636b5265666572656e63653e0d0a09090909093c6d3a53797374656d436f64653e61616161616161616161616161616161616161616161616161616161616161613c2f6d3a53797374656d436f64653e0d0a09090909093c6d3a426c6f636b49443e61616161616161616161616161616161616161616161616161616161616161613c2f6d3a426c6f636b49443e0d0a090909093c2f6d3a426c6f636b5265666572656e63653e0d0a090909093c6d3a4d6f76656d656e745265666572656e63653e0d0a09090909093c6d3a53797374656d436f64653e61616161616161616161616161616161616161616161616161616161616161613c2f6d3a53797374656d436f64653e0d0a09090909093c6d3a4d6f76656d656e7449443e61616161616161616161616161616161616161616161616161616161616161613c2f6d3a4d6f76656d656e7449443e0d0a090909093c2f6d3a4d6f76656d656e745265666572656e63653e0d0a090909093c6d3a43424163636f756e743e39363330323937383230303031303133303838353c2f6d3a43424163636f756e743e0d0a090909093c6d3a4d6f76656d656e74416d6f756e743e31323334353637383931323334353637383931322e3132333c2f6d3a4d6f76656d656e74416d6f756e743e0d0a090909093c6d3a4f626a6563745265666572656e63653e61616161616161616161616161616161616161616161616161616161616161613c2f6d3a4f626a6563745265666572656e63653e0d0a090909093c6d3a4578744d6f64756c653e61616161616161616161616161616161616161616161616161616161616161613c2f6d3a4578744d6f64756c653e0d0a090909093c6d3a4578744f7065726174696f6e436f64653e61616161616161616161616161616161616161616161616161616161616161613c2f6d3a4578744f7065726174696f6e436f64653e0d0a090909093c6d3a5072696f726974793e353c2f6d3a5072696f726974793e0d0a090909093c6d3a4f7065726174696f6e54696d657374616d703e323030312d31322d31375430393a33303a34375a3c2f6d3a4f7065726174696f6e54696d657374616d703e0d0a090909093c6d3a447243723e443c2f6d3a447243723e0d0a090909093c6d3a4e61727261746976653e537472696e673c2f6d3a4e61727261746976653e0d0a090909093c6d3a5573654f76657264726166743e747275653c2f6d3a5573654f76657264726166743e0d0a090909093c6d3a49676e6f726542616c616e63653e747275653c2f6d3a49676e6f726542616c616e63653e0d0a090909093c6d3a49676e6f7265426c6f636b466c61673e747275653c2f6d3a49676e6f7265426c6f636b466c61673e0d0a090909093c6d3a5573654641464f3e537472696e673c2f6d3a5573654641464f3e0d0a090909093c6d3a56616c7565446174653e313936372d30382d31333c2f6d3a56616c7565446174653e0d0a090909093c6d3a5072696d65426c6f636b49443e537472696e673c2f6d3a5072696d65426c6f636b49443e0d0a090909093c6d3a53746f726e6f3e747275653c2f6d3a53746f726e6f3e0d0a090909093c6d3a4142535370656369666963506172616d65746572733e0d0a09090909093c6d3a4d696461735370656369666963506172616d65746572733e0d0a0909090909093c6d3a53504f533e616161616161613c2f6d3a53504f533e0d0a0909090909093c6d3a4f5452463e6161616161616161616161616161613c2f6d3a4f5452463e0d0a0909090909093c6d3a4465706172746d656e743e6161613c2f6d3a4465706172746d656e743e0d0a0909090909093c6d3a50726f66697443656e7465723e616161613c2f6d3a50726f66697443656e7465723e0d0a0909090909093c6d3a426f6f6b436f64653e61613c2f6d3a426f6f6b436f64653e0d0a0909090909093c6d3a506f7374696e6754726e5479703e61616161613c2f6d3a506f7374696e6754726e5479703e0d0a0909090909093c6d3a506f7374696e674e61727261746976653e6161616161616161616161616161616161616161616161616161616161613c2f6d3a506f7374696e674e61727261746976653e0d0a0909090909093c6d3a50726f6a65637454726e5479703e61613c2f6d3a50726f6a65637454726e5479703e0d0a0909090909093c6d3a50726f6a65637454726e4e62723e6161616161613c2f6d3a50726f6a65637454726e4e62723e0d0a0909090909093c6d3a50726f6a6563744e61727261746976653e6161616161616161616161616161616161616161616161616161616161613c2f6d3a50726f6a6563744e61727261746976653e0d0a09090909093c2f6d3a4d696461735370656369666963506172616d65746572733e0d0a090909093c2f6d3a4142535370656369666963506172616d65746572733e0d0a0909093c2f6d3a4d6f76656d656e743e0d0a0909093c6d3a4d6f76656d656e743e0d0a090909093c6d3a526571756573744e756d6265723e303c2f6d3a526571756573744e756d6265723e0d0a090909093c6d3a426c6f636b5265666572656e63653e0d0a09090909093c6d3a53797374656d436f64653e61616161616161616161616161616161616161616161616161616161616161613c2f6d3a53797374656d436f64653e0d0a09090909093c6d3a426c6f636b49443e61616161616161616161616161616161616161616161616161616161616161613c2f6d3a426c6f636b49443e0d0a090909093c2f6d3a426c6f636b5265666572656e63653e0d0a090909093c6d3a4d6f76656d656e745265666572656e63653e0d0a09090909093c6d3a53797374656d436f64653e61616161616161616161616161616161616161616161616161616161616161613c2f6d3a53797374656d436f64653e0d0a09090909093c6d3a4d6f76656d656e7449443e61616161616161616161616161616161616161616161616161616161616161613c2f6d3a4d6f76656d656e7449443e0d0a090909093c2f6d3a4d6f76656d656e745265666572656e63653e0d0a090909093c6d3a43424163636f756e743e39363330323937383230303031303133303838353c2f6d3a43424163636f756e743e0d0a090909093c6d3a4d6f76656d656e74416d6f756e743e31323334353637383931323334353637383931322e3132333c2f6d3a4d6f76656d656e74416d6f756e743e0d0a090909093c6d3a4f626a6563745265666572656e63653e61616161616161616161616161616161616161616161616161616161616161613c2f6d3a4f626a6563745265666572656e63653e0d0a090909093c6d3a4578744d6f64756c653e61616161616161616161616161616161616161616161616161616161616161613c2f6d3a4578744d6f64756c653e0d0a090909093c6d3a4578744f7065726174696f6e436f64653e61616161616161616161616161616161616161616161616161616161616161613c2f6d3a4578744f7065726174696f6e436f64653e0d0a090909093c6d3a5072696f726974793e353c2f6d3a5072696f726974793e0d0a090909093c6d3a4f7065726174696f6e54696d657374616d703e323030312d31322d31375430393a33303a34375a3c2f6d3a4f7065726174696f6e54696d657374616d703e0d0a090909093c6d3a447243723e443c2f6d3a447243723e0d0a090909093c6d3a4e61727261746976653e537472696e673c2f6d3a4e61727261746976653e0d0a090909093c6d3a5573654f76657264726166743e747275653c2f6d3a5573654f76657264726166743e0d0a090909093c6d3a49676e6f726542616c616e63653e747275653c2f6d3a49676e6f726542616c616e63653e0d0a090909093c6d3a49676e6f7265426c6f636b466c61673e747275653c2f6d3a49676e6f7265426c6f636b466c61673e0d0a090909093c6d3a5573654641464f3e537472696e673c2f6d3a5573654641464f3e0d0a090909093c6d3a56616c7565446174653e313936372d30382d31333c2f6d3a56616c7565446174653e0d0a090909093c6d3a5072696d65426c6f636b49443e537472696e673c2f6d3a5072696d65426c6f636b49443e0d0a090909093c6d3a53746f726e6f3e747275653c2f6d3a53746f726e6f3e0d0a090909093c6d3a4142535370656369666963506172616d65746572733e0d0a09090909093c6d3a4d696461735370656369666963506172616d65746572733e0d0a0909090909093c6d3a53504f533e616161616161613c2f6d3a53504f533e0d0a0909090909093c6d3a4f5452463e6161616161616161616161616161613c2f6d3a4f5452463e0d0a0909090909093c6d3a4465706172746d656e743e6161613c2f6d3a4465706172746d656e743e0d0a0909090909093c6d3a50726f66697443656e7465723e616161613c2f6d3a50726f66697443656e7465723e0d0a0909090909093c6d3a426f6f6b436f64653e61613c2f6d3a426f6f6b436f64653e0d0a0909090909093c6d3a506f7374696e6754726e5479703e61616161613c2f6d3a506f7374696e6754726e5479703e0d0a0909090909093c6d3a506f7374696e674e61727261746976653e6161616161616161616161616161616161616161616161616161616161613c2f6d3a506f7374696e674e61727261746976653e0d0a0909090909093c6d3a50726f6a65637454726e5479703e61613c2f6d3a50726f6a65637454726e5479703e0d0a0909090909093c6d3a50726f6a65637454726e4e62723e6161616161613c2f6d3a50726f6a65637454726e4e62723e0d0a0909090909093c6d3a50726f6a6563744e61727261746976653e6161616161616161616161616161616161616161616161616161616161613c2f6d3a50726f6a6563744e61727261746976653e0d0a09090909093c2f6d3a4d696461735370656369666963506172616d65746572733e0d0a090909093c2f6d3a4142535370656369666963506172616d65746572733e0d0a0909093c2f6d3a4d6f76656d656e743e0d0a09093c2f6d3a53434153414d6f76656d656e7443726561746546726f6d426c6f636b3e0d0a093c2f534f41502d454e563a426f64793e0d0a3c2f534f41502d454e563a456e76656c6f70653e0d0a</Text>\n" +
            "\t\t\t\t\t\t\t\t</Insert>\n" +
            "\t\t\t\t\t\t\t\t<ParserException>\n" +
            "\t\t\t\t\t\t\t\t\t<File>F:\\build\\S700_P\\src\\MTI\\MTIforBroker\\GenXmlParser4\\ImbXMLNSCParser.cpp</File>\n" +
            "\t\t\t\t\t\t\t\t\t<Line>1073</Line>\n" +
            "\t\t\t\t\t\t\t\t\t<Function>ImbXMLNSCParser::parseLastChild</Function>\n" +
            "\t\t\t\t\t\t\t\t\t<Type/>\n" +
            "\t\t\t\t\t\t\t\t\t<Name/>\n" +
            "\t\t\t\t\t\t\t\t\t<Label/>\n" +
            "\t\t\t\t\t\t\t\t\t<Catalog>BIPmsgs</Catalog>\n" +
            "\t\t\t\t\t\t\t\t\t<Severity>3</Severity>\n" +
            "\t\t\t\t\t\t\t\t\t<Number>5009</Number>\n" +
            "\t\t\t\t\t\t\t\t\t<Text>XML Parsing Errors have occurred</Text>\n" +
            "\t\t\t\t\t\t\t\t\t<ParserException>\n" +
            "\t\t\t\t\t\t\t\t\t\t<File>F:\\build\\S700_P\\src\\MTI\\MTIforBroker\\GenXmlParser4\\ImbXMLNSCDocHandler.cpp</File>\n" +
            "\t\t\t\t\t\t\t\t\t\t<Line>634</Line>\n" +
            "\t\t\t\t\t\t\t\t\t\t<Function>ImbXMLNSCDocHandler::handleParseErrors</Function>\n" +
            "\t\t\t\t\t\t\t\t\t\t<Type>ComIbmMQInputNode</Type>\n" +
            "\t\t\t\t\t\t\t\t\t\t<Name>ucbru/ats/amds/v4/mocr/RequestHandler#FCMComposite_1_2</Name>\n" +
            "\t\t\t\t\t\t\t\t\t\t<Label>ucbru.ats.amds.v4.mocr.RequestHandler.UCBRU.AMDS.V4.MOCR.REQUEST</Label>\n" +
            "\t\t\t\t\t\t\t\t\t\t<Catalog>BIPmsgs</Catalog>\n" +
            "\t\t\t\t\t\t\t\t\t\t<Severity>3</Severity>\n" +
            "\t\t\t\t\t\t\t\t\t\t<Number>5025</Number>\n" +
            "\t\t\t\t\t\t\t\t\t\t<Text>A schema validation error has occurred while parsing the XML document</Text>\n" +
            "\t\t\t\t\t\t\t\t\t\t<Insert>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t<Type>2</Type>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t<Text>5006</Text>\n" +
            "\t\t\t\t\t\t\t\t\t\t</Insert>\n" +
            "\t\t\t\t\t\t\t\t\t\t<Insert>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t<Type>2</Type>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t<Text>2</Text>\n" +
            "\t\t\t\t\t\t\t\t\t\t</Insert>\n" +
            "\t\t\t\t\t\t\t\t\t\t<Insert>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t<Type>2</Type>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t<Text>42</Text>\n" +
            "\t\t\t\t\t\t\t\t\t\t</Insert>\n" +
            "\t\t\t\t\t\t\t\t\t\t<Insert>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t<Type>2</Type>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t<Text>62</Text>\n" +
            "\t\t\t\t\t\t\t\t\t\t</Insert>\n" +
            "\t\t\t\t\t\t\t\t\t\t<Insert>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t<Type>5</Type>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t<Text>cvc-complex-type.2.4.c: The matching wildcard is strict, but no declaration can be found for element \"m:SCASAMovementCreateFromBlock\".</Text>\n" +
            "\t\t\t\t\t\t\t\t\t\t</Insert>\n" +
            "\t\t\t\t\t\t\t\t\t\t<Insert>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t<Type>5</Type>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t<Text>/Root/XMLNSC/http://schemas.xmlsoap.org/soap/envelope/:Envelope/http://schemas.xmlsoap.org/soap/envelope/:Body</Text>\n" +
            "\t\t\t\t\t\t\t\t\t\t</Insert>\n" +
            "\t\t\t\t\t\t\t\t\t</ParserException>\n" +
            "\t\t\t\t\t\t\t\t</ParserException>\n" +
            "\t\t\t\t\t\t\t</ParserException>\n" +
            "\t\t\t\t\t\t</ExceptionList>\n" +
            "\t\t\t\t\t</gbo:ErrorExtension>\n" +
            "\t\t\t\t</gbo:ExtendedError>\n" +
            "\t\t\t</detail>\n" +
            "\t\t</soapenv:Fault>\n" +
            "\t</soapenv:Body>\n" +
            "</soapenv:Envelope>";

    private String incomingExample =
        "<NS1:Envelope xmlns:NS1=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
            "\t<NS1:Header>\n" +
            "\t\t<m:UCBRUHeaders xmlns:m=\"urn:ucbru:gbo:v4\">\n" +
            "\t\t\t<m:Correlation>\n" +
            "\t\t\t\t<m:XRef>c20c47f4-fa1f-46dd-bcc0-b224cdc42181</m:XRef>\n" +
            "\t\t\t\t<m:Segmentation>\n" +
            "\t\t\t\t\t<m:CanSegmentResponse>true</m:CanSegmentResponse>\n" +
            "\t\t\t\t</m:Segmentation>\n" +
            "\t\t\t</m:Correlation>\n" +
            "\t\t\t<m:Security/>\n" +
            "\t\t\t<m:Audit>\n" +
            "\t\t\t\t<m:MessagePath>\n" +
            "\t\t\t\t\t<m:Step>\n" +
            "\t\t\t\t\t\t<m:Application.Module>String</m:Application.Module>\n" +
            "\t\t\t\t\t\t<m:VersionId>String</m:VersionId>\n" +
            "\t\t\t\t\t\t<m:TimeStamp>2001-12-17T09:30:47Z</m:TimeStamp>\n" +
            "\t\t\t\t\t\t<m:RoutingRole>String</m:RoutingRole>\n" +
            "\t\t\t\t\t\t<m:Comment>String</m:Comment>\n" +
            "\t\t\t\t\t</m:Step>\n" +
            "\t\t\t\t\t<m:Step>\n" +
            "\t\t\t\t\t\t<m:Application.Module>SCASAMovementCreate.RequestHandler</m:Application.Module>\n" +
            "\t\t\t\t\t\t<m:VersionId>v4</m:VersionId>\n" +
            "\t\t\t\t\t\t<m:TimeStamp>2015-02-20T12:32:07.484+03:00</m:TimeStamp>\n" +
            "\t\t\t\t\t\t<m:RoutingRole>START</m:RoutingRole>\n" +
            "\t\t\t\t\t\t<m:Comment/>\n" +
            "\t\t\t\t\t</m:Step>\n" +
            "\t\t\t\t\t<m:Step>\n" +
            "\t\t\t\t\t\t<m:Application.Module>SCASAMovementCreate.MaAcPo_ResponseHandler</m:Application.Module>\n" +
            "\t\t\t\t\t\t<m:VersionId>v4</m:VersionId>\n" +
            "\t\t\t\t\t\t<m:TimeStamp>2015-02-20T12:32:08.063+03:00</m:TimeStamp>\n" +
            "\t\t\t\t\t\t<m:RoutingRole>ROUTE</m:RoutingRole>\n" +
            "\t\t\t\t\t\t<m:Comment/>\n" +
            "\t\t\t\t\t</m:Step>\n" +
            "\t\t\t\t\t<m:Step>\n" +
            "\t\t\t\t\t\t<m:Application.Module>SCASAMovementCreate.ResponseHandler</m:Application.Module>\n" +
            "\t\t\t\t\t\t<m:VersionId>v4</m:VersionId>\n" +
            "\t\t\t\t\t\t<m:TimeStamp>2015-02-20T12:32:10.753+03:00</m:TimeStamp>\n" +
            "\t\t\t\t\t\t<m:RoutingRole>SUCCESS</m:RoutingRole>\n" +
            "\t\t\t\t\t\t<m:Comment/>\n" +
            "\t\t\t\t\t</m:Step>\n" +
            "\t\t\t\t</m:MessagePath>\n" +
            "\t\t\t\t<m:ProcessInfo>\n" +
            "\t\t\t\t\t<m:Name>N9</m:Name>\n" +
            "\t\t\t\t\t<m:InstanceId>I1</m:InstanceId>\n" +
            "\t\t\t\t</m:ProcessInfo>\n" +
            "\t\t\t</m:Audit>\n" +
            "\t\t\t<m:Usability>\n" +
            "\t\t\t\t<m:Internationalization>\n" +
            "\t\t\t\t\t<m:Language>aa</m:Language>\n" +
            "\t\t\t\t</m:Internationalization>\n" +
            "\t\t\t\t<m:Fetch>\n" +
            "\t\t\t\t\t<m:MaxRecords>0</m:MaxRecords>\n" +
            "\t\t\t\t\t<m:MoreRecordsAvailable>true</m:MoreRecordsAvailable>\n" +
            "\t\t\t\t\t<m:RecordCount>0</m:RecordCount>\n" +
            "\t\t\t\t</m:Fetch>\n" +
            "\t\t\t</m:Usability>\n" +
            "\t\t\t<m:Tools>\n" +
            "\t\t\t\t<m:Environment/>\n" +
            "\t\t\t</m:Tools>\n" +
            "\t\t</m:UCBRUHeaders>\n" +
            "\t</NS1:Header>\n" +
            "\t<NS1:Body>\n" +
            "\t\t<NS2:ExtendedStatus xmlns:NS2=\"urn:ucbru:gbo:v4\">\n" +
            "\t\t\t<NS2:StatusDetails>\n" +
            "\t\t\t\t<NS2:Status>SUCCESS</NS2:Status>\n" +
            "\t\t\t\t<NS2:Description>Обработка запроса прошла успешно</NS2:Description>\n" +
            "\t\t\t\t<NS2:Source>SCASA</NS2:Source>\n" +
            "\t\t\t\t<NS2:Kind>Обработка запроса прошла успешно</NS2:Kind>\n" +
            "\t\t\t\t<NS2:DateTime>2015-02-20T12:32:10.749528</NS2:DateTime>\n" +
            "\t\t\t</NS2:StatusDetails>\n" +
            "\t\t\t<NS2:StatusExtension>\n" +
            "\t\t\t\t<NS3:SCASAMovementExtension xmlns:NS3=\"urn:ucbru:gbo:v4:casa\">\n" +
            "\t\t\t\t\t<NS3:Movement>\n" +
            "\t\t\t\t\t\t<NS3:BlockReference>\n" +
            "\t\t\t\t\t\t\t<NS3:SystemCode>system_code_bl_r_1</NS3:SystemCode>\n" +
            "\t\t\t\t\t\t\t<NS3:BlockID>block_id_1</NS3:BlockID>\n" +
            "\t\t\t\t\t\t</NS3:BlockReference>\n" +
            "\t\t\t\t\t\t<NS3:MovementReference>\n" +
            "\t\t\t\t\t\t\t<NS2:SystemCode>system_code_mo_r_1</NS2:SystemCode>\n" +
            "\t\t\t\t\t\t\t<NS3:MovementID>movement_id_1</NS3:MovementID>\n" +
            "\t\t\t\t\t\t</NS3:MovementReference>\n" +
            "\t\t\t\t\t\t<NS3:MovementAmount>3.14</NS3:MovementAmount>\n" +
            "\t\t\t\t\t\t<NS3:ABSReference>AMobt0000583</NS3:ABSReference>\n" +
            "\t\t\t\t\t</NS3:Movement>\n" +
            "\t\t\t\t\t<NS3:Movement>\n" +
            "\t\t\t\t\t\t<NS3:BlockReference>\n" +
            "\t\t\t\t\t\t\t<NS3:SystemCode>system_code_bl_r_2</NS3:SystemCode>\n" +
            "\t\t\t\t\t\t\t<NS3:BlockID>block_id_2</NS3:BlockID>\n" +
            "\t\t\t\t\t\t</NS3:BlockReference>\n" +
            "\t\t\t\t\t\t<NS3:MovementReference>\n" +
            "\t\t\t\t\t\t\t<NS2:SystemCode>system_code_mo_r_2</NS2:SystemCode>\n" +
            "\t\t\t\t\t\t\t<NS3:MovementID>movement_id_2</NS3:MovementID>\n" +
            "\t\t\t\t\t\t</NS3:MovementReference>\n" +
            "\t\t\t\t\t\t<NS3:MovementAmount>3.72</NS3:MovementAmount>\n" +
            "\t\t\t\t\t\t<NS3:ABSReference>AMobt0000584</NS3:ABSReference>\n" +
            "\t\t\t\t\t</NS3:Movement>\n" +
            "\t\t\t\t\t<NS3:Movement>\n" +
            "\t\t\t\t\t\t<NS3:BlockReference>\n" +
            "\t\t\t\t\t\t\t<NS3:SystemCode>system_code_bl_r_3</NS3:SystemCode>\n" +
            "\t\t\t\t\t\t\t<NS3:BlockID>block_id_3</NS3:BlockID>\n" +
            "\t\t\t\t\t\t</NS3:BlockReference>\n" +
            "\t\t\t\t\t\t<NS3:MovementReference>\n" +
            "\t\t\t\t\t\t\t<NS2:SystemCode>system_code_mo_r_3</NS2:SystemCode>\n" +
            "\t\t\t\t\t\t\t<NS3:MovementID>movement_id_3</NS3:MovementID>\n" +
            "\t\t\t\t\t\t</NS3:MovementReference>\n" +
            "\t\t\t\t\t\t<NS3:MovementAmount>3.18</NS3:MovementAmount>\n" +
            "\t\t\t\t\t\t<NS3:ABSReference>AMobt0000585</NS3:ABSReference>\n" +
            "\t\t\t\t\t</NS3:Movement>\n" +
            "\t\t\t\t</NS3:SCASAMovementExtension>\n" +
            "\t\t\t</NS2:StatusExtension>\n" +
            "\t\t</NS2:ExtendedStatus>\n" +
            "\t</NS1:Body>\n" +
            "</NS1:Envelope>\n";

    private String outcomingExample =
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
            "    <soapenv:Header>\n" +
            "        <ns2:UCBRUHeaders xmlns:ns2=\"urn:ucbru:gbo:v4\">\n" +
            "            <ns2:Correlation>\n" +
            "                <ns2:XRef>c20c47f4-fa1f-46dd-bcc0-b224cdc42181</ns2:XRef>\n" +
            "            </ns2:Correlation>\n" +
            "        </ns2:UCBRUHeaders>\n" +
            "    </soapenv:Header>\n" +
            "    <soapenv:Body>\n" +
            "        <ns2:SCASAMovementCreate xmlns:ns2=\"urn:ucbru:gbo:v4:casa\">\n" +
            "            <ns2:Movement>\n" +
            "                <ns2:RequestNumber>1</ns2:RequestNumber>\n" +
            "                <ns2:MovementReference>\n" +
            "                    <ns2:SystemCode>BARSGL</ns2:SystemCode>\n" +
            "                    <ns2:MovementID>789003</ns2:MovementID>\n" +
            "                </ns2:MovementReference>\n" +
            "                <ns2:CBAccount>40702810700010003896</ns2:CBAccount>\n" +
            "                <ns2:MovementAmount>777.77</ns2:MovementAmount>\n" +
            "                <ns2:ObjectReference>88000</ns2:ObjectReference>\n" +
            "                <ns2:ExtModule>PHub1</ns2:ExtModule>\n" +
            "                <ns2:ExtOperationCode>PAY</ns2:ExtOperationCode>\n" +
            "                <ns2:Priority>5</ns2:Priority>\n" +
            "                <ns2:OperationTimestamp>2016-06-08T10:46:59.593+03:00</ns2:OperationTimestamp>\n" +
            "                <ns2:DrCr>D</ns2:DrCr>\n" +
            "                <ns2:Narrative>test</ns2:Narrative>\n" +
            "                <ns2:UseOverdraft>true</ns2:UseOverdraft>\n" +
            "                <ns2:IgnoreBalance>false</ns2:IgnoreBalance>\n" +
            "                <ns2:IgnoreBlockFlag>false</ns2:IgnoreBlockFlag>\n" +
            "                <ns2:ValueDate>2016-01-18</ns2:ValueDate>\n" +
            "                <ns2:ABSSpecificParameters>\n" +
            "                    <ns2:MIDASSpecificParameters>\n" +
            "                        <ns2:SPOS>PHUB</ns2:SPOS>\n" +
            "                        <ns2:OTRF>88000</ns2:OTRF>\n" +
            "                        <ns2:Department>ITD</ns2:Department>\n" +
            "                        <ns2:ProfitCenter>Test</ns2:ProfitCenter>\n" +
            "                        <ns2:PostingTrnTyp></ns2:PostingTrnTyp>\n" +
            "                        <ns2:PostingNarrative>88000</ns2:PostingNarrative>\n" +
            "                        <ns2:ProjectNarrative>88000</ns2:ProjectNarrative>\n" +
            "                    </ns2:MIDASSpecificParameters>\n" +
            "                </ns2:ABSSpecificParameters>\n" +
            "            </ns2:Movement>\n" +
            "        </ns2:SCASAMovementCreate>\n" +
            "    </soapenv:Body>\n" +
            "</soapenv:Envelope>";

    String doubleError="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:gbo4=\"urn:ucbru:gbo:v4\">\n" +
                           "    <soapenv:Body>\n" +
                           "        <soapenv:Fault>\n" +
                           "            <faultcode>soapenv:Server</faultcode>\n" +
                           "            <faultstring>[FC12]:Ошибка при обработке в FC12;</faultstring>\n" +
                           "            <faultactor>FC12;</faultactor>\n" +
                           "            <detail>\n" +
                           "                <gbo4:ExtendedError>\n" +
                           "                    <gbo4:ErrorDetails>\n" +
                           "                        <gbo4:Code>ERROR</gbo4:Code>\n" +
                           "                        <gbo4:Description>[FC12]:Ошибка при обработке в FC12;</gbo4:Description>\n" +
                           "                        <gbo4:Source>FC12;</gbo4:Source>\n" +
                           "                        <gbo4:Kind>ERROR</gbo4:Kind>\n" +
                           "                        <gbo4:DateTime>2016-11-23T21:52:29.689996+03:00</gbo4:DateTime>\n" +
                           "                    </gbo4:ErrorDetails>\n" +
                           "                    <gbo4:ErrorExtension>\n" +
                           "                        <m:UCBRUHeaders xmlns:m=\"urn:ucbru:gbo:v4\">\n" +
                           "                            <m:Correlation>\n" +
                           "                                <m:XRef>59443D.7f30c5.59443C.7f30c5</m:XRef>\n" +
                           "                            </m:Correlation>\n" +
                           "                            <m:Audit>\n" +
                           "                                <m:MessagePath>\n" +
                           "                                    <m:Step>\n" +
                           "                                        <m:Application.Module>SCASAMovementCreate.RequestHandler</m:Application.Module>\n" +
                           "                                        <m:VersionId>v4</m:VersionId>\n" +
                           "                                        <m:TimeStamp>2016-11-23T21:52:24.339+03:00</m:TimeStamp>\n" +
                           "                                        <m:RoutingRole>START</m:RoutingRole>\n" +
                           "                                        <m:Comment/>\n" +
                           "                                    </m:Step>\n" +
                           "                                    <m:Step>\n" +
                           "                                        <m:Application.Module>SRVACC.MaAcPoBaQu.ESBDB_RequestHandler\n" +
                           "                                        </m:Application.Module>\n" +
                           "                                        <m:VersionId>v4</m:VersionId>\n" +
                           "                                        <m:TimeStamp>2016-11-23T21:52:24.344+03:00</m:TimeStamp>\n" +
                           "                                        <m:RoutingRole>START</m:RoutingRole>\n" +
                           "                                        <m:Comment>Start of message processing</m:Comment>\n" +
                           "                                    </m:Step>\n" +
                           "                                    <m:Step>\n" +
                           "                                        <m:Application.Module>SRVACC.MaAcPoBaQu.ESBDB_RequestHandler\n" +
                           "                                        </m:Application.Module>\n" +
                           "                                        <m:VersionId>v4</m:VersionId>\n" +
                           "                                        <m:TimeStamp>2016-11-23T21:52:24.357+03:00</m:TimeStamp>\n" +
                           "                                        <m:RoutingRole>ROUTE</m:RoutingRole>\n" +
                           "                                        <m:Comment>Routing no fc12 accounts</m:Comment>\n" +
                           "                                    </m:Step>\n" +
                           "                                    <m:Step>\n" +
                           "                                        <m:Application.Module>SRVACC.MaAcPoBaQu.ESBDB_ResponseHandler\n" +
                           "                                        </m:Application.Module>\n" +
                           "                                        <m:VersionId>v4</m:VersionId>\n" +
                           "                                        <m:TimeStamp>2016-11-23T21:52:24.368+03:00</m:TimeStamp>\n" +
                           "                                        <m:RoutingRole>SUCCESS</m:RoutingRole>\n" +
                           "                                        <m:Comment>Finish of message processing</m:Comment>\n" +
                           "                                    </m:Step>\n" +
                           "                                    <m:Step>\n" +
                           "                                        <m:Application.Module>SCASAMovementCreate.MaAcPoResponseHandler\n" +
                           "                                        </m:Application.Module>\n" +
                           "                                        <m:VersionId>v4</m:VersionId>\n" +
                           "                                        <m:TimeStamp>2016-11-23T21:52:24.438+03:00</m:TimeStamp>\n" +
                           "                                        <m:RoutingRole>ROUTE</m:RoutingRole>\n" +
                           "                                        <m:Comment>Route to CoordinatorHandler</m:Comment>\n" +
                           "                                    </m:Step>\n" +
                           "                                    <m:Step>\n" +
                           "                                        <m:Application.Module>SCASAMovementCreate.CoordinatorHandler\n" +
                           "                                        </m:Application.Module>\n" +
                           "                                        <m:VersionId>v4</m:VersionId>\n" +
                           "                                        <m:TimeStamp>2016-11-23T21:52:24.445+03:00</m:TimeStamp>\n" +
                           "                                        <m:RoutingRole>ROUTE</m:RoutingRole>\n" +
                           "                                        <m:Comment>Route to Send2ABS</m:Comment>\n" +
                           "                                    </m:Step>\n" +
                           "                                    <m:Step>\n" +
                           "                                        <m:Application.Module>SCASAMovementCreate.Send2ABS_Request\n" +
                           "                                        </m:Application.Module>\n" +
                           "                                        <m:VersionId>v4</m:VersionId>\n" +
                           "                                        <m:TimeStamp>2016-11-23T21:52:24.455+03:00</m:TimeStamp>\n" +
                           "                                        <m:RoutingRole>ROUTE</m:RoutingRole>\n" +
                           "                                        <m:Comment/>\n" +
                           "                                    </m:Step>\n" +
                           "                                    <m:Step>\n" +
                           "                                        <m:Application.Module>AFC12MovementCreate.RequestHandler</m:Application.Module>\n" +
                           "                                        <m:VersionId>v4</m:VersionId>\n" +
                           "                                        <m:TimeStamp>2016-11-23T21:52:24.461+03:00</m:TimeStamp>\n" +
                           "                                        <m:RoutingRole>START</m:RoutingRole>\n" +
                           "                                        <m:Comment/>\n" +
                           "                                    </m:Step>\n" +
                           "                                    <m:Step>\n" +
                           "                                        <m:Application.Module>AFC12MovementCreate.ResponseHandler</m:Application.Module>\n" +
                           "                                        <m:VersionId>v4</m:VersionId>\n" +
                           "                                        <m:TimeStamp>2016-11-23T21:52:29.656+03:00</m:TimeStamp>\n" +
                           "                                        <m:RoutingRole>ERROR</m:RoutingRole>\n" +
                           "                                        <m:Comment>Error: Code=2951; Description=Ошибка при обработке в FC12</m:Comment>\n" +
                           "                                    </m:Step>\n" +
                           "                                    <m:Step>\n" +
                           "                                        <m:Application.Module>SCASAMovementCreate.Send2ABS_Response\n" +
                           "                                        </m:Application.Module>\n" +
                           "                                        <m:VersionId>v4</m:VersionId>\n" +
                           "                                        <m:TimeStamp>2016-11-23T21:52:29.677+03:00</m:TimeStamp>\n" +
                           "                                        <m:RoutingRole>ROUTE</m:RoutingRole>\n" +
                           "                                        <m:Comment>Route to CoordinatorHandler</m:Comment>\n" +
                           "                                    </m:Step>\n" +
                           "                                    <m:Step>\n" +
                           "                                        <m:Application.Module>SCASAMovementCreate.CoordinatorHandler\n" +
                           "                                        </m:Application.Module>\n" +
                           "                                        <m:VersionId>v4</m:VersionId>\n" +
                           "                                        <m:TimeStamp>2016-11-23T21:52:29.684+03:00</m:TimeStamp>\n" +
                           "                                        <m:RoutingRole>ROUTE</m:RoutingRole>\n" +
                           "                                        <m:Comment>Route to ResponseHandler</m:Comment>\n" +
                           "                                    </m:Step>\n" +
                           "                                    <m:Step>\n" +
                           "                                        <m:Application.Module>SCASAMovementCreate.ResponseHandler</m:Application.Module>\n" +
                           "                                        <m:VersionId>v4</m:VersionId>\n" +
                           "                                        <m:TimeStamp>2016-11-23T21:52:29.690+03:00</m:TimeStamp>\n" +
                           "                                        <m:RoutingRole>ERROR</m:RoutingRole>\n" +
                           "                                        <m:Comment>Error: Code=ERROR; Description=[FC12]:Ошибка при обработке в FC12;\n" +
                           "                                        </m:Comment>\n" +
                           "                                    </m:Step>\n" +
                           "                                </m:MessagePath>\n" +
                           "                                <m:ProcessInfo>\n" +
                           "                                    <m:Name>BARSGL</m:Name>\n" +
                           "                                    <m:InstanceId>59443D.7f30c5.59443C.7f30c5</m:InstanceId>\n" +
                           "                                </m:ProcessInfo>\n" +
                           "                            </m:Audit>\n" +
                           "                            <m:Tools>\n" +
                           "                                <m:Environment/>\n" +
                           "                            </m:Tools>\n" +
                           "                            <m:Composite>\n" +
                           "                                <m:Orchestration>SERVICE_DR_FIRST_OPTIMISTIC</m:Orchestration>\n" +
                           "                            </m:Composite>\n" +
                           "                        </m:UCBRUHeaders>\n" +
                           "                        <NS1:SCASAMovementExtension xmlns:NS1=\"urn:ucbru:gbo:v4:casa\">\n" +
                           "                            <NS1:Movement>\n" +
                           "                                <NS1:MovementReference>\n" +
                           "                                    <NS1:SystemCode>BARSGL</NS1:SystemCode>\n" +
                           "                                    <NS1:MovementID>59443D.7f30c5</NS1:MovementID>\n" +
                           "                                </NS1:MovementReference>\n" +
                           "                                <NS1:ABS>FC12</NS1:ABS>\n" +
                           "                                <NS1:CBAccount>30114840400010145648</NS1:CBAccount>\n" +
                           "                                <NS1:Status>F</NS1:Status>\n" +
                           "                                <NS1:ErrorCode>BA-022</NS1:ErrorCode>\n" +
                           "                                <NS1:ErrorDescription>Movement process error: Process:DE Teller\n" +
                           "                                    creating:EXTERNAL_TXN_ID:BARSGL 59443D.7f30c5.59443C.7f30c5:ORDER_DE:1:AC-UPD02;:MOS\n" +
                           "                                    00201996USDBANO301~;\n" +
                           "                                </NS1:ErrorDescription>\n" +
                           "                            </NS1:Movement>\n" +
                           "                            <NS1:Movement>\n" +
                           "                                <NS1:MovementReference>\n" +
                           "                                    <NS1:SystemCode>BARSGL</NS1:SystemCode>\n" +
                           "                                    <NS1:MovementID>59443C.7f30c5</NS1:MovementID>\n" +
                           "                                </NS1:MovementReference>\n" +
                           "                                <NS1:ABS>FC12</NS1:ABS>\n" +
                           "                                <NS1:CBAccount>30114840700010000438</NS1:CBAccount>\n" +
                           "                                <NS1:Status>F</NS1:Status>\n" +
                           "                                <NS1:ErrorDescription>There is some failed request for EXTERNAL_TXN_ID:BARSGL\n" +
                           "                                    59443D.7f30c5.59443C.7f30c5\n" +
                           "                                </NS1:ErrorDescription>\n" +
                           "                            </NS1:Movement>\n" +
                           "                        </NS1:SCASAMovementExtension>\n" +
                           "                    </gbo4:ErrorExtension>\n" +
                           "                </gbo4:ExtendedError>\n" +
                           "            </detail>\n" +
                           "        </soapenv:Fault>\n" +
                           "    </soapenv:Body>\n" +
                           "</soapenv:Envelope>";
}
/*
<ns2:Movement>
                <ns2:RequestNumber>1</ns2:RequestNumber>
                <ns2:BlockReference>
                    <ns2:SystemCode>FlexteraPH</ns2:SystemCode>
                    <ns2:BlockID>777007</ns2:BlockID>
                </ns2:BlockReference>
                <ns2:MovementReference>
                    <ns2:SystemCode>FlexteraPH</ns2:SystemCode>
                    <ns2:MovementID>789003</ns2:MovementID>
                </ns2:MovementReference>
                <ns2:CBAccount>40702810700010003896</ns2:CBAccount>
                <ns2:MovementAmount>777.77</ns2:MovementAmount>
                <ns2:ObjectReference>88000</ns2:ObjectReference>
                <ns2:ExtModule>PHub1</ns2:ExtModule>
                <ns2:ExtOperationCode>PAY</ns2:ExtOperationCode>
                <ns2:Priority>5</ns2:Priority>
                <ns2:OperationTimestamp>2016-01-18T10:30:17Z</ns2:OperationTimestamp>
                <ns2:DrCr>D</ns2:DrCr>
                <ns2:Narrative>test</ns2:Narrative>
                <ns2:UseOverdraft>true</ns2:UseOverdraft>
                <ns2:IgnoreBalance>false</ns2:IgnoreBalance>
                <ns2:IgnoreBlockFlag>false</ns2:IgnoreBlockFlag>
                <ns2:ValueDate>2016-01-18</ns2:ValueDate>
                <ns2:ABSSpecificParameters>
                    <ns2:MIDASSpecificParameters>
                        <ns2:SPOS>PHUB</ns2:SPOS>
                        <ns2:OTRF>88000</ns2:OTRF>
                        <ns2:Department>ITD</ns2:Department>
                        <ns2:ProfitCenter/>
                        <ns2:BookCode/>
                        <ns2:PostingTrnTyp>92200</ns2:PostingTrnTyp>
                        <ns2:PostingNarrative>PH88000</ns2:PostingNarrative>
                        <ns2:ProjectTrnTyp>PH</ns2:ProjectTrnTyp>
                        <ns2:ProjectTrnNbr/>
                        <ns2:ProjectNarrative>PH88000</ns2:ProjectNarrative>
                    </ns2:MIDASSpecificParameters>
                </ns2:ABSSpecificParameters>
            </ns2:Movement>


public void processP(List<MovementCreateData> mcdList) {
        ExecutorService executor = Executors.newFixedThreadPool(mcdList.size() > 10 ? 10 : mcdList.size());

        List<FutureTask<MovementCreateData>> futureTasks = new ArrayList<>();

        for (MovementCreateData item : mcdList) {
            MovementCreateProcessorCallable callable = new MovementCreateProcessorCallable(item);
            FutureTask<MovementCreateData> futureTask = new FutureTask<MovementCreateData>(callable);
            futureTasks.add(futureTask);
            executor.execute(futureTask);
        }

        for (int i = 0; i < ATTEMPTS || !executor.isShutdown(); i++) {
            int quorum = 0;
            for (FutureTask task : futureTasks) {
                if (task.isDone() || task.isCancelled()) {
                    quorum++;
                }
            }

            if (quorum == futureTasks.size()) {
                break;
            }

            try {
                Thread.sleep(PERIOD);
            } catch (InterruptedException e) {
                log.info("",e);
            }
        }

        if(!executor.isShutdown()) {
            executor.shutdown();
        }

        // Если закончилось по таймауту, то надо прописать ошибки
        for (MovementCreateData item : mcdList) {
            if(item.getState()==null){
                item.setState(MovementCreateData.StateEnum.Error);
                item.setErrType(MovementCreateData.ErrorTypes.err3);
            }
        }

        System.out.println();
    }

public void setJMSMessageID(java.lang.String id)
throws JMSException
Set the message ID.
Any value set using this method is ignored when the message is sent, but
this method can be used to change the value in a received message.
 */