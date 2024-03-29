package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;


import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ru.rbt.barsgl.ejb.entity.acc.*;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountController;
import ru.rbt.barsgl.ejb.repository.*;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.repository.dict.CurrencyCacheRepository;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.shared.enums.DealSource;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.ExceptionUtils;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.AccountDetailsNotify;
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Sources.FC12;
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Sources.FCC;
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Status.*;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;

/**
 * Created by ER22228 on 29.03.2016.
 */
@SuppressWarnings("ALL")
@Stateless
@LocalBean
@Deprecated
public class AccountDetailsNotifyProcessorOld implements Serializable {
    private static final Logger log = Logger.getLogger(AccountDetailsNotifyProcessorOld.class);

    @Inject
    private GLAccountRepository glAccountRepository;

    @Inject
    private GLAccountController glAccountController;

    @Inject
    private CurrencyCacheRepository bankCurrencyRepository;

    @EJB
    private AcDNJournalRepository journalRepository;

    @EJB
    private AuditController auditController;

    @EJB
    private CoreRepository coreRepository;

    static String[] paramNamesFCC = {"AccountNo", "Branch", "CBAccountNo", "Ccy", "CcyDigital", "Description", "Status",
            "CustomerNo", "Special", "OpenDate", "CloseDate", "Positioning/HostABS"};

    static String[] paramNamesFC12 = {"Branch", "CBAccountNo", "Status", "CloseDate", "Positioning/HostABS"};

    // TODO нельзя делать static SimpleDateFormat!!
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public void process(AcDNJournal.Sources source, String incomingEnvelope, final Long jId) throws Exception {
        switch(source) {
            case FC12:
            case FCC_CLOSE:
                processFC12(FC12, incomingEnvelope, jId);
                return;
            case FCC:
                processFCC(source, incomingEnvelope, jId);
                return;
            default:
                throw new DefaultApplicationException("Неверный источник: " + source.name());
        }
    }

    public void processFC12(AcDNJournal.Sources source, String incomingEnvelope, final Long jId) throws Exception {
        Map<String, String> xmlData = readFromXML(incomingEnvelope, jId, paramNamesFC12);
        if (xmlData == null) {
            // Запись в аудит, в таблицу аудита, в лог и возврат
            journalRepository.updateLogStatus(jId, ERROR, "Ошибка во время распознования XML");
            return;
        }

        String hostABS = xmlData.get("Positioning/HostABS");
        String bsaacid = xmlData.get("CBAccountNo");
        String status = xmlData.get("Status");

        if("C".equals(status)) {
            closeAccountAny(source, hostABS, bsaacid, xmlData, jId);
        } else {
            checkBranchFlex(source, hostABS, bsaacid, xmlData, jId);
        }

        log.info("Обработка одного сообщения от " + hostABS + " завершена. cbAccount:" + xmlData.get("CBAccountNo"));
    }

    public void processFCC(AcDNJournal.Sources source, String incomingEnvelope, final Long jId) throws Exception {
        // Преобразуем данные из сообщения
        Map<String, String> xmlData = readFromXML(incomingEnvelope, jId, paramNamesFCC);
        if (xmlData == null) {
            // Запись в аудит, в таблицу аудита, в лог и возврат
            journalRepository.updateLogStatus(jId, ERROR, "Ошибка во время распознования XML");
            return;
        }

        String hostABS = xmlData.get("Positioning/HostABS");
        String bsaacid = xmlData.get("CBAccountNo");
        String status = xmlData.get("Status");

        if("C".equals(status)) {
            closeAccountAny(source, hostABS, bsaacid, xmlData, jId);
        } else {
            processOpenFCC(source, hostABS, bsaacid, xmlData, jId);
        }

        log.info("Обработка одного сообщения от " + hostABS + " завершена. cbAccount:" + xmlData.get("CBAccountNo"));
    }

    private Map<String, String> readFromXML(String bodyXML, Long jId, String[] paramNames) throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        org.w3c.dom.Document doc = null;
        try {
            DocumentBuilder b = XmlUtilityLocator.getInstance().newDocumentBuilder();
            doc = b.parse(new ByteArrayInputStream(bodyXML.getBytes("UTF-8")));
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
        XPath xPath = XmlUtilityLocator.getInstance().newXPath();
        try {
            nodes = (NodeList) xPath.evaluate("Body/AccountList/AccountDetails", doc.getDocumentElement(), XPathConstants.NODESET);
            if (nodes == null || nodes.getLength() != 1) {
                //Ошибка XML
                journalRepository.updateLogStatus(jId, ERROR, "Отсутствуют неоходимые данные /Body/AccountList/AccountDetails");
                return null;
            }
        } catch (XPathExpressionException e) {
            journalRepository.updateLogStatus(jId, ERROR, "Ошибка при чтении входящего XML\n" + e.getMessage());
            throw e;
        }

        Node accDetailsNode = nodes.item(0);
        Map<String, String> params = new HashMap<>();

        for (String item : paramNames) {
            params.put(item, (String) xPath.evaluate("./" + item, accDetailsNode, XPathConstants.STRING));
        }

        return params;
    }

    private String getBodyXML(String fullTopic2, Long jId) throws Exception {
        String topic = fullTopic2.replace("\n", "").replace("\r", "").replaceAll(".*<(.*[B|b]ody)>(.*)</\\1>.*", "$2");
        if (isEmpty(topic)) {
            // Записать ошибку
            journalRepository.updateLogStatus(jId, ERROR, "Неправильный формат сообщения. Раздел <body> отсутствует.");
            return null;
        }
        topic = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + topic;
        return topic;
    }

