package ru.rbt.barsgl.ejb.repository;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.entity.sec.AuditRecord;
import ru.rbt.barsgl.ejbcore.mapping.BaseEntity;
import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.sql.Date;

import static ru.rbt.barsgl.ejbcore.util.StringUtils.substr;

/**
 * Created by ER18837 on 02.06.15.
 */
@Stateless
@LocalBean
public class AuditRepository extends AbstractBaseEntityRepository<AuditRecord, Long> {

    public AuditRecord createAuditRecord(
            AuditRecord.LogLevel logLevel, AuditRecord.LogCode logCode,
            String message, String errorMessage,
            String source, String errorSource,
            String entity, String entity_id,
            String stackTrace, String transactionId,
            String attachment, int duration,
            String userName, String userHost, Date sysTime) {
        AuditRecord auditRecord = new AuditRecord();

        auditRecord.setUserName(substr(userName, 64));
        auditRecord.setUserHost(substr(userHost, 64));
        auditRecord.setLogCode(logCode);
        auditRecord.setLogLevel(logLevel);
        auditRecord.setMessage(substr(message, 512));
        auditRecord.setErrorMessage(substr(errorMessage, 4000));
        auditRecord.setStackTrace(stackTrace);
        auditRecord.setEntityType(substr(entity, 256));
        auditRecord.setEntityId(substr(entity_id, 128));
        auditRecord.setTransactionId(substr(transactionId, 512));
        auditRecord.setSource(substr(source, 512));
        auditRecord.setErrorSource(substr(errorSource, 4000));
        auditRecord.setAttachment(attachment);
        auditRecord.setProcessDuration(duration);
        auditRecord.setLogTime(sysTime);

        return auditRecord;
    }

    public AuditRecord createAuditRecord(
            AuditRecord.LogLevel logLevel, AuditRecord.LogCode logCode, String message) {
        return createAuditRecord(logLevel, logCode, message
                , null, null, null, null, null, null, null, null, 0, "", "", null);
    }
}
