/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2017
 */
package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

/**
 *
 * @author Andrew Samsonov
 */
public class XmlUtilityLocator {
    private DocumentBuilderFactory documentBuilderFactory;
    private XPathFactory xPathFactory;
    
    private XmlUtilityLocator(){      
    }
    
    public static XmlUtilityLocator getInstance() {
       return DocumentBuilderHolder.INSTANCE;
    }

    private static class DocumentBuilderHolder {
      private static final XmlUtilityLocator INSTANCE = new XmlUtilityLocator();
    }

    public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
      if(documentBuilderFactory == null)
        documentBuilderFactory = DocumentBuilderFactory.newInstance(); 
      return documentBuilderFactory.newDocumentBuilder();
    }
    
    public XPath newXPath(){
      if(xPathFactory == null){
        xPathFactory = XPathFactory.newInstance();        
      }
      return xPathFactory.newXPath();
    }
}