    private void closeAccountAny(AcDNJournal.Sources source, String hostABS, String bsaacid, Map<String, String> xmlData, Long jId) throws Exception {
        String closeDateXml = xmlData.get("CloseDate");
        GLAccount account;
        if (!isEmpty(bsaacid) && !isEmpty(hostABS) && !isEmpty(closeDateXml) && source.name().equals(hostABS)) {
            Date closeDate = sdf.parse(closeDateXml);
            if (glAccountRepository.isExistsGLAccount(bsaacid, closeDate)) {
                glAccountController.closeGLAccountNotify(bsaacid, closeDate);
                journalRepository.updateLogStatus(jId, PROCESSED, "Счет с bsaacid=" + bsaacid + " закрыт");
                auditController.info(AccountDetailsNotify, String.format("Счет '%s' закрыт по нотификации от %s", bsaacid, source.name()));
            } else if (null != (account = glAccountRepository. findGLAccount(bsaacid))) {
                journalRepository.updateLogStatus(jId, ERROR, String.format("Счет '%s' уже закрыт с датой ''", bsaacid, sdf.format(account.getDateClose()))); //new DateUtils().onlyDateString(account.getDateClose())));
                return;
            } else {
                journalRepository.updateLogStatus(jId, ERROR, String.format("Счет '%s' не существует", bsaacid));
                return;
            }
        } else {
            journalRepository.updateLogStatus(jId, ERROR, "Ошибка в данных bsaacid / hostABS / closeDate");
            return;
        }
    }

    private void checkBranchFlex(AcDNJournal.Sources source, String hostABS, String bsaacid, Map<String, String> xmlData, Long jId) throws SQLException {
        String branch = xmlData.get("Branch");
        if (isEmpty(branch)) {
            journalRepository.updateLogStatus(jId, ERROR, "Branch не задан");
            return;
        }

        DataRecord recMidas = coreRepository.selectFirst("SELECT midas_branch FROM DH_BR_MAP where fcc_branch=?", branch);
        DataRecord recGlacc = coreRepository.selectFirst("SELECT branch from gl_acc where bsaacid=?", bsaacid);

        if (recMidas == null){
            journalRepository.updateLogStatus(jId, ERROR, "Не найден DH_BR_MAP.fcc_branch = " + branch);
            log.warn("Не найден DH_BR_MAP.fcc_branch = " + branch);
            return;
        }else if (recGlacc == null){
            journalRepository.updateLogStatus(jId, ERROR, "Не найден gl_acc.bsaacid = " + bsaacid);
            log.warn("Не найден gl_acc.bsaacid = " + bsaacid);
            return;
        }
        String midasBranch = recMidas.getString(0);
        String glaccBranch = recGlacc.getString(0);
        if (midasBranch.equals(glaccBranch)){
            journalRepository.updateLogStatus(jId, ERROR, "Branch не менялся");
            auditController.info(AccountDetailsNotify, "Branch не менялся");
        }else{
            coreRepository.executeNativeUpdate("UPDATE GL_ACC SET branch=? WHERE BSAACID=?", midasBranch, bsaacid);
            String msg = "На счете " + bsaacid + " branch " + glaccBranch + " заменен на " + midasBranch + "(" + branch + ")";
            journalRepository.updateLogStatus(jId, PROCESSED, msg);
            auditController.info(AccountDetailsNotify, msg);
        }
    }

/*
    private void updateDateClose(String bsaacid, Date dateClose) {
        coreRepository.executeNativeUpdate("UPDATE GL_ACC SET DTC=? WHERE BSAACID=?", dateClose, bsaacid);
    }
*/

    private void processOpenFCC(AcDNJournal.Sources source, String hostABS, String bsaacid, Map<String, String> xmlData, Long jId) throws Exception {
        if (FCC.equals(source) && !FCC.name().equals(hostABS)) {
            journalRepository.updateLogStatus(jId, ERROR,
                    "Не обрабатываем сообщение из " + source.name() + " c HostABS=" + hostABS);
            return;
        }

        if (isEmpty(bsaacid)) {
            journalRepository.updateLogStatus(jId, ERROR, "bsaacid не задан");
            return;
        }

        if( bsaacid.startsWith("423") || bsaacid.startsWith("426") ){
            journalRepository.updateLogStatus(jId, ERROR, "Депозитный счет " + bsaacid + " не открывается через сервис");
            return;
        }

        Date openDate = sdf.parse(xmlData.get("OpenDate"));
        if (glAccountRepository.isExistsGLAccount(bsaacid, openDate)) {
            journalRepository.updateLogStatus(jId, ERROR, "Счет с bsaacid=" + bsaacid + " уже существует в GL_ACC");
            return;
        }

        // validate после enrichment потому что для FCC-ветки нужен MIDAS_BRANCH
        if (validateAll(source, jId, xmlData)) { // && enrichmentData(source, xmlData, jId)) {
            // Вставка в таблицу GL_ACC
            openAccountFCC(source, xmlData, openDate, jId);
        }

    }

