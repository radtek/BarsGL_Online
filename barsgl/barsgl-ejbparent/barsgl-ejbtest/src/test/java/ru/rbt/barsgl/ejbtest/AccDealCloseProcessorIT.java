package ru.rbt.barsgl.ejbtest;

import com.ibm.mq.jms.MQQueueConnectionFactory;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.task.AccDealCloseNotifyTask;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.*;
import ru.rbt.barsgl.ejb.entity.acc.AcDNJournal;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.props.PropertyName;
import ru.rbt.barsgl.ejb.repository.GLAccountRepository;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.util.StringUtils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static java.math.BigDecimal.ZERO;
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Status.ERROR;
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Status.RAW;
import static ru.rbt.barsgl.ejb.entity.acc.GLAccount.CloseType.Cancel;
import static ru.rbt.barsgl.ejb.entity.acc.GLAccount.CloseType.Change;
import static ru.rbt.barsgl.ejb.entity.acc.GLAccount.CloseType.Normal;
import static ru.rbt.barsgl.ejb.entity.acc.GLAccount.OpenType.ERR;
import static ru.rbt.barsgl.ejbtest.mq.MqUtil.answerToQueue;
import static ru.rbt.barsgl.ejbtest.mq.MqUtil.getConnectionFactory;

/**
 * Created by er18837 on 16.03.2018.
 */
public class AccDealCloseProcessorIT extends AbstractQueueIT {

    class TestParams {
        private String bsaAcid;
        private String dealId;
        private String flag;

        public TestParams(String bsaAcid, String dealId, String flag) {
            this.bsaAcid = bsaAcid;
            this.dealId = dealId;
            this.flag = flag;
        }
    }

    private final static String qType = "KTP_CLOSE";
    private static final String parentNode = "Body/SGLAccountTBOCloseRequest";
    private static final String accNode = "AccNum";
    private static final String flagNode = "IsErrAcc";
    private static final String dealNode = "DealID";

    private static final String host = "vs569";
    private static final String broker = "QM_MBROKER4_T4";
    private static final String channel= "SYSTEM.DEF.SVRCONN";
    private static final String acliquIn = "UCBRU.ADP.BARSGL.ACLIQU.REQUEST";
    private static final String acliquOut = "UCBRU.ADP.BARSGL.ACLIQU.RESPONSE";
    private static final String ktpIn = "UCBRU.P2P.KTP2GL.CLOSEACC.REQUEST";
    private static final String ktpOut = "UCBRU.P2P.KTP2GL.CLOSEACC.RESPONSE";
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

//    private String getQProperty(String topic, String ahost, String abroker, String alogin, String apassw) {
//        return getQueueProperties (topic, ktpIn, ktpOut, ahost, "1414", abroker, channel, alogin, apassw, "10", writeOut, remoteQueueOut);
//    }

    private String getJobProperty(String topic, String ahost, String abroker) {
        return getJobProperty (topic, ktpIn, ktpOut, ahost, "1414", abroker, channel, login, passw, "30", writeOut);
    }

    private QueueProperties getQueueProperties(String topic, String ahost, String abroker) {
        return getQueueProperties(topic, ktpIn, ktpOut, ahost, 1414, abroker, channel, login, passw, 30, writeOut, false);
    }

