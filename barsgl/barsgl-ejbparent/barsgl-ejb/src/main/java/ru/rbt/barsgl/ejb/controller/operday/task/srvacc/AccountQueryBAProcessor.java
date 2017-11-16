package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;


import org.apache.log4j.Logger;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ru.rbt.barsgl.ejb.repository.AclirqJournalRepository;
import ru.rbt.barsgl.ejb.repository.WorkdayRepository;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.AccountQueryRepository;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.StringUtils;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

import static ru.rbt.barsgl.ejb.entity.acc.AclirqJournal.Status.ERROR;
import static ru.rbt.ejbcore.util.StringUtils.*;


/**
 * Created by ER22228 on 29.03.2016.
 */
@Deprecated
@SuppressWarnings("ALL")
@Stateless
@LocalBean
public class AccountQueryBAProcessor extends CommonAccountQueryProcessor implements Serializable {
    private static final Logger log = Logger.getLogger(AccountQueryBAProcessor.class);

    // количество счетов для "пакетной" обработки (чтобы число SQL запросов сократить)
    private static final int batchSize = 3;

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
        String XRef;
        // Преобразуем данные из сообщения
        if (!fullTopic.startsWith("<?xml")) {
            fullTopic = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + fullTopic;
        }

        if (fullTopic == null || !fullTopic.contains("AccountBalanceListQuery")) {
            journalRepository.updateLogStatus(jId, ERROR, "Ошибка при распозновании сообщения");
            // Меняем содержание на ошибку
            return getEmptyBodyMessage();
        }

