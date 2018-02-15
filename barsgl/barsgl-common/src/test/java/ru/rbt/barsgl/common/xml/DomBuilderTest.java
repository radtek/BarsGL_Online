package ru.rbt.barsgl.common.xml;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Ivan Sevastyanov
 */
public class DomBuilderTest {

    public static final Logger log = Logger.getLogger(DomBuilderTest.class);

    @Test public void testDomBuilder () throws Exception {
        String parentStr = "<parent></parent>";
        String childStr = "<child><par4 er='88899' er2=''/></child>";
        DomBuilder builder = new DomBuilder(parentStr);
        DomBuilder child = new DomBuilder(childStr);
        builder.importNodeByXPath ("/parent", child.getDocument());
        Assert.assertNotNull(builder.getNodeByXPath("/parent/child/par4/@er"));

        builder.setNodeValueByPath("/parent/child/par4/@er", "45600");
        Assert.assertEquals(builder.getNodeByXPath("/parent/child/par4/@er").getTextContent(), "45600");
        Assert.assertEquals("", builder.getNodeByXPath("//par4/@er2").getTextContent());
    }

    @Test public void test_extractDocument ()  throws Exception {
        String XML =
                "<parent>" +
                        "<child id=\"57\" />" +
                        "</parent>";
        DomBuilder builder = new DomBuilder(XML);
        Document doc = builder.extractDocument("/parent/child");
        DomBuilder new2 = new DomBuilder(doc);
        log.debug(new2.getStringValue());
        Assert.assertEquals(new2.getNodeByXPath("/child/@id").getTextContent(), "57");
    }

    @Test public void testGetNodeByXPATH ()  throws Exception {
        String XML =
                "<parent>" +
                        "<child id=\"57\" id2=\"44\">" +
                        "test123" +
                        "</child>" +
                        "<child id=\"57\" id2=\"42\">" +
                        "test1255" +
                        "</child>" +
                        "<some id=\"57\" id2=\"42\">" +
                        "test1255" +
                        "</some>" +
                        "<some.class.Name>" +
                        "test1251" +
                        "</some.class.Name>" +
                        "<crazy id=\"1\" id2=\"42\">" +
                        "<row id='77'>" +
                        "test1257" +
                        "</row>" +
                        "<row id='78'>" +
                        "test1258" +
                        "</row>" +
                        "</crazy>" +
                        "<crazy id=\"2\" id2=\"42\">" +
                        "<row id='77'>" +
                        "test1257" +
                        "</row>" +
                        "<row id='80'>" +
                        "test1280" +
                        "</row>" +
                        "</crazy>" +
                        "<crazy1 id=\"2\" id2=\"42\">" +
                        "<row1 id='77'>" +
                        "test1257" +
                        "</row1>" +
                        "<row2 id='80'>" +
                        "test1280" +
                        "</row2>" +
                        "</crazy1>" +
                        "<crazy1 id=\"2\" id2=\"42\">" +
                        "<row1 id='77'>" +
                        "test1257" +
                        "</row1>" +
                        "<row2 id='81'>" +
                        "test1281" +
                        "</row2>" +
                        "</crazy1>" +
                        "</parent>";
        DomBuilder builder = new DomBuilder(XML);
        Node node = builder.getNodeByXPath("/parent/child[@id=57 and @id2=44]");
        Assert.assertTrue(node.getTextContent().equals("test123"));

        Assert.assertNotNull(builder.getNodeByXPath("//some"));
        Assert.assertNotNull(builder.getNodeByXPath("//some.class.Name"));

        NodeList list = builder.getNodeListByXPath("/parent/child");
        Assert.assertTrue(list.getLength() == 2);
        log.debug("------- " + list.item(0));
        Assert.assertTrue(list.item(0).getTextContent().equals("test123"));

        NodeList listCrazy = builder.getNodeListByXPath("/parent/crazy");
        Assert.assertTrue(list.getLength()+"", listCrazy.getLength() == 2);
        Assert.assertTrue(DomBuilder.getNodeByXPathStat("row[@id=77]", listCrazy.item(0)).getTextContent().equals("test1257"));
        Assert.assertNull(DomBuilder.getNodeByXPathStat("row[@id=80]", listCrazy.item(0)));

        Assert.assertTrue(DomBuilder.getNodeByXPathStat("row[@id=80]", listCrazy.item(1)).getTextContent().equals("test1280"));

        NodeList rows = DomBuilder.getNodeListByXPathStat("/parent/crazy[@id=2]/row", builder.getDocument());
        Assert.assertTrue(rows.getLength() == 2);
        Assert.assertTrue(rows.item(0).getTextContent(), rows.item(0).getTextContent().equals("test1257"));
        Assert.assertTrue(rows.item(1).getTextContent().equals("test1280"));

        NodeList crazy1Rows = DomBuilder.getNodeListByXPathStat("/parent/crazy1", builder.getDocument());
        log.debug(crazy1Rows.item(0));
        Assert.assertTrue(crazy1Rows.getLength()+"", crazy1Rows.getLength() == 2);

        Assert.assertTrue(DomBuilder.getNodeByXPathStat("row2", crazy1Rows.item(1)).getTextContent().equals("test1281"));

    }

