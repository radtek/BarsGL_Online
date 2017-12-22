package ru.rbt.barsgl.ejbtest;

import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.controller.operday.task.CustomerDetailsNotifyTask;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.CustomerNotifyQueueController;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.QueueProperties;
import ru.rbt.barsgl.ejb.entity.cust.CustDNInput;
import ru.rbt.barsgl.ejb.entity.cust.CustDNJournal;
import ru.rbt.barsgl.ejb.entity.cust.CustDNMapped;
import ru.rbt.barsgl.ejb.entity.cust.Customer;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.StringUtils;

import javax.jms.JMSException;
import java.io.File;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.Properties;

import static ru.rbt.barsgl.ejb.entity.cust.CustDNJournal.Status.*;
import static ru.rbt.barsgl.ejb.entity.cust.CustDNMapped.CustResult.INSERT;
import static ru.rbt.barsgl.ejb.entity.cust.CustDNMapped.CustResult.NOCHANGE;
import static ru.rbt.barsgl.ejb.entity.cust.CustDNMapped.CustResult.UPDATE;
import static ru.rbt.barsgl.ejb.entity.cust.Customer.Resident.N;
import static ru.rbt.barsgl.ejb.entity.cust.Customer.Resident.R;
import static ru.rbt.barsgl.ejb.props.PropertyName.CUST_LOAD_ONLINE;
import static ru.rbt.barsgl.ejbtest.AccountQueryMPIT.getConnectionFactory;
import static ru.rbt.barsgl.ejbtest.AccountQueryMPIT.getQueueProperty;
import static ru.rbt.barsgl.ejbtest.AccountQueryMPIT.sendToQueue;

/**
 * Created by er18837 on 14.12.2017.
 */
public class CustomerDetailsNotifyIT extends AbstractTimerJobIT {

//    private final String host = "vs569";
//    private final String broker = "QM_MBROKER4_T4";

    private final static String host = "vs338";
    private final static String broker = "QM_MBROKER10_TEST";
    private final static String channel= "SYSTEM.DEF.SVRCONN";
    private final static String cudenoIn = "UCBRU.ADP.BARSGL.V3.CUDENO.NOTIF";
//    private final static String inQueue = "UCBRU.ADP.BARSGL.ACLIQU.REQUEST";
//    private final static String outQueue = "UCBRU.ADP.BARSGL.ACLIQU.RESPONSE";
    private static final String login = "srvwbl4mqtest";
    private static final String passw = "UsATi8hU";

    private static final String qType = "CUST";

    private static final String fakeCustomer = "00000010";

//    @BeforeClass
    public static void before() {
        deletePropertyOnline();
    }

    private String getQProperty(String topic, String ahost, String abroker, String alogin, String apassw) {
        return getQueueProperty (topic, cudenoIn, null, ahost, "1414", abroker, channel, alogin, apassw, "30");
    }

    /**
     * Тест определения параметров очереди из настроек задачи
     * @throws Exception
     */
    @Test
    public void testReadProperties() throws Exception {
        testProperties(qType, host, "1414", broker, login, passw, "30");
        testProperties(qType, host, " 1414 ", broker, login, passw, "30");
        testProperties(qType, host, "1414", "", login, passw, "-1");
        testProperties(qType, "", "1414", broker, login, passw, "-1");
        testProperties(qType, host, "1414a", broker, login, passw, "0");
        testProperties("mq.batchSize = 30\n"
                + "mq.host = " + host + "\n"
                + "mq.port = 1414\n"
//                + "mq.queueManager = " + broker + "\n"
//                + "mq.channel = SYSTEM.DEF.SVRCONN\n"
                + "mq.topics = " + qType + ":" + cudenoIn + "\n"   // + ":" + outQueue
                + "mq.user=" + login + "\n"
                + "mq.password=" + passw +"\n");
    }

/*
*/