        String answerBody = processAccountBalanceListQuery(fullTopic, jId, currencyMap, workdayRepository.getWorkday(), currencyNBDPMap);
        return answerBody;
    }

    private String processAccountBalanceListQuery(String fullTopic, Long jId, Map<String, String> currencyMap, Date workday, Map<String, Integer> currencyNBDPMap) throws Exception {
        Set<String> countsToProcess = readFromXML(fullTopic, jId);
        String body = createOutMessage(countsToProcess, currencyMap, workday, currencyNBDPMap);
        return body;
    }

    private Set<String> readFromXML(String bodyXML, Long jId) throws Exception {
        org.w3c.dom.Document doc = null;
        try {
            DocumentBuilder b = XmlUtilityLocator.getInstance().newDocumentBuilder();
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

        Set<String> accounts = new HashSet<>();
        XPath xPath = XmlUtilityLocator.getInstance().newXPath();
        NodeList queries = ((NodeList) xPath.evaluate("/AccountBalanceListQuery/AccountBalanceQuery", doc.getDocumentElement(), XPathConstants.NODESET));
        if (queries == null || queries.getLength() == 0) {
            //Ошибка XML
            journalRepository.updateLogStatus(jId, ERROR, "Отсутствуют неоходимые данные /AccountBalanceListQuery/AccountBalanceQuery");
            return null;
        }

        for (int i = 0; i < queries.getLength(); i++) {
            String accountNo = null, branchCode = null;
            for (int j = 0; j < queries.item(i).getChildNodes().getLength(); j++) {
                String name = queries.item(i).getChildNodes().item(j).getNodeName();
                String value = queries.item(i).getChildNodes().item(j).getTextContent();
                if (name.endsWith("CBAccountNo")) {
                    accounts.add(value);
                    break;
                } else if (name.endsWith("AccountNo")) {
                    if (value.length() == 20) {
                        accountNo = value;
                    } else {
                        journalRepository.updateLogStatus(jId, ERROR, "Длина AccountNumber не равна 20");
                        throw new Exception("Длина AccountNumber не равна 20");
                    }
                } else if (name.endsWith("Branch")) {
                    branchCode = value;
                }
            }

            String condition = "";
            if (!isEmpty(accountNo)) {//проверить на буквы и цифры
                condition += "acid='" + accountNo + "'";
            }
            if (!isEmpty(branchCode)) {//проверить на буквы и цифры
                condition += " AND ccode in (SELECT BCBBR FROM IMBCBBRP WHERE A8BRCD ='" + branchCode + "')";
            }

            if (!isEmpty(condition)) {
                accounts.addAll(queryRepository.getCountsByAB(condition));
            }
        }

        return accounts;
    }

    private String createOutMessage(Set<String> countsToProcess, Map<String, String> currencyMap, Date workday, Map<String, Integer> currencyNBDPMap) throws Exception {
        List<String> stringList = countsToProcess==null || countsToProcess.size() == 0 ? new ArrayList<>() : new ArrayList<>(countsToProcess);
        StringBuilder result = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                     "<asbo:AccountBalanceList xmlns:asbo=\"urn:asbo:barsgl\">\n");
        for (int i = 0; i < stringList.size(); i += batchSize) {
            result.append(batchCreateOutMessage(stringList.subList(i, Math.min(i + batchSize, stringList.size())), currencyMap, workday, currencyNBDPMap));
        }

        return result.append("</asbo:AccountBalanceList>").toString();
    }

    private StringBuilder batchCreateOutMessage(List<String> counts, Map<String, String> currencyMap, Date workday, Map<String, Integer> currencyNBDPMap) throws Exception {
        StringBuilder sb = new StringBuilder();
        String inCondition = "'" + StringUtils.listToString(counts, "','") + "'";

        List<DataRecord> accrlnRecordsRaw = queryRepository.getAccrlnRecords(inCondition, null);

        List<DataRecord> glAccRecordsRaw = queryRepository.getGlAccRecords(inCondition, null);
        Map<String, DataRecord> glAccRecordMap = new HashMap<>();
        for (DataRecord item : glAccRecordsRaw) {
            glAccRecordMap.put(item.getString("BSAACID"), item);
        }

        List<String> processedBsaacids = new ArrayList<>();

        for (DataRecord record : accrlnRecordsRaw) {
            // Список обработанных, чтобы потом заполнить ответы для счетов, по которым нет информации
            processedBsaacids.add(record.getString("BSAACID"));

            sb.append("<asbo:AccountBalance>\n");

            String acid = ifEmpty(record.getString("ACID"), "");
            if (acid.length() != 20) {
                log.warn("Для BSAACID=" + record.getString("BSAACID") + " некорректное значение ACID=" + acid);
            }
            if (acid.length() == 18) {
                acid = "00" + acid;
            }
//            sb.append("<asbo:AccountNo>").append(acid).append("</asbo:AccountNo>\n");
            sb.append(toTag("AccountNo", rsubstr(acid, 18)));
            sb.append("<asbo:Status>").append(lastDate.compareTo(record.getDate("DRLNC")) == 0 ? AccountStatus.O : AccountStatus.C).append("</asbo:Status>\n");
            String branch = queryRepository.getBranchByBsaacidorAcid(record.getString("BSAACID"), acid, workday);
            // method convertBranchToFcc calling from getBranchByBsaacidorAcid now
            /*
            if (!(branch.charAt(0) >= 'A' && branch.charAt(0) <= 'Z')) {
                branch = ifEmpty(queryRepository.convertBranchToFcc(branch), branch);
            }
            */
            sb.append("<asbo:Branch>").append(branch).append("</asbo:Branch>\n");
            sb.append("<asbo:CBAccountNo>").append(record.getString("BSAACID")).append("</asbo:CBAccountNo>\n");

            sb.append("<asbo:Ccy>").append(currencyMap.get(record.getString("CBCCY"))).append("</asbo:Ccy>\n");

            sb.append("<asbo:OpenBalance>\n");
            BigDecimal[] amounts = queryRepository.getAccountBalance(record.getString("BSAACID"));
            if (amounts != null) {
                sb.append("<asbo:Amount>").append(amounts[0].movePointLeft(currencyNBDPMap.get(record.getString("CBCCY")))).append("</asbo:Amount>\n");
                sb.append("<asbo:AmountRub>").append(amounts[1].movePointLeft(currencyNBDPMap.get(record.getString("CBCCY")))).append("</asbo:AmountRub>\n");
            }
            XMLGregorianCalendar xmlGregorianCalendar = dateToXML(new Date());
            sb.append("<asbo:Date>").append(xmlGregorianCalendar.toString().substring(0, 10)).append("</asbo:Date>\n");
            sb.append("<asbo:Time>").append(xmlGregorianCalendar.toString().substring(11, 19)).append("</asbo:Time>\n");
            sb.append("</asbo:OpenBalance>\n");

            sb.append("<asbo:CurrentBalance>\n");
            if (amounts != null) {
                sb.append("<asbo:Amount>").append(amounts[2].movePointLeft(currencyNBDPMap.get(record.getString("CBCCY")))).append("</asbo:Amount>\n");
                sb.append("<asbo:AmountRub>").append(amounts[3].movePointLeft(currencyNBDPMap.get(record.getString("CBCCY")))).append("</asbo:AmountRub>\n");
            }
            sb.append("<asbo:Date>").append(xmlGregorianCalendar.toString().substring(0, 10)).append("</asbo:Date>\n");
            sb.append("<asbo:Time>").append(xmlGregorianCalendar.toString().substring(11, 19)).append("</asbo:Time>\n");
            sb.append("</asbo:CurrentBalance>\n");

            sb.append("<asbo:AccountOpenDate>").append(sdf.format(record.getDate("DRLNO"))).append("</asbo:AccountOpenDate>\n");

            if (!lastDate.equals(record.getDate("DRLNC"))) {
                sb.append("<asbo:AccountCloseDate>").append(sdf.format(record.getDate("DRLNC"))).append("</asbo:AccountCloseDate>\n");
            }

            String sq = acid.length() == 20 ? acid.substring(15, 17) : "0";
            sb.append("<asbo:AccountSequence>").append(sq).append("</asbo:AccountSequence>\n");

            String desc = "";
            DataRecord glDR = glAccRecordMap.get(record.getString("BSAACID"));
            if (glDR != null) {
                desc = trim(glDR.getString("DESCRIPTION"));
            }

            if (isEmpty(desc)) {
                String anam = queryRepository.getAnam(acid, record.getString("BSAACID"), workday);
                desc = trim(anam);
            }
//            sb.append("<asbo:Description>").append(ifEmpty(desc,"")).append("</asbo:Description>\n");
            desc = prepareDescription(desc);
            sb.append(toTag("Description", desc, descriptionLength));
//            String acod = acid.length() == 20 ? acid.substring(11, 15) : "0";
//            sb.append("<asbo:Special>").append(acod).append("</asbo:Special>\n");

            sb.append("</asbo:AccountBalance>\n");
        }

        if (counts.size() != processedBsaacids.size()) {
            for (String item : counts) {
                if (processedBsaacids.contains(item)) continue;
                sb.append("<asbo:AccountBalance>\n");
                sb.append("<asbo:CBAccountNo>").append(item).append("</asbo:CBAccountNo>\n");
                sb.append("</asbo:AccountBalance>\n");
            }
        }

        return sb;
    }

    public static String fullTopicTestB =
