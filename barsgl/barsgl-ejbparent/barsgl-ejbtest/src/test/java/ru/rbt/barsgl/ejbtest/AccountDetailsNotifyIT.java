package ru.rbt.barsgl.ejbtest;

import com.ibm.mq.jms.*;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.controller.operday.task.AccountDetailsNotifyTask;
import ru.rbt.barsgl.ejb.controller.operday.task.AccountDetailsNotifyTaskOld;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.*;
import ru.rbt.barsgl.ejb.entity.acc.AcDNJournal;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbtest.mq.MqUtil;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.ejbcore.datarec.DataRecord;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Logger;

import static junit.framework.TestCase.assertTrue;
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Status.ERROR;
import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Status.PROCESSED;
import static ru.rbt.ejbcore.util.DateUtils.getDatabaseDate;
import static ru.rbt.ejbcore.util.DateUtils.getFinalDateStr;

/**
 * Created by ER22228
 */
public class AccountDetailsNotifyIT extends AbstractQueueIT {

    public static final Logger logger = Logger.getLogger(AccountDetailsNotifyIT.class.getName());

    private static final String fakeCustomer = "00000010";

//    private final static String HOST_NAME = "vs529";
//    private final static String MBROKER = "QM_MBROKER4_T5";

    public static final String HOST_NAME = "mbrk4-inta.testhpcsa.imb.ru"; // "vs11205";
    public static final String MBROKER = "QM_MBROKER4";

    private final static String CHANNEL= "SYSTEM.DEF.SVRCONN";
    public static final String USERNAME = "srvwbl4mqtest";   //"er22228";   "@ntd1";//
    public static final String PASSWORD = "UsATi8hU";   //"Vugluskr4";
    private final static String ACDENO6  = "UCBRU.ADP.BARSGL.V5.ACDENO.FCC.NOTIF";
    private final static String ACDENO12 = "UCBRU.ADP.BARSGL.V5.ACDENO.FCC12.NOTIF";
    private final static String ACDENOM = "UCBRU.ADP.BARSGL.V4.ACDENO.MDSOPEN.NOTIF";

//    public static final String HOST_NAME = "localhost";
//    public static final String USERNAME = "";
//    public static final String PASSWORD = "";

    @Test
    public void testNotifyClose12() throws Exception {

        baseEntityRepository.executeNativeUpdate("delete from GL_ACC where bsaacid='40702810400154748352'");

        String message = getResourceText("/AccountDetailsNotifyFcc12Close.xml");

        MQQueueConnectionFactory cf = MqUtil.getConnectionFactory(HOST_NAME, MBROKER, CHANNEL);
        MqUtil.sendToQueue(cf, ACDENO12, message.getBytes(), null, USERNAME, PASSWORD);

        SingleActionJob job =
                SingleActionJobBuilder.create()
                        .withClass(AccountDetailsNotifyTask.class)
                        .withProps(
//                     queue|topic
                                "mq.type = queue\n" +
                                        "mq.algo = simple\n" +
                                        "mq.host = " + HOST_NAME + "\n" +
                                        "mq.port = 1414\n" +
                                        "mq.queueManager = " + MBROKER + "\n" +
                                        "mq.channel = " + CHANNEL + "\n" +
                                        "mq.batchSize = 7\n" +
                                        "mq.topics = FC12:" + ACDENO12 + "\n" +
                                        "mq.user=" + USERNAME + "\n" +
                                        "mq.password=" + PASSWORD + ""
                        )
                        .build();
        jobService.executeJob(job);
    }

    @Test
    public void testNotifyOpen6() throws Exception {

        baseEntityRepository.executeNativeUpdate("delete from gl_acc where bsaacid='40702810400154748352'");

        String message = getResourceText("/AccountDetailsNotifyFcc6Full.xml");

        MQQueueConnectionFactory cf = MqUtil.getConnectionFactory(HOST_NAME, MBROKER, CHANNEL);
        MqUtil.sendToQueue(cf, ACDENO6, message.getBytes(), null, USERNAME, PASSWORD);

        SingleActionJob job =
                SingleActionJobBuilder.create()
                        .withClass(AccountDetailsNotifyTask.class)
                        .withProps(
                                "mq.type = queue\n" +
                                        "mq.algo = simple\n" +
                                        "mq.host = " + HOST_NAME + "\n" +
                                        "mq.port = 1414\n" +
                                        "mq.queueManager = " + MBROKER + "\n" +
                                        "mq.channel = SYSTEM.DEF.SVRCONN\n" +
                                        "mq.batchSize = 7\n" +
                                        "mq.topics = FCC:" + ACDENO6 + "\n" +
                                        "mq.user=" + USERNAME + "\n" +
                                        "mq.password=" + PASSWORD + ""
                        )
                        .build();
        jobService.executeJob(job);


    }