    private void testProperties(String topic, String ahost, String aport, String abroker, String alogin, String apassw, String batch) throws Exception {
        testProperties(getQueueProperty (topic, cudenoIn, null, ahost, aport, abroker, channel, login, passw, batch));
    }

    private void testProperties(String propStr) throws Exception {
        System.out.print(propStr);
        Properties properties = new Properties();
        properties.load(new StringReader(propStr));
        try {
            QueueProperties queueProperties = new QueueProperties(properties);
            System.out.println(queueProperties.toString());
        } catch (Exception e) {
        }
        System.out.println();
    }

    /**
     * Тест получения сообщения из очереди
     * @throws Exception
     */
    @Test
    public void testReadQueue() throws Exception {
        long idAudit = getAuditMaxId();
        remoteAccess.invoke(CustomerNotifyQueueController.class, "closeConnection");

        SingleActionJob job =
                SingleActionJobBuilder.create()
                        .withClass(CustomerDetailsNotifyTask.class)
                        .withName("CustomerNotify1")
                        .withProps(getQProperty(qType, host, broker, login, passw))
                        .build();
        jobService.executeJob(job);

        Thread.sleep(2000L);
        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));
    }

    /**
     * Тест обработки сообщения из очереди
     * @throws Exception
     */
    @Test
    public void testLoadParams() throws Exception {
        deletePropertyOnline();

        long idAudit = getAuditMaxId();
        long idCudeno = getCudenoMaxId();
        remoteAccess.invoke(CustomerNotifyQueueController.class, "closeConnection");

        MQQueueConnectionFactory cf = getConnectionFactory(host, broker, channel);

        sendToQueue(cf, cudenoIn,
                new File(this.getClass().getResource("/CustomerDetailsTest_B.xml").getFile()),
                null, login, passw);

        SingleActionJob job =
                SingleActionJobBuilder.create()
                        .withClass(CustomerDetailsNotifyTask.class)
                        .withName("CustomerNotify2")
                        .withProps(getQProperty(qType, host, broker, login, passw))
                        .build();
        jobService.executeJob(job);

        Thread.sleep(2000L);
        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));

        CustDNJournal journal = getCudenoNewRecord(idCudeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_CUDENO1", journal);
        Assert.assertEquals(PROCESSED, journal.getStatus());

        Assert.assertNotNull("Нет записи в таблице GL_CUDENO2", getCudenoInput(journal.getId()));
        Assert.assertNotNull("Нет записи в таблице GL_CUDENO3", getCudenoMapped(journal.getId()));
    }

    /**
     * Тест обработки сообщения по клиенту-физ.лицу (пропустить)
     * @throws Exception
     */
    @Test
    public void testSkipCustomer() throws Exception {

        long idAudit = getAuditMaxId();
        long idCudeno = getCudenoMaxId();

        String textMessage = FileUtils.readFileToString(new File(this.getClass().getResource("/CustomerDetailsTest_I.xml").getFile()), "UTF-8");
        // 00488888

        // Long processing(String queueType, String[] incMessage, String toQueue, long receiveTime, long waitingTime) throws Exception {
        remoteAccess.invoke(CustomerNotifyQueueController.class, "processingWithLog", qType, new String[] {textMessage}, null, -1, -1);

        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));

        CustDNJournal journal = getCudenoNewRecord(idCudeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_CUDENO1", journal);
        Assert.assertEquals(SKIPPED, journal.getStatus());

    }

    /**
     * Тест обработки сообщения без изменения клиента
     * @throws Exception
     */
    @Test
    public void testNoChangeCustomer() throws Exception {

        long idAudit = getAuditMaxId();
        long idCudeno = getCudenoMaxId();

        String textMessage = FileUtils.readFileToString(new File(this.getClass().getResource("/CustomerDetailsTest_C.xml").getFile()), "UTF-8");
        // 00694379 A35	12 : 064 18

        remoteAccess.invoke(CustomerNotifyQueueController.class, "processingWithLog", qType, new String[] {textMessage}, null, -1, -1);

        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));

        CustDNJournal journal = getCudenoNewRecord(idCudeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_CUDENO1", journal);
        Assert.assertEquals(PROCESSED, journal.getStatus());
        Assert.assertEquals(NOCHANGE.name(), journal.getComment());

        Assert.assertNotNull("Нет записи в таблице GL_CUDENO2", getCudenoInput(journal.getId()));
        CustDNMapped mapped = getCudenoMapped(journal.getId());
        Assert.assertNotNull("Нет записи в таблице GL_CUDENO3", mapped);
        Assert.assertEquals(NOCHANGE, mapped.getResult());
    }

    /**
     * Тест обработки сообщения с изменением клиента
     * @throws Exception
     */
    @Test
    public void testUpdateCustomer() throws Exception {
        setPropertyOnline(true);   // режим Online

        long idAudit = getAuditMaxId();
        long idCudeno = getCudenoMaxId();

        String textMessage = FileUtils.readFileToString(new File(this.getClass().getResource("/CustomerDetailsTest_C.xml").getFile()), "UTF-8");
        // 00694379 A35	12 : 064 18

        updateCustomer("00694379", "001", "11", N);
        remoteAccess.invoke(CustomerNotifyQueueController.class, "processingWithLog", qType, new String[] {textMessage}, null, -1, -1);

        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));

        CustDNJournal journal = getCudenoNewRecord(idCudeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_CUDENO1", journal);
        Assert.assertEquals(PROCESSED, journal.getStatus());
        Assert.assertEquals(UPDATE.name(), journal.getComment());

        Assert.assertNotNull("Нет записи в таблице GL_CUDENO2", getCudenoInput(journal.getId()));
        CustDNMapped mapped = getCudenoMapped(journal.getId());
        Assert.assertNotNull("Нет записи в таблице GL_CUDENO3", mapped);
        Assert.assertEquals(UPDATE, mapped.getResult());

        checkCustomer("00694379", "064", "18", R);
    }

    /**
     * Тест обработки сообщения с созданием клиента
     * @throws Exception
     */
    @Test
    public void testCreateCustomer() throws Exception {
        setPropertyOnline(true);   // режим Online

        long idAudit = getAuditMaxId();
        long idCudeno = getCudenoMaxId();

        String textMessage = FileUtils.readFileToString(new File(this.getClass().getResource("/CustomerDetailsTest_insert.xml").getFile()), "UTF-8");
        // 00000010 A35	12 : 064 18

        deleteCustomer(fakeCustomer);
        remoteAccess.invoke(CustomerNotifyQueueController.class, "processingWithLog", qType, new String[] {textMessage}, null, -1, -1);

        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));

        CustDNJournal journal = getCudenoNewRecord(idCudeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_CUDENO1", journal);
        Assert.assertEquals(PROCESSED, journal.getStatus());
        Assert.assertEquals(INSERT.name(), journal.getComment());

        Assert.assertNotNull("Нет записи в таблице GL_CUDENO2", getCudenoInput(journal.getId()));
        CustDNMapped mapped = getCudenoMapped(journal.getId());
        Assert.assertNotNull("Нет записи в таблице GL_CUDENO3", mapped);
        Assert.assertEquals(INSERT, mapped.getResult());

        checkCustomer(fakeCustomer, "064", "18", R);
    }

    /**
     * Тест обработки сообщения без изменения клиента
     * @throws Exception
     */
    @Test
    public void testEmulateCreateCustomer() throws Exception {

        setPropertyOnline(false);   // режим эмуляции

        long idAudit = getAuditMaxId();
        long idCudeno = getCudenoMaxId();

        String textMessage = FileUtils.readFileToString(new File(this.getClass().getResource("/CustomerDetailsTest_insert.xml").getFile()), "UTF-8");
        deleteCustomer(fakeCustomer);
        remoteAccess.invoke(CustomerNotifyQueueController.class, "processingWithLog", qType, new String[] {textMessage}, null, -1, -1);

        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));

        CustDNJournal journal = getCudenoNewRecord(idCudeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_CUDENO1", journal);
        Assert.assertEquals(EMULATED, journal.getStatus());
        Assert.assertEquals(INSERT.name(), journal.getComment());

        Assert.assertNotNull("Нет записи в таблице GL_CUDENO2", getCudenoInput(journal.getId()));
        CustDNMapped mapped = getCudenoMapped(journal.getId());
        Assert.assertNotNull("Нет записи в таблице GL_CUDENO3", mapped);
        Assert.assertEquals(INSERT, mapped.getResult());

        Assert.assertNull(getCustomer(fakeCustomer));
        setPropertyOnline(true);   // режим эмуляции
    }

    /**
     * Тест обработки сообщения без изменения клиента
     * @throws Exception
     */
    @Test
    public void testEmulateUpdateCustomer() throws Exception {

        setPropertyOnline(false);   // режим эмуляции

        long idAudit = getAuditMaxId();
        long idCudeno = getCudenoMaxId();

        String textMessage = FileUtils.readFileToString(new File(this.getClass().getResource("/CustomerDetailsTest_C.xml").getFile()), "UTF-8");
        updateCustomer("00694379", "001", "11", N);
        remoteAccess.invoke(CustomerNotifyQueueController.class, "processingWithLog", qType, new String[] {textMessage}, null, -1, -1);

        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));

        CustDNJournal journal = getCudenoNewRecord(idCudeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_CUDENO1", journal);
        Assert.assertEquals(EMULATED, journal.getStatus());
        Assert.assertEquals(UPDATE.name(), journal.getComment());

        Assert.assertNotNull("Нет записи в таблице GL_CUDENO2", getCudenoInput(journal.getId()));
        CustDNMapped mapped = getCudenoMapped(journal.getId());
        Assert.assertNotNull("Нет записи в таблице GL_CUDENO3", mapped);
        Assert.assertEquals(UPDATE, mapped.getResult());

        Assert.assertNull(getCustomer(fakeCustomer));
        checkCustomer("00694379", "001", "11", N);
        updateCustomer("00694379", "064", "18", R);

        setPropertyOnline(true);   // режим эмуляции
    }

    /**
     * Тест обработки сообщения с ошибкой в формате XML
     * @throws Exception
     */
    @Test
    public void testValidationError() throws Exception {

        long idAudit = getAuditMaxId();
        long idCudeno = getCudenoMaxId();

        String textMessage = FileUtils.readFileToString(new File(this.getClass().getResource("/CustomerDetailsTest_errval.xml").getFile()), "UTF-8");

        // Long processing(String queueType, String[] incMessage, String toQueue, long receiveTime, long waitingTime) throws Exception {
        remoteAccess.invoke(CustomerNotifyQueueController.class, "processingWithLog", qType, new String[] {textMessage}, null, -1, -1);

        Thread.sleep(2000L);
        Assert.assertNotNull("Нет записи об ошибке в аудит", getAuditError(idAudit));

        CustDNJournal journal = getCudenoNewRecord(idCudeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_CUDENO1", journal);
        Assert.assertEquals(ERR_VAL, journal.getStatus());
        System.out.println(journal.getComment());
    }

    /**
     * Тест обработки сообщения с ошибкой в параметрах
     * @throws Exception
     */
    @Test
    public void testMappedError() throws Exception {

        long idAudit = getAuditMaxId();
        long idCudeno = getCudenoMaxId();

        String textMessage = FileUtils.readFileToString(new File(this.getClass().getResource("/CustomerDetailsTest_errmap.xml").getFile()), "UTF-8");

        // Long processing(String queueType, String[] incMessage, String toQueue, long receiveTime, long waitingTime) throws Exception {
        remoteAccess.invoke(CustomerNotifyQueueController.class, "processingWithLog", qType, new String[] {textMessage}, null, -1, -1);

        Thread.sleep(2000L);
        AuditRecord auditRecord = getAuditError(idAudit);
        Assert.assertNotNull("Нет записи об ошибке в аудит", auditRecord);

        CustDNJournal journal = getCudenoNewRecord(idCudeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_CUDENO1", journal);
        Assert.assertEquals(ERR_MAP, journal.getStatus());
        System.out.println(journal.getComment());

        Assert.assertNotNull("Нет записи в таблице GL_CUDENO2", getCudenoInput(journal.getId()));
    }

    private long getCudenoMaxId() throws SQLException {
        DataRecord res = baseEntityRepository.selectFirst("select max(MESSAGE_ID) from GL_CUDENO1");
        return null == res ? 0 : res.getLong(0);
    }

    private CustDNJournal getCudenoNewRecord(long idFrom) throws SQLException {
        return (CustDNJournal) baseEntityRepository.selectFirst(CustDNJournal.class, "from CustDNJournal j where j.id > ?1", idFrom);
    }

    private CustDNInput getCudenoInput(long id) throws SQLException {
        return (CustDNInput) baseEntityRepository.selectFirst(CustDNInput.class, "from CustDNInput j where j.id = ?1", id);
    }

    private CustDNMapped getCudenoMapped(long id) throws SQLException {
        return (CustDNMapped) baseEntityRepository.selectFirst(CustDNMapped.class, "from CustDNMapped j where j.id = ?1", id);
    }

    private long getAuditMaxId() throws SQLException {
        DataRecord res = baseEntityRepository.selectFirst("select max(ID_RECORD) from GL_AUDIT");
        return null == res ? 0 : res.getLong(0);
    }

    private AuditRecord getAuditError(long idFrom ) throws SQLException {
        return (AuditRecord) baseEntityRepository.selectFirst(AuditRecord.class,
                "from AuditRecord a where a.logCode in ('CustomerDetailsNotify', 'QueueProcessor') and a.logLevel <> 'Info' and a.id > ?1 ", idFrom);
    }

    private int updateCustomer(String custNo, String branch, String cbType, Customer.Resident resident) {
        return baseEntityRepository.executeUpdate("update Customer c set c.branch = ?1, c.resident = ?2, c.cbType = ?3 where c.id = ?4",
                branch, resident, cbType, custNo);
    }

    private int deleteCustomer(String custNo) {
        return baseEntityRepository.executeUpdate("delete from Customer c where c.id = ?1", custNo);
    }

    private Customer getCustomer(String custNo) {
        return (Customer) baseEntityRepository.findById(Customer.class, custNo);

    }

    private boolean checkCustomer(String custNo, String branch, String cbType, Customer.Resident resident) {
        Customer customer = getCustomer(custNo);
        Assert.assertNotNull("Не найден клиент " + custNo, customer);
        return customer.getBranch().equals(branch)
                && customer.getResident().equals(resident)
                && customer.getCbType().equals(cbType);
    }

    private void setPropertyOnline(boolean updateOn) {
        deletePropertyOnline();
        baseEntityRepository.executeNativeUpdate("insert into gl_prprp values " +
                "('customer.load.online', 'root', 'Y', 'STRING_TYPE', 'Признак загрузки клиентов по нотификации Online (Yes / No, default = Yes)', null, ?, null)",
                updateOn ? "Yes" : "No");
        remoteAccess.invoke(PropertiesRepository.class, "flushCache");
    }

    private static void deletePropertyOnline() {
        baseEntityRepository.executeNativeUpdate("delete from gl_prprp where ID_PRP = ?", CUST_LOAD_ONLINE.getName());
        remoteAccess.invoke(PropertiesRepository.class, "flushCache");
    }
}
