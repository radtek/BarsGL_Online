package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;


import org.apache.log4j.Logger;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ru.rbt.barsgl.ejb.entity.acc.AclirqJournal;
import ru.rbt.barsgl.ejb.repository.AclirqJournalRepository;
import ru.rbt.barsgl.ejb.repository.WorkdayRepository;
import ru.rbt.barsgl.ejb.security.AuditController;
import ru.rbt.barsgl.ejbcore.AccountQueryRepository;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.util.StringUtils;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static ru.rbt.barsgl.ejb.entity.acc.AclirqJournal.Status.ERROR;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.ifEmpty;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.isEmpty;


/**
 * Created by ER22228 on 29.03.2016.
 */
@SuppressWarnings("ALL")
@Stateless
@LocalBean
public class MasterAccountProcessor extends CommonAccountQueryProcessor implements Serializable {
    private static final Logger log = Logger.getLogger(MasterAccountProcessor.class);

    // количество счетов для "пакетной" обработки (чтобы число SQL запросов сократить)
    private static final int batchSize = 100;

    @EJB
    private AclirqJournalRepository journalRepository;

    @EJB
    private AuditController auditController;

    @EJB
    private CoreRepository coreRepository;

    @EJB
    private AccountQueryRepository queryRepository;

    @Inject
    private WorkdayRepository workdayRepository;


    public String process(String fullTopic, Map<String, String> currencyMap, Map<String, Integer> currencyNBDPMap, long jId) throws Exception {
        return process(fullTopic, jId);
    }

    public String process(String fullTopic, long jId) throws Exception {
        if (!fullTopic.startsWith("<?xml")) {
            fullTopic = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + fullTopic;
        }

        if (fullTopic == null || !fullTopic.contains("MasterAccountPositioningBatchQuery")) {
            journalRepository.updateLogStatus(jId, AclirqJournal.Status.ERROR, "Ошибка при распозновании сообщения");
            // Меняем содержание на ошибку
            return getEmptyBodyMessage();
        }

        String answerBody = processAccountListQuery(fullTopic, jId);
        return answerBody;
    }

    private Set<String> readFromXML(String bodyXML, Long jId) throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        org.w3c.dom.Document doc = null;
        try {
            DocumentBuilder b = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc = b.parse(new ByteArrayInputStream(bodyXML.getBytes("UTF8")));
            if (doc == null) {
                //Ошибка XML
                journalRepository.updateLogStatus(jId, ERROR, "Ошибка при преобразовании входящего XML");
                return null;
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            journalRepository.updateLogStatus(jId, ERROR, "Ошибка при преобразовании входящего XML\n" + e.getMessage());
            throw e;
        }

        XPath xPath = XPathFactory.newInstance().newXPath();
        Set<String> accounts = new HashSet<>();
/*
<asbo:MasterAccountPositioningBatchQuery xmlns:asbo="urn:asbo:barsgl">
	<asbo:MasterAccountPositioningQuery>
		<asbo:CBAccount>40702810000012189256</asbo:CBAccount>MasterAccountPositioningQuery
	</asbo:MasterAccountPositioningQuery>
</asbo:MasterAccountPositioningBatchQuery>
 */
        NodeList queries = ((NodeList) xPath.evaluate("./MasterAccountPositioningQuery", doc.getDocumentElement(), XPathConstants.NODESET));

        for (int i = 0; i < queries.getLength(); i++) {
            String CBAccount = null, IMBAccount = null, IMBBranch = null;
            for (int j = 0; j < queries.item(i).getChildNodes().getLength(); j++) {
                String name = queries.item(i).getChildNodes().item(j).getNodeName();
                String value = queries.item(i).getChildNodes().item(j).getTextContent();
                if (!isEmpty(value) && (name.endsWith("CBAccount") || name.endsWith("IMBAccount"))) {
                    accounts.add(value);
                    break;// потому что или/или
                }
            }
        }
        return accounts;
    }

    private String processAccountListQuery(String fullTopic, Long jId) throws Exception {
        Set<String> countsToProcess = readFromXML(/*"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + */fullTopic, jId);
        String body = createOutMessage(countsToProcess);
        return body;
//        return null;
    }

