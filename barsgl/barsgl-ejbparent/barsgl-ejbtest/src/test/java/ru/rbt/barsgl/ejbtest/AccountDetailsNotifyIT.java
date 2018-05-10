package ru.rbt.barsgl.ejbtest;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.controller.operday.task.AccountDetailsNotifyTask;
import ru.rbt.barsgl.ejb.controller.operday.task.AccountDetailsNotifyTaskOld;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.*;
import ru.rbt.barsgl.ejb.entity.acc.AcDNJournal;
import ru.rbt.barsgl.ejb.entity.cust.CustDNJournal;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.ejbcore.datarec.DataRecord;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Logger;

import static junit.framework.TestCase.assertTrue;
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Status.PROCESSED;
import static ru.rbt.ejbcore.util.DateUtils.getDatabaseDate;
import static ru.rbt.ejbcore.util.DateUtils.getFinalDateStr;

/**
 * Created by ER22228
 */
public class AccountDetailsNotifyIT extends AbstractQueueIT {

    public static final Logger logger = Logger.getLogger(AccountDetailsNotifyIT.class.getName());

//    private final static String host = "vs529";
//    private final static String broker = "QM_MBROKER4_T5";

    public static final String host = "vs11205"; //"mbrk4-inta.testhpcsa.imb.ru"; // "vs11205";
    public static final String broker = "QM_MBROKER4";

    private static final String channel= "SYSTEM.DEF.SVRCONN";
    private static final String login = "srvwbl4mqtest";
    private static final String passw = "UsATi8hU";
    private static final String acdeno6  = "UCBRU.ADP.BARSGL.V5.ACDENO.FCC.NOTIF";
    private static final String acdeno12 = "UCBRU.ADP.BARSGL.V5.ACDENO.FCC12.NOTIF";
    private static final Boolean writeOut = false;
    private static final int batchSize = 7;
    @Test
    public void testNotifyCloseFcc12() throws Exception {
        String qType = AcDNJournal.Sources.FC12.name();
        String qName = acdeno12;

        printCommunicatorName();

        QueueProperties properties = getQueueProperties(qType, qName);
        clearQueue(properties, qName, 10);

        startConnection(properties);
        sendToQueue(getResourceText("/AccountDetailsNotifyFcc12Close.xml"), properties, null, null, qName);

        long idJournal = getJournalMaxId();
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

        baseEntityRepository.executeNativeUpdate("delete from gl_acc where bsaacid='40817810000010696538'");

        printCommunicatorName();

        QueueProperties properties = getQueueProperties(qType, qName);
        clearQueue(properties, qName, 10);

        startConnection(properties);
        sendToQueue(getResourceText("/AccountDetailsNotifyFcc6Open.xml"), properties, null, null, qName);

        long idJournal = getJournalMaxId();
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
        long idJournal = getJournalMaxId();

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
        long idJournal = getJournalMaxId();

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
        long idJournal = getJournalMaxId();

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
        long idJournal = getJournalMaxId();

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
        long idJournal = getJournalMaxId();

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
        long idJournal = getJournalMaxId();

        String qType = AcDNJournal.Sources.FC12.name();
        String textMessage = getResourceText("/AccountDetailsNotifyFcc12Branch.xml");

        remoteAccess.invoke(AccountDetailsNotifyController.class, "processingWithLog", qType, new QueueInputMessage(textMessage), null, -1, -1);
        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));

        AcDNJournal journal = getJournalNewRecord(idJournal);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(AcDNJournal.Status.ERROR, journal.getStatus());

        if (null != baseEntityRepository.selectFirst("SELECT branch from gl_acc where bsaacid=? and branch=?", bsaacid, oldBranch))
            assertTrue(true);
        else{
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
        long idJournal = getJournalMaxId();

        String qType = AcDNJournal.Sources.FC12.name();
        String textMessage = getResourceText("/AccountDetailsNotifyFcc12Branch.xml");

        remoteAccess.invoke(AccountDetailsNotifyController.class, "processingWithLog", qType, new QueueInputMessage(textMessage), null, -1, -1);
        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));

        AcDNJournal journal = getJournalNewRecord(idJournal);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(AcDNJournal.Status.PROCESSED, journal.getStatus());

        if (null != baseEntityRepository.selectFirst("SELECT branch from gl_acc where bsaacid=? and branch=?", bsaacid, oldBranch)) {
            assertTrue(true);
        }else{
            baseEntityRepository.executeNativeUpdate("UPDATE GL_ACC SET branch=? WHERE BSAACID=?", bsaacid, oldBranch );
            assertTrue(false);
        }
    }

    private void reopenAccount(String bsaacid) {
        baseEntityRepository.executeNativeUpdate("update GL_ACC set DTC = null where BSAACID = ?", bsaacid);
    }

    private void checkCloseDate(String bsaacid, Date closeDate, boolean withGL) throws SQLException {
        Assert.assertNotNull(baseEntityRepository.selectFirst("SELECT * from GL_ACC where BSAACID=? and DTC = ?", bsaacid, closeDate));

    }

