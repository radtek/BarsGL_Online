package ru.rbt.barsgl.testjavadoc;

import com.sun.javadoc.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;

import static java.lang.String.format;

/**
 * Created by ER18837 on 02.07.15.
 */
public class TestJavadoc {

    public static final String TEST_METHOD = "org.junit.Test";
    public static final String NOTDOC_TAG = "@notdoc";
    public static final String FSD_TAG = "@fsd";

    // TODO написать относительный путь
    private static String xmlPath = "testJavadoc.xml"; //"C:\\dev\\projects\\barsgl.trunk\\barsgl-testjavadoc\\testJavadoc.xml";
    // TODO передавать как параметры командной строки
    private static boolean toWord = false;
    private static boolean toExcel = true;
    private static String docPath = "..\\..\\..\\..\\..\\"; //c:\\dev\\projects\\apidocs\\";
    private static String docName = "Протокол внутреннего тестирования";
    private static String template = "";

    private static javadocOutFile docWord;
    private static javadocOutFile docExcel;

    private static File logFile;
    private static FileOutputStream fos;
    private static OutputStreamWriter osr;
    private static BufferedWriter writer;
    private static boolean isLog = false;

    private static boolean isExcel = false;

    public static boolean start(RootDoc root) {

        isLog = createLog("testJavadoc.txt");
        outLog("Start create javadoc in " + docPath);
        parseOptions(xmlPath);

        if (!initDoc(docPath, docName, template)) {
            outLog("Can't create javadoc file");
            closeLog();
            return false;
        }

        ClassDoc[] classes = root.classes();
        int r = 0;
        for (ClassDoc classDoc : classes) {
            if (!isClassJavadoc(classDoc))
                continue;

            outLog("Class: " + classDoc.name() + "; " + classDoc.commentText());
            boolean classWriten = false;
            MethodDoc[] methods = classDoc.methods();
            for (MethodDoc methodDoc : methods) {
                outLog("Method. " + methodDoc.name() + "; " + methodDoc.commentText());
                if (isMethodTest(methodDoc)) {
                    outLog("Method: " + methodDoc.name() + "; " + methodDoc.commentText());
                    for (Parameter parameter : methodDoc.parameters())
                        outLog("Parameter: " + parameter.toString());

                    if (!classWriten) {
                        writeClass(classDoc.name(), getClassComment(classDoc));
                        classWriten = true;
                    }
                    writeMethod(methodDoc.name(), methodDoc.commentText(), getMethodFsd(methodDoc));
                }
            }
        }
        closeDoc();

        closeLog();
        return true;
    }

    private static boolean parseOptions(String[][] options) {
        for (int i = 0; i < options.length; i++) {
            String opt = options[i][0];
            if (options[i].length > 1)
                opt += ": " + options[i][1];
            outLog("opt: " + opt);
        }
        return true;
    }

    private static boolean parseOptions(String fileName) {
        Document doc = getDocument(fileName);
        if (null == doc)
            return false;

        NodeList nodeList = doc.getElementsByTagName("docpath");
        if (null != nodeList) {
            docPath = nodeList.item(0).getTextContent();
        }
        if (docPath.charAt(docPath.length()-1) != '\\') {
            docPath += '\\';
        }
        File pathDir = new File(docPath);
        if (!pathDir.exists()) {
            pathDir.mkdirs();
        }
        nodeList = doc.getElementsByTagName("docname");
        if (null != nodeList) {
            docName = nodeList.item(0).getTextContent();
        }
        Node rootNode = doc.getChildNodes().item(0);
        nodeList = rootNode.getChildNodes();
        for ( int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (Node.ELEMENT_NODE == node.getNodeType()) {
                if ("excel".equalsIgnoreCase(node.getNodeName()))
                    excelOptions(node);
                else if ("word".equalsIgnoreCase(node.getNodeName()))
                    wordOptions(node);
            }
        }
        outLog("docPath='" + docPath + "'");
        outLog("docName='" + docName + "'");
        outLog("toExcel='" + toExcel + "'");
        outLog("toWord='" + toWord + "'");
        outLog("template='" + template + "'");

        return true;
    }

    private static void excelOptions(Node node) {
        NodeList nodeList = node.getChildNodes();
        for ( int i = 0; i < nodeList.getLength(); i++) {
            Node childNode = nodeList.item(i);
            if (Node.ELEMENT_NODE == childNode.getNodeType()) {
                if ("enabled".equalsIgnoreCase(childNode.getNodeName()))
                    toExcel = Boolean.valueOf(childNode.getTextContent());
            }
        }
    }