    @Test public void testStaticXPATH()  throws Exception{
        String str =
                "<request>" +
                        "<object>" +
                        "<property name='series'>" +
                        "<eq>12</eq>" +
                        "</property>" +
                        "<property name='number'>" +
                        "<eq>15</eq>" +
                        "</property>" +
                        "</object>" +
                        "</request>";
        DomBuilder dom = new DomBuilder(str);
        NodeList list = dom.getNodeListByXPath("/request/object/property");
        Assert.assertTrue(list.getLength()+"", list.getLength() == 2);
        Node node = list.item(0);
        Assert.assertNotNull(node);
        Assert.assertNotNull(DomBuilder.getNodeByXPathStat("@name", node));
        Assert.assertTrue(DomBuilder.getNodeByXPathStat("@name", node).getTextContent().equals("series"));
        Assert.assertTrue(DomBuilder.getNodeByXPathStat("eq", node).getTextContent().equals("12"));
        node = list.item(1);
        Assert.assertTrue(DomBuilder.getNodeByXPathStat("@name", node).getTextContent().equals("number"));
    }

    @Test public void testDoc ()  throws Exception {
        String str =
                "<request>" +
                        "<object>" +
                        "<property name='series'>" +
                        "<eq>12</eq>" +
                        "</property>" +
                        "<property name='number'>" +
                        "<eq>15</eq>" +
                        "</property>" +
                        "</object>" +
                        "</request>";
        Document document = new DomBuilder(str).getDocument();
        XPath xpath = XPathFactory.newInstance().newXPath();
        String expression = "/request/object";
        Node objectNode = (Node) xpath.evaluate(expression, document, XPathConstants.NODE);
        Assert.assertNotNull(objectNode);

        xpath = XPathFactory.newInstance().newXPath();
        expression = "property";
        NodeList list = (NodeList) xpath.evaluate(expression, objectNode, XPathConstants.NODESET);
        Assert.assertNotNull(list);
        Assert.assertTrue(list.getLength() == 2);

        DomBuilder docBuilder = new DomBuilder(document);
        Assert.assertTrue(docBuilder.getNodeByXPath("/request").getFirstChild().getNodeName().equals("object"));

    }

    @Test public void testSomeNodeToDocument ()  throws Exception {
        String testVal = "vaH";
        String xmlStr =
                "<doc>" +
                        "<object>" +
                        "<value abc='" + testVal + "'/>" +
                        "</object>" +
                        "</doc>";

        DomBuilder dom = new DomBuilder(xmlStr);

        Node objNode = dom.getNodeByXPath("/doc").getFirstChild();
        log.debug("node=" + objNode.getNodeName());

        DomBuilder domObj = new DomBuilder(DomBuilder.createDocument());
        domObj.importNodeByXPath("/", objNode);

        log.debug(domObj.getStringValue());

        Assert.assertTrue(domObj.getNodeByXPath("/object/value/@abc").getTextContent().equals(testVal));
    }