/*
    private static String closeEqualBranch=
            "<NS1:Envelope xmlns:NS1=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                    "    <NS1:Header>\n" +
                    "        <NS2:UCBRUHeaders xmlns:NS2=\"urn:imb:gbo:v2\">\n" +
                    "            <NS2:Audit>\n" +
                    "                <NS2:MessagePath>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountDetailsNotify.NotificationHandler</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v4</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-06-22T18:07:32.740+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>START</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountListQuery</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-06-22T18:07:32.747+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>START</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountListQuery</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-06-22T18:07:32.942+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>SUCCESS</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountDetailsNotify.Publisher</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-06-22T18:07:32.964+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>SUCCESS</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment></NS2:Comment>\n" +
                    "                    </NS2:Step>\n" +
                    "                </NS2:MessagePath>\n" +
                    "            </NS2:Audit>\n" +
                    "        </NS2:UCBRUHeaders>\n" +
                    "    </NS1:Header>\n" +
                    "    <NS1:Body>\n" +
                    "        <gbo:AccountList xmlns:gbo=\"urn:imb:gbo:v2\">\n" +
                    "            <gbo:AccountDetails>\n" +
                    "                <gbo:AccountNo>01628209RURPRDC102</gbo:AccountNo>\n" +
                    "                <gbo:Branch>A29</gbo:Branch>\n" +
                    "                <gbo:CBAccountNo>40802810900014820908</gbo:CBAccountNo>\n" +
                    "                <gbo:Ccy>RUR</gbo:Ccy>\n" +
                    "                <gbo:CcyDigital>810</gbo:CcyDigital>\n" +
                    "                <gbo:Status>O</gbo:Status>\n" +
                    "                <gbo:CustomerNo>01628209</gbo:CustomerNo>\n" +
                    "                <gbo:Special>PRDC1</gbo:Special>\n" +
                    "                <gbo:OpenDate>2016-06-22</gbo:OpenDate>\n" +
                    "                <gbo:AltAccountNo>01628209RURPRDC102</gbo:AltAccountNo>\n" +
                    "                <gbo:Type>S</gbo:Type>\n" +
                    "                <gbo:ATMAvailable>Y</gbo:ATMAvailable>\n" +
                    "                <gbo:IsDormant>N</gbo:IsDormant>\n" +
                    "                <gbo:PostAllowed>N</gbo:PostAllowed>\n" +
                    "                <gbo:CreditTransAllowed>Y</gbo:CreditTransAllowed>\n" +
                    "                <gbo:DebitTransAllowed>Y</gbo:DebitTransAllowed>\n" +
                    "                <gbo:ClearingBank>044525545</gbo:ClearingBank>\n" +
                    "                <gbo:CorBank>044525545</gbo:CorBank>\n" +
                    "                <gbo:Positioning>\n" +
                    "                    <gbo:CBAccount>40817810950010736918</gbo:CBAccount>\n" +
                    "                    <gbo:IMBAccountNo>40817810950010736918</gbo:IMBAccountNo>\n" +
                    "                    <gbo:IMBBranch>A29</gbo:IMBBranch>\n" +
                    "                    <gbo:HostABSAccountNo>01628209RURPRDC102</gbo:HostABSAccountNo>\n" +
                    "                    <gbo:HostABSBranch>A29</gbo:HostABSBranch>\n" +
                    "                    <gbo:HostABS>FCC</gbo:HostABS>\n" +
                    "                </gbo:Positioning>\n" +
                    "                <gbo:MIS>\n" +
                    "                    <gbo:GroupComponent>R_P_MC</gbo:GroupComponent>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS1</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS2</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS3</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS4</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS5</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS6</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS7</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS8</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS9</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS10</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS1</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS2</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS3</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS4</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS5</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS6</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS7</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS8</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS9</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS10</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CostCode>\n" +
                    "                        <gbo:Name>COSTCOD1</gbo:Name>\n" +
                    "                    </gbo:CostCode>\n" +
                    "                    <gbo:CostCode>\n" +
                    "                        <gbo:Name>COSTCOD2</gbo:Name>\n" +
                    "                    </gbo:CostCode>\n" +
                    "                    <gbo:CostCode>\n" +
                    "                        <gbo:Name>COSTCOD3</gbo:Name>\n" +
                    "                    </gbo:CostCode>\n" +
                    "                    <gbo:CostCode>\n" +
                    "                        <gbo:Name>COSTCOD4</gbo:Name>\n" +
                    "                    </gbo:CostCode>\n" +
                    "                    <gbo:CostCode>\n" +
                    "                        <gbo:Name>COSTCOD5</gbo:Name>\n" +
                    "                    </gbo:CostCode>\n" +
                    "                </gbo:MIS>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>GWSAccType</gbo:Name>\n" +
                    "                    <gbo:Value>CURR</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>OperationTypeCodes</gbo:Name>\n" +
                    "                    <gbo:Value>VIEW=0,DOMPAY=1,CUPADE=0,CUPACO=0,</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>ParentBranchName</gbo:Name>\n" +
                    "                    <gbo:Value>UCB, Moscow</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>CusSegment</gbo:Name>\n" +
                    "                    <gbo:Value>TIER_I</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "            </gbo:AccountDetails>\n" +
                    "        </gbo:AccountList>\n" +
                    "    </NS1:Body>\n" +
                    "</NS1:Envelope>";

    public static String error1=
            "<NS1:Envelope xmlns:NS1=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                    "    <NS1:Header>\n" +
                    "        <NS2:UCBRUHeaders xmlns:NS2=\"urn:imb:gbo:v2\">\n" +
                    "            <NS2:Audit>\n" +
                    "                <NS2:MessagePath>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountDetailsNotify.NotificationHandler</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v4</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-09-07T09:15:55.966+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>START</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountListQuery.ESBDBRequest</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-09-07T09:15:55.988+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>START</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountListQuery</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-09-07T09:15:55.994+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>START</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountListQuery</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-09-07T09:15:56.171+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>SUCCESS</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountListQuery.ESBDBResponse</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-09-07T09:15:56.178+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>SUCCESS</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment>Finish of message processing</NS2:Comment>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountDetailsNotify.Publisher</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-09-07T09:15:56.198+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>SUCCESS</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment></NS2:Comment>\n" +
                    "                    </NS2:Step>\n" +
                    "                </NS2:MessagePath>\n" +
                    "            </NS2:Audit>\n" +
                    "        </NS2:UCBRUHeaders>\n" +
                    "    </NS1:Header>\n" +
                    "    <NS1:Body>\n" +
                    "        <gbo:AccountList xmlns:gbo=\"urn:imb:gbo:v2\">\n" +
                    "            <gbo:AccountDetails>\n" +
                    "                <gbo:AccountNo>800458RUR400902065</gbo:AccountNo>\n" +
                    "                <gbo:Branch>065</gbo:Branch>\n" +
                    "                <gbo:CBAccountNo>40802810500014908835</gbo:CBAccountNo>\n" +
                    "                <gbo:Ccy>RUR</gbo:Ccy>\n" +
                    "                <gbo:CcyDigital>810</gbo:CcyDigital>\n" +
                    "                <gbo:Description>VISHNYAKOV ALEKSEY</gbo:Description>\n" +
                    "                <gbo:Status>O</gbo:Status>\n" +
                    "                <gbo:CustomerNo>00800458</gbo:CustomerNo>\n" +
                    "                <gbo:Special>4009</gbo:Special>\n" +
                    "                <gbo:OpenDate>2016-09-07</gbo:OpenDate>\n" +
                    "                <gbo:CreditTransAllowed>Y</gbo:CreditTransAllowed>\n" +
                    "                <gbo:DebitTransAllowed>Y</gbo:DebitTransAllowed>\n" +
                    "                <gbo:CorBank>044525545</gbo:CorBank>\n" +
                    "                <gbo:CorINN>772457163509</gbo:CorINN>\n" +
                    "                <gbo:Positioning>\n" +
                    "                    <gbo:CBAccount>40802810500014908835</gbo:CBAccount>\n" +
                    "                    <gbo:IMBAccountNo>40802810500014908835</gbo:IMBAccountNo>\n" +
                    "                    <gbo:IMBBranch>065</gbo:IMBBranch>\n" +
                    "                    <gbo:HostABSAccountNo>800458RUR400902065</gbo:HostABSAccountNo>\n" +
                    "                    <gbo:HostABSBranch>065</gbo:HostABSBranch>\n" +
                    "                    <gbo:HostABS>MIDAS</gbo:HostABS>\n" +
                    "                </gbo:Positioning>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>GWSAccType</gbo:Name>\n" +
                    "                    <gbo:Value>CURR</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>OperationTypeCodes</gbo:Name>\n" +
                    "                    <gbo:Value>\n" +
                    "                        VIEW=1,DOMPAY=1,DOMTAX=1,DOMCUS=1,CUPADE=1,CUPACO=1,CUSEOD=1,CUSEOC=1,CURBUY=1,CUOPCE=1,\n" +
                    "                    </gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>ParentBranchName</gbo:Name>\n" +
                    "                    <gbo:Value>UCB, Moscow</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>CusSegment</gbo:Name>\n" +
                    "                    <gbo:Value>TIER_I</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "            </gbo:AccountDetails>\n" +
                    "        </gbo:AccountList>\n" +
                    "    </NS1:Body>\n" +
                    "</NS1:Envelope>\n";

    static String fullTopicTest =
            "<NS1:Envelope xmlns:NS1=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                    "    <NS1:Header>\n" +
                    "        <NS2:UCBRUHeaders xmlns:NS2=\"urn:imb:gbo:v2\">\n" +
                    "            <NS2:Audit>\n" +
                    "                <NS2:MessagePath>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountDetailsNotify.NotificationHandler</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v4</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-06-22T18:07:32.740+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>START</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountListQuery</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-06-22T18:07:32.747+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>START</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountListQuery</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-06-22T18:07:32.942+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>SUCCESS</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountDetailsNotify.Publisher</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-06-22T18:07:32.964+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>SUCCESS</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment></NS2:Comment>\n" +
                    "                    </NS2:Step>\n" +
                    "                </NS2:MessagePath>\n" +
                    "            </NS2:Audit>\n" +
                    "        </NS2:UCBRUHeaders>\n" +
                    "    </NS1:Header>\n" +
                    "    <NS1:Body>\n" +
                    "        <gbo:AccountList xmlns:gbo=\"urn:imb:gbo:v2\">\n" +
                    "            <gbo:AccountDetails>\n" +
                    "                <gbo:AccountNo>01628209RURPRDC102</gbo:AccountNo>\n" +
                    "                <gbo:Branch>A29</gbo:Branch>\n" +
                    "                <gbo:CBAccountNo>40817810950010736918</gbo:CBAccountNo>\n" +
                    "                <gbo:Ccy>RUR</gbo:Ccy>\n" +
                    "                <gbo:CcyDigital>810</gbo:CcyDigital>\n" +
                    "                <gbo:Status>O</gbo:Status>\n" +
                    "                <gbo:CustomerNo>01628209</gbo:CustomerNo>\n" +
                    "                <gbo:Special>PRDC1</gbo:Special>\n" +
                    "                <gbo:OpenDate>2016-06-22</gbo:OpenDate>\n" +
                    "                <gbo:AltAccountNo>01628209RURPRDC102</gbo:AltAccountNo>\n" +
                    "                <gbo:Type>S</gbo:Type>\n" +
                    "                <gbo:ATMAvailable>Y</gbo:ATMAvailable>\n" +
                    "                <gbo:IsDormant>N</gbo:IsDormant>\n" +
                    "                <gbo:PostAllowed>N</gbo:PostAllowed>\n" +
                    "                <gbo:CreditTransAllowed>Y</gbo:CreditTransAllowed>\n" +
                    "                <gbo:DebitTransAllowed>Y</gbo:DebitTransAllowed>\n" +
                    "                <gbo:ClearingBank>044525545</gbo:ClearingBank>\n" +
                    "                <gbo:CorBank>044525545</gbo:CorBank>\n" +
                    "                <gbo:Positioning>\n" +
                    "                    <gbo:CBAccount>40817810950010736918</gbo:CBAccount>\n" +
                    "                    <gbo:IMBAccountNo>40817810950010736918</gbo:IMBAccountNo>\n" +
                    "                    <gbo:IMBBranch>A29</gbo:IMBBranch>\n" +
                    "                    <gbo:HostABSAccountNo>01628209RURPRDC102</gbo:HostABSAccountNo>\n" +
                    "                    <gbo:HostABSBranch>A29</gbo:HostABSBranch>\n" +
                    "                    <gbo:HostABS>FCC</gbo:HostABS>\n" +
                    "                </gbo:Positioning>\n" +
                    "                <gbo:MIS>\n" +
                    "                    <gbo:GroupComponent>R_P_MC</gbo:GroupComponent>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS1</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS2</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS3</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS4</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS5</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS6</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS7</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS8</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS9</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:TransactionClass>\n" +
                    "                        <gbo:Name>TXNMIS10</gbo:Name>\n" +
                    "                    </gbo:TransactionClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS1</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS2</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS3</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS4</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS5</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS6</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS7</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS8</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS9</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CompositeClass>\n" +
                    "                        <gbo:Name>COMPMIS10</gbo:Name>\n" +
                    "                    </gbo:CompositeClass>\n" +
                    "                    <gbo:CostCode>\n" +
                    "                        <gbo:Name>COSTCOD1</gbo:Name>\n" +
                    "                    </gbo:CostCode>\n" +
                    "                    <gbo:CostCode>\n" +
                    "                        <gbo:Name>COSTCOD2</gbo:Name>\n" +
                    "                    </gbo:CostCode>\n" +
                    "                    <gbo:CostCode>\n" +
                    "                        <gbo:Name>COSTCOD3</gbo:Name>\n" +
                    "                    </gbo:CostCode>\n" +
                    "                    <gbo:CostCode>\n" +
                    "                        <gbo:Name>COSTCOD4</gbo:Name>\n" +
                    "                    </gbo:CostCode>\n" +
                    "                    <gbo:CostCode>\n" +
                    "                        <gbo:Name>COSTCOD5</gbo:Name>\n" +
                    "                    </gbo:CostCode>\n" +
                    "                </gbo:MIS>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>GWSAccType</gbo:Name>\n" +
                    "                    <gbo:Value>CURR</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>OperationTypeCodes</gbo:Name>\n" +
                    "                    <gbo:Value>VIEW=0,DOMPAY=1,CUPADE=0,CUPACO=0,</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>ParentBranchName</gbo:Name>\n" +
                    "                    <gbo:Value>UCB, Moscow</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>CusSegment</gbo:Name>\n" +
                    "                    <gbo:Value>TIER_I</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "            </gbo:AccountDetails>\n" +
                    "        </gbo:AccountList>\n" +
                    "    </NS1:Body>\n" +
                    "</NS1:Envelope>";

    public static String fullTopicTestMidasOld =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "<NS1:Envelope xmlns:NS1=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                    "    <NS1:Header>\n" +
                    "        <NS2:UCBRUHeaders xmlns:NS2=\"urn:imb:gbo:v2\">\n" +
                    "            <NS2:Audit>\n" +
                    "                <NS2:MessagePath>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountDetailsNotify.NotificationHandler</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v4</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-03-29T17:50:54.289+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>START</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountListQuery</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-03-29T17:50:54.295+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>START</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountListQuery</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-03-29T17:50:54.405+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>SUCCESS</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment/>\n" +
                    "                    </NS2:Step>\n" +
                    "                    <NS2:Step>\n" +
                    "                        <NS2:Application.Module>SRVACC.AccountDetailsNotify.Publisher</NS2:Application.Module>\n" +
                    "                        <NS2:VersionId>v2</NS2:VersionId>\n" +
                    "                        <NS2:TimeStamp>2016-03-29T17:50:54.413+03:00</NS2:TimeStamp>\n" +
                    "                        <NS2:RoutingRole>SUCCESS</NS2:RoutingRole>\n" +
                    "                        <NS2:Comment></NS2:Comment>\n" +
                    "                    </NS2:Step>\n" +
                    "                </NS2:MessagePath>\n" +
                    "            </NS2:Audit>\n" +
                    "        </NS2:UCBRUHeaders>\n" +
                    "    </NS1:Header>\n" +
                    "    <NS1:Body>\n" +
                    "        <gbo:AccountList xmlns:gbo=\"urn:imb:gbo:v2\">\n" +
                    "            <gbo:AccountDetails>\n" +
                    "                <gbo:AccountNo>695430RUR401102097</gbo:AccountNo>\n" +
                    "                <gbo:Branch>097</gbo:Branch>\n" +
                    "                <gbo:CBAccountNo>40702810400154748352</gbo:CBAccountNo>\n" +
                    "                <gbo:Ccy>RUR</gbo:Ccy>\n" +
                    "                <gbo:CcyDigital>810</gbo:CcyDigital>\n" +
                    "                <gbo:Description>БББББББББББББ</gbo:Description>\n" +//(портфель 2,  5 КК)
                    "                <gbo:Status>O</gbo:Status>\n" +
                    "                <gbo:CustomerNo>00695430</gbo:CustomerNo>\n" +
                    "                <gbo:Special>4011</gbo:Special>\n" +
                    "                <gbo:OpenDate>2014-12-22</gbo:OpenDate>\n" +
                    "                <gbo:CreditTransAllowed>N</gbo:CreditTransAllowed>\n" +
                    "                <gbo:DebitTransAllowed>N</gbo:DebitTransAllowed>\n" +
                    "                <gbo:CorBank>046027238</gbo:CorBank>\n" +
                    "                <gbo:CorINN>6166083860</gbo:CorINN>\n" +
                    "                <gbo:Positioning>\n" +
                    "                    <gbo:CBAccount>40702810400154748352</gbo:CBAccount>\n" +
                    "                    <gbo:IMBAccountNo>40702810400154748352</gbo:IMBAccountNo>\n" +
                    "                    <gbo:IMBBranch>097</gbo:IMBBranch>\n" +
                    "                    <gbo:HostABSAccountNo>695430RUR401102097</gbo:HostABSAccountNo>\n" +
                    "                    <gbo:HostABSBranch>097</gbo:HostABSBranch>\n" +
                    "                    <gbo:HostABS>MIDAS</gbo:HostABS>\n" +
                    "                </gbo:Positioning>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>GWSAccType</gbo:Name>\n" +
                    "                    <gbo:Value>CURR</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>OperationTypeCodes</gbo:Name>\n" +
                    "                    <gbo:Value>\n" +
                    "                        VIEW=1,DOMPAY=1,DOMTAX=1,DOMCUS=1,CUPADE=1,CUPACO=1,CUSEOD=1,CUSEOC=1,CURBUY=1,CUOPCE=1,\n" +
                    "                    </gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>ParentBranchName</gbo:Name>\n" +
                    "                    <gbo:Value>UCB, Rostov Branch</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:UDF>\n" +
                    "                    <gbo:Name>CusSegment</gbo:Name>\n" +
                    "                    <gbo:Value>TIER_I</gbo:Value>\n" +
                    "                </gbo:UDF>\n" +
                    "                <gbo:ShadowAccounts>\n" +
                    "                    <gbo:HostABS>FCC</gbo:HostABS>\n" +
                    "                    <gbo:AccountNo>00695430RURCOSA101</gbo:AccountNo>\n" +
                    "                    <gbo:Branch>C04</gbo:Branch>\n" +
                    "                </gbo:ShadowAccounts>\n" +
                    "            </gbo:AccountDetails>\n" +
                    "        </gbo:AccountList>\n" +
                    "    </NS1:Body>\n" +
                    "</NS1:Envelope>";

    String notifyFC12Close ="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:gbo=\"urn:ucbru:gbo:v5\"\n" +
            "                  xmlns:acc=\"urn:ucbru:gbo:v5:acc\">\n" +
            "    <soapenv:Header>\n" +
            "        <gbo:UCBRUHeaders>\n" +
            "            <gbo:Audit>\n" +
            "                <gbo:MessagePath>\n" +
            "                    <gbo:Step>\n" +
            "                        <gbo:Application.Module>ATSFC12AC.AccountDetailsNotify.NotificationHandler\n" +
            "                        </gbo:Application.Module>\n" +
            "                        <gbo:VersionId>v5</gbo:VersionId>\n" +
            "                        <gbo:TimeStamp>2016-11-24T12:43:11.055+03:00</gbo:TimeStamp>\n" +
            "                        <gbo:RoutingRole>ROUTE</gbo:RoutingRole>\n" +
            "                        <gbo:Comment>Routing in SACC.v5.AcDeNo</gbo:Comment>\n" +
            "                    </gbo:Step>\n" +
            "                    <gbo:Step>\n" +
            "                        <gbo:Application.Module>SACC.v5.AcDeNo.NotificationHandler</gbo:Application.Module>\n" +
            "                        <gbo:VersionId>v5</gbo:VersionId>\n" +
            "                        <gbo:TimeStamp>2016-11-24T12:43:11.059+03:00</gbo:TimeStamp>\n" +
            "                        <gbo:RoutingRole>START</gbo:RoutingRole>\n" +
            "                        <gbo:Comment>Start of message processing</gbo:Comment>\n" +
            "                    </gbo:Step>\n" +
            "                    <gbo:Step>\n" +
            "                        <gbo:Application.Module>SACC.AccountListQuery.RequestHandler</gbo:Application.Module>\n" +
            "                        <gbo:VersionId>v5</gbo:VersionId>\n" +
            "                        <gbo:TimeStamp>2016-11-24T12:43:11.063+03:00</gbo:TimeStamp>\n" +
            "                        <gbo:RoutingRole>START</gbo:RoutingRole>\n" +
            "                        <gbo:Comment>Start of message processing</gbo:Comment>\n" +
            "                    </gbo:Step>\n" +
            "                    <gbo:Step>\n" +
            "                        <gbo:Application.Module>SACC.MasterAccountPositioningBatchQuery.RequestHandler\n" +
            "                        </gbo:Application.Module>\n" +
            "                        <gbo:VersionId>v5</gbo:VersionId>\n" +
            "                        <gbo:TimeStamp>2016-11-24T12:43:11.069+03:00</gbo:TimeStamp>\n" +
            "                        <gbo:RoutingRole>START</gbo:RoutingRole>\n" +
            "                        <gbo:Comment>Start of message processing</gbo:Comment>\n" +
            "                    </gbo:Step>\n" +
            "                    <gbo:Step>\n" +
            "                        <gbo:Application.Module>SACC.MasterAccountPositioningBatchQuery.RequestHandler\n" +
            "                        </gbo:Application.Module>\n" +
            "                        <gbo:VersionId>v5</gbo:VersionId>\n" +
            "                        <gbo:TimeStamp>2016-11-24T12:43:11.075+03:00</gbo:TimeStamp>\n" +
            "                        <gbo:RoutingRole>ROUTE</gbo:RoutingRole>\n" +
            "                        <gbo:Comment>In the cache of the accounts not detected, will be requested ABS</gbo:Comment>\n" +
            "                    </gbo:Step>\n" +
            "                    <gbo:Step>\n" +
            "                        <gbo:Application.Module>SACC.MasterAccountPositioningBatchQuery.ABSRequestHandler\n" +
            "                        </gbo:Application.Module>\n" +
            "                        <gbo:VersionId>v5</gbo:VersionId>\n" +
            "                        <gbo:TimeStamp>2016-11-24T12:43:11.078+03:00</gbo:TimeStamp>\n" +
            "                        <gbo:RoutingRole>START</gbo:RoutingRole>\n" +
            "                        <gbo:Comment>Start of message processing</gbo:Comment>\n" +
            "                    </gbo:Step>\n" +
            "                    <gbo:Step>\n" +
            "                        <gbo:Application.Module>SACC.MasterAccountPositioningBatchQuery.ABSRequestHandler\n" +
            "                        </gbo:Application.Module>\n" +
            "                        <gbo:VersionId>v5</gbo:VersionId>\n" +
            "                        <gbo:TimeStamp>2016-11-24T12:43:11.080+03:00</gbo:TimeStamp>\n" +
            "                        <gbo:RoutingRole>ROUTE</gbo:RoutingRole>\n" +
            "                        <gbo:Comment>Routing by cache settings</gbo:Comment>\n" +
            "                    </gbo:Step>\n" +
            "                    <gbo:Step>\n" +
            "                        <gbo:Application.Module>SACC.MasterAccountPositioningBatchQuery.ABSResponseHandler\n" +
            "                        </gbo:Application.Module>\n" +
            "                        <gbo:VersionId>v5</gbo:VersionId>\n" +
            "                        <gbo:TimeStamp>2016-11-24T12:43:11.316+03:00</gbo:TimeStamp>\n" +
            "                        <gbo:RoutingRole>ROUTE SUCCESS</gbo:RoutingRole>\n" +
            "                        <gbo:Comment>The responses received from ECHO,FC12</gbo:Comment>\n" +
            "                    </gbo:Step>\n" +
            "                    <gbo:Step>\n" +
            "                        <gbo:Application.Module>SACC.MasterAccountPositioningBatchQuery.ABSResponseHandler\n" +
            "                        </gbo:Application.Module>\n" +
            "                        <gbo:VersionId>v5</gbo:VersionId>\n" +
            "                        <gbo:TimeStamp>2016-11-24T12:43:11.318+03:00</gbo:TimeStamp>\n" +
            "                        <gbo:RoutingRole>SUCCESS</gbo:RoutingRole>\n" +
            "                        <gbo:Comment>Finish of message processing</gbo:Comment>\n" +
            "                    </gbo:Step>\n" +
            "                    <gbo:Step>\n" +
            "                        <gbo:Application.Module>SACC.MasterAccountPositioningBatchQuery.ResponseHandler\n" +
            "                        </gbo:Application.Module>\n" +
            "                        <gbo:VersionId>v5</gbo:VersionId>\n" +
            "                        <gbo:TimeStamp>2016-11-24T12:43:11.321+03:00</gbo:TimeStamp>\n" +
            "                        <gbo:RoutingRole>SUCCESS</gbo:RoutingRole>\n" +
            "                        <gbo:Comment>Finish of message processing</gbo:Comment>\n" +
            "                    </gbo:Step>\n" +
            "                    <gbo:Step>\n" +
            "                        <gbo:Application.Module>SACC.AccountListQuery.ABSRouter</gbo:Application.Module>\n" +
            "                        <gbo:VersionId>v5</gbo:VersionId>\n" +
            "                        <gbo:TimeStamp>2016-11-24T12:43:11.325+03:00</gbo:TimeStamp>\n" +
            "                        <gbo:RoutingRole>ROUTE</gbo:RoutingRole>\n" +
            "                        <gbo:Comment>Routing by cache settings</gbo:Comment>\n" +
            "                    </gbo:Step>\n" +
            "                    <gbo:Step>\n" +
            "                        <gbo:Application.Module>SACC.AccountListQuery.ResponseHandler</gbo:Application.Module>\n" +
            "                        <gbo:VersionId>5</gbo:VersionId>\n" +
            "                        <gbo:TimeStamp>2016-11-24T12:43:11.496+03:00</gbo:TimeStamp>\n" +
            "                        <gbo:RoutingRole>ROUTE SUCCESS</gbo:RoutingRole>\n" +
            "                        <gbo:Comment>The responses received from ECHO,FC12</gbo:Comment>\n" +
            "                    </gbo:Step>\n" +
            "                    <gbo:Step>\n" +
            "                        <gbo:Application.Module>SACC.AccountListQuery.ResponseHandler</gbo:Application.Module>\n" +
            "                        <gbo:VersionId>v5</gbo:VersionId>\n" +
            "                        <gbo:TimeStamp>2016-11-24T12:43:11.498+03:00</gbo:TimeStamp>\n" +
            "                        <gbo:RoutingRole>SUCCESS</gbo:RoutingRole>\n" +
            "                        <gbo:Comment>Finish of message processing</gbo:Comment>\n" +
            "                    </gbo:Step>\n" +
            "                    <gbo:Step>\n" +
            "                        <gbo:Application.Module>SACC.v5.AcDeNo.PublishHandler</gbo:Application.Module>\n" +
            "                        <gbo:VersionId>v5</gbo:VersionId>\n" +
            "                        <gbo:TimeStamp>2016-11-24T12:43:11.504+03:00</gbo:TimeStamp>\n" +
            "                        <gbo:RoutingRole>SUCCESS</gbo:RoutingRole>\n" +
            "                        <gbo:Comment>Finish of message processing</gbo:Comment>\n" +
            "                    </gbo:Step>\n" +
            "                </gbo:MessagePath>\n" +
            "            </gbo:Audit>\n" +
            "        </gbo:UCBRUHeaders>\n" +
            "    </soapenv:Header>\n" +
            "    <soapenv:Body>\n" +
            "        <acc:AccountList>\n" +
            "            <acc:AccountDetails>\n" +
            "                <acc:AccountNo>00145005RURCOSA101</acc:AccountNo>\n" +
            "                <acc:Branch>E01</acc:Branch>\n" +
            "                <acc:CBAccountNo>40702810400094449118</acc:CBAccountNo>\n" +
            "                <acc:Ccy>RUR</acc:Ccy>\n" +
            "                <acc:Description>OOO GruzAvtoImport</acc:Description>\n" +
            "                <acc:Status>C</acc:Status>\n" +
            "                <acc:CustomerNo>00145005</acc:CustomerNo>\n" +
            "                <acc:Special>COSA1</acc:Special>\n" +
            "                <acc:OpenDate>2011-07-22</acc:OpenDate>\n" +
            "                <acc:AltAccountNo>145005RUR401102009</acc:AltAccountNo>\n" +
            "                <acc:ATMAvailable>N</acc:ATMAvailable>\n" +
            "                <acc:CreditTransAllowed>Y</acc:CreditTransAllowed>\n" +
            "                <acc:DebitTransAllowed>Y</acc:DebitTransAllowed>\n" +
            "                <acc:BackPeriodEntryAllowed>Y</acc:BackPeriodEntryAllowed>\n" +
            "                <acc:AutoProvisionRequired>N</acc:AutoProvisionRequired>\n" +
            "                <acc:ClearingAccountNo>00145005RURCOSA101</acc:ClearingAccountNo>\n" +
            "                <acc:DormantParameter>B</acc:DormantParameter>\n" +
            "                <acc:ExcludeReversalTrans>N</acc:ExcludeReversalTrans>\n" +
            "                <acc:MT210Required>N</acc:MT210Required>\n" +
            "                <acc:SweepType>1</acc:SweepType>\n" +
            "                <acc:CorBank>045773873</acc:CorBank>\n" +
            "                <acc:CorINN>7710030411</acc:CorINN>\n" +
            "                <acc:Positioning>\n" +
            "                    <acc:CBAccount>40702810400094449118</acc:CBAccount>\n" +
            "                    <acc:IMBAccountNo>00145005RURCOSA101</acc:IMBAccountNo>\n" +
            "                    <acc:IMBBranch>E01</acc:IMBBranch>\n" +
            "                    <acc:HostABSAccountNo>00145005RURCOSA101</acc:HostABSAccountNo>\n" +
            "                    <acc:HostABSBranch>E01</acc:HostABSBranch>\n" +
            "                    <acc:HostABS>FC12</acc:HostABS>\n" +
            "                </acc:Positioning>\n" +
            "                <acc:GLCredit>\n" +
            "                    <acc:CBLine>LI400</acc:CBLine>\n" +
            "                    <acc:GLValue>202019102</acc:GLValue>\n" +
            "                    <acc:HOLine>202000</acc:HOLine>\n" +
            "                </acc:GLCredit>\n" +
            "                <acc:GLDebit>\n" +
            "                    <acc:CBLine>AS400</acc:CBLine>\n" +
            "                    <acc:GLValue>105049022</acc:GLValue>\n" +
            "                    <acc:HOLine>105000</acc:HOLine>\n" +
            "                </acc:GLDebit>\n" +
            "                <acc:StmtSettings>\n" +
            "                    <acc:Cycle>D</acc:Cycle>\n" +
            "                    <acc:Type>D</acc:Type>\n" +
            "                    <acc:GenOnMovement>Y</acc:GenOnMovement>\n" +
            "                </acc:StmtSettings>\n" +
            "                <acc:Stmt2Settings>\n" +
            "                    <acc:Type>N</acc:Type>\n" +
            "                </acc:Stmt2Settings>\n" +
            "                <acc:Stmt3Settings>\n" +
            "                    <acc:Type>N</acc:Type>\n" +
            "                </acc:Stmt3Settings>\n" +
            "                <acc:PrevStmt>\n" +
            "                    <acc:Date>2016-09-14</acc:Date>\n" +
            "                </acc:PrevStmt>\n" +
            "                <acc:PrevStmt2/>\n" +
            "                <acc:PrevStmt3/>\n" +
            "                <acc:MIS>\n" +
            "                    <acc:Pool>POOL2</acc:Pool>\n" +
            "                </acc:MIS>\n" +
            "                <acc:ControlledAccount>Y</acc:ControlledAccount>\n" +
            "                <acc:CloseDate>2016-09-14</acc:CloseDate>\n" +
            "                <acc:OpenBalance>0</acc:OpenBalance>\n" +
            "                <acc:CurrentBalance>0</acc:CurrentBalance>\n" +
            "                <acc:AvailableBalance>0</acc:AvailableBalance>\n" +
            "                <acc:UncollectedBalance>0</acc:UncollectedBalance>\n" +
            "                <acc:CreditTurnoverToday>0</acc:CreditTurnoverToday>\n" +
            "                <acc:DebitTurnoverToday>0</acc:DebitTurnoverToday>\n" +
            "                <acc:BlockedAmount>0</acc:BlockedAmount>\n" +
            "                <acc:DebitAccrued>0</acc:DebitAccrued>\n" +
            "                <acc:CreditAccrued>0</acc:CreditAccrued>\n" +
            "                <acc:GWSAccType>CURR</acc:GWSAccType>\n" +
            "                <acc:OperationTypeCodes>\n" +
            "                    VIEW=1,DOMPAY=1,DOMTAX=1,DOMCUS=1,CUPADE=1,CUPACO=1,CUSEOD=1,CUSEOC=1,CURBUY=1,CUOPCE=1,\n" +
            "                </acc:OperationTypeCodes>\n" +
            "            </acc:AccountDetails>\n" +
            "        </acc:AccountList>\n" +
            "    </soapenv:Body>\n" +
            "</soapenv:Envelope>";

    String notifyFccClose ="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:gbo=\"urn:ucbru:gbo:v5\"\n" +
            "                  xmlns:acc=\"urn:ucbru:gbo:v5:acc\">\n" +
            "    <soapenv:Header>\n" +
            "        <NS1:UCBRUHeaders xmlns:NS1=\"urn:ucbru:gbo:v5\">\n" +
            "            <NS1:Audit>\n" +
            "                <NS1:MessagePath>\n" +
            "                    <NS1:Step>\n" +
            "                        <NS1:Application.Module>SACC.v5.AcDeNo.NotificationHandler</NS1:Application.Module>\n" +
            "                        <NS1:VersionId>v5</NS1:VersionId>\n" +
            "                        <NS1:TimeStamp>2017-11-22T14:13:42.032+03:00</NS1:TimeStamp>\n" +
            "                        <NS1:RoutingRole>START</NS1:RoutingRole>\n" +
            "                        <NS1:Comment>Start of message processing</NS1:Comment>\n" +
            "                    </NS1:Step>\n" +
            "                    <NS1:Step>\n" +
            "                        <NS1:Application.Module>SACC.AccountListQuery.RequestHandler</NS1:Application.Module>\n" +
            "                        <NS1:VersionId>v5</NS1:VersionId>\n" +
            "                        <NS1:TimeStamp>2017-11-22T14:13:42.044+03:00</NS1:TimeStamp>\n" +
            "                        <NS1:RoutingRole>START</NS1:RoutingRole>\n" +
            "                        <NS1:Comment>Start of message processing</NS1:Comment>\n" +
            "                    </NS1:Step>\n" +
            "                    <NS1:Step>\n" +
            "                        <NS1:Application.Module>SACC.MasterAccountPositioningBatchQuery.RequestHandler\n" +
            "                        </NS1:Application.Module>\n" +
            "                        <NS1:VersionId>v5</NS1:VersionId>\n" +
            "                        <NS1:TimeStamp>2017-11-22T14:13:42.076+03:00</NS1:TimeStamp>\n" +
            "                        <NS1:RoutingRole>START</NS1:RoutingRole>\n" +
            "                        <NS1:Comment>Start of message processing</NS1:Comment>\n" +
            "                    </NS1:Step>\n" +
            "                    <NS1:Step>\n" +
            "                        <NS1:Application.Module>SACC.MasterAccountPositioningBatchQuery.RequestHandler\n" +
            "                        </NS1:Application.Module>\n" +
            "                        <NS1:VersionId>v5</NS1:VersionId>\n" +
            "                        <NS1:TimeStamp>2017-11-22T14:13:42.105+03:00</NS1:TimeStamp>\n" +
            "                        <NS1:RoutingRole>ROUTE</NS1:RoutingRole>\n" +
            "                        <NS1:Comment>In the cache of the accounts not detected, will be requested ABS</NS1:Comment>\n" +
            "                    </NS1:Step>\n" +
            "                    <NS1:Step>\n" +
            "                        <NS1:Application.Module>SACC.MasterAccountPositioningBatchQuery.ABSRequestHandler\n" +
            "                        </NS1:Application.Module>\n" +
            "                        <NS1:VersionId>v5</NS1:VersionId>\n" +
            "                        <NS1:TimeStamp>2017-11-22T14:13:42.119+03:00</NS1:TimeStamp>\n" +
            "                        <NS1:RoutingRole>START</NS1:RoutingRole>\n" +
            "                        <NS1:Comment>Start of message processing</NS1:Comment>\n" +
            "                    </NS1:Step>\n" +
            "                    <NS1:Step>\n" +
            "                        <NS1:Application.Module>SACC.MasterAccountPositioningBatchQuery.ABSRequestHandler\n" +
            "                        </NS1:Application.Module>\n" +
            "                        <NS1:VersionId>v5</NS1:VersionId>\n" +
            "                        <NS1:TimeStamp>2017-11-22T14:13:42.126+03:00</NS1:TimeStamp>\n" +
            "                        <NS1:RoutingRole>ROUTE</NS1:RoutingRole>\n" +
            "                        <NS1:Comment>Routing by cache settings</NS1:Comment>\n" +
            "                    </NS1:Step>\n" +
            "                    <NS1:Step>\n" +
            "                        <NS1:Application.Module>SACC.MasterAccountPositioningBatchQuery.ABSResponseHandler\n" +
            "                        </NS1:Application.Module>\n" +
            "                        <NS1:VersionId>v5</NS1:VersionId>\n" +
            "                        <NS1:TimeStamp>2017-11-22T14:13:42.190+03:00</NS1:TimeStamp>\n" +
            "                        <NS1:RoutingRole>ROUTE SUCCESS</NS1:RoutingRole>\n" +
            "                        <NS1:Comment>The responses received from ECHO,FCC</NS1:Comment>\n" +
            "                    </NS1:Step>\n" +
            "                    <NS1:Step>\n" +
            "                        <NS1:Application.Module>SACC.MasterAccountPositioningBatchQuery.ABSResponseHandler\n" +
            "                        </NS1:Application.Module>\n" +
            "                        <NS1:VersionId>v5</NS1:VersionId>\n" +
            "                        <NS1:TimeStamp>2017-11-22T14:13:42.194+03:00</NS1:TimeStamp>\n" +
            "                        <NS1:RoutingRole>SUCCESS</NS1:RoutingRole>\n" +
            "                        <NS1:Comment>Finish of message processing</NS1:Comment>\n" +
            "                    </NS1:Step>\n" +
            "                    <NS1:Step>\n" +
            "                        <NS1:Application.Module>SACC.MasterAccountPositioningBatchQuery.ResponseHandler\n" +
            "                        </NS1:Application.Module>\n" +
            "                        <NS1:VersionId>v5</NS1:VersionId>\n" +
            "                        <NS1:TimeStamp>2017-11-22T14:13:42.204+03:00</NS1:TimeStamp>\n" +
            "                        <NS1:RoutingRole>SUCCESS</NS1:RoutingRole>\n" +
            "                        <NS1:Comment>Finish of message processing</NS1:Comment>\n" +
            "                    </NS1:Step>\n" +
            "                    <NS1:Step>\n" +
            "                        <NS1:Application.Module>SACC.AccountListQuery.ABSRouter</NS1:Application.Module>\n" +
            "                        <NS1:VersionId>v5</NS1:VersionId>\n" +
            "                        <NS1:TimeStamp>2017-11-22T14:13:42.214+03:00</NS1:TimeStamp>\n" +
            "                        <NS1:RoutingRole>ROUTE</NS1:RoutingRole>\n" +
            "                        <NS1:Comment>Routing by cache settings</NS1:Comment>\n" +
            "                    </NS1:Step>\n" +
            "                    <NS1:Step>\n" +
            "                        <NS1:Application.Module>SACC.AccountListQuery.ResponseHandler</NS1:Application.Module>\n" +
            "                        <NS1:VersionId>5</NS1:VersionId>\n" +
            "                        <NS1:TimeStamp>2017-11-22T14:13:42.341+03:00</NS1:TimeStamp>\n" +
            "                        <NS1:RoutingRole>ROUTE SUCCESS</NS1:RoutingRole>\n" +
            "                        <NS1:Comment>The responses received from ECHO,FCC</NS1:Comment>\n" +
            "                    </NS1:Step>\n" +
            "                    <NS1:Step>\n" +
            "                        <NS1:Application.Module>SACC.AccountListQuery.ResponseHandler</NS1:Application.Module>\n" +
            "                        <NS1:VersionId>v5</NS1:VersionId>\n" +
            "                        <NS1:TimeStamp>2017-11-22T14:13:42.346+03:00</NS1:TimeStamp>\n" +
            "                        <NS1:RoutingRole>SUCCESS</NS1:RoutingRole>\n" +
            "                        <NS1:Comment>Finish of message processing</NS1:Comment>\n" +
            "                    </NS1:Step>\n" +
            "                    <NS1:Step>\n" +
            "                        <NS1:Application.Module>SACC.v5.AcDeNo.PublishHandler</NS1:Application.Module>\n" +
            "                        <NS1:VersionId>v5</NS1:VersionId>\n" +
            "                        <NS1:TimeStamp>2017-11-22T14:13:42.359+03:00</NS1:TimeStamp>\n" +
            "                        <NS1:RoutingRole>SUCCESS</NS1:RoutingRole>\n" +
            "                        <NS1:Comment>Finish of message processing</NS1:Comment>\n" +
            "                    </NS1:Step>\n" +
            "                </NS1:MessagePath>\n" +
            "            </NS1:Audit>\n" +
            "        </NS1:UCBRUHeaders>\n" +
            "    </soapenv:Header>\n" +
            "    <soapenv:Body>\n" +
            "        <acc:AccountList>\n" +
            "            <acc:AccountDetails>\n" +
            "                <acc:AccountNo>02372591RURPRDC101</acc:AccountNo>\n" +
            "                <acc:Branch>M03</acc:Branch>\n" +
            "                <acc:CBAccountNo>40817810050450101811</acc:CBAccountNo>\n" +
            "                <acc:Ccy>RUR</acc:Ccy>\n" +
            "                <acc:CcyDigital>810</acc:CcyDigital>\n" +
            "                <acc:Status>C</acc:Status>\n" +
            "                <acc:CustomerNo>02372591</acc:CustomerNo>\n" +
            "                <acc:Special>PRDC1</acc:Special>\n" +
            "                <acc:OpenDate>2017-02-15</acc:OpenDate>\n" +
            "                <acc:AltAccountNo>02372591RURPRDC101</acc:AltAccountNo>\n" +
            "                <acc:Type>S</acc:Type>\n" +
            "                <acc:ATMAvailable>Y</acc:ATMAvailable>\n" +
            "                <acc:IsDormant>N</acc:IsDormant>\n" +
            "                <acc:PostAllowed>N</acc:PostAllowed>\n" +
            "                <acc:CreditTransAllowed>Y</acc:CreditTransAllowed>\n" +
            "                <acc:DebitTransAllowed>Y</acc:DebitTransAllowed>\n" +
            "                <acc:ClearingBank>042202799</acc:ClearingBank>\n" +
            "                <acc:CorBank>042202799</acc:CorBank>\n" +
            "                <acc:Positioning>\n" +
            "                    <acc:CBAccount>40817810050450101811</acc:CBAccount>\n" +
            "                    <acc:IMBAccountNo>40817810050450101811</acc:IMBAccountNo>\n" +
            "                    <acc:IMBBranch>M03</acc:IMBBranch>\n" +
            "                    <acc:HostABSAccountNo>02372591RURPRDC101</acc:HostABSAccountNo>\n" +
            "                    <acc:HostABSBranch>M03</acc:HostABSBranch>\n" +
            "                    <acc:HostABS>FCC</acc:HostABS>\n" +
            "                </acc:Positioning>\n" +
            "                <acc:MIS>\n" +
            "                    <acc:GroupComponent>R_P_MC</acc:GroupComponent>\n" +
            "                    <acc:TransactionClass>\n" +
            "                        <acc:Name>TXNMIS1</acc:Name>\n" +
            "                    </acc:TransactionClass>\n" +
            "                    <acc:TransactionClass>\n" +
            "                        <acc:Name>TXNMIS2</acc:Name>\n" +
            "                    </acc:TransactionClass>\n" +
            "                    <acc:TransactionClass>\n" +
            "                        <acc:Name>TXNMIS3</acc:Name>\n" +
            "                    </acc:TransactionClass>\n" +
            "                    <acc:TransactionClass>\n" +
            "                        <acc:Name>TXNMIS4</acc:Name>\n" +
            "                    </acc:TransactionClass>\n" +
            "                    <acc:TransactionClass>\n" +
            "                        <acc:Name>TXNMIS5</acc:Name>\n" +
            "                    </acc:TransactionClass>\n" +
            "                    <acc:TransactionClass>\n" +
            "                        <acc:Name>TXNMIS6</acc:Name>\n" +
            "                    </acc:TransactionClass>\n" +
            "                    <acc:TransactionClass>\n" +
            "                        <acc:Name>TXNMIS7</acc:Name>\n" +
            "                    </acc:TransactionClass>\n" +
            "                    <acc:TransactionClass>\n" +
            "                        <acc:Name>TXNMIS8</acc:Name>\n" +
            "                    </acc:TransactionClass>\n" +
            "                    <acc:TransactionClass>\n" +
            "                        <acc:Name>TXNMIS9</acc:Name>\n" +
            "                    </acc:TransactionClass>\n" +
            "                    <acc:TransactionClass>\n" +
            "                        <acc:Name>TXNMIS10</acc:Name>\n" +
            "                    </acc:TransactionClass>\n" +
            "                    <acc:CompositeClass>\n" +
            "                        <acc:Name>COMPMIS1</acc:Name>\n" +
            "                    </acc:CompositeClass>\n" +
            "                    <acc:CompositeClass>\n" +
            "                        <acc:Name>COMPMIS2</acc:Name>\n" +
            "                    </acc:CompositeClass>\n" +
            "                    <acc:CompositeClass>\n" +
            "                        <acc:Name>COMPMIS3</acc:Name>\n" +
            "                    </acc:CompositeClass>\n" +
            "                    <acc:CompositeClass>\n" +
            "                        <acc:Name>COMPMIS4</acc:Name>\n" +
            "                    </acc:CompositeClass>\n" +
            "                    <acc:CompositeClass>\n" +
            "                        <acc:Name>COMPMIS5</acc:Name>\n" +
            "                    </acc:CompositeClass>\n" +
            "                    <acc:CompositeClass>\n" +
            "                        <acc:Name>COMPMIS6</acc:Name>\n" +
            "                    </acc:CompositeClass>\n" +
            "                    <acc:CompositeClass>\n" +
            "                        <acc:Name>COMPMIS7</acc:Name>\n" +
            "                    </acc:CompositeClass>\n" +
            "                    <acc:CompositeClass>\n" +
            "                        <acc:Name>COMPMIS8</acc:Name>\n" +
            "                    </acc:CompositeClass>\n" +
            "                    <acc:CompositeClass>\n" +
            "                        <acc:Name>COMPMIS9</acc:Name>\n" +
            "                    </acc:CompositeClass>\n" +
            "                    <acc:CompositeClass>\n" +
            "                        <acc:Name>COMPMIS10</acc:Name>\n" +
            "                    </acc:CompositeClass>\n" +
            "                    <acc:CostCode>\n" +
            "                        <acc:Name>COSTCOD1</acc:Name>\n" +
            "                    </acc:CostCode>\n" +
            "                    <acc:CostCode>\n" +
            "                        <acc:Name>COSTCOD2</acc:Name>\n" +
            "                    </acc:CostCode>\n" +
            "                    <acc:CostCode>\n" +
            "                        <acc:Name>COSTCOD3</acc:Name>\n" +
            "                    </acc:CostCode>\n" +
            "                    <acc:CostCode>\n" +
            "                        <acc:Name>COSTCOD4</acc:Name>\n" +
            "                    </acc:CostCode>\n" +
            "                    <acc:CostCode>\n" +
            "                        <acc:Name>COSTCOD5</acc:Name>\n" +
            "                    </acc:CostCode>\n" +
            "                </acc:MIS>\n" +
            "                <acc:CloseDate>2017-11-22</acc:CloseDate>\n" +
            "                <acc:OpenBalance>0.00</acc:OpenBalance>\n" +
            "                <acc:CurrentBalance>0.00</acc:CurrentBalance>\n" +
            "                <acc:GWSAccType>CURR</acc:GWSAccType>\n" +
            "                <acc:OperationTypeCodes>VIEW=0,DOMPAY=1,CUPADE=0,CUPACO=0,</acc:OperationTypeCodes>\n" +
            "            </acc:AccountDetails>\n" +
            "        </acc:AccountList>\n" +
            "    </soapenv:Body>\n" +
            "</soapenv:Envelope>";
*/

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

    private long getJournalMaxId() throws SQLException {
        DataRecord res = baseEntityRepository.selectFirst("select max(MESSAGE_ID) from GL_ACDENO");
        return null == res.getLong(0) ? 0 : res.getLong(0);
    }

    private AcDNJournal getJournalNewRecord(long idFrom) throws SQLException {
        return (AcDNJournal) baseEntityRepository.selectFirst(AcDNJournal.class, "from AcDNJournal j where j.id > ?1", idFrom);
    }

    private AuditRecord getAuditError(long idFrom ) throws SQLException {
        return (AuditRecord) baseEntityRepository.selectFirst(AuditRecord.class,
                "from AuditRecord a where a.logCode in ('AccountDetailsNotify', 'QueueProcessor') and a.logLevel <> 'Info' and a.id > ?1 ", idFrom);
    }

}
