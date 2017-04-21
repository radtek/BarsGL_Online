package ru.rbt.barsgl.common.xml;

import org.apache.log4j.Logger;
import org.w3c.dom.*;
import ru.rbt.shared.Assert;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.DataInputStream;
import java.io.InputStream;

/**
 * Created by Ivan Sevastyanov
 */
public class DomBuilder {

    private static Logger log = Logger.getLogger(DomBuilder.class);

    public static final String APP_ENCODING = "utf8";

    private static TransformerFactory transFactory = TransformerFactory.newInstance();
//    private transient XPath pat;
    private XPathExpHolder xpathHolder = new XPathExpHolder();

    private Document document;

    public DomBuilder(String xml) throws Exception {
        document = stringToDocument(xml);
    }

    public DomBuilder(Document document) {
        this.document = document;
    }

    public DomBuilder(InputStream inputStream) throws Exception {
        try {
//            log.debug("Init with " + inputStream);
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(true);
            DataInputStream stream = new DataInputStream(inputStream);
            DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
            document = documentBuilder.parse(stream);
        }catch(Exception e) {
            e.printStackTrace();
            log.error(e.getMessage(), e);
            throw new Exception("Exception in forming Document object", e);
        }
    }

    /**
     * В java6 вычитал что XPath не thread-safe... такие пироги...
     * @return
     */
    private static XPath getXPathStatic() {
        return XPathFactory.newInstance().newXPath();
    }

