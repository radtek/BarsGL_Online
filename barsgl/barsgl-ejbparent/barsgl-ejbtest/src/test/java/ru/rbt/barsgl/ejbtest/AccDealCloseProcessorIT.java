package ru.rbt.barsgl.ejbtest;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.AccDealCloseProcessor;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.AccDealCloseQueueController;
import ru.rbt.barsgl.ejb.entity.acc.AcDNJournal;
import ru.rbt.barsgl.ejb.repository.AcDNJournalRepository;
import ru.rbt.ejbcore.datarec.DataRecord;

import java.sql.SQLException;

import static ru.rbt.barsgl.ejb.entity.acc.AcDNJournal.Status.PROCESSED;
import static ru.rbt.barsgl.ejbtest.CustomerDetailsNotifyIT.getAuditMaxId;

/**
 * Created by er18837 on 16.03.2018.
 */
public class AccDealCloseProcessorIT extends AbstractTimerJobIT {

    private final String qType = "KTP_CLOSE";
    /**
     * Тест обработки сообщения из очереди
     * @throws Exception
     */
    @Test
    public void testProcessAccClose() throws Exception {

        long idAudit = getAuditMaxId();
        long idCudeno = getAcdenoMaxId();

        String message = IOUtils.toString(this.getClass().getResourceAsStream("/AccountCloseRequest.xml"), "UTF-8");

        Long jId = remoteAccess.invoke(AccDealCloseQueueController.class, "createJournalEntry", qType, message);
        remoteAccess.invoke(AccDealCloseProcessor.class, "process", message, jId);
        Assert.assertNull("Есть запись об ошибке в аудит", getAuditError(idAudit));

        AcDNJournal journal = getAcdenoNewRecord(idCudeno);
        Assert.assertNotNull("Нет новой записи в таблице GL_ACDENO", journal);
        Assert.assertEquals(PROCESSED, journal.getStatus());

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


}
