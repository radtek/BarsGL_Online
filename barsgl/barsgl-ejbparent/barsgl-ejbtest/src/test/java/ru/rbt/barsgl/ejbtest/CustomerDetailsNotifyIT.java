package ru.rbt.barsgl.ejbtest;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.controller.operday.task.CustomerDetailsNotifyTask;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.QueueInputMessage;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.CustomerNotifyProcessor;
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

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import static ru.rbt.barsgl.ejb.entity.cust.CustDNJournal.Status.*;
import static ru.rbt.barsgl.ejb.entity.cust.CustDNMapped.CustResult.INSERT;
import static ru.rbt.barsgl.ejb.entity.cust.CustDNMapped.CustResult.NOCHANGE;
import static ru.rbt.barsgl.ejb.entity.cust.CustDNMapped.CustResult.UPDATE;
import static ru.rbt.barsgl.ejb.entity.cust.Customer.Resident.N;
import static ru.rbt.barsgl.ejb.entity.cust.Customer.Resident.R;
import static ru.rbt.barsgl.ejb.props.PropertyName.CUST_LOAD_ONLINE;

/**
 * Created by er18837 on 14.12.2017.
 */
public class CustomerDetailsNotifyIT extends AbstractQueueIT {

    private final static String host = "vs529";
    private final static String broker = "QM_MBROKER4_T5";
    private final static String channel= "SYSTEM.DEF.SVRCONN";
    private final static String cudenoIn = "UCBRU.ADP.BARSGL.V3.CUDENO.NOTIF";
    private static final Boolean writeOut = true;
    private static final String unspents = "show";

    private static final String qType = "CUST";

    private static final String fakeCustomer = "00000010";

//    @BeforeClass
    public static void before() {
        deletePropertyOnline();
    }

    private String getJobProperty(String topic, String ahost, String abroker, String alogin, String apassw) {
        return getJobProperty (topic, cudenoIn, null, ahost, "1414", abroker, channel, alogin, apassw, "30", writeOut);
    }

    private QueueProperties getQueueProperties(String topic, String ahost, String abroker, String alogin, String apassw) {
        return getQueueProperties(topic, cudenoIn, null, ahost, 1414, abroker, channel, alogin, apassw, 30, writeOut, false);
    }

    public String getResourceText(String resource) throws IOException {
        File inFile = new File(this.getClass().getResource(resource).getFile());
        return FileUtils.readFileToString(inFile, CustomerNotifyProcessor.charsetName);
    }

    /**
     * Тест определения параметров очереди из настроек задачи
     * @throws Exception
     */
    @Test
    public void testReadProperties() throws Exception {
        testProperties(qType, host, "1414", broker, mqTestLogin, mqTestPassw, "30", false);
        testProperties(qType, host, " 1414 ", broker, mqTestLogin, mqTestPassw, "30", false);
        testProperties(qType, host, "1414", "", mqTestLogin, mqTestPassw, "-1", true);
        testProperties(qType, "", "1414", broker, mqTestLogin, mqTestPassw, "-1", true);
        testProperties(qType, host, "1414a", broker, mqTestLogin, mqTestPassw, "0", true);
        testProperties("mq.batchSize = 30\n"
                + "mq.host = " + host + "\n"
                + "mq.port = 1414\n"
//                + "mq.queueManager = " + broker + "\n"
//                + "mq.channel = SYSTEM.DEF.SVRCONN\n"
                + "mq.topics = " + qType + ":" + cudenoIn + "\n"   // + ":" + outQueue
                + "mq.user=" + mqTestLogin + "\n"
                + "mq.password=" + mqTestPassw +"\n"
                , true);
    }

    private void testProperties(String topic, String ahost, String aport, String abroker, String alogin, String apassw, String batch, boolean isError) throws Exception {
        testProperties(getJobProperty (topic, cudenoIn, null, ahost, aport, abroker, channel, mqTestLogin, mqTestPassw, batch, writeOut), isError);
    }

