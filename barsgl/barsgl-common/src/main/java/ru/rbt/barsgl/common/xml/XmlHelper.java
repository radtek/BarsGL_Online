package ru.rbt.barsgl.common.xml;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.builder.Builder;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.*;
import ru.rbt.shared.Assert;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.AbstractList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.w3c.dom.Node.*;

/**
 * final String orderNum = getOptionalElement(node, "//*[local-name()='Posting']/pos:OrderNo", nsc, String.class);
 */
public class XmlHelper {
    private static final Logger log = Logger.getLogger(XmlHelper.class);

    private static final String XML_SCHEMA_INSTANCE_NAMESPASE = "http://www.w3.org/2001/XMLSchema-instance";

    private static final XPathExpHolder expHolder = new XPathExpHolder();
    /*private static final SyncDomBuilder domBuilder = new SyncDomBuilder();
    private static final SyncDomSerializer domSerializer = new SyncDomSerializer();*/

    private XmlHelper() {}

/*
    public static Document buildDom(String xml) throws RuntimeException {
        try {
            return domBuilder.buildDom(xml);
        } catch (SAXException e) {
            log.error("Failed to parse xml:\n" + xml, e);
            throw new RuntimeException("Не удалось произвести парсинг XML-документа");
        }
    }

    public static String serializeDom(Document document) {
        return domSerializer.serializeDom(document);
    }

    public static String serializeDomWithoutNamespaceDeclaration(Document document) {
        return domSerializer.serializeDomWithoutNamespaceDeclaration(document);
    }
*/