    private boolean validateAll(AcDNJournal.Sources source, Long jId, Map<String, String> xmlData) throws Exception {
        if (!validateCcy(xmlData.get("Ccy"))) {
            journalRepository.updateLogStatus(jId, ERROR, "Ошибка проверки кода валюты " + xmlData.get("Ccy"));
            return false;
        }

        if (!glAccountRepository.validateBranch(xmlData.get("Branch"))) {
            journalRepository.updateLogStatus(jId, ERROR, "Ошибка проверки кода бранча " + xmlData.get("Branch"));
            return false;
        }

        return true;
    }

    public boolean validateCcy(String ccy) throws SQLException {
        return bankCurrencyRepository.findCached(ccy) != null;
    }


    private void openAccountFCC(AcDNJournal.Sources source, Map<String, String> xmlData, Date openDate, Long jId) throws Exception {
        AcDNJournal journal = journalRepository.findById(AcDNJournal.class, jId);
        if (journal.getStatus().equals(AcDNJournal.Status.ERROR)) {
            return;
        }

        String bsaAcid = xmlData.get("CBAccountNo");
        // Если FCC, то обогащение. Придумываем счёт MIDAS
        // Если ошибка в процессе обогащения - вернётся null
        AccountKeys keys = createAccountKeys(source, xmlData, jId);

        if (null != keys) {
            glAccountController.createGLAccountNotify(bsaAcid, openDate, keys);
            String logMessage = xmlData.get("AccountNo") + "/" + bsaAcid;
            journalRepository.updateLogStatus(jId, PROCESSED, logMessage);
        }
    }

    private AccountKeys createAccountKeys(AcDNJournal.Sources source, Map<String, String> xmlData, Long jId) throws Exception {
        AccountKeys keys = new AccountKeys("");
        String cnum = format("%08d", Integer.parseInt(xmlData.get("CustomerNo")));
        keys.setCustomerNumber(cnum);
        keys.setCurrency(xmlData.get("Ccy"));

        String midasBranch = glAccountRepository.getMidasBranchByFlex(xmlData.get("Branch"));
        if (isEmpty(midasBranch)) {
            journalRepository.updateLogStatus(jId, ERROR, "Параметр Midas Branch не вычислен. SELECT MIDAS_BRANCH FROM DH_BR_MAP WHERE FCC_BRANCH=? :" + xmlData.get("Branch"));
            return null;
        }
        keys.setBranch(midasBranch);
        String acid = generateAcid(midasBranch, cnum, keys.getCurrency());
        keys.setAccountMidas(acid);

        DataRecord data = glAccountRepository.getFilialByBranch(midasBranch);
        if (null == data) {
            journalRepository.updateLogStatus(jId, ERROR, "Не определен филиал. SELECT A8CMCD, BCBBR from IMBCBBRP  where A8BRCD = ? :" + midasBranch);
            return null;
        }
        keys.setFilial(data.getString(0));
        keys.setCompanyCode(data.getString(1));
        String acc2 = xmlData.get("CBAccountNo").substring(0,5);
        keys.setAccount2(acc2);

        Short custType = glAccountRepository.getCustomerType(cnum, acc2);
        if (null == custType) {
            journalRepository.updateLogStatus(jId, ERROR, String.format("Не определен тип собственности клиента по номеру клиента = '%s' и ACC2 = '%s'", cnum, acc2));
            return null;
        }
        keys.setCustomerType(custType.toString());

        keys.setAccountType("0");
        keys.setAccountCode("0");
        keys.setAccSequence(acid.substring(15,17));
        keys.setDealSource(DealSource.BarsGL.getLabel());

//        keys.setTerm(null);
//        keys.setGlSequence(null);
//        keys.setPlCode(null);
//        keys.setDealId(null);
//        keys.setSubDealId(null);

//        keys.setDescription(null);    // TODO может понадобиться

        journalRepository.updateLogStatus(jId, ENRICHED, "");

        return keys;
    }

    private String generateAcid(String midasBranch, String customerNo, String ccy) throws Exception {
/*
        // пока считаем, что это не нужно
        String pseudoAcid = null;
        for (int i = 99; i > 9; i--) {
            pseudoAcid = customerNo + ccy + "0000" + i + midasBranch;
            if (isEmpty(journalDataRepository.existsAcid(pseudoAcid))) {
                break;
            }
            pseudoAcid = null;
        }
        if (pseudoAcid == null) {
            pseudoAcid =  customerNo + ccy + "0000" + "90" + midasBranch;
        }
*/

        return customerNo + ccy + "0000" + "90" + midasBranch;
    }

