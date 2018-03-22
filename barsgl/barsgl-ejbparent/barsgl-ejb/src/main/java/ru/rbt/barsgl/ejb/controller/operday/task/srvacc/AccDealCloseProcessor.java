package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import org.apache.log4j.Logger;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountController;
import ru.rbt.barsgl.ejb.repository.AcDNJournalRepository;
import ru.rbt.barsgl.ejb.repository.CloseAccountsRepository;
import ru.rbt.barsgl.ejb.repository.GLAccountRepository;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.ExceptionUtils;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.Serializable;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.util.*;

import static ru.rbt.audit.entity.AuditRecord.LogCode.AccDealCloseTask;
import static ru.rbt.audit.entity.AuditRecord.LogCode.AccountDetailsNotify;
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Status.ERROR;
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Status.PROCESSED;
import static ru.rbt.barsgl.ejb.entity.acc.GLAccount.CloseType.Cancel;
import static ru.rbt.barsgl.ejb.entity.acc.GLAccount.CloseType.Change;
import static ru.rbt.barsgl.ejb.entity.acc.GLAccount.CloseType.Normal;
import static ru.rbt.barsgl.shared.enums.DealSource.KondorPlus;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;
import static ru.rbt.ejbcore.validation.ErrorCode.ACCCLOSE_ERROR;

/**
 * Created by er18837 on 16.03.2018.
 */
@Stateless
@LocalBean
public class AccDealCloseProcessor extends CommonNotifyProcessor implements Serializable {
    private static final Logger log = Logger.getLogger(CustomerNotifyProcessor.class);

    public static final String journalName = "GL_ACDENO";
    public static final String charsetName = "UTF-8";

    private static final String parentNodeName = "SGLAccountTBOCloseRequest";
    private static final XmlParam[] paramNamesDeal = {
             new XmlParam("BSAACID",   "AccNum",     false, 20)
            ,new XmlParam("IS_ERRACC", "IsErrAcc",   false, 1)
            ,new XmlParam("DEALID",    "DealID",     false, 20)
    };

    @EJB
    AcDNJournalRepository journalRepository;

    @Inject
    GLAccountController glAccountController;

    @Inject
    GLAccountRepository glAccountRepository;

    @EJB
    CloseAccountsRepository closeAccountsRepository;

    @Inject
    OperdayController operdayController;

    @Inject
    DateUtils dateUtils;

    @Inject
    private PropertiesRepository propertiesRepository;

    @EJB
    AuditController auditController;

    @Override
    protected void updateLogStatusError(Long journalId, String message) {
        journalRepository.updateLogStatus(journalId, ERROR, "Ошибка во время распознования XML: " + message);
        return;
    }

    // выполняется в своей транзакции
    public String process(String fullTopic, final Long journalId) throws Exception {
        // разбор сообщения и обработка сетов
        List<String> errList = new ArrayList<>();
        Map<String, String> xmlData = processMessage(fullTopic, journalId);

        // ответное сообщеине
        String answerBody = createOutMessage(xmlData);

        if (isEmpty(xmlData.get("ERROR"))) {
            journalRepository.updateComment(journalId, String.format("Счет с bsaacid = '%s' %s",
                    xmlData.get("BSAACID"), (!isEmpty(xmlData.get("DTC")) ? "закрыт" : "поставлен в очередь на закрытие")));
        }

        return answerBody;
    }

    /*
    возвращает сообщений об ошибке
     */
    private Map<String, String> processMessage(String fullTopic, Long journalId) throws Exception {
        Map<String, String> xmlData = readFromXML(fullTopic, charsetName, parentNodeName, paramNamesDeal, journalId);
        if (xmlData == null) {
            // Запись в аудит ???
//            updateLogStatusError(journalId, "Ошибка при разборе сообщения XML");      // должен уст Error в процедуре
            xmlData = new HashMap<>();
            xmlData.put("ERROR", "Ошибка во время распознавания XML-сообщения");
            return xmlData;
        }

        String err = validateXmlParams(xmlData, paramNamesDeal);
        if (!isEmpty(err)) {
            updateLogStatusError(journalId, err);
            xmlData.put("ERROR", err);
            return xmlData;
        }

        String bsaAcid = xmlData.get("BSAACID");
        String dealId = xmlData.get("DEALID");
        GLAccount.CloseType errorFlag = null;
        GLAccount mainAccount = null;
        Date dateClose = null;
        Date curDate = operdayController.getOperday().getCurrentDate();
        try {
            errorFlag = getCloseFlagWithCheck(bsaAcid, xmlData.get("IS_ERRACC"));
            mainAccount = findAccountByDealWithCheck(bsaAcid, dealId);

            dateClose = processOneAccount(mainAccount, curDate, errorFlag);
            if (Cancel == errorFlag) {
                List<GLAccount> accounts = closeAccountsRepository.getDealCustAccounts(mainAccount);
                for (GLAccount account: accounts) {
                    processOneAccount(account, curDate, Normal);
                }
            }
        } catch (ValidationError ex) {
            String msg = ValidationError.getErrorText(ex.getMessage());
            journalRepository.updateLogStatus(journalId, ERROR, msg);
            auditController.warning(AccDealCloseTask, msg, journalName, journalId.toString(), ex);
            xmlData.put("ERROR", msg);
            return xmlData;
        } catch (Throwable ex) {
            String msg = getErrorMessage(ex);
            journalRepository.updateLogStatus(journalId, ERROR, msg);
            auditController.error(AccDealCloseTask, msg, journalName, journalId.toString(), ex);
            xmlData.put("ERROR", msg);
            return xmlData;
        }
        xmlData.put("DTO", dateUtils.onlyDateString(mainAccount.getDateOpen()));
        if (null != dateClose)
            xmlData.put("DTC", dateUtils.onlyDateString(dateClose));

        return xmlData;
    }