    private Document stringToDocument (String xml) throws Exception {
        try {
//            log.debug("Init with " + xml);
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(true);
            DataInputStream stream = new DataInputStream(new ByteArrayInputStream(xml.getBytes(APP_ENCODING)));
            DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
            return documentBuilder.parse(stream);
        }catch(Exception e) {
            e.printStackTrace();
            log.error(e.getMessage(), e);
            throw new Exception("Exception in forming Document object", e);
        }
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument (Document document) {
        this.document = document;
    }

    void setDataTag (String tagName, String data) {
        log.debug("Setting data=" + tagName + " value=" + data);
        Node targetNode = document.getElementsByTagName(tagName).item(0);
        // if <bla>123</bla>
        if (targetNode.hasChildNodes()) {
            log.debug("found of childs");
            targetNode.setTextContent(data);
        } else {
            // if <bla/>
            log.debug("");
            targetNode.appendChild(document.createTextNode(data));
        }
    }

    String getDataTag (String tagName) {
        Node targetTag = document.getElementsByTagName(tagName).item(0);
        return targetTag.getFirstChild().getTextContent();
    }

    public static Document createDocument (String xml) throws Exception {
        DomBuilder builder = new DomBuilder(xml);
        return builder.getDocument();
    }

    public String getStringValueSetEncoding (boolean prettyPrint, String encoding) throws Exception {
        // Forming the charseqs for removing parasite keys
        char[] sequenceFrom = new char[] {13,13};   String strFrom = new String(sequenceFrom);
        char[] sequenceTo = new char[] {13};        String strTo = new String(sequenceTo);

        Transformer transformer;
        CharArrayWriter output;
        try {
            transformer = transFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
            if (prettyPrint)
            {
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            }

            DOMSource domSource = new DOMSource(document);
            output = new CharArrayWriter();
            StreamResult resultStream = new StreamResult(output);
            transformer.transform(domSource, resultStream);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
            log.error(e.getClass().getName(), e);
            throw new Exception("Exception name: " + e.getClass().getName() + "\n"+
                    e.getMessage(), e);
        }
        return output.toString().replace(strFrom, strTo);
    }

    public String getStringValue(boolean prettyPrint) throws Exception {
        return getStringValueSetEncoding(prettyPrint, APP_ENCODING);
    }

    public String getStringValue() throws Exception {
        return getStringValue(false);
    }

    public final void setNodeValueByPath (String path, String value, NamespaceContext ctx) throws Exception {
        Node target = null == ctx ? getNodeByXPath(path) : getNodeByXPath(path, ctx);
        if (target == null) throw new RuntimeException("Node by xPath: " + path + " was not found");
        target.setTextContent(value);
    }

    public final void setNodeValueByPath (String path, String value) throws Exception {
        setNodeValueByPath(path, value, null);
    }

    public final Node getNodeByXPath (String xPath) throws Exception {
        try {
            Node result = (Node) xpathHolder.getXPathExpression(xPath, null).evaluate(getDocument(), XPathConstants.NODE);
            return result;
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            throw new Exception(e.getMessage(), e);
        }
    }

    public final String getTextByXPath(String xpath) throws Exception {
        Node node = getNodeByXPath(xpath);
        if (node == null) throw new RuntimeException("Node by xPath: " + xpath + " was not found");
        return  node.getTextContent();
    }

    public final String getTextByXPathSecurity(String xpath) throws Exception {
        Node node = getNodeByXPath(xpath);
        if (node == null) {
            return null;
        }
        return  node.getTextContent();
    }

    public final Node getNodeByXPath (final String xPath, NamespaceContext ctx) throws Exception {
        XPath pat = XPathFactory.newInstance().newXPath();
        pat.setNamespaceContext(ctx);
        try {
            Node result = (Node) pat.evaluate(xPath, getDocument(), XPathConstants.NODE);
            return result;
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            throw new Exception(e.getMessage(), e);
        }
    }

    /**
     * Не рекомендуется использовать из-за возможных проблем с производительностью.
     * Лучше использовать XPath в корневом объекте (getNodeByXPath()) например //SomeNode//ChildNode[i]/@id,
     * где i, допустим, переменная цикла.
     * @param xPath
     * @param node
     * @return
     */
    public static Node getNodeByXPathStat (String xPath, Node node) throws Exception {
        try {
            Node result = (Node) getXPathStatic().evaluate(xPath, node, XPathConstants.NODE);
            return result;
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            throw new Exception(e.getMessage(), e);
        }
    }

    public final NodeList getNodeListByXPath (String xPath) throws Exception {
        return getNodeListByXPath(xPath, null);
    }

    public final NodeList getNodeListByXPath (String xPath, NamespaceContext ctx) throws Exception {
        XPath pat = XPathFactory.newInstance().newXPath();
        if (null != ctx)
            pat.setNamespaceContext(ctx);
        try {
            NodeList result = (NodeList) pat.evaluate(xPath, getDocument(), XPathConstants.NODESET);
            return result;
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            throw new Exception(e.getMessage(), e);
        }
    }

    public void importNodeByXPath(String xPath, Document child) throws Exception {
        Node parent = getNodeByXPath(xPath);

        if (parent == null) throw new RuntimeException("Node by xPath: " + xPath + " was not found");

        Node childRoot = child.getFirstChild();
        if (log.isDebugEnabled())
        {
            log.debug("root element class " + childRoot.getClass().getName());
            String interfaces = "root element all interfaces: ";
            for (Class inter : childRoot.getClass().getInterfaces())
            {
                interfaces += " " + inter;
            } // for
            log.debug(interfaces);
        }
        if (childRoot instanceof Comment) {
            childRoot = child.getChildNodes().item(1);
        }
        Assert.notNull(childRoot, "Root element was not found");
        childRoot = this.document.importNode(childRoot, true);
        parent.appendChild(childRoot);
    }

    /**
     * Убирает из dom-дерева все ноды находящиеся по заданному Xpath
     * @param xPath
     */
    public void removeAllNodesByXPath(String xPath)
    {
        removeAllNodesByXPath(xPath, null);
    }

    /**
     * Убирает из dom-дерева все ноды находящиеся по заданному Xpath.
     * Дополнительно использует контекст пространства имен.
     * @param xPath
     * @param nsContext контекст пространства имен, либо null - в этом случае он
     * не учитывается
     */
    public void removeAllNodesByXPath(String xPath, NamespaceContext nsContext)
    {
        XPath pat = XPathFactory.newInstance().newXPath();
        if (null != nsContext)
            pat.setNamespaceContext(nsContext);
        NodeList list = null;
        try
        {
            list = (NodeList) pat.evaluate(xPath, getDocument(), XPathConstants.NODESET);
            if (list == null) throw new RuntimeException("Node by xPath: " + xPath + " was not found");
        }
        catch (XPathExpressionException xpathe)
        {
            throw new IllegalArgumentException(xpathe);
        }
        log.debug("nodes to remove=" + list);
        for (int i = 0; i < list.getLength(); ++i)
        {
            Node toremove = list.item(i);
            log.debug("removing node="+toremove);
            toremove.getParentNode().removeChild(toremove);
        }


    }

    public void importNodeByXPath(String xPath, Document child, NamespaceContext ctx) throws Exception {
        Node parent = getNodeByXPath(xPath, ctx);

        if (parent == null) throw new RuntimeException("Node by xPath: " + xPath + " was not found");

        Node childRoot = child.getFirstChild();
//        log.debug("childRoot="+childRoot);
        if (log.isDebugEnabled())
        {
//            log.debug("root element class " + childRoot.getClass().getName());
            String interfaces = "root element all interfaces: ";
            for (Class inter : childRoot.getClass().getInterfaces())
            {
                interfaces += " " + inter;
            }
            log.debug(interfaces);
        }
        if (childRoot instanceof Comment) {
            childRoot = child.getChildNodes().item(1);
        }
        Assert.notNull(childRoot, "Root element was not found");
        childRoot = this.document.importNode(childRoot, true);
        parent.appendChild(childRoot);
    }

    public void importNodeByXPath(String xPath, Node child) throws Exception {
        Node parent = getNodeByXPath(xPath);

        if (parent == null) throw new IllegalArgumentException("Node by xPath: " + xPath + " was not found");

        Node childRoot = child;
        childRoot = document.importNode(childRoot, true);
        parent.appendChild(childRoot);
    }

    public Document extractDocument(String xPath) throws Exception {
        Node target = getNodeByXPath(xPath);
        DOMImplementation impl = document.getImplementation();
        Document doc = impl.createDocument(null, null, null);
        target = doc.importNode(target, true);
        doc.appendChild(target);
        return doc;
    }

    public static Document createDocument () throws Exception {
        DomBuilder builder = new DomBuilder("<x></x>");
        DOMImplementation impl = builder.getDocument().getImplementation();
        return impl.createDocument(null, null, null);
    }

    public static NodeList getNodeListByXPathStat(String xPath, Node node) throws Exception {
        try {
            return (NodeList) getXPathStatic().evaluate(xPath, node, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            throw new Exception(e.getMessage(), e);
        }
    }

    public void createCDATAByXPath(String xpath, String body) throws Exception {
        CDATASection cdata = document.createCDATASection(body);
        Node target = getNodeByXPath(xpath);
        target.appendChild(cdata);
    }

    /**
     * Добавление CDATA в ноду с неймспейсом
     * @param xpath
     * @param body
     * @param nsctx
     */
    public void createCDATAByXPath(String xpath, String body, NamespaceContext nsctx) throws Exception {
        CDATASection cdata = document.createCDATASection(body);
        Node target = getNodeByXPath(xpath, nsctx);
        target.appendChild(cdata);
    }

    public void importNodeFirstByXPath(String xPath, Document child) throws Exception {
        Node parent = getNodeByXPath(xPath);

        if (parent == null) throw new RuntimeException("Node by xPath: " + xPath + " was not found");

        Node childRoot = child.getFirstChild();
        if (log.isDebugEnabled())
        {
//            log.debug("root element class " + childRoot.getClass().getName());
            String interfaces = "root element all interfaces: ";
            for (Class inter : childRoot.getClass().getInterfaces())
            {
                interfaces += " " + inter;
            }
            log.debug(interfaces);
        }
        if (childRoot instanceof Comment) {
            childRoot = child.getChildNodes().item(1);
        }
        Assert.notNull(childRoot, "Root element was not found");
        childRoot = this.document.importNode(childRoot, true);

        Node first = parent.getFirstChild();
        if (first == null) throw new IllegalStateException("first child element was not found");

        parent.insertBefore(childRoot, first);
    }

    /**
     * Создать новый xml-node по заданному xPath.
     * Производится поиск родительского node и в нем создается новый node с заданным именем, даже если node с таким именем уже существует.
     * Если родительского node не существует, то он создается.
     *
     * @param xPath - путь к node, который необходимо создать
     * @return созданый node
     * @throws Exception - исключение, если задан неверный xPath
     */
    public Node createNodeByXPath(String xPath) throws Exception {
        Node node = null;
        int index = xPath.lastIndexOf('/');
        if (index < 0) {
            throw new Exception("Illegal xPath: " + xPath);
        } else if (index == 0) {
            String nodeName = xPath.substring(1);
            node = document.createElement(nodeName);
            document.appendChild(node);
            return node;
        } else {
            String parentXPath = xPath.substring(0, index);
            Node parentNode = getNodeByXPath(parentXPath);
            if (parentNode == null) {
                parentNode = createNodeByXPath(parentXPath);
            }
            String nodeName = xPath.substring(index + 1);
            node = document.createElement(nodeName);
            parentNode.appendChild(node);
            return node;
        }
    }

    /**
     * Получение документа содержащегося внутри тега, заданного XPath
     * @param path
     * @return
     */
    public Document getInnerDocumentByXPath(String path) throws Exception {
        Node child = getNodeByXPath(path);
        NodeList childNodes = child.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node nextNode = childNodes.item(i);
            if (nextNode.getNodeType() != Node.ELEMENT_NODE) continue;
            String nodeName = nextNode.getNodeName();
            log.debug("nodeName="+nodeName);
            Document doc = extractDocument(path + "/" + nodeName);
            return doc;
        }
        return null;
    }

    /**
     * Получить дочерний node по его наименованию и родительскому node.
     *
     * @param parent - родительский node
     * @param childNodeName - наименование дочернего node
     * @return первый найденый дочерний node с указанным именем; если не найден, то null
     */
    public Node getChildNode(Node parent, String childNodeName) {
        NodeList nodeList = parent.getChildNodes();
        for(int i=0; i<nodeList.getLength();i++){
            Node child = nodeList.item(i);
            if(child.getNodeName().equals(childNodeName)){
                return child;
            }
        }
        return null;
    }

    public boolean isExists(String xpath) throws Exception {
        return getNodeByXPath(xpath) != null;
    }

}
