package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;


import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.repository.od.OperdayRepository;
import ru.rbt.barsgl.ejb.entity.acc.*;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.sec.AuditRecord;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountService;
import ru.rbt.barsgl.ejb.repository.*;
import ru.rbt.barsgl.ejb.security.AuditController;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;

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
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Status.*;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.ifEmpty;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.isEmpty;

/**
 * Created by ER22228 on 29.03.2016.
 */
@SuppressWarnings("ALL")
@Stateless
@LocalBean
public class AccountDetailsNotifyProcessor implements Serializable {
    private static final Logger log = Logger.getLogger(AccountDetailsNotifyProcessor.class);

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @EJB
    private GLAccountRequestRepository accountRequestRepository;

    @Inject
    private AccRlnRepository accRlnRepository;

    @Inject
    private BsaAccRepository bsaAccRepository;

    @Inject
    private AccRepository accRepository;

    @EJB
    private AcDNJournalRepository journalRepository;

    @EJB
    private AcDNJournalDataRepository journalDataRepository;

    @Inject
    private GLAccountService glAccountService;

    @Inject
    private OperdayRepository operdayRepository;

    @EJB
    private AuditController auditController;

    @EJB
    private CoreRepository coreRepository;

    @Inject
    private OperdayController operdayController;

    static String[] paramNamesOpen = {"AccountNo", "Branch", "CBAccountNo", "Ccy", "CcyDigital", "Description", "Status",
            "CustomerNo", "Special", "OpenDate", "Positioning/HostABS"};

    static String[] paramNamesClose = {"Branch", "CBAccountNo", "Status", "CloseDate", "Positioning/HostABS"};

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public void process(AcDNJournal.Sources source, String incomingEnvelope, final Long jId) throws Exception {
        if (AcDNJournal.Sources.FCC_CLOSE.equals(source)) {
            processClose(source, incomingEnvelope, jId);
        } else {
            processOpen(source, incomingEnvelope, jId);
        }
    }

    public void processClose(AcDNJournal.Sources source, String incomingEnvelope, final Long jId) throws Exception {
        Map<String, String> xmlData = readFromXML(incomingEnvelope, jId, paramNamesClose);
        if (xmlData == null) {
            // Запись в аудит, в таблицу аудита, в лог и возврат
            journalRepository.updateLogStatus(jId, ERROR, "Ошибка во время распознования XML");
            return;
        }

        String branch = xmlData.get("Branch");
        String bsaacid = xmlData.get("CBAccountNo");
        String status = xmlData.get("Status");
        String hostABS = xmlData.get("Positioning/HostABS");
        String closeDate = xmlData.get("CloseDate");

        if(!isEmpty(status) && "C".equals(status)) {
            if (!isEmpty(bsaacid) && !isEmpty(hostABS) && !isEmpty(closeDate) && "FC12".equals(hostABS)) {
                DataRecord accRln = accRlnRepository.findByBsaacid(bsaacid);
                if (accRln != null) {
                    Date storingDate = sdf.parse(closeDate);
                    if ((accRln.getDate("drlnc") == null ? "2029-01-01" : sdf.format(accRln.getDate("drlnc"))).equals("2029-01-01")) {
                        coreRepository.executeNativeUpdate("UPDATE ACCRLN SET DRLNC=? WHERE BSAACID=?", storingDate, bsaacid);
                        coreRepository.executeNativeUpdate("UPDATE GL_ACC SET DTC=? WHERE BSAACID=?", storingDate, bsaacid);
                        coreRepository.executeNativeUpdate("UPDATE BSAACC SET BSAACC=? WHERE ID=?", storingDate, bsaacid);

                        journalRepository.updateLogStatus(jId, PROCESSED, "Счет с bsaacid=" + bsaacid + " закрыт");
                        auditController.info(AuditRecord.LogCode.AccountDetailsNotify, "Счет с bsaacid=" + bsaacid + " закрыт по нотификации от FC12");
                    } else {
                        journalRepository.updateLogStatus(jId, ERROR, "Счет с bsaacid=" + bsaacid + " уже закрыт ранее");
                        return;
                    }
                } else {
                    journalRepository.updateLogStatus(jId, ERROR, "Счет с bsaacid=" + bsaacid + " не существует ");
                    return;
                }
            } else {
                journalRepository.updateLogStatus(jId, ERROR, "Ошибка в данных bsaacid / hostABS / closeDate");
                return;
            }
        } else if (!isEmpty(branch)){
            DataRecord recMidas = coreRepository.selectFirst("SELECT midas_branch FROM DWH.DH_BR_MAP where fcc_branch=?", branch);
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
                auditController.info(AuditRecord.LogCode.AccountDetailsNotify, "Branch не менялся");
            }else{
                coreRepository.executeNativeUpdate("UPDATE GL_ACC SET branch=? WHERE BSAACID=?", midasBranch, bsaacid);
                journalRepository.updateLogStatus(jId, PROCESSED, "На счете " + bsaacid + " branch "+glaccBranch+"("+branch+") заменен на "+midasBranch);
                auditController.info(AuditRecord.LogCode.AccountDetailsNotify, "На счете " + bsaacid + " branch "+glaccBranch+"("+branch+") заменен на "+midasBranch);
            }
            return;
        } else{
            journalRepository.updateLogStatus(jId, ERROR, "Запрос не про закрытие счёта");
            return;
        }