    private String createOutMessage(Set<String> countsToProcess) throws Exception {
        List<String> stringList = countsToProcess == null || countsToProcess.size() == 0 ? new ArrayList<>() : new ArrayList<>(countsToProcess);
        StringBuilder result = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                     "<asbo:MasterAccountPositioningBatch xmlns:asbo=\"urn:asbo:barsgl\">\n");
        for (int i = 0; i < stringList.size(); i += batchSize) {
            result.append(batchCreateOutMessage(stringList.subList(i, Math.min(i + batchSize, stringList.size()))));
        }

        return result.append("</asbo:MasterAccountPositioningBatch>").toString();
    }

    private static final String dohodnik = "70601810";

    private StringBuilder batchCreateOutMessage(List<String> counts) throws Exception {
        String inCondition = "'" + StringUtils.listToString(counts, "','") + "'";
        List<DataRecord> glAccRecordsRaw = queryRepository.getGlAccRecords(inCondition, null);

        /* Сделали, а потом передумали, но решили оставить на всякий случай
        List<DataRecord> glAccRecordsRaw;
        if (!inCondition.contains(dohodnik)) {
            glAccRecordsRaw = queryRepository.getGlAccRecords(inCondition);
        } else {
            glAccRecordsRaw = new ArrayList<>();
            for (String item : counts){
                List<DataRecord> oneCountList = queryRepository.getGlAccRecords("'"+item+"'");
                // Patch для счетов-доходников
                if ((oneCountList == null || oneCountList.size() == 0) && item.contains(dohodnik)) {
                    oneCountList = queryRepository.getAccrlnRecords("'"+item+"'");
                }

                if (oneCountList != null && oneCountList.size() > 0) {
                    glAccRecordsRaw.addAll(oneCountList);
                }
            }
        }
        */

        List<String> processedBsaacids = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        try {
            for (DataRecord record : glAccRecordsRaw) {
                // Список обработанных, чтобы потом заполнить ответы для счетов, по которым нет информации
                processedBsaacids.add(record.getString("BSAACID"));

                sb.append("<asbo:MasterAccountPositioning>\n");

                sb.append("<asbo:CBAccount>").append(record.getString("BSAACID")).append("</asbo:CBAccount>\n");
                sb.append("<asbo:CustomerNumber>").append(record.getString("CUSTNO")).append("</asbo:CustomerNumber>\n");
                sb.append("<asbo:IMBAccountNo>").append(record.getString("BSAACID")).append("</asbo:IMBAccountNo>\n");
                String branch = record.getString("BRANCH");
                if (!(branch.charAt(0) >= 'A' && branch.charAt(0) <= 'Z')) {
                    branch = ifEmpty(queryRepository.convertBranchToFcc(branch), branch);
                }
                sb.append("<asbo:IMBBranch>").append(branch).append("</asbo:IMBBranch>\n");

                sb.append("<asbo:AccountingType>").append(record.getString("ACCTYPE")).append("</asbo:AccountingType>\n");

                sb.append("</asbo:MasterAccountPositioning>\n");
            }
        } catch (Exception ex) {
            log.error("", ex);
        }

        // Если есть счета, для которых нет данных
        if (counts.size() != processedBsaacids.size()) {
            for (String item : counts) {
                if (processedBsaacids.contains(item)) continue;
                sb.append("<asbo:MasterAccountPositioning>\n");
                sb.append("<asbo:CBAccount>").append(item).append("</asbo:CBAccount>\n");
                sb.append("</asbo:MasterAccountPositioning>\n");
            }
        }

        return sb;
    }

    public static String fullTopicTestA =
