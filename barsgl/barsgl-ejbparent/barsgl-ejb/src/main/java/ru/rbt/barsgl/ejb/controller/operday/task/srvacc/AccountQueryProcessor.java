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
import javax.xml.datatype.XMLGregorianCalendar;
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
import java.math.BigDecimal;
import java.util.*;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.entity.acc.AclirqJournal.Status.ERROR;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.*;


/**
 * Created by ER22228 on 29.03.2016.
 */
@SuppressWarnings("ALL")
@Stateless
@LocalBean
public class AccountQueryProcessor extends CommonAccountQueryProcessor implements Serializable {
    private static final Logger log = Logger.getLogger(AccountQueryProcessor.class);

    // количество счетов для "пакетной" обработки (чтобы число SQL запросов сократить)
    private static final int batchSize = 100;

    @EJB
    private AclirqJournalRepository journalRepository;

//    @EJB
//    private AuditController auditController;
//
//    @EJB
//    private CoreRepository coreRepository;

    @EJB
    private AccountQueryRepository queryRepository;

    @Inject
    private WorkdayRepository workdayRepository;

    private enum AccountMap {EXISTS, NOT_EXISTS};
    
    private ThreadLocal<Boolean> isAccRst = new ThreadLocal<>();

    public String process(String fullTopic, Map<String, String> currencyMap, Map<String, Integer> currencyNBDPMap, long jId, boolean showUnspents) throws Exception {
        isAccRst.set(false);
        if (!fullTopic.startsWith("<?xml")) {
            fullTopic = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + fullTopic;
        }

        if (fullTopic == null || !fullTopic.contains("AccountListQuery")) {
            journalRepository.updateLogStatus(jId, AclirqJournal.Status.ERROR, "Ошибка в содержании сообщения");
            // Меняем содержание на ошибку
            return getEmptyBodyMessage();
        }

        String answerBody = processAccountListQuery(fullTopic, jId, currencyMap, workdayRepository.getWorkday(), currencyNBDPMap, showUnspents);

//        log.info("Обработка одного сообщения завершена.");
        return answerBody;
    }

    private Map<AccountMap, Object> readFromXML(String bodyXML, Long jId) throws Exception {
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

        NodeList nodes = null;
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            nodes = (NodeList) xPath.evaluate("/AccountListQuery", doc.getDocumentElement(), XPathConstants.NODESET);
            if (nodes == null || nodes.getLength() != 1) {
                nodes = (NodeList) xPath.evaluate("Body/AccountListQuery", doc.getDocumentElement(), XPathConstants.NODESET);
                if (nodes == null || nodes.getLength() != 1) {
                    //Ошибка XML
                    journalRepository.updateLogStatus(jId, ERROR, "Отсутствуют неоходимые данные /AccountList/AccountDetails");
                    return null;
                }
            }
        } catch (XPathExpressionException e) {
            journalRepository.updateLogStatus(jId, ERROR, "Ошибка при чтении входящего XML\n" + e.getMessage());
            throw e;
        }

        Set<String> accounts = new HashSet<>();
        List<DataRecord> accountsDataRecordList = new ArrayList<>();
//        XPath xPath = XPathFactory.newInstance().newXPath();
//        NodeList queries = ((NodeList) xPath.evaluate("/AccountListQuery/AccountQuery", doc.getDocumentElement(), XPathConstants.NODESET));
        NodeList queries = ((NodeList) xPath.evaluate("./AccountQuery", nodes.item(0), XPathConstants.NODESET));

