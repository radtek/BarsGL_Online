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
import ru.rbt.barsgl.ejb.entity.acc.Acc;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.repository.GLAccountRepository;
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
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static java.math.BigDecimal.ZERO;
import static org.apache.poi.ss.util.CellReference.NameType.ROW;
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Status.ERROR;
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Status.PROCESSED;
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Status.RAW;
import static ru.rbt.barsgl.ejbtest.AccountQueryMPIT.*;

/**
 * Created by er18837 on 16.03.2018.
 */
public class AccDealCloseProcessorIT extends AbstractQueueIT {

    private final static String qType = "KTP_CLOSE";
    private static final String parentNode = "Body/SGLAccountTBOCloseRequest";
    private static final String accNode = "AccNum";
    private static final String flagNode = "IsErrAcc";
    private static final String dealNode = "DealID";

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
    private static final boolean remoteQueueOut = true;

    private DocumentBuilder docBuilder;
    private XPath xPath;

    private enum OpenClose {toOpen, toClose};
    @Before
    public void before() throws ParserConfigurationException {
        docBuilder = XmlUtilityLocator.getInstance().newDocumentBuilder();
        xPath = XmlUtilityLocator.getInstance().newXPath();
    }

    private String getQProperty(String topic, String ahost, String abroker, String alogin, String apassw) {
        return getQueueProperty (topic, ktpIn, ktpOut, ahost, "1414", abroker, channel, alogin, apassw, "10", writeOut, remoteQueueOut);
    }

