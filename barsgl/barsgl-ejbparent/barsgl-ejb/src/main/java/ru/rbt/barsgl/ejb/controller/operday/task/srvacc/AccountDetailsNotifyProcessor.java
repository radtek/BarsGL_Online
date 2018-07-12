package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import org.apache.log4j.Logger;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.entity.acc.AcDNJournal;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountController;
import ru.rbt.barsgl.ejb.repository.AcDNJournalRepository;
import ru.rbt.barsgl.ejb.repository.GLAccountRepository;
import ru.rbt.barsgl.ejb.repository.dict.CurrencyCacheRepository;
import ru.rbt.barsgl.shared.enums.DealSource;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.ExceptionUtils;

import javax.ejb.EJB;
import javax.inject.Inject;

import java.sql.DataTruncation;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.AccountDetailsNotify;
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Sources.FC12;
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Sources.FCC;
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Status.ENRICHED;
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Status.ERROR;
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Status.PROCESSED;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;

/**
 * Created by er18837 on 03.05.2018.
 */
public class AccountDetailsNotifyProcessor extends CommonNotifyProcessor {
    private static final Logger log = Logger.getLogger(AccountDetailsNotifyProcessor.class);

    public static final String journalName = "GL_ACDENO";
    public static final String charsetName = "UTF-8";

    public static final String parentNodeName = "AccountDetails";
    public static final String parentNodePath = "Body/AccountList/AccountDetails";

    // TODO нельзя делать static SimpleDateFormat!!
    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    // многопоточный форматтер
    protected static final DateTimeFormatter sdf = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    static XmlParam[] paramNamesFCC = {
            new XmlParam("AccountNo", true, 20),
            new XmlParam("CBAccountNo", false, 20),
            new XmlParam("Branch", false, 3),
            new XmlParam("Ccy", false, 3),
            new XmlParam("CcyDigital", true, 3),
            new XmlParam("Status", false, 1),
            new XmlParam("CustomerNo", false, 8),
            new XmlParam("OpenDate", false, 0),
            new XmlParam("CloseDate", true, 0),
            new XmlParam("Special", true, 0),
            new XmlParam("Description", true, 0),
            new XmlParam("Positioning/HostABS", false, 0)};

    static XmlParam[] paramNamesFC12 = {
            new XmlParam("CBAccountNo", false, 20),
            new XmlParam("Branch", false, 3),
            new XmlParam("Status", false, 1),
            new XmlParam("CloseDate", true, 0),
            new XmlParam("Positioning/HostABS", false, 20)};

    @Inject
    private GLAccountRepository glAccountRepository;

    @Inject
    private GLAccountController glAccountController;

    @Inject
    private CurrencyCacheRepository bankCurrencyRepository;

    @EJB
    AuditController auditController;

    @EJB
    AcDNJournalRepository journalRepository;

    @Override
    protected void updateLogStatusError(Long journalId, String message) {
        journalRepository.updateLogStatus(journalId, ERROR, "Ошибка во время распознования XML: " + message);
        return;
    }

    public void process(AcDNJournal.Sources source, String textMessage, final Long jId) throws Exception {
        switch(source) {
            case FC12:
            case FCC_CLOSE:
                processFC12(FC12, textMessage, jId);
                return;
            case FCC:
                processFCC(source, textMessage, jId);
                return;
            default:
                throw new DefaultApplicationException("Неверный источник: " + source.name());
        }
    }

    public void processFC12(AcDNJournal.Sources source, String textMessage, final Long jId) throws Exception {
        Map<String, String> xmlData = readFromXML(textMessage, charsetName, parentNodeName, parentNodePath, paramNamesFC12, jId);
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

    public void processFCC(AcDNJournal.Sources source, String textMessage, final Long jId) throws Exception {
        // Преобразуем данные из сообщения
        Map<String, String> xmlData = readFromXML(textMessage, charsetName, parentNodeName, parentNodePath, paramNamesFCC, jId);
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

    private void closeAccountAny(AcDNJournal.Sources source, String hostABS, String bsaacid, Map<String, String> xmlData, Long jId) throws Exception {
        String closeDateXml = xmlData.get("CloseDate");
        GLAccount account;
        if (!isEmpty(bsaacid) && !isEmpty(hostABS) && !isEmpty(closeDateXml) && source.name().equals(hostABS)) {
            Date closeDate = df.parse(closeDateXml);
            if (glAccountRepository.isExistsGLAccount(bsaacid, closeDate)) {
                glAccountController.closeGLAccountNotify(bsaacid, closeDate);
                journalRepository.updateLogStatus(jId, PROCESSED, "Счет с bsaacid=" + bsaacid + " закрыт");
                auditController.info(AccountDetailsNotify, String.format("Счет '%s' закрыт по нотификации от %s", bsaacid, source.name()));
            } else if (null != (account = glAccountRepository. findGLAccount(bsaacid))) {
                journalRepository.updateLogStatus(jId, ERROR, String.format("Счет '%s' уже закрыт с датой ''", bsaacid, df.format(account.getDateClose()))); //new DateUtils().onlyDateString(account.getDateClose())));
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

        DataRecord recMidas = glAccountRepository.selectFirst("SELECT midas_branch FROM DH_BR_MAP where fcc_branch=?", branch);
        DataRecord recGlacc = glAccountRepository.selectFirst("SELECT branch from gl_acc where bsaacid=?", bsaacid);

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
            glAccountRepository.executeNativeUpdate("UPDATE GL_ACC SET branch=? WHERE BSAACID=?", midasBranch, bsaacid);
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

        Date openDate = new SimpleDateFormat("yyyy-MM-dd").parse(xmlData.get("OpenDate"));
//        java.sql.Date openDate = java.sql.Date.valueOf(LocalDate.parse(xmlData.get("OpenDate"), sdf));
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
        keys.setDealSource(DealSource.FCC.getLabel());

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

}