    /**
     * Не понимаю смысл - без клиента счет не откроется, а для этого счета есть клиент
     * @throws Exception
     */
    @Test
    public void testFCCnoCustomer() throws Exception {
        String bsaacid = "40817810250300081806";
        baseEntityRepository.executeNativeUpdate("delete from gl_acc where bsaacid=?", bsaacid);
        baseEntityRepository.executeNativeUpdate("delete from sdcustpd where bbcust=?", fakeCustomer);

        long idAcdeno = getAcdenoMaxId();
        String message = getResourceText("/AccountDetailsNotifyFcc6NoCust.xml");
        Long jId = remoteAccess.invoke(AccountDetailsNotifyController.class, "createJournalEntry", AcDNJournal.Sources.FCC.name(), message);

        remoteAccess.invoke(AccountDetailsNotifyProcessor.class, "process", AcDNJournal.Sources.FCC, message, jId);

        AcDNJournal journal = getAcdenoNewRecord(idAcdeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(PROCESSED, journal.getStatus());

        Assert.assertNotNull(baseEntityRepository.selectFirst("select * from gl_acc where bsaacid=?", bsaacid));
    }

    @Test
    public void testFCCShadow() throws Exception {
        String bsaacid = "40817840250010046747";

        baseEntityRepository.executeNativeUpdate("delete from gl_acc where bsaacid=?", bsaacid);

        long idAcdeno = getAcdenoMaxId();
        String message = getResourceText("/AccountDetailsNotifyFcc6Shadow.xml");
        Long jId = remoteAccess.invoke(AccountDetailsNotifyController.class, "createJournalEntry", AcDNJournal.Sources.FCC.name(), message);

        remoteAccess.invoke(AccountDetailsNotifyProcessor.class, "process", AcDNJournal.Sources.FCC, message, jId);

        AcDNJournal journal = getAcdenoNewRecord(idAcdeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(ERROR, journal.getStatus());

        Assert.assertNull(baseEntityRepository.selectFirst("select * from gl_acc where bsaacid=?", bsaacid));

    }

    @Test
    public void testFccOpen() throws Exception {
        baseEntityRepository.executeNativeUpdate("delete from gl_acc where bsaacid='40817810000010696538'");

        long idAcdeno = getAcdenoMaxId();
        String message = getResourceText("/AccountDetailsNotifyFcc6Open.xml"); // AccountDetailsNotifyProcessorOld.messageFCC;
        Long jId = remoteAccess.invoke(AccountDetailsNotifyController.class, "createJournalEntry", AcDNJournal.Sources.FCC.name(), message);

        remoteAccess.invoke(AccountDetailsNotifyProcessor.class, "process", AcDNJournal.Sources.FCC, message, jId);

        AcDNJournal journal = getAcdenoNewRecord(idAcdeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(PROCESSED, journal.getStatus());

        assertTrue(null != baseEntityRepository.selectFirst("select * from gl_acc where bsaacid=?", "40817810000010696538"));
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

        long idAcdeno = getAcdenoMaxId();
        String message = getResourceText("/AccountDetailsNotifyFcc12Close.xml");
        Long jId = remoteAccess.invoke(AccountDetailsNotifyController.class, "createJournalEntry", AcDNJournal.Sources.FCC_CLOSE.name(), message);

        remoteAccess.invoke(AccountDetailsNotifyProcessor.class, "process", AcDNJournal.Sources.FCC_CLOSE, message, jId);

        AcDNJournal journal = getAcdenoNewRecord(idAcdeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(PROCESSED, journal.getStatus());

        checkCloseDate(bsaacid, getDatabaseDate().parse(closeDateStr), true);
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

        long idAcdeno = getAcdenoMaxId();
        String message = getResourceText("/AccountDetailsNotifyFcc6Close.xml");
        Long jId = remoteAccess.invoke(AccountDetailsNotifyController.class, "createJournalEntry", AcDNJournal.Sources.FCC.name(), message);

        remoteAccess.invoke(AccountDetailsNotifyProcessor.class, "process", AcDNJournal.Sources.FCC, message, jId);

        AcDNJournal journal = getAcdenoNewRecord(idAcdeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(PROCESSED, journal.getStatus());

        checkCloseDate(bsaacid, getDatabaseDate().parse(closeDateStr), false);
    }

    @Test
    public void testFC12EqualBranch() throws Exception {
        String bsaacid = "40802810900014820908";
        DataRecord rec = baseEntityRepository.selectFirst("SELECT branch from gl_acc where bsaacid=?", bsaacid);
        String oldBranch = rec.getString(0);
        String newBranch = "053";
        baseEntityRepository.executeNativeUpdate("UPDATE GL_ACC SET branch=? WHERE BSAACID=?", newBranch, bsaacid);

        long idAcdeno = getAcdenoMaxId();
        String message = getResourceText("/AccountDetailsNotifyFcc12Branch.xml");
        Long jId = remoteAccess.invoke(AccountDetailsNotifyController.class, "createJournalEntry", AcDNJournal.Sources.FCC.name(), message);

        remoteAccess.invoke(AccountDetailsNotifyProcessor.class, "process", AcDNJournal.Sources.FC12, message, jId);

        AcDNJournal journal = getAcdenoNewRecord(idAcdeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(ERROR, journal.getStatus());

        try {
            Assert.assertNotNull(baseEntityRepository.selectFirst("SELECT branch from gl_acc where bsaacid=? and branch=?", bsaacid, newBranch));
        } finally {
            baseEntityRepository.executeNativeUpdate("UPDATE GL_ACC SET branch=? WHERE BSAACID=?", oldBranch, bsaacid );
        }

    }

    @Test
    public void testFC12NoEqualBranch() throws Exception {
        String bsaacid = "40802810900014820908";
        DataRecord rec = baseEntityRepository.selectFirst("SELECT branch from gl_acc where bsaacid=?", bsaacid);
        String oldBranch = rec.getString(0);
        String errBranch = "054";
        String newBranch = "053";
        baseEntityRepository.executeNativeUpdate("UPDATE GL_ACC SET branch=? WHERE BSAACID=?", errBranch, bsaacid);

        long idAcdeno = getAcdenoMaxId();
        String message = getResourceText("/AccountDetailsNotifyFcc12Branch.xml");
        Long jId = remoteAccess.invoke(AccountDetailsNotifyController.class, "createJournalEntry", AcDNJournal.Sources.FCC.name(), message);

        remoteAccess.invoke(AccountDetailsNotifyProcessor.class, "process", AcDNJournal.Sources.FC12, message, jId);

        AcDNJournal journal = getAcdenoNewRecord(idAcdeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(PROCESSED, journal.getStatus());

        try {
            Assert.assertNotNull(baseEntityRepository.selectFirst("SELECT branch from gl_acc where bsaacid=? and branch=?", bsaacid, newBranch));
        } finally {
            baseEntityRepository.executeNativeUpdate("UPDATE GL_ACC SET branch=? WHERE BSAACID=?", oldBranch, bsaacid );
        }
    }

    private void reopenAccount(String bsaacid) {
        baseEntityRepository.executeNativeUpdate("update GL_ACC set DTC = null where BSAACID = ?", bsaacid);
    }

    private void checkCloseDate(String bsaacid, Date closeDate, boolean withGL) throws SQLException {
        Assert.assertNotNull(baseEntityRepository.selectFirst("SELECT * from GL_ACC where BSAACID=? and DTC = ?", bsaacid, closeDate));

    }

    public String getResourceText(String resource) throws IOException {
        File inFile = new File(this.getClass().getResource(resource).getFile());
        return FileUtils.readFileToString(inFile, AccountDetailsNotifyProcessor.charsetName);
    }

    private long getAcdenoMaxId() throws SQLException {
        DataRecord res = baseEntityRepository.selectFirst("select max(MESSAGE_ID) from GL_ACDENO");
        return null == res.getLong(0) ? 0 : res.getLong(0);
    }

    private AcDNJournal getAcdenoNewRecord(long idFrom) throws SQLException {
        return (AcDNJournal) baseEntityRepository.selectFirst(AcDNJournal.class, "from AcDNJournal j where j.id > ?1", idFrom);
    }
}
