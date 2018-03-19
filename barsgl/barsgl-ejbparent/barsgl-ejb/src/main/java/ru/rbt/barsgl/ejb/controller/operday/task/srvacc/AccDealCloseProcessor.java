package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import org.apache.log4j.Logger;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.repository.AcDNJournalRepository;
import ru.rbt.barsgl.ejb.repository.GLAccountRepository;
import ru.rbt.ejb.repository.properties.PropertiesRepository;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.Map;

import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Status.ERROR;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;

/**
 * Created by er18837 on 16.03.2018.
 */
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

    public enum CloseErrorFlag {ZERO, ONE, TWE};

    @EJB
    AcDNJournalRepository journalRepository;

    @Inject
    GLAccountRepository glAccountRepository;

    @EJB
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



    }

}
