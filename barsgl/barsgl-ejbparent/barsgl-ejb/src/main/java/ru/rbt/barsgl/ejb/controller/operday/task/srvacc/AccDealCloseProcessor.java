package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import org.apache.log4j.Logger;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.CommonQueueController.QueueProcessResult;
import ru.rbt.barsgl.ejb.entity.acc.AcDNJournal;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountController;
import ru.rbt.barsgl.ejb.repository.AcDNJournalRepository;
import ru.rbt.barsgl.ejb.repository.CloseAccountsRepository;
import ru.rbt.barsgl.ejb.repository.GLAccountRepository;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.util.StringUtils;
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
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Status.ERROR;
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

    public static class AccWaitParams {
        Long glacid;
        String isErrAcc;

        public AccWaitParams(Long glacid, String isErrAcc) {
            this.glacid = glacid;
            this.isErrAcc = isErrAcc;
        }
    }

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
    public QueueProcessResult process(String fullTopic, final Long journalId) throws Exception {
        // разбор сообщения и обработка сетов
        Map<String, String> xmlData = processMessage(fullTopic, journalId);

        // ответное сообщеине
        String answerBody = createOutMessage(xmlData);

        if (isEmpty(xmlData.get("ERROR"))) {
            journalRepository.updateComment(journalId, String.format("Счет с bsaacid = '%s' %s",
                    xmlData.get("BSAACID"), (!isEmpty(xmlData.get("DTC")) ? "закрыт" : "поставлен в очередь на закрытие")));
            return new QueueProcessResult(answerBody, false);
        } else
            return new QueueProcessResult(answerBody, true);

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

            dateClose = mainAccount.getDateRegister().equals(curDate) ? mainAccount.getDateOpen() : curDate;
            dateClose = processOneAccount(mainAccount, curDate, dateClose, errorFlag);
            if (Cancel == errorFlag) {
                List<GLAccount> accounts = closeAccountsRepository.getDealCustAccounts(mainAccount);
                for (GLAccount account: accounts) {
                    processOneAccount(account, curDate, curDate, Normal);
                }
            }
        } catch (ValidationError ex) {
            String msg = ValidationError.getErrorText(ex.getMessage());
            updateLogStatus(journalId, ERROR, msg);
            auditController.warning(AccDealCloseTask, "Ошибка закрытия счета по нотификации от K+TP", journalName, journalId.toString(), ex);
            xmlData.put("ERROR", msg);
            return xmlData;
        } catch (Exception ex) {
            String msg = StringUtils.substr(getErrorMessage(ex), 255);
            updateLogStatus(journalId, ERROR, getErrorMessage(ex));
            auditController.error(AccDealCloseTask, "Ошибка закрытия счета по нотификации от K+TP", journalName, journalId.toString(), ex);
            xmlData.put("ERROR", msg);
            return xmlData;
        }
        xmlData.put("DTO", dateUtils.dbDateString(mainAccount.getDateOpen()));
        if (null != dateClose)
            xmlData.put("DTC", dateUtils.dbDateString(dateClose));

        return xmlData;
    }

    private Date processOneAccount(GLAccount account, Date curDate, Date dateClose, GLAccount.CloseType closeType) throws Exception {
        // проверить остаток
        if (glAccountRepository.isAccountBalanceZero(account.getBsaAcid(), account.getAcid(), curDate)) {
            // нулевой, закрыть счет
            glAccountController.closeGLAccountDeals(account, dateClose, closeType);
            auditController.info(AccDealCloseTask, String.format("Счет с bsaacid = '%s' закрыт с датой '%s' по нотификации от K+TP",
                    account.getBsaAcid(), dateUtils.onlyDateString(dateClose)));
            return dateClose;
        } else { // не нулевой
            // добавить запись в очередь на закрытие
            String openTypeWas = account.getOpenType();
            if (Normal != closeType) {
                glAccountController.updateGLAccountOpenType(account, GLAccount.OpenType.ERR);
            }
            closeAccountsRepository.moveToWaitClose(account, curDate, closeType, openTypeWas); //);
            // обновить OpenType
            return null;
        }
    }

    private GLAccount findAccountByDealWithCheck(String bsaAcid, String dealId) {
        GLAccount account = closeAccountsRepository.getAccountByDeal(bsaAcid, dealId);
        if (null == account) {
            throw new ValidationError(ACCCLOSE_ERROR, String.format("Не найден счет в GL_ACC c BSAACID = '%s' и DEALID = '%s'", bsaAcid, dealId));
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
        final String head = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "\t<SOAP-ENV:Header>\n" +
                "\t</SOAP-ENV:Header>\n" +
                "\t<SOAP-ENV:Body>\n" +
                "\t\t<gbo:SGLAccountTBOCloseResponse xmlns:gbo=\"urn:ucbru:gbo:v4\">\n";
        final String foot =
                "\t\t</gbo:SGLAccountTBOCloseResponse>\n" +
                "\t</SOAP-ENV:Body>\n" +
                "</SOAP-ENV:Envelope>\n";
/*
<?xml version="1.0" encoding="UTF-8"?>
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/" xmlns:SOAP-ENC="http://schemas.xmlsoap.org/soap/encoding/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema">
    <SOAP-ENV:Header>
    </SOAP-ENV:Header>
    <SOAP-ENV:Body>
        <gbo:SGLAccountTBOCloseResponse xmlns:gbo="urn:ucbru:gbo:v4">
            <gbo:CBAccountNo>42105810220130000003</gbo:CBAccountNo>
            <gbo:OpenClose>Open</gbo:OpenClose >
            <gbo:ErrorMsg>Сообщение об ошибке</gbo:ErrorMsg>
            <gbo:OpenDate>2017-12-14</gbo:OpenDate>
            <gbo:CloseDate>2017-12-14</gbo:CloseDate>
            <gbo:DealId>1846398</gbo:DealId>
        </gbo:SGLAccountTBOCloseResponse>
    </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
*/
        StringBuilder result = new StringBuilder();
        result.append(head);

        result = appendXmlParameter(result, "CBAccountNo", xmlData.get("BSAACID"), true);
        String errorMsg = xmlData.get("ERROR");
        String dateClose = xmlData.get("DTC");
        String openClose = !isEmpty(errorMsg) ? "Error" : isEmpty(dateClose) ? "Open" : "Close";

        result = appendXmlParameter(result, "OpenClose", openClose, true);
        result = appendXmlParameter(result, "ErrorMsg", errorMsg, false);
        result = appendXmlParameter(result, "OpenDate", xmlData.get("DTO"), true);
        result = appendXmlParameter(result, "CloseDate", dateClose, true);
        result = appendXmlParameter(result, "DealId", xmlData.get("DEALID"), true);

        result.append(foot);

        return result.toString();
    }

    private StringBuilder appendXmlParameter(StringBuilder sb, String tag, String value, boolean force) {
        if (!isEmpty(value) || force)
            return sb.append("\t\t\t<gbo:").append(tag).append(">").append(value).append("</gbo:").append(tag).append(">\r\n");
        else
            return sb;
    }

    private String formatDate(Date date) {
        return dateUtils.dbDateString(date);
    }

    private void updateLogStatus(Long journalId, AcDNJournal.Status status, String errorMessage) throws Exception {
        journalRepository.executeInNewTransaction(persistence -> {
            journalRepository.updateLogStatus(journalId, ERROR, errorMessage); return null;});
    }

    public int processAccWaitClose(Operday operday) throws Exception {
        Date curDate = operday.getCurrentDate();
        List<AccWaitParams> waitList = closeAccountsRepository.getAccountsWaitClose();
        for (AccWaitParams accWait: waitList) {
            GLAccount account = glAccountRepository.findById(GLAccount.class, accWait.glacid);
            GLAccount.CloseType closeType = GLAccount.CloseType.getByFlag(accWait.isErrAcc);
            Date dateClose = !GLAccount.CloseType.Normal.equals(closeType) && account.getDateRegister().equals(curDate)
                    ? account.getDateOpen()
                    : curDate;

            glAccountController.closeGLAccountDeals(account, dateClose, closeType);
            closeAccountsRepository.moveWaitCloseToHistory(account, dateClose);
            auditController.info(AccDealCloseTask, String.format("Счет с bsaacid = '%s' закрыт с датой '%s' по списку ожидания",
                    account.getBsaAcid(), dateUtils.onlyDateString(dateClose)));
        }
        return waitList.size();
    }

    public String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable,
                ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class, DefaultApplicationException.class);
    }

}