    private Date processOneAccount(GLAccount account, Date curDate, GLAccount.CloseType closeType) throws Exception {
        Date dateClose = null;
        // проверить остаток
        if (glAccountRepository.isAccountBalanceZero(account.getBsaAcid(), account.getAcid(), curDate)) {
            // нулевой, закрыть счет
            dateClose = account.getDateRegister().equals(curDate) ? account.getDateOpen() : curDate;
            glAccountController.closeGLAccountDeals(account, dateClose, closeType);
            auditController.info(AccDealCloseTask, String.format("Счет с bsaacid = '%s' закрыт по нотификации от %s", account.getBsaAcid(), KondorPlus.name()));
        } else { // не нулевой
            // добавить запись в очередь на закрытие
//            journalRepository.executeInNewTransaction(persistence ->
            closeAccountsRepository.moveToWaitClose(account, curDate, closeType); //);
            // обновить OpenType
            if (Normal != closeType) {
                glAccountController.updateGLAccountOpenType(account, GLAccount.OpenType.ERR);
            }
        }
        return dateClose;
    }

    private GLAccount findAccountByDealWithCheck(String bsaAcid, String dealId) {
        GLAccount account = closeAccountsRepository.getAccountByDeal(bsaAcid, dealId);
        if (null == account) {
            throw new ValidationError(ACCCLOSE_ERROR, String.format("Не найден счет в GL_ACC c BSAACID = '%s'", bsaAcid));
        } else if (null !=  account.getDateClose()) {
            throw new ValidationError(ACCCLOSE_ERROR, String.format("Cчет c BSAACID = '%s' уже закрыт с датой закрытия '%s'"
                    , bsaAcid, dateUtils.onlyDateString(account.getDateClose())));
        }
        return account;
    }

    private GLAccount.CloseType getCloseFlagWithCheck(String bsaAcid, String flag) {

        if (Cancel.getFlag().equals(flag))
            return Cancel;
        else if (Change.getFlag().equals(flag))
            return Change;
        else
            throw new ValidationError(ACCCLOSE_ERROR, String.format("Неверный флаг закрытия счета '%s': '%s'", bsaAcid, flag));
    }

    private String createOutMessage(Map<String, String> xmlData) throws Exception {
        StringBuilder result = new StringBuilder();
/*
        ("<?xml version=\"1.0\" encoding=\"").append(charsetName).append("\"?>\n")
        .append("<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/")
        .append("\" xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/")
        .append("\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance")
        .append("\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n");
*/
/*
        <NS3:CBAccountNo>GL_ACC.BSAACID</NS3:CBAccountNo>
        <NS3:OpenClose>Close</NS3: OpenClose > (может содержать значения: Open, Close или Error)
        <NS3:ErrorMsg></NS3:ErrorMsg> (по умолчанию тег ErrorMsg отсутствует, в случае OpenClose == Error, в теге ErrorMsg описание ошибки )
        <NS3:OpenDate>GL_ACC.DTO(в формате даты)</NS3:OpenDate>
        <NS3:CloseDate>GL_ACC.DTC(в формате даты)</NS3:CloseDate>
        <NS3:DealId>GL_ACC.DEALID</NS3:DealId>
*/
        result = appendXmlParameter(result, "CBAccountNo", xmlData.get("BSAACID"), true);

        String errorMsg = xmlData.get("ERROR");
        String dateClose = xmlData.get("DTC");
        String openClose = !isEmpty(errorMsg) ? "Error" : isEmpty(dateClose) ? "Open" : "Close";

        result = appendXmlParameter(result, "OpenClose", openClose, true);
        result = appendXmlParameter(result, "ErrorMsg", errorMsg, false);
        result = appendXmlParameter(result, "OpenDate", xmlData.get("DTO"), false);
        result = appendXmlParameter(result, "CloseDate", dateClose, false);
        result = appendXmlParameter(result, "DealId", xmlData.get("DEALID"), true);

        return result.toString();
    }

    private StringBuilder appendXmlParameter(StringBuilder sb, String tag, String value, boolean force) {
        if (!isEmpty(value) || force)
            return sb.append("<NS3:").append(tag).append(">").append(value).append("</NS3:").append(tag).append(">\r\n");
        else
            return sb;
    }

    private String formatDate(Date date) {
        return dateUtils.dbDateString(date);
    }

    public String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable,
                ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class, DefaultApplicationException.class);
    }


}
