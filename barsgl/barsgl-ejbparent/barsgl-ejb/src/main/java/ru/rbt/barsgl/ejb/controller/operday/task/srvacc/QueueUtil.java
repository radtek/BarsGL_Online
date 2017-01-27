package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by ER22228 on 02.06.2016.
 */
public class QueueUtil {

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