        log.info("Обработка одного сообщения завершена. cbAccount:" + xmlData.get("CBAccountNo"));
    }

    public void processOpen(AcDNJournal.Sources source, String incomingEnvelope, final Long jId) throws Exception {
        // Преобразуем данные из сообщения
        Map<String, String> xmlData = readFromXML(incomingEnvelope, jId, paramNamesOpen);
        if (xmlData == null) {
            // Запись в аудит, в таблицу аудита, в лог и возврат
            journalRepository.updateLogStatus(jId, ERROR, "Ошибка во время распознования XML");
            return;
        }

        String bsaacid = xmlData.get("CBAccountNo");
        if (!isEmpty(bsaacid)) {
            if( bsaacid.startsWith("423") || bsaacid.startsWith("426") ){
                journalRepository.updateLogStatus(jId, ERROR, "Депозитный счет " + bsaacid + " не открывается через сервис");
                return;              
            }
              
            DataRecord accRln = accRlnRepository.findByBsaacid(bsaacid);
            
            if (accRln != null && (accRln.getDate("drlnc") == null || ifEmpty(xmlData.get("OpenDate"), "").compareTo(sdf.format(accRln.getDate("drlnc"))) <= 0)) {
                journalRepository.updateLogStatus(jId, ERROR, "Счет с bsaacid=" + bsaacid + " уже существует в ACCRLN");
                return;
            }
        } else {
            journalRepository.updateLogStatus(jId, ERROR, "bsaacid не задан");
            return;
        }


        if (AcDNJournal.Sources.FCC.equals(source) && !"FCC".equals(xmlData.get("Positioning/HostABS"))) {
            journalRepository.updateLogStatus(jId, ERROR,
                    "Не обрабатываем сообщение из FCC c HostABS=" + xmlData.get("Positioning/HostABS"));
            return;
        }

        // Если FCC, то обогащение. Придумываем счёт MIDAS
        // Если ошибка в процессе обогащения - вернётся false

        // validate после enrichment потому что для FCC-ветки нужен MIDAS_BRANCH
        if (validateAll(source, jId, xmlData) && enrichmentData(source, xmlData, jId)) {
            // Вставка в таблицы
            openAccounts(jId, source, xmlData);
        }
        log.info("Обработка одного сообщения завершена. cbAccount:" + xmlData.get("CBAccountNo"));
    }

    private Map<String, String> readFromXML(String bodyXML, Long jId, String[] paramNames) throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        org.w3c.dom.Document doc = null;
        try {
            DocumentBuilder b = DocumentBuilderFactory.newInstance().newDocumentBuilder();
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
        XPath xPath = XPathFactory.newInstance().newXPath();
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

    private boolean validateAll(AcDNJournal.Sources source, Long jId, Map<String, String> xmlData) throws Exception {
//        try {
//            if (!journalDataRepository.validateClientNum(source, accountList)) {
//                journalRepository.updateLogStatus(jId, ERROR, "Ошибка проверки номера клиента");
//                return false;
//            }

        if (!journalDataRepository.validateCcy(xmlData.get("Ccy"))) {
            journalRepository.updateLogStatus(jId, ERROR, "Ошибка проверки кода валюты " + xmlData.get("Ccy"));
            return false;
        }

        if (!journalDataRepository.validateBranch(source, xmlData.get("Branch"))) {
            journalRepository.updateLogStatus(jId, ERROR, "Ошибка проверки кода бранча " + xmlData.get("Branch"));
            return false;
        }

        return true;

//        } catch (SQLException e) {
//            journalRepository.updateLogStatus(jId, ERROR, "Ошибка проверки кода бранча " + xmlData.get("Branch"));
//            log.error("Ошибка при выполнении запроса", e);
//        }
//        return false;
    }

    private boolean enrichmentData(AcDNJournal.Sources source, Map<String, String> xmlData, Long jId) throws Exception {
        if (source.equals(AcDNJournal.Sources.FCC)) {
            xmlData.put("Special", "0000"); // От FCC значение ACOD переопределяем
//            try {
            String midasBranch = journalDataRepository.selectMidasBranchByBranch(xmlData.get("Branch"));
            if (!isEmpty(midasBranch)) {
                xmlData.put("Branch", midasBranch);
                if (!isEmpty(midasBranch)) {
                    String pseudoAcid = null;
                    
                    for (int i = 99; i > 9; i--) {
                        pseudoAcid = xmlData.get("CustomerNo") + xmlData.get("Ccy") + "0000" + i + midasBranch;
                        if (isEmpty(journalDataRepository.existsAcid(pseudoAcid))) {
                            break;
                        }
                        pseudoAcid = null;
                    }
                    
                    if (pseudoAcid == null) {
                        pseudoAcid =  xmlData.get("CustomerNo") + xmlData.get("Ccy") + "0000" + "90" + midasBranch;
                    }
                    
                    xmlData.put("AccountNo", pseudoAcid);
                } else {
                    journalRepository.updateLogStatus(jId, ERROR, "Параметр Midas Branch не вычислен. SELECT MIDAS_BRANCH FROM DWH.DH_BR_MAP WHERE FCC_BRANCH=? :" + xmlData.get("Branch"));
                    return false;
                }
                journalRepository.updateLogStatus(jId, ENRICHED, "");
            } else {
                return false;
            }
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
        }

        if (xmlData.get("AccountNo").length() == 18) {
            xmlData.put("AccountNo", "00" + xmlData.get("AccountNo"));
        }

        xmlData.put("CustomerNo", format("%08d", Integer.parseInt(xmlData.get("CustomerNo"))));

//        String descr = ifEmpty(xmlData.get("Description"), "");
//        descr = descr.length() > 120 ? descr.substring(0, 120) : descr;
//        xmlData.put("Description", descr);

        return true;
    }

    private void openAccounts(Long jId, AcDNJournal.Sources source, Map<String, String> xmlData) throws Exception {
        AcDNJournal journal = journalRepository.findById(AcDNJournal.class, jId);
        if (journal.getStatus().equals(AcDNJournal.Status.ERROR)) {
            // Возможно нужно логировать
            return;
        }
//        try {
        boolean existsInAccrln = false, existsInBsaacc = false, existsInAcc = false;

        String bsaacid = xmlData.get("CBAccountNo");
        DataRecord accRln = accRlnRepository.findByBsaacid(bsaacid);
        if (accRln != null && (accRln.getDate("drlnc") == null || ifEmpty(xmlData.get("OpenDate"), "").compareTo(sdf.format(accRln.getDate("drlnc"))) <= 0)) {
            existsInAccrln = true;
            journalRepository.updateLogStatus(jId, ERROR, "Счет с bsaacid=" + bsaacid + " уже существует в ACCRLN");
            return;
        }

        Optional<BsaAcc> bsaAcc = bsaAccRepository.findBsaAcc(bsaacid);
        if (bsaAcc.isPresent() && (bsaAcc.get().getDateClose() == null || ifEmpty(xmlData.get("OpenDate"), "").compareTo(sdf.format(bsaAcc.get().getDateClose())) <= 0)) {
            existsInBsaacc = true;
            journalRepository.updateLogStatus(jId, ERROR, "Счет с bsaacid=" + bsaacid + " уже существует в BSAACC");
            return;
        }

        String acid = xmlData.get("AccountNo");
        DataRecord acc = accRepository.findByAcid(acid);
        if (acc != null/* && (acc.getDate("dacc") == null || ifEmpty(xmlData.get("OpenDate"), "").compareTo(sdf.format(acc.getDate("dacc"))) <= 0)*/) {
            existsInAcc = true;
        }

        GLAccount glAccount = null;
        Short customerType = null;
        String ccodeDr = null, psavDr = null;
        AcDNJournal.Status status = PROCESSED;
        String logMessage = "";

        customerType = journalDataRepository.findCustomerType(Long.parseLong(xmlData.get("CustomerNo")), xmlData.get("CBAccountNo").substring(0, 5));
        ccodeDr = journalDataRepository.findCCode(xmlData.get("Branch"));
        psavDr = journalDataRepository.findPsav(xmlData.get("CBAccountNo").substring(0, 5));
        glAccount = fillGlAccount(source, xmlData, ccodeDr, psavDr);

        if (existsInAccrln && existsInBsaacc && existsInAcc) {
            logMessage = "Записи во всех таблицах уже существуют";
        }

        if (existsInAcc || existsInAccrln || existsInBsaacc) {
            status = ERROR;
        } else {
            createCounts(source, xmlData, glAccount, customerType, ccodeDr, psavDr);
        }

        if (status.equals(PROCESSED)) {
            logMessage = xmlData.get("AccountNo") + "/" + bsaacid;
        }

        journalRepository.updateLogStatus(jId, status, logMessage);
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
    }

    private void createCounts(AcDNJournal.Sources source, Map<String, String> xmlData, GLAccount glAccount, Short customerType, String ccodeDr, String psavDr) throws SQLException, ParseException {
        createAccrlnRecord(source, xmlData, customerType, ccodeDr, psavDr);
        bsaAccRepository.createBsaAcc(glAccount);
        createAccRecord(source, xmlData, glAccount);
    }

    private void createAccRecord(AcDNJournal.Sources source, Map<String, String> xmlData, GLAccount glAccount) {
        glAccount.setBranch(xmlData.get("Branch"));
        glAccount.setCustomerNumberD(Integer.parseInt(xmlData.get("CustomerNo")));
        glAccount.setAccountCode(Short.parseShort(xmlData.get("Special")));

        glAccount.setAccountSequence(Short.parseShort(glAccount.getAcid().substring(15, 17)));

        glAccount.setDescription(xmlData.get("Description"));
        glAccount.setCustomerNumber(String.valueOf(Integer.parseInt(xmlData.get("CustomerNo")))); // для АСС без лидирующих нулей
        accRepository.createAcc(glAccount);
    }

    private GLAccount fillGlAccount(AcDNJournal.Sources source, Map<String, String> xmlData, String ccodeDr, String psavDr) throws ParseException {
        GLAccount glAccount = new GLAccount();
        String acid = xmlData.get("AccountNo");
        String bsaacid = xmlData.get("CBAccountNo");
//      save(glAccount) в дальнейшем не вызывается, поэтому glAccount.id=null
//2        GLAccount glAccount = new GLAccount(accountRequestRepository.getGlAccId(acid, bsaacid));
        glAccount.setBsaAcid(bsaacid);
        glAccount.setAcid(acid);

        BankCurrency bankCurrency = new BankCurrency(xmlData.get("Ccy"));
        bankCurrency.setDigitalCode(xmlData.get("CcyDigital"));
        glAccount.setCurrency(bankCurrency);

        glAccount.setCompanyCode(ccodeDr);

        glAccount.setDateOpen(sdf.parse(xmlData.get("OpenDate")));
        glAccount.setDateClose(Date.from(LocalDate.of(2029, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()));

        glAccount.setPassiveActive(psavDr);

        glAccount.setRelationType(GLAccount.RelationType.ZERO);
        glAccount.setCustomerNumber(xmlData.get("CustomerNo"));

        return glAccount;
    }

    private void createAccrlnRecord(AcDNJournal.Sources source, Map<String, String> xmlData, Short customerType, String ccodeDr, String psavDr) throws SQLException, ParseException {
        GlAccRln newAccRln = new GlAccRln();
        AccRlnId accRlnId = new AccRlnId();
        accRlnId.setAcid(xmlData.get("AccountNo"));

        accRlnId.setBsaAcid(xmlData.get("CBAccountNo"));
        newAccRln.setId(accRlnId);
        newAccRln.setRelationType("0");
        newAccRln.setDateOpen(sdf.parse(xmlData.get("OpenDate")));
        newAccRln.setDateClose(Date.from(LocalDate.of(2029, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        newAccRln.setCustomerType(customerType);
        newAccRln.setCustomerNumber(xmlData.get("CustomerNo"));
        newAccRln.setCompanyCode(ccodeDr);
        newAccRln.setBssAccount(xmlData.get("CBAccountNo").substring(0, 5));
        newAccRln.setPassiveActive(psavDr);

        newAccRln.setAccountCode(xmlData.get("Special"));//source.equals(AcDNJournal.Sources.MIDAS_OPEN) ? xmlData.get("Special")/*accRlnId.getAcid().substring(11, 15)*/ : "0000");
        newAccRln.setCurrencyD(xmlData.get("CcyDigital"));
        newAccRln.setIncludeExclude("0");
        newAccRln.setTransactSrc("000");
        newAccRln.setPlCode("");
        newAccRln.setPairBsa("");

        accRlnRepository.save(newAccRln);
    }

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
                    "        <gbo:OpenDate>2002-03-29</gbo:OpenDate>\n" +
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
