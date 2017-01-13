package ru.rbt.barsgl.common.xml;

import org.apache.log4j.Logger;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ivan Sevastyanov
 */
public class XPathExpHolder {

    private static final Logger log = Logger.getLogger(XPathExpHolder.class.getName());

    private final Map<String, XPathExpression> expressionMap;
    private final XPath xPath;

    public XPathExpHolder() {
        this.expressionMap = new HashMap<String, XPathExpression>();
        this.xPath = XPathFactory.newInstance().newXPath();
    }

    public XPathExpression getXPathExpression(String exp, NamespaceContext namespaceContext) {
        XPathExpression expression = expressionMap.get(exp);
        if (expression == null) {
            synchronized (this) {
                expression = expressionMap.get(exp);
                if (expression == null) {
                    if (null != namespaceContext) {
                        xPath.setNamespaceContext(namespaceContext);
                    }
                    try {
                        expression = new SyncXPathExp(xPath.compile(exp));
                    } catch (XPathExpressionException e) {
                        throw new RuntimeException("Failed to compile XPath expression", e);
                    }
                    expressionMap.put(exp, expression);
                }
            }
        }
        return expression;
    }

}