    private static void wordOptions(Node node) {
        NodeList nodeList = node.getChildNodes();
        for ( int i = 0; i < nodeList.getLength(); i++) {
            Node childNode = nodeList.item(i);
            if (Node.ELEMENT_NODE == childNode.getNodeType()) {
                if ("enabled".equalsIgnoreCase(childNode.getNodeName()))
                    toWord = Boolean.valueOf(childNode.getTextContent());
                else if ("template".equalsIgnoreCase(childNode.getNodeName()))
                    template = childNode.getTextContent();
            }
        }
    }

    private static Document getDocument(String fileName) {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance().newInstance();
        f.setValidating(false);
        DocumentBuilder builder = null;
        Document doc = null;
        try {
            builder = f.newDocumentBuilder();
            doc = builder.parse(new File(fileName));
        } catch (Exception e) {
            outLog("XML parsing error '" + fileName + "': " + e.getMessage());
            return null;
        }
        return doc;
    }

    private static boolean initDoc(String filePath, String fileName, String template){
        boolean res = true;
        if (toWord) {
            docWord = new javadocWord();
            if (!docWord.init(docPath, docName, template))
                res = false;
        }
        if (toExcel) {
            docExcel = new javadocExcel();
            if (!docExcel.init(docPath, docName, template))
                res = false;
        }
        return res;
    };

    private static void writeClass(String className, String comment){
        if (toWord) {
            docWord.writeClass(className, comment);
        }
        if (toExcel) {
            docExcel.writeClass(className, comment);
        }
    };

    private static void writeMethod(String methodName, String ... params){
        if (toWord) {
            docWord.writeMethod(methodName, params);
        }
        if (toExcel) {
            docExcel.writeMethod(methodName, params);
        }
    };

    private static void closeDoc(){
        if (toWord) {
            docWord.close();
        }
        if (toExcel) {
            docExcel.close();
        }
    };

    private static boolean isClassJavadoc(ClassDoc classDoc) {
        if (classDoc.isAbstract())
            return false;

        for (Tag tag : classDoc.tags()) {
            if (NOTDOC_TAG.equalsIgnoreCase(tag.name()))
                return false;
        }
        return true;
    }

    private static String getClassComment(ClassDoc classDoc) {
        String comment = classDoc.commentText();
        int n = comment.indexOf("\n");
        if (n > 0)
            return comment.substring(n + 1, comment.length());
        else
            return "";
    }

    private  static String getMethodFsd(MethodDoc methodDoc) {
        String fsdString = "";
        for (Tag tag : methodDoc.tags()) {
            if (!"@throws".equalsIgnoreCase(tag.name()))
                outLog(format("Тэг: %s -> %s", tag.name(), tag.text()));
            if (FSD_TAG.equalsIgnoreCase(tag.name()))
                fsdString += (fsdString.isEmpty() ? "" : "\n") + tag.text();
        }
        return fsdString;
    }

    private static boolean isMethodTest(MethodDoc methodDoc) {
        for (AnnotationDesc annotation : methodDoc.annotations()) {
            if (TEST_METHOD.equals(annotation.annotationType().toString())) {
                for (Tag tag : methodDoc.tags()) {
                    if (NOTDOC_TAG.equalsIgnoreCase(tag.name()))
                        return false;
                }
                return true;
            }
        }
        return false;
    }

    private static boolean createLog(String flaeName) {
        logFile = new File(flaeName);
        try {
            logFile.delete();
            logFile.createNewFile();
            fos = new FileOutputStream(logFile);
            osr = new OutputStreamWriter(fos);
            writer = new BufferedWriter(osr);
            return true;
        } catch (IOException e) {
            System.out.println("Create log file error: " + e.getMessage());
            return false;
        }
    }

    public static void outLog(String logStr) {
        if (isLog) {
            try {
                writer.write(logStr);
                writer.newLine();
            } catch (IOException e) {
                isLog = false;
                System.out.println("Write error: " + e.getMessage());
            }
        }
        System.out.println(logStr);
    }

    private static void closeLog() {
        if (isLog) {
            try {
                writer.flush();
            } catch (IOException e) {
                System.out.println("Flush error: " + e.getMessage());
            }
        }
    }
}