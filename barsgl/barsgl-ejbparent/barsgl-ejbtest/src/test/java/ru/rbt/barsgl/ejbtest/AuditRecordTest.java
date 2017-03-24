package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejbtesting.test.AuditControllerTest;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import ru.rbt.barsgl.audit.entity.AuditRecord;

/**
 * Created by Ivan Sevastyanov
 * Системный журнал
 */
public class AuditRecordTest extends AbstractRemoteTest {

    private static final Logger log = Logger.getLogger(AuditRecordTest.class.getName());

    /**
     * Создание записи в системной журнале
     */
    @Test public void testAuditRecord() {
        AuditRecord rec = new AuditRecord();
        rec.setLogLevel(AuditRecord.LogLevel.Error);
        rec.setLogCode(AuditRecord.LogCode.Authorization);
        rec.setUserHost("191.12.12.12");
        rec.setUserName("User Test Name");
        rec.setMessage("Message");
        rec.setErrorMessage("Error Message");
        rec.setSource("Source");
        rec.setErrorSource("Error Source");
        rec.setStackTrace("Stack Trace");
        rec.setTransactionId("Transaction Id");
        rec.setAttachment(org.apache.commons.lang3.StringUtils.rightPad("", 40000, "Attachment"));
        rec.setLogTime(new Date());

        rec = (AuditRecord) baseEntityRepository.save(rec);

        Assert.assertNotNull(rec.getId());
    }

    /**
     * Запись в системны журнал последовательность событий - информация, предупреждение, ошибка
     * @throws InterruptedException
     */
    @Test
    public void  testAuditController() throws InterruptedException {
        long stamp = System.currentTimeMillis();

        AuditRecord.LogLevel logLevel = AuditRecord.LogLevel.Error;
        String msg = logLevel + "_" + stamp;
        Long gloid = 1234L;
        remoteAccess.invoke(AuditControllerTest.class.getName(), "testLog", new Object[]{logLevel, msg, gloid});
        Thread.sleep(2000L);

        List<AuditRecord> rec = baseEntityRepository.select(AuditRecord.class,
                "from AuditRecord a where a.message like ?1", "%" + msg + "%");
        Assert.assertNotNull(rec);
        Assert.assertEquals(9, rec.size());
    }

    @Test public void testTransaction() throws InterruptedException {
        // удалить все записи аудита с AuditControllerTest
        log.info("deleted 0: " + baseEntityRepository.executeNativeUpdate(
                "delete from gl_audit where src like 'ru.rbt.barsgl.ejbtesting.test.AuditControllerTest%'"));
        // выполнить метод с откатом транзакции автоматом (*)
        remoteAccess.invoke(AuditControllerTest.class, "testLog2");

        Thread.sleep(5000);
        // ID транзакции должны быть одинаковыми с (*)
        List<AuditRecord> auditRecords = baseEntityRepository
                .select(AuditRecord.class, "from AuditRecord r where r.source like ?1"
                        , "ru.rbt.barsgl.ejbtesting.test.AuditControllerTest%");
        Assert.assertEquals(2, auditRecords.size());
        Assert.assertTrue(1 < auditRecords.get(0).getTransactionId().length());
        Assert.assertTrue(auditRecords.stream().allMatch(auditRecord ->
                auditRecord.getTransactionId().equals(auditRecords.get(0).getTransactionId())));
    }

}
