/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2017
 */
package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import javax.xml.namespace.QName;
import org.w3c.dom.Node;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

/**
 *
 * @author Andrew Samsonov
 */
public class XPathUtility {
// https://blogs.sap.com/2009/12/04/performance-improvements-in-nw-java-applications-with-xml-processing/
  
  private static final String DTM_MANAGER_PROP_NAME = "com.sun.org.apache.xml.internal.dtm.DTMManager";
  private static final String DTM_MANAGER_CLASS_NAME = "com.sun.org.apache.xml.internal.dtm.ref.DTMManagerDefault";
  
  private static boolean speedUpDTMManagerSearch;

  static {
    try {
      speedUpDTMManagerSearch = !isDTMManagerDetermined();
    } catch (SecurityException e) {
      speedUpDTMManagerSearch = false;
      // handle exception here
    }
  }

  private static boolean isDTMManagerDetermined() throws SecurityException {
    return (System.getProperty(DTM_MANAGER_PROP_NAME) != null);
  }

  /*
    increase performance in single thread. but(!!!)
    regress in multi-threaded enviroment
  */
  public static synchronized Object evaluateXPath(
          XPath xPathProcessor,
          String xPath,          
          Node node,
          QName returnType)
          throws XPathExpressionException {
    if (speedUpDTMManagerSearch) {
      synchronized (XPathUtility.class) {
        boolean setDTMManager = !isDTMManagerDetermined();
        if (setDTMManager) {
          System.setProperty(DTM_MANAGER_PROP_NAME, DTM_MANAGER_CLASS_NAME);
        }
        try {
          return xPathProcessor.evaluate(xPath, node, returnType);
        } finally {
          if (setDTMManager) {
            System.clearProperty(DTM_MANAGER_PROP_NAME);
          }
        }
      }
    } else {
      return xPathProcessor.evaluate(xPath, node, returnType);
    }
  }
}
