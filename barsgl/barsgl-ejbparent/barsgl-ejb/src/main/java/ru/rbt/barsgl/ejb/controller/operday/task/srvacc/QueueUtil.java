package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by ER22228 on 02.06.2016.
 */
public class QueueUtil {

    public static synchronized Object unmarshallJAXB(String topic, Class clazz) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        return jaxbUnmarshaller.unmarshal(new StringReader(topic));
    }

    public static synchronized String marshallJAXB(Object toXML, Class clazz) throws JAXBException {
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

    public static synchronized XMLGregorianCalendar dateToXML(Date date) {
        if(date==null) return null;
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
}
