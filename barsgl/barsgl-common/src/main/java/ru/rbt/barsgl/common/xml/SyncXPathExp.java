package ru.rbt.barsgl.common.xml;

import org.xml.sax.InputSource;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

/**
 * Created by Ivan Sevastyanov
 */
public class SyncXPathExp implements XPathExpression{

    private final XPathExpression xPathExp;

    public SyncXPathExp(XPathExpression xPathExp) {
        this.xPathExp = xPathExp;
    }

    @Override
    public synchronized Object evaluate(Object item, QName returnType) throws XPathExpressionException {
        return xPathExp.evaluate(item, returnType);
    }

    @Override
    public synchronized String evaluate(Object item) throws XPathExpressionException {
        return xPathExp.evaluate(item);
    }

    @Override
    public synchronized Object evaluate(InputSource source, QName returnType) throws XPathExpressionException {
        return xPathExp.evaluate(source, returnType);
    }

    @Override
    public synchronized String evaluate(InputSource source) throws XPathExpressionException {
        return xPathExp.evaluate(source);
    }
}