        for (int i = 0; i < queries.getLength(); i++) {
            String customerNo = null;
            List<String> specs = new ArrayList<>();
            List<String> accTypes = new ArrayList<>();//AccountingType
            for (int j = 0; j < queries.item(i).getChildNodes().getLength(); j++) {
                String name = queries.item(i).getChildNodes().item(j).getNodeName();
                String value = queries.item(i).getChildNodes().item(j).getTextContent();
                if (name.endsWith("AccountNumber")) {
                    if (value.length() == 20) {
                        accounts.add(value);
                    } else {
                        journalRepository.updateLogStatus(jId, ERROR, "Длина AccountNumber не равна 20");
                        throw new Exception("Длина AccountNumber не равна 20");
                    }
                } else if (name.endsWith("CustomerNo")) {
                    customerNo = format("%08d", Integer.parseInt(value));
                } else if (name.endsWith("AccountSpecials")) {
                    if (!"ALL".equalsIgnoreCase(value) && !"*".equals(value)) {
                        specs.add(value);
                    }
                } else if (name.endsWith("AccountingType")) {
                    accTypes.add(value);
                }
            }
            
            if(!accounts.isEmpty()){
              accountsDataRecordList.addAll(queryRepository.getCountsByAccount(accounts));
              isAccRst.set(true);
            }
            
            if (customerNo != null) {                
                if (!specs.isEmpty()) {
                    accountsDataRecordList.addAll(queryRepository.getCountsByAcod(customerNo, specs));
                }

                if (!accTypes.isEmpty()) {
                    accountsDataRecordList.addAll(queryRepository.getCountsByAcctype(customerNo, accTypes));
                }

                if (specs.isEmpty() && accTypes.isEmpty()) {
                    accountsDataRecordList.addAll(queryRepository.getCountsByCustomerNoOnly(customerNo));
                }
            }
        }

        accountsDataRecordList.stream().forEach(item -> {
          accounts.remove(item.getString("BSAACID"));
        });
        
