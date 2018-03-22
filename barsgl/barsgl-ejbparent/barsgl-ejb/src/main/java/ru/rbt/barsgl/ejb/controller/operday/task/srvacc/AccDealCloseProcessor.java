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
import java.util.Date;
import java.util.List;
import java.util.Map;

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

    public void process(String fullTopic, final Long journalId) throws Exception {
        if (fullTopic == null || !fullTopic.contains(parentNodeName)) {
            updateLogStatusError(journalId, "Не найден родительский узел " + parentNodeName);
            return;
        }

        Map<String, String> xmlData = readFromXML(fullTopic, charsetName, "/" + parentNodeName, paramNamesDeal, journalId);
        if (xmlData == null) {
            // Запись в аудит ???
//            updateLogStatusError(journalId, "Ошибка при разборе сообщения XML");      // должен уст Error в процедуре
            return;
        }

        String err = validateXmlParams(xmlData, paramNamesDeal);
        if (!isEmpty(err)) {
            updateLogStatusError(journalId, err);
            return;
        }

        String bsaAcid = xmlData.get("BSAACID");
        String dealId = xmlData.get("DEALID");
        GLAccount.CloseType errorFlag = null;
        GLAccount mainAccount = null;
        boolean mainClosed;
        Date curDate = operdayController.getOperday().getCurrentDate();
        try {
            errorFlag = getCloseFlagWithCheck(bsaAcid, xmlData.get("IS_ERRACC"));
            mainAccount = findAccountByDealWithCheck(bsaAcid, dealId);

            mainClosed = processOneAccount(mainAccount, curDate, errorFlag);
            if (Cancel == errorFlag) {
                List<GLAccount> accounts = closeAccountsRepository.getDealCustAccounts(mainAccount);
                for (GLAccount account: accounts) {
                    processOneAccount(account, curDate, Normal);
                }
            }
        } catch (Throwable ex) {
            journalRepository.updateLogStatus(journalId, ERROR, getErrorMessage(ex));
            auditController.error(AccDealCloseTask, getErrorMessage(ex), journalName, journalId.toString(), ex);
            return;
        }

        // TODO ответное сообщеине
        journalRepository.updateLogStatus(journalId, PROCESSED, String.format("Счет с bsaacid = '%s' %s", bsaAcid,
                (mainClosed ? "закрыт" : "поставлен в очередь на закрытие")) );
    }

    private boolean processOneAccount(GLAccount account, Date curDate, GLAccount.CloseType closeType) throws Exception {
        // проверить остаток
        if (glAccountRepository.isAccountBalanceZero(account.getBsaAcid(), account.getAcid(), curDate)) {
            // нулевой, закрыть счет
            Date closeDate = account.getDateRegister().equals(curDate) ? account.getDateOpen() : curDate;
            glAccountController.closeGLAccountDeals(account, closeDate, closeType);
            auditController.info(AccDealCloseTask, String.format("Счет с bsaacid = '%s' закрыт по нотификации от %s", account.getBsaAcid(), KondorPlus.name()));
            return true;
        } else { // не нулевой
            if (Normal != closeType) {      // обновить OpenType
                glAccountController.updateGLAccountOpenType(account, GLAccount.OpenType.ERR);
            }
            // создать запись в GL_ACWAITCLOSE
//            journalRepository.executeInNewTransaction(persistence ->
            closeAccountsRepository.moveToWaitClose(account, curDate, closeType); //);
            return false;
        }
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

    public String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable,
                ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class, DefaultApplicationException.class);
    }


}