    @Test public void testNameSpaceAware ()  throws Exception {
        String testVal = "vaH";
        String xmlStr =
                "<doc:rootEl xmlns:doc=\"http://www.fms.gov.ru/services/passport\">" +
                        "<doc:object>" +
                        "<doc:value abc='" + testVal + "'/>" +
                        "</doc:object>" +
                        "</doc:rootEl>";
        final String namespace = "http://www.fms.gov.ru/services/passport";

        DomBuilder dom = new DomBuilder(xmlStr);

        NamespaceContext ctx = new NamespaceContext() {
            public String getNamespaceURI(String prefix) {
                return namespace;
            }

            public String getPrefix(String namespaceURI) {
                return "doc";
            }

            public Iterator getPrefixes(String namespaceURI) {
                List list = new ArrayList(1);
                list.add("doc");
                return list.iterator();
            }
        };

        Node objNode = dom.getNodeByXPath("/doc:rootEl/doc:object/doc:value/@abc", ctx);
        Assert.assertTrue(objNode.getTextContent().equals(testVal));

        String inner = "<doc:value2 abc='"+testVal+"' xmlns:doc='http://www.fms.gov.ru/services/passport'/>";
        DomBuilder innDom = new DomBuilder(inner);

        dom.importNodeByXPath("/doc:rootEl/doc:object", innDom.getDocument(), ctx);
        objNode = dom.getNodeByXPath("/doc:rootEl/doc:object/doc:value2/@abc", ctx);
        Assert.assertTrue(objNode.getTextContent().equals(testVal));

        log.debug(dom.getStringValue());

    }

    @Test public void testCDATA ()  throws Exception {
        String xmlString =
                "<test>" +
                        "<inner-tag/>" +
                        "</test>";
        DomBuilder dom = new DomBuilder(xmlString);

        String text = "<test-data>";
        dom.createCDATAByXPath("/test/inner-tag", text);
        Assert.assertTrue(dom.getNodeByXPath("/test/inner-tag").getFirstChild() instanceof CDATASection);
        Assert.assertTrue(dom.getNodeByXPath("/test/inner-tag").getTextContent().equals(text));

        log.debug(dom.getStringValue());

    }

    @Test public void testInsertBefore()  throws Exception {
        String xml =
                "<root>" +
                        "<child1/>" +
                        "</root>";
        String childStr2 = "<child2/>";
        DomBuilder dom = new DomBuilder(xml);
        DomBuilder child2 = new DomBuilder(childStr2);
        dom.importNodeFirstByXPath("/root", child2.getDocument());

        NodeList nodeList = dom.getNodeByXPath("/root").getChildNodes();

        log.debug("result:\n" + dom.getStringValue());

        Assert.assertTrue(nodeList.item(0).getNodeName(), nodeList.item(0).getNodeName().equals("child2"));
    }

    @Test public void testInsertLast()  throws Exception {
        String xml =
                "<root>" +
                        "<child1/>" +
                        "<child4/>" +
                        "</root>";
        String childStr2 = "<child2/>";
        DomBuilder dom = new DomBuilder(xml);
        DomBuilder child2 = new DomBuilder(childStr2);
        dom.importNodeByXPath("/root", child2.getDocument());

        NodeList nodeList = dom.getNodeByXPath("/root").getChildNodes();

        log.fatal("result:\n" + dom.getStringValue());

        Assert.assertEquals("child2", nodeList.item(2).getNodeName());
    }

    @Test public void removeNodeList ()  throws Exception {
        String xml =
                "<root>" +
                        "<child1/>" +
                        "<child2/>" +
                        "<child3/>" +
                        "</root>";
        DomBuilder dom = new DomBuilder(xml);
        Assert.assertTrue(dom.getNodeListByXPath("/root/*").getLength() > 0);
        dom.removeAllNodesByXPath("/root/*");
        Assert.assertTrue(dom.getNodeListByXPath("/root/*").getLength() == 0);

    }