    public static List<Node> getNodes(Node node, String exp, NamespaceContext nsc) throws RuntimeException {
        final XPathExpression xPathExp = expHolder.getXPathExpression(exp, nsc);
        final NodeList nodes;
        try {
            nodes = (NodeList) xPathExp.evaluate(node, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            log.error("Failed to evaluate XPath expression: " + exp, e);
            throw new RuntimeException("Ошибка извлечения по XPath-выражению: " + exp);
        }
        return (nodes != null) ? new ImmutableListOfNodes(nodes) : Collections.<Node>emptyList();
    }

    public static Node getSingleNode(Node parent, String exp, NamespaceContext nsc) throws RuntimeException {
        final Node node = getNode(parent, exp, nsc);

        if (node == null) {
            log.error("Failed to evaluate XPath expression: " + exp + ". One element was expected");
            throw new RuntimeException("Ошибка извлечения по XPath-выражению: " + exp + ". Ожидался один элемент");
        }

        return node;
    }

    public static Node getNode(Node parent, String exp, NamespaceContext nsc) throws RuntimeException {
        try {
            return Iterables.getOnlyElement(getNodes(parent, exp, nsc), null);
        } catch (IllegalArgumentException e) {
            log.error("Failed to evaluate XPath expression: " + exp + ". Found more than one element");
            throw new RuntimeException("Ошибка извлечения по XPath-выражению: " + exp + ". Найдено несколько элементов");
        }
    }

    public static <T> T getMandatoryElement(Node node, String exp, NamespaceContext nsc, Class<T> clazz, String attrName) throws RuntimeException {
        final Requisite<T> requisite = getRequisite(node, exp, nsc, clazz, false);

        if (!requisite.isPresent()) {
            throw new RuntimeException("Не указан обязательный реквизит '" + attrName + "' (xpath: " + exp + ')');
        }

        return requisite.getValue();
    }

    public static <T> T getOptionalElement(Node node, String exp, NamespaceContext nsc, Class<T> clazz) throws RuntimeException {
        return getOptionalElement(node, exp, nsc, clazz, true);
    }

    public static <T> T getOptionalElement(Node node, String exp, NamespaceContext nsc, Class<T> clazz, boolean nilable) throws RuntimeException {
        Requisite<T> requisite = getRequisite(node, exp, nsc, clazz, nilable);

        // Отсутствующие и пустые (с атрибутом xsi:nill) атрибуты пока что одно и то же и мапятся на java null
        // Если понадобится различать эти 2 ситуации, то нужно переделать. Как вариант, типы полей наших транспортных POJO
        // заменить на Requisite
        return requisite.isPresent() ? requisite.getValue() : null;
    }

    public static boolean isElementPresent(Node node, String exp, NamespaceContext nsc) throws RuntimeException {
        final XPathExpression xPathExp = expHolder.getXPathExpression(exp, nsc);
        final Node elem;
        try {
            elem = (Node) xPathExp.evaluate(node, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            log.error("Failed to evaluate XPath expression: " + exp, e);
            throw new RuntimeException("Ошибка извлечения по XPath-выражению: " + exp);
        }
        return elem != null && !hasXsiNilTrueAttribute(elem);
    }


    @SuppressWarnings({"unchecked"})
    public static <T> Requisite<T> getRequisite(Node node, String exp, NamespaceContext nsc, Class<T> clazz, boolean nilable) throws RuntimeException {
        Requisite<String> req = getTextRequisite(node, exp, nsc);

        // Check possibility for nil values
        if (req.isPresent() && req.getValue() == null && !nilable) {
            throw new RuntimeException("Unexpected xsi:nil value for [" + exp + "]");
        }

        // For absent or nil requisite need not custom transformations
        if (!req.isPresent() || req.getValue() == null) {
            return (Requisite<T>) req;
        }

        final Requisite<T> result;

        if (String.class == clazz) {
            result = (Requisite<T>) req;
        } else if (BigDecimal.class == clazz) {
            try {
                result = (Requisite<T>) Requisite.forPresent(new BigDecimal(req.getValue()));
            } catch (NumberFormatException e) {
                throw new RuntimeException("Cannot convert element [" + exp + "] with value [" + req.getValue() + "] to BigDecimal");
            }
        } else if (BigInteger.class == clazz) {
            try {
                result = (Requisite<T>) Requisite.forPresent(new BigInteger(req.getValue()));
            } catch (NumberFormatException e) {
                throw new RuntimeException("Cannot convert element [" + exp + "] with value [" + req.getValue() + "] to BigInteger");
            }
        } else if (Integer.class == clazz) {
            try {
                result = (Requisite<T>) Requisite.forPresent(Integer.valueOf(req.getValue()));
            } catch (NumberFormatException e) {
                throw new RuntimeException("Cannot convert element [" + exp + "] with value [" + req.getValue() + "] to Integer");
            }
        } else if (Long.class == clazz) {
            try {
                result = (Requisite<T>) Requisite.forPresent(Long.valueOf(req.getValue()));
            } catch (NumberFormatException e) {
                throw new RuntimeException("Cannot convert element [" + exp + "] with value [" + req.getValue() + "] to Long");
            }
        } else if (Date.class == clazz) {
            /*try {
                result = (Requisite<T>) Requisite.forPresent(XmlDateTimeFormatter.parseXmlDateTime(req.getValue()));
            } catch (XmlDateTimeFormatter.IllegalXmlDateTimeFormatException e) {
                throw new RuntimeException("Cannot convert element [" + exp + "] with value [" + req.getValue() + "] to DateTime");
            }*/
            try {
                result  = (Requisite<T>) Requisite.forPresent(DateUtils.parseDate(req.getValue(), "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd"));
            } catch (ParseException e) {
                throw new RuntimeException("Cannot convert element [" + exp + "] with value [" + req.getValue() + "] to Boolean");
            }
        } else if (Boolean.class == clazz) {
            Boolean b = null;
            if ("true".equals(req.getValue()) || "1".equals(req.getValue())) {
                b = true;
            } else if ("false".equals(req.getValue()) || "0".equals(req.getValue())) {
                b = false;
            } else {
                throw new RuntimeException("Cannot convert element [" + exp + "] with value [" + req.getValue() + "] to Boolean");
            }
            result = (Requisite<T>) Requisite.forPresent(b);
        } else if (Enum.class == clazz.getSuperclass()) {
            try {
                result = (Requisite<T>) Requisite.forPresent(Enum.valueOf((Class<Enum>) clazz, req.getValue()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Cannot convert element [" + exp + "] with value [" + req.getValue() + "] to Enum [" + clazz.getSimpleName() + "]");
            }
        } else {
            throw new IllegalArgumentException("Class [" + clazz.getSimpleName() + "] of xml requisite not supported yet");
        }

        return result;
    }

    public static Requisite<String> getTextRequisite(Node node, String exp, NamespaceContext nsc) throws RuntimeException {
        final XPathExpression xPathExp = expHolder.getXPathExpression(exp, nsc);

        final Node elem;
        try {
            elem = (Node) xPathExp.evaluate(node, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            log.error("Failed to evaluate XPath expression: " + exp, e);
            throw new RuntimeException("Ошибка извлечения по XPath-выражению: " + exp);
        }

        if (elem == null) {
            return Requisite.forAbsent();
        } else if (hasXsiNilTrueAttribute(elem)) {
            return Requisite.forPresent(null);
        }

        return Requisite.forPresent(getNodeTextValue(elem));
    }

    public static String getNodeTextValue(final Node node) {

        final short nodeType = node.getNodeType();
        if (nodeType == ELEMENT_NODE) {
            for (Node n = node.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n.getNodeType() == TEXT_NODE) {
                    return n.getNodeValue();
                }
            }
        } else if (nodeType == ATTRIBUTE_NODE) {
            return node.getNodeValue();
        }

        return "";
    }

    public static boolean hasXsiNilTrueAttribute(Node node) {

        final NamedNodeMap attributes = node.getAttributes();

        if (attributes == null) {
            return false;
        }

        final Node xsiNilAttr = attributes.getNamedItemNS(XML_SCHEMA_INSTANCE_NAMESPASE, "nil");

        if (xsiNilAttr == null) {
            return false;
        }

        final String xsiNilAttrValue = xsiNilAttr.getNodeValue();
        return "true".equals(xsiNilAttrValue) || "1".equals(xsiNilAttrValue);
    }

    public static String replaceRootElementNamespacePrefix(String xml, String prefix) {
        if (xml == null || prefix == null) {
            return xml;
        }

        final StringBuilder sb = new StringBuilder(xml);

        final int i = sb.indexOf("?>");
        if (i >= 0) {
            sb.delete(0, i + 2);
        }

        try {
            return sb
                    .replace(sb.indexOf("<") + 1, sb.indexOf(":"), prefix)
                    .replace(sb.lastIndexOf("</") + 2, sb.lastIndexOf(":"), prefix)
                    .toString();
        } catch (StringIndexOutOfBoundsException x) {
            return xml;
        }
    }

    public static void replaceTextNode(Node node, String exp, NamespaceContext nsc, String newValue) throws RuntimeException {
        final XPathExpression xPathExp = expHolder.getXPathExpression(exp, nsc);

        try {
            final Node elem = (Node) xPathExp.evaluate(node, XPathConstants.NODE);
            if (elem != null && elem.getFirstChild() != null) {
                elem.getFirstChild().setNodeValue(newValue);
            }
        } catch (XPathExpressionException e) {
            log.error("Failed to evaluate XPath expression: " + exp, e);
            throw new RuntimeException("Ошибка извлечения по XPath-выражению: " + exp);
        }
    }

    public static Element createComplexElement(Document document, String tagName, Node ... children) {
        final Element result = document.createElement(tagName);
        for (Node item: children) {
            result.appendChild(item);
        }

        return result;
    }

    public static Element createComplexElement(Document document, String tagName, String value) {
        return createComplexElement(document, tagName, document.createTextNode(value));
    }

    private static class ImmutableListOfNodes extends AbstractList<Node> {

        private final NodeList nodeList;

        private ImmutableListOfNodes(NodeList nodeList) {
            this.nodeList = Assert.notNull(nodeList);
        }

        @Override
        public Node get(int index) {
            return (0 <= index && index < size()) ? nodeList.item(index) : null;
        }

        @Override
        public int size() {
            return nodeList.getLength();
        }
    }


    private static class Requisite<T> {
        private final T value;
        private final boolean present;

        @SuppressWarnings({"unchecked"})
        private static final Requisite ABSENT = new Requisite(null, false);

        private Requisite(T value, boolean present) {
            this.value = value;
            this.present = present;
        }

        public static <T> Requisite<T> forPresent(T value) {
            return new Requisite<T>(value, true);
        }

        @SuppressWarnings({"unchecked"})
        public static <T> Requisite<T> forAbsent() {
            return ABSENT;
        }

        public T getValue() {
            return value;
        }

        public boolean isPresent() {
            return present;
        }
    }

    // =============================================================================================================

    public static class NodeBuilder implements Builder<Node> {
        private Document document;
        private String nodeTag;
        private List<Node> children = Lists.newArrayList();

        public NodeBuilder(Document document, String nodeTag) {
            this.document = document;
            this.nodeTag = nodeTag;
        }

        public static NodeBuilder newNodeBuilder(Document document, String nodeTag) {
            return new NodeBuilder(document, nodeTag);
        }

        @Override
        public Node build() {
            final Node result = XmlHelper.createComplexElement(document, nodeTag, children.toArray(new Node[children.size()]));
            return result;
        }

        public NodeBuilder appendChild(Node node) {
            this.children.add(node);
            return this;
        }

        public NodeBuilder appendChild(NodeBuilder node) {
            this.children.add(node.build());
            return this;
        }

        public NodeBuilder appendChild(String tagName, String value) {
            return appendChild(XmlHelper.createComplexElement(document, tagName, value));
        }

        public NodeBuilder withTextValue(String value) {
            appendChild(document.createTextNode(value));
            return this;
        }
    }
}