        Map<AccountMap, Object> map = new HashMap<>();
        map.put(AccountMap.EXISTS, accountsDataRecordList);
        map.put(AccountMap.NOT_EXISTS, accounts);
        return map;
    }

    private String processAccountListQuery(String fullTopic, Long jId, Map<String, String> currencyMap, Date workday, Map<String, Integer> currencyNBDPMap, boolean showUnspents) throws Exception {
        Map<AccountMap, Object> map = readFromXML(fullTopic, jId);
        List<DataRecord> countsToProcess = (List<DataRecord>) map.get(AccountMap.EXISTS);
        Set<String> notExistsAccounts = (Set<String>) map.get(AccountMap.NOT_EXISTS);
        String body = createOutMessage(countsToProcess, notExistsAccounts, currencyMap, workday, currencyNBDPMap, showUnspents);
        return body;
    }

    private String createOutMessage(List<DataRecord> countsToProcessList, Set<String> notExistsAccounts, Map<String, String> currencyMap, Date workday, Map<String, Integer> currencyNBDPMap, boolean showUnspents) throws Exception {
        StringBuilder result = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                     "<asbo:AccountList xmlns:asbo=\"urn:asbo:barsgl\">\n");
        
        if(countsToProcessList != null){
          for (int i = 0; i < countsToProcessList.size(); i += batchSize) {
              result.append(batchCreateOutMessage(countsToProcessList.subList(i, Math.min(i + batchSize, countsToProcessList.size())), 
                      currencyMap, workday, showUnspents, currencyNBDPMap));
          }
        }

        // Если есть счета, для которых нет данных
        notExistsAccounts.forEach(item -> {
          result.
          append("<asbo:AccountDetails>\n").
            append("<asbo:CBAccountNo>").
                  append(item).
            append("</asbo:CBAccountNo>\n").
          append("</asbo:AccountDetails>\n");
        });
        
        return result.append("</asbo:AccountList>").toString();
    }

    private StringBuilder batchCreateOutMessage(List<DataRecord> accrlnRecordsRaw, Map<String, String> currencyMap, Date workday, boolean showUnspents, Map<String, Integer> currencyNBDPMap) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (DataRecord record : accrlnRecordsRaw) {

            sb.append("<asbo:AccountDetails>\n");

            sb.append("<asbo:CBAccountNo>").append(record.getString("BSAACID")).append("</asbo:CBAccountNo>\n");

            String acid = ifEmpty(record.getString("ACID"), "");
            if (acid.length() != 20) {
                log.warn("Для BSAACID=" + record.getString("BSAACID") + " некорректное значение ACID=" + acid);
            }
            if (acid.length() == 18) {
                acid = "00" + acid;
            }
//                sb.append("<asbo:AccountNo>").append(acid).append("</asbo:AccountNo>\n");
            sb.append(toTag("AccountNo", rsubstr(acid, 18)));

            String branch = queryRepository.getBranchByBsaacidorAcid(record.getString("BSAACID"), 
                    record.getString("BRANCH"), 
                    workday);
            
            // method convertBranchToFcc calling from getBranchByBsaacidorAcid now
            /*
            if (!(branch.charAt(0) >= 'A' && branch.charAt(0) <= 'Z')) {
                branch = ifEmpty(queryRepository.convertBranchToFcc(branch), branch);
            }
            */

            sb.append("<asbo:Branch>").append(branch).append("</asbo:Branch>\n");

            sb.append("<asbo:Ccy>").append(record.getString("CCY")).append("</asbo:Ccy>\n");
            String ccyDigital = record.getString("BSAACID").substring(5, 8);
            sb.append("<asbo:CcyDigital>").append(ccyDigital).append("</asbo:CcyDigital>\n");
            sb.append("<asbo:CustomerNo>").append(record.getString("CUSTNO")).append("</asbo:CustomerNo>\n");

            sb.append("<asbo:Special>").append(record.getString("ACOD")).append("</asbo:Special>\n");

            if (showUnspents && isAccRst.get()) {
                sb.append("<asbo:OpenBalance>\n");
                BigDecimal[] amounts = queryRepository.getAccountBalance(record.getString("BSAACID"));
                if (amounts != null) {
                    sb.append("<asbo:Amount>").append(amounts[0].movePointLeft(currencyNBDPMap.get(ccyDigital))).append("</asbo:Amount>\n");
                    sb.append("<asbo:AmountRub>").append(amounts[1].movePointLeft(currencyNBDPMap.get(ccyDigital))).append("</asbo:AmountRub>\n");
                }
                XMLGregorianCalendar xmlGregorianCalendar = dateToXML(new Date());
                sb.append("<asbo:Date>").append(xmlGregorianCalendar.toString().substring(0, 10)).append("</asbo:Date>\n");
                sb.append("<asbo:Time>").append(xmlGregorianCalendar.toString().substring(11, 19)).append("</asbo:Time>\n");
                sb.append("</asbo:OpenBalance>\n");

                sb.append("<asbo:CurrentBalance>\n");
                if (amounts != null) {
                    sb.append("<asbo:Amount>").append(amounts[2].movePointLeft(currencyNBDPMap.get(ccyDigital))).append("</asbo:Amount>\n");
                    sb.append("<asbo:AmountRub>").append(amounts[3].movePointLeft(currencyNBDPMap.get(ccyDigital))).append("</asbo:AmountRub>\n");
                }
                sb.append("<asbo:Date>").append(xmlGregorianCalendar.toString().substring(0, 10)).append("</asbo:Date>\n");
                sb.append("<asbo:Time>").append(xmlGregorianCalendar.toString().substring(11, 19)).append("</asbo:Time>\n");
                sb.append("</asbo:CurrentBalance>\n");
            }

            sb.append("<asbo:OpenDate>").append(sdf.format(record.getDate("DTO"))).append("</asbo:OpenDate>\n");

            if (record.getDate("DTC") != null) {
                sb.append("<asbo:CloseDate>").append(sdf.format(record.getDate("DTC"))).append("</asbo:CloseDate>\n");
            }

            sb.append("<asbo:Status>").append(record.getDate("DTC") == null ? AccountStatus.O : AccountStatus.C).append("</asbo:Status>\n");

            String sq = acid.length() == 20 ? acid.substring(15, 17) : "0";
            sb.append("<asbo:AccountSequence>").append(sq).append("</asbo:AccountSequence>\n");

            String desc = "";
            String glDR = record.getString("BSAACID");
            if (glDR != null) {
                sb.append("<asbo:AccountingType>").append(ifEmpty(record.getString("ACCTYPE"), "")).append("</asbo:AccountingType>\n");
                sb.append("<asbo:DealSourceId>").append(ifEmpty(record.getString("DEALSRS"), "")).append("</asbo:DealSourceId>\n");
                sb.append("<asbo:DealId>").append(ifEmpty(record.getString("DEALID"), "")).append("</asbo:DealId>\n");
                sb.append("<asbo:SubDealId>").append(ifEmpty(record.getString("SUBDEALID"), "")).append("</asbo:SubDealId>\n");
                desc = trim(record.getString("DESCRIPTION"));
            }

            if (isEmpty(desc)) {
                String anam = queryRepository.getAnam(acid, record.getString("BSAACID"), workday);//todo
                desc = trim(anam);
            }
            //sb.append("<asbo:Description>").append(ifEmpty(desc, "")).append("</asbo:Description>\n");
            desc = prepareDescription(desc);
            sb.append(toTag("Description", desc, descriptionLength));
            sb.append("</asbo:AccountDetails>\n");
        }

        return sb;
    }

    /*

        @Deprecated
        private AccountList fillAccounts(Set<String> countsToProcess, Map<String, String> currencyMap, Date workday, Map<String, Integer> currencyNBDPMap) {
            List<String> stringList = new ArrayList<>(countsToProcess);
            AccountList result = new AccountList();
            List<AccountDetails> accountDetailsList = new ArrayList<>();
            result.setAccountDetails(accountDetailsList);
            for (int i = 0; i < stringList.size(); i += batchSize) {
                accountDetailsList.addAll(batchFillCounts(
                    stringList.subList(i, Math.min(i + batchSize, stringList.size())), currencyMap, workday));
            }
            return result;
        }

        @Deprecated
        private List<AccountDetails> batchFillCounts(List<String> counts, Map<String, String> currencyMap, Date workday) {
            String inCondition = "'" + StringUtils.listToString(counts, "','") + "'";

            List<DataRecord> accrlnRecordsRaw = queryRepository.getAccrlnRecords(inCondition);

            List<DataRecord> glAccRecordsRaw = queryRepository.getGlAccRecords(inCondition);
            Map<String, DataRecord> glAccRecordMap = new HashMap<>();
            for (DataRecord item : glAccRecordsRaw) {
                glAccRecordMap.put(item.getString("BSAACID"), item);
            }

            List<String> processedBsaacids = new ArrayList<>();
            List<AccountDetails> accountsInfo = new ArrayList<>();
            try {
                for (DataRecord record : accrlnRecordsRaw) {

                    AccountDetails details = new AccountDetails();

                    // Список обработанных, чтобы потом заполнить ответы для счетов, по которым нет информации
                    processedBsaacids.add(record.getString("BSAACID"));

                    details.setCBAccountNo(record.getString("BSAACID"));

                    String acid = ifEmpty(record.getString("ACID"), "");
                    if (acid.length() != 20) {
                        log.warn("Для BSAACID=" + record.getString("BSAACID") + " некорректное значение ACID=" + acid);
                    }
                    if (acid.length() != 18) {
                        acid = "00" + acid;
                    }
                    details.setAccountNo(acid);

                    String branch = queryRepository.getBranchByBsaacidorAcid(record.getString("BSAACID"), acid, workday);
                    details.setBranch(branch);

                    details.setCcy(currencyMap.get(record.getString("CBCCY")));
                    details.setCcyDigital(record.getString("CBCCY"));
                    details.setCustomerNo(record.getString("CNUM"));
                    String acod = acid.length() == 20 ? acid.substring(11, 15) : "0";
                    details.setSpecial(acod);
                    details.setOpenDate(dateToXML(record.getDate("DRLNO")));

                    if (!lastDate.equals(record.getDate("DRLNC"))) {
                        details.setCloseDate(dateToXML(record.getDate("DRLNC")));
                    }

                    details.setStatus(lastDate.compareTo(record.getDate("DRLNC")) == 0 ? AccountStatus.fromValue("O") : AccountStatus.fromValue("C"));

                    String sq = acid.length() == 20 ? acid.substring(15, 17) : "0";
                    details.setAccountSequence(sq);

                    DataRecord glDR = glAccRecordMap.get(record.getString("BSAACID"));
                    if (glDR != null) {
                        details.setAccountingType(glDR.getString("ACCTYPE"));
                        details.setDealSourceId(glDR.getString("DEALSRS"));
                        details.setDealId(glDR.getString("DEALID"));
                        details.setSubDealId(glDR.getString("SUBDEALID"));
                        details.setDescription(trim(glDR.getString("DESCRIPTION")));
                    }

                    if (isEmpty(details.getDescription())) {
                        String anam = queryRepository.getAnam(acid, record.getString("BSAACID"), workday);//todo
                        details.setDescription(trim(anam));
                    }

                    accountsInfo.add(details);
                }
            } catch (Exception ex) {
                log.error("", ex);
            }

            // Если есть счета, для которых нет данных
            if (counts.size() != processedBsaacids.size()) {
                for (String item : counts) {
                    if (processedBsaacids.contains(item)) continue;
                    AccountDetails details = new AccountDetails();
                    details.setCBAccountNo(item);
                    accountsInfo.add(details);
                }
            }

            return accountsInfo;
        }

    */
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
