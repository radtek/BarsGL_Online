package ru.rbt.barsgl.ejbtest;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.controller.operday.task.AccDealCloseNotifyTask;
import ru.rbt.barsgl.ejb.controller.operday.task.AccountDetailsNotifyTask;
import ru.rbt.barsgl.ejb.controller.operday.task.AccountQueryTaskMT;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.*;
import ru.rbt.barsgl.ejb.entity.acc.AcDNJournal;
import ru.rbt.barsgl.ejbcore.mapping.job.IntervalJob;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbcore.mapping.job.TimerJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.barsgl.shared.enums.JobSchedulingType;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.tasks.ejb.job.BackgroundJobsController;

import javax.jms.JMSException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import static junit.framework.TestCase.assertTrue;
import static ru.rbt.barsgl.ejb.common.CommonConstants.ACLIRQ_TASK;
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Status.PROCESSED;
import static ru.rbt.barsgl.ejbcore.mapping.job.TimerJob.JobState.STOPPED;
import static ru.rbt.barsgl.ejbtest.AccountDetailsNotifyProcessorIT.*;
import static ru.rbt.barsgl.ejbtest.AccountQueryMPIT.restartSequenceWithTable;
import static ru.rbt.barsgl.ejbtest.OperdayIT.shutdownJob;
import static ru.rbt.barsgl.shared.enums.JobStartupType.MANUAL;
import static ru.rbt.ejbcore.util.DateUtils.getDatabaseDate;
import static ru.rbt.ejbcore.util.DateUtils.getFinalDateStr;

/**
 * Created by ER22228
 */
public class AccountDetailsNotifyIT extends AbstractQueueIT {

    public static final Logger logger = Logger.getLogger(AccountDetailsNotifyIT.class.getName());

    public static final String ACDENO_TASK = "AccountDetailsNotifyTask";
//    private final static String host = "vs529";
//    private final static String broker = "QM_MBROKER4_T5";

    public static final String host = "vs11205"; //"mbrk4-inta.testhpcsa.imb.ru"; // "vs11205";
    public static final String broker = "QM_MBROKER4";

    private static final String channel = "SYSTEM.DEF.SVRCONN";
    private static final String login = "srvwbl4mqtest";
    private static final String passw = "UsATi8hU";
    private static final String acdeno6 = "UCBRU.ADP.BARSGL.V5.ACDENO.FCC.NOTIF";
    private static final String acdeno12 = "UCBRU.ADP.BARSGL.V5.ACDENO.FCC12.NOTIF";
    private static final Boolean writeOut = false;
    private static final int batchSize = 7;

    private DocumentBuilder docBuilder;
    private XPath xPath;

    @Before
    public void before() throws ParserConfigurationException {
        docBuilder = XmlUtilityLocator.getInstance().newDocumentBuilder();
        xPath = XmlUtilityLocator.getInstance().newXPath();
    }

    @Test
    public void testNotifyCloseFcc12() throws Exception {
        String qType = AcDNJournal.Sources.FC12.name();
        String qName = acdeno12;

        printCommunicatorName();

        QueueProperties properties = getQueueProperties(qType, qName);
        clearQueue(properties, qName, 10);

        startConnection(properties);
        sendToQueue(getResourceText("/AccountDetailsNotifyFcc12Close.xml"), properties, null, null, qName);

        long idJournal = getAcdenoMaxId();
        SingleActionJob job =
                SingleActionJobBuilder.create()
                        .withClass(AccountDetailsNotifyTask.class)
                        .withProps(getJobProperty(qType + ":" + qName, qName))
                        .build();
        jobService.executeJob(job);

        AcDNJournal journal = getJournalNewRecord(idJournal);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(PROCESSED, journal.getStatus());

    }

    @Test
    public void testNotifyOpenFcc6() throws Exception {
        String qType = AcDNJournal.Sources.FCC.name();
        String qName = acdeno6;

        String fileName = "AccountDetailsNotifyFcc6Open.xml";
//        Date openDate = getOpenDate(fileName);

        baseEntityRepository.executeNativeUpdate("delete from gl_acc where bsaacid='40817810000010696538'");

        printCommunicatorName();

        QueueProperties properties = getQueueProperties(qType, qName);
        clearQueue(properties, qName, 10);

        startConnection(properties);
        sendToQueue(getResourceText("/" + fileName), properties, null, null, qName);

        long idJournal = getAcdenoMaxId();
        SingleActionJob job =
                SingleActionJobBuilder.create()
                        .withClass(AccountDetailsNotifyTask.class)
                        .withProps(getJobProperty(qType + ":" + qName, qName))
                        .build();
        jobService.executeJob(job);

        AcDNJournal journal = getJournalNewRecord(idJournal);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(PROCESSED, journal.getStatus());
    }

