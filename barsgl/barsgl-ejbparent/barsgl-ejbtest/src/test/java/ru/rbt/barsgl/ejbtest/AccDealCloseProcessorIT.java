package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.AccDealCloseProcessor;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.AccDealCloseQueueController;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.XmlUtilityLocator;
import ru.rbt.barsgl.ejb.entity.acc.AcDNJournal;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.BaseEntityRepository;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import java.sql.SQLException;
import java.util.Date;

import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Status.PROCESSED;
import static ru.rbt.barsgl.ejbtest.CustomerDetailsNotifyIT.getAuditMaxId;

/**
 * Created by er18837 on 16.03.2018.
 */
public class AccDealCloseProcessorIT extends AbstractTimerJobIT {

    private final static String qType = "KTP_CLOSE";
    private static final String parentNode = "Body/SGLAccountTBOCloseRequest";
    private static final String accNode = "AccNum";
    private static final String flagNode = "IsErrAcc";
    private static final String dealNode = "DealID";


    private DocumentBuilder docBuilder;
    private XPath xPath;

    @Before
    public void before() throws ParserConfigurationException {
        docBuilder = XmlUtilityLocator.getInstance().newDocumentBuilder();
        xPath = XmlUtilityLocator.getInstance().newXPath();
    }
    /**
     * Тест обработки сообщения из очереди
     * @throws Exception
     */
    @Test
    public void testProcessAccCloseErr() throws Exception {

        long idAudit = getAuditMaxId();
        long idCudeno = getAcdenoMaxId();

        String message = getRecourceText("AccountCloseRequest.xml");
        Document doc = getDocument(docBuilder, message);
        String cb = getXmlParam(xPath, doc, parentNode, accNode);
        String deal = getXmlParam(xPath, doc, parentNode, dealNode);
        String flag = getXmlParam(xPath, doc, parentNode, flagNode);

        GLAccount account = findGlAccountWithDeal(baseEntityRepository, getOperday(), "421%");
        if (null != account.getDateClose()) {
            updateDateClose(account.getBsaAcid(), null);
        }
        message = changeXmlParam(message, accNode, cb, account.getBsaAcid());
        message = changeXmlParam(message, dealNode, deal, account.getDealId());
        message = changeXmlParam(message, flagNode, flag, "1");

//        String message = IOUtils.toString(this.getClass().getResourceAsStream("/AccountCloseRequest.xml"), "UTF-8");

        Long jId = remoteAccess.invoke(AccDealCloseQueueController.class, "createJournalEntry", qType, message);
        remoteAccess.invoke(AccDealCloseProcessor.class, "process", message, jId);

        AcDNJournal journal = getAcdenoNewRecord(idCudeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(PROCESSED, journal.getStatus());

        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));

    }

    public static long getAcdenoMaxId() throws SQLException {
        DataRecord res = baseEntityRepository.selectFirst("select max(MESSAGE_ID) from GL_ACDENO");
        return null == res ? 0 : res.getLong(0);
    }

    private AuditRecord getAuditError(long idFrom ) throws SQLException {
        return (AuditRecord) baseEntityRepository.selectFirst(AuditRecord.class,
                "from AuditRecord a where a.logCode in ('AccDealCloseNotify', 'QueueProcessor') and a.logLevel <> 'Info' and a.id > ?1 ", idFrom);
    }

    private AcDNJournal getAcdenoNewRecord(long idFrom) throws SQLException {
        return (AcDNJournal) baseEntityRepository.selectFirst(AcDNJournal.class, "from AcDNJournal j where j.id > ?1", idFrom);
    }

    private GLAccount findGlAccountWithDeal(BaseEntityRepository baseEntityRepository, Operday operday, final String like) throws SQLException {
        return (GLAccount) baseEntityRepository.selectFirst(GLAccount.class,
                "from GLAccount a where a.bsaAcid like ?1 and not a.dealId is null and a.dateOpen <= ?2"
                , like, operday.getCurrentDate());
    }

    private void updateDateClose(String bsaacid, Date dateClose) {
        baseEntityRepository.executeNativeUpdate("update GL_ACC set DTC = ? where BSAACID = ?", dateClose, bsaacid);
        baseEntityRepository.executeNativeUpdate("update ACCRLN set DRLNC = ? where BSAACID = ?", dateClose, bsaacid);
        baseEntityRepository.executeNativeUpdate("update BSAACC set BSAACC = ? where ID = ?", dateClose, bsaacid);
    }
}
