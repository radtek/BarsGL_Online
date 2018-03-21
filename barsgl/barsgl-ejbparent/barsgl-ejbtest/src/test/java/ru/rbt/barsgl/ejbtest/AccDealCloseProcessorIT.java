package ru.rbt.barsgl.ejbtest;

import com.ibm.mq.jms.MQQueueConnectionFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.task.AccDealCloseNotifyTask;
import ru.rbt.barsgl.ejb.controller.operday.task.CustomerDetailsNotifyTask;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.*;
import ru.rbt.barsgl.ejb.entity.acc.AcDNJournal;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.BaseEntityRepository;
import ru.rbt.ejbcore.util.StringUtils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

import static org.apache.poi.ss.util.CellReference.NameType.ROW;
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Status.ERROR;
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Status.PROCESSED;
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Status.RAW;
import static ru.rbt.barsgl.ejbtest.AccountQueryMPIT.*;
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

    /*"mq.type = queue
mq.host = vs569
mq.port = 1414
mq.queueManager = QM_MBROKER4_T4
mq.channel = SYSTEM.DEF.SVRCONN
mq.batchSize=-1
mq.topics=FCC:UCBRU.ADP.BARSGL.V5.ACDENO.FCC.NOTIF
mq.user=srvwbl4mqtest
mq.password=UsATi8hU
unspents=show"*/

    private final static String host = "vs569";
    private final static String broker = "QM_MBROKER4_T4";
    private final static String channel= "SYSTEM.DEF.SVRCONN";
    private final static String acliquIn = "UCBRU.ADP.BARSGL.ACLIQU.REQUEST";
    private final static String acliquOut = "UCBRU.ADP.BARSGL.ACLIQU.RESPONSE";
    private final static String ktpIn = "UCBRU.P2P.KTP2GL.CLOSEACC.REQUEST";
    private final static String ktpOut = "UCBRU.P2P.KTP2GL.CLOSEACC.RESPONSE";
    private static final String login = "srvwbl4mqtest";
    private static final String passw = "UsATi8hU";
    private static final boolean writeOut = true;

    private DocumentBuilder docBuilder;
    private XPath xPath;

    private enum OpenClose {toOpen, toClose};
    @Before
    public void before() throws ParserConfigurationException {
        docBuilder = XmlUtilityLocator.getInstance().newDocumentBuilder();
        xPath = XmlUtilityLocator.getInstance().newXPath();
    }

    private String getQProperty(String topic, String ahost, String abroker, String alogin, String apassw) {
        return getQueueProperty (topic, acliquIn, acliquOut, ahost, "1414", abroker, channel, alogin, apassw, "10", writeOut);
    }

    /**
     * Тест подключения
     * @throws Exception
     */
    @Test
    public void testConnectToQueue() throws Exception {
        long idAudit = getAuditMaxId();
        remoteAccess.invoke(AccDealCloseQueueController.class, "closeConnection");

        SingleActionJob job =
                SingleActionJobBuilder.create()
                        .withClass(AccDealCloseNotifyTask.class)
                        .withName("AccDealClose1")
                        .withProps(getQProperty(qType, host, broker, login, passw))
                        .build();
        jobService.executeJob(job);

        Thread.sleep(2000L);
        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));
    }

    /**
     * Тест подключения
     * @throws Exception
     */
    @Test
    public void testReceiveSend() throws Exception {
        MQQueueConnectionFactory cf = getConnectionFactory(host, broker, channel);

//        sendToQueue(cf, ktpIn, new File(this.getClass().getResource("/AccountCloseRequest.xml").getFile()), null, login, passw);
        String request = receiveFromQueue(cf, ktpIn, login, passw);
        System.out.println("request: " + request);

//        sendToQueue(cf, ktpOut, new File(this.getClass().getResource("/AccountCloseResponse.xml").getFile()), null, login, passw);
//        String response = receiveFromQueue(cf, ktpOut, login, passw);
//        System.out.println("response: " + response);
    }

    @Test
    public void testProcessFromQueue() throws Exception {
        MQQueueConnectionFactory cf = getConnectionFactory(host, broker, channel);

        String request = receiveFromQueue(cf, ktpIn, login, passw);
        System.out.println("request: " + request);
        Assert.assertFalse(StringUtils.isEmpty(request));

        Long jId = remoteAccess.invoke(AccDealCloseQueueController.class, "createJournalEntry", qType, request);
        String responce = remoteAccess.invoke(AccDealCloseProcessor.class, "process", request, jId);
        System.out.println("response:" + responce);
        Assert.assertFalse(StringUtils.isEmpty(responce));

        sendToQueue(cf, ktpOut, responce.getBytes(), null, login, passw, 1);
    }

    /**
     * Тест обработки сообщения при отсутствии счета
     * @throws Exception
     */
    @Test
    public void testProcessErrorNoAccount() throws Exception {

        long idAudit = getAuditMaxId();
        long idCudeno = getAcdenoMaxId();

        String message = createRequestXml("AccountCloseRequest.xml", "12345678901234567890", "123456", GLAccount.CloseType.Cancel);

        Long jId = remoteAccess.invoke(AccDealCloseQueueController.class, "createJournalEntry", qType, message);
        String outmsg = remoteAccess.invoke(AccDealCloseProcessor.class, "process", message, jId);
        System.out.println("" + outmsg);

        AcDNJournal journal = getAcdenoNewRecord(idCudeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(ERROR, journal.getStatus());
        System.out.println(journal.getComment());

        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));

    }

    /**
     * Тест обработки сообщения по отмене сделки
     * @throws Exception
     */
    @Test
    public void testProcessErrorAccountClosed() throws Exception {

        long idAudit = getAuditMaxId();
        long idCudeno = getAcdenoMaxId();

        GLAccount account = findGlAccountWithDeal("421%", getOperday().getCurrentDate(), OpenClose.toClose);
        String message = createRequestXml("AccountCloseRequest.xml", account, GLAccount.CloseType.Cancel);

        Long jId = remoteAccess.invoke(AccDealCloseQueueController.class, "createJournalEntry", qType, message);
        String outmsg = remoteAccess.invoke(AccDealCloseProcessor.class, "process", message, jId);
        System.out.println("" + outmsg);

        AcDNJournal journal = getAcdenoNewRecord(idCudeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(ERROR, journal.getStatus());

        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));
    }

    /**
     * Тест обработки сообщения по отмене сделки
     * @throws Exception
     */
    @Test
    public void testProcessDealCancel() throws Exception {

        long idAudit = getAuditMaxId();
        long idCudeno = getAcdenoMaxId();

        GLAccount account = findGlAccountWithDeal("421%", getOperday().getCurrentDate(), OpenClose.toOpen);
        String message = createRequestXml("AccountCloseRequest.xml", account, GLAccount.CloseType.Cancel);

        Long jId = remoteAccess.invoke(AccDealCloseQueueController.class, "createJournalEntry", qType, message);
        String outmsg = remoteAccess.invoke(AccDealCloseProcessor.class, "process", message, jId);
        System.out.println("" + outmsg);

        AcDNJournal journal = getAcdenoNewRecord(idCudeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(RAW, journal.getStatus());

        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));
    }

    /**
     * Тест обработки сообщения по отмене сделки
     * @throws Exception
     */
    @Test
    public void testProcessDealChange() throws Exception {

        long idAudit = getAuditMaxId();
        long idCudeno = getAcdenoMaxId();

        GLAccount account = findGlAccountWithDeal("421%", getOperday().getCurrentDate(), OpenClose.toOpen);
        String message = createRequestXml("AccountCloseRequest.xml", account, GLAccount.CloseType.Change);

        Long jId = remoteAccess.invoke(AccDealCloseQueueController.class, "createJournalEntry", qType, message);
        String outmsg = remoteAccess.invoke(AccDealCloseProcessor.class, "process", message, jId);
        System.out.println("" + outmsg);

        AcDNJournal journal = getAcdenoNewRecord(idCudeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(RAW, journal.getStatus());

        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));
    }

    private String createRequestXml(String fileName, GLAccount account, GLAccount.CloseType closeType) throws Exception {
        return createRequestXml(fileName, account.getBsaAcid(), account.getDealId(), closeType) ;
    }

    private String createRequestXml(String fileName, String bsaAcid, String dealId, GLAccount.CloseType closeType) throws Exception {
        String message = getRecourceText(fileName);
        Document doc = getDocument(docBuilder, message);
        String cb = getXmlParam(xPath, doc, parentNode, accNode);
        String deal = getXmlParam(xPath, doc, parentNode, dealNode);
        String flag = getXmlParam(xPath, doc, parentNode, flagNode);

        message = changeXmlParam(message, accNode, cb, bsaAcid);
        message = changeXmlParam(message, dealNode, deal, dealId);
        message = changeXmlParam(message, flagNode, flag, closeType.getFlag());
        return message;
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

    private GLAccount findGlAccountWithDeal(final String like, Date operdate, OpenClose openClose) throws SQLException {
        GLAccount account = (GLAccount) baseEntityRepository.selectFirst(GLAccount.class,
                "from GLAccount a where a.bsaAcid like ?1 and not a.dealId is null and a.dateOpen <= ?2"
                , like, operdate);
        Assert.assertNotNull(account);
        if (openClose == OpenClose.toOpen && null != account.getDateClose()) {
            updateDateClose(account.getBsaAcid(), null);
        }
        else if (openClose == OpenClose.toClose && null == account.getDateClose()) {
            updateDateClose(account.getBsaAcid(), operdate);
        }
        return (GLAccount) baseEntityRepository.refresh(account, true);
    }

    private void updateDateClose(String bsaacid, Date dateClose) {
        baseEntityRepository.executeNativeUpdate("update GL_ACC set DTC = ? where BSAACID = ?", dateClose, bsaacid);
        baseEntityRepository.executeNativeUpdate("update ACCRLN set DRLNC = ? where BSAACID = ?", dateClose, bsaacid);
        baseEntityRepository.executeNativeUpdate("update BSAACC set BSAACC = ? where ID = ?", dateClose, bsaacid);
    }
}