//        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
            " <S:Header>\n" +
            "  <UCBRUHeaders xmlns=\"urn:ucbru:gbo:v3\">\n" +
            "\t\t\t<gbo:Correlation>\t\t\t\t\n" +
            "\t\t\t\t\t<gbo:XRef>1234567847567488</gbo:XRef>\n" +
            "\t\t\t\t<gbo:Segmentation>\n" +
            "\t\t\t\t\t<gbo:CanSegmentResponse>true</gbo:CanSegmentResponse>\n" +
            "\t\t\t\t</gbo:Segmentation>\n" +
            "\t\t\t</gbo:Correlation>\n" +
            "\t\t</gbo:UCBRUHeaders>\n" +
            " </S:Header>\n" +
            " <S:Body>\n" +
            "  <AccountBalanceListQuery xmlns=\"urn:ucbru:gbo:v3\">\n" +
            "   <AccountBalanceQuery>\n" +
            "    <CBAccountNo>40807840000010003982</CBAccountNo>\n" +
            "   </AccountBalanceQuery>\n" +
            "   <AccountBalanceQuery>\n" +
            "    <CBAccountNo>40807756200014415596</CBAccountNo>\n" +
            "   </AccountBalanceQuery>\n" +
            "   <AccountBalanceQuery>\n" +
            "    <CBAccountNo>40807840300010155035</CBAccountNo>\n" +
            "   </AccountBalanceQuery>\n" +
            "   <AccountBalanceQuery>\n" +
            "    <CBAccountNo>40807978700010392690</CBAccountNo>\n" +
            "   </AccountBalanceQuery>\n" +
            "   <AccountBalanceQuery>\n" +
            "    <CBAccountNo>40807810000010524077</CBAccountNo>\n" +
            "   </AccountBalanceQuery>\n" +
            "   <AccountBalanceQuery>\n" +
            "    <CBAccountNo>40807978400013048937</CBAccountNo>\n" +
            "   </AccountBalanceQuery>\n" +
            "   <AccountBalanceQuery>\n" +
            "    <CBAccountNo>40807756400014504362</CBAccountNo>\n" +
            "   </AccountBalanceQuery>\n" +
            "   <AccountBalanceQuery>\n" +
            "    <CBAccountNo>40807810500010504018</CBAccountNo>\n" +
            "   </AccountBalanceQuery>\n" +
            "   <AccountBalanceQuery>\n" +
            "    <CBAccountNo>40701810100014429491</CBAccountNo>\n" +
            "   </AccountBalanceQuery>\n" +
            "   <AccountBalanceQuery>\n" +
            "    <CBAccountNo>40602810300454473963</CBAccountNo>\n" +
            "   </AccountBalanceQuery>\n" +
            "   <AccountBalanceQuery>\n" +
            "    <CBAccountNo>40603810400010656574</CBAccountNo>\n" +
            "   </AccountBalanceQuery>\n" +
            "   <AccountBalanceQuery>\n" +
            "    <CBAccountNo>40701810000010529123</CBAccountNo>\n" +
            "   </AccountBalanceQuery>\n" +
            "   <AccountBalanceQuery>\n" +
            "    <CBAccountNo>40701810100013964999</CBAccountNo>\n" +
            "   </AccountBalanceQuery>\n" +
            "  </AccountBalanceListQuery>\n" +
            " </S:Body>\n" +
            "</S:Envelope>";
}