    /**
     * Тест подключения
     * @throws Exception
     */
    @Test
    public void testConnectToQueue() throws Exception {
        long idAudit = getAuditMaxId();
        closeConnection();

        SingleActionJob job =
                SingleActionJobBuilder.create()
                        .withClass(CustomerDetailsNotifyTask.class)
                        .withName("CustomerNotify1")
                        .withProps(getJobProperty(qType, host, broker, mqTestLogin, mqTestPassw))
                        .build();
        jobService.executeJob(job);

        Thread.sleep(2000L);
        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));
    }

    /**
     * Тест отправки сообщения в очередь
     * @throws Exception
     */
    @Test
    public void testSendQueue() throws Exception {
        long idAudit = getAuditMaxId();
        closeConnection();

        QueueProperties properties = getQueueProperties(qType, host, broker, mqTestLogin, mqTestPassw);
        startConnection(properties);
        sendToQueue(getResourceText("/CustomerDetailsTest_B.xml"), properties, null, null, cudenoIn);
        sendToQueue(getResourceText("/CustomerDetailsTest_C.xml"), properties, null, null, cudenoIn);

        Thread.sleep(2000L);
        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));
    }

    /**
     * Тест обработки сообщения из очереди
     * @throws Exception
     */
    @Test
    public void testProcessCustomer() throws Exception {
        deletePropertyOnline();

        long idAudit = getAuditMaxId();
        long idCudeno = getCudenoMaxId();

        String message = getResourceText("/CustomerDetailsTest_B.xml");

        Long jId = remoteAccess.invoke(CustomerNotifyQueueController.class, "createJournalEntry", qType, message);
        remoteAccess.invoke(CustomerNotifyProcessor.class, "process", message, jId);
        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));

        CustDNJournal journal = getCudenoNewRecord(idCudeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_CUDENO1", journal);
        Assert.assertEquals(PROCESSED, journal.getStatus());

        Assert.assertNotNull("Нет записи в таблице GL_CUDENO2", getCudenoInput(journal.getId()));
        Assert.assertNotNull("Нет записи в таблице GL_CUDENO3", getCudenoMapped(journal.getId()));
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
        closeConnection();

        QueueProperties properties = getQueueProperties(qType, host, broker, mqTestLogin, mqTestPassw);
        startConnection(properties);
        sendToQueue(getResourceText("/CustomerDetailsTest_B.xml"), properties, null, null, cudenoIn);

        SingleActionJob job =
                SingleActionJobBuilder.create()
                        .withClass(CustomerDetailsNotifyTask.class)
                        .withName("CustomerNotify2")
                        .withProps(getJobProperty(properties))
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
     * Тест обработки сообщения из очереди
     * @throws Exception
     */
    @Test
    public void testEncoding() throws Exception {
        deletePropertyOnline();

        long idAudit = getAuditMaxId();
        long idCudeno = getCudenoMaxId();
        closeConnection();

        QueueProperties properties = getQueueProperties(qType, host, broker, mqTestLogin, mqTestPassw);
        startConnection(properties);
        sendToQueue(getResourceText("/CustomerDetailsTest_Xs.xml"), properties, null, null, cudenoIn);

        SingleActionJob job =
                SingleActionJobBuilder.create()
                        .withClass(CustomerDetailsNotifyTask.class)
                        .withName("CustomerNotify2")
                        .withProps(getJobProperty(properties))
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

        String textMessage = getResourceText("/CustomerDetailsTest_I.xml");
        // 00488888

        remoteAccess.invoke(CustomerNotifyQueueController.class, "processingWithLog", qType, new QueueInputMessage(textMessage), null, -1, -1);

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

        String textMessage = getResourceText("/CustomerDetailsTest_C.xml");
        // 00694379 A35	12 : 064 18

        remoteAccess.invoke(CustomerNotifyQueueController.class, "processingWithLog", qType, new QueueInputMessage(textMessage), null, -1, -1);

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

        String textMessage = getResourceText("/CustomerDetailsTest_C.xml");
        // 00694379 A35	12 : 064 18

        updateCustomer("00694379", "001", "11", N);
        remoteAccess.invoke(CustomerNotifyQueueController.class, "processingWithLog", qType, new QueueInputMessage(textMessage), null, -1, -1);

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

        String textMessage = getResourceText("/CustomerDetailsTest_insert.xml");
        // 00000010 A35	12 : 064 18

        deleteCustomer(fakeCustomer);
        remoteAccess.invoke(CustomerNotifyQueueController.class, "processingWithLog", qType, new QueueInputMessage(textMessage), null, -1, -1);

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

        String textMessage = getResourceText("/CustomerDetailsTest_insert.xml");
        deleteCustomer(fakeCustomer);
        remoteAccess.invoke(CustomerNotifyQueueController.class, "processingWithLog", qType, new QueueInputMessage(textMessage), null, -1, -1);

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
        deleteCustomer(fakeCustomer);

        String textMessage = getResourceText("/CustomerDetailsTest_C.xml");
        updateCustomer("00694379", "001", "11", N);

        remoteAccess.invoke(CustomerNotifyQueueController.class, "processingWithLog", qType, new QueueInputMessage(textMessage), null, -1, -1);

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

        String textMessage = getResourceText("/CustomerDetailsTest_errval.xml");

        // Long processing(String queueType, String[] incMessage, String toQueue, long receiveTime, long waitingTime) throws Exception {
        remoteAccess.invoke(CustomerNotifyQueueController.class, "processingWithLog", qType, new QueueInputMessage(textMessage), null, -1, -1);

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

        String textMessage = getResourceText("/CustomerDetailsTest_errmap.xml");

        // Long processing(String queueType, String[] incMessage, String toQueue, long receiveTime, long waitingTime) throws Exception {
        remoteAccess.invoke(CustomerNotifyQueueController.class, "processingWithLog", qType, new QueueInputMessage(textMessage), null, -1, -1);

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
        return null == res.getLong(0) ? 0 : res.getLong(0);
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