    public String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable,
                ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class, DefaultApplicationException.class);
    }

    // examples
    public static String messageFCCNoCustomer =
            "<NS1:Envelope xmlns:NS1=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                    "    <NS1:Header>\n" +
                    "        <NS2:UCBRUHeaders xmlns:NS2=\"urn:imb:gbo:v2\">\n" +
                    "            <NS2:Audit>\n" +
                    "                <NS2:MessagePath>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountDetailsNotify.NotificationHandler</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v4</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-06-24T16:14:55.585+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>START</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountListQuery</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-06-24T16:14:55.591+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>START</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountListQuery</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-06-24T16:14:55.766+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>SUCCESS</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountDetailsNotify.Publisher</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-06-24T16:14:55.787+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>SUCCESS</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment></NS2:Comment>\n" +
                    "                    </NS2:Step>\n" +
                    "                </NS2:MessagePath>\n" +
                    "            </NS2:Audit>\n" +
                    "        </NS2:UCBRUHeaders>\n" +
                    "    </NS1:Header>\n" +
                    "    <NS1:Body>\n" +
                    "        <gbo:AccountList xmlns:gbo=\"urn:imb:gbo:v2\">\n" +
                    "            <gbo:AccountDetails>\n" +
                    "                <gbo:AccountNo>02263713RURPRCA101</gbo:AccountNo>\n" +
                    "                <gbo:Branch>J01</gbo:Branch>\n" +
                    "                <gbo:CBAccountNo>40817810250300081806</gbo:CBAccountNo>\n" +
                    "                <gbo:Ccy>RUR</gbo:Ccy>\n" +
                    "                <gbo:CcyDigital>810</gbo:CcyDigital>\n" +
                    "                <gbo:Description>PRCA1 02 KAA</gbo:Description>\n" +
                    "                <gbo:Status>O</gbo:Status>\n" +
                    "                <gbo:CustomerNo>02263713</gbo:CustomerNo>\n" +
                    "                <gbo:Special>PRCA1</gbo:Special>\n" +
                    "                <gbo:OpenDate>2016-06-24</gbo:OpenDate>\n" +
                    "                <gbo:AltAccountNo>02263713RURPRCA101</gbo:AltAccountNo>\n" +
                    "                <gbo:Type>S</gbo:Type>\n" +
                    "                <gbo:ATMAvailable>N</gbo:ATMAvailable>\n" +
                    "                <gbo:IsFrozen>N</gbo:IsFrozen>\n" +
                    "                <gbo:IsDormant>N</gbo:IsDormant>\n" +
                    "                <gbo:CreditTransAllowed>Y</gbo:CreditTransAllowed>\n" +
                    "                <gbo:DebitTransAllowed>Y</gbo:DebitTransAllowed>\n" +
                    "                <gbo:ClearingBank>042007709</gbo:ClearingBank>\n" +
                    "                <gbo:CorBank>042007709</gbo:CorBank>\n" +
                    "                <gbo:Positioning>\n" +
                    "                    <gbo:CBAccount>40817810250300081806</gbo:CBAccount>\n" +
                    "                    <gbo:IMBAccountNo>40817810250300081806</gbo:IMBAccountNo>\n" +
                    "                    <gbo:IMBBranch>J01</gbo:IMBBranch>\n" +
                    "                    <gbo:HostABSAccountNo>02263713RURPRCA101</gbo:HostABSAccountNo>\n" +
                    "                    <gbo:HostABSBranch>J01</gbo:HostABSBranch>\n" +
                    "                    <gbo:HostABS>FCC</gbo:HostABS>\n" +
                    "                </gbo:Positioning>\n" +
                    "                <gbo:MIS>\n" +
                    "                    <gbo:Group>R_P_MC</gbo:Group>\n" +
                    "                    <gbo:Pool>DFLTPOOL</gbo:Pool>\n" +
                    "                    <gbo:GroupComponent>R_P_MC</gbo:GroupComponent>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS1</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS2</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS3</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS4</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS5</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS6</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS7</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS8</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS9</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS10</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS1</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS2</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS3</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS4</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS5</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS6</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS7</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS8</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS9</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS10</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CostCode>\n" +
                    "                        <gbo:Name>COSTCOD1</gbo:Name>\n" +
                    "                    </gbo:CostCode>\n" +
                    "                    <gbo:CostCode>\n" +
                    "                        <gbo:Name>COSTCOD2</gbo:Name>\n" +
                    "                    </gbo:CostCode>\n" +
                    "                    <gbo:CostCode>\n" +
                    "                        <gbo:Name>COSTCOD3</gbo:Name>\n" +
                    "                    </gbo:CostCode>\n" +
                    "                    <gbo:CostCode>\n" +
                    "                        <gbo:Name>COSTCOD4</gbo:Name>\n" +
                    "                    </gbo:CostCode>\n" +
                    "                    <gbo:CostCode>\n" +
                    "                        <gbo:Name>COSTCOD5</gbo:Name>\n" +
                    "                    </gbo:CostCode>\n" +
                    "                </gbo:MIS>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>GWSAccType</gbo:Name>\n" +
                    "                    <gbo:Value>CURR</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>OperationTypeCodes</gbo:Name>\n" +
                    "                    <gbo:Value>VIEW=1,DOMPAY=1,CUPADE=1,CUPACO=1,</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>ParentBranchName</gbo:Name>\n" +
                    "                    <gbo:Value>UCB, Voronezh Branch</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>CusSegment</gbo:Name>\n" +
                    "                    <gbo:Value>TIER_I</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "            </gbo:AccountDetails>\n" +
                    "        </gbo:AccountList>\n" +
                    "    </NS1:Body>\n" +
                    "</NS1:Envelope>";

    public static String messageFCCShadow =
            "<NS1:Envelope xmlns:NS1=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                    "    <NS1:Header>\n" +
                    "        <NS2:UCBRUHeaders xmlns:NS2=\"urn:imb:gbo:v2\">\n" +
                    "            <NS2:Audit>\n" +
                    "                <NS2:MessagePath>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountDetailsNotify.NotificationHandler</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v4</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-06-24T13:35:33.540+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>START</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountListQuery</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-06-24T13:35:33.550+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>START</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountListQuery</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-06-24T13:35:33.771+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>SUCCESS</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountDetailsNotify.Publisher</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-06-24T13:35:33.789+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>SUCCESS</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment></NS2:Comment>\n" +
                    "                    </NS2:Step>\n" +
                    "                </NS2:MessagePath>\n" +
                    "            </NS2:Audit>\n" +
                    "        </NS2:UCBRUHeaders>\n" +
                    "    </NS1:Header>\n" +
                    "    <NS1:Body>\n" +
                    "        <gbo:AccountList xmlns:gbo=\"urn:imb:gbo:v2\">\n" +
                    "            <gbo:AccountDetails>\n" +
                    "                <gbo:AccountNo>770078RUR400902024</gbo:AccountNo>\n" +
                    "                <gbo:Branch>024</gbo:Branch>\n" +
                    "                <gbo:CBAccountNo>40802810900014879108</gbo:CBAccountNo>\n" +
                    "                <gbo:Ccy>RUR</gbo:Ccy>\n" +
                    "                <gbo:CcyDigital>810</gbo:CcyDigital>\n" +
                    "                <gbo:Description>SANNIKOVA NATALYA</gbo:Description>\n" +
                    "                <gbo:Status>O</gbo:Status>\n" +
                    "                <gbo:CustomerNo>00770078</gbo:CustomerNo>\n" +
                    "                <gbo:Special>4009</gbo:Special>\n" +
                    "                <gbo:OpenDate>2016-06-24</gbo:OpenDate>\n" +
                    "                <gbo:CreditTransAllowed>Y</gbo:CreditTransAllowed>\n" +
                    "                <gbo:DebitTransAllowed>Y</gbo:DebitTransAllowed>\n" +
                    "                <gbo:CorBank>044525545</gbo:CorBank>\n" +
                    "                <gbo:CorINN>772446207507</gbo:CorINN>\n" +
                    "                <gbo:Positioning>\n" +
                    "                    <gbo:CBAccount>40802810900014879108</gbo:CBAccount>\n" +
                    "                    <gbo:IMBAccountNo>40802810900014879108</gbo:IMBAccountNo>\n" +
                    "                    <gbo:IMBBranch>024</gbo:IMBBranch>\n" +
                    "                    <gbo:HostABSAccountNo>770078RUR400902024</gbo:HostABSAccountNo>\n" +
                    "                    <gbo:HostABSBranch>024</gbo:HostABSBranch>\n" +
                    "                    <gbo:HostABS>MIDAS</gbo:HostABS>\n" +
                    "                </gbo:Positioning>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>GWSAccType</gbo:Name>\n" +
                    "                    <gbo:Value>CURR</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>OperationTypeCodes</gbo:Name>\n" +
                    "                    <gbo:Value>\n" +
                    "                        VIEW=1,DOMPAY=1,DOMTAX=1,DOMCUS=1,CUPADE=1,CUPACO=1,CUSEOD=1,CUSEOC=1,CURBUY=1,CUOPCE=1,\n" +
                    "                    </gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>ParentBranchName</gbo:Name>\n" +
                    "                    <gbo:Value>UCB, Moscow</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>CusSegment</gbo:Name>\n" +
                    "                    <gbo:Value>TIER_I</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:ShadowAccounts>\n" +
                    "                    <gbo:HostABS>FCC</gbo:HostABS>\n" +
                    "                    <gbo:AccountNo>00770078RURENSA101</gbo:AccountNo>\n" +
                    "                    <gbo:Branch>A12</gbo:Branch>\n" +
                    "                </gbo:ShadowAccounts>\n" +
                    "            </gbo:AccountDetails>\n" +
                    "        </gbo:AccountList>\n" +
                    "    </NS1:Body>\n" +
                    "</NS1:Envelope>\n";

    public static String messageMidas =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "<NS1:Envelope xmlns:NS1=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                    "    <NS1:Header>\n" +
                    "        <NS2:UCBRUHeaders xmlns:NS2=\"urn:imb:gbo:v2\">\n" +
                    "            <NS2:Audit>\n" +
                    "                <NS2:MessagePath>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountDetailsNotify.NotificationHandler</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v4</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-03-29T17:50:54.289+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>START</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountListQuery</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-03-29T17:50:54.295+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>START</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountListQuery</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-03-29T17:50:54.405+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>SUCCESS</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountDetailsNotify.Publisher</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-03-29T17:50:54.413+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>SUCCESS</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment></NS2:Comment>\n" +
                    "                    </NS2:Step>\n" +
                    "                </NS2:MessagePath>\n" +
                    "            </NS2:Audit>\n" +
                    "        </NS2:UCBRUHeaders>\n" +
                    "    </NS1:Header>\n" +
                    "    <NS1:Body>\n" +
                    "        <gbo:AccountList xmlns:gbo=\"urn:imb:gbo:v2\">\n" +
                    "            <gbo:AccountDetails>\n" +
                    "                <gbo:AccountNo>695430RUR401102097</gbo:AccountNo>\n" +
                    "                <gbo:Branch>097</gbo:Branch>\n" +
                    "                <gbo:CBAccountNo>40702810400154748352</gbo:CBAccountNo>\n" +
                    "                <gbo:Ccy>RUR</gbo:Ccy>\n" +
                    "                <gbo:CcyDigital>810</gbo:CcyDigital>\n" +
                    "                <gbo:Description>ROSTENERGORESURS</gbo:Description>\n" +
                    "                <gbo:Status>O</gbo:Status>\n" +
                    "                <gbo:CustomerNo>00695430</gbo:CustomerNo>\n" +
                    "                <gbo:Special>4011</gbo:Special>\n" +
                    "                <gbo:OpenDate>2014-12-22</gbo:OpenDate>\n" +
                    "                <gbo:CreditTransAllowed>N</gbo:CreditTransAllowed>\n" +
                    "                <gbo:DebitTransAllowed>N</gbo:DebitTransAllowed>\n" +
                    "                <gbo:CorBank>046027238</gbo:CorBank>\n" +
                    "                <gbo:CorINN>6166083860</gbo:CorINN>\n" +
                    "                <gbo:Positioning>\n" +
                    "                    <gbo:CBAccount>40702810400154748352</gbo:CBAccount>\n" +
                    "                    <gbo:IMBAccountNo>40702810400154748352</gbo:IMBAccountNo>\n" +
                    "                    <gbo:IMBBranch>097</gbo:IMBBranch>\n" +
                    "                    <gbo:HostABSAccountNo>695430RUR401102097</gbo:HostABSAccountNo>\n" +
                    "                    <gbo:HostABSBranch>097</gbo:HostABSBranch>\n" +
                    "                    <gbo:HostABS>MIDAS</gbo:HostABS>\n" +
                    "                </gbo:Positioning>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>GWSAccType</gbo:Name>\n" +
                    "                    <gbo:Value>CURR</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>OperationTypeCodes</gbo:Name>\n" +
                    "                    <gbo:Value>\n" +
                    "                        VIEW=1,DOMPAY=1,DOMTAX=1,DOMCUS=1,CUPADE=1,CUPACO=1,CUSEOD=1,CUSEOC=1,CURBUY=1,CUOPCE=1,\n" +
                    "                    </gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>ParentBranchName</gbo:Name>\n" +
                    "                    <gbo:Value>UCB, Rostov Branch</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>CusSegment</gbo:Name>\n" +
                    "                    <gbo:Value>TIER_I</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:ShadowAccounts>\n" +
                    "                    <gbo:HostABS>FCC</gbo:HostABS>\n" +
                    "                    <gbo:AccountNo>00695430RURCOSA101</gbo:AccountNo>\n" +
                    "                    <gbo:Branch>C04</gbo:Branch>\n" +
                    "                </gbo:ShadowAccounts>\n" +
                    "            </gbo:AccountDetails>\n" +
                    "        </gbo:AccountList>\n" +
                    "    </NS1:Body>\n" +
                    "</NS1:Envelope>";

    public static String messageFCC =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "<NS1:Envelope xmlns:NS1=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                    "    <NS1:Header>\n" +
                    "        <NS2:UCBRUHeaders xmlns:NS2=\"urn:imb:gbo:v2\">\n" +
                    "            <NS2:Audit>\n" +
                    "                <NS2:MessagePath>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountDetailsNotify.NotificationHandler</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v4</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-03-29T17:50:54.289+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>START</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountListQuery</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-03-29T17:50:54.295+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>START</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountListQuery</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-03-29T17:50:54.405+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>SUCCESS</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountDetailsNotify.Publisher</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-03-29T17:50:54.413+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>SUCCESS</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment></NS2:Comment>\n" +
                    "                    </NS2:Step>\n" +
                    "                </NS2:MessagePath>\n" +
                    "            </NS2:Audit>\n" +
                    "        </NS2:UCBRUHeaders>\n" +
                    "    </NS1:Header>\n" +
                    "    <NS1:Body>\n" +
                    "<gbo:AccountList xmlns:gbo=\"urn:imb:gbo:v2\">\n" +
                    "    <gbo:AccountDetails>\n" +
                    "        <gbo:AccountNo>00516770RURPRDC101</gbo:AccountNo>\n" +
                    "        <gbo:Branch>A01</gbo:Branch>\n" +
                    "        <gbo:CBAccountNo>40817810000010696538</gbo:CBAccountNo>\n" +
                    "        <gbo:Ccy>RUR</gbo:Ccy>\n" +
                    "        <gbo:CcyDigital>810</gbo:CcyDigital>\n" +
                    "        <gbo:Description>KOZHEMYAKO D.A.-VE</gbo:Description>\n" +
                    "        <gbo:Status>O</gbo:Status>\n" +
                    "        <gbo:CustomerNo>00516770</gbo:CustomerNo>\n" +
                    "        <gbo:Special>PRDC1</gbo:Special>\n" +
                    "        <gbo:OpenDate>2003-03-29</gbo:OpenDate>\n" +
                    "        <gbo:AltAccountNo>516770RUR405712001</gbo:AltAccountNo>\n" +
                    "        <gbo:Type>S</gbo:Type>\n" +
                    "        <gbo:ATMAvailable>N</gbo:ATMAvailable>\n" +
                    "        <gbo:IsFrozen>N</gbo:IsFrozen>\n" +
                    "        <gbo:IsDormant>N</gbo:IsDormant>\n" +
                    "        <gbo:CreditTransAllowed>Y</gbo:CreditTransAllowed>\n" +
                    "        <gbo:DebitTransAllowed>Y</gbo:DebitTransAllowed>\n" +
                    "        <gbo:ClearingBank>044525545</gbo:ClearingBank>\n" +
                    "        <gbo:CorBank>044525545</gbo:CorBank>\n" +
                    "        <gbo:Positioning>\n" +
                    "            <gbo:CBAccount>40817810000010696539</gbo:CBAccount>\n" +
                    "            <gbo:IMBAccountNo>40817810000010696539</gbo:IMBAccountNo>\n" +
                    "            <gbo:IMBBranch>001</gbo:IMBBranch>\n" +
                    "            <gbo:HostABSAccountNo>00516770RURPRDC101</gbo:HostABSAccountNo>\n" +
                    "            <gbo:HostABSBranch>A01</gbo:HostABSBranch>\n" +
                    "            <gbo:HostABS>FCC</gbo:HostABS>\n" +
                    "        </gbo:Positioning>\n" +
                    "        <gbo:MIS>\n" +
                    "            <gbo:Pool>DFLTPOOL</gbo:Pool>\n" +
                    "            <gbo:TransactionClass>\n" +
                    "                <gbo:Name>TXNMIS1</gbo:Name>\n" +
                    "            </gbo:TransactionClass>\n" +
                    "            <gbo:TransactionClass>\n" +
                    "                <gbo:Name>TXNMIS2</gbo:Name>\n" +
                    "            </gbo:TransactionClass>\n" +
                    "            <gbo:TransactionClass>\n" +
                    "                <gbo:Name>TXNMIS3</gbo:Name>\n" +
                    "            </gbo:TransactionClass>\n" +
                    "            <gbo:TransactionClass>\n" +
                    "                <gbo:Name>TXNMIS4</gbo:Name>\n" +
                    "            </gbo:TransactionClass>\n" +
                    "            <gbo:TransactionClass>\n" +
                    "                <gbo:Name>TXNMIS5</gbo:Name>\n" +
                    "            </gbo:TransactionClass>\n" +
                    "            <gbo:TransactionClass>\n" +
                    "                <gbo:Name>TXNMIS6</gbo:Name>\n" +
                    "            </gbo:TransactionClass>\n" +
                    "            <gbo:TransactionClass>\n" +
                    "                <gbo:Name>TXNMIS7</gbo:Name>\n" +
                    "            </gbo:TransactionClass>\n" +
                    "            <gbo:TransactionClass>\n" +
                    "                <gbo:Name>TXNMIS8</gbo:Name>\n" +
                    "            </gbo:TransactionClass>\n" +
                    "            <gbo:TransactionClass>\n" +
                    "                <gbo:Name>TXNMIS9</gbo:Name>\n" +
                    "            </gbo:TransactionClass>\n" +
                    "            <gbo:TransactionClass>\n" +
                    "                <gbo:Name>TXNMIS10</gbo:Name>\n" +
                    "            </gbo:TransactionClass>\n" +
                    "            <gbo:CompositeClass>\n" +
                    "                <gbo:Name>COMPMIS1</gbo:Name>\n" +
                    "            </gbo:CompositeClass>\n" +
                    "            <gbo:CompositeClass>\n" +
                    "                <gbo:Name>COMPMIS2</gbo:Name>\n" +
                    "            </gbo:CompositeClass>\n" +
                    "            <gbo:CompositeClass>\n" +
                    "                <gbo:Name>COMPMIS3</gbo:Name>\n" +
                    "            </gbo:CompositeClass>\n" +
                    "            <gbo:CompositeClass>\n" +
                    "                <gbo:Name>COMPMIS4</gbo:Name>\n" +
                    "            </gbo:CompositeClass>\n" +
                    "            <gbo:CompositeClass>\n" +
                    "                <gbo:Name>COMPMIS5</gbo:Name>\n" +
                    "            </gbo:CompositeClass>\n" +
                    "            <gbo:CompositeClass>\n" +
                    "                <gbo:Name>COMPMIS6</gbo:Name>\n" +
                    "            </gbo:CompositeClass>\n" +
                    "            <gbo:CompositeClass>\n" +
                    "                <gbo:Name>COMPMIS7</gbo:Name>\n" +
                    "            </gbo:CompositeClass>\n" +
                    "            <gbo:CompositeClass>\n" +
                    "                <gbo:Name>COMPMIS8</gbo:Name>\n" +
                    "            </gbo:CompositeClass>\n" +
                    "            <gbo:CompositeClass>\n" +
                    "                <gbo:Name>COMPMIS9</gbo:Name>\n" +
                    "            </gbo:CompositeClass>\n" +
                    "            <gbo:CompositeClass>\n" +
                    "                <gbo:Name>COMPMIS10</gbo:Name>\n" +
                    "            </gbo:CompositeClass>\n" +
                    "            <gbo:CostCode>\n" +
                    "                <gbo:Name>COSTCOD1</gbo:Name>\n" +
                    "            </gbo:CostCode>\n" +
                    "            <gbo:CostCode>\n" +
                    "                <gbo:Name>COSTCOD2</gbo:Name>\n" +
                    "            </gbo:CostCode>\n" +
                    "            <gbo:CostCode>\n" +
                    "                <gbo:Name>COSTCOD3</gbo:Name>\n" +
                    "            </gbo:CostCode>\n" +
                    "            <gbo:CostCode>\n" +
                    "                <gbo:Name>COSTCOD4</gbo:Name>\n" +
                    "            </gbo:CostCode>\n" +
                    "            <gbo:CostCode>\n" +
                    "                <gbo:Name>COSTCOD5</gbo:Name>\n" +
                    "            </gbo:CostCode>\n" +
                    "        </gbo:MIS>\n" +
                    "        <gbo:UDF>\n" +
                    "            <gbo:Name>GWSAccType</gbo:Name>\n" +
                    "            <gbo:Value>CURR</gbo:Value>\n" +
                    "        </gbo:UDF>\n" +
                    "        <gbo:UDF>\n" +
                    "            <gbo:Name>OperationTypeCodes</gbo:Name>\n" +
                    "            <gbo:Value>VIEW=0,DOMPAY=1,CUPADE=0,CUPACO=0,</gbo:Value>\n" +
                    "        </gbo:UDF>\n" +
                    "        <gbo:UDF>\n" +
                    "            <gbo:Name>ParentBranchName</gbo:Name>\n" +
                    "            <gbo:Value>UCB, Moscow</gbo:Value>\n" +
                    "        </gbo:UDF>\n" +
                    "        <gbo:UDF>\n" +
                    "            <gbo:Name>CusSegment</gbo:Name>\n" +
                    "            <gbo:Value>TIER_I</gbo:Value>\n" +
                    "        </gbo:UDF>\n" +
                    "                <gbo:ShadowAccounts>\n" +
                    "                    <gbo:HostABS>FCC</gbo:HostABS>\n" +
                    "                    <gbo:AccountNo>00695430RURCOSA101</gbo:AccountNo>\n" +
                    "                    <gbo:Branch>C04</gbo:Branch>\n" +
                    "                </gbo:ShadowAccounts>\n" +
                    "    </gbo:AccountDetails>\n" +
                    "</gbo:AccountList>\n" +
            /*
            "        <gbo:AccountList xmlns:gbo=\"urn:imb:gbo:v2\">\n" +
            "            <gbo:AccountDetails>\n" +
            "                <gbo:AccountNo>695430RUR401102097</gbo:AccountNo>\n" +
            "                <gbo:Branch>097</gbo:Branch>\n" +
            "                <gbo:CBAccountNo>40702810400154748352</gbo:CBAccountNo>\n" +
            "                <gbo:Ccy>RUR</gbo:Ccy>\n" +
            "                <gbo:CcyDigital>810</gbo:CcyDigital>\n" +
            "                <gbo:Description>ROSTENERGORESURS</gbo:Description>\n" +
            "                <gbo:Status>O</gbo:Status>\n" +
            "                <gbo:CustomerNo>00695430</gbo:CustomerNo>\n" +
            "                <gbo:Special>4011</gbo:Special>\n" +
            "                <gbo:OpenDate>2014-12-22</gbo:OpenDate>\n" +
            "                <gbo:CreditTransAllowed>N</gbo:CreditTransAllowed>\n" +
            "                <gbo:DebitTransAllowed>N</gbo:DebitTransAllowed>\n" +
            "                <gbo:CorBank>046027238</gbo:CorBank>\n" +
            "                <gbo:CorINN>6166083860</gbo:CorINN>\n" +
            "                <gbo:Positioning>\n" +
            "                    <gbo:CBAccount>40702810400154748352</gbo:CBAccount>\n" +
            "                    <gbo:IMBAccountNo>40702810400154748352</gbo:IMBAccountNo>\n" +
            "                    <gbo:IMBBranch>097</gbo:IMBBranch>\n" +
            "                    <gbo:HostABSAccountNo>695430RUR401102097</gbo:HostABSAccountNo>\n" +
            "                    <gbo:HostABSBranch>097</gbo:HostABSBranch>\n" +
            "                    <gbo:HostABS>MIDAS</gbo:HostABS>\n" +
            "                </gbo:Positioning>\n" +
            "                <gbo:UDF>\n" +
            "                    <gbo:Name>GWSAccType</gbo:Name>\n" +
            "                    <gbo:Value>CURR</gbo:Value>\n" +
            "                </gbo:UDF>\n" +
            "                <gbo:UDF>\n" +
            "                    <gbo:Name>OperationTypeCodes</gbo:Name>\n" +
            "                    <gbo:Value>\n" +
            "                        VIEW=1,DOMPAY=1,DOMTAX=1,DOMCUS=1,CUPADE=1,CUPACO=1,CUSEOD=1,CUSEOC=1,CURBUY=1,CUOPCE=1,\n" +
            "                    </gbo:Value>\n" +
            "                </gbo:UDF>\n" +
            "                <gbo:UDF>\n" +
            "                    <gbo:Name>ParentBranchName</gbo:Name>\n" +
            "                    <gbo:Value>UCB, Rostov Branch</gbo:Value>\n" +
            "                </gbo:UDF>\n" +
            "                <gbo:UDF>\n" +
            "                    <gbo:Name>CusSegment</gbo:Name>\n" +
            "                    <gbo:Value>TIER_I</gbo:Value>\n" +
            "                </gbo:UDF>\n" +
            "                <gbo:ShadowAccounts>\n" +
            "                    <gbo:HostABS>FCC</gbo:HostABS>\n" +
            "                    <gbo:AccountNo>00695430RURCOSA101</gbo:AccountNo>\n" +
            "                    <gbo:Branch>C04</gbo:Branch>\n" +
            "                </gbo:ShadowAccounts>\n" +
            "            </gbo:AccountDetails>\n" +
            "        </gbo:AccountList>\n" +*/
                    "    </NS1:Body>\n" +
                    "</NS1:Envelope>";

//    public void processTest(String s) throws Exception {
//        String topic = getBodyXML(fullTopicTest, 1L);
//        if (topic == null) return;

    // Если ошибка, то JAXBException
//        AccountList accountList = unmarshallTopicJAXB(topic, 1L);
//        ru.rbt.barsgl.ejb.controller.operday.task.srvacc.imb.gbo.v2.AccountList accountList = unmarshallTopicJAXB(topic, 1L);
//    }
}