    @Test public void testText()  throws Exception {
        DomBuilder dom = new DomBuilder(
                "<s1>" +
                        "<in attr1='10' attr2='11'/>" +
                        "<in attr1='20' attr2='22'/>" +
                        "<in attr1='30' attr2='33'/>" +
                        "</s1>");
        Assert.assertEquals("10", dom.getNodeListByXPath("//in").item(0).getAttributes().getNamedItem("attr1").getTextContent());
        Assert.assertEquals("11", dom.getNodeListByXPath("//in").item(0).getAttributes().getNamedItem("attr2").getTextContent());
        Assert.assertEquals("20", dom.getNodeListByXPath("//in").item(1).getAttributes().getNamedItem("attr1").getTextContent());
        Assert.assertEquals("30", dom.getNodeListByXPath("//in").item(2).getAttributes().getNamedItem("attr1").getTextContent());

        Assert.assertEquals("10", dom.getNodeByXPath("//in[1]/@attr1").getTextContent());
        Assert.assertEquals("22", dom.getNodeByXPath("//in[2]/@attr2").getTextContent());
        Assert.assertEquals("33", dom.getNodeByXPath("//in[3]/@attr2").getTextContent());

    }

    @Test public void testXPathTagName()  throws Exception {
        DomBuilder dom = new DomBuilder(
                "<t1>" +
                        "<a1/>" +
                        "<c1 attr='2'>" +
                        "<b1>456</b1>" +
                        "</c1>" +
                        "<c1 attr='1'>" +
                        "<b1>123</b1>" +
                        "<b2>0909</b2>" +
                        "</c1>" +
                        "</t1>");
        Assert.assertEquals("c1", dom.getNodeByXPath("//c1[b1 = '123']").getNodeName());
        Assert.assertEquals("1", dom.getNodeByXPath("//c1[b1 = '123' and b2='0909']").getAttributes().getNamedItem("attr").getTextContent());
        Assert.assertEquals("2", dom.getNodeByXPath("//c1[b1 = '456']").getAttributes().getNamedItem("attr").getTextContent());
        Assert.assertEquals("1", dom.getNodeByXPath("//c1[count(b2) = 1]").getAttributes().getNamedItem("attr").getTextContent());
        Assert.assertEquals("1", dom.getNodeByXPath("//c1[count(*) = 2]").getAttributes().getNamedItem("attr").getTextContent());
        Assert.assertEquals("2", dom.getNodeByXPath("//c1[count(*) = 1]").getAttributes().getNamedItem("attr").getTextContent());
    }

    @Test public void testGetNodeByChildContents()  throws Exception {
        DomBuilder dom = new DomBuilder(
                "<x1>" +
                        "<ch1 a1='1'>" +
                        "<t1>1</t1>" +
                        "<ch2>" + "124" +
                        "<ch3>" + "001" + "</ch3>" +
                        "</ch2>" +
                        "</ch1>" +
                        "<ch1 a1='2'>" +
                        "<t1>2</t1>" +
                        "<ch2>" + "123" +
                        "<ch3>" + "000" + "</ch3>" +
                        "</ch2>" +
                        "</ch1>" +
                        "</x1>");
        Assert.assertEquals("2", dom.getNodeByXPath("//x1/ch1[ch2/text() = '123' and ch2/ch3 = '000']/@a1").getTextContent());
        Assert.assertEquals("2", dom.getNodeByXPath("//x1/ch1[ch2/text() = '123' and t1 = '2']/@a1").getTextContent());
    }

    @Test public void testTextSame()  throws Exception {
        DomBuilder dom = new DomBuilder(
                "<document>" +
                        "<series>1212</series>" +
                        "</document>");
        Assert.assertNotNull(dom.getNodeByXPath("//series[text() = '1212']"));
        Assert.assertEquals("series", dom.getNodeByXPath("//series[text() = '1212']").getNodeName());
    }
}