//        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
        "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "\t<SOAP-ENV:Header>\n" +
            "\t\t<gbo:UCBRUHeaders xmlns:gbo=\"urn:imb:gbo:v2\">\n" +
            "\t\t\t<gbo:Correlation>\t\t\t\t\n" +
            "\t\t\t\t\t<gbo:XRef>1234567847567488</gbo:XRef>\n" +
            "\t\t\t\t<gbo:Segmentation>\n" +
            "\t\t\t\t\t<gbo:CanSegmentResponse>true</gbo:CanSegmentResponse>\n" +
            "\t\t\t\t</gbo:Segmentation>\n" +
            "\t\t\t</gbo:Correlation>\n" +
            "\t\t\t<gbo:Security/>\n" +
            "\t\t\t<gbo:Audit>\n" +
            "\t\t\t\t<gbo:MessagePath>\n" +
            "\t\t\t\t\t<gbo:Step>\n" +
            "\t\t\t\t\t\t<gbo:Application.Module>String</gbo:Application.Module>\n" +
            "\t\t\t\t\t\t<gbo:VersionId>String</gbo:VersionId>\n" +
            "\t\t\t\t\t\t<gbo:TimeStamp>2001-12-17T09:30:47Z</gbo:TimeStamp>\n" +
            "\t\t\t\t\t\t<gbo:RoutingRole>String</gbo:RoutingRole>\n" +
            "\t\t\t\t\t\t<gbo:Comment>String</gbo:Comment>\n" +
            "\t\t\t\t\t</gbo:Step>\n" +
            "\t\t\t\t</gbo:MessagePath>\n" +
            "\t\t\t\t<gbo:ProcessInfo>\n" +
            "\t\t\t\t\t<gbo:Name>String</gbo:Name>\n" +
            "\t\t\t\t\t<gbo:InstanceId>String</gbo:InstanceId>\n" +
            "\t\t\t\t</gbo:ProcessInfo>\n" +
            "\t\t\t</gbo:Audit>\n" +
            "\t\t\t<gbo:Usability>\n" +
            "\t\t\t\t<gbo:Internationalization>\n" +
            "\t\t\t\t\t<gbo:Language>aa</gbo:Language>\n" +
            "\t\t\t\t</gbo:Internationalization>\n" +
            "\t\t\t\t<gbo:Fetch>\n" +
            "\t\t\t\t\t<gbo:MaxRecords>0</gbo:MaxRecords>\n" +
            "\t\t\t\t\t<gbo:MoreRecordsAvailable>true</gbo:MoreRecordsAvailable>\n" +
            "\t\t\t\t\t<gbo:RecordCount>0</gbo:RecordCount>\n" +
            "\t\t\t\t</gbo:Fetch>\n" +
            "\t\t\t</gbo:Usability>\n" +
            "\t\t\t<gbo:Tools>\n" +
            "\t\t\t\t<gbo:Environment/>\n" +
            "\t\t\t</gbo:Tools>\n" +
            "\t\t</gbo:UCBRUHeaders>\n" +
            "\t</SOAP-ENV:Header>\n" +
            "\t<SOAP-ENV:Body>\n" +
            "\t\t<gbo:AccountListQuery xmlns:gbo=\"urn:imb:gbo:v2\">\n" +
            "\t\t\t<gbo:AccountQuery>\n" +
            "\t\t\t\t<gbo:CustomerNo>00000018</gbo:CustomerNo>\n" +
            "\t\t\t\t<gbo:AccountSpecials>PRCA1</gbo:AccountSpecials>\n" +
            "\t\t\t\t<gbo:AccountSpecials>PRCD2</gbo:AccountSpecials>\n" +
            "\t\t\t\t<gbo:AccountSpecials>9070</gbo:AccountSpecials>\n" +
            "\t\t\t\t<gbo:AccountSpecials>7301</gbo:AccountSpecials>\n" +
            "\t\t\t</gbo:AccountQuery>\n" +
            "\t\t\t<gbo:AccountQuery>\n" +
            "\t\t\t\t<gbo:CustomerNo>00000018</gbo:CustomerNo>\n" +
            "\t\t\t\t<gbo:AccountingType>702010300</gbo:AccountingType>\n" +
            "\t\t\t\t<gbo:AccountingType>758030400</gbo:AccountingType>\n" +
            "\t\t\t</gbo:AccountQuery>\n" +
            "\t\t\t<gbo:AccountQuery>\n" +
            "\t\t\t\t<gbo:CustomerNo>00597197</gbo:CustomerNo>\n" +
            "\t\t\t</gbo:AccountQuery>\n" +
            "\t\t\t<gbo:AccountQuery>\n" +
            "\t\t\t\t<gbo:CustomerNo>00500633</gbo:CustomerNo>\n" +
            "\t\t\t</gbo:AccountQuery>\t\t\t\t\n" +
            "\t\t</gbo:AccountListQuery>\n" +
            "\t</SOAP-ENV:Body>\n" +
            "</SOAP-ENV:Envelope>";


}
