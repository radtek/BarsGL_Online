package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ru.rbt.barsgl.ejb.entity.cust.CustDNJournal;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static ru.rbt.barsgl.ejb.entity.cust.CustDNJournal.Status.ERR_VAL;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;

/**
 * Created by er18837 on 16.03.2018.
 */
abstract public class CommonNotifyProcessor implements Serializable {

    protected abstract void updateLogStatusError(Long jourbnalId, String message);

    protected Map<String, String> readFromXML(String bodyXML, String charsetName, String parentName, XmlParam[] paramNames, Long journalId)
            throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        org.w3c.dom.Document doc = null;
        if (!bodyXML.startsWith("<?xml")) {
            bodyXML = "<?xml version=\"1.0\" encoding=\"" + charsetName + "\"?>\n" + bodyXML;
        }

        try {
            DocumentBuilder b = XmlUtilityLocator.getInstance().newDocumentBuilder();
            doc = b.parse(new ByteArrayInputStream(bodyXML.getBytes(charsetName )));
            if (doc == null) {
                //Ошибка XML
                updateLogStatusError(journalId, "Ошибка при преобразовании входящего XML");
                return null;
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            updateLogStatusError(journalId, "Ошибка при преобразовании входящего XML\n" + e.getMessage());
            throw e;
        }

        NodeList nodes;
        XPath xPath = XmlUtilityLocator.getInstance().newXPath();
        try {
            Element element = doc.getDocumentElement();
            nodes = (NodeList) xPath.evaluate(parentName, element, XPathConstants.NODESET);
            if (nodes == null || nodes.getLength() != 1) {
                nodes = (NodeList) xPath.evaluate("Body" + parentName, element, XPathConstants.NODESET);
                if (nodes == null || nodes.getLength() != 1) {
                    //Ошибка XML
                    updateLogStatusError(journalId, "Отсутствуют неоходимые данные " + parentName);
                    return null;
                }
            }
        } catch (XPathExpressionException e) {
            updateLogStatusError(journalId, "Ошибка при чтении входящего XML\n" + e.getMessage());
            throw e;
        }

        Node parentNode = nodes.item(0);

        Map<String, String> params = new HashMap<>();

        for (XmlParam item : paramNames) {
            params.put(item.fieldName, (String) xPath.evaluate("./" + item.xmlName, parentNode, XPathConstants.STRING));
        }

        return params;
    }

    protected String validateXmlParams(Map<String, String> xmlData, XmlParam[] paramNames) {
        StringBuilder builder = new StringBuilder();
        for (XmlParam item: paramNames) {
            String value = xmlData.get(item.fieldName);
            if (isEmpty(value)) {
                if (!item.nullable)
                    builder.append(String.format("Не задано поле '%s'; ", item.xmlName));
            } else if (value.length() > item.length)
                builder.append(String.format("Длина поля '%s' > %d; ", item.xmlName, item.length));
        }
        return builder.toString();
    }

    protected static class XmlParam {
        String fieldName;
        String xmlName;
        int length;
        boolean nullable;

        public XmlParam(String fieldName, String xmlName, boolean nullable, int length) {
            this.fieldName = fieldName;
            this.xmlName = xmlName;
            this.length = length;
            this.nullable = nullable;
        }
    }


}