    /**
     * Тест обработки сообщения при отсутствии счета
     * @throws Exception
     */
    @Test
    public void testProcessErrorNoAccount() throws Exception {

        long idAudit = getAuditMaxId();
        long idAcdeno = getAcdenoMaxId();

        String message = createRequestXml("AccountCloseRequest.xml", "12345678901234567890", "123456", GLAccount.CloseType.Cancel);

        Long jId = remoteAccess.invoke(AccDealCloseQueueController.class, "createJournalEntry", qType, message);
        ProcessResult processResult = remoteAccess.invoke(AccDealCloseProcessor.class, "process", message, jId);
        Assert.assertTrue(processResult.isError());
        System.out.println("" + processResult.getOutMessage());

        AcDNJournal journal = getAcdenoNewRecord(idAcdeno);
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
        long idAcdeno = getAcdenoMaxId();

        GLAccount account = findGlAccountWithDeal("421%", getOperday().getCurrentDate());
        String message = createRequestXml("AccountCloseRequest.xml", account, GLAccount.CloseType.Cancel);

        Date closeWas = updateDateClose(account, getOperday().getCurrentDate());
        Long jId = remoteAccess.invoke(AccDealCloseQueueController.class, "createJournalEntry", qType, message);
        ProcessResult processResult = remoteAccess.invoke(AccDealCloseProcessor.class, "process", message, jId);
        updateDateClose(account, closeWas);

        System.out.println(processResult.getOutMessage());
        Assert.assertTrue(processResult.isError());

        AcDNJournal journal = getAcdenoNewRecord(idAcdeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(ERROR, journal.getStatus());

        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));
    }

    /**
     * Тест обработки сообщения по отмене сделки
     * @throws Exception
     */
    @Test
    public void testProcessDealCancelWait() throws Exception {

        long idAudit = getAuditMaxId();
        long idAcdeno = getAcdenoMaxId();
        Date curDate = getOperday().getCurrentDate();

//        String message = getRecourceText("AccountCloseRequest_bal.xml");
        GLAccount account = findGlAccountWithDeal("421__810%", getOperday().getCurrentDate());
        String message = createRequestXml("AccountCloseRequest.xml", account, GLAccount.CloseType.Change);

        BigDecimal bal = getBalance(account, curDate);
        if (isZero(bal))
            insertIntoGlBaltur(account, curDate, 1000, 0);
        Date closeWas = updateDateClose(account, null);
        deleteFromWaitClose(account.getBsaAcid());

        Long jId = remoteAccess.invoke(AccDealCloseQueueController.class, "createJournalEntry", qType, message);
        ProcessResult processResult = remoteAccess.invoke(AccDealCloseProcessor.class, "process", message, jId);
//        MQQueueConnectionFactory cf = getConnectionFactory(host, broker, channel);
//        sendToQueue(cf, ktpIn, message.getBytes(), ktpOut, login, passw);
//        Thread.sleep(2000L);
//        executeJobAccDealClose();

        updateDateClose(account, closeWas);
        if (isZero(bal))
            deleteFromGlBaltur(account, curDate);

        Assert.assertNotNull(processResult);
        System.out.println(processResult.getOutMessage());
        Assert.assertFalse(processResult.isError());

        AcDNJournal journal = getAcdenoNewRecord(idAcdeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(RAW, journal.getStatus());

        checkWaitClose(account.getBsaAcid());

        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));
    }

    /**
     * Тест обработки сообщения по отмене сделки
     * @throws Exception
     */
    @Test
    public void testProcessDealChange() throws Exception {

        long idAudit = getAuditMaxId();
        long idAcdeno = getAcdenoMaxId();

        GLAccount account = findGlAccountWithDeal("421%", getOperday().getCurrentDate());
        String message = createRequestXml("AccountCloseRequest.xml", account, GLAccount.CloseType.Change);

        Date closeWas = updateDateClose(account, null);
        Long jId = remoteAccess.invoke(AccDealCloseQueueController.class, "createJournalEntry", qType, message);
        ProcessResult processResult = remoteAccess.invoke(AccDealCloseProcessor.class, "process", message, jId);
        updateDateClose(account, closeWas);

        System.out.println(processResult.getOutMessage());
        Assert.assertTrue(!processResult.isError());


        AcDNJournal journal = getAcdenoNewRecord(idAcdeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(RAW, journal.getStatus());

        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));
    }

    /**
     * Тест обработки сообщения по отмене сделки
     * @throws Exception
     */
    @Test
    public void testProcessDealCancel() throws Exception {

        long idAudit = getAuditMaxId();
        long idAcdeno = getAcdenoMaxId();

        GLAccount account = findGlAccountWithDeal("421%", getOperday().getCurrentDate());
        String message = createRequestXml("AccountCloseRequest.xml", account, GLAccount.CloseType.Cancel);

        Date closeWas = updateDateClose(account, null);
        Long jId = remoteAccess.invoke(AccDealCloseQueueController.class, "createJournalEntry", qType, message);
        ProcessResult processResult = remoteAccess.invoke(AccDealCloseProcessor.class, "process", message, jId);
        updateDateClose(account, closeWas);

        System.out.println(processResult.getOutMessage());
        Assert.assertTrue(!processResult.isError());

        AcDNJournal journal = getAcdenoNewRecord(idAcdeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(RAW, journal.getStatus());

        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));
    }

    // TODO не закончен
    @Test
    public void testProcessDealCanceled() throws Exception {
        long idAudit = getAuditMaxId();
        long idAcdeno = getAcdenoMaxId();

        List<GLAccount> accounts = findGlAccountsWithDeal();
        Assert.assertFalse(accounts.isEmpty());

        String message = createRequestXml("AccountCloseRequest.xml", accounts.get(0), GLAccount.CloseType.Cancel);

        Long jId = remoteAccess.invoke(AccDealCloseQueueController.class, "createJournalEntry", qType, message);
        String outmsg = remoteAccess.invoke(AccDealCloseProcessor.class, "process", message, jId);
        System.out.println("" + outmsg);

        AcDNJournal journal = getAcdenoNewRecord(idAcdeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(RAW, journal.getStatus());

        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));

    }

    /**
     * Тест подключения
     * @throws Exception
     */
    @Test
    public void testConnectToQueue() throws Exception {
        long idAudit = getAuditMaxId();
        remoteAccess.invoke(AccDealCloseQueueController.class, "closeConnection");

        executeJobAccDealClose();

        Thread.sleep(2000L);
        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));
    }

    @Test
    public void testReceiveFromQueue() throws Exception {
        long idAudit = getAuditMaxId();
        long idAcdeno = getAcdenoMaxId();

        remoteAccess.invoke(AccDealCloseQueueController.class, "closeConnection");

        MQQueueConnectionFactory cf = getConnectionFactory(host, broker, channel);
        sendToQueue(cf, ktpIn, new File(this.getClass().getResource("/AccountCloseRequest.xml").getFile()), ktpOut, login, passw);
        Thread.sleep(2000L);

        executeJobAccDealClose();

        Thread.sleep(2000L);
        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));
        AcDNJournal journal = getAcdenoNewRecord(idAcdeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(ERROR, journal.getStatus());
        System.out.println(journal.getComment());

    }

    /**
     * Тест подключения
     * @throws Exception
     */
    @Test
    public void testReceiveSend() throws Exception {
        MQQueueConnectionFactory cf = getConnectionFactory(host, broker, channel);

        sendToQueue(cf, ktpIn, new File(this.getClass().getResource("/AccountCloseRequest.xml").getFile()), ktpOut, login, passw);
        testProcessFromQueue();

    }

    @Test
    public void testProcessFromQueue() throws Exception {
        MQQueueConnectionFactory cf = getConnectionFactory(host, broker, channel);

        String[] requestArr = receiveFromQueue(cf, ktpIn, login, passw);
        String request = requestArr[0];
        System.out.println("messageId:    " + requestArr[1]);
        System.out.println("replyToQueue: " + requestArr[2]);
        System.out.println("request: ");
        System.out.println(request);
        Assert.assertFalse(StringUtils.isEmpty(request));
        Assert.assertNotNull(requestArr[1]);
        Assert.assertNotNull(requestArr[2]);

        Long jId = remoteAccess.invoke(AccDealCloseQueueController.class, "createJournalEntry", qType, request);
        String responce = remoteAccess.invoke(AccDealCloseProcessor.class, "process", request, jId);
        System.out.println("response:");
        System.out.println(responce);
        Assert.assertFalse(StringUtils.isEmpty(responce));

        answerToQueue(cf, ktpOut, responce.getBytes(), requestArr[1], login, passw);
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

    private GLAccount findGlAccountWithDeal(final String like, Date operdate) throws SQLException {
        GLAccount account = (GLAccount) baseEntityRepository.selectFirst(GLAccount.class,
                "from GLAccount a where a.bsaAcid like ?1 and not a.dealId is null and a.dateOpen <= ?2"
                , like, operdate);
        Assert.assertNotNull(account);
        return account;
    }

    private List<GLAccount> findGlAccountsWithDeal() throws SQLException {
        List<DataRecord> res = baseEntityRepository.select(
                "  select  id from gl_acc a where (dealid, custno) in\n" +
                "  (select dealid, custno from \n" +
                "    (select dealid, custno from gl_acc where dealsrs = 'K+TP' and dealid is not null and dealid <> '0' and dtc is null\n" +
                "      group by dealid, custno having count(1) > 2)\n" +
                "    where rownum = 1 )\n" +
                "  and dealsrs = 'K+TP'\n");
        Assert.assertNotNull(res);
        Assert.assertFalse(res.isEmpty());

        String in = StringUtils.listToString(res.stream().map(r->r.getInteger(0)).collect(Collectors.toList()), ",");
        List<GLAccount> accounts = baseEntityRepository.select(GLAccount.class,
                "from GLAccount a where a.id in (" + in + ") order by a.id");
        Assert.assertNotNull(accounts);
        return accounts;
    }

    private Date updateDateClose(GLAccount account, Date dateClose) {
        baseEntityRepository.executeNativeUpdate("update GL_ACC set DTC = ? where BSAACID = ?", dateClose, account.getBsaAcid());
        baseEntityRepository.executeNativeUpdate("update ACCRLN set DRLNC = ? where BSAACID = ? and ACID = ?", dateClose, account.getBsaAcid(), account.getAcid());
        baseEntityRepository.executeNativeUpdate("update BSAACC set BSAACC = ? where ID = ?", dateClose, account.getBsaAcid());
        return account.getDateClose();
    }

    private BigDecimal getBalance(GLAccount account, Date dat) {
        return remoteAccess.invoke(GLAccountRepository.class, "getAccountBalance", account.getBsaAcid(), account.getAcid(), dat);
    }

    private boolean isZero(BigDecimal bal) {
        return ZERO.equals(bal);
    }

    private void deleteFromWaitClose(String bsaAcid) {
        baseEntityRepository.executeNativeUpdate("delete from gl_acwaitclose where bsaacid = ?", bsaAcid);
    }

    private void checkWaitClose(String bsaAcid) throws SQLException {
        DataRecord res = baseEntityRepository.selectFirst("select * from gl_acwaitclose where bsaacid = ?", bsaAcid);
        Assert.assertNotNull(res);
    }

    private void insertIntoGlBaltur(GLAccount account, Date dat, long dt, long ct) {
        baseEntityRepository.executeNativeUpdate("insert into GL_BALTUR (DAT, BSAACID, ACID, DTAC, DTBC, CTAC, CTBC, MOVED) values (?,?,?,?,?,?,?,?)",
                dat, account.getBsaAcid(), account.getAcid(), -dt, -dt, ct, ct, "N");
    }

    private void deleteFromGlBaltur(GLAccount account, Date dat) {
        baseEntityRepository.executeNativeUpdate("delete from GL_BALTUR where BSAACID = ?", account.getBsaAcid());
    }

    private void executeJobAccDealClose() throws Exception {
        SingleActionJob job =
                SingleActionJobBuilder.create()
                        .withClass(AccDealCloseNotifyTask.class)
                        .withName("AccDealClose1")
                        .withProps(getQProperty(qType, host, broker, login, passw))
                        .build();
        jobService.executeJob(job);
    }

}
