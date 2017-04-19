package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;


import com.ibm.jms.JMSTextMessage;
import com.ibm.mq.jms.*;
import com.ibm.msg.client.wmq.WMQConstants;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import static ru.rbt.ejbcore.util.StringUtils.*;

/**
 * Created by ER22228 on 16.05.2016.
 */
public class CommonAccountQueryProcessor {
    private static final Logger log = Logger.getLogger(CommonAccountQueryProcessor.class);
    public static final int descriptionLength=35;

    protected static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public String prepareDescription(String desc){
        if(isEmpty(desc)) return "";
        desc = desc.replaceAll("&","&amp;");
        desc = desc.substring(0, Integer.min(descriptionLength,desc.length()));
        int index = desc.lastIndexOf("&");
        if(index>descriptionLength-5){
            desc = desc.substring(0,index);
        }
        return desc;
    }

    public String getEmptyInputSetMessage(String message) throws DatatypeConfigurationException {
        String answerBody;
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        DatatypeFactory df = DatatypeFactory.newInstance();
        XMLGregorianCalendar dateTime = df.newXMLGregorianCalendar(calendar);

        answerBody =
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                        "\t\t<asbo:Error xmlns:asbo=\"urn:asbo:barsgl\">\n" +
                        "\t\t\t<asbo:Code>0200</asbo:Code>\n" +
                        "\t\t\t<asbo:Description>" + message + "</asbo:Description>\n" +
                        "\t\t\t<asbo:Source>BarsGL</asbo:Source>\n" +
                        "\t\t\t<asbo:Kind>ERROR</asbo:Kind>\n" +
                        "\t\t\t<asbo:DateTime>" + dateTime.toString() + "</asbo:DateTime>\n" +
                        "\t\t</asbo:Error>\n";
        return answerBody;
    }

    public String toTag(String tagName, String tagValue, int maxLen) {
        tagValue = ifEmpty(tagValue, "");
        tagValue = maxLen > 0 ? substr(tagValue, maxLen) : tagValue;
        return "<asbo:" + tagName + ">" + tagValue + "</asbo:" + tagName + ">\n";
    }

    public String toTag(String tagName, String tagValue) {
        return toTag(tagName, tagValue, 0);
    }

    public String getEmptyBodyMessage() throws DatatypeConfigurationException {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        DatatypeFactory df = DatatypeFactory.newInstance();
        XMLGregorianCalendar dateTime = df.newXMLGregorianCalendar(calendar);

        return
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                        "\t\t<asbo:Error xmlns:asbo=\"urn:asbo:barsgl\">\n" +
                        "\t\t\t<asbo:Status>9999</asbo:Code>\n" +
                        "\t\t\t<asbo:Description>Empty \"body\" section</asbo:Description>\n" +
                        "\t\t\t<asbo:Source>BarsGL</asbo:Source>\n" +
                        "\t\t\t<asbo:Kind>ERROR</asbo:Kind>\n" +
                        "\t\t\t<asbo:DateTime>" + dateTime.toString() + "</asbo:DateTime>\n" + //2016-06-24T16:10:03.010925+03:00
                        "\t\t</asbo:Error>\n";
    }


    // Фрагменты ответного сообщения. Составляется из header и body, которые генерируются JAXB
    private static final String[] envelopTemplates = new String[]{
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:gbo=\"urn:imb:gbo:v2\">\n" +
                    "    <SOAP-ENV:Header>\n" +
                    "        <gbo:UCBRUHeaders>",
            "</gbo:UCBRUHeaders>\n" +
                    "    </SOAP-ENV:Header>\n" +
                    "    <SOAP-ENV:Body>",
            "    </SOAP-ENV:Body>\n" +
                    "</SOAP-ENV:Envelope>"
    };

    public static Date lastDate;

    static {
        try {
            lastDate = DateUtils.parseDate("2029-01-01", "yyyy-MM-dd");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public Object unmarshallJAXB(String topic, Class clazz, Long jId) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        return jaxbUnmarshaller.unmarshal(new StringReader(topic));
    }

    public String marshallJAXB(Object toXML, Class clazz, Long jId) throws JAXBException {
        String result = null;

        JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        StringWriter stringWriter = new StringWriter();
        jaxbMarshaller.marshal(toXML, stringWriter);
        result = stringWriter.toString();

        return result;
    }

    public XMLGregorianCalendar dateToXML(Date date) {
        GregorianCalendar c = new GregorianCalendar();
        c.setTime(date);
        try {
            XMLGregorianCalendar date2 = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
            return date2;
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String[] getInfoFromSOAPEnvelope(String envelope) {
        String errorMessage = "";
        envelope = envelope.replace("\n", "").replace("\r", "").replace("\t", "");
        String body = ifEmpty(envelope.replaceAll(".*<(.*[B|b]ody)>(.*)</\\1>.*", "$2"), "").trim();
        if (isEmpty(body) || envelope.equals(body)) {
            // Записать ошибку
            errorMessage += "Неправильный формат сообщения. Раздел <body> отсутствует.";
        }

        return new String[]{body, null, errorMessage};
    }


    public void sendToQueue(String outMessage, QueueProperties queueProperties, String[] incMessage, String params2) throws JMSException {
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
            MQQueue queueOut = (MQQueue) session.createQueue(!isEmpty(incMessage[2]) ? incMessage[2] : "queue:///" + params2);
            MQQueueSender sender = (MQQueueSender) session.createSender(queueOut);
            sender.send(message);
            sender.close();

            session.close();
            connection.close();
            log.info("Сессия обработки очередей завершена");
        } finally{
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

}
