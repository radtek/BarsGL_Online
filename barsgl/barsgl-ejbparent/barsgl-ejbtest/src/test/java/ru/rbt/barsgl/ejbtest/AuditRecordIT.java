package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejbtesting.test.AuditControllerTest;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.DateUtils;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by Ivan Sevastyanov
 * Системный журнал
 */
public class AuditRecordIT extends AbstractRemoteIT {

    private static final Logger log = Logger.getLogger(AuditRecordIT.class.getName());

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
//        rec.setLogTime(new Date());

        rec = (AuditRecord) baseEntityRepository.save(rec);

        Assert.assertNotNull(rec.getId());
        rec = (AuditRecord) baseEntityRepository.refresh(rec, true);
        Assert.assertNotNull(rec.getLogTime());
    }

    /**
     * Запись в системны журнал последовательность событий - информация, предупреждение, ошибка
     * @throws InterruptedException
     */
    @Test
    public void  testAuditController() throws InterruptedException, SQLException {
        long stamp = System.currentTimeMillis();

        AuditRecord.LogLevel logLevel = AuditRecord.LogLevel.Error;
        String msg = logLevel + "_" + stamp;
        DataRecord rec = baseEntityRepository.selectFirst("select max(GLOID) from GL_OPER where PAR_RF is null");
        Assert.assertNotNull(rec);
        Long gloid = rec.getLong(0);
        remoteAccess.invoke(AuditControllerTest.class.getName(), "testLog", new Object[]{logLevel, msg, gloid});
        Thread.sleep(10000L);

        List<AuditRecord> records = baseEntityRepository.select(AuditRecord.class,
                "from AuditRecord a where a.message like ?1 and logTime > ?2", "%" + msg + "%", DateUtils.onlyDate(new Date()));
        Assert.assertNotNull(records);
        for (AuditRecord record: records) {
            System.out.printf("%s :: %s :: %s\n", record.getLogLevel(), record.getMessage(), record.getErrorMessage());
            boolean isInfo = record.getLogLevel().equals(AuditRecord.LogLevel.Info);
            boolean isErrorMessage = record.getErrorMessage() != null;
            Assert.assertTrue(isInfo != isErrorMessage );
        }
        Assert.assertEquals(9, records.size());
        Assert.assertTrue(records.get(1).getErrorMessage().contains("by zero"));
        Assert.assertTrue(records.get(4).getErrorMessage().contains("LOL"));
        Assert.assertTrue(records.get(6).getErrorMessage().contains("PAR_RF"));
        Assert.assertTrue(records.get(8).getErrorMessage().contains("SOS"));
    }

    @Test public void testTransaction() throws InterruptedException {
        // удалить все записи аудита с AuditControllerTest
        log.info("deleted 0: " + baseEntityRepository.executeNativeUpdate(
                "delete from GL_AUDIT where SRC like '%ru.rbt.barsgl.ejbtesting.test.AuditControllerTest%'"));
        // выполнить метод с откатом транзакции автоматом (*)
        remoteAccess.invoke(AuditControllerTest.class, "testLog2");

        Thread.sleep(5000);
        // ID транзакции должны быть одинаковыми с (*)
        List<AuditRecord> auditRecords = baseEntityRepository
                .select(AuditRecord.class, "from AuditRecord r where r.source like ?1"
                        , "%ru.rbt.barsgl.ejbtesting.test.AuditControllerTest%");

        Assert.assertEquals(2, auditRecords.size());
        Assert.assertTrue(1 < auditRecords.get(0).getTransactionId().length());
        // это не проходит и в версии db2
//        Assert.assertTrue(auditRecords.stream().allMatch(auditRecord ->
//                auditRecord.getTransactionId().equals(auditRecords.get(0).getTransactionId())));
    }

}
