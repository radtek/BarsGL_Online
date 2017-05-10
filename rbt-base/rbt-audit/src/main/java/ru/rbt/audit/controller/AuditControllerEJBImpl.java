package ru.rbt.audit.controller;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.DataTruncation;
import java.sql.Date;
import java.sql.SQLException;
import java.util.Optional;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import ru.rbt.audit.entity.AuditRecord;
import static ru.rbt.audit.entity.AuditRecord.LogLevel.Error;
import static ru.rbt.audit.entity.AuditRecord.LogLevel.Info;
import static ru.rbt.audit.entity.AuditRecord.LogLevel.SysError;
import static ru.rbt.audit.entity.AuditRecord.LogLevel.Warning;
import ru.rbt.audit.repository.AuditRepository;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.ExceptionUtils;
import ru.rbt.shared.ctx.UserRequestHolder;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.shared.security.RequestContext;

/**
 * Created by ER18837 on 03.06.15.
 */
@SuppressWarnings("ALL")
@Stateless
@LocalBean
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class AuditControllerEJBImpl implements AuditController {

    public static final String NON_TRANSACTIONAL = "Not transactional";

    private static final Logger log = Logger.getLogger(AuditController.class);
    private static final String myClassName = AuditControllerEJBImpl.class.getName();
    private static final String myClassNameToo = myClassName + "_";

    @EJB
    private AuditRepository auditRepository;

    @Inject
    private RequestContext contextBean;

    @Override
    public void info(AuditRecord.LogCode operCode, String message) {
        logAuditRecord(AuditRecord.LogLevel.Info, operCode, message, "", null, null);
    }

    @Override
    public void info(AuditRecord.LogCode operCode, String message, BaseEntity entity) {
        logAuditRecord(AuditRecord.LogLevel.Info, operCode, message, "", entity, null);
    }

    @Override
    public void info(AuditRecord.LogCode operCode, String message, String entity_type, String entity_id) {
        logAuditRecord(AuditRecord.LogLevel.Info, operCode, message, "", entity_type, entity_id, null);
    }

    /**
     * @param operCode
     * @param message
     * @param longMessage 4000 letter if Latin only, 2000 if UTF8 and contains cyrillic or other national symbols
     */    
    @Override
    public void stat(AuditRecord.LogCode operCode, String message, String longMessage, String entityId) {
        logAuditRecordStat(AuditRecord.LogLevel.Info, operCode, message, longMessage, entityId);
    }

    @Override
    public void stat(AuditRecord.LogCode operCode, String message, String longMessage) {
        logAuditRecordStat(AuditRecord.LogLevel.Info, operCode, message, longMessage, null);
    }

    @Override
    public void warning(AuditRecord.LogCode operCode, String message) {
        logAuditRecord(AuditRecord.LogLevel.Warning, operCode, message, null, null, null);
    }

    @Override
    public void warning(AuditRecord.LogCode operCode, String message, BaseEntity entity, String errorMessage) {
        logAuditRecord(AuditRecord.LogLevel.Warning, operCode, message, errorMessage, entity, null);
    }

    @Override
    public void warning(AuditRecord.LogCode operCode, String message, BaseEntity entity, Throwable e) {
        logAuditRecord(AuditRecord.LogLevel.Warning, operCode, message, "", entity, e);
    }

    @Override
    public void warning(AuditRecord.LogCode operCode, String message, String entity_type, String entity_id, Throwable e) {
        logAuditRecord(AuditRecord.LogLevel.Warning, operCode, message, "", entity_type, entity_id, e);
    }

    @Override
    public void error(AuditRecord.LogCode operCode, String message, BaseEntity entity, String errorMessage) {
        logAuditRecord(AuditRecord.LogLevel.Error, operCode, message, errorMessage, entity, null);
    }

    @Override
    public void error(AuditRecord.LogCode operCode, String message, BaseEntity entity, Throwable e) {
        logAuditRecord(AuditRecord.LogLevel.Error, operCode, message, "", entity, e);
    }

    @Override
    public void error(AuditRecord.LogCode operCode, String message, String entity_type, String entity_id, String errorMessage) {
        logAuditRecord(AuditRecord.LogLevel.Error, operCode, message, errorMessage, entity_type, entity_id, null);
    }

    @Override
    public void error(AuditRecord.LogCode operCode, String message, String entity_type, String entity_id, Throwable e) {
        logAuditRecord(AuditRecord.LogLevel.Error, operCode, message, "", entity_type, entity_id, e);
    }

    private void logAuditRecord(AuditRecord.LogLevel logLevel, AuditRecord.LogCode operCode,
                                String message, String errorMessage, BaseEntity entity, Throwable e) {
        String entity_type = "", entity_id = "";
        try {
            if (null != entity) {
                entity_id = entity.getId().toString();
                entity_type = entity.getTableName();
                if (StringUtils.isEmpty(entity_type))
                    entity_type = entity.getClass().getName();
            }
        } catch (Throwable t) {
            log.error("" + t.getMessage());
        }
        logAuditRecord(logLevel, operCode, message, errorMessage, entity_type, entity_id, e);
    }

    private void logAuditRecord(AuditRecord.LogLevel logLevel, AuditRecord.LogCode operCode,
                                String message, String errorMessage, String entity, String entity_id, Throwable e) {
        String logSourse = getSource(Thread.currentThread().getStackTrace());
        StringBuilder sbmsg = new StringBuilder(operCode.toString()).append("; ").append(message).append("; source: ").append(logSourse);
        switch (logLevel) {
            case Info:
                log.info(sbmsg.toString(), e);
                break;
            case Warning:
                log.warn(sbmsg.toString(), e);
                break;
            case Error:
                log.error(sbmsg.toString(), e);
                break;
        }
        
        final UserRequestHolder requestHolder = contextBean.getRequest().orElse(UserRequestHolder.empty());
        String userName = requestHolder.getUser();
        String hostName = requestHolder.getUserHost();

        String stackTrace = "";
        String source = "";
        String errorSource = "";
        StringWriter err = new StringWriter();
        if (null != e) {
            String errorMsg = getErrorMessage(e);
            String causedMsg = getCausedMessage(e);
            errorMessage += errorMsg;
            if (!errorMessage.equals(causedMsg))
                errorMessage += ">> " + causedMsg;
            if (!errorMessage.contains("Код ошибки") && Error == logLevel)
                logLevel = SysError;
            e.printStackTrace(new PrintWriter(err));
            stackTrace = err.toString();
            errorSource = getSourceEx(e);
        }
        String transactionId = getTransacionID();

        createAuditRecord(logLevel, operCode,
            message, errorMessage, logSourse, errorSource,
            entity, entity_id, stackTrace, transactionId,
            null, 0, userName, hostName);
    }

    private void logAuditRecordStat(AuditRecord.LogLevel logLevel, AuditRecord.LogCode operCode, String message, String errorMessage, String entityId) {
        String logSourse = getSource(Thread.currentThread().getStackTrace());
        StringBuilder sbmsg = new StringBuilder(operCode.toString()).append("; ").append(message).append("; source: ").append(logSourse);

        log.info(sbmsg.toString());

        final UserRequestHolder requestHolder = contextBean.getRequest().orElse(UserRequestHolder.empty());
        String userName = requestHolder.getUser();
        String hostName = requestHolder.getUserHost();
        String errorSource = "";

        String transactionId = getTransacionID();

        createAuditRecord(logLevel, operCode,
            message, null, logSourse, errorSource,
            null, entityId, errorMessage, transactionId,
            null, 0, userName, hostName);
    }


    private void createAuditRecord(
                                      AuditRecord.LogLevel logLevel, AuditRecord.LogCode operCode,
                                      String message, String errorMessage,
                                      String source, String errorSource,
                                      String entity, String entity_id,
                                      String stackTrace, String transactionId,
                                      String attachment, int duration,
                                      String userName, String userHost) {
        try {

            Date operTime = new Date(System.currentTimeMillis());
            auditRepository.invokeAsynchronous(persistence -> {
//            auditRepository.executeInNewTransaction(persistence -> {
                AuditRecord auditRecord = auditRepository.createAuditRecord(
                    logLevel, operCode,
                    message, errorMessage,
                    source, errorSource,
                    entity, entity_id,
                    stackTrace, transactionId,
                    attachment, duration,
                    userName, userHost, operTime);
                return auditRepository.save(auditRecord);
            });
        } catch (Throwable e) {
            log.error("Ошибка записи в системный журнал: " + e.getMessage(), e);
        }
    }

    private String getSourceString(StackTraceElement el) {
        String className = el.getClassName();
        if (!className.equals(myClassName) && !className.contains(myClassNameToo) &&
                className.contains("ru.rbt") &&
                el.getLineNumber() > 0) {
            StringBuilder builder = new StringBuilder();
//            int p = className.lastIndexOf(".");
            builder.append(className)
                .append(".").append(el.getMethodName())
                .append("(").append(el.getFileName())
                .append(":").append(el.getLineNumber())
                .append(")");
            return builder.toString();
        } else {
            return "";
        }
    }

    private String getSource(StackTraceElement[] stackTraceElements) {
        for (final StackTraceElement el : stackTraceElements) {
            String src = getSourceString(el);
            if (!src.isEmpty())
                return src;
        }
        return "#UNDEFINED#: 0";
    }

    private String getSourceEx(Throwable e) {
        StringBuilder builder = new StringBuilder();
        while (null != e.getCause()) {
            e = e.getCause();
        }
        builder.append("Exception:\n");
        for (final StackTraceElement el : e.getStackTrace()) {
            String src = getSourceString(el);
            if (!src.isEmpty())
                builder.append("\tat ").append(src).append("\n");
        }
        return builder.toString();
    }

    private String getCausedMessage(Throwable e) {
        while (null != e.getCause()) {
            e = e.getCause();
        }
        return getShortMessage(e);
    }

    private String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable,
            ValidationError.class, DataTruncation.class, SQLException.class, DefaultApplicationException.class);
    }

    private String getShortMessage(Throwable throwable) {
        String msg = throwable.getMessage();
        int pos = 0;
        if (null != msg)
            pos = msg.indexOf("\tat ");
        return pos > 0 ? StringUtils.substr(msg, pos) : msg;
    }

    private String getTransacionID() {
        String transactionId = null;
        try {
            transactionId = Optional.ofNullable(auditRepository
                                                    .getTransactionKey()).orElseGet(() -> NON_TRANSACTIONAL).toString();
        } catch (Exception e) {
            transactionId = NON_TRANSACTIONAL;
        }
        int pos0 = transactionId.indexOf("Xid=");
        if (pos0 > 0) {
            int pos1 = transactionId.indexOf(",", pos0 + 1);
            if (pos1 > 0)
                return transactionId.substring(pos0, pos1);
            else
                return transactionId.substring(pos0);
        } else {
            log.error("Не удалось определить Xid транзакции. TransactionKey:\n" + transactionId);
            return "0";
        }
    }
}