    @Test
    public void testFCCnoCustomer() throws Exception {
        baseEntityRepository.executeNativeUpdate("delete from gl_acc where bsaacid='40817810250300081806'");

        long idAudit = getAuditMaxId();
        long idJournal = getAcdenoMaxId();

        String qType = AcDNJournal.Sources.FCC.name();
        String textMessage = getResourceText("/AccountDetailsNotifyFcc6NoCust.xml");

        remoteAccess.invoke(AccountDetailsNotifyController.class, "processingWithLog", qType, new QueueInputMessage(textMessage), null, -1, -1);
        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));

        AcDNJournal journal = getJournalNewRecord(idJournal);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(AcDNJournal.Status.PROCESSED, journal.getStatus());

        assertTrue(null != baseEntityRepository.selectFirst("select * from gl_acc where bsaacid=?", "40817810250300081806"));
    }

    @Test
    public void testFCCShadow() throws Exception {

        baseEntityRepository.executeNativeUpdate("delete from gl_acc where bsaacid='40817840250010046747'");

        long idAudit = getAuditMaxId();
        long idJournal = getAcdenoMaxId();

        String qType = AcDNJournal.Sources.FCC.name();
        String textMessage = getResourceText("/AccountDetailsNotifyFcc6Shadow.xml");

        remoteAccess.invoke(AccountDetailsNotifyController.class, "processingWithLog", qType, new QueueInputMessage(textMessage), null, -1, -1);
        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));

        AcDNJournal journal = getJournalNewRecord(idJournal);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(AcDNJournal.Status.ERROR, journal.getStatus());

        assertTrue(null == baseEntityRepository.selectFirst("select * from gl_acc where bsaacid=?", "40817840250010046747"));
    }

    @Test
    public void testFccOpen() throws Exception {
        baseEntityRepository.executeNativeUpdate("delete from gl_acc where bsaacid='40817810000010696538'");

        long idAudit = getAuditMaxId();
        long idJournal = getAcdenoMaxId();

        String qType = AcDNJournal.Sources.FCC.name();
        String textMessage = getResourceText("/AccountDetailsNotifyFcc6Open.xml");

        remoteAccess.invoke(AccountDetailsNotifyController.class, "processingWithLog", qType, new QueueInputMessage(textMessage), null, -1, -1);
        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));

        AcDNJournal journal = getJournalNewRecord(idJournal);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(AcDNJournal.Status.PROCESSED, journal.getStatus());

        assertTrue(null != baseEntityRepository.selectFirst("select * from gl_acc where bsaacid=?", "40817810000010696538"));
    }

    @Test
    public void testFccClose() throws Exception {
        String bsaacid = "40817810050450101811";
        String closeDateStr = "2017-11-22";
        DataRecord rec = baseEntityRepository.selectFirst("SELECT * from gl_acc where bsaacid = ?", bsaacid);
        Date dtc = rec.getDate("DTC");
        if (null == dtc || !getFinalDateStr().equals(dtc)) {
            reopenAccount(bsaacid);
        }

        long idAudit = getAuditMaxId();
        long idJournal = getAcdenoMaxId();

        String qType = AcDNJournal.Sources.FCC.name();
        String textMessage = getResourceText("/AccountDetailsNotifyFcc6Close.xml");

        remoteAccess.invoke(AccountDetailsNotifyController.class, "processingWithLog", qType, new QueueInputMessage(textMessage), null, -1, -1);
        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));

        AcDNJournal journal = getJournalNewRecord(idJournal);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(AcDNJournal.Status.PROCESSED, journal.getStatus());

        checkCloseDate(bsaacid, getDatabaseDate().parse(closeDateStr), false);
    }

    @Test
    public void testFC12Close() throws Exception {
        String bsaacid = "40702810400094449118";
        String closeDateStr = "2016-09-14";
        DataRecord rec = baseEntityRepository.selectFirst("SELECT * from gl_acc where bsaacid = ?", bsaacid);
        Date dtc = rec.getDate("DTC");
        if (null == dtc || !getFinalDateStr().equals(dtc)) {
            reopenAccount(bsaacid);
        }

        long idAudit = getAuditMaxId();
        long idJournal = getAcdenoMaxId();

        String qType = AcDNJournal.Sources.FCC_CLOSE.name();
        String textMessage = getResourceText("/AccountDetailsNotifyFcc12Close.xml");

        remoteAccess.invoke(AccountDetailsNotifyController.class, "processingWithLog", qType, new QueueInputMessage(textMessage), null, -1, -1);
        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));

        AcDNJournal journal = getJournalNewRecord(idJournal);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(AcDNJournal.Status.PROCESSED, journal.getStatus());

        checkCloseDate(bsaacid, getDatabaseDate().parse(closeDateStr), true);
    }

    @Test
    public void testFC12EqualBranch() throws Exception {
        String bsaacid = "40802810900014820908";
        DataRecord rec = baseEntityRepository.selectFirst("SELECT branch from gl_acc where bsaacid=?", bsaacid);
        String oldBranch = rec.getString(0);
        String newBranch = "053";
        baseEntityRepository.executeNativeUpdate("UPDATE GL_ACC SET branch=? WHERE BSAACID=?", newBranch, bsaacid);

        long idAudit = getAuditMaxId();
        long idJournal = getAcdenoMaxId();

        String qType = AcDNJournal.Sources.FC12.name();
        String textMessage = getResourceText("/AccountDetailsNotifyFcc12Branch.xml");

        remoteAccess.invoke(AccountDetailsNotifyController.class, "processingWithLog", qType, new QueueInputMessage(textMessage), null, -1, -1);
        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));

        AcDNJournal journal = getJournalNewRecord(idJournal);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(AcDNJournal.Status.ERROR, journal.getStatus());

        if (null != baseEntityRepository.selectFirst("SELECT branch from gl_acc where bsaacid=? and branch=?", bsaacid, oldBranch))
            assertTrue(true);
        else {
            baseEntityRepository.executeNativeUpdate("UPDATE GL_ACC SET branch=? WHERE BSAACID=?", bsaacid, oldBranch);
            assertTrue(false);
        }
    }

    @Test
    public void testFC12NoEqualBranch() throws Exception {
        String bsaacid = "40802810900014820908";
        DataRecord rec = baseEntityRepository.selectFirst("SELECT branch from gl_acc where bsaacid=?", bsaacid);
        String oldBranch = rec.getString(0);
        String errBranch = "054";
        baseEntityRepository.executeNativeUpdate("UPDATE GL_ACC SET branch=? WHERE BSAACID=?", errBranch, bsaacid);

        long idAudit = getAuditMaxId();
        long idJournal = getAcdenoMaxId();

        String qType = AcDNJournal.Sources.FC12.name();
        String textMessage = getResourceText("/AccountDetailsNotifyFcc12Branch.xml");

        remoteAccess.invoke(AccountDetailsNotifyController.class, "processingWithLog", qType, new QueueInputMessage(textMessage), null, -1, -1);
        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));

        AcDNJournal journal = getJournalNewRecord(idJournal);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(AcDNJournal.Status.PROCESSED, journal.getStatus());

        if (null != baseEntityRepository.selectFirst("SELECT branch from gl_acc where bsaacid=? and branch=?", bsaacid, oldBranch)) {
            assertTrue(true);
        } else {
            baseEntityRepository.executeNativeUpdate("UPDATE GL_ACC SET branch=? WHERE BSAACID=?", bsaacid, oldBranch);
            assertTrue(false);
        }
    }

    /* =========================================================================================================================
    * */
    @Test
    public void testOpenFccStress() throws Exception {

        int cnt = 300;

        Date dbDate = DateUtils.dbDateParse("2017-11-09");
        String curDateStr = DateUtils.dbDateString(getOperday().getCurrentDate());
        String qType = AcDNJournal.Sources.FCC.name();
        String qName = acdeno6;

        printCommunicatorName();

        QueueProperties properties = getQueueProperties(qType, qName);
        clearQueue(properties, qName, cnt * 2);

        shutdownJob(ACDENO_TASK);
        startConnection(properties);

        List<DataRecord> mlist = baseEntityRepository.selectMaxRows(
                "select message from gl_acdeno where status_date <= ? and source = 'FCC' and status = 'PROCESSED' and \"COMMENT\" like '%/40817%' order by message_id desc", cnt, new Object[]{dbDate});
        Assert.assertNotEquals(0, mlist.size());
        for (DataRecord rec : mlist) {
            sendOneMessage(rec.getString(0), curDateStr, properties, qName);
        }

        Thread.sleep(1000L);

        String property = getJobProperty(qType + ":" + qName, qName, null, host, "1414", broker, channel, login, passw, "30", writeOut) + "mq.algo = simple\n";

        cnt = mlist.size();
        long idJ = getAcdenoMaxId();
        long idAcc = getGLAccMaxId();
        System.out.println(String.format("message_id = %d, gl_acc.id = %d", idJ, idAcc));
        runAcdenoJob(property, 50L);

        long cntrec = 0;
        long cntrec0 = -1;
        while (cntrec < cnt && cntrec != cntrec0) {
            Thread.sleep(30 * 1000L);
            cntrec0 = cntrec;
            cntrec = getStatistics(idJ);

        }
        Assert.assertEquals("Обработаны не все запросы", cnt, cntrec);

        DataRecord data = baseEntityRepository.selectFirst(
                "select count(*) from gl_acdeno where message_id > ? and status != 'PROCESSED'", idJ);
        Assert.assertEquals("Есть ошибки обработки", 0, data.getLong(0).longValue());

        data = baseEntityRepository.selectFirst(
                "select count(*) from gl_acc where id > ? and dto = ?", idAcc, getOperday().getCurrentDate());
        Assert.assertEquals("Есть ошибки даты открытия", cnt, data.getLong(0).longValue());

        shutdownJob(ACDENO_TASK);
        clearQueue(properties, qName, cnt);

    }

    private void sendOneMessage(String message, String curDateStr, QueueProperties properties, String qName) throws IOException, SAXException, XPathExpressionException, JMSException {
        Document doc = getDocument(docBuilder, message);
        String bsaacid = getXmlParam(xPath, doc, acdenoParentNode, cbNode);
        String dateOpenStr = getXmlParam(xPath, doc, acdenoParentNode, openNode);
        message =

        changeXmlParam(message, openNode, dateOpenStr, curDateStr);
                baseEntityRepository.executeNativeUpdate("delete from GL_ACC where BSAACID = ?",bsaacid);

        sendToQueue(message, properties, null,null,qName);
    }

    private long getStatistics(long idJ) throws SQLException {
        DataRecord record = baseEntityRepository.selectOne("select min(message_id), max(message_id), count(*), max(status_date) - min(status_date) from gl_acdeno where message_id > ? and status != 'RAW'", idJ);
        long cnt = record.getLong(2);
        System.out.println(String.format("Всего записей: %d, время: %s", cnt, record.getString(3)));
        return cnt;
    }

    @Test
    public void testReconectProps() throws Exception {

        int cnt = 3;

        Date dbDate = DateUtils.dbDateParse("2017-11-09");
        String curDateStr = DateUtils.dbDateString(getOperday().getCurrentDate());
        String qType = AcDNJournal.Sources.FCC.name();
        String qName = acdeno6;

        printCommunicatorName();

        QueueProperties properties = getQueueProperties(qType, qName);
        clearQueue(properties, qName, cnt * 2);

        shutdownJob(ACDENO_TASK);
        startConnection(properties);

        long idJ = getAcdenoMaxId();
        long idAudit = getAuditMaxId();
        System.out.println(String.format("message_id = %d, gl_audit.id = %d", idJ, idAudit));

        List<DataRecord> mlist = baseEntityRepository.selectMaxRows(
                "select message from gl_acdeno where status_date <= ? and source = 'FCC' and status = 'PROCESSED' and \"COMMENT\" like '%/40817%' order by message_id desc", cnt, new Object[]{dbDate});
        Assert.assertNotEquals(0, mlist.size());

        String []pass = {passw, "passw", passw};
        String []hosts = {host, "vs", host};
        int i = 0;
        for (DataRecord rec : mlist) {
            sendOneMessage(rec.getString(0), curDateStr, properties, qName);
            String property = getJobProperty(qType + ":" + qName, qName, null, hosts[i], "1414", broker, channel, login, passw, "10", writeOut) + "mq.algo = simple\n";
            Thread.sleep(1000L);
            SingleActionJob job =
                    SingleActionJobBuilder.create()
                            .withClass(AccountDetailsNotifyTask.class)
                            .withProps(property)
                            .build();
            jobService.executeJob(job);
//            runAcdenoJob(property, 50L);
            Thread.sleep(1000L);
            AuditRecord audit = getAuditError(idAudit);
            if (null != audit) {
                idAudit = audit.getId();
                System.out.println(audit.getMessage());
                System.out.println(audit.getErrorMessage());
//                Assert.assertTrue(i == 1);
            } else {
                System.out.println("Нет ошибки подключения");
//                Assert.assertTrue(i != 1);
            }
            i++;
        }

        shutdownJob(ACDENO_TASK);
        clearQueue(properties, qName, cnt);

    }

    private void reopenAccount(String bsaacid) {
        baseEntityRepository.executeNativeUpdate("update GL_ACC set DTC = null where BSAACID = ?", bsaacid);
    }

    private void checkCloseDate(String bsaacid, Date closeDate, boolean withGL) throws SQLException {
        Assert.assertNotNull(baseEntityRepository.selectFirst("SELECT * from GL_ACC where BSAACID=? and DTC = ?", bsaacid, closeDate));

    }

    private String getJobProperty(String topic, String queueIn) {
        return getJobProperty (topic, queueIn, null, host, "1414", broker, channel, login, passw, Integer.toString(batchSize), writeOut)
                + "mq.algo = simple\n";
    }

    private QueueProperties getQueueProperties(String topic, String qName) {
        return getQueueProperties(topic, qName, null, host, 1414, broker, channel, login, passw, 30, writeOut, false);
    }

    public String getResourceText(String resource) throws IOException {
        File inFile = new File(this.getClass().getResource(resource).getFile());
        return FileUtils.readFileToString(inFile, AccountDetailsNotifyProcessor.charsetName);
    }

    private AcDNJournal getJournalNewRecord(long idFrom) throws SQLException {
        return (AcDNJournal) baseEntityRepository.selectFirst(AcDNJournal.class, "from AcDNJournal j where j.id > ?1", idFrom);
    }

    private AuditRecord getAuditError(long idFrom ) throws SQLException {
        return (AuditRecord) baseEntityRepository.selectFirst(AuditRecord.class,
                "from AuditRecord a where a.logCode in ('AccountDetailsNotify', 'QueueProcessor') and a.logLevel <> 'Info' and a.id > ?1 ", idFrom);
    }

    private Date getOpenDate(String fileName) throws IOException, SAXException, XPathExpressionException, ParseException {
        String message = getRecourceText(fileName);
        Document doc = getDocument(docBuilder, message);
        String dtOpenParam = getXmlParam(xPath, doc, AccountDetailsNotifyProcessor.parentNodePath, "OpenDate");
        Date dateOpen = DateUtils.dbDateParse(dtOpenParam);
        return dateOpen;
    }
    public static void runAcdenoJob(String props, long interval) throws SQLException {
        DataRecord tsk = baseEntityRepository.selectFirst("select * from GL_SCHED_S where TSKNM = ?", ACDENO_TASK);
        TimerJob acdenoTaskJob = (TimerJob) baseEntityRepository.selectFirst(TimerJob.class
                , "from TimerJob j where j.name = ?1", ACDENO_TASK);
        if(null != acdenoTaskJob &&
                (!acdenoTaskJob.getSchedulingType().equals(JobSchedulingType.INTERVAL)
                        || acdenoTaskJob.getInterval() != interval
                        || !props.equals(acdenoTaskJob.getProperties())
                )
                || null != tsk) {
            baseEntityRepository.executeUpdate("delete from TimerJob j where j.name = ?1", ACDENO_TASK);
            acdenoTaskJob = null;
        }
        if (null == acdenoTaskJob) {
            IntervalJob acdenoJob = new IntervalJob();
            acdenoJob.setDelay(0L);
            acdenoJob.setDescription(ACDENO_TASK);
            acdenoJob.setRunnableClass(AccountDetailsNotifyTask.class.getName());
            acdenoJob.setProperties(props);
            acdenoJob.setStartupType(MANUAL);
            acdenoJob.setState(STOPPED);
            acdenoJob.setName(ACDENO_TASK);
            acdenoJob.setInterval(interval);
            try {
                acdenoTaskJob = (IntervalJob) baseEntityRepository.save(acdenoJob);
            } catch (Exception e) {
                restartSequenceWithTable(IntervalJob.class);
                acdenoTaskJob = (IntervalJob) baseEntityRepository.save(acdenoJob);
            }
        }
        remoteAccess.invoke(BackgroundJobsController.class, "startupJob", acdenoTaskJob );
//            jobService.startupJob(aclirqJob);
//            registerJob(aclirqJob);
    }

}