    public String getResourceText(String resource) throws IOException {
        File inFile = new File(this.getClass().getResource(resource).getFile());
        return FileUtils.readFileToString(inFile, AccountQueryProcessor.charsetName);
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
        QueueProcessResult processResult = remoteAccess.invoke(AccDealCloseProcessor.class, "process", message, jId);
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
        try {
            Long jId = remoteAccess.invoke(AccDealCloseQueueController.class, "createJournalEntry", qType, message);
            QueueProcessResult processResult = remoteAccess.invoke(AccDealCloseProcessor.class, "process", message, jId);
            updateDateClose(account, closeWas);

            System.out.println(processResult.getOutMessage());
            Assert.assertTrue(processResult.isError());

            AcDNJournal journal = getAcdenoNewRecord(idAcdeno);
            Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
            Assert.assertEquals(ERROR, journal.getStatus());

            Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));
        } finally {
            updateDateClose(account, closeWas);
        }
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
        String message = createRequestXml("AccountCloseRequest.xml", account, Change);

        Date closeWas = updateDateClose(account, null);
        try {
            Long jId = remoteAccess.invoke(AccDealCloseQueueController.class, "createJournalEntry", qType, message);
            QueueProcessResult processResult = remoteAccess.invoke(AccDealCloseProcessor.class, "process", message, jId);
            updateDateClose(account, closeWas);

            System.out.println(processResult.getOutMessage());
            Assert.assertTrue(!processResult.isError());


            AcDNJournal journal = getAcdenoNewRecord(idAcdeno);
            Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
            Assert.assertEquals(RAW, journal.getStatus());

            Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));
        } finally {
            updateDateClose(account, closeWas);
        }
    }

    /**
     * Тест обработки сообщения по отмене сделки (один счет)
     * @throws Exception
     */
    @Test
    public void testProcessDealCancel() throws Exception {

        long idAudit = getAuditMaxId();
        long idAcdeno = getAcdenoMaxId();

        GLAccount account = findGlAccountWithDeal("421%", getOperday().getCurrentDate());
        String message = createRequestXml("AccountCloseRequest.xml", account, GLAccount.CloseType.Cancel);

        Date closeWas = updateDateClose(account, null);
        try {
            Long jId = remoteAccess.invoke(AccDealCloseQueueController.class, "createJournalEntry", qType, message);
            QueueProcessResult processResult = remoteAccess.invoke(AccDealCloseProcessor.class, "process", message, jId);

            System.out.println(processResult.getOutMessage());
            Assert.assertTrue(!processResult.isError());

            AcDNJournal journal = getAcdenoNewRecord(idAcdeno);
            Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
            Assert.assertEquals(RAW, journal.getStatus());

            Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));
        } finally {
            updateDateClose(account, closeWas);
        }
    }

    /**
     * Тест обработки сообщения по отмене сделки (несколько счетов счет)
     * @throws Exception
     */
    @Test
    public void testProcessDealCanceled() throws Exception {
        long idAudit = getAuditMaxId();
        long idAcdeno = getAcdenoMaxId();
        Date curDate = getOperday().getCurrentDate();

        List<GLAccount> accounts = findGlAccountsWithDeal(false);
        Assert.assertFalse(accounts.isEmpty());
        GLAccount mainAccount = accounts.get(0);

        String message = createRequestXml("AccountCloseRequest.xml", mainAccount, GLAccount.CloseType.Cancel);

        updateDateClose(mainAccount, null);
        boolean changeBal = balanceNonZero(mainAccount, curDate);
        try {
            Long jId = remoteAccess.invoke(AccDealCloseQueueController.class, "createJournalEntry", qType, message);
            QueueProcessResult processResult = remoteAccess.invoke(AccDealCloseProcessor.class, "process", message, jId);
            System.out.println(processResult.getOutMessage());
            Assert.assertTrue(!processResult.isError());

            AcDNJournal journal = getAcdenoNewRecord(idAcdeno);
            Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
            Assert.assertEquals(RAW, journal.getStatus());

            checkWaitClose(mainAccount);
            for (int i = 1; i < accounts.size(); i++) {
                GLAccount account = (GLAccount) baseEntityRepository.refresh(accounts.get(i), true);
                Assert.assertEquals(curDate, account.getDateClose());
            }

            Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));
        } finally {
            for (GLAccount account : accounts)
                updateDateClose(account, account.getDateClose());
            if (changeBal)
                deleteFromGlBaltur(mainAccount, curDate);
        }
    }

    /**
     * Тест обработки сообщения по отмене сделки (несколько счетов счет)
     * @throws Exception
     */
    @Test
    public void testProcessDealChanged() throws Exception {
        long idAudit = getAuditMaxId();
        long idAcdeno = getAcdenoMaxId();
        Date curDate = getOperday().getCurrentDate();

        List<GLAccount> accounts = findGlAccountsWithDeal(true);
        Assert.assertFalse(accounts.isEmpty());
        GLAccount mainAccount = accounts.get(0);

        String message = createRequestXml("AccountCloseRequest.xml", mainAccount, Change);

        updateDateClose(mainAccount, null);
        boolean changeBal = balanceToZero(mainAccount, curDate);
        try {
            Long jId = remoteAccess.invoke(AccDealCloseQueueController.class, "createJournalEntry", qType, message);
            QueueProcessResult processResult = remoteAccess.invoke(AccDealCloseProcessor.class, "process", message, jId);
            System.out.println(processResult.getOutMessage());
            Assert.assertTrue(!processResult.isError());

            AcDNJournal journal = getAcdenoNewRecord(idAcdeno);
            Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
            Assert.assertEquals(RAW, journal.getStatus());

            GLAccount account = (GLAccount) baseEntityRepository.refresh(mainAccount, true);
            Assert.assertEquals(curDate, account.getDateClose());
            for (int i = 1; i < accounts.size(); i++) {
                account = (GLAccount) baseEntityRepository.refresh(accounts.get(i), true);
                Assert.assertNull(account.getDateClose());
            }

            Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));
        } finally {
            for (GLAccount account : accounts)
                updateDateClose(account, account.getDateClose());
            if (changeBal)
                deleteFromGlBaltur(mainAccount, curDate);
        }
    }

    /**
     * Тест постановки счета в очередь на закрытие
     * @throws Exception
     */
    @Test
    public void testAccountDealWait() throws Exception {
        processDealCancelWait("421__810%", Change);
    }

    /**
     * Тест закрытия счетов из очереди на закрытие
     * @throws Exception
     */
    @Test
    public void testProcessAccountsWait() throws Exception {
        Operday operday = getOperday();
        Date curDate = operday.getCurrentDate();

        // поставить счета в очередь на закрытие
        GLAccount[] accounts = new GLAccount[3];
        accounts[0] = processDealCancelWait("4____810%1", Change);
        accounts[1] = processDealCancelWait("4____810%2", Cancel);
        accounts[2] = processDealCancelWait("4____810%3", Cancel);
        boolean[] chgBal = new boolean[3];

        try {
            for (int i = 0; i < accounts.length; i++) {
                // счет должен быть открыт, дата регистр = текущая дата, дата открытия != текущая дата
                GLAccount account = accounts[i];
                updateDateClose(account, null);
                if (!account.getDateRegister().equals(curDate))
                    baseEntityRepository.executeNativeUpdate("update GL_ACC set DTR = ? where ID = ?", curDate, account.getId());
                if (account.getDateOpen().equals(curDate))
                    baseEntityRepository.executeNativeUpdate("update GL_ACC set DTO = ? where ID = ?", DateUtils.addDay(curDate, -1), account.getId());
                // обнулить баланс
                chgBal[i] = balanceToZero(account, curDate);
            }

            // эмулируем 3-й счет закрытием сделки
            baseEntityRepository.executeNativeUpdate("update GL_ACWAITCLOSE set OPENTYPE = 'AENEW', IS_ERRACC = 0 where GLACID = ?", accounts[2].getId());
            baseEntityRepository.executeNativeUpdate("update GL_ACC set OPENTYPE = 'AENEW' where ID = ?", accounts[2].getId());

            // закрытие по очереди
            int cnt = remoteAccess.invoke(AccDealCloseProcessor.class, "processAccWaitClose", operday);
            Assert.assertTrue(cnt >= 3);

            for (int i = 0; i < accounts.length; i++) {
                GLAccount account = (GLAccount) baseEntityRepository.refresh(accounts[i], true);
                Date dateClose = ERR.name().equals(account.getOpenType()) ? account.getDateOpen() : curDate;
                Assert.assertEquals(dateClose, account.getDateClose());
            }
        } finally {
            for (int i = 0; i < accounts.length; i++) {
                updateDateClose(accounts[i], accounts[i].getDateClose());
                if (chgBal[i])
                    deleteFromGlBaltur(accounts[i], curDate);
            }
        }
    }

    /**
     * Тест исключения счетов из очереди на закрытие
     * @throws Exception
     */
    @Test
    public void testExcludeAccountsWait() throws Exception {
        Operday operday = getOperday();
        Date curDate = operday.getCurrentDate();

        GLAccount[] accounts = new GLAccount[2];
        // поставить счета в очередь на закрытие
        accounts[0] = processDealCancelWait("4____810%4", Change, true);
        accounts[1] = processDealCancelWait("4____810%5", Cancel, true);

        try {
            for (int i = 0; i < accounts.length; i++) {
                // счет должен быть открыт
                GLAccount account = accounts[i];
                updateDateClose(account, null);
                // не нулевой обнулить баланс
            }

            Long maxDays = remoteAccess.invoke(PropertiesRepository.class, "getNumberDef", PropertyName.ACC_WAIT_CLOSE.getName(), 30L);
            baseEntityRepository.executeNativeUpdate("update GL_ACWAITCLOSE set OPERDAY = ? - ? where GLACID = ?", curDate, maxDays, accounts[0].getId());
            baseEntityRepository.executeNativeUpdate("update GL_ACWAITCLOSE set OPERDAY = ? - ? - 1 where GLACID = ?", curDate, maxDays, accounts[1].getId());

            // обработка закрытия - не должна пройти
            int cntClose = remoteAccess.invoke(AccDealCloseProcessor.class, "processAccWaitClose", operday);
            // исключить только 2-й счет
            int cntExclude = remoteAccess.invoke(AccDealCloseProcessor.class, "excludeAccWaitClose", operday);
            Assert.assertTrue(cntExclude == 1);

            for (int i = 0; i < accounts.length; i++) {
                GLAccount account = (GLAccount) baseEntityRepository.refresh(accounts[i], true);
                Assert.assertNull(account.getDateClose());
            }
            DataRecord wait0 = baseEntityRepository.selectFirst("select EXCLDATE from GL_ACWAITCLOSE where GLACID = ?", accounts[0].getId());
            Assert.assertNull(wait0.getDate(0));
            DataRecord wait1 = baseEntityRepository.selectFirst("select EXCLDATE from GL_ACWAITCLOSE where GLACID = ?", accounts[1].getId());
            Assert.assertEquals(curDate, wait1.getDate(0));
        } finally {
            for (int i = 0; i < accounts.length; i++) {
                updateDateClose(accounts[i], accounts[i].getDateClose());
            }
        }
    }

    public GLAccount processDealCancelWait(String mask, GLAccount.CloseType closeType) throws Exception {
        return processDealCancelWait(mask, closeType, false);
    }

    public GLAccount processDealCancelWait(String mask, GLAccount.CloseType closeType, boolean withBal) throws Exception {

        long idAudit = getAuditMaxId();
        long idAcdeno = getAcdenoMaxId();
        Date curDate = getOperday().getCurrentDate();

//        String message = getRecourceText("AccountCloseRequest_bal.xml");
        GLAccount account = withBal
                ? findGlAccountWithDealBal(mask, getOperday().getCurrentDate())
                : findGlAccountWithDeal(mask, getOperday().getCurrentDate());

        String message = createRequestXml("AccountCloseRequest.xml", account, closeType);

        boolean changeBal = balanceNonZero(account, curDate);
        Date closeWas = updateDateClose(account, null);
        try {
            deleteFromWaitClose(account);

            Long jId = remoteAccess.invoke(AccDealCloseQueueController.class, "createJournalEntry", qType, message);
            QueueProcessResult processResult = remoteAccess.invoke(AccDealCloseProcessor.class, "process", message, jId);
//        MQQueueConnectionFactory cf = getConnectionFactory(host, broker, channel);
//        sendToQueue(cf, ktpIn, message.getBytes(), ktpOut, login, passw);
//        Thread.sleep(2000L);
//        executeJobAccDealClose();

            Assert.assertNotNull(processResult);
            System.out.println(processResult.getOutMessage());
            Assert.assertFalse(processResult.isError());

            AcDNJournal journal = getAcdenoNewRecord(idAcdeno);
            Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
            Assert.assertEquals(RAW, journal.getStatus());

            checkWaitClose(account);

            Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));
        } finally {
            updateDateClose(account, closeWas);
            if (changeBal)
                deleteFromGlBaltur(account, curDate);
        }
        return account;
    }

    /**
     * Тест подключения
     * @throws Exception
     */
    @Test
    public void testConnectToQueue() throws Exception {
        long idAudit = getAuditMaxId();
        remoteAccess.invoke(QueueCommunicator.class, "closeConnection");

        executeJobAccDealClose();

        Thread.sleep(2000L);
        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));
    }

    @Test
    public void testSendToQeue() throws Exception {
        TestParams[] testParams = {
                new TestParams("42102810020010008276", "A01DEP1180570017", "1"),
                new TestParams("42102810020010008467", "A01DEP1180710011", "2"),
                new TestParams("42102810720930000067", "O01DEP1173630001", "2")
        };

//        MQQueueConnectionFactory cf = getConnectionFactory(host, broker, channel);
        QueueProperties properties = getQueueProperties(qType, host, broker );

        startConnection(properties);

        for (TestParams params : testParams) {
            String message = createRequestXml("AccountCloseRequest.xml", params.bsaAcid, params.dealId, params.flag);
//            sendToQueue(cf, ktpIn, message.getBytes(), ktpOut, login, passw);
            sendToQueue(message, properties, null, null, ktpIn);
        }
    }

    @Test
    public void testJobAccDealClose() throws Exception {
        long idAudit = getAuditMaxId();
        long idAcdeno = getAcdenoMaxId();

        remoteAccess.invoke(QueueCommunicator.class, "closeConnection");

//        MQQueueConnectionFactory cf = getConnectionFactory(host, broker, channel);
//        sendToQueue(cf, ktpIn, new File(this.getClass().getResource("/AccountCloseRequest.xml").getFile()), ktpOut, login, passw);
        QueueProperties properties = getQueueProperties(qType, host, broker );
        startConnection(properties);
        sendToQueue(getResourceText("/AccountCloseRequest.xml"), properties, null, null, ktpIn);

        Thread.sleep(2000L);

        executeJobAccDealClose();

        Thread.sleep(3000L);
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
    @Ignore
    @Test
    public void testReceiveSend() throws Exception {
//        MQQueueConnectionFactory cf = getConnectionFactory(host, broker, channel);
//        sendToQueue(cf, ktpIn, new File(this.getClass().getResource("/AccountCloseRequest.xml").getFile()), ktpOut, login, passw);

        QueueProperties properties = getQueueProperties(qType, host, broker );
        startConnection(properties);
        sendToQueue(getResourceText("/AccountCloseRequest.xml"), properties, null, null, ktpIn);
        testProcessFromQueue();
    }

    @Ignore
    @Test
    public void testProcessFromQueue() throws Exception {
//        MQQueueConnectionFactory cf = getConnectionFactory(host, broker, channel);

        QueueProperties properties = getQueueProperties(qType, host, broker );
        startConnection(properties);
        QueueInputMessage answer = receiveFromQueue(ktpIn, Charset.defaultCharset());
        String message = answer.getTextMessage();
        System.out.println("messageId:    " + answer.getRequestId());
        System.out.println("replyToQueue: " + answer.getReplyTo());
        System.out.println("request: ");
        System.out.println();
        Assert.assertFalse(StringUtils.isEmpty(message));
        Assert.assertNotNull(answer.getRequestId());
        Assert.assertNotNull(answer.getReplyTo());

        Long jId = remoteAccess.invoke(AccDealCloseQueueController.class, "createJournalEntry", qType, message);
        QueueProcessResult responce = remoteAccess.invoke(AccDealCloseProcessor.class, "process", message, jId);
        System.out.println("response:");
        System.out.println(responce);
        Assert.assertFalse(StringUtils.isEmpty(responce.getOutMessage()));

        sendToQueue(responce.getOutMessage(), properties, answer.getRequestId(), null, ktpIn);
//        answerToQueue(cf, ktpOut, responce.getOutMessage().getBytes(), answer.getRequestId(), login, passw);
    }

    private String createRequestXml(String fileName, GLAccount account, GLAccount.CloseType closeType) throws Exception {
        return createRequestXml(fileName, account.getBsaAcid(), account.getDealId(), closeType) ;
    }

    private String createRequestXml(String fileName, String bsaAcid, String dealId, GLAccount.CloseType closeType) throws Exception {
        return createRequestXml(fileName, bsaAcid, dealId, closeType.getFlag()) ;
    }

    private String createRequestXml(String fileName, String bsaAcid, String dealId, String closeType) throws Exception {
        String message = getRecourceText(fileName);
        Document doc = getDocument(docBuilder, message);
        String cb = getXmlParam(xPath, doc, parentNode, accNode);
        String deal = getXmlParam(xPath, doc, parentNode, dealNode);
        String flag = getXmlParam(xPath, doc, parentNode, flagNode);

        message = changeXmlParam(message, accNode, cb, bsaAcid);
        message = changeXmlParam(message, dealNode, deal, dealId);
        message = changeXmlParam(message, flagNode, flag, closeType);
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

    // select * from GL_ACC a join BALTUR b on a.bsaacid = b.bsaacid and a.acid = b.acid and b.DATTO = '2029-01-01' where a.bsaAcid like '421__810%4' and not a.dealId is null and b.OBBC + b.DTBC + b.CTBC != 0;
    private GLAccount findGlAccountWithDealBal(final String like, Date operdate) throws SQLException {
        DataRecord data = baseEntityRepository.selectFirst("select ID from GL_ACC a join BALTUR b on a.bsaacid = b.bsaacid and a.acid = b.acid and b.DATTO = '2029-01-01'" +
                " where a.bsaAcid like ? and not a.dealId is null and b.OBBC + b.DTBC + b.CTBC != 0", like);
        Assert.assertNotNull(data);
        GLAccount account = (GLAccount) baseEntityRepository.findById(GLAccount.class, data.getLong(0));
        return account;
    }

    private List<GLAccount> findGlAccountsWithDeal(boolean onlyOpen) throws SQLException {
        String opened = onlyOpen ? "and dtc is null" : "";
        List<DataRecord> res = baseEntityRepository.select(
                "  select  id from gl_acc a where (dealid, custno) in\n" +
                "  (select dealid, custno from \n" +
                "    (select dealid, custno from gl_acc where dealsrs = 'K+TP' and dealid is not null and dealid <> '0' " + opened + "\n" +
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

    private Date updateDateClose(GLAccount acc, Date dateClose) {
        GLAccount account = (GLAccount) baseEntityRepository.refresh(acc, true);
        baseEntityRepository.executeNativeUpdate("update GL_ACC set DTC = ? where BSAACID = ?", dateClose, account.getBsaAcid());
        return account.getDateClose();
    }

    private BigDecimal getBalance(GLAccount account, Date dat) {
        return remoteAccess.invoke(GLAccountRepository.class, "getAccountBalance", account.getBsaAcid(), account.getAcid(), dat);
    }

    private boolean isZero(BigDecimal bal) {
        return ZERO.equals(bal);
    }

    private boolean balanceNonZero(GLAccount account, Date curDate) {
        BigDecimal bal = getBalance(account, curDate);
        if (isZero(bal)) {
            insertIntoGlBaltur(account, curDate, 1000, 0);
            return true;
        }
        return false;
    }

    private boolean balanceToZero(GLAccount account, Date curDate) {
        BigDecimal bal = getBalance(account, curDate);
        int sign = bal.compareTo(ZERO);
        if (sign > 0)
            insertIntoGlBaltur(account, curDate, bal.longValue(), 0);
        else if (sign < 0)
            insertIntoGlBaltur(account, curDate, 0, bal.longValue());
        return sign != 0;
    }

    private void deleteFromWaitClose(GLAccount account) {
        baseEntityRepository.executeNativeUpdate("delete from gl_acwaitclose where bsaacid = ?", account.getBsaAcid());
    }

    private void deleteFromWaitCloseH(GLAccount account) {
        baseEntityRepository.executeNativeUpdate("delete from gl_acwaitclose_h where bsaacid = ?", account.getBsaAcid());
    }

    private void checkWaitClose(GLAccount account) throws SQLException {
        DataRecord res = baseEntityRepository.selectFirst("select * from gl_acwaitclose where bsaacid = ?", account.getBsaAcid());
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
                        .withProps(getJobProperty(qType, host, broker))
                        .build();
        jobService.executeJob(job);
    }

}
